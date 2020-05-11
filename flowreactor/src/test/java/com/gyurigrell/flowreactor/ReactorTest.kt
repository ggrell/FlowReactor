package com.gyurigrell.flowreactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Test

@FlowPreview
@ExperimentalCoroutinesApi
class ReactorTest {
    @Test
    @FlowPreview
    fun `each method is invoked`() = runBlocking {
        val reactor = TestReactor(this)

        launch {
            reactor.action.send(mutableListOf("action"))
            reactor.action.send(mutableListOf("secondAction"))
            reactor.action.close()
        }

        val result = reactor.state.toCollection(mutableListOf())
        assertThat(
            result, equalTo(
                listOf(
                    listOf("transformedState"),
                    listOf(
                        "action",
                        "transformedAction",
                        "mutation",
                        "transformedMutation",
                        "transformedState"
                    ),
                    listOf(
                        "action",
                        "transformedAction",
                        "mutation",
                        "transformedMutation",
                        "transformedState",
                        "secondAction",
                        "transformedAction",
                        "mutation",
                        "transformedMutation",
                        "transformedState"
                    )
                )
            )
        )
    }

    @Test
    fun `state replay current state`() = runBlocking {
        // Arrange
        val reactor = CounterReactor(this)

        // Act
        launch {
            reactor.action.send(Unit) // state: 1
            reactor.action.send(Unit) // state: 2
            reactor.action.close()
        }

        // Assert
        val output = reactor.state.toCollection(mutableListOf())
        assertThat(output, equalTo(listOf(0, 1, 2)))
    }

    @Test
    fun `stream ignores error from mutate`() = runBlocking {
        // Arrange
        val reactor = CounterReactor(this)
        reactor.stateForTriggerError = 1

        // Act
        launch {
            reactor.action.send(Unit)
            reactor.action.send(Unit)
            reactor.action.send(Unit)
            reactor.action.send(Unit)
            reactor.action.send(Unit)
            reactor.action.close()
        }

        // Assert
        val output = reactor.state.toCollection(mutableListOf())
        assertThat(output, equalTo(listOf(0, 1, 2, 3, 4, 5)))
    }
    
    @FlowPreview
    class TestReactor(scope: CoroutineScope) : Reactor<List<String>, List<String>, List<String>>(scope, listOf()) {
        // 1. ["action"] + ["transformedAction"]
        override fun transformAction(action: Flow<List<String>>): Flow<List<String>> {
            return action.map { it + "transformedAction" }
        }

        // 2. ["action", "transformedAction"] + ["mutation"]
        override fun mutate(action: List<String>): Flow<List<String>> {
            return flow { emit(action + "mutation") }
        }

        // 3. ["action", "transformedAction", "mutation"] + ["transformedMutation"]
        override fun transformMutation(mutation: Flow<List<String>>): Flow<List<String>> {
            return mutation.map { it + "transformedMutation" }
        }

        // 4. [] + ["action", "transformedAction", "mutation", "transformedMutation"]
        override fun reduce(state: List<String>, mutation: List<String>): List<String> {
            return state + mutation
        }

        // 5. ["action", "transformedAction", "mutation", "transformedMutation"] + ["transformedState"]
        override fun transformState(state: Flow<List<String>>): Flow<List<String>> {
            return state.map { it + "transformedState" }
        }
    }

    @FlowPreview
    class CounterReactor(scope: CoroutineScope) : Reactor<Unit, Unit, Int>(scope, initialState = 0) {
        var stateForTriggerError: Int? = null
        var stateForTriggerCompleted: Int? = null

        override fun mutate(action: Unit): Flow<Unit> = when (currentState) {
            stateForTriggerError -> flow {
                emit(action)
                throw TestError()
            }
            
            stateForTriggerCompleted -> flow {
                emit(action)
            }

            else -> flow {
                emit(action)
            }
        }

        override fun reduce(state: Int, mutation: Unit): Int {
            return state + 1
        }
    }

    class TestError : Error()
}