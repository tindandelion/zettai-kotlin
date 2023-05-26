package zettai.core

sealed class ToDoListCommand

data class CreateToDoList(val user: User, val listName: ListName) : ToDoListCommand()
data class AddToDoItem(val user: User, val name: ListName, val item: ToDoItem) : ToDoListCommand()