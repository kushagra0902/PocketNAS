/*
 * Copyright 2026 PocketServer contributors
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.pocketserver.core.protocol

import java.util.Base64
import kotlinx.serialization.json.Json

object InvitationCodec {
    private const val PREFIX = "pocketserver1:"
    private const val MAX_TICKET_CHARACTERS = 24 * 1024
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }

    fun encode(ticket: InvitationTicket): String {
        require(ticket.version == ProtocolLimits.VERSION)
        val payload = json.encodeToString(InvitationTicket.serializer(), ticket).encodeToByteArray()
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    fun decode(value: String): InvitationTicket {
        val text = value.trim()
        require(text.length <= MAX_TICKET_CHARACTERS) { "Invitation is too large" }
        require(text.startsWith(PREFIX)) { "Invitation format is not recognized" }
        val bytes = try {
            Base64.getUrlDecoder().decode(text.removePrefix(PREFIX))
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Invitation encoding is invalid", error)
        }
        val ticket = try {
            json.decodeFromString(InvitationTicket.serializer(), bytes.decodeToString())
        } catch (error: Exception) {
            throw IllegalArgumentException("Invitation data is invalid", error)
        }
        require(ticket.version == ProtocolLimits.VERSION) { "Invitation version is unsupported" }
        require(ticket.secret.length in 32..256) { "Invitation secret is invalid" }
        require(ticket.hostPeerId.value.isNotBlank()) { "Invitation host identity is missing" }
        return ticket
    }
}
