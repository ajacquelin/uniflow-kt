
# UniFlow 🦄 - Unidirectional Data Flow for Android, using Kotlin coroutines

## Setup

#### Current version is `0.6.2`

Choose one of the following dependency:

```gradle
// Android
implementation 'io.uniflow:uniflow-android:$version'
testImplementation 'io.uniflow:uniflow-android-test:$version'

// AndroidX
implementation 'io.uniflow:uniflow-androidx:$version'
testImplementation 'io.uniflow:uniflow-androidx-test:$version'
```

this version is based on Kotlin `1.3.50` & Coroutines `1.3.0`

## Quick intro

### What is Unidirectional Data Flow?

Unidirectional Data Flow is a concept that means that data has one, and only one, way to be transferred to other parts of the application.

This means that:

- state is passed to the view
- actions are triggered by the view
- actions can update the state
- the state change is passed to the view

The view is a result of the application state. State can only change when actions happen. When actions happen, the state is updated.

Thanks to one-way bindings, data cannot flow in the opposite way (as would happen with two-way bindings, for example), and this has some key advantages:

it’s less error prone, as you have more control over your data
it’s easier to debug, as you know what is coming from where

### Why UniFlow🦄?

UniFlow help you write your app with states and events to ensure consistency through the time, and this with Coroutines.

UniFlow provides:

* Smart way to write a Data flow in pure Kotlin
* Android extensions to let you just focus on States & Events
* Ready for Kotlin coroutines
* Easy to test

## Getting Started

### Writing your first UI state

Describe your data flow states with `UIState`:

```kotlin
data class WeatherState(val day : String, val temperature : String) : UIState()
```

Publish state updates from your ViewModel:

```kotlin
class WeatherDataFlow(val repo : WeatherRepository) : AndroidDataFlow() {

    fun getWeather() = setState {
        // call to get data
        val weather = repo.getWeatherForToday().await()
        // return a new state
        WeatherState(weather.day, weather.temperature)
    }
}
```

Observe your state flow from your Activity or Fragment:

```kotlin
class WeatherActivity : AppCompatActivity {

	val weatherFlow : WeatherDataFlow ... // ViewModel created with Koin for example :)
	
	override fun onCreate(savedInstanceState: Bundle?) {		
		// Observe incoming states
		onStates(weatherFlow) { state ->
			when (state) {
				// react on WeatherState update
				is WeatherState -> showWeather(state)
			}
		}
	}
}

```

### Easy testing!

Create your ViewModel instance and mock your states observer:

```kotlin
val mockedRepo : WeatherRepository = mockk(relaxed = true)
lateinit var dataFlow : WeatherDataFlow

@Before
fun before() {
	// create WeatherDataFlow instance with mocked WeatherRepository
	dataFlow = WeatherDataFlow(mockedRepo)
	// create mocked observer
	view = detailViewModel.mockObservers()
}
```

Now you can test incoming states with `hasState`. We are providing Mockk to help mock underlying coroutines call: 

```kotlin
@Test
fun `has some weather`() {
	// prepare test data
	val weatherData = WeatherData(...)
	// setup mocked call
	coEvery { mockedRepo.getWeatherForToday() } return weatherData

	// Call getWeather()
	dataFlow.getWeather()
		
	// verify state
	verifySequence {
		view.hasState(WeatherState(weatherData.day, weatherData.temperature))
	}
}
```

## Actions, States & Events

Your ViewModel class, aka your DataFlow, will provide `actions` that will trigger states and events.

An action is a simple function, that directly use one state operator:

```kotlin
class WeatherDataFlow(...) : AndroidDataFlow() {
    
    // getWeather action
    fun getWeather() = setState {
        ...
    }
}
```

The state operator are the following:

- `setState { current -> ... newState}` - from current state, udpate the current state
- `withState { current -> ... }` - from current state, do a side effect
- `fromState<T> { current as T -> newState }` - from current state <T>, udpate the current state
- `stateFlow { setState() ... }` - allow several state updates from same flow/job


### Writing states as immutable data

To describe your data flow states, inherit from `UIState` class directly or use a sealed class as follow:

```kotlin
class WeatherStates : UIState(){
	object LoadingWeather : WeatherStates()
	data class WeatherState(val day : String, val temperature : String) : WeatherStates()
}

```

### Updating the current state

`SetState` is an action builder to simply set a new state:

```kotlin
// update the current state
fun getWeather() = setState{
    // return directly your state object
    WeatherState(...)
}
```

Don't forget to listen to states from your Activity/Fragment with `onStates`:

```kotlin
// Observe incoming states
onStates(weatherFlow) { state ->
	when (state) {
		// react on WeatherState update
		is WeatherState -> showWeather(state)
	}
}
```

The `FromState<T>` operator, help set a new state if you are in the given state <T>. Else your DataFlow will send `BadOrWrongState` event:

```kotlin
// Execute loadNewLocation action only if current state is in WeatherListState
fun loadNewLocation(location: String) = fromState<WeatherState>{ currentState ->
    // currentState is WeatherListState
    // ...
}
```

### Side effets

For side effects actions, you can use `UIEvent` and avoid update the current state with `withState`:

```kotlin
fun getWeather() = withState {
    sendEvent(...)
    // won't update the current state
}
```

### StateFlow: multiple state updates from one action

When you need to publish several states from one action function, then you can't use one of the function above. Use the `stateFlow` action builder to let you publish states when needed, with `setState()` function:

```kotlin
// push state updates
fun getWeather() = stateFlow {
	// Set loading state
	setState(UIState.Loading)
	// return directly your state object
	setState(WeatherState(...))
}
```

### Triggering events

For fire and forget side effects/events, define some events with `UIEvent`:

```kotlin
// Events definition
sealed class WeatherEvent : UIEvent() {
    data class Success(val location: String) : WeatherEvent()
    data class Failed(val location: String, val error: Throwable? = null) : WeatherEvent()
}
```

Note that, like States, Events are immutable data too.

From your Data Flow VIewModel, trigger events with `sendEvent()`:

```kotlin
	fun loadNewLocation(location: String) = fromState<WeatherListState>{
	    // send event
	    sendEvent(WeatherEvent.Success(location))
	}
}

```

_note_: sendEvent() return a null state (UIState?). Can help write

Observe events from your ViewModel with `onEvent`:

```kotlin
onEvents(viewModel) { event ->
    when (val data = event.take()) {
        is WeatherListUIEvent.Success -> showSuccess(data.location)
        is WeatherListUIEvent.Failed -> showFailed(data.location, data.error)
    }
}
```

On an event, you can either `take()` or `peek()` its data:

- `take` - consume the event data, can't be taken by other event consumer
- `peek` - peek the event's data, even if the data has been consumed


## Smart Coroutines

### Clean Error Handling

Each action allow you to provide an error handling function. You can also catch any error more globally:


```kotlin
class WeatherViewModelFlow : AndroidDataFlow() {

    init {
        // init state as Loading
        setState { UIState.Loading }
    }

    fun getMyWeather(val day : String) = setState(
        { lastState ->
            // Background call
            val weather = getWeatherForDay(day).await()
            // return state to UI
            WeatherState(weather)
        },
        { error -> // get error here })

    // Unhandled errors here
    override suspend fun onError(error: Throwable){
        // ...
    }

}
```

### Handling Coroutines Threading


### FlowResult: functional coroutines


## More tools for test

### Scheduling & Testing

First don't forget to use thr following rule:

```kotlin
@get:Rule
var rule = TestDispatchersRule()
```

`TestDispatchersRule` allow you to dispatch with `Unconfined` thread, as we flatten all scheduling to help sequential running of all states & events.

You can also use the `TestThreadRule`, to emulate a main thread: replace main dispatcher by a single thread context dispatcher


