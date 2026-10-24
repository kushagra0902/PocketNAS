/*
 * Copyright 2026 PocketServer contributors
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.pocketserver.core.security

import com.pocketserver.core.model.DeviceId
import com.pocketserver.core.model.LibraryId
import com.pocketserver.core.model.MemberId
import com.pocketserver.core.model.OperationId
import com.pocketserver.core.model.PeerId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DevicePrincipal(
    val deviceId: DeviceId,
    val memberId: MemberId,
    val peerId: PeerId,
)

data class Admission(
    val operationId: OperationId,
    val principal: DevicePrincipal,
    val libraryId: LibraryId,
    val policyGeneration: Long,
)

sealed interface AdmissionResult {
    data class Allowed(val admission: Admission) : AdmissionResult
    data object UnknownDevice : AdmissionResult
    data object RevokedDevice : AdmissionResult
    data object MissingGrant : AdmissionResult
    data object LimitExceeded : AdmissionResult
}

/**
 * Coordinates policy mutation with operation admission. Durable persistence is supplied by the
 * app module; this class owns the in-memory generation and cancellation boundary.
 */
class AuthorizationCoordinator(
    private val maxActiveOperations: Int,
) {
    private val mutex = Mutex()
    private var generation = 0L
    private val principals = mutableMapOf<PeerId, DevicePrincipal>()
    private val revokedDevices = mutableSetOf<DeviceId>()
    private val grants = mutableSetOf<Pair<MemberId, LibraryId>>()
    private val active = mutableMapOf<OperationId, ActiveOperation>()

    private data class ActiveOperation(
        val admission: Admission,
        val cancel: () -> Unit,
    )

    suspend fun replacePolicy(
        devices: Collection<DevicePrincipal>,
        revoked: Set<DeviceId>,
        readGrants: Set<Pair<MemberId, LibraryId>>,
    ): Long = mutex.withLock {
        principals.clear()
        devices.associateByTo(principals) { it.peerId }
        revokedDevices.clear()
        revokedDevices.addAll(revoked)
        grants.clear()
        grants.addAll(readGrants)
        generation += 1
        val invalid = active.values.filterNot { operation ->
            principals[operation.admission.principal.peerId]?.deviceId == operation.admission.principal.deviceId &&
                operation.admission.principal.deviceId !in revokedDevices &&
                (operation.admission.principal.memberId to operation.admission.libraryId) in grants
        }
        invalid.forEach { active.remove(it.admission.operationId) }
        invalid.forEach { it.cancel() }
        generation
    }

    suspend fun admit(
        operationId: OperationId,
        peerId: PeerId,
        libraryId: LibraryId,
        onCancel: () -> Unit,
    ): AdmissionResult = mutex.withLock {
        val principal = principals[peerId] ?: return@withLock AdmissionResult.UnknownDevice
        if (principal.deviceId in revokedDevices) return@withLock AdmissionResult.RevokedDevice
        if ((principal.memberId to libraryId) !in grants) return@withLock AdmissionResult.MissingGrant
        if (active.size >= maxActiveOperations) return@withLock AdmissionResult.LimitExceeded
        val admission = Admission(operationId, principal, libraryId, generation)
        active[operationId] = ActiveOperation(admission, onCancel)
        AdmissionResult.Allowed(admission)
    }

    suspend fun complete(operationId: OperationId) {
        mutex.withLock { active.remove(operationId) }
    }

    suspend fun currentGeneration(): Long = mutex.withLock { generation }
    suspend fun activeCount(): Int = mutex.withLock { active.size }
}
