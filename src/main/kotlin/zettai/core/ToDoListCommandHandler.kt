package zettai.core

sealed class ToDoListState
object InitialState : ToDoListState()
data class ActiveToDoList(
    val name: ListName,
    val items: List<ToDoItem>
) : ToDoListState()

typealias ToDoListRetriever = (User, ListName) -> ToDoListState?

class ToDoListCommandHandler(
    private val retriever: ToDoListRetriever,
    private val readModel: ToDoListUpdatableFetcher
) :
    CommandHandler<ToDoListCommand, ToDoListEvent> {
    override fun invoke(cmd: ToDoListCommand): List<ToDoListEvent>? {
        return when (cmd) {
            is CreateToDoList -> cmd.execute()
            else -> null
        }
    }

    private fun CreateToDoList.execute(): List<ListCreated>? {
        val currentState = retriever(user, list)
        return if (currentState == InitialState) {
            readModel.assignListToUser(user, ToDoList(list, emptyList()))
            listOf(ListCreated(user to list))
        } else null
    }
}