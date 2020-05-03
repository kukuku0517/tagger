package com.project.tagger.util.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.project.tagger.R
import com.project.tagger.util.tag


class ShadowLayout : FrameLayout {

    private var mShadowColor: Int = 0
    private var mShadowRadius: Float = 0.toFloat()
    private var mCornerRadius: Float = 0.toFloat()
    private var mDx: Float = 0.toFloat()
    private var mDy: Float = 0.toFloat()

    private var mInvalidateShadowOnSizeChanged = true
    private var mForceInvalidateShadow = false

    constructor(context: Context) : super(context) {
        initView(context, null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initView(context, attrs)
    }

    override fun getSuggestedMinimumWidth(): Int {
        return 0
    }

    override fun getSuggestedMinimumHeight(): Int {
        return 0
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && (background == null || mInvalidateShadowOnSizeChanged || mForceInvalidateShadow)) {
            mForceInvalidateShadow = false
            Log.i(tag(), "onSizeChanged w:h = $w : $h")
            setBackgroundCompat(w, h)
        }
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        if (mForceInvalidateShadow) {
            mForceInvalidateShadow = false
            Log.i(tag(), "w:h = ${right - left} : ${bottom - top}")
            setBackgroundCompat(right - left, bottom - top)
        }
    }

    fun setInvalidateShadowOnSizeChanged(invalidateShadowOnSizeChanged: Boolean) {
        mInvalidateShadowOnSizeChanged = invalidateShadowOnSizeChanged
    }

    fun invalidateShadow() {
        mForceInvalidateShadow = true
        requestLayout()
        invalidate()
    }

    private fun initView(
        context: Context,
        attrs: AttributeSet?
    ) {
        initAttributes(context, attrs)
//        val pl  = min(paddingLeft,(mShadowRadius + abs(mDx)).toInt())
//        val pr =  min(paddingRight,(mShadowRadius + abs(mDx)).toInt())
//        val pt =  min(paddingTop,(mShadowRadius + abs(mDy)).toInt())
//        val pb =  min(paddingBottom,(mShadowRadius + abs(mDy)).toInt())
//
//        setPadding(pl, pt, pr, pb)
    }

    private fun setBackgroundCompat(
        w: Int,
        h: Int
    ) {
        val bitmap = createShadowBitmap(w, h, mCornerRadius, mShadowRadius, mDx, mDy, mShadowColor, Color.BLACK)
        val drawable = BitmapDrawable(resources, bitmap)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(drawable)
        } else {
            background = drawable
        }
    }


    private fun initAttributes(
        context: Context,
        attrs: AttributeSet?
    ) {
        val attr = getTypedArray(context, attrs, R.styleable.ShadowLayout) ?: return

        try {
            mCornerRadius = attr.getDimension(R.styleable.ShadowLayout_sl_cornerRadius, 0f)
            mShadowRadius = attr.getDimension(R.styleable.ShadowLayout_sl_shadowRadius, 0f)
            mDx = attr.getDimension(R.styleable.ShadowLayout_sl_dx, 0f)
            mDy = attr.getDimension(R.styleable.ShadowLayout_sl_dy, 0f)
            mShadowColor = attr.getColor(R.styleable.ShadowLayout_sl_shadowColor, resources.getColor(R.color.transparent))
        } catch (e: Exception) {
            Log.w(tag(), e)
        } finally {
            attr.recycle()
        }

    }

    private fun getTypedArray(
        context: Context,
        attributeSet: AttributeSet?,
        attr: IntArray
    ): TypedArray? {
        return context.obtainStyledAttributes(attributeSet, attr, 0, 0)
    }

    private fun createShadowBitmap(
        shadowWidth: Int,
        shadowHeight: Int,
        cornerRadius: Float,
        shadowRadius: Float,
        dx: Float,
        dy: Float,
        shadowColor: Int,
        fillColor: Int
    ): Bitmap {

        val output = Bitmap.createBitmap(shadowWidth, shadowHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val shadowRect = RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            shadowWidth - paddingRight.toFloat(),
            shadowHeight - paddingBottom.toFloat())

        if (dy > 0) {
            shadowRect.top += dy
            shadowRect.bottom -= dy
        } else if (dy < 0) {
            shadowRect.top += Math.abs(dy)
            shadowRect.bottom -= Math.abs(dy)
        }

        if (dx > 0) {
            shadowRect.left += dx
            shadowRect.right -= dx
        } else if (dx < 0) {
            shadowRect.left += Math.abs(dx)
            shadowRect.right -= Math.abs(dx)
        }

        val shadowPaint = Paint()
        shadowPaint.setAntiAlias(true)
        shadowPaint.setColor(fillColor)
        shadowPaint.setStyle(Paint.Style.FILL)


        if (!isInEditMode) {
            shadowPaint.setShadowLayer(shadowRadius, dx, dy, shadowColor)
        }

        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        return output
    }

}