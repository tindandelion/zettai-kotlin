package zettai.core

typealias ListIdentifier = Pair<User, ListName>

interface EntityEvent {
    val list: ListIdentifier
}


sealed class ToDoListEvent : EntityEvent {
    abstract fun applyTo(state: ToDoListState): ToDoListState
}

data class ListCreated(override val list: ListIdentifier) :
    ToDoListEvent() {
    override fun applyTo(state: ToDoListState): ToDoListState =
        when (state) {
            is InitialState -> ActiveToDoList(list.second, emptyList())
            else -> state
        }
}

data class ItemAdded(
    override val list: ListIdentifier,
    val item: ToDoItem
) : ToDoListEvent() {
    override fun applyTo(state: ToDoListState): ToDoListState =
        when (state) {
            is ActiveToDoList -> ActiveToDoList(list.second, state.items + item)
            else -> state
        }
}
