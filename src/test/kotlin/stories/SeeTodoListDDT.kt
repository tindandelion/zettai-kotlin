package stories

import com.ubertob.pesticide.core.DDT
import stories.tooling.ToDoListOwner
import stories.tooling.ZettaiDDT
import stories.tooling.allActions

class SeeTodoListDDT : ZettaiDDT(allActions()) {
    val frank by NamedActor(::ToDoListOwner)
    val bob by NamedActor(::ToDoListOwner)
    val dylan by NamedActor(::ToDoListOwner)

    val shoppingListName = "shopping"
    val shoppingItems = listOf("carrots", "apples", "milk")

    val gardenListName = "gardening"
    val gardenItems = listOf("fix the fence", "mowing the lawn")

    @DDT
    fun `List owners can see their lists`() = ddtScenario {
        setUp {
            frank.`starts with a list`(shoppingListName, shoppingItems)
            bob.`starts with a list`(gardenListName, gardenItems)
        }.thenPlay(
            frank.`can see #listname with #itemnames`(shoppingListName, shoppingItems),
            bob.`can see #listname with #itemnames`(gardenListName, gardenItems)
        )
    }

    @DDT
    fun `Only list owner can see their lists`() = ddtScenario {
        setUp {
            frank.`starts with a list`(shoppingListName, shoppingItems)
            bob.`starts with a list`(gardenListName, gardenItems)
        }.thenPlay(
            frank.`cannot see #listname`(gardenListName),
            bob.`cannot see #listname`(shoppingListName)
        )
    }

    @DDT
    fun `users can create new lists`() = ddtScenario {
        setUp {
            dylan.`starts with no lists`()
        }.thenPlay(
            dylan.`can not see any lists`(),
            dylan.`can create a new list called #listname`("gardening"),
            dylan.`can create a new list called #listname`("music"),
            dylan.`can see the lists #listNames`(setOf("gardening", "music"))
        )
    }

    @DDT
    fun `users can not create duplicate lists`() = ddtScenario {
        setUp {
        }.thenPlay(
            dylan.`can create a new list called #listname`("shopping"),
            dylan.`can not create a new list called #listname`("shopping"),
        )
    }
}