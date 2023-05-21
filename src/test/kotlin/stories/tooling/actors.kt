package stories.tooling

import ListName
import ToDoItem
import ToDoList
import User
import com.ubertob.pesticide.core.DdtActor
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isNotNull
import strikt.assertions.isNull

data class ToDoListOwner(override val name: String) : DdtActor<ZettaiActions>() {
    val user = User(name)

    fun `can see #listname with #itemnames`(
        listName: String,
        expectedItems: List<String>
    ) = step(listName, expectedItems) {
        val list = getToDoList(user, ListName.fromTrusted(listName))
        expectThat(list)
            .isNotNull()
            .itemNames
            .containsExactlyInAnyOrder(expectedItems)
    }

    fun `cannot see #listname`(listName: String) = step(listName) {
        val list = getToDoList(user, ListName.fromTrusted(listName))
        expectThat(list).isNull()
    }

    fun `can add #item to #listname`(itemDesc: String, listName: String) =
        step(itemDesc, listName) {
            val updatedList = addListItem(user, ListName.fromTrusted(listName), ToDoItem(itemDesc))
            expectThat(updatedList)
                .isNotNull()
                .itemNames
                .contains(itemDesc)
        }

    fun `can not add #item to #listname`(itemDesc: String, listName: String) =
        step(itemDesc, listName) {
            val resultList = addListItem(user, ListName.fromTrusted(listName), ToDoItem(itemDesc))
            expectThat(resultList).isNull()
        }

    private val Assertion.Builder<ToDoList>.itemNames
        get() = get { items.map { it.description } }
}
