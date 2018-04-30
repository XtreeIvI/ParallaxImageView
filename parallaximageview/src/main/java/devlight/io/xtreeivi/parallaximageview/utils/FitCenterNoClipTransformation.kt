package devlight.io.xtreeivi.parallaximageview.utils

import android.graphics.Bitmap
import com.bumptech.glide.load.Key

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView

import java.security.MessageDigest

/**
 * Helper class that is scale image similarly as Center Crop transformation did,
 * but does not crop rest of image, preserving latter for parallax effect in [ParallaxImageView].
 */
class FitCenterNoClipTransformation : BitmapTransformation() {

    override fun transform(pool: BitmapPool,
                           toTransform: Bitmap,
                           outWidth: Int,
                           outHeight: Int): Bitmap {
        val inWidth = toTransform.width
        val inHeight = toTransform.height
        val newOutWidth: Int
        val newOutHeight: Int
        if (inWidth / inHeight.toFloat() > outWidth / outHeight.toFloat()) {
            newOutHeight = outHeight
            newOutWidth = (inWidth * outHeight / inHeight.toFloat()).toInt()
        } else {
            newOutWidth = outWidth
            newOutHeight = (inHeight * outWidth / inWidth.toFloat()).toInt()
        }
        return TransformationUtils.fitCenter(pool, toTransform, newOutWidth, newOutHeight)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    companion object {
        private val ID = "com.bumptech.glide.load.resource.bitmap.FitCenterOutside"
        private val ID_BYTES = ID.toByteArray(Key.CHARSET)
    }
}