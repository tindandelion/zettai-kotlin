package stories.tooling

import com.ubertob.pesticide.core.*
import org.http4k.client.JettyClient
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.server.Jetty
import org.http4k.server.asServer
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import zettai.core.*
import zettai.main.MapListFetcher
import zettai.main.ToDoListStore
import zettai.main.ZettaiHttpServer
import zettai.main.json.AddItemRequest
import zettai.main.json.AddListRequest
import kotlin.test.fail

interface ZettaiActions : DdtActions<DdtProtocol> {
    fun getToDoList(user: User, listName: ListName): ToDoList?
    fun addListItem(user: User, listName: ListName, item: ToDoItem): ToDoList?
    fun allUserLists(user: User): List<ListName>

    fun ToDoListOwner.`starts with no lists`()
    fun ToDoListOwner.`starts with a list`(listName: String, items: List<String>)
    fun ToDoListOwner.`starts with some lists`(expectedLists: Map<String, List<String>>)
    fun createList(user: User, listName: ListName)
}

abstract class InMemoryListActions : ZettaiActions {
    private val lists: ToDoListStore = mutableMapOf()
    protected val hub =
        ToDoListHub(MapListFetcher(lists), ToDoListCommandHandler(), ToDoListEventStore())

    override fun ToDoListOwner.`starts with a list`(listName: String, items: List<String>) {
        val userLists = lists[user] ?: mutableMapOf()
        val newList = ToDoList(ListName.fromTrusted(listName), items.map(::ToDoItem))
        userLists[newList.name] = newList
        lists[user] = userLists
    }

    override fun ToDoListOwner.`starts with some lists`(expectedLists: Map<String, List<String>>) {
        expectedLists.forEach { (listName, items) -> `starts with a list`(listName, items) }
    }

    override fun ToDoListOwner.`starts with no lists`() {
        lists[user] = mutableMapOf()
    }
}

class DomainOnlyActions : InMemoryListActions() {
    override val protocol: DdtProtocol = DomainOnly
    override fun prepare(): DomainSetUp = Ready

    override fun getToDoList(user: User, listName: ListName): ToDoList? =
        hub.getList(user, listName)

    override fun addListItem(user: User, listName: ListName, item: ToDoItem): ToDoList? =
        hub.addItemToList(user, listName, item)

    override fun allUserLists(user: User): List<ListName> =
        hub.getUserLists(user) ?: fail("User not found: ${user.name}")

    override fun createList(user: User, listName: ListName) {
        hub.handle(CreateToDoList(user, listName))
    }
}


class HttpActions : InMemoryListActions() {
    val zettaiPort = 8081
    val server = ZettaiHttpServer(hub).asServer(Jetty(zettaiPort))
    val client = JettyClient()

    override val protocol: DdtProtocol = Http("local")

    override fun prepare(): DomainSetUp {
        server.start()
        return Ready
    }

    override fun tearDown(): HttpActions = also { server.stop() }

    override fun getToDoList(user: User, listName: ListName): ToDoList? {
        return fetchListFromUrl("/todo/${user.name}/${listName.name}")
    }

    override fun addListItem(user: User, listName: ListName, item: ToDoItem): ToDoList? {
        val request = Body.auto<AddItemRequest>()
            .toLens()
            .inject(
                AddItemRequest(item.description),
                Request(Method.POST, todoListUrl(user, listName))
            )

        val response = client(request)
        if (response.status == Status.NOT_FOUND) return null

        expectThat(response.status).isEqualTo(Status.SEE_OTHER)
        val listUrl = response.header("Location") ?: fail("List url is not found from the response")
        return fetchListFromUrl(listUrl) ?: fail("List ${listName.name} not found")
    }

    override fun allUserLists(user: User): List<ListName> {
        val request = Request(Method.GET, withHost("/todo/${user.name}"))
        val response = client(request)

        expectThat(response.status).isEqualTo(Status.OK)
        return Body.auto<List<ListName>>().toLens().invoke(response)
    }

    override fun createList(user: User, listName: ListName) {
        val request = Body.auto<AddListRequest>()
            .toLens()
            .inject(
                AddListRequest(listName.name),
                Request(Method.POST, withHost("/todo/${user.name}"))
            )
        val response = client(request)
        expectThat(response.status).isEqualTo(Status.CREATED)
    }

    override fun ToDoListOwner.`starts with some lists`(expectedLists: Map<String, List<String>>) {
        expectedLists.forEach { (listName, items) -> `starts with a list`(listName, items) }
    }

    private fun withHost(url: String) = "http://localhost:$zettaiPort$url"

    private fun todoListUrl(user: User, listName: ListName) =
        withHost("/todo/${user.name}/${listName.name}")

    private fun fetchListFromUrl(listUrl: String): ToDoList? {
        val request = Request(Method.GET, appendHostTo(listUrl))
        val response = client(request)

        return when (response.status) {
            Status.OK -> parseResponseJson(response)
            Status.NOT_FOUND -> return null
            else -> fail(response.toMessage())
        }
    }

    private fun parseResponseJson(response: Response): ToDoList {
        return Body.auto<ToDoList>().toLens().invoke(response)
    }

    private fun appendHostTo(listUrl: String): String = "http://localhost:$zettaiPort" + listUrl

}

val allActions = setOf(DomainOnlyActions(), HttpActions())
typealias ZettaiDDT = DomainDrivenTest<ZettaiActions>

