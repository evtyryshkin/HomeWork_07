package otus.homework.customview

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.widget.doAfterTextChanged
import org.json.JSONArray
import java.util.Calendar

private const val ONE_SECOND = 1000L

class MainActivity : AppCompatActivity() {
    private var colors: IntArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        colors = applicationContext.resources.getIntArray(R.array.rainbow)

        /*findViewById<PieChart>(R.id.pie).apply {
            setData(fetchData())
            setClickListener {
                Log.e("Clicked on category - ", it)
            }
        }*/

        val textView = findViewById<TextView>(R.id.countdown).apply {
            addAnimator(getUpScaledValueAnimator()) { textView, animator ->
                textView.doAfterTextChanged { animator.start() }
            }
        }

        launchCountDownTimerForTextView(textView = textView, startMillis = 10000) {
            textView.getAlphaObjectAnimator().apply {
                start()
                doOnEnd {
                    findViewById<LinearChart>(R.id.linear).apply {
                        visibility = View.VISIBLE
                        setData(fetchData())
                    }
                }
            }
        }
    }

    private fun fetchData(): List<Purchase> {
        val jsonArray = JSONArray(
            applicationContext.resources.openRawResource(R.raw.payload).reader().readText()
        )
        return (0 until jsonArray.length()).map {
            val jsonObj = jsonArray.getJSONObject(it)
            val dayOfMonth = Calendar.getInstance().run {
                timeInMillis = jsonObj.optLong("time") * 1000
                get(Calendar.DAY_OF_MONTH)
            }
            Purchase(
                jsonObj.optLong("id"),
                jsonObj.optString("name"),
                jsonObj.optLong("amount"),
                jsonObj.optString("category"),
                dayOfMonth
            )
        }
    }
    private fun launchCountDownTimerForTextView(textView: TextView, startMillis: Long, onFinish: () -> Unit) =
        object : CountDownTimer(startMillis, ONE_SECOND) {
            override fun onTick(millisUntilFinished: Long) {
                val sec: Int = (millisUntilFinished / ONE_SECOND).toInt()
                textView.text = sec.toString()
                textView.setTextColor(colors?.get(sec) ?: Color.BLACK)
            }

            override fun onFinish() {
                onFinish()
            }
        }.run { start() }
}

private fun <T> T.addAnimator(animator: ValueAnimator, startWith: (T, ValueAnimator) -> Unit) {
    startWith(this, animator)
}

private fun View.getUpScaledValueAnimator(): ValueAnimator {
    val view = this
    return ValueAnimator.ofFloat(0f, 50f).apply {
        duration = ONE_SECOND
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val animatedValue = it.animatedValue as Float
            view.scaleX = animatedValue
            view.scaleY = animatedValue
        }
    }
}

private fun View.getAlphaObjectAnimator(): AnimatorSet {
    val view = this

    val rotationAnimator = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 1800f).apply {
        duration = ONE_SECOND
        interpolator = AccelerateInterpolator()
    }

    val scaleXAnimator = ObjectAnimator.ofFloat(view, View.SCALE_X, 50f, 0f).apply {
        duration = ONE_SECOND
        interpolator = AccelerateInterpolator()
    }
    val scaleYAnimator = ObjectAnimator.ofFloat(view, View.SCALE_Y, 50f, 0f).apply {
        duration = ONE_SECOND
        interpolator = AccelerateInterpolator()
    }

    return AnimatorSet().apply { playTogether(rotationAnimator, scaleXAnimator, scaleYAnimator) }
}
