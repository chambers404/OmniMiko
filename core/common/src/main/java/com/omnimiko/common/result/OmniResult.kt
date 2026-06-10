package com.omnimiko.common.result

/**
 * A lightweight result type used across module boundaries so callers can handle
 * failures explicitly instead of relying on exceptions propagating through the
 * agent loop (where a single tool failure should not crash the whole run).
 */
sealed interface OmniResult<out T> {
    data class Success<T>(val value: T) : OmniResult<T>
    data class Failure(val error: Throwable, val message: String = error.message ?: "Unknown error") :
        OmniResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.value

    fun <R> map(transform: (T) -> R): OmniResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun onSuccess(block: (T) -> Unit): OmniResult<T> {
        if (this is Success) block(value)
        return this
    }

    fun onFailure(block: (Throwable) -> Unit): OmniResult<T> {
        if (this is Failure) block(error)
        return this
    }

    companion object {
        inline fun <T> catching(block: () -> T): OmniResult<T> = try {
            Success(block())
        } catch (t: Throwable) {
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            Failure(t)
        }
    }
}
