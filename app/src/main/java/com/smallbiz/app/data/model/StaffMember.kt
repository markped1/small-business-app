package com.smallbiz.app.data.model

import org.json.JSONObject

/**
 * Represents a named staff member with their own PIN.
 * Up to 5 staff members can be added per business.
 * Stored as JSON array in SharedPreferences (not Room — no sensitive data in DB).
 */
data class StaffMember(
    val id: String,         // UUID
    val name: String,       // e.g. "Amaka Obi"
    val pin: String,        // 4+ digit PIN (stored as plain text in prefs)
    val role: String = "staff",  // "staff" or future roles
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("pin", pin)
        put("role", role)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject) = StaffMember(
            id        = json.optString("id", java.util.UUID.randomUUID().toString()),
            name      = json.optString("name", "Staff"),
            pin       = json.optString("pin", ""),
            role      = json.optString("role", "staff"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )

        const val MAX_STAFF = 5
    }
}
