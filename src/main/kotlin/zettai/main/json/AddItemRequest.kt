package zettai.main.json

import java.time.LocalDate

data class AddItemRequest(val itemName: String, val itemDue: LocalDate? = null)
data class AddListRequest(val listName: String)