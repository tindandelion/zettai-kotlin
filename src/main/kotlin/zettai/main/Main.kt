package zettai.main

import org.http4k.core.*
import org.http4k.core.body.form
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import zettai.core.*


object Responses {
    val badRequest = Response(Status.BAD_REQUEST)
    val notFound = Response(Status.NOT_FOUND)
    fun seeOther(location: String) = Response(Status.SEE_OTHER).header("Location", location)
    fun ok(body: String) = Response(Status.OK).body(body)
}

class ZettaiHttpServer(private val hub: ZettaiHub) : HttpHandler {
    val routes = routes(
        "/" bind Method.GET to ::showHomepage,
        "/todo/{user}/{list}" bind Method.GET to ::showList,
        "/todo/{user}/{list}" bind Method.POST to ::addNewItem,
    )

    override fun invoke(req: Request): Response = routes(req)

    @Suppress("UNUSED_PARAMETER")
    private fun showHomepage(req: Request) =
        Response(Status.OK).body(
            """
            <html>
                <body>
                    <h1>Zettai</h1>
                    <h2>Home page</h2>
                </body>
            </html>
            """.trimIndent()
        )

    private fun addNewItem(request: Request): Response {
        val user = request.path("user")?.let(::User) ?: return Responses.badRequest
        val listName = request.path("list")
            ?.let { ListName.fromUntrusted(it) }
            ?: return Responses.badRequest
        val item = request.form("itemname")
            ?.let { ToDoItem(it) }
            ?: return Responses.badRequest

        return hub.addItemToList(user, listName, item)
            ?.let { Responses.seeOther("/todo/${user.name}/${it.name.name}") }
            ?: Responses.notFound
    }

    private fun showList(req: Request): Response {
        val user = req.path("user")?.let(::User) ?: return Responses.badRequest
        val list =
            req.path("list")?.let { ListName.fromUntrusted(it) } ?: return Responses.badRequest

        return hub.getList(user, list)
            ?.let(::renderList)
            ?.let { Responses.ok(it.raw) }
            ?: Responses.notFound
    }
}

fun main() {
    val items = listOf("write chapter", "insert code", "draw diagrams")
    val list = ToDoList(ListName.fromTrusted("book"), items.map(::ToDoItem))
    val lists = mutableMapOf(User("uberto") to mutableMapOf(list.name to list))
    ZettaiHttpServer(ToDoListHub(MapListFetcher(lists))).asServer(Jetty(8080)).start()
}
