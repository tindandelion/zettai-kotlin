package zettai.core

interface ZettaiHub {
    fun getList(user: User, listName: ListName): ToDoList?
    fun getUserLists(user: User): List<ListName>?
    fun handle(command: ToDoListCommand): ToDoListCommand?
}

interface ToDoListUpdatableFetcher {
    fun getList(user: User, listName: ListName): ToDoList?
    fun assignListToUser(user: User, list: ToDoList): ToDoList?
    fun getAll(user: User): List<ListName>?
    fun addItemToList(user: User, listName: ListName, item: ToDoItem): ToDoList?
}

typealias CommandHandler<Cmd, Evt> = (Cmd) -> List<Evt>?
typealias EventPersister<Evt> = (Iterable<Evt>) -> Iterable<Evt>

class ToDoListHub(
    private val fetcher: ToDoListUpdatableFetcher,
    private val commandHandler: CommandHandler<ToDoListCommand, ToDoListEvent>,
    private val persistEvents: EventPersister<ToDoListEvent>
) : ZettaiHub {
    override fun getList(user: User, listName: ListName) = fetcher.getList(user, listName)

    override fun getUserLists(user: User): List<ListName>? {
        return fetcher.getAll(user)
    }

    override fun handle(command: ToDoListCommand): ToDoListCommand? =
        commandHandler(command)?.let(persistEvents)?.let { command }
}


