package zettai.core

interface EntityEvent {
    val id: EntityId
}

sealed class ToDoListEvent : EntityEvent
data class ListCreated(override val id: ToDoListId, val name: ListName) : ToDoListEvent()
data class ItemAdded(override val id: ToDoListId, val item: ToDoItem) : ToDoListEvent()
