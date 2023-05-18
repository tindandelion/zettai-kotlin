import org.http4k.core.*
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

data class User(val name: String)
data class ListName(val name: String)
data class ToDoItem(val description: String)
data class ToDoList(val name: ListName, val items: List<ToDoItem>)


private data class HtmlPage(val raw: String)

class Zettai(private val lists: Map<User, List<ToDoList>>) : HttpHandler {
    val routes = routes(
        "/" bind Method.GET to ::showHomepage,
        "/todo/{user}/{list}" bind Method.GET to ::showList
    )


    override fun invoke(req: Request): Response = routes(req)

    @Suppress("UNUSED_PARAMETER")
    private fun showHomepage(req: Request): Response {
        val homePage = """
        <html>
            <body>
                <h1>Zettai</h1>
                <h2>Home page</h2>
            </body>
        </html>
        """.trimIndent()
        return Response(Status.OK).body(homePage)
    }

    private fun showList(req: Request): Response =
        req.let(::extractListData)
            .let(::fetchListContent)
            .let(::renderHtml)
            .let(::createResponse)

    private fun extractListData(req: Request): Pair<User, ListName> {
        val user = req.path("user").orEmpty()
        val list = req.path("list").orEmpty()
        return User(user) to ListName(list)
    }

    private fun fetchListContent(listId: Pair<User, ListName>): ToDoList {
        val (user, listName) = listId
        return lists[user]
            ?.firstOrNull { it.name == listName }
            ?: error("List unknown")
    }

    private fun renderHtml(list: ToDoList): HtmlPage = HtmlPage(
        """
        <html>
            <body>
                <h1>Zettai</h1>
                <h2>${list.name.name}</h2>
                <table>
                    <tbody>${renderItems(list.items)}</tbody>
                </table>
            </body>
        </html>
        """.trimIndent()
    )

    private fun renderItems(items: List<ToDoItem>): String =
        items.joinToString("") { "<tr><td>${it.description}</td></tr>" }

    private fun createResponse(page: HtmlPage): Response =
        Response(Status.OK).body(page.raw)
}

