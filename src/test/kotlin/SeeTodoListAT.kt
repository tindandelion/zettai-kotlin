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


class SeeTodoListAT {
    @Test
    fun `List owners can see their lists`() {
        val user = User("frank")
        val listName = ListName("shopping")
        val foodToBuy = listOf(
            ToDoItem("carrots"),
            ToDoItem("apples"),
            ToDoItem("milk")
        )

        startApplication(user, listName, foodToBuy)

        val list = getToDoList(user, listName)
        expectThat(list.name).isEqualTo(listName)
        expectThat(list.items).isEqualTo(foodToBuy)
    }
}

fun startApplication(
    user: User,
    listName: ListName,
    foodToBuy: List<ToDoItem>
) {
    val lists = mapOf(user to listOf(ToDoList(listName, foodToBuy)))
    val server = Zettai(lists).asServer(Jetty(8081)).start()
}

fun getToDoList(user: User, listName: ListName): ToDoList {
    val client = JettyClient()
    val request = Request(
        Method.GET,
        "http://localhost:8081/todo/${user.name}/${listName.name}"
    )
    val response = client(request)
    return if (response.status == Status.OK) parseResponse(response.body.toString())
    else fail(response.toMessage())
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



