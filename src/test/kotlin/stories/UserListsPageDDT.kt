package stories

import com.ubertob.pesticide.core.DDT
import stories.tooling.ToDoListOwner
import stories.tooling.ZettaiDDT
import stories.tooling.allActions

class UserListsPageDDT : ZettaiDDT(allActions()) {
    val carol by NamedActor(::ToDoListOwner)
    val emma by NamedActor(::ToDoListOwner)

    @DDT
    fun `new users have no lists`() = ddtScenario {
        setUp {
            emma.`starts with no lists`()
        }.thenPlay(emma.`can not see any lists`())
    }

    @DDT
    fun `only owners can see all their lists`() = ddtScenario {
        val expectedLists = generateSomeTodoLists()
        setUp {
            emma.`starts with no lists`()
            carol.`starts with some lists`(expectedLists)
        }.thenPlay(
            emma.`can not see any lists`(),
            carol.`can see the lists #listNames`(expectedLists.keys),
        )
    }

    private fun generateSomeTodoLists() = mapOf(
        "work" to listOf("meeting", "spreadsheet"),
        "home" to listOf("buy food"),
        "friends" to listOf("buy present", "book restaurant")
    )
}