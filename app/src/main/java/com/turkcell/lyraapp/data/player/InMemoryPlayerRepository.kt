package com.turkcell.lyraapp.data.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryPlayerRepository @Inject constructor() : PlayerRepository {

    private val _currentTrack = MutableStateFlow<NowPlayingTrack?>(null)
    override val currentTrack: StateFlow<NowPlayingTrack?> = _currentTrack.asStateFlow()

    private var queue: List<NowPlayingTrack> = emptyList()
    private var currentIndex: Int = -1

    override fun play(track: NowPlayingTrack) {
        queue = listOf(track)
        currentIndex = 0
        _currentTrack.value = track
    }

    override fun playQueue(tracks: List<NowPlayingTrack>, startIndex: Int) {
        queue = tracks
        currentIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _currentTrack.value = queue[currentIndex]
    }

    override fun skipNext() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex + 1) % queue.size
        _currentTrack.value = queue[currentIndex]
    }

    override fun skipPrevious() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex - 1 + queue.size) % queue.size
        _currentTrack.value = queue[currentIndex]
    }
}
