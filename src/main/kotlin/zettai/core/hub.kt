package zettai.core

interface ZettaiHub {
    fun getList(user: User, listName: ListName): ToDoList?
    fun addItemToList(user: User, listName: ListName, item: ToDoItem): ToDoList?
    fun getUserLists(user: User): List<ListName>?
    fun handle(command: ToDoListCommand)
}

interface ToDoListUpdatableFetcher {
    fun getList(user: User, listName: ListName): ToDoList?
    fun assignListToUser(user: User, list: ToDoList): ToDoList?
    fun getAll(user: User): List<ListName>?
}

typealias CommandHandler<Cmd, Evt> = (Cmd) -> List<Evt>?
typealias EventPersister<E> = (List<E>) -> List<E>

class ToDoListCommandHandler : CommandHandler<ToDoListCommand, ToDoListEvent> {
    override fun invoke(cmd: ToDoListCommand): List<ToDoListEvent>? {
        TODO("Not yet implemented")
    }
}

class ToDoListEventStore : EventPersister<ToDoListEvent> {
    override fun invoke(events: List<ToDoListEvent>): List<ToDoListEvent> {
        TODO("Not yet implemented")
    }
}

class ToDoListHub(
    private val fetcher: ToDoListUpdatableFetcher,
    private val commandHandler: CommandHandler<ToDoListCommand, ToDoListEvent>,
    private val persistEvents: EventPersister<ToDoListEvent>
) : ZettaiHub {
    override fun getList(user: User, listName: ListName) = fetcher.getList(user, listName)

    override fun addItemToList(user: User, listName: ListName, item: ToDoItem): ToDoList? =
        fetcher.getList(user, listName)?.run {
            val newList = addItem(item)
            fetcher.assignListToUser(user, newList)
        }

    override fun getUserLists(user: User): List<ListName>? {
        return fetcher.getAll(user)
    }

    override fun handle(command: ToDoListCommand) {
        commandHandler(command)?.let(persistEvents)?.let { command }
    }
}


