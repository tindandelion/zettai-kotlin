package unit

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import zettai.core.*
import kotlin.test.fail

class ToDoListCommandHandlerTest {
    private val store = ToDoListEventStore()
    private val handler =
        ToDoListCommandHandler(store::retrieveByName)
    private val user = User("alice")
    private val listName = ListName.fromTrusted("gardening")

    @Test
    fun `creates a new todo list`() {
        val cmd = CreateToDoList(user, listName)
        val events = handle(cmd).onFailure { fail() }
        expectThat(events).containsExactly(ListCreated(cmd.user to cmd.list))
    }

    @Test
    fun `fails to create a duplicate list`() {
        val cmd = CreateToDoList(user, listName)
        handle(cmd)
        handle(cmd).onFailure { return }
        fail("Expected a command to fail")
    }

    private fun handle(cmd: CreateToDoList) =
        handler(cmd).transform { store.receiveEvents(it) }
}