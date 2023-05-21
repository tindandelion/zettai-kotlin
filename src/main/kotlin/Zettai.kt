import java.time.LocalDate

data class User(val name: String)
data class ListName(val name: String)
data class ToDoItem(val description: String, val dueDate: LocalDate? = null)

data class ToDoList(val name: ListName, val items: List<ToDoItem>) {
    fun addItem(item: ToDoItem): ToDoList = copy(items = items.except(item) + item)

    private fun List<ToDoItem>.except(item: ToDoItem) =
        filterNot { it.description == item.description }
}

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
