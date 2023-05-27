package zettai.core

sealed class ToDoListCommand
data class CreateToDoList(val user: User, val list: ListName) : ToDoListCommand()
data class AddToDoItem(val user: User, val list: ListName, val item: ToDoItem) : ToDoListCommand()