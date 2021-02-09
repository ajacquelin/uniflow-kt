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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.uniflow.androidx.flow

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.uniflow.core.dispatcher.UniFlowDispatcher
import io.uniflow.core.flow.ActionDispatcher
import io.uniflow.core.flow.ActionReducer
import io.uniflow.core.flow.DataFlow
import io.uniflow.core.flow.DataPublisher
import io.uniflow.core.flow.data.UIEvent
import io.uniflow.core.flow.data.UIState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

/**
 * Android implementation of [DataFlow].
 * This is also a [ViewModel].
 * Its [coroutineScope] equals [viewModelScope].
 *
 * @param defaultCapacity
 * The default capacity of this `DataFlow`.
 * Any state actions dispatched using [setState] will be added to the buffer unless it's full.
 * Defaults to [Channel.BUFFERED].
 *
 * @param defaultDispatcher The default [CoroutineDispatcher] on which state actions are dispatched.
 * Defaults to [Dispatchers.IO].
 */
abstract class AndroidDataFlow<T: UIState>(
        defaultState: T,
        defaultCapacity: Int = Channel.BUFFERED,
        defaultDispatcher: CoroutineDispatcher = UniFlowDispatcher.dispatcher.io()
) : ViewModel(), DataFlow<T>, DataPublisher<T> {
    override val tag = this.toString()

    private val coroutineScope: CoroutineScope = viewModelScope

    val defaultDataPublisher = liveDataPublisher(defaultState, "main")
    override suspend fun publishState(state: T, pushStateUpdate: Boolean) = defaultPublisher().publishState(state, pushStateUpdate)
    override suspend fun publishEvent(event: UIEvent) = defaultPublisher().publishEvent(event)
    override suspend fun getState(): T = defaultPublisher().getState()
    override fun defaultPublisher(): DataPublisher<T> = defaultDataPublisher

    private val actionReducer = ActionReducer(::defaultPublisher, coroutineScope, defaultDispatcher, defaultCapacity, tag)
    override val actionDispatcher: ActionDispatcher<T> = ActionDispatcher(coroutineScope, actionReducer, ::onError, tag)

    @CallSuper
    override fun onCleared() {
        actionReducer.close()
        super.onCleared()
    }
}
