package com.gyurigrell.flowreactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Test

@FlowPreview
@ExperimentalCoroutinesApi
class ReactorTest {
    private val scope = TestCoroutineScope()

    @After
    fun teardown() {
        scope.cleanupTestCoroutines()
    }

    @Test
    @FlowPreview
    fun `each method is invoked`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(scope)
        val results = mutableListOf<List<String>>()

        reactor.state.onEach { results.add(it) }.launchIn(scope)

        // Act
        reactor.action.send(mutableListOf("action"))

        // Assert
        assertThat(
            results, equalTo(
                listOf(
                    listOf("transformedState"),
                    listOf(
                        "action",
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
    fun `state replay current state`() = runBlockingTest {
        // Arrange
        val reactor = CounterReactor(scope)
        val results = mutableListOf<Int>()
        reactor.state.onEach { results.add(it) }.launchIn(scope)

        assertThat(results, equalTo(listOf(0)))

        reactor.action.send(Unit) // state: 1
        reactor.action.send(Unit) // state: 2

        assertThat(results, equalTo(listOf(0, 1, 2)))

        // Assert
        assertThat(results, equalTo(listOf(0, 1, 2)))
    }

    @Test
    fun `stream ignores error from mutate`() = runBlockingTest {
        // Arrange
        val reactor = CounterReactor(scope)
        reactor.stateForTriggerError = 2

        val results = mutableListOf<Int>()
        reactor.state.onEach { results.add(it) }.launchIn(scope)

        // Act
        reactor.action.send(Unit)
        reactor.action.send(Unit)
        reactor.action.send(Unit)
        reactor.action.send(Unit)
        reactor.action.send(Unit)

        // Assert
        assertThat(results, equalTo(listOf(0, 1, 2, 3, 4, 5)))
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