/*
 * Copyright 2026 PocketServer contributors
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.pocketserver.core.protocol

import com.pocketserver.core.contracts.DuplexStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.serialization.KSerializer

/** Reads one bounded length-prefixed metadata message from a transport stream. */
suspend fun <T> DuplexStream.receiveMetadata(
    serializer: KSerializer<T>,
    framer: MetadataFramer = MetadataFramer(),
): T {
    val prefix = receiveExactly(Int.SIZE_BYTES)
    val length = ByteBuffer.wrap(prefix).order(ByteOrder.BIG_ENDIAN).int
    if (length !in 0..ProtocolLimits.MAX_METADATA_BYTES) {
        throw FramingException("Invalid metadata length: $length")
    }
    val payload = receiveExactly(length)
    return framer.decode(serializer, prefix + payload)
}

suspend fun <T> DuplexStream.sendMetadata(
    serializer: KSerializer<T>,
    value: T,
    framer: MetadataFramer = MetadataFramer(),
) {
    send(framer.encode(serializer, value))
}

private suspend fun DuplexStream.receiveExactly(length: Int): ByteArray {
    if (length == 0) return ByteArray(0)
    val output = ByteArray(length)
    var copied = 0
    while (copied < length) {
        val part = receive(length - copied)
            ?: throw FramingException("Unexpected end of stream while reading metadata")
        if (part.isEmpty()) throw FramingException("Transport returned an empty non-EOF read")
        part.copyInto(output, destinationOffset = copied)
        copied += part.size
    }
    return output
}
