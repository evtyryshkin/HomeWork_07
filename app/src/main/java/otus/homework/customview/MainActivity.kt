package otus.homework.customview

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<PieChart>(R.id.pie).apply {
            setData(fetchData())
            setClickListener {
                Log.e("Clicked on category - ", it)
            }
        }
        findViewById<LinearChart>(R.id.linear).apply {
            setData(fetchData())
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
}