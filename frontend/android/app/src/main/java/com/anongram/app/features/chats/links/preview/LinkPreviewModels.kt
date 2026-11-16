package com.anongram.app.features.chats.links.preview

data class LinkPreviewData(
    val url: String,
    val domain: String,
    val title: String,
    val description: String = "",
    val imageUrl: String = "",
    val siteName: String = ""
)
