package com.anongram.app.features.settings.qr.i18n

/**
 * Контракт строк локализации (i18n) для раздела QR-кода AnonGram.
 */
interface QrI18nStrings {
    val myQrCode: String
    val close: String
    val back: String
    val scanQrCode: String
    val shareQrDescription: String
    val shareChooserTitle: String
    val changeStyle: String
    val downloadQrDescription: String
    fun savedToGallery(handle: String): String
    val saveError: String
    val cameraPermissionRequiredTitle: String
    val cameraPermissionRequiredDescription: String
    val allowCameraAccess: String
    val pickPhotoDescription: String
    val flashlightDescription: String
    val photoQrProcessed: String
    val grantAccess: String
    val stealthLogoDescription: String
}
