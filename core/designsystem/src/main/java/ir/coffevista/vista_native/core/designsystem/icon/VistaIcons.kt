package ir.coffevista.vista_native.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Unified, crisp vector icon registry with consistent 24dp viewport,
 * 1.75dp round strokes, and optical balance tailored to Vista's UI.
 */
object VistaIcons {
    private val DefaultFill = SolidColor(Color.Black)

    val HeartOutline: ImageVector by lazy {
        strokeIcon("VistaHeartOutline") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 20.2f)
                curveTo(10.6f, 19.0f, 3.5f, 14.0f, 3.5f, 8.8f)
                curveTo(3.5f, 5.8f, 5.9f, 3.8f, 8.5f, 3.8f)
                curveTo(10.1f, 3.8f, 11.4f, 4.6f, 12f, 5.8f)
                curveTo(12.6f, 4.6f, 13.9f, 3.8f, 15.5f, 3.8f)
                curveTo(18.1f, 3.8f, 20.5f, 5.8f, 20.5f, 8.8f)
                curveTo(20.5f, 14.0f, 13.4f, 19.0f, 12f, 20.2f)
                close()
            }
        }
    }

    val HeartFilled: ImageVector by lazy {
        strokeIcon("VistaHeartFilled") {
            path(fill = DefaultFill) {
                moveTo(12f, 20.4f)
                curveTo(10.6f, 19.2f, 3.2f, 14.0f, 3.2f, 8.7f)
                curveTo(3.2f, 5.5f, 5.8f, 3.5f, 8.5f, 3.5f)
                curveTo(10.1f, 3.5f, 11.4f, 4.3f, 12f, 5.5f)
                curveTo(12.6f, 4.3f, 13.9f, 3.5f, 15.5f, 3.5f)
                curveTo(18.2f, 3.5f, 20.8f, 5.5f, 20.8f, 8.7f)
                curveTo(20.8f, 14.0f, 13.4f, 19.2f, 12f, 20.4f)
                close()
            }
        }
    }

    val BookmarkOutline: ImageVector by lazy {
        strokeIcon("VistaBookmarkOutline") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.2f, 4.2f)
                lineTo(17.8f, 4.2f)
                curveTo(18.35f, 4.2f, 18.8f, 4.65f, 18.8f, 5.2f)
                lineTo(18.8f, 20.2f)
                lineTo(12f, 15.8f)
                lineTo(5.2f, 20.2f)
                lineTo(5.2f, 5.2f)
                curveTo(5.2f, 4.65f, 5.65f, 4.2f, 6.2f, 4.2f)
                close()
            }
        }
    }

    val BookmarkFilled: ImageVector by lazy {
        strokeIcon("VistaBookmarkFilled") {
            path(fill = DefaultFill) {
                moveTo(6.2f, 3.8f)
                lineTo(17.8f, 3.8f)
                curveTo(18.57f, 3.8f, 19.2f, 4.43f, 19.2f, 5.2f)
                lineTo(19.2f, 20.4f)
                lineTo(12f, 15.6f)
                lineTo(4.8f, 20.4f)
                lineTo(4.8f, 5.2f)
                curveTo(4.8f, 4.43f, 5.43f, 3.8f, 6.2f, 3.8f)
                close()
            }
        }
    }

    val Comment: ImageVector by lazy {
        strokeIcon("VistaComment") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20.2f, 11.7f)
                curveTo(20.2f, 16.0f, 16.53f, 19.4f, 12f, 19.4f)
                curveTo(10.65f, 19.4f, 9.38f, 19.1f, 8.26f, 18.56f)
                lineTo(4.3f, 19.8f)
                lineTo(5.45f, 16.15f)
                curveTo(4.42f, 14.88f, 3.8f, 13.35f, 3.8f, 11.7f)
                curveTo(3.8f, 7.4f, 7.47f, 4.0f, 12f, 4.0f)
                curveTo(16.53f, 4.0f, 20.2f, 7.4f, 20.2f, 11.7f)
                close()
            }
        }
    }

    val Share: ImageVector by lazy {
        strokeIcon("VistaShare") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20.4f, 4.2f)
                lineTo(10.6f, 14.0f)
            }
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20.4f, 4.2f)
                lineTo(14.3f, 20.2f)
                lineTo(10.6f, 14.0f)
                lineTo(4.0f, 10.3f)
                lineTo(20.4f, 4.2f)
                close()
            }
        }
    }

    val MoreHorizontal: ImageVector by lazy {
        strokeIcon("VistaMoreHorizontal") {
            circlePath(5.5f, 12f, 1.5f)
            circlePath(12f, 12f, 1.5f)
            circlePath(18.5f, 12f, 1.5f)
        }
    }

    val MoreVertical: ImageVector by lazy {
        strokeIcon("VistaMoreVertical") {
            circlePath(12f, 5.5f, 1.5f)
            circlePath(12f, 12f, 1.5f)
            circlePath(12f, 18.5f, 1.5f)
        }
    }

    val BellOutline: ImageVector by lazy {
        strokeIcon("VistaBellOutline") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 14.5f)
                verticalLineTo(10f)
                curveTo(18f, 6.69f, 15.31f, 4f, 12f, 4f)
                curveTo(8.69f, 4f, 6f, 6.69f, 6f, 10f)
                verticalLineTo(14.5f)
                lineTo(4.2f, 17f)
                horizontalLineTo(19.8f)
                lineTo(18f, 14.5f)
                close()
            }
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9.8f, 19.2f)
                curveTo(10.3f, 20.1f, 11.1f, 20.6f, 12f, 20.6f)
                curveTo(12.9f, 20.6f, 13.7f, 20.1f, 14.2f, 19.2f)
            }
        }
    }

    val BellFilled: ImageVector by lazy {
        strokeIcon("VistaBellFilled") {
            path(fill = DefaultFill) {
                moveTo(18.2f, 14.4f)
                verticalLineTo(10f)
                curveTo(18.2f, 6.58f, 15.42f, 3.8f, 12f, 3.8f)
                curveTo(8.58f, 3.8f, 5.8f, 6.58f, 5.8f, 10f)
                verticalLineTo(14.4f)
                lineTo(4.0f, 17.1f)
                horizontalLineTo(20.0f)
                lineTo(18.2f, 14.4f)
                close()
            }
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(9.8f, 19.3f)
                curveTo(10.3f, 20.2f, 11.1f, 20.7f, 12f, 20.7f)
                curveTo(12.9f, 20.7f, 13.7f, 20.2f, 14.2f, 19.3f)
            }
        }
    }

    val Search: ImageVector by lazy {
        strokeIcon("VistaSearch") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(17.5f, 11f)
                curveTo(17.5f, 14.59f, 14.59f, 17.5f, 11f, 17.5f)
                curveTo(7.41f, 17.5f, 4.5f, 14.59f, 4.5f, 11f)
                curveTo(4.5f, 7.41f, 7.41f, 4.5f, 11f, 4.5f)
                curveTo(14.59f, 4.5f, 17.5f, 7.41f, 17.5f, 11f)
                close()
            }
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(16.1f, 16.1f)
                lineTo(20.0f, 20.0f)
            }
        }
    }

    val Refresh: ImageVector by lazy {
        strokeIcon("VistaRefresh") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19.5f, 12f)
                curveTo(19.5f, 16.14f, 16.14f, 19.5f, 12f, 19.5f)
                curveTo(8.5f, 19.5f, 5.58f, 17.1f, 4.74f, 13.86f)
            }
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.5f, 12f)
                curveTo(4.5f, 7.86f, 7.86f, 4.5f, 12f, 4.5f)
                curveTo(15.2f, 4.5f, 17.92f, 6.51f, 19f, 9.35f)
                moveTo(19.5f, 5.2f)
                lineTo(19.2f, 9.6f)
                lineTo(14.8f, 9.2f)
            }
        }
    }

    val Copy: ImageVector by lazy {
        strokeIcon("VistaCopy") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 9f)
                horizontalLineTo(18f)
                curveTo(19.1f, 9f, 20f, 9.9f, 20f, 11f)
                verticalLineTo(18.5f)
                curveTo(20f, 19.6f, 19.1f, 20.5f, 18f, 20.5f)
                horizontalLineTo(11f)
                curveTo(9.9f, 20.5f, 9f, 19.6f, 9f, 18.5f)
                verticalLineTo(9f)
                close()
            }
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.2f, 15.5f)
                horizontalLineTo(5.8f)
                curveTo(4.7f, 15.5f, 3.8f, 14.6f, 3.8f, 13.5f)
                verticalLineTo(6f)
                curveTo(3.8f, 4.9f, 4.7f, 4f, 5.8f, 4f)
                horizontalLineTo(13.3f)
                curveTo(14.4f, 4f, 15.3f, 4.9f, 15.3f, 6f)
                verticalLineTo(6.4f)
            }
        }
    }

    val Edit: ImageVector by lazy {
        strokeIcon("VistaEdit") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15.8f, 4.7f)
                lineTo(19.3f, 8.2f)
                lineTo(8.8f, 18.7f)
                lineTo(4.5f, 19.5f)
                lineTo(5.3f, 15.2f)
                lineTo(15.8f, 4.7f)
                close()
            }
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(13.8f, 6.7f)
                lineTo(17.3f, 10.2f)
            }
        }
    }

    val Delete: ImageVector by lazy {
        strokeIcon("VistaDelete") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.5f, 7f)
                horizontalLineTo(19.5f)
                moveTo(9.5f, 7f)
                verticalLineTo(4.8f)
                curveTo(9.5f, 4.25f, 9.95f, 3.8f, 10.5f, 3.8f)
                horizontalLineTo(13.5f)
                curveTo(14.05f, 3.8f, 14.5f, 4.25f, 14.5f, 4.8f)
                verticalLineTo(7f)
                moveTo(6.5f, 7f)
                lineTo(7.4f, 18.6f)
                curveTo(7.48f, 19.65f, 8.35f, 20.4f, 9.4f, 20.4f)
                horizontalLineTo(14.6f)
                curveTo(15.65f, 20.4f, 16.52f, 19.65f, 16.6f, 18.6f)
                lineTo(17.5f, 7f)
            }
        }
    }

    val Flag: ImageVector by lazy {
        strokeIcon("VistaFlag") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5.5f, 20.5f)
                verticalLineTo(4.5f)
                moveTo(5.5f, 5.2f)
                curveTo(7.8f, 3.9f, 10.6f, 3.9f, 12.6f, 5.2f)
                curveTo(14.5f, 6.4f, 17f, 6.2f, 18.8f, 5.0f)
                verticalLineTo(13.6f)
                curveTo(17f, 14.8f, 14.5f, 15.0f, 12.6f, 13.8f)
                curveTo(10.6f, 12.5f, 7.8f, 12.5f, 5.5f, 13.8f)
            }
        }
    }

    val Lock: ImageVector by lazy {
        strokeIcon("VistaLock") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 10.5f)
                horizontalLineTo(17f)
                curveTo(18.1f, 10.5f, 19f, 11.4f, 19f, 12.5f)
                verticalLineTo(18.5f)
                curveTo(19f, 19.6f, 18.1f, 20.5f, 17f, 20.5f)
                horizontalLineTo(7f)
                curveTo(5.9f, 20.5f, 5f, 19.6f, 5f, 18.5f)
                verticalLineTo(12.5f)
                curveTo(5f, 11.4f, 5.9f, 10.5f, 7f, 10.5f)
                close()
            }
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(8.5f, 10.5f)
                verticalLineTo(7.8f)
                curveTo(8.5f, 5.87f, 10.07f, 4.3f, 12f, 4.3f)
                curveTo(13.93f, 4.3f, 15.5f, 5.87f, 15.5f, 7.8f)
                verticalLineTo(10.5f)
            }
        }
    }

    val Hide: ImageVector by lazy {
        strokeIcon("VistaHide") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(19.8f, 12f)
                curveTo(19.8f, 16.31f, 16.31f, 19.8f, 12f, 19.8f)
                curveTo(7.69f, 19.8f, 4.2f, 16.31f, 4.2f, 12f)
                curveTo(4.2f, 7.69f, 7.69f, 4.2f, 12f, 4.2f)
                curveTo(16.31f, 4.2f, 19.8f, 7.69f, 19.8f, 12f)
                close()
                moveTo(8.5f, 12f)
                horizontalLineTo(15.5f)
            }
        }
    }

    val Visibility: ImageVector by lazy {
        strokeIcon("VistaVisibility") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.2f, 12f)
                curveTo(5.1f, 8.2f, 8.3f, 6.2f, 12f, 6.2f)
                curveTo(15.7f, 6.2f, 18.9f, 8.2f, 20.8f, 12f)
                curveTo(18.9f, 15.8f, 15.7f, 17.8f, 12f, 17.8f)
                curveTo(8.3f, 17.8f, 5.1f, 15.8f, 3.2f, 12f)
                close()
            }
            circlePath(12f, 12f, 2.3f)
        }
    }

    val VisibilityOff: ImageVector by lazy {
        strokeIcon("VistaVisibilityOff") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.2f, 4.2f)
                lineTo(19.8f, 19.8f)
                moveTo(9.8f, 6.6f)
                curveTo(10.5f, 6.35f, 11.25f, 6.2f, 12f, 6.2f)
                curveTo(15.7f, 6.2f, 18.9f, 8.2f, 20.8f, 12f)
                curveTo(20.1f, 13.4f, 19.2f, 14.6f, 18.1f, 15.5f)
                moveTo(6.1f, 8.7f)
                curveTo(4.9f, 9.6f, 3.9f, 10.7f, 3.2f, 12f)
                curveTo(5.1f, 15.8f, 8.3f, 17.8f, 12f, 17.8f)
                curveTo(13.2f, 17.8f, 14.3f, 17.55f, 15.3f, 17.1f)
            }
        }
    }

    val Archive: ImageVector by lazy {
        strokeIcon("VistaArchive") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.5f, 4.8f)
                horizontalLineTo(19.5f)
                curveTo(20.05f, 4.8f, 20.5f, 5.25f, 20.5f, 5.8f)
                verticalLineTo(8.5f)
                horizontalLineTo(3.5f)
                verticalLineTo(5.8f)
                curveTo(3.5f, 5.25f, 3.95f, 4.8f, 4.5f, 4.8f)
                close()
                moveTo(5.2f, 8.5f)
                verticalLineTo(18.2f)
                curveTo(5.2f, 19.3f, 6.1f, 20.2f, 7.2f, 20.2f)
                horizontalLineTo(16.8f)
                curveTo(17.9f, 20.2f, 18.8f, 19.3f, 18.8f, 18.2f)
                verticalLineTo(8.5f)
                moveTo(10f, 12.6f)
                horizontalLineTo(14f)
            }
        }
    }

    val Info: ImageVector by lazy {
        strokeIcon("VistaInfo") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(20f, 12f)
                curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
                curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
                curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
                close()
                moveTo(12f, 11.2f)
                verticalLineTo(15.6f)
            }
            circlePath(12f, 8.2f, 1.0f)
        }
    }

    val Shield: ImageVector by lazy {
        strokeIcon("VistaShield") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3.6f)
                lineTo(18.8f, 6.4f)
                verticalLineTo(11.6f)
                curveTo(18.8f, 15.8f, 15.9f, 19.1f, 12f, 20.4f)
                curveTo(8.1f, 19.1f, 5.2f, 15.8f, 5.2f, 11.6f)
                verticalLineTo(6.4f)
                lineTo(12f, 3.6f)
                close()
            }
        }
    }

    val Timer: ImageVector by lazy {
        strokeIcon("VistaTimer") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9.5f, 3.6f)
                horizontalLineTo(14.5f)
                moveTo(19f, 13f)
                curveTo(19f, 16.87f, 15.87f, 20f, 12f, 20f)
                curveTo(8.13f, 20f, 5f, 16.87f, 5f, 13f)
                curveTo(5f, 9.13f, 8.13f, 6f, 12f, 6f)
                curveTo(15.87f, 6f, 19f, 9.13f, 19f, 13f)
                close()
                moveTo(12f, 9.5f)
                verticalLineTo(13f)
                lineTo(14.5f, 14.5f)
            }
        }
    }

    val Group: ImageVector by lazy {
        strokeIcon("VistaGroup") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12.2f, 8.2f)
                curveTo(12.2f, 9.97f, 10.77f, 11.4f, 9f, 11.4f)
                curveTo(7.23f, 11.4f, 5.8f, 9.97f, 5.8f, 8.2f)
                curveTo(5.8f, 6.43f, 7.23f, 5f, 9f, 5f)
                curveTo(10.77f, 5f, 12.2f, 6.43f, 12.2f, 8.2f)
                close()
                moveTo(3.8f, 18.8f)
                curveTo(3.8f, 15.8f, 6.1f, 14f, 9f, 14f)
                curveTo(11.9f, 14f, 14.2f, 15.8f, 14.2f, 18.8f)
                moveTo(15.2f, 5.8f)
                curveTo(16.65f, 6.15f, 17.6f, 7.45f, 17.6f, 8.95f)
                curveTo(17.6f, 10.45f, 16.65f, 11.75f, 15.2f, 12.1f)
                moveTo(16.4f, 14.5f)
                curveTo(18.5f, 15.0f, 20.0f, 16.5f, 20.0f, 18.8f)
            }
        }
    }

    val Block: ImageVector by lazy {
        strokeIcon("VistaBlock") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(20f, 12f)
                curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
                curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
                curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
                close()
                moveTo(6.4f, 6.4f)
                lineTo(17.6f, 17.6f)
            }
        }
    }

    val Check: ImageVector by lazy {
        strokeIcon("VistaCheck") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 12.5f)
                lineTo(9.8f, 17.2f)
                lineTo(19.2f, 7.2f)
            }
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("VistaClose") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }
    }

    val QrScanner: ImageVector by lazy {
        strokeIcon("VistaQrScanner") {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 8.5f)
                verticalLineTo(5.8f)
                curveTo(4f, 4.8f, 4.8f, 4f, 5.8f, 4f)
                horizontalLineTo(8.5f)
                moveTo(15.5f, 4f)
                horizontalLineTo(18.2f)
                curveTo(19.2f, 4f, 20f, 4.8f, 20f, 5.8f)
                verticalLineTo(8.5f)
                moveTo(20f, 15.5f)
                verticalLineTo(18.2f)
                curveTo(20f, 19.2f, 19.2f, 20f, 18.2f, 20f)
                horizontalLineTo(15.5f)
                moveTo(8.5f, 20f)
                horizontalLineTo(5.8f)
                curveTo(4.8f, 20f, 4f, 19.2f, 4f, 18.2f)
                verticalLineTo(15.5f)
                moveTo(6.5f, 12f)
                horizontalLineTo(17.5f)
            }
        }
    }

    val ChevronBack: ImageVector by lazy {
        strokeIcon("VistaChevronBack", autoMirror = true) {
            path(
                stroke = DefaultFill,
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 5.5f)
                lineTo(8.5f, 12f)
                lineTo(15f, 18.5f)
            }
        }
    }

    private inline fun strokeIcon(
        name: String,
        autoMirror: Boolean = false,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply(block).build()

    private fun ImageVector.Builder.circlePath(cx: Float, cy: Float, r: Float) {
        path(fill = DefaultFill, pathFillType = PathFillType.NonZero) {
            moveTo(cx - r, cy)
            curveTo(cx - r, cy - r * 0.55f, cx - r * 0.55f, cy - r, cx, cy - r)
            curveTo(cx + r * 0.55f, cy - r, cx + r, cy - r * 0.55f, cx + r, cy)
            curveTo(cx + r, cy + r * 0.55f, cx + r * 0.55f, cy + r, cx, cy + r)
            curveTo(cx - r * 0.55f, cy + r, cx - r, cy + r * 0.55f, cx - r, cy)
            close()
        }
    }
}
