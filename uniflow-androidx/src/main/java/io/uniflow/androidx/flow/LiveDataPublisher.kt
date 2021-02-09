package io.uniflow.androidx.flow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.uniflow.core.flow.DataPublisher
import io.uniflow.core.flow.data.Event
import io.uniflow.core.flow.data.UIEvent
import io.uniflow.core.flow.data.UIState
import io.uniflow.core.logger.UniFlowLogger
import io.uniflow.core.threading.onMain

class LiveDataPublisher<T: UIState>(defaultState: T, val tag: String) : DataPublisher<T> {

    private val _states = MutableLiveData<T>()
    val states: LiveData<T> = _states
    private val _events = MutableLiveData<Event<UIEvent>>()
    val events: LiveData<Event<UIEvent>> = _events

    init {
        _states.value = defaultState
    }

    override suspend fun getState(): T = _states.value ?: error("No state in LiveData")
    override suspend fun publishState(state: T, pushStateUpdate: Boolean) {
        onMain(immediate = true) {
            UniFlowLogger.debug("$tag <-- $state")
            _states.value = state
        }
    }

    override suspend fun publishEvent(event: UIEvent) {
        onMain(immediate = true) {
            UniFlowLogger.debug("$tag <-- $event")
            _events.value = Event(content = event)
        }
    }
}

fun <T: UIState> liveDataPublisher(defaultState: T, tag: String) = LiveDataPublisher(defaultState,tag)