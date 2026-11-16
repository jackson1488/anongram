package com.anongram.app.features.chats.links.preview

enum class LinkMediaType {
    NONE,
    IMAGE,
    VIDEO
}

data class LinkPreviewData(
    val url: String,
    val domain: String,
    val title: String,
    val description: String = "",
    val imageUrl: String = "",
    val siteName: String = "",
    val mediaType: LinkMediaType = LinkMediaType.NONE,
    val embedUrl: String = ""
)
