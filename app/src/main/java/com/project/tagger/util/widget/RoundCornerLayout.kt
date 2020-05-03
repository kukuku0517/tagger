package com.project.tagger.util.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.project.tagger.R


class RoundCornerLayout : FrameLayout {

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


    private fun initView(
        context: Context,
        attrs: AttributeSet?
    ) {
        initAttributes(context, attrs)
    }


    var mCornerRadius: Float = 0f
    var mBackgroundColor: Int = 0
    var mBorderWidth: Float = 0f
    var mBorderColor: Int = -1
    var shouldUseMask: Boolean = false
    fun setRoundBackgroundColor(color: Int) {
        this.mBackgroundColor = color
    }


    var paint: Paint? = null
    var maskPaint: Paint? = null
    var maskBitmap: Bitmap? = null
    var shape: GradientDrawable? = null
    private fun initAttributes(
        context: Context,
        attrs: AttributeSet?
    ) {
        val attr = getTypedArray(context, attrs, R.styleable.RoundCornerLayout) ?: return

        try {
            mCornerRadius = attr.getDimension(R.styleable.RoundCornerLayout_roundCornerRadius, resources.getDimension(R.dimen.dp_1))
            mBackgroundColor = attr.getColor(R.styleable.RoundCornerLayout_roundBackgroundColor, resources.getColor(R.color.white))

            mBorderWidth = attr.getDimension(R.styleable.RoundCornerLayout_roundBorderWidth, resources.getDimension(R.dimen.dp_1))
            mBorderColor = attr.getColor(R.styleable.RoundCornerLayout_roundBorderColor, resources.getColor(R.color.transparent))
            shouldUseMask = attr.getBoolean(R.styleable.RoundCornerLayout_shouldUseMask, false)
        } catch (e: Exception) {

        } finally {
            attr.recycle()
        }


        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint?.apply {
            isAntiAlias = true
            color = mBackgroundColor
            style = Paint.Style.FILL
        }
        shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = mCornerRadius
            setColor(resources.getColor(R.color.transparent))
            setStroke(mBorderWidth.toInt(), mBorderColor)
        }

        maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        maskPaint?.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))

//        setFocusable(true);
//        setFocusableInTouchMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            clipToOutline = true
        }
        setWillNotDraw(false)

    }

    private fun getTypedArray(
        context: Context,
        attributeSet: AttributeSet?,
        attr: IntArray
    ): TypedArray? {
        return context.obtainStyledAttributes(attributeSet, attr, 0, 0)
    }


    private fun createRoundBitmap(
        width: Int,
        height: Int,
        mBackgroundColor: Int,
        mCornerRadius: Float,
        mBorderColor: Int,
        mBorderWidth: Float
    ): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val roundRect = RectF(
            0f,
            0f,
            width.toFloat(),
            height.toFloat())

        val roundPaint = Paint().apply {
            isAntiAlias = true
            color = mBackgroundColor
            style = Paint.Style.FILL

        }

        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = mCornerRadius
            setColor(resources.getColor(R.color.transparent))
            setStroke(mBorderWidth.toInt(), mBorderColor)
        }




        canvas.drawRoundRect(roundRect, mCornerRadius, mCornerRadius, roundPaint)
        background = (shape)

//        if (mBorderColor != -1) {
//            val borderRect = RectF(
//                    0f,
//                    0f,
//                    width.toFloat(),
//                    height.toFloat())
//
//            val borderPaint = Paint().apply {
//                isAntiAlias = true
//                color = mBorderColor
//                style = Paint.Style.STROKE
//                strokeWidth = mBorderWidth
//            }
//            canvas.drawRoundRect(borderRect, mCornerRadius, mCornerRadius, borderPaint)
//        }

        return output
    }


    override fun draw(canvas: Canvas) {

        if (shouldUseMask) {

            val offscreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            Log.i("draw", "draw")

            val offscreenCanvas = Canvas(offscreenBitmap)
            super.draw(offscreenCanvas)

            if (maskBitmap == null) {
                maskBitmap = createMask(width, height)
            }

//            background = (shape)
            offscreenCanvas.drawBitmap(maskBitmap!!, 0f, 0f, maskPaint)
            canvas.drawBitmap(offscreenBitmap, 0f, 0f, paint)

        } else {

            Log.i("draw", "draw")
            val roundRect = RectF(
                0f,
                0f,
                width.toFloat(),
                height.toFloat())

            val roundPaint = Paint().apply {
                isAntiAlias = true
                color = mBackgroundColor
                style = Paint.Style.FILL

            }


            canvas.drawRoundRect(roundRect, mCornerRadius, mCornerRadius, roundPaint)
            background = (shape)
            super.draw(canvas)
        }

    }

    private fun createMask(
        width: Int,
        height: Int
    ): Bitmap? {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(mask)
        val paint = Paint(ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), mCornerRadius, mCornerRadius, paint)
        return mask
    }
}