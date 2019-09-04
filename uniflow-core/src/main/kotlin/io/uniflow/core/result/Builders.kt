package io.uniflow.core.result

fun <T : Any> safeResult(value: T): SafeResult<T> = SafeResult.Success(value)

fun errorResult(message: String): SafeResult<Nothing> = SafeResult.Error(IllegalStateException(message))

fun errorResult(exception: Exception): SafeResult<Nothing> = SafeResult.Error(exception)

fun <T : Any> T.asSafeResult(): SafeResult<T> = safeResult(this)

suspend fun <T : Any> safeCall(expr: suspend () -> T): SafeResult<T> {
    return try {
        expr().asSafeResult()
    } catch (error: Exception) {
        SafeResult.Error(error)
    }
}

suspend fun <T : Any> safeCall(expr: suspend () -> T, onError: (Exception) -> Exception): SafeResult<T> {
    return try {
        expr().asSafeResult()
    } catch (exception: Exception) {
        SafeResult.Error(onError(exception))
    }
}

suspend fun <T : Any> safeResultCall(expr: suspend () -> SafeResult<T>): SafeResult<T> {
    return try {
        expr()
    } catch (exception: Exception) {
        SafeResult.Error(exception)
    }
}

suspend fun <T : Any> safeResultCall(expr: suspend () -> SafeResult<T>, onError: (Exception) -> Exception): SafeResult<T> {
    return try {
        expr()
    } catch (exception: Exception) {
        SafeResult.Error(onError(exception))
    }
}

suspend fun <T : Any> networkCall(expr: suspend () -> T): SafeResult<T> {
    return safeCall(expr) { error ->
        NetworkException(error = error)
    }
}

suspend fun <T : Any> databaseCall(expr: suspend () -> T): SafeResult<T> {
    return safeCall(expr) { error ->
        DatabaseException(error = error)
    }
}
