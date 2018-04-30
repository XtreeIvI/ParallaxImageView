package devlight.io.xtreeivi.sample.fragment

import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.annotation.StringRes
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.*
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView
import devlight.io.xtreeivi.parallaximageview.extension.addFlag
import devlight.io.xtreeivi.parallaximageview.extension.containsFlag
import devlight.io.xtreeivi.parallaximageview.extension.removeFlag
import devlight.io.xtreeivi.parallaximageview.extension.toggleFlag
import devlight.io.xtreeivi.sample.R
import devlight.io.xtreeivi.sample.fragment.core.BaseFragment
import devlight.io.xtreeivi.sample.framework.FORMAT_FRACTION
import devlight.io.xtreeivi.sample.interface_adapters.TextWatcherAdapter
import kotlinx.android.synthetic.main.fragment_widget.*
import java.util.concurrent.TimeUnit

class WidgetFragment : BaseFragment(),
        SeekBar.OnSeekBarChangeListener,
        RadioGroup.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener,
        View.OnLongClickListener,
        View.OnClickListener {

    private var currentImageIndex = 0
    private var currentBgImageIndex = 0
    private var maxViewWidth = 0
    private var maxViewHeight = 0
    private var maxImageDimension = 1200
    private var maxViewDimension = 0
    private var minViewDimension = 0
    private var minViewSize = 0
    private var maxViewPadding = 0
    private var isWidthHeightBound = false
    private var isVerticalFractionAutoPlay = false
    private var isHorizontalFractionAutoPlay = false
    private var isViewWidthProgressChangeFromText = false
    private var isViewHeightProgressChangeFromText = false
    private var isViewWidthChangeFromProgress = false
    private var isViewHeightChangeFromProgress = false
    private var isViewPaddingChangeFromProgress = false
    private var isViewPaddingChangeFromText = false
    private var isVerticalFractionChangeFromProgress = false
    private var isVerticalFractionChangeFromText = false
    private var isHorizontalFractionChangeFromProgress = false
    private var isHorizontalFractionChangeFromText = false
    private var isParallaxTensionFractionChangeFromProgress = false
    private var isParallaxTensionFractionChangeFromText = false
    private var isViewCornerChangeFromProgress = false
    private var isViewCornerChangeFromText = false
    private var isImageCornerChangeFromProgress = false
    private var isImageCornerChangeFromText = false
    private var isBackgroundRadiusChangeFromProgress = false
    private var isBackgroundRadiusChangeFromText = false

    private val imagesList = listOf(
            R.drawable.square_image_1,
            R.drawable.square_image_2,
            R.drawable.square_image_3,
            R.drawable.square_image_4,
            R.drawable.square_image_5,
            R.drawable.square_image_6,
            R.drawable.tall_image_1,
            R.drawable.tall_image_2,
            R.drawable.tall_image_3,
            R.drawable.tall_image_4,
            R.drawable.wide_image_1,
            R.drawable.wide_image_2,
            R.drawable.wide_image_3,
            R.drawable.wide_image_4,
            R.mipmap.ic_launcher,
            R.drawable.ic_camera_48,
            R.drawable.ic_burst_mode_64
    )

    private val backgroundImagesList = listOf(
            0,
            R.drawable.bg_gradient_horizontal,
            R.drawable.bg_gradient_vertical,
            R.drawable.bg_image_1,
            R.drawable.bg_image_2,
            R.drawable.bg_image_3,
            R.drawable.bg_image_4,
            R.drawable.bg_image_5,
            R.drawable.bg_image_6
    )

    private val interpolatorMap = hashMapOf<String, TimeInterpolator>(
            Pair("Accelerate Decelerate Interpolator", AccelerateDecelerateInterpolator()),
            Pair("Decelerate Interpolator", DecelerateInterpolator()),
            Pair("Accelerate Interpolator", AccelerateInterpolator()),
            Pair("Linear Interpolator", LinearInterpolator()),
            Pair("Bounce Interpolator", BounceInterpolator()),
            Pair("Cycle 2 Interpolator", CycleInterpolator(2.0F)),
            Pair("Linear Out Slow InInterpolator", LinearOutSlowInInterpolator()),
            Pair("Fast Out Linear In Interpolator", FastOutLinearInInterpolator()),
            Pair("Fast Out Slow In Interpolator", FastOutSlowInInterpolator())
    )
    private var currentInterpolator: TimeInterpolator? = interpolatorMap["Accelerate Decelerate Interpolator"]

    private var toast: Toast? = null

    private val verticalPlayHandler = Handler()
    private val playVerticalFractionRunnable = object : Runnable {

        private val fullCycleDuration = TimeUnit.SECONDS.toMillis(2)
        private val delay = (fullCycleDuration / 100)
        private var offset = 1
            get() = if (isReversedDirection) -1 else 1
        var isReversedDirection = false

        override fun run() {
            var currentProgress = sbVerticalFraction.progress + offset
            if (currentProgress > 100) {
                currentProgress = 100
                isReversedDirection = true
            } else if (currentProgress < 0) {
                currentProgress = 0
                isReversedDirection = false
            }
            sbVerticalFraction.progress = currentProgress
            verticalPlayHandler.postDelayed(this, delay)
        }
    }

    private val horizontalPlayHandler = Handler()
    private val playHorizontalFractionRunnable = object : Runnable {

        private val fullCycleDuration = TimeUnit.SECONDS.toMillis(2)
        private val delay = (fullCycleDuration / 100)
        private var offset = 1
            get() = if (isReversedDirection) -1 else 1
        var isReversedDirection = false

        override fun run() {
            var currentProgress = sbHorizontalFraction.progress + offset
            if (currentProgress > 100) {
                currentProgress = 100
                isReversedDirection = true
            } else if (currentProgress < 0) {
                currentProgress = 0
                isReversedDirection = false
            }
            sbHorizontalFraction.progress = currentProgress
            horizontalPlayHandler.postDelayed(this, delay)
        }
    }

    private var isCustomUrlGrayscale = false
    private var isCustomUrlBlur = false
    private var customUrlWidth = 0
    private var customUrlHeight = 0
    private var customUrlNumber = 0
    private var lastCustomUrl: String? = null
    private var lastLoadedCustomUrl: String? = null
    private var isSuccessfullyLoaded = false

    override fun containerView(): Int = R.layout.fragment_widget

    override fun setUI() {
        val res = resources
        minViewSize = res.getDimension(R.dimen.size_min_img).toInt()
        customUrlWidth = minViewSize
        customUrlHeight = minViewSize
        parallaxImageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                parallaxImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                maxViewWidth = parallaxImageView.width
                maxViewHeight = parallaxImageView.height
                minViewDimension = Math.min(maxViewWidth, maxViewHeight)
                maxViewDimension = Math.max(maxViewWidth, maxViewHeight)
                maxViewPadding = (minViewDimension - minViewSize) / 2
                setImageDimensionRelatedParams()
            }
        })

        sbVerticalFraction.setOnSeekBarChangeListener(this)
        sbVerticalFraction.progress = 50

        sbHorizontalFraction.setOnSeekBarChangeListener(this)
        sbHorizontalFraction.progress = 50

        sbParallaxTensionFraction.setOnSeekBarChangeListener(this)
        sbParallaxTensionFraction.progress = 100

        etxtVerticalFraction.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isVerticalFractionChangeFromProgress) return
                val percent = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtVerticalFraction.setText("0")
                    return
                }

                val clampedPercent = when {
                    percent > 100 -> {
                        showToast("vertical parallax fraction clamped to 100")
                        100
                    }
                    percent < 0 -> {
                        showToast("vertical parallax fraction clamped to 0")
                        0
                    }
                    else -> {
                        showToast()
                        percent
                    }
                }

                isVerticalFractionChangeFromText = true
                sbVerticalFraction.progress = clampedPercent
                isVerticalFractionChangeFromText = false

            }
        })
        etxtHorizontalFraction.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isHorizontalFractionChangeFromProgress) return
                val percent = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtHorizontalFraction.setText("0")
                    return
                }

                val clampedPercent = when {
                    percent > 100 -> {
                        showToast("horizontal parallax fraction clamped to 100")
                        100
                    }
                    percent < 0 -> {
                        showToast("horizontal parallax fraction clamped to 0")
                        0
                    }
                    else -> {
                        showToast()
                        percent
                    }
                }

                isHorizontalFractionChangeFromText = true
                sbHorizontalFraction.progress = clampedPercent
                isHorizontalFractionChangeFromText = false

            }
        })
        etxtParallaxTensionFraction.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isParallaxTensionFractionChangeFromProgress) return
                val percent = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtHorizontalFraction.setText("0")
                    return
                }

                val clampedPercent = when {
                    percent > 100 -> {
                        showToast("parallax tension fraction clamped to 100")
                        100
                    }
                    percent < 0 -> {
                        showToast("parallax tension fraction clamped to 0")
                        0
                    }
                    else -> {
                        showToast()
                        percent
                    }
                }

                isParallaxTensionFractionChangeFromText = true
                sbParallaxTensionFraction.progress = clampedPercent
                isParallaxTensionFractionChangeFromText = false

            }
        })
        etxtParallaxTensionFraction.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isParallaxTensionFractionChangeFromProgress) return
                val percent = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtParallaxTensionFraction.setText("0")
                    return
                }

                val clampedPercent = when {
                    percent > 100 -> {
                        showToast("parallax tension fraction clamped to 100")
                        100
                    }
                    percent < 0 -> {
                        showToast("parallax tension fraction clamped to 0")
                        0
                    }
                    else -> {
                        showToast()
                        percent
                    }
                }

                isParallaxTensionFractionChangeFromText = true
                sbParallaxTensionFraction.progress = clampedPercent
                isParallaxTensionFractionChangeFromText = false

            }
        })
        etxtAbsoluteParallaxTension.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                val px = try {
                    s.toString().toFloat()
                } catch (e: Exception) {
                    etxtAbsoluteParallaxTension.setText("0")
                    0.0F
                }

                if (px == 0.0F && llParallaxTensionFractionContainer.alpha != 1.0F) {
                    llParallaxTensionFractionContainer.animate().alpha(1.0F).duration = 300
                    sbParallaxTensionFraction.isEnabled = true
                    etxtParallaxTensionFraction.isEnabled = true
                } else if (px != 0.0F && llParallaxTensionFractionContainer.alpha != 0.3F) {
                    llParallaxTensionFractionContainer.animate().alpha(0.3F).duration = 300
                    sbParallaxTensionFraction.isEnabled = false
                    etxtParallaxTensionFraction.isEnabled = false
                }

                if (px != 0.0F && txtParallaxTensionFractionAbsoluteOn.visibility != View.VISIBLE) {
                    txtParallaxTensionFractionAbsoluteOn.visibility = View.VISIBLE
                    cyclePopupAnimation(txtParallaxTensionFractionAbsoluteOn)
                } else if (px == 0.0F && txtParallaxTensionFractionAbsoluteOn.visibility != View.GONE) {
                    txtParallaxTensionFractionAbsoluteOn.animate().cancel()
                    txtParallaxTensionFractionAbsoluteOn.visibility = View.GONE
                }

                parallaxImageView.parallaxAbsoluteTension = px
            }
        })

        btnParallaxInterpolator.text = "Accelerate Decelerate Interpolator"
        btnParallaxInterpolator.setOnClickListener(this)
        btnPivInfo.setOnClickListener(this)
        btnCustomImageApply.setOnClickListener(this)

        cbCustomImageBlur.setOnCheckedChangeListener(this)
        cbCustomImageGrayscale.setOnCheckedChangeListener(this)

        cbParallaxDirectionHorizontal.isChecked = parallaxImageView.parallaxDirectionFlag
                .containsFlag(ParallaxImageView.ParallaxDirectionFlag.HORIZONTAL)
        cbParallaxDirectionVertical.isChecked = parallaxImageView.parallaxDirectionFlag
                .containsFlag(ParallaxImageView.ParallaxDirectionFlag.VERTICAL)
        cbForceParallaxByScale.isChecked = parallaxImageView.isForceParallaxByScale
        cbInverseParallaxDirections.isChecked = parallaxImageView.isParallaxInverseDirection
        cbSwapParallaxDirections.isChecked = parallaxImageView.isSwapParallaxDirections
        cbForceRecalculations.isChecked = parallaxImageView.isForceRecalculationEnabled
        cbPreventViewCornerOverlap.isChecked = parallaxImageView.isPreventViewCornerOverlap
        cbForceViewCornerWhEqual.isChecked = parallaxImageView.isForceViewportCornersWhRatioEqual
        cbPreventImageCornerOverlap.isChecked = parallaxImageView.isPreventImageCornerOverlap
        cbForceImageCornerWhEqual.isChecked = parallaxImageView.isForceImageCornersWhRatioEqual
        cbImageCornerClipSource.isChecked = parallaxImageView.imageCornerClipFlag containsFlag ParallaxImageView.ImageCornerClipFlag.SOURCE
        cbImageCornerClipTension.isChecked = parallaxImageView.imageCornerClipFlag containsFlag ParallaxImageView.ImageCornerClipFlag.TENSION
        cbImageCornerClipBound.isChecked = parallaxImageView.imageCornerClipFlag containsFlag ParallaxImageView.ImageCornerClipFlag.BOUND
        cbImageCornerClipSource.tag = ParallaxImageView.ImageCornerClipFlag.SOURCE
        cbImageCornerClipTension.tag = ParallaxImageView.ImageCornerClipFlag.TENSION
        cbImageCornerClipBound.tag = ParallaxImageView.ImageCornerClipFlag.BOUND

        cbVerticalLoop.setOnCheckedChangeListener(this)
        cbHorizontalLoop.setOnCheckedChangeListener(this)
        cbParallaxDirectionHorizontal.setOnCheckedChangeListener(this)
        cbParallaxDirectionVertical.setOnCheckedChangeListener(this)
        cbForceParallaxByScale.setOnCheckedChangeListener(this)
        cbInverseParallaxDirections.setOnCheckedChangeListener(this)
        cbSwapParallaxDirections.setOnCheckedChangeListener(this)
        cbForceRecalculations.setOnCheckedChangeListener(this)
        cbPreventViewCornerOverlap.setOnCheckedChangeListener(this)
        cbAllowViewCornerWhRatioChange.setOnCheckedChangeListener(this)
        cbForceViewCornerWhEqual.setOnCheckedChangeListener(this)
        cbPreventImageCornerOverlap.setOnCheckedChangeListener(this)
        cbAllowImageCornerWhRatioChange.setOnCheckedChangeListener(this)
        cbForceImageCornerWhEqual.setOnCheckedChangeListener(this)
        cbImageCornerClipSource.setOnCheckedChangeListener(this)
        cbImageCornerClipTension.setOnCheckedChangeListener(this)
        cbImageCornerClipBound.setOnCheckedChangeListener(this)

        cbAllowViewCornerWhRatioChange.isChecked = parallaxImageView.isViewCornerWhRatioChangeEnabled
        cbAllowImageCornerWhRatioChange.isChecked = parallaxImageView.isImageCornerWhRatioChangeEnabled

        rgViewCornerType.check(if (parallaxImageView.viewCornerType == ParallaxImageView.CornerType.ROUND)
            R.id.rbViewCornerTypeRound else R.id.rbViewCornerTypeBevel
        )

        rgImageCornerType.check(if (parallaxImageView.imageCornerType == ParallaxImageView.CornerType.ROUND)
            R.id.rbImageCornerTypeRound else R.id.rbImageCornerTypeBevel
        )

        rgViewCornerType.setOnCheckedChangeListener(this)
        rgImageCornerType.setOnCheckedChangeListener(this)

        validateForceParallaxByScale()

        btnPivLining.setOnTouchListener(object : View.OnTouchListener {
            private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    lastLoadedCustomUrl = null
                    buildCustomUrlAndInvalidate()
                    if (++currentImageIndex > imagesList.lastIndex) currentImageIndex = 0
                    parallaxImageView.setImageResource(imagesList[currentImageIndex])
                    return true
                }

                override fun onDown(e: MotionEvent?): Boolean {
                    return true
                }

                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    if (++currentBgImageIndex > backgroundImagesList.lastIndex) currentBgImageIndex = 0
                    parallaxImageView.setBackgroundResource(backgroundImagesList[currentBgImageIndex])
                    return true
                }

                override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                    if (e1 == null || e2 == null) return false
                    if (isHorizontalFractionAutoPlay) cbHorizontalLoop.isChecked = false
                    if (isVerticalFractionAutoPlay) cbVerticalLoop.isChecked = false
                    sbVerticalFraction.progress += (distanceY / maxViewHeight.toFloat() * 100.0F).toInt()
                    sbHorizontalFraction.progress += (distanceX / maxViewWidth.toFloat() * 100.0F).toInt()
                    return true
                }

                override fun onLongPress(e: MotionEvent?) {
                    showScaleTypePickDialog()
                }
            }

            private val gestureDetector = GestureDetector(this@WidgetFragment.activity, gestureListener)

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return gestureDetector.onTouchEvent(event)
            }
        })

        // tutorial long click dialogs
        llCustomImageContainer.setOnLongClickListener(this)
        llParallaxDirectionContainer.setOnLongClickListener(this)
        llParallaxHorizontalFractionContainer.setOnLongClickListener(this)
        llParallaxVerticalFractionContainer.setOnLongClickListener(this)
        llParallaxTensionFractionContainer.setOnLongClickListener(this)
        llParallaxAbsoluteTensionContainer.setOnLongClickListener(this)
        llParallaxInterpolatorContainer.setOnLongClickListener(this)
        cbForceParallaxByScale.setOnLongClickListener(this)
        cbInverseParallaxDirections.setOnLongClickListener(this)
        cbSwapParallaxDirections.setOnLongClickListener(this)
        cbForceRecalculations.setOnLongClickListener(this)
        llViewCornerTypeContainer.setOnLongClickListener(this)
        llViewCornerSizeContainer.setOnLongClickListener(this)
        cbPreventViewCornerOverlap.setOnLongClickListener(this)
        cbAllowViewCornerWhRatioChange.setOnLongClickListener(this)
        cbForceViewCornerWhEqual.setOnLongClickListener(this)
        llImageCornerClipFlagContainer.setOnLongClickListener(this)
        llImageCornerClipTypeContainer.setOnLongClickListener(this)
        llImageCornerSizeContainer.setOnLongClickListener(this)
        cbPreventImageCornerOverlap.setOnLongClickListener(this)
        cbAllowImageCornerWhRatioChange.setOnLongClickListener(this)
        cbForceImageCornerWhEqual.setOnLongClickListener(this)
        llBackgroundRoundCornerContainer.setOnLongClickListener(this)
    }

    private fun setImageDimensionRelatedParams() {
        sbPivWidth.max = maxViewWidth - minViewSize
        sbPivWidth.setOnSeekBarChangeListener(this)
        sbPivWidth.progress = sbPivWidth.max

        sbPivHeight.max = maxViewHeight - minViewSize
        sbPivHeight.setOnSeekBarChangeListener(this)
        sbPivHeight.progress = sbPivHeight.max

        sbPivPadding.max = maxViewPadding
        sbPivPadding.setOnSeekBarChangeListener(this)
        sbPivPadding.progress = 0

        sbViewCornerSize.max = maxViewDimension
        sbViewCornerSize.setOnSeekBarChangeListener(this)
        sbViewCornerSize.progress = parallaxImageView.viewCornerSize.toInt()

        sbImageCornerSize.max = maxViewDimension
        sbImageCornerSize.setOnSeekBarChangeListener(this)
        sbImageCornerSize.progress = parallaxImageView.imageCornerSize.toInt()

        sbBackgroundCornerRadius.max = maxViewDimension
        sbBackgroundCornerRadius.setOnSeekBarChangeListener(this)
        sbBackgroundCornerRadius.progress = parallaxImageView.backgroundCornerRadius.toInt()

        etxtImageNumber.setText(customUrlNumber.toString())
        etxtImageWidth.setText(minViewSize.toString())
        etxtImageHeight.setText(minViewSize.toString())
        buildCustomUrlAndInvalidate()

        etxtViewWidth.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isViewWidthChangeFromProgress) return
                val widthPx = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtViewWidth.setText("0")
                    return
                }

                val clampedWidthPx = when {
                    widthPx > maxViewWidth -> {
                        showToast("view width was clamped to max value ($maxViewWidth)")
                        maxViewWidth
                    }
                    widthPx < minViewSize -> {
                        showToast("view width was clamped to min value ($minViewSize)")
                        minViewSize
                    }
                    else -> {
                        showToast()
                        widthPx
                    }
                }

                isViewWidthProgressChangeFromText = true
                sbPivWidth.progress = clampedWidthPx - minViewSize
                isViewWidthProgressChangeFromText = false

            }
        })
        etxtViewHeight.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isViewHeightChangeFromProgress) return
                val heightPx = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtViewHeight.setText("0")
                    return
                }

                val clampedHeightPx = when {
                    heightPx > maxViewHeight -> {
                        showToast("view height was clamped to max value ($maxViewHeight)")
                        maxViewHeight
                    }
                    heightPx < minViewSize -> {
                        showToast("view height was clamped to min value ($minViewSize)")
                        minViewSize
                    }
                    else -> {
                        showToast()
                        heightPx
                    }
                }

                isViewHeightProgressChangeFromText = true
                sbPivHeight.progress = clampedHeightPx - minViewSize
                isViewHeightProgressChangeFromText = false
            }
        })
        etxtViewPadding.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isViewPaddingChangeFromProgress) return
                val paddingPx = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtViewPadding.setText("0")
                    return
                }

                val clampedViewPaddingPx = when {
                    paddingPx > maxViewPadding -> {
                        showToast("view padding was clamped to max value ($maxViewPadding)")
                        maxViewPadding
                    }
                    paddingPx < 0 -> {
                        showToast("view padding was clamped to min value (0)")
                        0
                    }
                    else -> {
                        showToast()
                        paddingPx
                    }
                }

                isViewPaddingChangeFromText = true
                sbPivPadding.progress = clampedViewPaddingPx
                isViewPaddingChangeFromText = false
            }
        })
        etxtViewCornerSize.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isViewCornerChangeFromProgress) return
                val cornerSizePx = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtViewCornerSize.setText("0")
                    return
                }

                val clampedCornerSizePx = when {
                    cornerSizePx > maxViewDimension -> {
                        showToast("view corner was clamped to max value ($maxViewDimension)")
                        maxViewDimension
                    }
                    cornerSizePx < 0 -> {
                        showToast("view corner was clamped to min value (0)")
                        0
                    }
                    else -> {
                        showToast()
                        cornerSizePx
                    }
                }

                isViewCornerChangeFromText = true
                sbViewCornerSize.progress = clampedCornerSizePx
                isViewCornerChangeFromText = false
            }
        })
        etxtImageCornerSize.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isImageCornerChangeFromProgress) return
                val cornerSizePx = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtImageCornerSize.setText("0")
                    return
                }

                val clampedCornerSizePx = when {
                    cornerSizePx > maxViewDimension -> {
                        showToast("image corner was clamped to max value ($maxViewDimension)")
                        maxViewDimension
                    }
                    cornerSizePx < 0 -> {
                        showToast("image corner was clamped to min value (0)")
                        0
                    }
                    else -> {
                        showToast()
                        cornerSizePx
                    }
                }

                isImageCornerChangeFromText = true
                sbImageCornerSize.progress = clampedCornerSizePx
                isImageCornerChangeFromText = false
            }
        })
        etxtBackgroundCornerRadius.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                if (isBackgroundRadiusChangeFromProgress) return
                val cornerRadiusPx = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    etxtBackgroundCornerRadius.setText("0")
                    return
                }

                val clampedCornerRadiusPx = when {
                    cornerRadiusPx > maxViewDimension -> {
                        showToast("background corner radius was clamped to max value ($maxViewDimension)")
                        maxViewDimension
                    }
                    cornerRadiusPx < 0 -> {
                        showToast("background corner radius was clamped to min value (0)")
                        0
                    }
                    else -> {
                        showToast()
                        cornerRadiusPx
                    }
                }

                isBackgroundRadiusChangeFromText = true
                sbBackgroundCornerRadius.progress = clampedCornerRadiusPx
                isBackgroundRadiusChangeFromText = false
            }
        })
        etxtImageWidth.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                try {
                    val imageWidthPx = s.toString().toInt()
                    customUrlWidth = when {
                        imageWidthPx > maxImageDimension -> {
                            showToast("image width was clamped to max value ($maxImageDimension)")
                            maxImageDimension
                        }
                        imageWidthPx < minViewSize -> {
                            showToast("image width was clamped to min value ($minViewSize)")
                            minViewSize
                        }
                        else -> {
                            showToast()
                            imageWidthPx
                        }
                    }
                } catch (e: Exception) {
                    customUrlWidth = minViewSize
                    showToast("image width was clamped to min value ($minViewSize)")
                }

                buildCustomUrlAndInvalidate()
            }
        })
        etxtImageHeight.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                try {
                    val imageHeightPx = s.toString().toInt()
                    customUrlHeight = when {
                        imageHeightPx > maxImageDimension -> {
                            showToast("image height was clamped to max value ($maxImageDimension)")
                            maxImageDimension
                        }
                        imageHeightPx < minViewSize -> {
                            showToast("image height was clamped to min value ($minViewSize)")
                            minViewSize
                        }
                        else -> {
                            showToast()
                            imageHeightPx
                        }
                    }
                } catch (e: Exception) {
                    customUrlHeight = minViewSize
                    showToast("image height was clamped to min value ($minViewSize)")
                }

                buildCustomUrlAndInvalidate()
            }
        })
        etxtImageNumber.addTextChangedListener(object : TextWatcherAdapter {
            override fun afterTextChanged(s: Editable?) {
                val imageNumber = try {
                    s.toString().toInt()
                } catch (e: Exception) {
                    customUrlNumber = 0
                    showToast("image number was clamped to min number (0)")
                    buildCustomUrlAndInvalidate()
                    return
                }

                customUrlNumber = when {
                    imageNumber > 1084 -> {
                        showToast("image number was clamped to max number (1084)")
                        maxImageDimension
                    }
                    imageNumber < 0 -> {
                        showToast("image number was clamped to min number (0)")
                        0
                    }
                    else -> {
                        showToast()
                        imageNumber
                    }
                }

                buildCustomUrlAndInvalidate()
            }
        })

        cbBindWidthHeight.setOnCheckedChangeListener(this)
    }

    private fun buildCustomUrlAndInvalidate(): Boolean {
        lastCustomUrl = "https://picsum.photos" +
                (if (isCustomUrlGrayscale) "/g" else "") +
                (if (customUrlWidth == 0 && customUrlHeight == 0) "" else "/$customUrlWidth/$customUrlHeight") +
                "?image=$customUrlNumber" +
                if (isCustomUrlBlur) "&blur" else ""
        return if (TextUtils.equals(lastCustomUrl, lastLoadedCustomUrl)) {
            btnCustomImageApply.setText(if (isSuccessfullyLoaded) R.string.piv_loaded else R.string.piv_apply)
            false
        } else {
            btnCustomImageApply.setText(R.string.piv_apply)
            true
        }
    }

    private fun applyCustomUrl() {
        if (TextUtils.isEmpty(lastCustomUrl) || TextUtils.equals(lastLoadedCustomUrl, lastCustomUrl)) return
        val pendingLoadUrl = lastCustomUrl
        Log.d("TAG", "pendingLoadUrl = $pendingLoadUrl")
        Glide.with(this)
                .asDrawable()
                .load(lastCustomUrl)
                .into(object:SimpleTarget<Drawable> () {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        showToast("Load Failed. Check Connection")
                        isSuccessfullyLoaded = false
                    }

                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        isSuccessfullyLoaded = true
                        lastLoadedCustomUrl = pendingLoadUrl
                        btnCustomImageApply.setText(R.string.piv_loaded)
                        parallaxImageView.setImageDrawable(resource)
                    }
                })
    }

    private fun showInterpolatorPickDialog() {
        val interpolatorPickDialog = AlertDialog.Builder(this@WidgetFragment.activity!!)
        interpolatorPickDialog.setTitle("Select Parallax Interpolator")
        val arrayAdapter = ArrayAdapter<String>(this@WidgetFragment.activity!!, android.R.layout.select_dialog_singlechoice)
        interpolatorMap.entries
                .filter { it.value != currentInterpolator }
                .sortedBy { it.key }
                .map { arrayAdapter.add(it.key) }
        interpolatorPickDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        interpolatorPickDialog.setAdapter(arrayAdapter) { dialog, which ->
            val key = arrayAdapter.getItem(which)
            btnParallaxInterpolator.text = key
            currentInterpolator = interpolatorMap[key]
            parallaxImageView.interpolator = currentInterpolator
            dialog.dismiss()
        }
        interpolatorPickDialog.show()
    }

    private fun showDialog(@StringRes title: Int, @StringRes message: Int) {
        val infoDialog = AlertDialog.Builder(this@WidgetFragment.activity!!)
        infoDialog.setTitle(title)
        infoDialog.setMessage(message)
        infoDialog.setPositiveButton(getString(R.string.piv_ok)) { dialog, _ -> dialog.dismiss() }
        infoDialog.show()
    }

    private fun showToast(message: CharSequence? = null) {
        toast?.cancel()
        toast = Toast.makeText(this@WidgetFragment.activity, message ?: return, Toast.LENGTH_SHORT)
        toast?.show()
    }

    private fun cyclePopupAnimation(view: View, show: Boolean = true) {
        view.animate()
                .scaleX(if (show) 1.0F else 0.8F)
                .scaleY(if (show) 1.0F else 0.8F)
                .alpha(if (show) 1.0F else 0.5F)
                .setDuration(500)
                .withEndAction({ cyclePopupAnimation(view, !show) })
    }

    private fun validateForceParallaxByScale() {
        if ((parallaxImageView.scaleType == ImageView.ScaleType.CENTER_CROP ||
                        parallaxImageView.scaleType == ImageView.ScaleType.FIT_XY)) {
            llForceParallaxByScaleContainer.animate().alpha(1.0F).duration = 300
            cbForceParallaxByScale.isEnabled = true
            txtForceParallaxByScaleScaleType.animate().cancel()
            txtForceParallaxByScaleScaleType.visibility = View.GONE
            parallaxImageView.isForceParallaxByScale = cbForceParallaxByScale.isChecked
        } else {
            llForceParallaxByScaleContainer.animate().alpha(0.3F).duration = 300
            cbForceParallaxByScale.isEnabled = false
            cbForceParallaxByScale.setOnCheckedChangeListener(null)
            cbForceParallaxByScale.isChecked = false
            cbForceParallaxByScale.setOnCheckedChangeListener(this)
            txtForceParallaxByScaleScaleType.visibility = View.VISIBLE
            cyclePopupAnimation(txtForceParallaxByScaleScaleType)
            parallaxImageView.isForceParallaxByScale = false
        }
    }

    private fun validateHorizontalParallaxDirection() {
        if (cbParallaxDirectionHorizontal.isChecked) {
            llParallaxHorizontalFractionContainer.animate().alpha(1.0F).duration = 300
            cbHorizontalLoop.isEnabled = true
            etxtHorizontalFraction.isEnabled = true
            sbHorizontalFraction.isEnabled = true
            imgParallaxHorizontalFractionBlock.animate().cancel()
            imgParallaxHorizontalFractionBlock.visibility = View.GONE
            parallaxImageView.parallaxDirectionFlag.addFlag(ParallaxImageView.ParallaxDirectionFlag.HORIZONTAL)
        } else {
            llParallaxHorizontalFractionContainer.animate().alpha(0.3F).duration = 300
            cbHorizontalLoop.isChecked = false
            cbHorizontalLoop.isEnabled = false
            etxtHorizontalFraction.isEnabled = false
            sbHorizontalFraction.isEnabled = false
            imgParallaxHorizontalFractionBlock.visibility = View.VISIBLE
            cyclePopupAnimation(imgParallaxHorizontalFractionBlock)
            parallaxImageView.parallaxDirectionFlag.removeFlag(ParallaxImageView.ParallaxDirectionFlag.HORIZONTAL)
        }
    }

    private fun validateVerticalParallaxDirection() {
        if (cbParallaxDirectionVertical.isChecked) {
            llParallaxVerticalFractionContainer.animate().alpha(1.0F).duration = 300
            cbVerticalLoop.isEnabled = true
            etxtVerticalFraction.isEnabled = true
            sbVerticalFraction.isEnabled = true
            imgParallaxVerticalFractionBlock.animate().cancel()
            imgParallaxVerticalFractionBlock.visibility = View.GONE
            parallaxImageView.parallaxDirectionFlag.addFlag(ParallaxImageView.ParallaxDirectionFlag.VERTICAL)
        } else {
            llParallaxVerticalFractionContainer.animate().alpha(0.3F).duration = 300
            cbVerticalLoop.isChecked = false
            cbVerticalLoop.isEnabled = false
            etxtVerticalFraction.isEnabled = false
            sbVerticalFraction.isEnabled = false
            imgParallaxVerticalFractionBlock.visibility = View.VISIBLE
            cyclePopupAnimation(imgParallaxVerticalFractionBlock)
            parallaxImageView.parallaxDirectionFlag.removeFlag(ParallaxImageView.ParallaxDirectionFlag.VERTICAL)
        }
    }

    private fun validateViewCornerForceWhEqualityChanged() {
        if (cbForceViewCornerWhEqual.isChecked) {
            llViewCornerControlContainer.animate().alpha(0.3F).duration = 300
            cbPreventViewCornerOverlap.animate().alpha(0.3F).duration = 300
            cbAllowViewCornerWhRatioChange.animate().alpha(0.3F).duration = 300
            sbViewCornerSize.isEnabled = false
            etxtViewCornerSize.isEnabled = false
            cbPreventViewCornerOverlap.isEnabled = false
            cbAllowViewCornerWhRatioChange.isEnabled = false

            txtViewCornerForcedWhEqualOn.visibility = View.VISIBLE
            cyclePopupAnimation(txtViewCornerForcedWhEqualOn)

            parallaxImageView.isForceViewportCornersWhRatioEqual = true
        } else {
            llViewCornerControlContainer.animate().alpha(1.0F).duration = 300
            cbPreventViewCornerOverlap.animate().alpha(1.0F).duration = 300
            cbAllowViewCornerWhRatioChange.animate().alpha(1.0F).duration = 300
            sbViewCornerSize.isEnabled = true
            etxtViewCornerSize.isEnabled = true
            cbPreventViewCornerOverlap.isEnabled = true
            cbAllowViewCornerWhRatioChange.isEnabled = true

            txtViewCornerForcedWhEqualOn.animate().cancel()
            txtViewCornerForcedWhEqualOn.visibility = View.GONE
            parallaxImageView.isForceViewportCornersWhRatioEqual = false
        }
    }

    private fun validateImageCornerForceWhEqualityChanged() {
        if (cbForceImageCornerWhEqual.isChecked) {
            llImageCornerControlContainer.animate().alpha(0.3F).duration = 300
            cbPreventImageCornerOverlap.animate().alpha(0.3F).duration = 300
            cbAllowImageCornerWhRatioChange.animate().alpha(0.3F).duration = 300
            sbImageCornerSize.isEnabled = false
            etxtImageCornerSize.isEnabled = false
            cbPreventImageCornerOverlap.isEnabled = false
            cbAllowImageCornerWhRatioChange.isEnabled = false

            txtImageCornerForcedWhEqualOn.visibility = View.VISIBLE
            cyclePopupAnimation(txtImageCornerForcedWhEqualOn)

            parallaxImageView.isForceImageCornersWhRatioEqual = true
        } else {
            llImageCornerControlContainer.animate().alpha(1.0F).duration = 300
            cbPreventImageCornerOverlap.animate().alpha(1.0F).duration = 300
            cbAllowImageCornerWhRatioChange.animate().alpha(1.0F).duration = 300
            sbImageCornerSize.isEnabled = true
            etxtImageCornerSize.isEnabled = true
            cbPreventImageCornerOverlap.isEnabled = true
            cbAllowImageCornerWhRatioChange.isEnabled = true

            txtImageCornerForcedWhEqualOn.animate().cancel()
            txtImageCornerForcedWhEqualOn.visibility = View.GONE
            parallaxImageView.isForceImageCornersWhRatioEqual = false
        }
    }

    private fun validateImageCornerClipFlags() {
        if (parallaxImageView.imageCornerClipFlag == ParallaxImageView.ImageCornerClipFlag.NONE) {
            viewImageCornerDisabler.visibility = View.VISIBLE
            viewImageCornerDisabler.isClickable = true
            llImageCornerClipControlsContainer.animate().alpha(0.3F).duration = 300
            txtImageCornerNoFlag.visibility = View.VISIBLE
            cyclePopupAnimation(txtImageCornerNoFlag)
        } else {
            llImageCornerClipControlsContainer.animate().alpha(1.0F).duration = 300
            viewImageCornerDisabler.visibility = View.GONE
            viewImageCornerDisabler.isClickable = false
            txtImageCornerNoFlag.animate().cancel()
            txtImageCornerNoFlag.visibility = View.GONE
        }
    }

    private fun showScaleTypePickDialog() {
        val scaleTypeDialog = AlertDialog.Builder(this@WidgetFragment.activity!!)
        scaleTypeDialog.setTitle("Select Scale Type")
        val originalList = mutableListOf<ImageView.ScaleType>()
        val arrayAdapter = ArrayAdapter<String>(this@WidgetFragment.activity, android.R.layout.select_dialog_singlechoice)
        ImageView.ScaleType.values()
                //.filter { it != parallaxImageView.scaleType }
                .sortedBy { it.name }
                .map {
                    originalList.add(it)
                    arrayAdapter.add(
                            it.name
                                    .replace("_", " ")
                                    .replace(
                                            "\\w{3,}".toRegex(),
                                            { matchResult: MatchResult ->
                                                "${matchResult.value[0].toTitleCase()}${
                                                matchResult.value
                                                        .substring(1)
                                                        .toLowerCase()
                                                }"
                                            }
                                    ) + if (it == parallaxImageView.scaleType) " (current)" else ""
                    )
                }
        scaleTypeDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        scaleTypeDialog.setAdapter(arrayAdapter) { dialog, which ->
            val selectedScaleType = originalList[which]
            if (selectedScaleType == parallaxImageView.scaleType) {
                showToast("${parallaxImageView.scaleType} Already Selected")
            } else {
                parallaxImageView.scaleType = originalList[which]
                validateForceParallaxByScale()
                showToast("New Scale Type: ${parallaxImageView.scaleType}")
            }
            dialog.dismiss()
        }
        scaleTypeDialog.show()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnCustomImageApply -> applyCustomUrl()
            R.id.btnParallaxInterpolator -> showInterpolatorPickDialog()
            R.id.btnPivInfo -> showDialog(R.string.piv_info_dialog_title, R.string.piv_info_dialog_message)
        }
    }

    override fun onLongClick(v: View?): Boolean {
        when (v?.id) {
            R.id.llCustomImageContainer ->
                showDialog(R.string.piv_custom_image_url, R.string.piv_custom_image_url_message)
            R.id.llParallaxDirectionContainer ->
                showDialog(R.string.piv_parallax_direction, R.string.piv_parallax_direction_message)
            R.id.llParallaxHorizontalFractionContainer ->
                showDialog(R.string.piv_parallax_horizontal_fraction, R.string.piv_parallax_horizontal_fraction_message)
            R.id.llParallaxVerticalFractionContainer ->
                showDialog(R.string.piv_parallax_vertical_fraction, R.string.piv_parallax_vertical_fraction_message)
            R.id.llParallaxTensionFractionContainer ->
                showDialog(R.string.piv_parallax_tension_fraction, R.string.piv_parallax_tension_fraction_message)
            R.id.llParallaxAbsoluteTensionContainer ->
                showDialog(R.string.piv_absolute_parallax_tension, R.string.parallax_absolute_tension_message)
            R.id.llParallaxInterpolatorContainer ->
                showDialog(R.string.piv_parallax_interpolator, R.string.piv_parallax_interpolator_message)
            R.id.cbForceParallaxByScale ->
                showDialog(R.string.piv_force_parallax_by_scale, R.string.piv_force_parallax_by_scale_message)
            R.id.cbInverseParallaxDirections ->
                showDialog(R.string.piv_inverse_parallax_directions, R.string.piv_inverse_direction_message)
            R.id.cbSwapParallaxDirections ->
                showDialog(R.string.piv_swap_parallax_directions, R.string.piv_swap_parallax_direction_message)
            R.id.cbForceRecalculations ->
                showDialog(R.string.piv_force_recalculations, R.string.piv_force_recalculation_message)
            R.id.llViewCornerTypeContainer ->
                showDialog(R.string.piv_view_corner_type, R.string.piv_view_corner_clip_type_message)
            R.id.llViewCornerSizeContainer ->
                showDialog(R.string.piv_view_corner_size, R.string.piv_view_corner_clip_size_message)
            R.id.cbPreventViewCornerOverlap ->
                showDialog(R.string.piv_prevent_view_corner_overlap, R.string.piv_view_prevent_overlap_message)
            R.id.cbAllowViewCornerWhRatioChange ->
                showDialog(R.string.piv_allow_view_corner_width_height_ratio_change, R.string.piv_view_allow_wh_change_message)
            R.id.cbForceViewCornerWhEqual ->
                showDialog(R.string.piv_force_view_corner_width_height_equal, R.string.piv_view_force_wh_equal_message)
            R.id.llImageCornerClipFlagContainer ->
                showDialog(R.string.piv_image_corner_clip_flag, R.string.piv_image_corner_clip_flag_message)
            R.id.llImageCornerClipTypeContainer ->
                showDialog(R.string.piv_image_corner_type, R.string.piv_image_corner_clip_type_message)
            R.id.llImageCornerSizeContainer ->
                showDialog(R.string.piv_image_corner_size, R.string.piv_image_corner_clip_size_message)
            R.id.cbPreventImageCornerOverlap ->
                showDialog(R.string.piv_prevent_image_corner_overlap, R.string.piv_image_prevent_overlap_message)
            R.id.cbAllowImageCornerWhRatioChange ->
                showDialog(R.string.piv_allow_image_image_width_height_ratio_change, R.string.piv_image_allow_wh_change_message)
            R.id.cbForceImageCornerWhEqual ->
                showDialog(R.string.piv_force_image_corner_width_height_equal, R.string.piv_force_image_corner_width_height_equal)
            R.id.llBackgroundRoundCornerContainer ->
                showDialog(R.string.piv_background_corner_radius, R.string.piv_background_corner_message)
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        when (seekBar?.id) {
            R.id.sbPivWidth -> {
                val newWidth = progress + minViewSize
                parallaxImageView.layoutParams.width = newWidth
                parallaxImageView.requestLayout()
                isViewWidthChangeFromProgress = true
                if (isWidthHeightBound && !isViewHeightChangeFromProgress) sbPivHeight.progress = progress
                if (!isViewWidthProgressChangeFromText) etxtViewWidth.setText(newWidth.toString())
                isViewWidthChangeFromProgress = false
            }
            R.id.sbPivHeight -> {
                val newHeight = progress + minViewSize
                parallaxImageView.layoutParams.height = newHeight
                parallaxImageView.requestLayout()
                isViewHeightChangeFromProgress = true
                if (isWidthHeightBound && !isViewWidthChangeFromProgress) sbPivWidth.progress = progress
                if (!isViewHeightProgressChangeFromText) etxtViewHeight.setText(newHeight.toString())
                isViewHeightChangeFromProgress = false
            }
            R.id.sbPivPadding -> {
                parallaxImageView.setPadding(progress, progress, progress, progress)
                isViewPaddingChangeFromProgress = true
                if (!isViewPaddingChangeFromText) etxtViewPadding.setText(progress.toString())
                isViewPaddingChangeFromProgress = false
            }
            R.id.sbViewCornerSize -> {
                parallaxImageView.viewCornerSize = progress.toFloat()
                isViewCornerChangeFromProgress = true
                if (!isViewCornerChangeFromText) etxtViewCornerSize.setText(progress.toString())
                isViewCornerChangeFromProgress = false
            }
            R.id.sbImageCornerSize -> {
                parallaxImageView.imageCornerSize = progress.toFloat()
                isImageCornerChangeFromProgress = true
                if (!isImageCornerChangeFromText) etxtImageCornerSize.setText(progress.toString())
                isImageCornerChangeFromProgress = false
            }
            R.id.sbBackgroundCornerRadius -> {
                parallaxImageView.backgroundCornerRadius = progress.toFloat()
                isBackgroundRadiusChangeFromProgress = true
                if (!isBackgroundRadiusChangeFromText) etxtBackgroundCornerRadius.setText(progress.toString())
                isBackgroundRadiusChangeFromProgress = false
            }
            R.id.sbVerticalFraction -> {
                if (fromUser && isVerticalFractionAutoPlay) {
                    cbVerticalLoop.isChecked = false
                    return
                }
                val fraction = progress / 100.0F
                parallaxImageView.parallaxVerticalFraction = fraction
                txtVerticalFraction.text = "(${FORMAT_FRACTION.format(fraction)})"
                if (isVerticalFractionChangeFromText) return
                isVerticalFractionChangeFromProgress = true
                etxtVerticalFraction.setText(progress.toString())
                isVerticalFractionChangeFromProgress = false
            }
            R.id.sbHorizontalFraction -> {
                if (fromUser && isHorizontalFractionAutoPlay) {
                    cbHorizontalLoop.isChecked = false
                    return
                }
                val fraction = progress / 100.0F
                parallaxImageView.parallaxHorizontalFraction = fraction
                txtHorizontalFraction.text = "(${FORMAT_FRACTION.format(fraction)})"
                if (isHorizontalFractionChangeFromText) return
                isHorizontalFractionChangeFromProgress = true
                etxtHorizontalFraction.setText(progress.toString())
                isHorizontalFractionChangeFromProgress = false
            }
            R.id.sbParallaxTensionFraction -> {
                val fraction = progress / 100.0F
                parallaxImageView.parallaxTension = fraction
                txtParallaxTensionFraction.text = "(${FORMAT_FRACTION.format(fraction)})"
                if (isParallaxTensionFractionChangeFromText) return
                isParallaxTensionFractionChangeFromProgress = true
                etxtParallaxTensionFraction.setText(progress.toString())
                isParallaxTensionFractionChangeFromProgress = false
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        //stub
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        //stub
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.cbCustomImageBlur -> {
                isCustomUrlBlur = isChecked
                buildCustomUrlAndInvalidate()
            }
            R.id.cbCustomImageGrayscale -> {
                isCustomUrlGrayscale = isChecked
                buildCustomUrlAndInvalidate()
            }
            R.id.cbBindWidthHeight -> {
                if (isChecked) {
                    val currentMinViewProgress = Math.min(sbPivHeight.progress, sbPivWidth.progress)
                    val minMaxProgressValue = Math.min(sbPivHeight.max, sbPivWidth.max)
                    sbPivHeight.max = minMaxProgressValue
                    sbPivWidth.max = minMaxProgressValue
                    sbPivHeight.progress = currentMinViewProgress
                    sbPivWidth.progress = currentMinViewProgress
                } else {
                    sbPivHeight.max = maxViewHeight - minViewSize
                    sbPivWidth.max = maxViewWidth - minViewSize
                }
                isWidthHeightBound = isChecked
            }
            R.id.cbParallaxDirectionHorizontal -> validateHorizontalParallaxDirection()
            R.id.cbParallaxDirectionVertical -> validateVerticalParallaxDirection()
            R.id.cbVerticalLoop -> {
                isVerticalFractionAutoPlay = isChecked
                etxtVerticalFraction.isEnabled = !isChecked
                if (isVerticalFractionAutoPlay) {
                    verticalPlayHandler.post(playVerticalFractionRunnable)
                } else {
                    verticalPlayHandler.removeCallbacks(playVerticalFractionRunnable)
                }
            }
            R.id.cbHorizontalLoop -> {
                isHorizontalFractionAutoPlay = isChecked
                etxtHorizontalFraction.isEnabled = !isChecked
                if (isHorizontalFractionAutoPlay) {
                    horizontalPlayHandler.post(playHorizontalFractionRunnable)
                } else {
                    horizontalPlayHandler.removeCallbacks(playHorizontalFractionRunnable)
                }
            }
            R.id.cbForceParallaxByScale -> validateForceParallaxByScale()
            R.id.cbInverseParallaxDirections -> parallaxImageView.isParallaxInverseDirection = isChecked
            R.id.cbSwapParallaxDirections -> parallaxImageView.isSwapParallaxDirections = isChecked
            R.id.cbForceRecalculations -> parallaxImageView.isForceRecalculationEnabled = isChecked
            R.id.cbPreventViewCornerOverlap -> {
                parallaxImageView.isPreventViewCornerOverlap = isChecked
                cbAllowViewCornerWhRatioChange.isEnabled = isChecked
                if (!isChecked && cbAllowViewCornerWhRatioChange.isChecked)
                    cbAllowViewCornerWhRatioChange.isChecked = false
            }
            R.id.cbAllowViewCornerWhRatioChange -> parallaxImageView.isViewCornerWhRatioChangeEnabled = isChecked
            R.id.cbForceViewCornerWhEqual -> validateViewCornerForceWhEqualityChanged()
            R.id.cbPreventImageCornerOverlap -> {
                parallaxImageView.isPreventImageCornerOverlap = isChecked
                cbAllowImageCornerWhRatioChange.isEnabled = isChecked
                if (!isChecked && cbAllowImageCornerWhRatioChange.isChecked)
                    cbAllowImageCornerWhRatioChange.isChecked = false
            }
            R.id.cbAllowImageCornerWhRatioChange -> parallaxImageView.isImageCornerWhRatioChangeEnabled = isChecked
            R.id.cbForceImageCornerWhEqual -> validateImageCornerForceWhEqualityChanged()
            R.id.cbImageCornerClipSource,
            R.id.cbImageCornerClipTension,
            R.id.cbImageCornerClipBound -> {
                parallaxImageView.imageCornerClipFlag =
                        parallaxImageView.imageCornerClipFlag toggleFlag (buttonView.tag as Int)
                validateImageCornerClipFlags()
            }
        }
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (group?.id) {
            R.id.rgViewCornerType -> {
                when (checkedId) {
                    R.id.rbViewCornerTypeRound -> parallaxImageView.viewCornerType = ParallaxImageView.CornerType.ROUND
                    R.id.rbViewCornerTypeBevel -> parallaxImageView.viewCornerType = ParallaxImageView.CornerType.BEVEL
                }
            }
            R.id.rgImageCornerType -> {
                when (checkedId) {
                    R.id.rbImageCornerTypeRound -> parallaxImageView.imageCornerType = ParallaxImageView.CornerType.ROUND
                    R.id.rbImageCornerTypeBevel -> parallaxImageView.imageCornerType = ParallaxImageView.CornerType.BEVEL
                }
            }
        }
    }

}
