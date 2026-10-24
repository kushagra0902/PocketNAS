/*
 * Copyright 2026 PocketServer contributors
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.pocketserver.core.protocol

import com.pocketserver.core.model.LibraryId
import com.pocketserver.core.model.DeviceId
import com.pocketserver.core.model.MemberId
import com.pocketserver.core.model.PeerId
import com.pocketserver.core.model.RequestId
import com.pocketserver.core.model.ResourceId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ProtocolLimits {
    const val VERSION = 1
    const val MAX_METADATA_BYTES = 16 * 1024
    const val MAX_LIST_ENTRIES = 100
    const val MAX_READ_BYTES = 4L * 1024L * 1024L
    const val MAX_CONNECTIONS = 16
    const val MAX_STREAMS_PER_PEER = 4
    const val MAX_GLOBAL_DATA_STREAMS = 8
    const val READ_BUFFER_BYTES = 64 * 1024
}

@Serializable
enum class Operation {
    @SerialName("pair") PAIR,
    @SerialName("list_libraries") LIST_LIBRARIES,
    @SerialName("list") LIST,
    @SerialName("stat") STAT,
    @SerialName("read") READ,
}

@Serializable
data class RequestEnvelope(
    val version: Int = ProtocolLimits.VERSION,
    val requestId: RequestId,
    val operation: Operation,
    val pair: PairRequest? = null,
    val list: ListRequest? = null,
    val stat: StatRequest? = null,
    val read: ReadRequest? = null,
)

@Serializable
data class PairRequest(
    val inviteId: String,
    val secret: String,
    val deviceName: String,
)

@Serializable
data class InvitationTicket(
    val version: Int = ProtocolLimits.VERSION,
    val hostPeerId: PeerId,
    val inviteId: String,
    val secret: String,
    val expiresAtEpochMillis: Long,
    val relayUrls: List<String> = emptyList(),
    val directAddresses: List<String> = emptyList(),
)

@Serializable
data class ListRequest(
    val libraryId: LibraryId,
    val parentId: ResourceId? = null,
    val cursor: String? = null,
    val limit: Int = ProtocolLimits.MAX_LIST_ENTRIES,
) {
    init {
        require(limit in 1..ProtocolLimits.MAX_LIST_ENTRIES)
    }
}

@Serializable
data class StatRequest(val libraryId: LibraryId, val resourceId: ResourceId)

@Serializable
data class ReadRequest(
    val libraryId: LibraryId,
    val resourceId: ResourceId,
    val offset: Long,
    val length: Long? = null,
    val expectedVersion: String? = null,
) {
    init {
        require(offset >= 0) { "Negative offset" }
        require(length == null || length in 1..ProtocolLimits.MAX_READ_BYTES) { "Invalid read length" }
        if (length != null) Math.addExact(offset, length)
    }
}

@Serializable
data class ResponseEnvelope(
    val version: Int = ProtocolLimits.VERSION,
    val requestId: RequestId,
    val status: ResponseStatus,
    val error: ProtocolError? = null,
    val libraries: List<LibraryDescriptor>? = null,
    val entries: List<ResourceDescriptor>? = null,
    val resource: ResourceDescriptor? = null,
    val read: ReadDescriptor? = null,
    val pairResult: PairResult? = null,
    val nextCursor: String? = null,
)

@Serializable
data class PairResult(
    val memberId: MemberId,
    val deviceId: DeviceId,
)

@Serializable
enum class ResponseStatus { OK, ERROR }

@Serializable
enum class ErrorCode {
    INVALID_REQUEST,
    INCOMPATIBLE_VERSION,
    NOT_FOUND,
    NOT_AUTHORIZED,
    NOT_PAIRED,
    INVITE_EXPIRED,
    INVITE_ALREADY_USED,
    APPROVAL_REQUIRED,
    INVALID_RANGE,
    SOURCE_UNAVAILABLE,
    SEEK_UNSUPPORTED,
    CONTENT_CHANGED,
    LIMIT_EXCEEDED,
    CANCELLED,
    TRANSPORT_UNAVAILABLE,
    INTERNAL,
}

@Serializable
data class ProtocolError(
    val code: ErrorCode,
    val message: String,
    val retryable: Boolean = false,
)

@Serializable
data class LibraryDescriptor(val id: LibraryId, val displayName: String)

@Serializable
data class ResourceDescriptor(
    val id: ResourceId,
    val displayName: String,
    val isDirectory: Boolean,
    val byteLength: Long? = null,
    val mimeType: String? = null,
    val seekable: Boolean = false,
    val contentVersion: String? = null,
)

@Serializable
data class ReadDescriptor(
    val offset: Long,
    val byteLength: Long,
    val totalLength: Long? = null,
    val contentVersion: String? = null,
    val digestAlgorithm: String? = null,
)
