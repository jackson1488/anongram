package com.anongram.app.core.theme

enum class FontApplyScope {
    ALL,
    MESSAGES_ONLY,
    UI_ONLY
}

enum class FontFilterCategory {
    ALL,
    CYRILLIC,
    SANS_SERIF,
    SERIF,
    MONOSPACE,
    DISPLAY,
    HANDWRITING
}

data class FontCatalogItem(
    val id: String,
    val family: String,
    val category: String,
    val hasCyrillic: Boolean
)

data class InstalledFontItem(
    val id: String,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
    val isImported: Boolean = false
)

data class FontModuleState(
    val activeFontId: String? = null,
    val applyScope: FontApplyScope = FontApplyScope.ALL,
    val installedFonts: List<InstalledFontItem> = emptyList(),
    val catalog: List<FontCatalogItem> = emptyList(),
    val isCatalogLoading: Boolean = false,
    val catalogError: String? = null,
    val downloadingFontId: String? = null,
    val downloadProgress: Float = 0f,
    val downloadError: String? = null
) {
    val activeInstalledFont: InstalledFontItem?
        get() = installedFonts.firstOrNull { it.id == activeFontId }
}
