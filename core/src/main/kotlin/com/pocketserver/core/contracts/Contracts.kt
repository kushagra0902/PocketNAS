/*
 * Copyright 2026 PocketServer contributors
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.pocketserver.core.contracts

import com.pocketserver.core.model.LibraryId
import com.pocketserver.core.model.PeerId
import com.pocketserver.core.model.ResourceId
import com.pocketserver.core.protocol.LibraryDescriptor
import com.pocketserver.core.protocol.ResourceDescriptor
import kotlinx.coroutines.flow.Flow

data class ConnectionHints(
    val hostPeerId: PeerId,
    val relayUrls: List<String> = emptyList(),
    val directAddresses: List<String> = emptyList(),
)

enum class PathKind { DIRECT, RELAYED, UNKNOWN }

data class PathState(val kind: PathKind, val description: String? = null)

interface DuplexStream {
    suspend fun send(bytes: ByteArray)
    suspend fun receive(maxBytes: Int): ByteArray?
    suspend fun finishSending()
    fun cancel(reason: String)
}

interface AuthenticatedConnection {
    val peerId: PeerId
    val pathStates: Flow<PathState>
    suspend fun openStream(): DuplexStream
    suspend fun acceptStream(): DuplexStream?
    suspend fun close(reason: String)
}

interface TransportEndpoint {
    val localPeerId: PeerId
    val pathStates: Flow<PathState>
    fun connectionHints(): ConnectionHints
    suspend fun connect(hints: ConnectionHints): AuthenticatedConnection
    suspend fun accept(): AuthenticatedConnection?
    suspend fun close()
}

data class StorageCapabilities(
    val seekable: Boolean,
    val knownLength: Long?,
    val available: Boolean,
    val contentVersion: String?,
)

interface BoundedRead {
    val expectedLength: Long?
    suspend fun read(buffer: ByteArray, offset: Int, length: Int): Int
    suspend fun close()
    fun cancel()
}

interface AuthorizedStorage {
    suspend fun listLibraries(): List<LibraryDescriptor>
    suspend fun list(libraryId: LibraryId, parentId: ResourceId?, cursor: String?): List<ResourceDescriptor>
    suspend fun stat(libraryId: LibraryId, resourceId: ResourceId): ResourceDescriptor
    suspend fun openRead(
        libraryId: LibraryId,
        resourceId: ResourceId,
        offset: Long,
        length: Long?,
        expectedVersion: String?,
    ): BoundedRead
}
