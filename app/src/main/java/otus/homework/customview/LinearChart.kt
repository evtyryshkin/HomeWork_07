package otus.homework.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class LinearChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var purchaces: List<Purchase> = emptyList()

    // Map<Category, Map<Day, List<Purchase>>>
    private var groupedPurchases: Map<String, Map<Int, List<Purchase>>> = emptyMap()
    private val rectF: RectF = RectF()
    private var offset: Float = 40f
    private val axisPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val axisText = Paint().apply {
        textSize = 30f
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val colors = context.resources.getIntArray(R.array.rainbow).toMutableList()

    fun setData(purchases: List<Purchase>) {
        this.purchaces = purchases
        groupedPurchases =
            purchases.groupBy { it.category }.mapValues { it.value.groupBy { it.dayOfMonth } }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minHeight = 400
        val minWidth = 600

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)

        width = when (widthMode) {
            MeasureSpec.EXACTLY -> if (width < minWidth) minWidth else width
            MeasureSpec.AT_MOST -> if (width < minWidth) minWidth else width
            else -> minWidth
        }

        height = when (heightMode) {
            MeasureSpec.EXACTLY -> if (height < minHeight) minHeight else height
            MeasureSpec.AT_MOST -> if (height < minHeight) minHeight else height
            else -> minHeight
        }
        rectF.apply {
            left = offset
            top = offset
            right = width.toFloat()
            bottom = height.toFloat() - offset
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем оси
        // Ось X
        canvas.drawLine(
            rectF.left + offset,
            rectF.bottom,
            rectF.right,
            rectF.bottom,
            axisPaint
        )
        // Ось Y
        canvas.drawLine(
            rectF.left + offset,
            rectF.top,
            rectF.left + offset,
            rectF.bottom,
            axisPaint
        )

        if (groupedPurchases.isEmpty()) return

        val days = purchaces.map { it.dayOfMonth }
        val maxDay = days.maxOrNull() ?: return
        val minDay = days.minOrNull() ?: return
        val dayRange = maxDay - minDay
        val dateStep = rectF.width() / dayRange.toFloat()

        var maxSum = 0L
        groupedPurchases.forEach { categoryEntry ->
            categoryEntry.value.forEach { dayEntry ->
                val sumOfThisDay = dayEntry.value.sumOf { it.price }
                if (sumOfThisDay > maxSum) maxSum = sumOfThisDay
            }
        }
        val sumStep = rectF.height() / maxSum.toFloat()

        var colorIndex = 0
        val categoryPaint = Paint().apply { strokeWidth = 3f }

        groupedPurchases.forEach { (_, categoryPurchaseList) ->
            categoryPaint.color = colors[colorIndex % colors.size]
            colorIndex++

            var previousX = 0f
            var previousY = 0f

            categoryPurchaseList.forEach { dayEntry ->
                val x = rectF.left + (dayEntry.key - minDay) * dateStep + offset
                val y = rectF.bottom - dayEntry.value.sumOf { it.price } * sumStep

                if (previousX != 0f && previousY != 0f) {
                    canvas.drawLine(previousX, previousY, x, y, categoryPaint)
                }

                canvas.drawCircle(x, y, 5f, categoryPaint)
                previousX = x
                previousY = y
            }
        }

        // Рисуем метки дат под осью X
        days.forEach { day ->
            val dateX = rectF.left + (day - minDay) * dateStep + offset
            canvas.drawText(day.toString(), dateX, rectF.bottom + 30, axisText)
        }

        // Рисуем метки значений по оси Y
        val numYLabels = 5
        val yLabelStep = maxSum / numYLabels
        for (i in 0..numYLabels) {
            val y = rectF.bottom - i * yLabelStep * sumStep
            canvas.drawText((i * yLabelStep).toString(), rectF.left - 35, y, axisText)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        val data = Gson().toJson(purchaces)
        bundle.putString("purchases", data)
        bundle.putParcelable("instanceState", super.onSaveInstanceState())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = state as Bundle
        val type = object : TypeToken<List<Purchase>>() {}.type
        purchaces = bundle.getString("purchases")?.let { Gson().fromJson(it, type) } ?: emptyList()
        super.onRestoreInstanceState(bundle.getParcelable("instanceState"))
    }
}
