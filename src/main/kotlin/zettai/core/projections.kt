package zettai.core

import java.util.concurrent.atomic.AtomicReference

typealias FetchStoredEvents<E> = (EventSeq) -> Sequence<StoredEvent<E>>
typealias ProjectEvents<Row, Evt> = (Evt) -> List<DeltaRow<Row>>

data class EventSeq(val progressive: Int) {
    operator fun compareTo(other: EventSeq): Int = progressive.compareTo(other.progressive)
}

data class RowId(val id: String)
data class StoredEvent<E : EntityEvent>(val eventSeq: EventSeq, val event: E)

sealed class DeltaRow<Row>
data class CreateRow<Row>(val rowId: RowId, val row: Row) : DeltaRow<Row>()
data class DeleteRow<Row>(val rowId: RowId) : DeltaRow<Row>()
data class UpdateRow<Row>(val rowId: RowId, val updateRow: Row.() -> Row) : DeltaRow<Row>()

interface Projection<Row, Evt : EntityEvent> {
    val eventProjector: ProjectEvents<Row, Evt>
    val eventFetcher: FetchStoredEvents<Evt>
    fun lastProjectedEvent(): EventSeq
    fun update() {
        eventFetcher(lastProjectedEvent()).forEach { storedEvent ->
            applyDelta(
                storedEvent.eventSeq,
                eventProjector(storedEvent.event)
            )
        }
    }

    fun applyDelta(eventSeq: EventSeq, deltas: List<DeltaRow<Row>>)
}

interface InMemoryProjection<Row, Evt : EntityEvent> : Projection<Row, Evt> {
    fun allRows(): Map<RowId, Row>
}

data class ConcurrentMapProjection<Row, Evt : EntityEvent>(
    override val eventFetcher: FetchStoredEvents<Evt>,
    override val eventProjector: ProjectEvents<Row, Evt>
) : InMemoryProjection<Row, Evt> {
    private val lastEventRef: AtomicReference<EventSeq> = AtomicReference(EventSeq(-1))
    private val rowsRef: AtomicReference<Map<RowId, Row>> = AtomicReference(emptyMap())

    override fun lastProjectedEvent(): EventSeq = lastEventRef.get()
    override fun allRows(): Map<RowId, Row> = rowsRef.get()

    override fun applyDelta(eventSeq: EventSeq, deltas: List<DeltaRow<Row>>) {
        deltas.forEach {
            rowsRef.getAndUpdate { rows -> applySingleDelta(rows, it) }
        }.also { lastEventRef.getAndSet(eventSeq) }
    }

    private fun applySingleDelta(rows: Map<RowId, Row>, delta: DeltaRow<Row>): Map<RowId, Row> {
        return when (delta) {
            is CreateRow -> rows + (delta.rowId to delta.row)
            is DeleteRow -> rows - delta.rowId
            is UpdateRow -> rows[delta.rowId]
                ?.let { oldRow ->
                    val newRow = (delta.rowId to delta.updateRow(oldRow))
                    return rows - delta.rowId + newRow
                } ?: rows
        }
    }
}

data class ToDoListProjectionRow(val user: User, val list: ToDoList) {
    fun addItem(item: ToDoItem): ToDoListProjectionRow =
        copy(list = list.addItem(item))
}

fun eventProjector(e: ToDoListEvent): List<DeltaRow<ToDoListProjectionRow>> {
    val rowId = "${e.list.first.name}-${e.list.second.name}"
    val (user, listName) = e.list
    return listOf(
        when (e) {
            is ListCreated -> CreateRow(
                RowId(rowId),
                ToDoListProjectionRow(user, ToDoList(listName, emptyList()))
            )

            is ItemAdded -> UpdateRow(RowId(rowId)) { addItem(e.item) }
        }
    )
}

class ToDoListProjection(eventFetcher: FetchStoredEvents<ToDoListEvent>) :
    InMemoryProjection<ToDoListProjectionRow, ToDoListEvent>
    by ConcurrentMapProjection(eventFetcher, ::eventProjector) {

    fun findAll(user: User): List<ListName> =
        allRows().values
            .filter { it.user == user }
            .map { it.list.name }
}
