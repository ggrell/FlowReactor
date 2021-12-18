
package com.gyurigrell.flowreactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
class ReactorTest {
    private val reactorScope = TestCoroutineScope()

    @After
    fun teardown() {
        reactorScope.cleanupTestCoroutines()
    }

    @Test
    fun `each method is invoked`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)
        val results = mutableListOf<List<String>>()
        reactor.state.onEach { results.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(mutableListOf("action"))

        // Assert
        val expected = listOf(
            listOf("transformedState"),
            listOf("action", "transformedAction", "mutation", "transformedMutation", "transformedState")
        )
        assertThat(results, equalTo(expected))
    }

    @Test
    fun `each subscription receives values`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)

        val resultsLowerCase = mutableListOf<List<String>>()
        reactor.state
            .map { list -> list.map { it.lowercase() } }
            .onEach { resultsLowerCase.add(it) }
            .launchIn(reactorScope)

        val resultsUpperCase = mutableListOf<List<String>>()
        reactor.state
            .map { list -> list.map { it.uppercase() } }
            .onEach { resultsUpperCase.add(it) }
            .launchIn(reactorScope)

        // Act
        reactor.action.emit(mutableListOf("action"))

        // Assert
        val expectedLowerCase = listOf(
            listOf("transformedstate"),
            listOf("action", "transformedaction", "mutation", "transformedmutation", "transformedstate")
        )
        assertThat(resultsLowerCase, equalTo(expectedLowerCase))
        val expectedUpperCase = listOf(
            listOf("TRANSFORMEDSTATE"),
            listOf("ACTION", "TRANSFORMEDACTION", "MUTATION", "TRANSFORMEDMUTATION", "TRANSFORMEDSTATE")
        )
        assertThat(resultsUpperCase, equalTo(expectedUpperCase))
    }

    @Test
    fun `state replay current state`() = runBlockingTest {
        // Arrange
        val reactor = CounterReactor(reactorScope)
        val results = mutableListOf<Int>()
        reactor.state.onEach { results.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Unit) // state: 1
        reactor.action.emit(Unit) // state: 2

        // Assert
        assertThat(results, equalTo(listOf(0, 1, 2)))
    }

    @Test
    fun `stream ignores error from mutate`() = runBlockingTest {
        // Arrange
        val reactor = CounterReactor(reactorScope)
        reactor.stateForTriggerError = 2

        val results = mutableListOf<Int>()
        reactor.state.onEach { results.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Unit)
        reactor.action.emit(Unit)
        reactor.action.emit(Unit)
        reactor.action.emit(Unit)
        reactor.action.emit(Unit)

        // Assert
        assertThat(results, equalTo(listOf(0, 1, 2, 3, 4, 5)))
    }

    class TestReactor(scope: CoroutineScope) : Reactor<List<String>, List<String>, List<String>>(scope, listOf()) {
        // 1. ["action"] + ["transformedAction"]
        override fun transformAction(action: Flow<List<String>>) =
            action.map { it + "transformedAction" }

        // 2. ["action", "transformedAction"] + ["mutation"]
        override fun mutate(action: List<String>) =
            flow { emit(action + "mutation") }

        // 3. ["action", "transformedAction", "mutation"] + ["transformedMutation"]
        override fun transformMutation(mutation: Flow<List<String>>) =
            mutation.map { it + "transformedMutation" }

        // 4. [] + ["action", "transformedAction", "mutation", "transformedMutation"]
        override fun reduce(state: List<String>, mutation: List<String>) =
            state + mutation

        // 5. ["action", "transformedAction", "mutation", "transformedMutation"] + ["transformedState"]
        override fun transformState(state: Flow<List<String>>) =
            state.map { it + "transformedState" }
    }

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
