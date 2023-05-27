package unit

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import zettai.core.*

class ToDoListEventStoreTest {
    val user = User("jack")
    val listName = ListName.fromTrusted("gardening")
    val store = ToDoListEventStore()

    @Test
    fun `returns initial state for a new list`() {
        val listState = store.retrieveByName(user, listName)
        expectThat(listState).isEqualTo(InitialState)
    }

    @Test
    fun `stores list creation event`() {
        store.receiveEvents(listOf(ListCreated(user to listName)))

        val listState = store.retrieveByName(user, listName)
        expectThat(listState).isEqualTo(ActiveToDoList(listName, emptyList()))
    }
}