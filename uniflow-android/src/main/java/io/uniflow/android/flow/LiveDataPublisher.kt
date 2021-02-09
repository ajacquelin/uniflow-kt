package io.uniflow.android.flow

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import io.uniflow.core.flow.UIDataPublisher
import io.uniflow.core.flow.data.Event
import io.uniflow.core.flow.data.UIEvent
import io.uniflow.core.flow.data.UIState
import io.uniflow.core.threading.onMain

class LiveDataPublisher<T: UIState>(defaultState: T) : UIDataPublisher {

    private val _states = MutableLiveData<T>()
    val states: LiveData<T> = _states
    private val _events = MutableLiveData<Event<UIEvent>>()
    val events: LiveData<Event<UIEvent>> = _events

    init {
        _states.value = defaultState
    }

    override suspend fun publishState(state: T) {
        onMain(immediate = true) {
            _states.value = state
        }
    }

    override suspend fun publishEvent(event: UIEvent) {
        onMain(immediate = true) {
            _events.value = Event(content = event)
        }
    }
}