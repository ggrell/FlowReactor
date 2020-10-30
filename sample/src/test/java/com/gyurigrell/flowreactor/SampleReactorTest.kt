package com.gyurigrell.flowreactor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test
import kotlin.time.ExperimentalTime

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@ExperimentalTime
@ExperimentalCoroutinesApi
class SampleReactorTest {
    private val reactorScope = TestCoroutineScope()

    @After
    fun teardown() {
        reactorScope.cleanupTestCoroutines()
    }

    @Test
    fun `Given an initial state, When constructed, Then username and password are considered valid`() {
        // Arrange
        val initialState = SampleReactor.State()

        // Assert
        assertThat(initialState.isUsernameValid, `is`(true))
        assertThat(initialState.isPasswordValid, `is`(true))
        assertThat(initialState.isBusy, `is`(false))
    }

    @Test
    fun `Given an initial state, When screen is launched, Then there are no events emitted`() = runBlockingTest {
        // Arrange
        val initialState = SampleReactor.State()
        val reactor = SampleReactor(reactorScope, initialState)
        val states = mutableListOf<SampleReactor.State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(SampleReactor.Action.EnterScreen)

        // Assert
        assertThat(states, equalTo(listOf(initialState)))
    }

    @Test
    fun `Given an initial state, When username changes, Then state username updated and is valid`() = runBlockingTest {
        // Arrange
        val initialState = SampleReactor.State()
        val reactor = SampleReactor(reactorScope, initialState)
        val states = mutableListOf<SampleReactor.State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)
        val username = "MaxHeadroom"

        // Act
        reactor.action.emit(SampleReactor.Action.UsernameChanged(username))

        // Assert
        assertThat(states, equalTo(listOf(initialState, SampleReactor.State(username = username, isUsernameValid = true))))
    }


    @Test
    fun `Given an state with valid username, When set to empty, Then the username is not valid`() = runBlockingTest {
        // Arrange
        val initialState = SampleReactor.State(username = "MaxHeadroom", isUsernameValid = true)
        val reactor = SampleReactor(reactorScope, initialState)
        val states = mutableListOf<SampleReactor.State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(SampleReactor.Action.UsernameChanged(""))

        // Assert
        assertThat(states, equalTo(listOf(initialState, SampleReactor.State(username = "", isUsernameValid = false))))
    }

    @Test
    fun `Given an initial state, When password changes, Then state password updated and is valid`() = runBlockingTest {
        // Arrange
        val initialState = SampleReactor.State()
        val reactor = SampleReactor(reactorScope, initialState)
        val states = mutableListOf<SampleReactor.State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)
        val password = "123456"

        // Act
        reactor.action.emit(SampleReactor.Action.PasswordChanged(password))

        // Assert
        assertThat(states, equalTo(listOf(initialState, SampleReactor.State(password = password, isPasswordValid = true))))
    }

    @Test
    fun `Given an state with valid password, When set to empty, Then the password is not valid`() = runBlockingTest {
        // Arrange
        val initialState = SampleReactor.State(password = "123456", isPasswordValid = true)
        val reactor = SampleReactor(reactorScope, initialState)
        val states = mutableListOf<SampleReactor.State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(SampleReactor.Action.PasswordChanged(""))

        // Assert
        assertThat(states, equalTo(listOf(initialState, SampleReactor.State(password = "", isPasswordValid = false))))
    }
}
