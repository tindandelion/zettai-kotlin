package tooling

import ListName
import ToDoItem
import ToDoList
import ToDoListHub
import User
import Zettai
import com.ubertob.pesticide.core.*
import org.http4k.client.JettyClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.server.Jetty
import org.http4k.server.asServer
import kotlin.test.fail

interface ZettaiActions : DdtActions<DdtProtocol> {
    fun getToDoList(user: User, listName: ListName): ToDoList?
    fun ToDoListOwner.`starts with a list`(listName: String, items: List<String>)
}

class DomainOnlyActions : ZettaiActions {
    private val lists: MutableMap<User, List<ToDoList>> = mutableMapOf()
    private val hub = ToDoListHub(lists)

    override val protocol: DdtProtocol = DomainOnly
    override fun prepare(): DomainSetUp = Ready

    override fun getToDoList(user: User, listName: ListName): ToDoList? =
        hub.getList(user, listName)

    override fun ToDoListOwner.`starts with a list`(listName: String, items: List<String>) {
        val newList = ToDoList(ListName(listName), items.map(::ToDoItem))
        lists[user] = listOf(newList)
    }
}

class HttpActions : ZettaiActions {
    private val lists: MutableMap<User, List<ToDoList>> = mutableMapOf()
    private val hub = ToDoListHub(lists)

    val zettaiPort = 8081
    val server = Zettai(hub).asServer(Jetty(zettaiPort))
    val client = JettyClient()

    override val protocol: DdtProtocol = Http("local")

    override fun getToDoList(user: User, listName: ListName): ToDoList? {
        val request =
            Request(Method.GET, "http://localhost:$zettaiPort/todo/${user.name}/${listName.name}")
        val response = client(request)

        return when (response.status) {
            Status.OK -> parseResponse(response.body.toString())
            Status.NOT_FOUND -> return null
            else -> fail(response.toMessage())
        }
    }

    override fun prepare(): DomainSetUp {
        server.start()
        return Ready
    }

    override fun tearDown(): HttpActions = also { server.stop() }

    override fun ToDoListOwner.`starts with a list`(listName: String, items: List<String>) {
        val newList = ToDoList(ListName(listName), items.map(::ToDoItem))
        lists[user] = listOf(newList)
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

val allActions = setOf(DomainOnlyActions(), HttpActions())
typealias ZettaiDDT = DomainDrivenTest<ZettaiActions>

