package zettai.main

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent

val headers: MutableMap<String, String> = mutableMapOf(
    "Content-Type" to "text/html",
    "X-Custom-Header" to "text/html"
)

val homePage = """
    <html>
        <body>
            <h1>Zettai</h1>
            <h2>Home page</h2>
        </body>
    </html>
""".trimIndent()

class LambdaHandler : RequestHandler<APIGatewayProxyRequestEvent?, APIGatewayProxyResponseEvent> {
    override fun handleRequest(
        input: APIGatewayProxyRequestEvent?,
        context: Context
    ): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent()
            .withHeaders(headers)
            .withStatusCode(200)
            .withBody(homePage)
    }
}
