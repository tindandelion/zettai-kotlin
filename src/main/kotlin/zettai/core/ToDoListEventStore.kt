package zettai.core

private class EventStreamer {
    var events: List<ToDoListEvent> = emptyList()

    fun store(newEvents: Iterable<ToDoListEvent>) {
        events += newEvents
    }

    fun getEvents(user: User, listName: ListName): Iterable<ToDoListEvent> =
        (user to listName).let { events.filter { evt -> evt.list == it } }
}


class ToDoListEventStore {
    private val streamer = EventStreamer()

    fun retrieveByName(user: User, listName: ListName): ToDoListState {
        return streamer.getEvents(user, listName)
            .fold(InitialState as ToDoListState) { state, event -> event.applyTo(state) }
    }

    fun receiveEvents(events: Iterable<ToDoListEvent>): Iterable<ToDoListEvent> {
        streamer.store(events)
        return events
    }
}