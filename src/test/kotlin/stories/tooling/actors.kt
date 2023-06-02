package stories.tooling

import com.ubertob.pesticide.core.DdtActor
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.*
import zettai.core.*
import kotlin.test.fail

data class ToDoListOwner(override val name: String) : DdtActor<ZettaiActions>() {
    val user = User(name)

    fun `can see #listname with #itemnames`(
        listName: String,
        expectedItems: List<String>
    ) = step(listName, expectedItems) {
        val list = getToDoList(user, ListName.fromTrusted(listName)).expectSuccess()
        expectThat(list).itemNames.containsExactlyInAnyOrder(expectedItems)
    }

    fun `cannot see #listname`(listName: String) = step(listName) {
        val outcome = getToDoList(user, ListName.fromTrusted(listName)).expectFailure()
        expectThat(outcome).isA<InvalidRequest>()
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
        val lists = allUserLists(user).expectSuccess()
        expectThat(lists).isEmpty()
    }

    fun `can see the lists #listNames`(listNames: Set<String>) = step(listNames) {
        val lists = allUserLists(user).expectSuccess()
        expectThat(lists).map { it.name }.containsExactly(listNames)
    }

    fun `can create a new list called #listname`(listName: String) = step(listName) {
        val result = createList(user, ListName.fromTrusted(listName))
        expectThat(result).isTrue()
    }

    fun `can not create a new list called #listname`(listName: String) = step(listName) {
        expectThat(createList(user, ListName.fromTrusted(listName))).isFalse()
    }

    private val Assertion.Builder<ToDoList>.itemNames
        get() = get { items.map { it.description } }

}

private fun <T> ZettaiOutcome<T>.expectFailure(): ZettaiError {
    this.onFailure { error -> return error }
    fail("Expected failure but was $this")
}

private fun <T> ZettaiOutcome<T>.expectSuccess(): T =
    this.onFailure { error -> fail("$this expected success but was $error") }

