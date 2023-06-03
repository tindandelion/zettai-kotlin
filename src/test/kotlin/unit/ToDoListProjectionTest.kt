package unit

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import zettai.core.*

class ToDoListProjectionTest {
    @Test
    fun `findAll returns all the lists of a user`() {
        val user = User("mike")
        val listName1 = ListName.fromTrusted("gardening")
        val listName2 = ListName.fromTrusted("shopping")
        val events = listOf(
            ListCreated(user to listName1),
            ListCreated(user to listName2),
            ListCreated(User("kevin") to ListName.fromTrusted("working"))
        )

        val projection = ToDoListProjection(simpleEventFetcher(events))
        projection.update()
        expectThat(projection.findAll(user)).isEqualTo(listOf(listName1, listName2))
    }

    private fun simpleEventFetcher(events: List<ToDoListEvent>): FetchStoredEvents<ToDoListEvent> =
        { lastEventSeq ->
            events.mapIndexed { i, e ->
                val nextSeq = EventSeq(lastEventSeq.progressive + i + 1)
                StoredEvent(nextSeq, e)
            }.asSequence()
        }
}