package zettai.core

sealed class ZettaiError : OutcomeError
data class ListNotFound(override val msg: String) : ZettaiError()

typealias ZettaiOutcome<T> = Outcome<ZettaiError, T>

interface ZettaiHub {
    fun getList(user: User, listName: ListName): ZettaiOutcome<ToDoList>
    fun getUserLists(user: User): ZettaiOutcome<List<ListName>>
    fun handle(command: ToDoListCommand): ZettaiOutcome<ToDoListCommand>
}

typealias CommandOutcome<Evt> = ZettaiOutcome<List<Evt>>
typealias CommandHandler<Cmd, Evt> = (Cmd) -> CommandOutcome<Evt>
typealias EventPersister<Evt> = (Iterable<Evt>) -> Iterable<Evt>

class ToDoListHub(
    private val queryRunner: ToDoListQueryRunner,
    private val commandHandler: CommandHandler<ToDoListCommand, ToDoListEvent>,
    private val persistEvents: EventPersister<ToDoListEvent>
) : ZettaiHub {
    override fun getList(user: User, listName: ListName): Outcome<ListNotFound, ToDoList> =
        queryRunner {
            listProjection
                .findList(user, listName)
                .failIfNull(ListNotFound("No lists found"))
        }.runIt()

    override fun getUserLists(user: User): ZettaiOutcome<List<ListName>> =
        queryRunner {
            listProjection
                .findAll(user)
                .failIfNull(ListNotFound("No lists found"))
        }.runIt()

    override fun handle(command: ToDoListCommand): ZettaiOutcome<ToDoListCommand> =
        commandHandler(command).transform(persistEvents).transform { command }
}


