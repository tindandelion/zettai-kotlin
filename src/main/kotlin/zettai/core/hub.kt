package zettai.core

interface ZettaiHub {
    fun getList(user: User, listName: ListName): ToDoList?
    fun addItemToList(user: User, listName: ListName, item: ToDoItem): ToDoList?
    fun getUserLists(user: User): List<ListName>?
    fun createList(user: User, listName: ListName)
}

typealias ToDoListFetcher = (User, ListName) -> ToDoList?

interface ToDoListUpdatableFetcher : ToDoListFetcher {
    fun assignListToUser(user: User, list: ToDoList): ToDoList?
    fun getAll(user: User): List<ListName>?
}

class ToDoListHub(private val fetcher: ToDoListUpdatableFetcher) : ZettaiHub {
    override fun getList(user: User, listName: ListName) = fetcher(user, listName)

    override fun addItemToList(user: User, listName: ListName, item: ToDoItem): ToDoList? =
        fetcher(user, listName)?.run {
            val newList = addItem(item)
            fetcher.assignListToUser(user, newList)
        }

    override fun getUserLists(user: User): List<ListName>? {
        return fetcher.getAll(user)
    }

    override fun createList(user: User, listName: ListName) {
        TODO("Not yet implemented")
    }
}
