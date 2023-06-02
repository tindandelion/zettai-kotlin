package zettai.core

interface OutcomeError {
    val msg: String
}

sealed class Outcome<out E : OutcomeError, out T> {
    fun <U> transform(f: (T) -> U): Outcome<E, U> =
        when (this) {
            is Success -> Success(f(value))
            is Failure -> this
        }
}

data class Success<T> internal constructor(val value: T) : Outcome<Nothing, T>()
data class Failure<E : OutcomeError> internal constructor(val error: E) : Outcome<E, Nothing>()

inline fun <E : OutcomeError, T> Outcome<E, T>.onFailure(exitBlock: (E) -> Nothing): T =
    when (this) {
        is Success<T> -> value
        is Failure<E> -> exitBlock(error)
    }

fun <E : OutcomeError> E.asFailure(): Failure<E> = Failure(this)
fun <T> T.asSuccess(): Success<T> = Success(this)

fun <T, U, E : OutcomeError> lift(f: (T) -> U): (Outcome<E, T>) -> Outcome<E, U> =
    { outcome -> outcome.transform(f) }

fun <T, E : OutcomeError> Outcome<E, T>.recover(onError: (E) -> T): T =
    when (this) {
        is Success -> value
        is Failure -> onError(error)
    }

fun <T, E : OutcomeError> T?.failIfNull(error: E): Outcome<E, T> =
    this?.asSuccess() ?: error.asFailure()

fun <E : OutcomeError, T> Outcome<E, T>.orNull(): T? = this.recover { null }

