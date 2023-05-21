package unit

import ListName
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class ListNameTest {
    @Test
    fun `can not be empty`() {
        expectThat(ListName.fromUntrusted("")).isNull()
    }

    @Test
    fun `is limited by 40 characters in length`() {
        val longName = "this-is-a-very-long-name-0123456789012345678901234567890123456789"
        expectThat(ListName.fromUntrusted(longName)).isNull()
    }

    @Test
    fun `can contain alphanumeric characters and hyphens`() {
        val validListName = "this-Is-a-Valid-name-123"
        expectThat(ListName.fromUntrusted(validListName))
            .isNotNull()
            .get { name }
            .isEqualTo(validListName)
    }

    @Test
    fun `can not contain invalid URL characters or spaces`() {
        expectThat(ListName.fromUntrusted("invalid-with spaces")).isNull()
        expectThat(ListName.fromUntrusted("invalid-list+bad_characters")).isNull()
        expectThat(ListName.fromUntrusted("invalid-list+\u2202\u2203")).isNull()
    }
}