package com.anongram.app.features.profile.i18n

/**
 * Контракт локализации модуля «Профиль» (RU / EN).
 */
interface ProfileI18nStrings {
    val profileTitle: String
    val onlineStatus: String
    val selectPhoto: String
    val edit: String
    val settings: String
    val changeProfileColor: String
    val changeName: String
    val copyLink: String
    val linkCopied: String
    val phoneLabel: String
    val bioLabel: String
    val usernameLabel: String
    val birthdayLabel: String
    val defaultBio: String
    val defaultBirthday: String
    val postsTab: String
    val postsArchiveTab: String
    val noPostsTitle: String
    val noPostsSubtitle: String
    val addPost: String
    val logout: String
    val back: String

    // Смена цвета профиля
    val tabProfile: String
    val tabName: String
    val backgroundEmoji: String
    val none: String
    val resetProfileColor: String
    val applyStyle: String
    val styleApplied: String
    val previewNameHint: String

    // Выбор и редактор фото/видео профиля
    val choosePhotoOrVideo: String
    val chooseEmoji: String
    val removePhoto: String
    val camera: String
    val selectThumbnailFrame: String
    val maxVideoLimitHint: String
    val rotate: String
    val mirror: String
    val draw: String
    val adjust: String
    val done: String
    val editNameTitle: String
    val save: String
    val cancel: String
    val permissionsNeeded: String

    // Галерея аватаров и полноэкранный просмотр
    val photoCounterOf: String
    val menuSetPrimaryPhoto: String
    val menuSaveToGallery: String
    val menuShare: String
    val menuDeletePhoto: String
    val deletePhotoTitle: String
    val deletePhotoMessage: String
    val deleteConfirm: String
    val savedToGalleryToast: String

    // Поля профиля и экран Аккаунт
    val copiedToClipboard: String
    val copyAction: String
    val copyUsernameAction: String
    val editBioAction: String
    val editUsernameAction: String
    val editBirthdayAction: String
    val deleteBirthdayAction: String
    val accountTitle: String
    val yourNameSection: String
    val firstNameHint: String
    val lastNameHint: String
    val bioFooter: String
    val infoAboutYouSection: String
    val accountIdSubtitle: String
    val tapToChangeUsername: String
    val tapToChangeBirthday: String
    val usernameScreenTitle: String
    val usernameFieldTitle: String
    val usernameHelper: String
    val birthdaySheetTitle: String
}
