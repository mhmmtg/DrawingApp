package net.mguler.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attributes: AttributeSet): View(context, attributes) {

    private var mDrawPath: CustomPath
    private lateinit var mCanvasBitmap: Bitmap
    private var mDrawPaint: Paint = Paint()
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 5F
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private var mPaths = ArrayList<CustomPath>()

    init {
        mDrawPath = CustomPath(color, mBrushSize)

        mDrawPaint.color = color
        mDrawPaint.strokeWidth = mBrushSize

        mDrawPaint.style = Paint.Style.STROKE
        mDrawPaint.strokeJoin = Paint.Join.ROUND
        mDrawPaint.strokeCap = Paint.Cap.ROUND

        mCanvasPaint = Paint(Paint.DITHER_FLAG)

        println("init")

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap, 0F, 0F, mCanvasPaint)

        //Draw all paths
        for (path in mPaths) {
            mDrawPaint.strokeWidth = path.brushSize
            mDrawPaint.color = path.color
            canvas.drawPath(path, mDrawPaint)
        }

        //color ve size her draw yeniden atanıyor
        if (!mDrawPath.isEmpty) {
            mDrawPaint.strokeWidth = mDrawPath.brushSize
            mDrawPaint.color = mDrawPath.color
            canvas.drawPath(mDrawPath, mDrawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath.color = color
                mDrawPath.brushSize = mBrushSize

                mDrawPath.reset()
                mDrawPath.moveTo(touchX, touchY)
            }

            MotionEvent.ACTION_MOVE -> { mDrawPath.lineTo(touchX, touchY) }

            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }

        invalidate()
        return true
    }

    fun setBrushSize(newSize: Float) {
        mBrushSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        //mDrawPaint.strokeWidth = mBrushSize
    }

    fun setBrushColor(newColor: String) {
        //mDrawPaint.color = Color.parseColor(newColor)
        color = Color.parseColor(newColor)
    }

    fun clear() {
        //mDrawPath.reset()
        mPaths.clear()
        invalidate()
    }

    fun undo() {
        if (mPaths.isNotEmpty()) {
            mPaths.removeLast()
            invalidate()
        }
    }



    internal inner class CustomPath(var color: Int, var brushSize: Float): Path()

}