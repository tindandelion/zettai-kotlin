import org.http4k.client.JettyClient
import org.http4k.core.*
import org.http4k.filter.ClientFilters
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import kotlin.test.fail

interface Actions {
    fun getToDoList(userName: String, listName: String): ToDoList?
}

typealias Step = Actions.() -> Unit

class ApplicationForAT(val client: HttpHandler, val server: AutoCloseable) :
    Actions {
    override fun getToDoList(userName: String, listName: String): ToDoList {
        val request = Request(Method.GET, "/todo/${userName}/${listName}")
        val response = client(request)

        return if (response.status == Status.OK) parseResponse(response.body.toString())
        else fail(response.toMessage())
    }

    fun runScenario(vararg steps: Step) {
        server.use { steps.forEach { step -> step(this) } }
    }

    private fun parseResponse(html: String): ToDoList {
        val listName = ListName(extractListName(html))
        val items = extractItemDescriptions(html).map(::ToDoItem).toList()
        return ToDoList(listName, items)
    }

    private fun extractItemDescriptions(html: String): Sequence<String> {
        val regex = "<td>.*?<".toRegex()
        return regex.findAll(html)
            .map { it.value.substringAfter("<td>").dropLast(1) }
    }

    private fun extractListName(html: String): String {
        val regex = "<h2>.*<".toRegex()
        return regex.find(html)
            ?.value
            ?.substringAfter("<h2>")
            ?.dropLast(1)
            .orEmpty()
    }
}

interface ScenarioActor {
    val name: String
}

class ToDoListOwner(override val name: String) : ScenarioActor {
    fun canSeeList(listName: ListName, items: List<String>): Step = {
        val expectedList = createList(listName.name, items)
        val list = getToDoList(name, listName.name)
        expectThat(list).isEqualTo(expectedList)
    }

    fun cannotSeeList(listName: ListName): Step = {
        expectThrows<AssertionFailedError> {
            getToDoList(name, listName.name)
        }
    }
}

class SeeTodoListAT {
    private val frank = ToDoListOwner("frank")
    private val bob = ToDoListOwner("bob")

    private val shoppingItems = listOf("carrots", "apples", "milk")
    private val frankList = createList("shopping", shoppingItems)

    private val gardenItems = listOf("fix the fence", "mowing the lawn")
    private val bobList = createList("gardening", gardenItems)

    private val lists = mapOf(
        User(frank.name) to listOf(frankList),
        User(bob.name) to listOf(bobList)
    )

    @Test
    fun `List owners can see their lists`() {
        startApplication(lists).runScenario(
            frank.canSeeList(frankList.name, shoppingItems),
            bob.canSeeList(bobList.name, gardenItems)
        )
    }

    @Test
    fun `Only owners can see their lists`() {
        startApplication(lists).runScenario(
            bob.cannotSeeList(frankList.name),
            frank.cannotSeeList(bobList.name)
        )
    }
}

private fun createList(listName: String, items: List<String>) =
    ToDoList(ListName(listName), items.map(::ToDoItem))

fun startApplication(lists: Map<User, List<ToDoList>>): ApplicationForAT {
    val port = 8081
    val client = ClientFilters
        .SetBaseUriFrom(Uri.of("http://localhost:$port"))
        .then(JettyClient())
    val server = Zettai(ToDoListHub(lists)).asServer(Jetty(port)).start()
    return ApplicationForAT(client, server)
}
