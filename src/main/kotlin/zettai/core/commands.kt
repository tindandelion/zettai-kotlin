package zettai.core

import java.util.*

data class EntityId(val raw: UUID) {
    companion object {
        fun mint() = EntityId(UUID.randomUUID())
    }
}
typealias ToDoListId = EntityId

sealed class ToDoListCommand
data class CreateToDoList(val user: User, val listName: ListName) : ToDoListCommand()
data class AddToDoItem(val user: User, val name: ListName, val item: ToDoItem) : ToDoListCommand()