package zettai.main

import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import zettai.core.*
import zettai.main.json.AddItemRequest
import zettai.main.json.AddListRequest

object Responses {
    val badRequest = Response(Status.BAD_REQUEST)
    val notFound = Response(Status.NOT_FOUND)
    fun seeOther(location: String) = Response(Status.SEE_OTHER).header("Location", location)
}

class CreateNewList(private val hub: ZettaiHub) : HttpHandler {
    override fun invoke(request: Request): Response {
        val user = request.extractUser() ?: return Responses.badRequest
        val listName = extractListName(request) ?: return Responses.badRequest

        return hub.handle(CreateToDoList(user, listName))
            ?.let { Response(Status.CREATED) }
            ?: Responses.badRequest
    }

    private fun extractListName(request: Request) =
        Body.auto<AddListRequest>()
            .toLens()
            .invoke(request)
            .let { ListName.fromUntrusted(it.listName) }
}

class AddNewItem(private val hub: ZettaiHub) : HttpHandler {
    override fun invoke(request: Request): Response {
        val user = request.extractUser() ?: return Responses.badRequest
        val listName = request.path("list")
            ?.let { ListName.fromUntrusted(it) }
            ?: return Responses.badRequest
        val body = Body.auto<AddItemRequest>()
            .toLens()
            .invoke(request)

        return hub.handle(AddToDoItem(user, listName, ToDoItem(body.itemName)))
            ?.let { Responses.seeOther("/todo/${user.name}/${listName.name}") }
            ?: Responses.notFound
    }

}

private fun Request.extractUser(): User? = path("user")?.let(::User)


class ZettaiHttpServer(private val hub: ZettaiHub) : HttpHandler {
    val routes = routes(
        "/" bind Method.GET to { showHomepage() },
        "/todo/{user}/{list}" bind Method.GET to ::showList,
        "/todo/{user}/{list}" bind Method.POST to AddNewItem(hub),
        "/todo/{user}" bind Method.GET to ::getUserLists,
        "/todo/{user}" bind Method.POST to CreateNewList(hub)
    )

    override fun invoke(req: Request): Response = routes(req)

    private fun showHomepage() = Response(Status.OK).body(
        """
            <html>
                <body>
                    <h1>Zettai</h1>
                    <h2>Home page</h2>
                </body>
            </html>
            """.trimIndent()
    )

    private fun showList(req: Request): Response {
        val user = req.extractUser() ?: return Responses.badRequest
        val list =
            req.path("list")?.let { ListName.fromUntrusted(it) } ?: return Responses.badRequest

        return hub.getList(user, list)
            .transform { Body.auto<ToDoList>().toLens().inject(it, Response(Status.OK)) }
            .recover { Responses.notFound }
    }

    private fun getUserLists(request: Request): Response {
        val user = request.extractUser() ?: return Responses.badRequest
        return hub.getUserLists(user).transform {
            Body.auto<List<ListName>>()
                .toLens()
                .inject(it, Response(Status.OK))
        }.recover { Responses.notFound }
    }
}

fun main() {
    val items = listOf("write chapter", "insert code", "draw diagrams")
    val list = ToDoList(ListName.fromTrusted("book"), items.map(::ToDoItem))
    val lists = mutableMapOf(User("uberto") to mutableMapOf(list.name to list))
    val fetcher = MapListFetcher(lists)
    val eventStore = ToDoListEventStore()
    val hub =
        ToDoListHub(
            fetcher,
            ToDoListCommandHandler(eventStore::retrieveByName, fetcher),
            eventStore::receiveEvents
        )

    ZettaiHttpServer(hub).asServer(Jetty(8080)).start()
}
