package zettai.core

sealed class ZettaiError : OutcomeError
data class ListNotFound(override val msg: String) : ZettaiError()
data class CommandError(override val msg: String) : ZettaiError()

typealias ZettaiOutcome<T> = Outcome<ZettaiError, T>

interface ZettaiHub {
    fun getList(user: User, listName: ListName): ZettaiOutcome<ToDoList>
    fun getUserLists(user: User): ZettaiOutcome<List<ListName>>
    fun handle(command: ToDoListCommand): ZettaiOutcome<ToDoListCommand>
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
    override fun getList(user: User, listName: ListName) =
        fetcher.getList(user, listName).failIfNull(ListNotFound("List not found"))

    override fun getUserLists(user: User): ZettaiOutcome<List<ListName>> {
        return fetcher.getAll(user).failIfNull(ListNotFound("No lists found"))
    }

    override fun handle(command: ToDoListCommand): ZettaiOutcome<ToDoListCommand> =
        commandHandler(command)?.let(persistEvents)
            .failIfNull(CommandError("Command error"))
            .transform { command }
}


