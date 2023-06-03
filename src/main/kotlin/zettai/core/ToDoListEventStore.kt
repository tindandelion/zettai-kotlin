package zettai.core

private class EventStreamer {
    var events: List<ToDoListEvent> = emptyList()

    fun append(newEvents: Iterable<ToDoListEvent>) {
        events += newEvents
    }

    fun getEvents(user: User, listName: ListName): Iterable<ToDoListEvent> =
        (user to listName).let { events.filter { evt -> evt.list == it } }

    fun fetchFrom(firstEventId: Int): Sequence<ToDoListEvent> =
        events.asSequence().drop(firstEventId)
}


class ToDoListEventStore {
    private val streamer = EventStreamer()

    fun retrieveByName(user: User, listName: ListName): ToDoListState {
        return streamer.getEvents(user, listName)
            .fold(InitialState as ToDoListState) { state, event -> event.applyTo(state) }
    }

    fun receiveEvents(events: Iterable<ToDoListEvent>): Iterable<ToDoListEvent> {
        streamer.append(events)
        return events
    }

    fun fetchEvents(eventSeq: EventSeq): Sequence<StoredEvent<ToDoListEvent>> {
        val nextEventId = eventSeq.progressive + 1
        return streamer.fetchFrom(nextEventId)
            .mapIndexed { index, event -> StoredEvent(EventSeq(nextEventId + index), event) }
    }
}