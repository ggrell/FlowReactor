/*
 * Copyright (c) 2020, Gyuri Grell and RxReactor contributors. All rights reserved
 *
 * Licensed under BSD 3-Clause License.
 * https://opensource.org/licenses/BSD-3-Clause
 */

package com.gyurigrell.flowreactor

import com.gyurigrell.flowreactor.ReactorWithEffectsTests.TestReactor.Action
import com.gyurigrell.flowreactor.ReactorWithEffectsTests.TestReactor.Effect
import com.gyurigrell.flowreactor.ReactorWithEffectsTests.TestReactor.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

/**
 * Unit tests for [ReactorWithEffects]
 */
@ExperimentalCoroutinesApi
class ReactorWithEffectsTests {
    private val reactorScope = TestCoroutineScope()

    @After
    fun teardown() {
        reactorScope.cleanupTestCoroutines()
    }

    @Test
    fun `SimpleAction updates State simpleAction to true`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)

        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Action.SimpleAction)

        // Assert
        assertThat(states, equalTo(listOf(State(false), State(true))))
        assertThat(effects, equalTo(emptyList()))
    }

    @Test
    fun `ActionWithValue updates State actionWithValue to correct string`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)

        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(reactorScope)

        val theValue = "I love apple pie"

        // Act
        reactor.action.emit(Action.ActionWithValue(theValue))

        // Assert
        assertThat(states, equalTo(listOf(State(), State(false, theValue))))
        assertThat(effects, equalTo(emptyList()))
    }

    @Test
    fun `ActionFiresEffectOne emits the effect `() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)

        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Action.ActionFiresEffectOne)

        // Assert
        assertThat(states, equalTo(listOf(State())))
        assertThat(effects, equalTo(listOf(Effect.EffectOne)))
    }

    @Test
    fun `ActionFiresEffectOne emits the effect to multiple subscribers`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)

        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        val effects1 = mutableListOf<Effect>()
        reactor.effect.onEach { effects1.add(it) }.launchIn(reactorScope)

        val effects2 = mutableListOf<Effect>()
        reactor.effect.onEach { effects2.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Action.ActionFiresEffectOne)

        // Assert
        assertThat(states, equalTo(listOf(State())))
        assertThat(effects1, equalTo(listOf(Effect.EffectOne)))
        assertThat(effects2, equalTo(listOf(Effect.EffectOne)))
    }

    @Test
    fun `ActionFiresEffectWithValue emits the effect with the correct value`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)
        val theValue = "Millions of peaches, peaches for me"

        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Action.ActionFiresEffectWithValue(theValue))

        // Assert
        assertThat(states, equalTo(listOf(State())))
        assertThat(effects, equalTo(listOf(Effect.EffectWithValue(theValue))))
    }

    @Test
    fun `One action fires two effects`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)
        val theValue = "Millions of peaches, peaches for me"

        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Action.ActionFiresTwoEffects(theValue))

        // Assert
        assertThat(states, equalTo(listOf(State())))
        val expectedEffects = listOf(Effect.EffectOne, Effect.EffectWithValue(theValue))
        assertThat(effects, equalTo(expectedEffects))
    }

    @Test
    fun `An action causes an exception to be thrown, but reactor keeps working`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)
        val theException = IllegalArgumentException("Some bad argument")

        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Action.ActionThrows(theException))

        // Assert
        assertThat(states, equalTo(listOf(State())))
        assertThat(effects, equalTo(emptyList()))
    }

    @Test
    fun `A mutation causes an exception to be thrown, but reactor keeps working`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(reactorScope)
        val theException = IllegalArgumentException("Some bad argument")

        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(reactorScope)

        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(reactorScope)

        // Act
        reactor.action.emit(Action.MutationThrows(theException))

        // Assert
        assertThat(states, equalTo(listOf(State(), State())))
        assertThat(effects, equalTo(emptyList()))
    }

    class TestReactor(
        scope: CoroutineScope,
        initialState: State = State()
    ) : ReactorWithEffects<Action, TestReactor.Mutation, State, Effect>(scope, initialState) {
        sealed class Action {
            object SimpleAction : Action()
            data class ActionWithValue(val theValue: String) : Action()
            object ActionFiresEffectOne : Action()
            data class ActionFiresEffectWithValue(val theValue: String) : Action()
            data class ActionFiresTwoEffects(val theValue: String) : Action()
            data class ActionThrows(val throwable: Throwable) : Action()
            data class MutationThrows(val throwable: Throwable) : Action()
        }

        sealed class Mutation {
            object SimpleActionMutation : Mutation()
            data class ActionWithValueMutation(val theValue: String) : Mutation()
            data class MutationThrows(val throwable: Throwable) : Mutation()
        }

        data class State(
            val simpleAction: Boolean = false,
            val actionWithValue: String = ""
        )

        sealed class Effect {
            object EffectOne : Effect()
            data class EffectWithValue(val theValue: String) : Effect()
        }

        override fun mutate(action: Action): Flow<Mutation> = when (action) {
            is Action.SimpleAction ->
                flow { emit(Mutation.SimpleActionMutation) }

            is Action.ActionWithValue ->
                flow { emit(Mutation.ActionWithValueMutation(action.theValue)) }

            is Action.ActionFiresEffectOne ->
                flow { emitEffect(Effect.EffectOne) } // Note, flow doesn't emit

            is Action.ActionFiresEffectWithValue ->
                flow { emitEffect(Effect.EffectWithValue(action.theValue)) } // Note, flow doesn't emit

            is Action.ActionFiresTwoEffects ->
                flow {
                    val effects = listOf(Effect.EffectOne, Effect.EffectWithValue(action.theValue))
                    emitEffect(effects.asFlow())
                } // Note, flow doesn't emit

            is Action.ActionThrows ->
                throw action.throwable

            is Action.MutationThrows ->
                listOf<Mutation>(Mutation.MutationThrows(action.throwable)).asFlow()
        }

        override fun reduce(state: State, mutation: Mutation): State = when (mutation) {
            is Mutation.SimpleActionMutation -> state.copy(simpleAction = true)

            is Mutation.ActionWithValueMutation -> state.copy(actionWithValue = mutation.theValue)

            is Mutation.MutationThrows -> throw mutation.throwable
        }
    }
}
