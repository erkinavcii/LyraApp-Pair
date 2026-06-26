package com.turkcell.lyraapp.data.player


import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val currentTrack: StateFlow<NowPlayingTrack?>
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>
    val downloadingTrackIds: StateFlow<Set<String>>
    val downloadedTracks: StateFlow<List<NowPlayingTrack>>

    fun play(track: NowPlayingTrack)
    fun playQueue(tracks: List<NowPlayingTrack>, startIndex: Int)
    fun skipNext()
    fun skipPrevious()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)

    fun setRepeat(enabled: Boolean)
    fun setShuffle(enabled: Boolean)

    fun isTrackDownloaded(trackId: String): Boolean
    suspend fun downloadTrack(track: NowPlayingTrack): Result<Unit>
    fun deleteDownloadedTrack(trackId: String): Boolean
}

