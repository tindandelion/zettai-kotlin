import org.http4k.client.JettyClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.test.fail

interface ScenarioActor {
    val name: String
}

class ToDoListOwner(override val name: String) : ScenarioActor {
    fun canSeeList(listName: String, items: List<String>) {
        val expectedList = createList(listName, items)
        val list = getToDoList(this.name, listName)
        expectThat(list).isEqualTo(expectedList)
    }


    private fun getToDoList(userName: String, listName: String): ToDoList {
        val client = JettyClient()
        val request = Request(
            Method.GET,
            "http://localhost:8081/todo/${userName}/${listName}"
        )
        val response = client(request)
        return if (response.status == Status.OK) parseResponse(response.body.toString())
        else fail(response.toMessage())
    }
}

private fun createList(listName: String, items: List<String>) =
    ToDoList(ListName(listName), items.map(::ToDoItem))


class SeeTodoListAT {
    @Test
    fun `List owners can see their lists`() {
        val listName = "shopping"
        val foodToBuy = listOf("carrots", "apples", "milk")
        val frank = ToDoListOwner("frank")

        startApplication(frank.name, listName, foodToBuy)
        frank.canSeeList(listName, foodToBuy)
    }
}

fun startApplication(
    user: String,
    listName: String,
    todoItems: List<String>
) {
    val lists = mapOf(User(user) to listOf(createList(listName, todoItems)))
    Zettai(lists).asServer(Jetty(8081)).start()
}

fun parseResponse(html: String): ToDoList {
    val listName = ListName(extractListName(html))
    val items = extractItemDescriptions(html).map(::ToDoItem).toList()
    return ToDoList(listName, items)
}

fun extractItemDescriptions(html: String): Sequence<String> {
    val regex = "<td>.*?<".toRegex()
    return regex.findAll(html)
        .map { it.value.substringAfter("<td>").dropLast(1) }
}

fun extractListName(html: String): String {
    val regex = "<h2>.*<".toRegex()
    return regex.find(html)
        ?.value
        ?.substringAfter("<h2>")
        ?.dropLast(1)
        .orEmpty()
}



