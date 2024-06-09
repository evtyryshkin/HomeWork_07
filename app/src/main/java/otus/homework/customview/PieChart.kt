package otus.homework.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt


class PieChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var size: Int = 0
    private var purchases: Map<String, List<Purchase>> = emptyMap()
    private val categoryAngles: MutableMap<String, List<Float>> = mutableMapOf()
    private var allSpentSum: Long = 0
    private val rectF: RectF = RectF()
    private var widthChart: Float = 80f
    private val fillPaint = Paint()
    private var listener: OnCategoryClickListener? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PieChart, defStyleAttr, 0)
        widthChart = typedArray.getDimension(R.styleable.PieChart_chart_width, 80f)
        typedArray.recycle()
        fillPaint.apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = widthChart
        }
    }

    private val colors = context.resources.getIntArray(R.array.rainbow).toMutableList()

    fun setData(purchases: List<Purchase>) {
        this.purchases = purchases.groupBy { it.category }
        allSpentSum = purchases.sumOf { it.price }
        invalidate()
    }

    fun setClickListener(onClickListener: OnCategoryClickListener) {
        listener = onClickListener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minSize = 300

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)

        width = when (widthMode) {
            MeasureSpec.EXACTLY -> if (width < minSize) minSize else width
            MeasureSpec.AT_MOST -> if (width < minSize) minSize else width
            else -> minSize
        }

        height = when (heightMode) {
            MeasureSpec.EXACTLY -> if (height < minSize) minSize else height
            MeasureSpec.AT_MOST -> if (height < minSize) minSize else height
            else -> minSize
        }
        size = min(width, height)
        rectF.apply {
            left = widthChart / 2
            top = widthChart / 2
            right = size.toFloat() - (widthChart / 2)
            bottom = size.toFloat() - (widthChart / 2)
        }
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var startAngle = 0f
        purchases.forEach { purchase ->
            val angle = ((purchase.value.sumOf { it.price } * 100).toFloat() / allSpentSum) * 360 / 100
            fillPaint.color = colors.last()
            colors.removeLast()
            canvas.drawArc(rectF, startAngle, angle, false, fillPaint)
            categoryAngles[purchase.key] = listOf(startAngle, startAngle + angle)

            startAngle += angle
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        val data = Gson().toJson(purchases)
        bundle.putString("purchases", data)
        bundle.putParcelable("instanceState", super.onSaveInstanceState())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = state as Bundle
        val type = object : TypeToken<Map<String, List<Purchase>>>() {}.type
        val p = bundle.getString("purchases")?.let {
            Gson().fromJson<Map<String, List<Purchase>>>(it, type)
        } ?: emptyMap()
        purchases = p
        super.onRestoreInstanceState(bundle.getParcelable("instanceState"))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val radius = size / 2
                val lenghtX = (if (event.x <= radius) radius - event.x else event.x - radius).toDouble()
                val lenghtY = (if (event.y <= radius) radius - event.y else event.y - radius).toDouble()
                val distanceFromMiddle = sqrt(lenghtX * lenghtX + lenghtY * lenghtY)
                val isClickIntoCategory = (distanceFromMiddle >= radius - widthChart) && (distanceFromMiddle <= radius)
                if (isClickIntoCategory) {
                    val angle = Math.toDegrees(atan2(lenghtY, lenghtX)).let {
                        if (event.x <= radius && event.y <= radius) it + 180
                        else if (event.x <= radius && event.y > radius) 180 - it
                        else if (event.x > radius && event.y <= radius) 360 - it
                        else 0 + it
                    }

                    categoryAngles.forEach {
                        if (angle >= it.value[0] && angle <= it.value[1]) {
                            listener?.onClick(it.key)
                        }
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

fun interface OnCategoryClickListener {
    fun onClick(category: String)
}
