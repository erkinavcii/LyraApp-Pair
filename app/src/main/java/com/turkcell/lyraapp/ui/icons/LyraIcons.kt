package com.turkcell.lyraapp.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * LyraApp ikon seti.
 *
 * Material Icons bağımlılığı eklemeden, ekranların ihtiyaç duyduğu glyph'leri
 * 24x24 viewport'lu [ImageVector] olarak tanımlar.
 */
object LyraIcons {

    val Waveform: ImageVector by lazy {
        lyraIcon(
            name = "Waveform",
            pathData = "M7,18h2V6H7v12zM11,22h2V2h-2v20zM3,14h2v-4H3v4zM15,18h2V6h-2v12zM19,10v4h2v-4h-2z",
        )
    }

    val Waveform4: ImageVector by lazy {
        lyraIcon(
            name = "Waveform4",
            pathData = "M12 24l-4-4h-4v-4l-4-4 4-4v-4h4l4-4 4 4h4v4l4 4-4 4v4h-4l-4 4z", // Dummy path for illustration, replace if needed
        )
    }

    val Smartphone: ImageVector by lazy {
        lyraIcon(
            name = "Smartphone",
            pathData = "M15.5,1h-8C6.12,1 5,2.12 5,3.5v17C5,21.88 6.12,23 7.5,23h8c1.38,0 " +
                    "2.5,-1.12 2.5,-2.5v-17C18,2.12 16.88,1 15.5,1zM13,21h-3v-1h3v1zM16.25,18H6.75V4h9.5V18z",
        )
    }

    val Lock: ImageVector by lazy {
        lyraIcon(
            name = "Lock",
            pathData = "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0," +
                    "1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 " +
                    "-2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 " +
                    "1.71,0 3.1,1.39 3.1,3.1v2z",
        )
    }

    val Visibility: ImageVector by lazy {
        lyraIcon(
            name = "Visibility",
            pathData = "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73," +
                    "-4.39 -6,-7.5 -11,-7.5zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 " +
                    "-5,5zM12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z",
        )
    }

    val ArrowForward: ImageVector by lazy {
        lyraIcon(
            name = "ArrowForward",
            pathData = "M12,4l-1.41,1.41L16.17,11H4v2h12.17l-5.58,5.59L12,20l8,-8z",
        )
    }

    val ArrowBack: ImageVector by lazy {
        lyraIcon(
            name = "ArrowBack",
            pathData = "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z",
        )
    }

    val LightMode: ImageVector by lazy {
        lyraIcon(
            name = "LightMode",
            pathData = "M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zm0 8c-1.65 0-3-1.35-3-3s1.35-3 3-3 3 1.35 3 3-1.35 3-3 3zm2-11h-4v3h4V4zm-4 13h4v3h-4v-3zm10-7h-3v4h3v-4zM7 10H4v4h3v-4zm10.39-4.89l-2.12-2.12-2.12 2.12 2.12 2.12 2.12-2.12zM5.61 18.89l2.12 2.12 2.12-2.12-2.12-2.12-2.12 2.12zM18.89 18.89l-2.12-2.12-2.12 2.12 2.12 2.12 2.12-2.12zM7.73 5.61L5.61 7.73l2.12 2.12 2.12-2.12-2.12-2.12z"
        )
    }

    val DarkMode: ImageVector by lazy {
        lyraIcon(
            name = "DarkMode",
            pathData = "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-3.03 0-5.5-2.47-5.5-5.5 0-1.82.89-3.42 2.26-4.4C12.92 3.04 12.46 3 12 3z"
        )
    }

    val Pause: ImageVector by lazy {
        lyraIcon(
            name = "Pause",
            pathData = "M6 19h4V5H6v14zm8-14v14h4V5h-4z"
        )
    }

    val SkipNext: ImageVector by lazy {
        lyraIcon(
            name = "SkipNext",
            pathData = "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"
        )
    }

    val ChevronDown: ImageVector by lazy {
        lyraIcon(
            name = "ChevronDown",
            pathData = "M7.41,8.58L12,13.17l4.59,-4.59L18,10l-6,6l-6,-6L7.41,8.58z",
        )
    }

    val MoreVert: ImageVector by lazy {
        lyraIcon(
            name = "MoreVert",
            pathData = "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2z" +
                    "M12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z" +
                    "M12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z",
        )
    }

    val Play: ImageVector by lazy {
        lyraIcon(
            name = "Play",
            pathData = "M8,5v14l11,-7z",
        )
    }

    val SkipPrevious: ImageVector by lazy {
        lyraIcon(
            name = "SkipPrevious",
            pathData = "M6,6h2v12H6V6zM9.5,12l8.5,6V6L9.5,12z",
        )
    }

    val Shuffle: ImageVector by lazy {
        lyraIcon(
            name = "Shuffle",
            pathData = "M10.59,9.17L5.41,4L4,5.41l5.17,5.17L10.59,9.17z" +
                    "M14.5,4l2.04,2.04L4,18.59L5.41,20L17.96,7.46L20,9.5V4H14.5z" +
                    "M14.83,13.41l-1.41,1.41l3.13,3.13L14.5,20H20v-5.5l-2.04,2.04L14.83,13.41z",
        )
    }

    val Repeat: ImageVector by lazy {
        lyraIcon(
            name = "Repeat",
            pathData = "M7,7h10v3l4,-4l-4,-4v3H5v6h2V7zM17,17H7v-3l-4,4l4,4v-3h12v-6h-2V17z",
        )
    }

    val Cast: ImageVector by lazy {
        lyraIcon(
            name = "Cast",
            pathData = "M1,18v3h3c0,-1.66 -1.34,-3 -3,-3z" +
                    "M1,14v2c2.76,0 5,2.24 5,5h2c0,-3.87 -3.13,-7 -7,-7z" +
                    "M1,10v2c4.97,0 9,4.03 9,9h2c0,-6.08 -4.93,-11 -11,-11z" +
                    "M21,3H3C1.9,3 1,3.9 1,5v3h2V5h18v14h-7v2h7c1.1,0 2,-0.9 2,-2V5C23,3.9 22.1,3 21,3z",
        )
    }

    val QueueMusic: ImageVector by lazy {
        lyraIcon(
            name = "QueueMusic",
            pathData = "M15,6H3v2h12V6zM15,10H3v2h12V10zM3,16h8v-2H3V16z" +
                    "M17,6v8.18C16.69,14.07 16.35,14 16,14c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3V8h3V6H17z",
        )
    }

    val Notifications: ImageVector by lazy {
        lyraIcon(
            name = "Notifications",
            pathData = "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.9,2 2,2z" +
                    "M18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68" +
                    "C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z",
        )
    }

    val Check: ImageVector by lazy {
        lyraIcon(
            name = "Check",
            pathData = "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41L9,16.17z",
        )
    }

    // ── Alt gezinme çubuğu (LyraBottomBar) ikonları ──

    val Home: ImageVector by lazy {
        lyraIcon(
            name = "Home",
            pathData = "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z",
        )
    }

    val HomeOutlined: ImageVector by lazy {
        lyraIcon(
            name = "HomeOutlined",
            pathData = "M12,5.69l5,4.5V18h-2v-6H9v6H7v-7.81l5,-4.5M12,3L2,12h3v8h6v-6h2v6h6v-8h3L12,3z",
        )
    }

    val Search: ImageVector by lazy {
        lyraIcon(
            name = "Search",
            pathData = "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 " +
                    "9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5," +
                    "4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 " +
                    "11.99,14 9.5,14z",
        )
    }

    val LibraryMusic: ImageVector by lazy {
        lyraIcon(
            name = "LibraryMusic",
            pathData = "M20,2H8C6.9,2 6,2.9 6,4v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 " +
                    "-0.9,-2 -2,-2zM18,7h-3v5.5c0,1.38 -1.12,2.5 -2.5,2.5S10,13.88 10,12.5 11.12,10 " +
                    "12.5,10c0.57,0 1.08,0.19 1.5,0.51V5h4v2zM4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6z",
        )
    }

    val LibraryMusicOutlined: ImageVector by lazy {
        lyraIcon(
            name = "LibraryMusicOutlined",
            pathData = "M18,7h-3v5.5c0,1.38 -1.12,2.5 -2.5,2.5S10,13.88 10,12.5 11.12,10 12.5,10c0.57," +
                    "0 1.08,0.19 1.5,0.51V5h4V7zM20,4v12H8V4H20M20,2H8C6.9,2 6,2.9 6,4v12c0,1.1 0.9,2 2," +
                    "2h12c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2L20,2zM4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6z",
        )
    }

    val Favorite: ImageVector by lazy {
        lyraIcon(
            name = "Favorite",
            pathData = "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 " +
                    "3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4," +
                    "6.86 -8.55,11.54L12,21.35z",
        )
    }

    val FavoriteOutlined: ImageVector by lazy {
        lyraIcon(
            name = "FavoriteOutlined",
            pathData = "M16.5,3c-1.74,0 -3.41,0.81 -4.5,2.09C10.91,3.81 9.24,3 7.5,3 4.42,3 2,5.42 " +
                    "2,8.5c0,3.78 3.4,6.86 8.55,11.54L12,21.35l1.45,-1.32C18.6,15.36 22,12.28 22,8.5 " +
                    "22,5.42 19.58,3 16.5,3zM12.1,18.55l-0.1,0.1 -0.1,-0.1C7.14,14.24 4,11.39 4,8.5 " +
                    "4,6.5 5.5,5 7.5,5c1.54,0 3.04,0.99 3.57,2.36h1.87C13.46,5.99 14.96,5 16.5,5c2,0 " +
                    "3.5,1.5 3.5,3.5 0,2.89 -3.14,5.74 -7.9,10.05z",
        )
    }

    val Person: ImageVector by lazy {
        lyraIcon(
            name = "Person",
            pathData = "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c" +
                    "-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z",
        )
    }

    val Download: ImageVector by lazy {
        lyraIcon(
            name = "Download",
            pathData = "M19,9h-4V3H9v6H5l7,7l7,-7zM5,18v2h14v-2H5z",
        )
    }

    val PersonOutlined: ImageVector by lazy {
        lyraIcon(
            name = "PersonOutlined",
            pathData = "M12,5.9c1.16,0 2.1,0.94 2.1,2.1s-0.94,2.1 -2.1,2.1S9.9,9.16 9.9,8s0.94,-2.1 " +
                    "2.1,-2.1zM12,14.9c2.97,0 6.1,1.46 6.1,2.1v1.1H5.9V17c0,-0.64 3.13,-2.1 6.1,-2.1zM12," +
                    "4C9.79,4 8,5.79 8,8s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zM12,13c-2.67,0 -8,1.34 " +
                    "-8,4v3h16v-3c0,-2.66 -5.33,-4 -8,-4z",
        )
    }
}

private fun lyraIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()