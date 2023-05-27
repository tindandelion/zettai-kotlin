package unit

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import zettai.core.*
import zettai.main.MapListFetcher

class ToDoListCommandHandlerTest {
    private val store = ToDoListEventStore()
    private val handler =
        ToDoListCommandHandler(store::retrieveByName, MapListFetcher(mutableMapOf()))
    private val user = User("alice")
    private val listName = ListName.fromTrusted("gardening")

    @Test
    fun `creates a new todo list`() {
        val cmd = CreateToDoList(user, listName)
        val events = handle(cmd)
        expectThat(events)
            .isNotNull()
            .containsExactly(ListCreated(cmd.user to cmd.list))
    }

    @Test
    fun `fails to create a duplicate list`() {
        val cmd = CreateToDoList(user, listName)
        handle(cmd)

        val duplicate = handle(cmd)
        expectThat(duplicate).isNull()
    }

    private fun handle(cmd: CreateToDoList) =
        handler(cmd)?.let { store.receiveEvents(it) }
}