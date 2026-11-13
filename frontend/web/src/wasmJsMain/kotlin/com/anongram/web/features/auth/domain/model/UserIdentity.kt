package com.anongram.web.features.auth.domain.model

data class UserIdentity(
    val publicId: String,
    val nickname: String,
    val avatarUri: String? = null,
    val masterVkHex: String,
    val pqMasterVkHex: String? = null,
    val isBiometricsEnabled: Boolean = false,
    val isPinSet: Boolean = false
)
