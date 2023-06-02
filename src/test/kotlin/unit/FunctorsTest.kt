package unit

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun <T> identity(a: T): T = a

infix fun <A, B, C> ((A) -> B).andThen(f: (B) -> C): (A) -> C = { a: A -> f(this(a)) }

data class Holder<T>(private val value: T) {
    fun <U> transform(f: (T) -> U): Holder<U> = Holder(f(value))

    companion object {
        fun <T, U> lift(f: ((T) -> U)): (Holder<T>) -> Holder<U> = { c: Holder<T> ->
            c.transform(f)
        }
    }
}

sealed class Outcome<out E, out T> {
    abstract fun <U> transform(f: (T) -> U): Outcome<E, U>
}

data class Success<T>(val value: T) : Outcome<Nothing, T>() {
    override fun <U> transform(f: (T) -> U): Outcome<Nothing, U> = Success(f(value))
}

data class Failure<E>(val error: E) : Outcome<E, Nothing>() {
    override fun <U> transform(f: (Nothing) -> U): Outcome<E, U> = this
}

inline fun <E, T> Outcome<E, T>.onFailure(exitBlock: (E) -> Nothing): T =
    when (this) {
        is Success<T> -> value
        is Failure<E> -> exitBlock(error)
    }


class FunctorsTest {
    private val sampleString = "Hello beautiful world"
    private val length = { s: String -> s.length }
    private val half = { x: Int -> x / 2 }

    @Test
    fun combineFunctions() {
        val halfLength = length andThen half
        expectThat(halfLength(sampleString)).isEqualTo(10)
    }

    @Test
    fun associateFunctions() {
        val toString = { a: Any -> a.toString() }
        val halfLengthStr1 = (length andThen half) andThen toString
        val halfLengthStr2 = length andThen (half andThen toString)

        expectThat(halfLengthStr1(sampleString)).isEqualTo("10")
        expectThat(halfLengthStr2(sampleString)).isEqualTo("10")
    }

    @Test
    fun transform() {
        val a = Holder("this is a string")
        val b = a.transform(String::length)
        expectThat(b).isEqualTo(Holder(16))
    }

    @Test
    fun lifting() {
        val length1 = Holder.lift(String::length)
        expectThat(length1(Holder("Hello"))).isEqualTo(Holder(5))
    }

    @Test
    fun identity() {
        val a = Holder(10)
        val a1 = a.transform(::identity)
        expectThat(a1).isEqualTo(a)
    }
}