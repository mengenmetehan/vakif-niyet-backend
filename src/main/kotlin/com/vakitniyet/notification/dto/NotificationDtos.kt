package com.vakitniyet.notification.dto

data class UpdateNotificationSettingsRequest(
    val enabled: Boolean? = null,
    val ilceId: String? = null,
    val offsetMinutes: Int? = null,
    val contentType: String? = null,  // HADIS | AYET | KARMA
    val fajrEnabled: Boolean? = null,
    val dhuhrEnabled: Boolean? = null,
    val asrEnabled: Boolean? = null,
    val maghribEnabled: Boolean? = null,
    val ishaEnabled: Boolean? = null
)

data class NotificationSettingsResponse(
    val enabled: Boolean,
    val ilceId: String?,
    val offsetMinutes: Int,
    val contentType: String,
    val fajrEnabled: Boolean,
    val dhuhrEnabled: Boolean,
    val asrEnabled: Boolean,
    val maghribEnabled: Boolean,
    val ishaEnabled: Boolean
)

data class NotificationTestResponse(
    val contentType: String,
    val title: String,
    val body: String,
    val metin: String,
    val kaynak: String,
    val sent: Boolean
)
