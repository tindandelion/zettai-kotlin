import java.time.LocalDate


data class User(val name: String)

data class ListName private constructor(val name: String) {
    companion object {
        private val validNamePattern = Regex(pattern = "[A-Za-z0-9-]+")
        private val String.hasValidLength: Boolean
            get() = length in 1..40

        fun fromUntrusted(listName: String): ListName? {
            return if (listName.matches(validNamePattern) && listName.hasValidLength)
                fromTrusted(listName)
            else null
        }

        fun fromTrusted(listName: String): ListName = ListName(listName)
    }
}

data class ToDoItem(
    val description: String,
    val status: ToDoStatus = ToDoStatus.ToDo,
    val dueDate: LocalDate? = null
)

data class ToDoList(val name: ListName, val items: List<ToDoItem>) {
    fun addItem(item: ToDoItem): ToDoList = copy(items = items.except(item) + item)

    private fun List<ToDoItem>.except(item: ToDoItem) =
        filterNot { it.description == item.description }
}

enum class ToDoStatus { ToDo }

interface ZettaiHub {
    fun getList(user: User, listName: ListName): ToDoList?
    fun addItemToList(user: User, listName: ListName, item: ToDoItem): ToDoList?
}

typealias ToDoListFetcher = (User, ListName) -> ToDoList?

interface ToDoListUpdatableFetcher : ToDoListFetcher {
    fun assignListToUser(user: User, list: ToDoList): ToDoList?
}

class ToDoListHub(private val fetcher: ToDoListUpdatableFetcher) : ZettaiHub {
    override fun getList(user: User, listName: ListName) = fetcher(user, listName)

    override fun addItemToList(user: User, listName: ListName, item: ToDoItem): ToDoList? =
        fetcher(user, listName)?.run {
            val newList = addItem(item)
            fetcher.assignListToUser(user, newList)
        }
}
