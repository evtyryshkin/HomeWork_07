package otus.homework.customview

data class Purchase(
    val id: Long,
    val name: String,
    val price: Long,
    val category: String,
    val dayOfMonth: Int
)
