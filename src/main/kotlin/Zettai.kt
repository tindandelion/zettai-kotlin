data class User(val name: String)
data class ListName(val name: String)
data class ToDoItem(val description: String)
data class ToDoList(val name: ListName, val items: List<ToDoItem>)

interface ZettaiHub {
    fun getList(user: User, listName: ListName): ToDoList?
}

class ToDoListHub(private val lists: Map<User, List<ToDoList>>) : ZettaiHub {
    override fun getList(user: User, listName: ListName): ToDoList? {
        return lists[user]?.firstOrNull { it.name == listName }
    }
}


