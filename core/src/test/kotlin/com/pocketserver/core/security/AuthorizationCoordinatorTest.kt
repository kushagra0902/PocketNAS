package com.pocketserver.core.security

import com.pocketserver.core.model.DeviceId
import com.pocketserver.core.model.LibraryId
import com.pocketserver.core.model.MemberId
import com.pocketserver.core.model.OperationId
import com.pocketserver.core.model.PeerId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizationCoordinatorTest {
    @Test
    fun revocationCancelsActiveReadAndBlocksNewAdmission() = runBlocking {
        val coordinator = AuthorizationCoordinator(maxActiveOperations = 2)
        val member = MemberId("member-01")
        val device = DeviceId("device-01")
        val peer = PeerId("peer-identity-0001")
        val library = LibraryId("library-01")
        val principal = DevicePrincipal(device, member, peer)
        var cancelled = false

        coordinator.replacePolicy(listOf(principal), emptySet(), setOf(member to library))
        val admitted = coordinator.admit(OperationId("op-1"), peer, library) { cancelled = true }
        assertTrue(admitted is AdmissionResult.Allowed)

        coordinator.replacePolicy(listOf(principal), setOf(device), emptySet())
        assertTrue(cancelled)
        assertEquals(0, coordinator.activeCount())
        assertEquals(
            AdmissionResult.RevokedDevice,
            coordinator.admit(OperationId("op-2"), peer, library) {},
        )
    }
}

