/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed launchOn an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.uniflow.android.flow

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import io.uniflow.core.flow.data.EventConsumer
import io.uniflow.core.flow.data.UIEvent
import io.uniflow.core.flow.data.UIState
import io.uniflow.core.logger.UniFlowLogger

/**
 * AndroidDataFlow Observers for states & events
 *
 * @author Arnaud Giuliani
 */

/**
 * Listen incoming states (UIState) on given AndroidDataFlow
 */
fun <T: UIState> LifecycleOwner.onStates(vm: AndroidDataFlow<T>, handleStates: (T) -> Unit) {
    var lastState: UIState? = null
    vm.dataPublisher.states.observe(this, Observer { state: T? ->
        // TODO Extract generic State observer
        state?.let {
            UniFlowLogger.debug("onStates - $this - last state: $lastState")
            if (lastState != state) {
                UniFlowLogger.debug("onStates - $this <- $state")
                handleStates(state)
                lastState = state
            } else {
                UniFlowLogger.debug("onStates - already received -  $this <- $state")
            }
        }
    })
}

/**
 * Listen incoming events (Event<UIEvent>) on given AndroidDataFlow
 */
fun LifecycleOwner.onEvents(vm: AndroidDataFlow, handleEvents: (UIEvent) -> Unit) {
    val consumer = EventConsumer(consumerId)
    vm.dataPublisher.events.observe(this, Observer { event ->
        // TODO Extract generic Event observer
        event?.let {
            consumer.onEvent(event)?.let {
                UniFlowLogger.debug("onEvents - $this <- $event")
                handleEvents(it)
            } ?: UniFlowLogger.debug("onEvents - already received - $this <- $event")
        }
    })
}

internal val Any.consumerId: String
    get() = this::class.simpleName ?: error("can't get consumerId for $this")