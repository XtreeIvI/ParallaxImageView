package devlight.io.xtreeivi.parallaximageview.extension

import android.graphics.Rect
import android.graphics.RectF

fun Rect.yByFraction(fraction: Float) = this.top + this.height() * fraction
fun Rect.xByFraction(fraction: Float) = this.left + this.width() * fraction
fun Rect.absHeight() = Math.abs(this.height())
fun Rect.absWidth() = Math.abs(this.width())
fun RectF.absHeight() = Math.abs(this.height())
fun RectF.absWidth() = Math.abs(this.width())
fun RectF.clear() = this.set(0.0F, 0.0F, 0.0F, 0.0F)
fun RectF.whRatio() = if (this.absHeight() <= 0) 0.0F else absWidth() / absHeight()

fun RectF.centerToCenter(rect: RectF) = offset(rect.centerX() - centerX(), rect.centerY() - centerY())

/**
 * @return new size
 */
fun RectF.makeSquareAndReturnSize(toLowerSide:Boolean = true):Float {
    val size = if (toLowerSide) Math.min(absWidth(), absHeight()) else Math.max(absWidth(), absHeight())
    with(size/2.0F) {
        val centerX = centerX()
        val centerY = centerY()
        set(centerX - this, centerY - this, centerX + this, centerY + this)
    }
    return size
}
fun RectF.maxSideSize() = Math.max(absWidth(), absHeight())
fun RectF.minSideSize() = Math.min(absWidth(), absHeight())

