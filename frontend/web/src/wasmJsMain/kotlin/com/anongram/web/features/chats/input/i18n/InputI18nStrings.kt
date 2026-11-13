package com.anongram.web.features.chats.input.i18n

interface InputI18nStrings {
    val messagePlaceholder: String
    val captionPlaceholder: String
    val emoji: String
    val attach: String
    val openFullEditor: String
    val send: String
    val save: String
    val stop: String
    val micDesc: String
    val videoRoundDesc: String

    val cancel: String
    val slideLeftToCancel: String
    val lockRecording: String

    val editMessage: String
    val message: String
    val replace: String
    val editPhoto: String
    val cancelEdit: String
    fun inReplyTo(author: String): String
    val replyToSelf: String
    val cancelReply: String

    val back: String
    val undo: String
    val redo: String

    val removeAttachment: String
    val addMore: String
    fun albumPhotos(count: Int): String
    val attachment: String
    val file: String
    val delete: String
    val draft: String
    val media: String
    val audioTrack: String
    val unknown: String
    fun location(lat: Double, lon: Double): String
    fun liveLocation(durationMin: Int): String

    val keyboard: String
    val lists: String
    val latexFormula: String
    val attachFilePhoto: String

    val styleBold: String
    val styleItalic: String
    val styleUnderline: String
    val styleStrikethrough: String
    val styleHeading: String
    val styleQuote: String
    val styleCallout: String
    val styleFont: String
    val styleSpoiler: String
    val styleCode: String
    val styleRegular: String
    val backToStyles: String
    val selected: String
    val submenu: String
    fun standardQuote(title: String): String
    fun collapsibleQuote(title: String): String

    val fontSans: String
    val fontSerif: String
    val fontMono: String
    val fontCursive: String
    val fontMonoShort: String
    val fontCursiveShort: String

    val listNone: String
    val listBullet: String
    val listNumbered: String
    val listChecklist: String
    val listCollapsible: String
    val indentRight: String
    val outdent: String

    fun codePlaceholder(language: String): String
    fun headingPlaceholder(level: Int): String
    val quotePlaceholder: String
    val calloutPlaceholder: String
    val enterHeadingPlaceholder: String
    val enterTextPlaceholder: String
    val collapsibleTemplateTitle: String
    val collapsibleTemplateBody: String

    fun headingLevel(level: Int, sp: Int): String

    val done: String

    val expand: String
    val collapse: String

    val settingsSectionGeneral: String
    val settingsLanguageTitle: String
    val settingsLanguageSubtitle: String
    val activeLanguageBadge: String
    val close: String

    val bannerCopiedToClipboard: String
    val bannerFullTextCopied: String
    val bannerCodeCopied: String
    val bannerFormulaCopied: String
    val bannerRecordPermissionRequired: String
    val bannerFileSavedToDownloads: String
    val bannerTrackSavedToMusic: String
    val bannerSaveFailed: String
    val bannerVideoCircleSavedToGallery: String
    val bannerVideoCircleSaveFailed: String
    val bannerVoiceSavedToDownloads: String
    val bannerVoiceSaveFailed: String
    val bannerVideoSavedToGallery: String
    val bannerPhotoSavedToGallery: String
    val bannerSavedToGallery: String
    fun bannerAlbumSavedCount(count: Int): String
    fun bannerAlbumSavedPartial(count: Int, total: Int): String
    val bannerAlbumSaveFailed: String
    val bannerFileSaveFailed: String
    fun bannerFilesSavedPartial(saved: Int, total: Int): String
    val bannerFilesSaveFailed: String
    val bannerTrackSaveFailed: String
    fun bannerTracksSavedPartial(saved: Int, total: Int): String
    val bannerTracksSaveFailed: String
    val bannerNothingToSave: String
    val bannerShareVideoPrepareFailed: String
    val bannerVoiceUnavailable: String
    val bannerFileUnavailable: String
    val bannerAudioUnavailable: String
    fun bannerSavedToGalleryNamed(name: String): String
    val bannerPhotoSaveFailed: String
    fun bannerSavedToDownloadsNamed(name: String): String
    fun bannerSavedToMusicNamed(title: String): String
    fun bannerSavedNamed(title: String): String
    fun bannerTrackSavedNamed(title: String): String
    fun bannerOpeningFileNamed(name: String): String
    fun bannerNoAppForFileNamed(name: String): String

    val searchNothingFound: String
    val searchZeroOfZero: String
    val stickersSection: String
    val gifSection: String
    val createSticker: String
    val settings: String
    val gifGallerySoon: String
    val editingVoice: String
    val editingVideoCircle: String
    fun trimOrReRecord(durationLabel: String): String
    val trimVoice: String
    val trimVideoCircle: String
    val photoLabel: String
    val videoLabel: String
    val playVideo: String
    val listen: String
    val playVideoNote: String
    fun videoNoteTime(timeLabel: String): String
    val geolocationLabel: String
    val contactLabel: String
    val play: String
    val pause: String
    val watch: String
    val replyToMessageTitle: String
    val removeReply: String
    fun filesCount(count: Int): String
    fun audioTracksCount(count: Int): String
    fun voiceMessageDuration(durationLabel: String): String
    fun videoMessageDuration(durationLabel: String): String
    fun contactNamed(name: String): String
    fun locationNamed(address: String): String
    fun stickerNamed(symbol: String): String
    val switchCamera: String
    val flash: String
    val pauseAndTrim: String
    val deleteVideo: String
    val tapToTrimOrListen: String
    val pauseOrTrim: String
    val cancelUpper: String
    val deleteRecording: String

    val chatSettingsTitle: String
    val themeCardTitle: String
    val themeCardSubtitle: String
    val themePolarityDark: String
    val themePolarityLight: String
    val themeTabPresets: String
    val themeTabWheel: String
    val themeTabSaved: String
    val themeTintDepth: String
    val themeTintMonochrome: String
    val themeTintAmoled: String
    val themeTintSoftOs: String
    val themeTintFullOs: String
    val themeBubbleStyle: String
    val themeBubbleGradient: String
    val themeBubbleTinted: String
    val themeBubbleOutline: String
    val themeAccentHex: String
    val themeSaturation: String
    val themeSaveButton: String
    val themeCustomNamePlaceholder: String
    val themeCopyCode: String
    val themeImportPlaceholder: String
    val themeImportApply: String
    val themeNoSavedYet: String
    val themePreviewIncoming: String
    val themePreviewIncomingReply: String
    fun themePreviewOutgoing(themeName: String): String
    val themePreviewInputPlaceholder: String
    val themeCodeCopied: String
    val themeImportInvalid: String
    val themeImportApplied: String
    val themeSavedToast: String
    val themeFilterAll: String
    val themeFilterDark: String
    val themeFilterLight: String
}
