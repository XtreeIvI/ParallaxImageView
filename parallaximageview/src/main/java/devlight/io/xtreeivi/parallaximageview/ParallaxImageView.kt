package devlight.io.xtreeivi.parallaximageview

import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ImageView.ScaleType.*
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView.CornerType.ROUND
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView.ImageCornerClipFlag.BOUND
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView.ImageCornerClipFlag.SOURCE
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView.ImageCornerClipFlag.TENSION
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView.ParallaxDirectionFlag.HORIZONTAL
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView.ParallaxDirectionFlag.NONE
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView.ParallaxDirectionFlag.VERTICAL
import devlight.io.xtreeivi.parallaximageview.extension.*

/**
 * Creator: XtreeIvI (Mikola Melnik), xtreeivi@gmail.com
 *
 *  ParallaxImageView designed to easily achieve parallax effect for image inside view.
 */
class ParallaxImageView : ImageView {

    private val centerDefault = 0.5F
    private val range = 0.0F..1.0F

    /**
     * Rect used as holder when calculating parallax relative to view passed
     * @see [updateParallax]
     */
    private val tempRect = Rect()

    /**
     * Canvas coordinates before translation
     */
    private val canvasRectF = RectF()

    /**
     * Image bounds (adjusted by scale type)
     */
    private val imageModifiedRectF = RectF()

    /**
     * Hold coordinates of current viewport, e.i. where image should be placed
     */
    private val imageTensionRectF = RectF()

    /**
     * Hold coordinates of minimal tension bounds.
     * For instance, if image is bigger that view, min tension bounds are the same as view bounds with padding.
     * Otherwise, min bounds repeat initial position of image inside view.
     */
    private val imageMinTensionRectF = RectF()

    /**
     * Hold coordinates of minimal tension bounds.
     * For instance, if image is bigger that view, max tension bounds are the same as image bounds.
     * Otherwise, max bounds repeat initial view bounds with padding.
     */
    private val imageMaxTensionRectF = RectF()

    /**
     * Defines max image/view rectangle, that is used to simplify clip image logic,
     * because we intersect other rectangles with this, resulting in rect on what we apply clipping logic and clip
     */
    private val imageMaxBoundRectF = RectF()

    /**
     * Internal parameter, that defines total horizontal offset (from max tension bound left)
     * image can be translated within to achieve its tension bounds
     */
    private var parallaxLeftTotalOffset = 0.0F

    /**
     * Internal parameter, that defines total vertical offset (from max tension bound top)
     * image can be translated within to achieve its tension bounds
     */
    private var parallaxTopTotalOffset = 0.0F

    /**
     * Internal parameter, positive value means image height are bigger that view height (with padding),
     * otherwise - negative.
     */
    private var parallaxHorizontalImageModifier = 0.0F

    /**
     * Internal parameter, positive value means image width are bigger that view width (with padding),
     * otherwise - negative.
     */
    private var parallaxVerticalImageModifier = 0.0F

    /**
     * Hold directions parallax allowed within.
     * For instance if image height (adjusted by scale type) is bigger than view height, but [parallaxDirectionFlag]
     * does not contain [VERTICAL], image will be placed by default to its scale type,
     * even when you explicitly pass different [parallaxVerticalFraction].
     *
     * Note that if image dimension (adjusted by scale type) is the same as respective view dimension,
     * no parallax can be achieved, despite all flag are properly set.
     *
     * However [isForceParallaxByScale] could be user for such purposes.
     *
     * Default value = [VERTICAL]|[HORIZONTAL]
     */
    var parallaxDirectionFlag = VERTICAL addFlag HORIZONTAL
        set(value) {
            if (field == value) return
            field = value
            allowHorizontalScroll = value containsFlag HORIZONTAL
            allowVerticalScroll = value containsFlag VERTICAL
            invalidateDirty()
        }

    private var backedAllowHorizontalScroll = parallaxDirectionFlag containsFlag HORIZONTAL
    private var allowHorizontalScroll = parallaxDirectionFlag containsFlag HORIZONTAL
        set(value) {
            if (field == value) return
            field = value
            backedAllowHorizontalScroll = field
        }
        get() = if (isSwapParallaxDirections) backedAllowVerticalScroll else field

    private var backedAllowVerticalScroll = parallaxDirectionFlag containsFlag VERTICAL
    private var allowVerticalScroll = parallaxDirectionFlag containsFlag VERTICAL
        set(value) {
            if (field == value) return
            field = value
            backedAllowVerticalScroll = field
        }
        get() = if (isSwapParallaxDirections) backedAllowHorizontalScroll else field

    private var backedPvf = centerDefault
    private var oldVerticalFraction:Float = centerDefault
    /**
     * Defines vertical relative offset of parallax.
     * Can be changed within 0.0F..1.0F range, and is clamped automatically to its marginal values if exceed.
     * If [parallaxDirectionFlag] does not contain [VERTICAL] always return its default value.
     * Also return proper values taking into consideration parameters,
     * such as [isSwapParallaxDirections] and [isParallaxInverseDirection]
     *
     * Default value = 0.5F
     */
    var parallaxVerticalFraction = centerDefault
        set(value) {
            backedPvf = interpolator?.getInterpolation(value) ?: value
            if (backedPvf > 1.0F) backedPvf = 1.0F
            else if (backedPvf < 0.0F) backedPvf = 0.0F
            if (field == backedPvf) return
            oldVerticalFraction = field
            field = backedPvf
            onParallaxFractionChangeListener?.onParallaxFractionChanged(
                    parallaxVerticalFraction,
                    parallaxHorizontalFraction,
                    oldVerticalFraction,
                    oldHorizontalFraction
            )
            invalidate()
        }
        get() = when {
            !allowVerticalScroll -> centerDefault
            isParallaxInverseDirection -> 1.0F - if (isSwapParallaxDirections) backedPhf else field
            else -> if (isSwapParallaxDirections) backedPhf else field
        }

    private var backedPhf = centerDefault
    private var oldHorizontalFraction:Float = centerDefault

    /**
     * Defines horizontal relative offset of parallax.
     * Can be changed within 0.0F..1.0F range, and is clamped automatically to its marginal values if exceed.
     * If [parallaxDirectionFlag] does not contain [HORIZONTAL] always return its default value.
     * Also return proper values taking into consideration parameters,
     * such as [isSwapParallaxDirections] and [isParallaxInverseDirection]
     *
     * Default value = 0.5F
     */
    var parallaxHorizontalFraction = centerDefault
        set(value) {
            backedPhf = interpolator?.getInterpolation(value) ?: value
            if (backedPhf > 1.0F) backedPhf = 1.0F
            else if (backedPhf < 0.0F) backedPhf = 0.0F

            if (field == backedPhf) return
            oldHorizontalFraction = field
            field = backedPhf
            onParallaxFractionChangeListener?.onParallaxFractionChanged(
                    parallaxVerticalFraction,
                    parallaxHorizontalFraction,
                    oldVerticalFraction,
                    oldHorizontalFraction
            )
            invalidate()
        }
        get() = when {
            !allowHorizontalScroll -> centerDefault
            isParallaxInverseDirection -> 1.0F - if (isSwapParallaxDirections) backedPvf else field
            else -> if (isSwapParallaxDirections) backedPvf else field
        }

    /**
     * [TimeInterpolator] ancestor, that can change fraction dispersion and thus visual effect of parallax
     * Note, that returned by interpolator values must not exceed 0.0F..1.0F range.
     * In such case value is clamped to its margin.
     * Thereby interpolators, such as Overshoot or Anticipate, could cause unpredicted and sharp result.
     *
     * Default interpolator = [AccelerateDecelerateInterpolator]
     */
    var interpolator: TimeInterpolator? = AccelerateDecelerateInterpolator()
        set(value) {
            field = value
            parallaxHorizontalFraction = parallaxHorizontalFraction
            parallaxVerticalFraction = parallaxVerticalFraction
            // no need to invalidate here
        }

    /**
     * This parameter works only with two scale types: [CENTER_CROP] and [FIT_XY] and allow only outer parallax.
     * By default if [parallaxTension] == 1.0F scale image (if needed) to be equal as 2x view dimension.
     * Note that scaling takes into consideration [parallaxDirectionFlag] flags.
     *
     * For instance, scale type = [CENTER_CROP] modified image is twice as wide as view,
     * image height == view height, [parallaxTension] = 1.0F
     * [parallaxDirectionFlag] = [HORIZONTAL].
     * In such case no scaling were made, because originally our view is 2x view width.
     *
     * 2. In case [parallaxDirectionFlag] were [VERTICAL] (from previous example), whole image would be scaled by 2x,
     * preserving its aspect ratio.
     *
     * 3. In case scale type = [CENTER_CROP] modified image width = 1.5 view width,
     * image height == view height, [parallaxTension] = 1.0F, [parallaxDirectionFlag] = [HORIZONTAL].
     * In this scenario image would to be scaled by only 1.5F, because initially image is wider.
     *
     * If [parallaxAbsoluteTension] != 0 and takes precedence over [parallaxTension] two next scenarios are possible:
     * - [parallaxAbsoluteTension] > 0.0F - scale image (if needed), so that scaled image exceeds view bounds by
     * specified value from each side.
     * Note that if resulting scale bigger that 2x view dimension it will be clamped to latter.
     * - [parallaxAbsoluteTension] < 0.0F - virtually apply max scale (2x view dimension) and subtract specified value.
     *
     * Default value = false
     */
    var isForceParallaxByScale = false
        get() = field && (scaleType == CENTER_CROP || scaleType == FIT_XY)
        set(value) {
            if (field == value) return
            field = value
            invalidateDirty()
        }

    /**
     * Define max parallax tension in px.
     * There is positive and negative values possible for this parameter
     * Value != 0.0F overrides [parallaxTension] default behavior.
     *
     * In order to [parallaxTension] takes precedence over this parameter set its value == 0.0F
     *
     * Instead of using respective side difference (restricted by relative [parallaxTension] percentage)
     * this parameter uses its absolute value.
     * For instance, if our difference 100px,
     * our [parallaxTension] = 0.5F (thus tension bounds 50px),
     * and our [parallaxAbsoluteTension] = 20px. In such case 20px is gonna ge used as tension bounds.
     *
     * However if our side difference less that [parallaxAbsoluteTension] value, than lower value is to be used.
     * For instance, if our difference 16px, our [parallaxTension] = 0.25F (thus tension bounds 4px),
     * and our [parallaxAbsoluteTension] = 20px. In such case 16px is gonna ge used as tension bounds,
     * because [parallaxAbsoluteTension] overrides [parallaxTension] but clamped to min value by difference.
     *
     * Negative value of this parameter inverse logic.
     * For instance, if difference 100px, [parallaxTension] = 0.25F (thus tension bounds 25px),
     * and [parallaxAbsoluteTension] = -20px. In such case 80px is gonna ge used as tension bounds,
     * because [parallaxAbsoluteTension] overrides [parallaxTension].
     * If in this same case [parallaxAbsoluteTension] = -110px - tension bounds would be 0.0F,
     * because value is clamped between min and max (in this example 0.0F and 100.0F respectively)
     *
     * @see [isForceParallaxByScale] for other usages
     *
     * Default value = 0.0F
     */
    var parallaxAbsoluteTension = 0.0F
        set(value) {
            if (field == value) return
            field = value
            invalidateDirty()
        }


    /**
     * By default parallax tension allows parallax effect for image in case view and image src aspect ratios differs.
     * Min value (0.0F) stands for no parallax effect.
     * Max value works differently for different scale type and also differs from image to view sizes.
     * Parallax use difference from image side (adjusted by scale type) and view respective side.
     * Tension itself can decrease this value (e.i. [parallaxTension] == 0.5F - half difference will be used for parallax)
     *
     * If aforementioned difference > 0 and parallax in that direction allowed (@see [ParallaxDirectionFlag])
     * that parallax is possible.
     *
     * In case image side are lower that view respective side (e.i. small image with [CENTER_INSIDE] scale type)
     * parallax will occur inside view bounds
     *
     * @see [isForceParallaxByScale] for other usages
     *
     * Default behavior of this parameter could be overridden by [parallaxAbsoluteTension]
     *
     * Default value = 1.0F
     */
    var parallaxTension = 1.0F
        set(value) {
            val newValue = when {

                value in range -> value
                value < 0.0F -> 0.0F
                else -> 1.0F
            }
            if (field == newValue) return
            field = newValue
            parallaxTensionScale = 1.0F + field
            invalidateDirty()
        }
    private var parallaxTensionScale = 1.0F + parallaxTension
    private var parallaxForcedScale = 1.0F

    private val imageCornerClipRectF = RectF()
    private val imageCornerClipPath = Path()

    /**
     * Defines image corner size in px, that will be clipped.
     * Note that only image itself is clipped, allowing you treat background differently.
     * If [imageCornerType] = [CornerType.ROUND], specifies image corner radius,
     * if [imageCornerType] = [CornerType.BEVEL], specifies size from corner between which bevel is clipped.
     *
     * Default value = 0.0F
     */
    var imageCornerSize: Float = 0.0F
        set(value) {
            val newValue = when {
                value < 0.0F -> 0.0F
                else -> value
            }
            if (field == newValue) return
            field = newValue
            invalidateDirty()
        }

    /**
     * Define image corner clip type.
     * @see [CornerType] for details.
     *
     * Default value = [CornerType.ROUND]
     */
    var imageCornerType = CornerType.ROUND
        set(value) {
            if (value == field || (value != CornerType.ROUND && value != CornerType.BEVEL)) return
            field = value
            invalidateDirty()
        }

    /**
     * Define image corner clip flag. In simple words, what bounds should be clipped.
     * Can be used as flags, e.i combine different [ImageCornerClipFlag]
     * @see [ImageCornerClipFlag] for details.
     *
     * Default value = [SOURCE]|[BOUND]
     */
    var imageCornerClipFlag: Int = SOURCE addFlag BOUND
        set(value) {
            if (field == value) return
            field = value
            isClipSourceCorner = value containsFlag SOURCE
            isClipTensionCorner = value containsFlag TENSION
            isClipBoundCorner = value containsFlag BOUND
            invalidateDirty()
        }
    private var isClipSourceCorner = imageCornerClipFlag containsFlag SOURCE
    private var isClipTensionCorner = imageCornerClipFlag containsFlag TENSION
    private var isClipBoundCorner = imageCornerClipFlag containsFlag BOUND

    /**
     * Parameter that can allow/disallow corner overlap.
     *
     * Default value = true
     */
    var isPreventImageCornerOverlap = true
        set(value) {
            if (value == field) return
            field = value
            invalidateDirty()
        }

    /**
     * This parameter work only in conjunction with [isPreventImageCornerOverlap] and when latter value is true.
     * In case wh ratio is 1.0F (image height and width are the same) it takes no effect.
     * Otherwise when [imageCornerSize] is bigger that half of min image side
     * than bigger side will be cut util wh ratio becomes 1.0F.
     * Note that is such case [imageCornerSize] could be max of min image side
     *
     * Default value = false
     */
    var isImageCornerWhRatioChangeEnabled = false
        set(value) {
            if (value == field) return
            field = value
            invalidateDirty()
        }

    /**
     * This parameter takes precedence over [isPreventImageCornerOverlap] parameter
     * and [isImageCornerWhRatioChangeEnabled], making image down crop with specified [imageCornerType] until
     * width/height ratio == 1.0F.
     *
     * Note that clipped width/height does not affect calculated tension bounds. (e.i. clipping only visual)
     *
     * Default value = false
     */
    var isForceImageCornersWhRatioEqual = false
        set(value) {
            if (value == field) return
            field = value
            invalidateDirty()
        }

    private var hasBgToSet = false

    /**
     * Defines background corner radius.
     * However this is optional parameter, and can only be used for simple backgrounds, such as [BitmapDrawable],
     * because it replace its content to [RoundedBitmapDrawable].
     *
     * Suits well for only simple background (color drawable, bitmap drawable and NOT for ripple drawable and such).
     *
     * Default value = 0.0F
     */
    var backgroundCornerRadius: Float = 0.0F
        set(value) {
            val newValue = when {
                value < 0.0F -> 0.0F
                else -> value
            }
            if (field == newValue) return
            field = value
            when (background) {
                null -> return
                is RoundedBitmapDrawable -> (background as? RoundedBitmapDrawable)?.cornerRadius = value
                else -> background = background // call setBackground
            }
            invalidateDirty()
        }

    /**
     * Internal parameter that notify when view corner should be recalculated.
     */
    private var isViewCornerDirty = true
    private val viewCornerClipPath = Path()
    private val viewCornerClipRectF = RectF()

    /**
     * Define view corner clip type.
     * @see [CornerType] for details.
     *
     * Default value = [CornerType.ROUND]
     */
    var viewCornerType = CornerType.ROUND
        set(value) {
            if (value == field || (value != CornerType.ROUND && value != CornerType.BEVEL)) return
            field = value
            isViewCornerDirty = true
            invalidate()
        }

    /**
     * Defines view corner size in px.
     * View corner size clip both background and image
     * If [viewCornerType] = [CornerType.ROUND], specifies view corner radius,
     * if [viewCornerType] = [CornerType.BEVEL], specifies size from corner between which bevel is clipped.
     *
     * Default value = 0.0F
     */
    var viewCornerSize: Float = 0.0F
        set(value) {
            val newValue = when {
                value < 0.0F -> 0.0F
                else -> value
            }
            if (field == newValue) return
            field = newValue
            isViewCornerDirty = true
            invalidate()
        }

    /**
     * Parameter that can allow/disallow corner overlap.
     * Default value = true
     */
    var isPreventViewCornerOverlap = true
        set(value) {
            if (value == field) return
            field = value
            isViewCornerDirty = true
            invalidate()
        }

    /**
     * This parameter work only in conjunction with [isPreventViewCornerOverlap] and when latter value is true.
     * In case wh ratio is 1.0F (view height and width are the same) it takes no effect.
     * Otherwise when [viewCornerSize] is bigger that half of min view side
     * that bigger side will be cut util wh ratio becomes 1.0F.
     * Note that is such case [viewCornerSize] could be max of min view side
     *
     * Default value = false
     */
    var isViewCornerWhRatioChangeEnabled = false
        set(value) {
            if (value == field) return
            field = value
            isViewCornerDirty = true
            invalidate()
        }

    /**
     * This parameter takes precedence over [isPreventViewCornerOverlap] parameter
     * and [isViewCornerWhRatioChangeEnabled], making image down crop with specified [viewCornerType] until
     * width/height ratio == 1.0F.
     *
     * Note that clipped width/height does not change view bounds (e.i. clipping only visual).
     *
     * Default value = false
     */
    var isForceViewportCornersWhRatioEqual = false
        set(value) {
            if (value == field) return
            field = value
            isViewCornerDirty = true
            invalidate()
        }

    /**
     * Reverse direction of parallax.
     * By default, in case of [parallaxDirectionFlag] contains [VERTICAL] flag and [parallaxTension] == 0.0F
     * top of image is visible in the view (in case image dimension bigger than view dimension)
     * or image is at a top of the view (in case image dimension lower than view dimension).
     *
     * If this parameter set to true, reversed behavior replace default behavior.
     * Technically if [isParallaxInverseDirection] set to true reversed [parallaxVerticalFraction] and
     * [parallaxHorizontalFraction] obtained during parallax calculation.
     * (e.i. 0.2F + inverse direction = 0.8F)
     *
     * Default value = false.
     */
    var isParallaxInverseDirection = false
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /**
     * Defined whether parallax direction should be swapped
     * (e.i. setting horizontal parallax will move image vertically).
     * Note that proper original direction that receive fraction must be allowed.
     * For example, if image is to move horizontally when scrolling up/down,
     * then [ParallaxDirectionFlag.VERTICAL] must be present in [parallaxDirectionFlag]
     *
     * Default value = false
     */
    var isSwapParallaxDirections = false
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /**
     * Inner flag that force recalculation for the given params in order for proper positioning
     */
    private var isCalculationDirty = true
        get() = isForceRecalculationEnabled || field

    /**
     * This parameter force to recalculate its params each draw frame.
     * Note that if set to true with conjunction with many clip flags
     * performance could be decreased from 2.5 to 6 times
     *
     * Default value = false
     */
    var isForceRecalculationEnabled = false
        set(value) {
            if (field == value) return
            field = value
            invalidateDirty()
        }

    /**
     * See description [OnParallaxFractionChangeListener]
     */
    var onParallaxFractionChangeListener: OnParallaxFractionChangeListener? = null

    constructor(context: Context?) : super(context) {
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setup(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup(attrs)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setup(attrs)
    }

    private fun setup(attrs: AttributeSet? = null) {
        isSaveEnabled = true

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ParallaxImageView)
            with(typedArray) {
                try {
                    parallaxDirectionFlag = getInt(
                            R.styleable.ParallaxImageView_piv_parallax_direction_flag,
                            parallaxDirectionFlag
                    )

                    parallaxTension = getFloat(
                            R.styleable.ParallaxImageView_piv_parallax_tension,
                            parallaxTension
                    )

                    parallaxAbsoluteTension = getDimension(
                            R.styleable.ParallaxImageView_piv_parallax_absolute_tension,
                            parallaxAbsoluteTension
                    )

                    parallaxVerticalFraction = getFloat(
                            R.styleable.ParallaxImageView_piv_parallax_v_fraction,
                            parallaxVerticalFraction
                    )

                    parallaxHorizontalFraction = getFloat(
                            R.styleable.ParallaxImageView_piv_parallax_h_fraction,
                            parallaxHorizontalFraction
                    )

                    imageCornerType = getInt(
                            R.styleable.ParallaxImageView_piv_image_corner_type,
                            imageCornerType
                    )

                    imageCornerClipFlag = getInt(
                            R.styleable.ParallaxImageView_piv_image_corner_clip_flag,
                            imageCornerClipFlag
                    )

                    imageCornerSize = getDimension(
                            R.styleable.ParallaxImageView_piv_image_corner_size,
                            imageCornerSize
                    )

                    isPreventImageCornerOverlap = getBoolean(
                            R.styleable.ParallaxImageView_piv_prevent_image_corner_overlap,
                            isPreventImageCornerOverlap
                    )

                    isImageCornerWhRatioChangeEnabled = getBoolean(
                            R.styleable.ParallaxImageView_piv_image_corner_wh_ratio_change_enable,
                            isImageCornerWhRatioChangeEnabled
                    )

                    isForceImageCornersWhRatioEqual = getBoolean(
                            R.styleable.ParallaxImageView_piv_image_corner_force_wh_ratio_equal,
                            isForceImageCornersWhRatioEqual
                    )

                    viewCornerType = getInt(
                            R.styleable.ParallaxImageView_piv_view_corner_type,
                            viewCornerType
                    )

                    viewCornerSize = getDimension(
                            R.styleable.ParallaxImageView_piv_view_corner_size,
                            viewCornerSize
                    )

                    isPreventViewCornerOverlap = getBoolean(
                            R.styleable.ParallaxImageView_piv_prevent_view_corner_overlap,
                            isPreventViewCornerOverlap
                    )

                    isViewCornerWhRatioChangeEnabled = getBoolean(
                            R.styleable.ParallaxImageView_piv_view_corner_wh_ratio_change_enable,
                            isViewCornerWhRatioChangeEnabled
                    )

                    isForceViewportCornersWhRatioEqual = getBoolean(
                            R.styleable.ParallaxImageView_piv_viewport_corner_force_wh_ratio_equal,
                            isForceViewportCornersWhRatioEqual
                    )

                    isForceParallaxByScale = getBoolean(
                            R.styleable.ParallaxImageView_piv_force_parallax_by_scale,
                            isForceParallaxByScale
                    )

                    isParallaxInverseDirection = getBoolean(
                            R.styleable.ParallaxImageView_piv_inverse_parallax_directions,
                            isParallaxInverseDirection
                    )

                    isSwapParallaxDirections = getBoolean(
                            R.styleable.ParallaxImageView_piv_swap_parallax_directions,
                            isSwapParallaxDirections
                    )

                    isForceRecalculationEnabled = getBoolean(
                            R.styleable.ParallaxImageView_piv_force_recalculation,
                            isForceRecalculationEnabled
                    )

                    backgroundCornerRadius = getDimension(
                            R.styleable.ParallaxImageView_piv_background_corner_radius,
                            backgroundCornerRadius
                    )

                } finally {
                    recycle()
                }
            }
        }
    }

    /**
     * inner method that force full recalculation cycle to be happen.
     * Takes place if any dependant parameters changed.
     */
    private fun invalidateDirty(dirty: Boolean = true) {
        isCalculationDirty = dirty
        invalidate()
    }

    /**
     * This function update its parallax (if respective direction flags are met)
     * according to parent passed on that moment
     */
    fun updateParallax(viewGroup: ViewGroup) {
        val height = viewGroup.height.toFloat()
        val width = viewGroup.width.toFloat()
        getDrawingRect(tempRect)
        viewGroup.offsetDescendantRectToMyCoords(this, tempRect)
        if (tempRect.bottom >= 0 && tempRect.top <= height)
            parallaxVerticalFraction = tempRect.yByFraction(1.0F - tempRect.bottom / (height + tempRect.absHeight())) / height
        if (tempRect.right >= 0 && tempRect.left <= width)
            parallaxHorizontalFraction = tempRect.xByFraction(1.0F - tempRect.right / (width + tempRect.absWidth())) / width
    }

    /**
     * Overall view clipping (background and image) happens here,
     * taking into consideration all the parameters, defined for view:
     *  - [viewCornerSize]
     *  - [viewCornerType]
     *  - [isPreventViewCornerOverlap]
     *  - [isViewCornerWhRatioChangeEnabled]
     *  - [isForceViewportCornersWhRatioEqual]
     */
    private fun clipViewCornerRadius(canvas: Canvas?) {
        if ((viewCornerSize > 0.0F || isForceViewportCornersWhRatioEqual) && canvas != null) {
            if (isViewCornerDirty) {
                viewCornerClipRectF.set(canvas.clipBounds)
                val cornerSize = when {
                    isForceViewportCornersWhRatioEqual -> viewCornerClipRectF.makeSquareAndReturnSize() / 2.0F
                    isPreventViewCornerOverlap -> {
                        val clampedCornerSize = Math.min(viewCornerClipRectF.minSideSize() / 2.0F, viewCornerSize)
                        if (isViewCornerWhRatioChangeEnabled)
                            viewCornerClipRectF.inset(
                                    Math.min(viewCornerClipRectF.absWidth() / 2.0F, viewCornerSize) - clampedCornerSize,
                                    Math.min(viewCornerClipRectF.absHeight() / 2.0F, viewCornerSize) - clampedCornerSize
                            )
                        clampedCornerSize
                    }
                    else -> viewCornerSize
                }
                with(viewCornerClipPath) {
                    rewind()
                    if (viewCornerType == CornerType.ROUND)
                        addRoundRectOverlap(viewCornerClipRectF, cornerSize, cornerSize)
                    else addBevelRect(viewCornerClipRectF, cornerSize, cornerSize)
                    close()
                }
                isViewCornerDirty = false
            }
            canvas.clipPath(viewCornerClipPath)
        }
    }

    /**
     * Where all parallax magic happens.
     * Contains full cycle logic for parallax calculation for given parameters.
     * Also image corners clipping happens here.
     */
    private fun calculateParallax(canvas: Canvas?) {
        if (parallaxDirectionFlag == NONE || (parallaxAbsoluteTension == 0.0F && parallaxTension == 0.0F) ||
                drawable == null || canvas == null) return

        // 1. Modify (mostly scale) rect of our source image inside canvas rect if needed
        // 2. Place newly found modified image rect according to canvas rect considering scale type
        // 3. Identify min and max tension rects.
        //    Also max tension rect is gonna be used for image round clipping (TENSION_BOUNDS)
        // 4. Calculate float tension rect for current fraction considering direction permission
        // 5. Clip all corners if any translate canvas for parallax effect

        fun addViewPadding(rectF: RectF) {
            rectF.left += paddingLeft
            rectF.top += paddingTop
            rectF.right -= paddingRight
            rectF.bottom -= paddingBottom
        }

//        fun clipImageCorners(
//                dxTranslation: Float = 0.0F,
//                dyTranslation: Float = 0.0F,
//                initClipRectIfNeeded: () -> Unit = {
//                    if (imageCornerClipRectF.isEmpty) {
//                        imageCornerClipRectF.set(canvas.clipBounds)
//                        addViewPadding(imageCornerClipTempRectF)
//                        imageCornerClipRectF.union(imageMaxBoundRectF)
//                    }
//                }
//        ) {
//            canvas.translate(dxTranslation, dyTranslation)
//
//            if ((imageCornerSize > 0.0F || isForceImageCornersWhRatioEqual)
//                    && imageCornerClipFlag != ImageCornerClipFlag.NONE) {
//                imageCornerClipRectF.clear()
//                if (isClipBoundCorner) {
//                    initClipRectIfNeeded()
//                    imageCornerClipTempRectF.set(canvas.clipBounds)
//                    addViewPadding(imageCornerClipTempRectF)
//                    imageCornerClipRectF.intersect(imageCornerClipTempRectF)
//                }
//                if (isClipSourceCorner) {
//                    initClipRectIfNeeded()
//                    imageCornerClipRectF.intersect(imageModifiedRectF)
//                }
//                if (isClipTensionCorner) {
//                    initClipRectIfNeeded()
//                    imageCornerClipTempRectF.set(imageMaxTensionRectF)
//                    imageCornerClipTempRectF.offset(
//                            if (parallaxHorizontalImageModifier == -1.0F) -dxTranslation else 0.0F,
//                            if (parallaxVerticalImageModifier == -1.0F) -dyTranslation else 0.0F
//                    )
//                    imageCornerClipRectF.intersect(imageCornerClipTempRectF)
//                }
//
//                if (!imageCornerClipRectF.isEmpty) {
//                    imageCornerClipPath.rewind()
//                    val cornerSize = when {
//                        isForceImageCornersWhRatioEqual -> imageCornerClipRectF.makeSquareAndReturnSize() / 2.0F
//                        isPreventImageCornerOverlap -> {
//                            val clampedCornerSize = Math.min(imageCornerClipRectF.minSideSize() / 2.0F, imageCornerSize)
//                            if (isImageCornerWhRatioChangeEnabled)
//                                imageCornerClipRectF.inset(
//                                        Math.min(imageCornerClipRectF.absWidth() / 2.0F, imageCornerSize) - clampedCornerSize,
//                                        Math.min(imageCornerClipRectF.absHeight() / 2.0F, imageCornerSize) - clampedCornerSize
//                                )
//                            clampedCornerSize
//                        }
//                        else -> imageCornerSize
//                    }
//                    if (imageCornerType == CornerType.ROUND)
//                        imageCornerClipPath.addRoundRectOverlap(imageCornerClipRectF, cornerSize, cornerSize)
//                    else imageCornerClipPath.addBevelRect(imageCornerClipRectF, cornerSize, cornerSize)
//                    imageCornerClipPath.close()
//                    canvas.clipPath(imageCornerClipPath)
//                }
//            }
//
//            if (isForceParallaxByScale)
//                canvas.scale(parallaxForcedScale, parallaxForcedScale, canvasRectF.centerX(), canvasRectF.centerY())
//        }

        fun clipImageCorners(
                dxTranslation: Float = 0.0F,
                dyTranslation: Float = 0.0F,
                calculateCornerSizeAndClipPath: () -> Unit =
                        {
                            imageCornerClipPath.rewind()
                            val cornerSize = when {
                                isForceImageCornersWhRatioEqual -> imageCornerClipRectF.makeSquareAndReturnSize() / 2.0F
                                isPreventImageCornerOverlap -> {
                                    val clampedCornerSize = Math.min(imageCornerClipRectF.minSideSize() / 2.0F, imageCornerSize)
                                    if (isImageCornerWhRatioChangeEnabled)
                                        imageCornerClipRectF.inset(
                                                Math.min(imageCornerClipRectF.absWidth() / 2.0F, imageCornerSize) - clampedCornerSize,
                                                Math.min(imageCornerClipRectF.absHeight() / 2.0F, imageCornerSize) - clampedCornerSize
                                        )
                                    clampedCornerSize
                                }
                                else -> imageCornerSize
                            }
                            if (imageCornerType == CornerType.ROUND)
                                imageCornerClipPath.addRoundRectOverlap(imageCornerClipRectF, cornerSize, cornerSize)
                            else imageCornerClipPath.addBevelRect(imageCornerClipRectF, cornerSize, cornerSize)
                            imageCornerClipPath.close()
                            if (!imageCornerClipPath.isEmpty) canvas.clipPath(imageCornerClipPath)
                        }
        ) {
            canvas.translate(dxTranslation, dyTranslation)
            if ((imageCornerSize > 0.0F || isForceImageCornersWhRatioEqual)
                    && imageCornerClipFlag != ImageCornerClipFlag.NONE) {

                if (isClipBoundCorner) {
                    imageCornerClipRectF.set(canvas.clipBounds)
                    addViewPadding(imageCornerClipRectF)
                    calculateCornerSizeAndClipPath()
                }

                if (isClipSourceCorner) {
                    imageCornerClipRectF.set(imageModifiedRectF)
                    calculateCornerSizeAndClipPath()
                }

                if (isClipTensionCorner) {
                    imageCornerClipRectF.set(imageMaxTensionRectF)
                    imageCornerClipRectF.offset(
                            if (parallaxHorizontalImageModifier == -1.0F) -dxTranslation else 0.0F,
                            if (parallaxVerticalImageModifier == -1.0F) -dyTranslation else 0.0F
                    )
                    calculateCornerSizeAndClipPath()
                }
            }

            if (isForceParallaxByScale)
                canvas.scale(parallaxForcedScale, parallaxForcedScale, canvasRectF.centerX(), canvasRectF.centerY())
        }

        if (isCalculationDirty) {
            imageModifiedRectF.set(drawable.bounds) // here image is not modified
            canvasRectF.set(canvas.clipBounds)
            addViewPadding(canvasRectF)
            if (imageModifiedRectF.isEmpty || canvasRectF.isEmpty) return

            val whInRatio = imageModifiedRectF.whRatio()
            val whOutRatio = canvasRectF.whRatio()
            val canvasHeight = canvasRectF.absHeight()
            val canvasWidth = canvasRectF.absWidth()
            val srcImageHeight = imageModifiedRectF.absHeight()
            val srcImageWidth = imageModifiedRectF.absWidth()

            var scaledImageWidth: Float
            var scaledImageHeight: Float

            fun scaleSrcImageInsideCanvas(fitCompletelyInside: Boolean = true, keepAspectRatio: Boolean = true) {
                if (whInRatio == whOutRatio || !keepAspectRatio) {
                    scaledImageWidth = canvasWidth
                    scaledImageHeight = canvasHeight
                } else if (fitCompletelyInside xor (whInRatio < whOutRatio)) {
                    scaledImageWidth = canvasWidth
                    scaledImageHeight = srcImageHeight * scaledImageWidth / srcImageWidth
                } else {
                    scaledImageHeight = canvasHeight
                    scaledImageWidth = srcImageWidth * scaledImageHeight / srcImageHeight
                }

                if (isForceParallaxByScale) {
                    parallaxForcedScale = 1.0F
                    when {
                        parallaxAbsoluteTension != 0.0F -> {
                            if (allowVerticalScroll) {
                                parallaxForcedScale = ((
                                        if (parallaxAbsoluteTension > 0.0F)
                                            (canvasHeight + (parallaxAbsoluteTension * 2.0F))
                                        else ((canvasHeight + parallaxAbsoluteTension) * 2.0F)
                                        ) clamp (scaledImageHeight..canvasHeight * 2.0F)) / scaledImageHeight
                            }
                            if (allowHorizontalScroll) {
                                parallaxForcedScale = Math.max(
                                        parallaxForcedScale,
                                        ((if (parallaxAbsoluteTension > 0.0F)
                                            (canvasWidth + (parallaxAbsoluteTension * 2.0F))
                                        else ((canvasWidth + parallaxAbsoluteTension) * 2.0F))
                                                clamp (scaledImageWidth..canvasWidth * 2.0F)) / scaledImageWidth
                                )
                            }
                        }
                        parallaxTension != 0.0F -> {
                            if (allowVerticalScroll)
                                parallaxForcedScale = Math.max(canvasHeight * parallaxTensionScale, scaledImageHeight) / scaledImageHeight
                            if (allowHorizontalScroll)
                                parallaxForcedScale = Math.max(parallaxForcedScale, Math.max(canvasWidth * parallaxTensionScale, scaledImageWidth) / scaledImageWidth)
                        }
                    }
                    if (parallaxForcedScale != 0.0F) {
                        scaledImageWidth *= parallaxForcedScale
                        scaledImageHeight *= parallaxForcedScale
                    }
                }

                imageModifiedRectF.set(0.0F, 0.0F, scaledImageWidth, scaledImageHeight)
            }

            fun findImageTensionRects() {
                // this rect defines biggest ares of view or image
                imageMaxBoundRectF.set(
                        Math.min(imageModifiedRectF.left, canvasRectF.left),
                        Math.min(imageModifiedRectF.top, canvasRectF.top),
                        Math.max(imageModifiedRectF.right, canvasRectF.right),
                        Math.max(imageModifiedRectF.bottom, canvasRectF.bottom)
                )
                //if tension is zero tension rect side could be <= canvas rect respective side
                imageMinTensionRectF.set(
                        Math.max(imageModifiedRectF.left, canvasRectF.left),
                        Math.max(imageModifiedRectF.top, canvasRectF.top),
                        Math.min(imageModifiedRectF.right, canvasRectF.right),
                        Math.min(imageModifiedRectF.bottom, canvasRectF.bottom)
                )
                with(imageMaxTensionRectF) {
                    set(imageMinTensionRectF)
                    if (allowHorizontalScroll) {
                        val leftOffset = Math.abs(imageModifiedRectF.left - canvasRectF.left)
                        val rightOffset = Math.abs(imageModifiedRectF.right - canvasRectF.right)
                        left -= when {
                            parallaxAbsoluteTension > 0.0F -> Math.min(leftOffset, parallaxAbsoluteTension)
                            parallaxAbsoluteTension < 0.0F -> Math.max(0.0F, leftOffset + parallaxAbsoluteTension)
                            isForceParallaxByScale -> leftOffset
                            else -> leftOffset * parallaxTension
                        }
                        right += when {
                            parallaxAbsoluteTension > 0.0F -> Math.min(rightOffset, parallaxAbsoluteTension)
                            parallaxAbsoluteTension < 0.0F -> Math.max(0.0F, rightOffset + parallaxAbsoluteTension)
                            isForceParallaxByScale -> rightOffset
                            else -> rightOffset * parallaxTension
                        }
                    }
                    if (allowVerticalScroll) {
                        val topOffset = Math.abs(imageModifiedRectF.top - canvasRectF.top)
                        val bottomOffset = Math.abs(imageModifiedRectF.bottom - canvasRectF.bottom)
                        top -= when {
                            parallaxAbsoluteTension > 0.0F -> Math.min(topOffset, parallaxAbsoluteTension)
                            parallaxAbsoluteTension < 0.0F -> Math.max(0.0F, topOffset + parallaxAbsoluteTension)
                            isForceParallaxByScale -> topOffset
                            else -> topOffset * parallaxTension
                        }
                        bottom += when {
                            parallaxAbsoluteTension > 0.0F -> Math.min(bottomOffset, parallaxAbsoluteTension)
                            parallaxAbsoluteTension < 0.0F -> Math.max(0.0F, bottomOffset + parallaxAbsoluteTension)
                            isForceParallaxByScale -> bottomOffset
                            else -> bottomOffset * parallaxTension
                        }
                    }
                }

                //prepare image tension rect and define total parallax offset
                imageTensionRectF.set(imageMinTensionRectF)
                val minTensionWidth = imageMinTensionRectF.absWidth()
                val minTensionHeight = imageMinTensionRectF.absHeight()
                parallaxLeftTotalOffset = (imageMaxTensionRectF.absWidth() - minTensionWidth)
                parallaxTopTotalOffset = (imageMaxTensionRectF.absHeight() - minTensionHeight)
                parallaxHorizontalImageModifier = if (canvasWidth > minTensionWidth) -1.0F else 1.0F
                parallaxVerticalImageModifier = if (canvasHeight > minTensionHeight) -1.0F else 1.0F
            }

            when (scaleType) {
                null -> {
                }
                CENTER -> {
                    // image could be bigger or lesser that view size
                    // 1. no scaling applied here
                    // 2.
                    imageModifiedRectF.centerToCenter(canvasRectF)
                }

                CENTER_CROP -> {
                    // if drawable and view aspect ratios the same - no parallax possible in such case
                    // otherwise - parallax allowed only in one direction
                    // 1.
                    scaleSrcImageInsideCanvas(false)
                    // 2.
                    imageModifiedRectF.centerToCenter(canvasRectF)
                }

                CENTER_INSIDE -> {
                    // if image is bigger that view bound - fitIn to bigger size.
                    // In such case only one parallax direction allowed
                    // In case image is originally smaller in both direction - parallax can work in both directions
                    // 1.
                    if (srcImageWidth > canvasWidth || srcImageHeight > canvasHeight) scaleSrcImageInsideCanvas()
                    // 2.
                    imageModifiedRectF.centerToCenter(canvasRectF)
                }

                FIT_CENTER -> {
                    // if drawable and view aspect ratios the same - no parallax possible in such case
                    // almost like previous center inside but always scale to fitIn higher side
                    // 1.
                    scaleSrcImageInsideCanvas()
                    // 2.
                    imageModifiedRectF.centerToCenter(canvasRectF)
                }

                FIT_END -> {
                    // note that is tension is max (1.0) parallax behavior the same as in fitIn center
                    // if drawable and view aspect ratios the same - no parallax possible in such case
                    // unless forceParallaxByScale turned on
                    // almost similar somehow to previous (fitIn center) -> always scale to fitIn higher side
                    // 1.
                    scaleSrcImageInsideCanvas()
                    // 2.
                    imageModifiedRectF.offsetTo(
                            canvasRectF.right - imageModifiedRectF.absWidth(),
                            canvasRectF.bottom - imageModifiedRectF.absHeight()
                    )
                }

                FIT_START -> {
                    // logic the same as in fitIn end, just inverted
                    // note that is tension is max (1.0) parallax behavior the same as in fitIn center
                    // 1.
                    scaleSrcImageInsideCanvas()
                    imageModifiedRectF.offsetTo(canvasRectF.left, canvasRectF.top)
                }

                FIT_XY -> {
                    /* parallax is not possible obviously here*/
                    // 1.
                    scaleSrcImageInsideCanvas(keepAspectRatio = false)
                    // 2.
                    imageModifiedRectF.centerToCenter(canvasRectF)
                }

                MATRIX -> {
                    /* for clever minds :)*/
                    //just repeat fitIn start as default matrix logic
                    // 1.
                    scaleSrcImageInsideCanvas()
                    // 2.
                    imageModifiedRectF.offsetTo(canvasRectF.left, canvasRectF.top)
                }
            }

            // 3.
            findImageTensionRects()
        }

        // 4.
        imageTensionRectF.offsetTo(
                imageMaxTensionRectF.left + parallaxLeftTotalOffset * parallaxHorizontalFraction,
                imageMaxTensionRectF.top + parallaxTopTotalOffset * parallaxVerticalFraction
        )

        // 5.
        clipImageCorners(
                (imageMinTensionRectF.left - imageTensionRectF.left) * parallaxHorizontalImageModifier,
                (imageMinTensionRectF.top - imageTensionRectF.top) * parallaxVerticalImageModifier
        )

        isCalculationDirty = false
    }

    override fun setBackground(background: Drawable?) {
        if (background == null || backgroundCornerRadius <= 0.0F) super.setBackground(background)
        else if (background.intrinsicWidth == 0 || background.intrinsicHeight == 0 || width == 0 || height == 0) {
            hasBgToSet = true
            super.setBackground(background)
        } else {
            val roundedDrawable: RoundedBitmapDrawable = when (background) {
                is RoundedBitmapDrawable -> background
                is BitmapDrawable -> RoundedBitmapDrawableFactory.create(resources, background.bitmap)
                else -> RoundedBitmapDrawableFactory.create(resources, background.toBitmap(width, height))
            }
            roundedDrawable.cornerRadius = backgroundCornerRadius
            super.setBackground(roundedDrawable)
        }
        invalidateDirty()
    }

    override fun setScaleType(scaleType: ScaleType?) {
        isCalculationDirty = scaleType != this.scaleType
        super.setScaleType(scaleType)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        if (!isCalculationDirty)
            isCalculationDirty = paddingLeft != left || paddingTop != top ||
                    paddingRight != right || paddingBottom != bottom
        super.setPadding(left, top, right, bottom)
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        if (!isCalculationDirty)
            isCalculationDirty = paddingLeft != left || paddingTop != top ||
                    paddingRight != right || paddingBottom != bottom
        super.setPaddingRelative(start, top, end, bottom)
    }

    override fun draw(canvas: Canvas?) {
        clipViewCornerRadius(canvas)
        super.draw(canvas)
    }

    override fun onDraw(canvas: Canvas?) {
        calculateParallax(canvas)
        super.onDraw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (hasBgToSet && measuredHeight != 0 && measuredWidth != 0) {
            hasBgToSet = false
            background = background
        }
        isViewCornerDirty = true
        invalidateDirty()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (hasBgToSet && w != 0 && h != 0) {
            hasBgToSet = false
            background = background
        }
        isViewCornerDirty = true
        invalidateDirty()
    }

    /**
     * The ParallaxDirectionFlag specifies directions in which parallax is allowed.
     * Note that in case image respective side (adjusted by scale type and other parameters) == view padded side,
     * no parallax effect could be achieved even if parallax is allowed in specified direction
     * Default type is [VERTICAL]|[HORIZONTAL]
     */
    object ParallaxDirectionFlag {
        const val NONE = 0
        const val VERTICAL = 1
        const val HORIZONTAL = 2
    }

    /**
     * The ImageCornerClipFlag specifies flags for image corner clipping.
     * All parameters can be used in conjunction as int flags.
     * Default value is [SOURCE]|[BOUND].
     */
    object ImageCornerClipFlag {
        /**
         * No clipping strategy. Event if corner size is explicitly specified no clipping will occur either.
         */
        const val NONE = 0

        /**
         * Clip source image. If image is bigger that view's respective dimensions you may not observe clipped corners,
         * otherwise you'll always see clipped image
         */
        const val SOURCE = 1

        /**
         * Clip tension flag used to clip image around max tension bounds.
         * In case [parallaxTension] == 1.0F and image is bigger that view this flag has the same effect as [SOURCE]
         */
        const val TENSION = 2

        /**
         * Specifies clipping area around view (padded) bounds.
         */
        const val BOUND = 4
    }

    /**
     * The CornerType specifies clipping type for image or view itself.
     * Default type is [ROUND]
     */
    object CornerType {
        const val ROUND = 1
        const val BEVEL = 2
    }

    /**
     * Listener to track changed whenever vertical or horizontal parallax changed.
     * Keep in mind that in practice callback is called for each fraction change (vertical or horizontal)
     * independently.
     *
     * For instance, when both parallax vertical and horizontal fractions are changed one after another -
     * two respective callback are called for each change.
     */
    interface OnParallaxFractionChangeListener {

        /**
         * Return old and new fraction for each axis respectively.
         *
         * Note that in practice new and old fraction for one axis always will be the same,
         * as method are triggered for each axis change independently.
         */
        fun onParallaxFractionChanged(
                newHorizontalFraction: Float,
                newVerticalFraction: Float,
                oldHorizontalFraction: Float,
                oldVerticalFraction: Float
        )
    }
}