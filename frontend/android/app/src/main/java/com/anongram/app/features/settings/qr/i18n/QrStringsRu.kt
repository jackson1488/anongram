package com.anongram.app.features.settings.qr.i18n

/**
 * Русская локализация раздела QR-кода AnonGram.
 */
object QrStringsRu : QrI18nStrings {
    override val myQrCode: String = "Мой QR-код"
    override val close: String = "Закрыть"
    override val back: String = "Назад"
    override val scanQrCode: String = "Сканировать QR-код"
    override val shareQrDescription: String = "Поделиться изображением QR"
    override val shareChooserTitle: String = "Поделиться QR-кодом"
    override val changeStyle: String = "Изменить стиль"
    override val downloadQrDescription: String = "Скачать QR в галерею"
    override fun savedToGallery(handle: String): String = "QR-код @$handle сохранён в Галерею"
    override val saveError: String = "Ошибка при сохранении QR-кода"
    override val cameraPermissionRequiredTitle: String = "Требуется доступ к камере"
    override val cameraPermissionRequiredDescription: String =
        "Чтобы отсканировать QR-код собеседника или войти на другом устройстве, разрешите AnonGram доступ к камере."
    override val allowCameraAccess: String = "Разрешить доступ к камере"
    override val pickPhotoDescription: String = "Выбрать фото с QR"
    override val flashlightDescription: String = "Фонарик"
    override val photoQrProcessed: String = "QR-код из выбранного фото обработан"
    override val grantAccess: String = "Предоставить доступ"
    override val stealthLogoDescription: String = "AnonGram Stealth Logo"
}
