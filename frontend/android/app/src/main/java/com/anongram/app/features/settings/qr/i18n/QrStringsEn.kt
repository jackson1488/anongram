package com.anongram.app.features.settings.qr.i18n

/**
 * English localization for AnonGram QR section.
 */
object QrStringsEn : QrI18nStrings {
    override val myQrCode: String = "My QR Code"
    override val close: String = "Close"
    override val back: String = "Back"
    override val scanQrCode: String = "Scan QR Code"
    override val shareQrDescription: String = "Share QR image"
    override val shareChooserTitle: String = "Share QR Code"
    override val changeStyle: String = "Change style"
    override val downloadQrDescription: String = "Download QR to gallery"
    override fun savedToGallery(handle: String): String = "QR code @$handle saved to Gallery"
    override val saveError: String = "Error saving QR code"
    override val cameraPermissionRequiredTitle: String = "Camera access required"
    override val cameraPermissionRequiredDescription: String =
        "To scan a contact's QR code or log in on another device, allow AnonGram access to the camera."
    override val allowCameraAccess: String = "Allow camera access"
    override val pickPhotoDescription: String = "Choose photo with QR"
    override val flashlightDescription: String = "Flashlight"
    override val photoQrProcessed: String = "QR code from selected photo processed"
    override val grantAccess: String = "Grant access"
    override val stealthLogoDescription: String = "AnonGram Stealth Logo"
}
