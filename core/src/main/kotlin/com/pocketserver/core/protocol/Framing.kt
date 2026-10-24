/*
 * Copyright 2026 PocketServer contributors
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.pocketserver.core.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class FramingException(message: String) : IllegalArgumentException(message)

class MetadataFramer(
    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    },
) {
    fun <T> encode(serializer: KSerializer<T>, value: T): ByteArray {
        val payload = json.encodeToString(serializer, value).encodeToByteArray()
        if (payload.size > ProtocolLimits.MAX_METADATA_BYTES) {
            throw FramingException("Metadata exceeds ${ProtocolLimits.MAX_METADATA_BYTES} bytes")
        }
        return ByteBuffer.allocate(Int.SIZE_BYTES + payload.size)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(payload.size)
            .put(payload)
            .array()
    }

    fun <T> decode(serializer: KSerializer<T>, frame: ByteArray): T {
        if (frame.size < Int.SIZE_BYTES) throw FramingException("Missing length prefix")
        val length = ByteBuffer.wrap(frame, 0, Int.SIZE_BYTES).order(ByteOrder.BIG_ENDIAN).int
        if (length < 0 || length > ProtocolLimits.MAX_METADATA_BYTES) {
            throw FramingException("Invalid metadata length: $length")
        }
        if (frame.size != Int.SIZE_BYTES + length) {
            throw FramingException("Frame length does not match prefix")
        }
        return try {
            json.decodeFromString(serializer, frame.decodeToString(Int.SIZE_BYTES, frame.size))
        } catch (error: Exception) {
            throw FramingException("Invalid metadata: ${error.message}")
        }
    }
}

