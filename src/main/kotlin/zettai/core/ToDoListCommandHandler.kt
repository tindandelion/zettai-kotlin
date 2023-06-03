package zettai.core

sealed class ToDoListState
object InitialState : ToDoListState()
data class ActiveToDoList(
    val name: ListName,
    val items: List<ToDoItem>
) : ToDoListState()

data class InconsistentListState(override val msg: String) : ZettaiError()

typealias ToDoListRetriever = (User, ListName) -> ToDoListState?

class ToDoListCommandHandler(
    private val retriever: ToDoListRetriever
) :
    CommandHandler<ToDoListCommand, ToDoListEvent> {
    override fun invoke(cmd: ToDoListCommand): CommandOutcome<ToDoListEvent> {
        return when (cmd) {
            is CreateToDoList -> cmd.execute()
            is AddToDoItem -> cmd.execute()
        }
    }

    private fun CreateToDoList.execute(): CommandOutcome<ToDoListEvent> {
        val currentState = retriever(user, list)
        return if (currentState == InitialState) {
            ListCreated(user to list).asCommandSuccess()
        } else InconsistentListState("Unable to create list").asFailure()
    }

    private fun AddToDoItem.execute(): CommandOutcome<ToDoListEvent> {
        val currentState = retriever(user, list)
        return if (currentState is ActiveToDoList)
            ItemAdded(user to list, item).asCommandSuccess()
        else InconsistentListState("Unable to add item to the list").asFailure()
    }

    private fun ToDoListEvent.asCommandSuccess(): CommandOutcome<ToDoListEvent> =
        listOf(this).asSuccess()
}

