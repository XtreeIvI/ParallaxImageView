package devlight.io.xtreeivi.parallaximageview.extension

import android.graphics.Path
import android.graphics.RectF

fun Path.addBevelRect(rect: RectF, dx: Float = 0.0F, dy: Float = 0.0F) {
    moveTo(rect.left, rect.top + dy)
    lineTo(rect.left + dx, rect.top)
    lineTo(rect.right - dx, rect.top)
    lineTo(rect.right, rect.top + dy)
    lineTo(rect.right, rect.bottom - dy)
    lineTo(rect.right - dx, rect.bottom)
    lineTo(rect.left + dx, rect.bottom)
    lineTo(rect.left, rect.bottom - dy)
    lineTo(rect.left, rect.top + dy)
}

private val rectF = RectF()
fun Path.addRoundRectOverlap(rect: RectF, rx: Float = 0.0F, ry: Float = 0.0F) {
    moveTo(rect.left, rect.top + ry)
    arcTo(
            rectF.apply { set(rect.left, rect.top, rect.left + rx * 2, rect.top + ry * 2) },
            180.0F,
            90.0F,
            false
    )
    lineTo(rect.right - rx, rect.top)
    arcTo(
            rectF.apply { set(rect.right - rx * 2, rect.top, rect.right, rect.top + ry * 2) },
            270.0F,
            90.0F,
            false
    )
    lineTo(rect.right, rect.bottom - ry)
    arcTo(
            rectF.apply { set(rect.right - rx * 2, rect.bottom - ry * 2, rect.right, rect.bottom) },
            0.0F,
            90.0F,
            false
    )
    lineTo(rect.left + rx, rect.bottom)
    arcTo(
            rectF.apply { set(rect.left, rect.bottom - ry * 2, rect.left + rx * 2, rect.bottom) },
            90.0F,
            90.0F,
            false
    )
    lineTo(rect.left, rect.top + ry)
}