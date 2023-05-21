import com.ubertob.pesticide.core.DDT
import tooling.ToDoListOwner
import tooling.ZettaiDDT
import tooling.allActions

class ModifyTodoListDDT : ZettaiDDT(allActions) {
    val ann by NamedActor(::ToDoListOwner)
    val frank by NamedActor(::ToDoListOwner)

    @DDT
    fun `Owner can add new items`() = ddtScenario {
        setUp {
            ann.`starts with a list`("diy", emptyList())
        }.thenPlay(
            ann.`can add #item to #listname`("paint the shelf", "diy"),
            ann.`can add #item to #listname`("fix the gate", "diy"),
            ann.`can add #item to #listname`("change the lock", "diy"),

            ann.`can see #listname with #itemnames`(
                "diy", listOf(
                    "fix the gate", "change the lock", "paint the shelf"
                )
            )
        )
    }

    @DDT
    fun `Adding duplicate items keeps only one item in the list`() = ddtScenario {
        val duplicateItem = "paint the shelf"
        setUp {
            ann.`starts with a list`("diy", listOf("fix the gate", duplicateItem))
        }.thenPlay(
            ann.`can add #item to #listname`(duplicateItem, "diy"),
            ann.`can see #listname with #itemnames`(
                "diy", listOf("fix the gate", "paint the shelf")
            )
        )
    }

    @DDT
    fun `Cannot add items to a non-existent list`() = ddtScenario {
        setUp {
            ann.`starts with a list`("diy", emptyList())
        }.thenPlay(
            ann.`can not add #item to #listname`("paint the shelf", "other-list"),
        )
    }

    @DDT
    fun `Non-owner can not update the other's list`() = ddtScenario {
        setUp {
            ann.`starts with a list`("diy", listOf("fix the fence"))
        }.thenPlay(
            frank.`can not add #item to #listname`("paint the shelf", "diy"),
            ann.`can see #listname with #itemnames`("diy", listOf("fix the fence"))
        )
    }
}