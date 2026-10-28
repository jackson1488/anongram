package com.anongram.app.features.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт управления аудиоплеером (ТЗ, player: PlayerController).
 * Не живёт во ViewModel окна, живёт как долгоживущий синглтон/сервис.
 */
interface PlayerController {
    val state: StateFlow<PlayerState>

    fun play(queue: List<Track>, index: Int = 0)
    fun toggle()
    fun seek(ms: Long)
    fun next()
    fun prev()
    fun setRepeat(mode: RepeatMode)
    fun stop()
    fun openFullPlayer()
    fun closeFullPlayer()
}
