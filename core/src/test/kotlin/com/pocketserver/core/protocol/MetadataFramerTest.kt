package com.pocketserver.core.protocol

import com.pocketserver.core.model.RequestId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MetadataFramerTest {
    private val framer = MetadataFramer()

    @Test
    fun roundTripPreservesRequest() {
        val request = RequestEnvelope(
            requestId = RequestId("request-0001"),
            operation = Operation.LIST_LIBRARIES,
        )
        val frame = framer.encode(RequestEnvelope.serializer(), request)
        assertEquals(request, framer.decode(RequestEnvelope.serializer(), frame))
    }

    @Test
    fun rejectsLengthMismatch() {
        val bad = byteArrayOf(0, 0, 0, 5, 1, 2)
        assertThrows(FramingException::class.java) {
            framer.decode(RequestEnvelope.serializer(), bad)
        }
    }

    @Test
    fun readRequestRejectsOverflow() {
        assertThrows(ArithmeticException::class.java) {
            ReadRequest(
                libraryId = com.pocketserver.core.model.LibraryId("library-01"),
                resourceId = com.pocketserver.core.model.ResourceId("resource-01"),
                offset = Long.MAX_VALUE,
                length = 1,
            )
        }
    }
}

