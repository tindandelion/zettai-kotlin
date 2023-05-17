import org.http4k.core.*
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main() {
    val app = routes(
        "/todo/{user}/{list}" bind Method.GET to ::showList
    )
    app.asServer(Jetty(8080)).start()
}

fun showList(req: Request): Response {
    val user = req.path("user")
    val list = req.path("list")
    val htmlPage = """
    <html>
        <body>
            <h1>Zettai</h1>
            <p>Here is a list <b>$list</b> of user <b>$user</b></p>
        </body>
    </html>    
    """.trimIndent()
    return Response(Status.OK).body(htmlPage)
}