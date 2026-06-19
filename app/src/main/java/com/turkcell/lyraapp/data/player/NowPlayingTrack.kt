package com.turkcell.lyraapp.data.player

data class NowPlayingTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val startColor: Long,
    val endColor: Long,
    val durationMs: Long = 223_000L,
) {
    companion object {
        private val gradientPairs = listOf(
            Pair(0xFF8B6FB8L, 0xFF4A3D6BL),
            Pair(0xFF7C83D9L, 0xFF3E4486L),
            Pair(0xFFD98E4AL, 0xFF8A5526L),
            Pair(0xFF4AC2A8L, 0xFF1F6E5CL),
            Pair(0xFF6FBF5AL, 0xFF356B2AL),
            Pair(0xFF5AAFC9L, 0xFF2A5F73L),
            Pair(0xFF3D5A80L, 0xFF1B2A45L)
        )

        fun getColorsForId(id: String): Pair<Long, Long> {
            val index = kotlin.math.abs(id.hashCode()) % gradientPairs.size
            return gradientPairs[index]
        }
    }
}
