package io.uniflow.core.flow

import io.uniflow.core.flow.data.UIState
import io.uniflow.core.logger.UniFlowLogger
import io.uniflow.core.threading.launchOnIO
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

/**
 * Handle dispatch logic to ActionReducer, help wrap Action
 * dispatchAction - Help dispatch given action to ActionReducer, on given coroutineScope context.
 * actionOn - Handle default error behavior by routing back to ::onError function
 *
 * dispatch will be done in background
 *
 * @author Arnaud Giuliani
 */
class ActionDispatcher<T: UIState>(
        private val coroutineScope: CoroutineScope,
        private val reducer: ActionReducer<T>,
        private val runError: suspend (Exception, T) -> Unit,
        val tag: String
) {
    fun dispatchAction(onAction: ActionFunction_T<T>): Action<T> = dispatchAction(onAction, onError = runError)

    fun dispatchAction(onAction: ActionFunction_T<T>, onError: suspend (Exception, T) -> Unit): Action<T> = Action(onAction, onError).also {
        dispatchAction(it)
    }

    fun dispatchAction(action: Action<T>) {
        coroutineScope.launchOnIO {
            UniFlowLogger.debug("$tag - enqueue: $action")
            reducer.enqueueAction(action)
        }
    }

    fun actionOn(kClass: KClass<out T>, onAction: ActionFunction_T<T>): Action<T> = actionOn(kClass, onAction) { error, state -> runError(error, state) }
    fun actionOn(kClass: KClass<out T>, onAction: ActionFunction_T<T>, onError: ActionErrorFunction_T<T>): Action<T> = Action(onAction, onError, kClass).also { dispatchAction(it) }

    fun close() {
        reducer.close()
    }
}