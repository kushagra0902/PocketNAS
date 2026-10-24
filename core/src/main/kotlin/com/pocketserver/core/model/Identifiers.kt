/*
 * Copyright 2026 PocketServer contributors
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.pocketserver.core.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class PeerId(val value: String) {
    init {
        require(value.length in 16..256) { "Peer ID length is invalid" }
    }
}

@JvmInline
@Serializable
value class MemberId(val value: String) {
    init {
        require(value.length in 8..128) { "Member ID length is invalid" }
    }
}

@JvmInline
@Serializable
value class DeviceId(val value: String) {
    init {
        require(value.length in 8..128) { "Device ID length is invalid" }
    }
}

@JvmInline
@Serializable
value class LibraryId(val value: String) {
    init {
        require(value.length in 8..128) { "Library ID length is invalid" }
    }
}

@JvmInline
@Serializable
value class ResourceId(val value: String) {
    init {
        require(value.length in 8..256) { "Resource ID length is invalid" }
    }
}

@JvmInline
@Serializable
value class RequestId(val value: String) {
    init {
        require(value.length in 8..128) { "Request ID length is invalid" }
    }
}

@JvmInline
@Serializable
value class OperationId(val value: String)

