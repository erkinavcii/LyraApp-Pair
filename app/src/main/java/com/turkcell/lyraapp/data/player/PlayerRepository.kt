package com.turkcell.lyraapp.data.player


import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val currentTrack: StateFlow<NowPlayingTrack?>
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>
    val downloadingTrackIds: StateFlow<Set<String>>

    fun play(track: NowPlayingTrack)
    fun playQueue(tracks: List<NowPlayingTrack>, startIndex: Int)
    fun skipNext()
    fun skipPrevious()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)

    fun isTrackDownloaded(trackId: String): Boolean
    suspend fun downloadTrack(trackId: String): Result<Unit>
    fun deleteDownloadedTrack(trackId: String): Boolean
}

