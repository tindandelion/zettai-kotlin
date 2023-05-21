package tooling

import ListName
import MapListFetcher
import ToDoItem
import ToDoList
import ToDoListHub
import ToDoListStore
import User
import Zettai
import com.ubertob.pesticide.core.*
import org.http4k.client.JettyClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.body.toBody
import org.http4k.server.Jetty
import org.http4k.server.asServer
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.test.fail

interface ZettaiActions : DdtActions<DdtProtocol> {
    fun getToDoList(user: User, listName: ListName): ToDoList?
    fun addListItem(user: User, listName: ListName, item: ToDoItem): ToDoList?
    fun ToDoListOwner.`starts with a list`(listName: String, items: List<String>)
}

class DomainOnlyActions : ZettaiActions {
    private val lists: ToDoListStore = mutableMapOf()
    private val hub = ToDoListHub(MapListFetcher(lists))

    override val protocol: DdtProtocol = DomainOnly
    override fun prepare(): DomainSetUp = Ready

    override fun getToDoList(user: User, listName: ListName): ToDoList? =
        hub.getList(user, listName)

    override fun addListItem(user: User, listName: ListName, item: ToDoItem): ToDoList? =
        hub.addItemToList(user, listName, item)

    override fun ToDoListOwner.`starts with a list`(listName: String, items: List<String>) {
        val newList = ToDoList(ListName(listName), items.map(::ToDoItem))
        lists[user] = mutableMapOf(newList.name to newList)
    }
}

class HttpActions : ZettaiActions {
    private val lists: ToDoListStore = mutableMapOf()
    private val hub = ToDoListHub(MapListFetcher(lists))

    val zettaiPort = 8081
    val server = Zettai(hub).asServer(Jetty(zettaiPort))
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
        val formData = listOf(
            "itemname" to item.description,
            "itemdue" to item.dueDate?.toString()
        )
        val request = Request(Method.POST, todoListUrl(user, listName)).body(formData.toBody())

        val response = client(request)
        if (response.status == Status.NOT_FOUND) return null

        expectThat(response.status).isEqualTo(Status.SEE_OTHER)
        val listUrl = response.header("Location") ?: fail("List url is not found from the response")
        return fetchListFromUrl(listUrl) ?: fail("List ${listName.name} not found")
    }

    override fun ToDoListOwner.`starts with a list`(listName: String, items: List<String>) {
        val newList = ToDoList(ListName(listName), items.map(::ToDoItem))
        lists[user] = mutableMapOf(newList.name to newList)
    }

    private fun todoListUrl(user: User, listName: ListName) =
        "http://localhost:$zettaiPort/todo/${user.name}/${listName.name}"

    private fun fetchListFromUrl(listUrl: String): ToDoList? {
        val request = Request(Method.GET, appendHostTo(listUrl))
        val response = client(request)

        return when (response.status) {
            Status.OK -> parseResponse(response.body.toString())
            Status.NOT_FOUND -> return null
            else -> fail(response.toMessage())
        }
    }

    private fun appendHostTo(listUrl: String): String = "http://localhost:$zettaiPort" + listUrl

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

val allActions = setOf(DomainOnlyActions(), HttpActions())
typealias ZettaiDDT = DomainDrivenTest<ZettaiActions>

