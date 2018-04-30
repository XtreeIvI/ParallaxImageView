package devlight.io.xtreeivi.parallaximageview.extension

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

fun Drawable.toBitmap(width: Int = 1, height: Int = 1, forceUse: Boolean = false): Bitmap {
    val innerWidth = when {
        forceUse -> if (width < 1) 1 else width
        intrinsicWidth > 0 -> intrinsicWidth
        minimumWidth > 0 -> minimumWidth
        else -> if (width < 1) 1 else width
    }
    val innerHeight = when {
        forceUse -> if (height < 1) 1 else height
        intrinsicHeight > 0 -> intrinsicHeight
        minimumHeight > 0 -> minimumHeight
        else -> if (height < 1) 1 else height
    }
    val bitmap = Bitmap.createBitmap(innerWidth, innerHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}