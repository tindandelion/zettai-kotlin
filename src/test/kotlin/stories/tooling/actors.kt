package stories.tooling

import com.ubertob.pesticide.core.DdtActor
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.*
import zettai.core.ListName
import zettai.core.ToDoItem
import zettai.core.ToDoList
import zettai.core.User

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

    fun `can not see any lists`() = step {
        val lists = allUserLists(user)
        expectThat(lists).isEmpty()
    }

    fun `can see the lists #listNames`(listNames: Set<String>) = step(listNames) {
        val lists = allUserLists(user)
        expectThat(lists).map { it.name }.containsExactly(listNames)
    }

    private val Assertion.Builder<ToDoList>.itemNames
        get() = get { items.map { it.description } }
}
