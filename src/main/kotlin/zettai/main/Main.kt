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

data class InvalidInput(override val msg: String) : OutcomeError

object Responses {
    val notFound = Response(Status.NOT_FOUND)
    fun seeOther(location: String) = Response(Status.SEE_OTHER).header("Location", location)
    fun badRequest(error: InvalidInput): Response =
        Response(Status.BAD_REQUEST).also { it.body(error.msg) }
}

class CreateNewList(private val hub: ZettaiHub) : HttpHandler {
    override fun invoke(request: Request): Response {
        val user = request.userFromPath().onFailure { return Responses.badRequest(it) }
        val listName = extractListName(request).onFailure { return Responses.badRequest(it) }

        return hub.handle(CreateToDoList(user, listName))
            .transform { Response(Status.CREATED) }
            .recover { Responses.badRequest(InvalidInput("Unable to create list")) }
    }

    private fun extractListName(request: Request) =
        Body.auto<AddListRequest>()
            .toLens()
            .invoke(request)
            .let { ListName.fromUntrusted(it.listName) }
            .failIfNull(InvalidInput("Invalid list name"))
}

class AddNewItem(private val hub: ZettaiHub) : HttpHandler {
    override fun invoke(request: Request): Response {
        val user = request.userFromPath().onFailure { return Responses.badRequest(it) }
        val listName = request.listNameFromPath().onFailure { return Responses.badRequest(it) }
        val body = Body.auto<AddItemRequest>()
            .toLens()
            .invoke(request)

        return hub.handle(AddToDoItem(user, listName, ToDoItem(body.itemName)))
            .transform { Responses.seeOther("/todo/${user.name}/${listName.name}") }
            .recover { Responses.notFound }
    }

}

private fun Request.userFromPath(): Outcome<InvalidInput, User> =
    path("user")
        .failIfNull(InvalidInput("Invalid user"))
        .transform(::User)

private fun Request.listNameFromPath(): Outcome<InvalidInput, ListName> {
    val untrustedName = path("list") ?: return InvalidInput("List name not provided").asFailure()
    return ListName.fromUntrusted(untrustedName).failIfNull(InvalidInput("Invalid list name"))
}


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
        val user = req.userFromPath().onFailure { return Responses.badRequest(it) }
        val list = req.listNameFromPath().onFailure { return Responses.badRequest(it) }
        return hub.getList(user, list)
            .transform { Body.auto<ToDoList>().toLens().inject(it, Response(Status.OK)) }
            .recover { Responses.notFound }
    }

    private fun getUserLists(request: Request): Response {
        val user = request.userFromPath().onFailure { return Responses.badRequest(it) }
        return hub.getUserLists(user)
            .transform {
                Body.auto<List<ListName>>()
                    .toLens()
                    .inject(it, Response(Status.OK))
            }.recover { Responses.notFound }
    }
}

fun main() {
    val eventStore = ToDoListEventStore()
    val hub =
        ToDoListHub(
            ToDoListQueryRunner(eventStore::fetchEvents),
            ToDoListCommandHandler(eventStore::retrieveByName),
            eventStore::receiveEvents
        )

    ZettaiHttpServer(hub).asServer(Jetty(8080)).start()
}
