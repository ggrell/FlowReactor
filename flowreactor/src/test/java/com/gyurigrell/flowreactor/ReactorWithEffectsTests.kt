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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
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
@FlowPreview
class ReactorWithEffectsTests {
    private val scope = TestCoroutineScope()

    @After
    fun teardown() {
        scope.cleanupTestCoroutines()
    }

    @Test
    fun `SimpleAction updates State simpleAction to true`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(scope)
        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(scope)

        // Act
        reactor.action.send(Action.SimpleAction)

        // Assert
        assertThat(states, equalTo(listOf(State(false), State(true))))
    }

    @Test
    fun `ActionWithValue updates State actionWithValue to correct string`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(scope)
        val theValue = "I love apple pie"
        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(scope)

        // Act
        reactor.action.send(Action.ActionWithValue(theValue))

        // Assert
        assertThat(states, equalTo(listOf(State(), State(false, theValue))))
    }

    @Test
    fun `ActionFiresEffectOne emits the effect `() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(scope)
        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(scope)
        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(scope)

        // Act
        reactor.action.send(Action.ActionFiresEffectOne)

        // Assert
        assertThat(states, equalTo(listOf(State())))
        assertThat(effects, equalTo(listOf(Effect.EffectOne)))
    }

    @Test
    fun `ActionFiresEffectWithValue emits the effect with the correct value`() = runBlockingTest {
        // Arrange
        val reactor = TestReactor(scope)
        val theValue = "Millions of peaches, peaches for me"
        val states = mutableListOf<State>()
        reactor.state.onEach { states.add(it) }.launchIn(scope)
        val effects = mutableListOf<Effect>()
        reactor.effect.onEach { effects.add(it) }.launchIn(scope)

        reactor.action.send(Action.ActionFiresEffectWithValue(theValue))

        // Assert
        assertThat(states, equalTo(listOf(State())))
        assertThat(effects, equalTo(listOf(Effect.EffectWithValue(theValue))))
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
        }

        sealed class Mutation : MutationWithEffect<Effect> {
            object SimpleActionMutation : Mutation()
            data class ActionWithValueMutation(val theValue: String) : Mutation()
            data class FireEffect(override val effect: Effect) : Mutation()
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
                flow { emit(Mutation.FireEffect(Effect.EffectOne)) }

            is Action.ActionFiresEffectWithValue ->
                flow { emit(Mutation.FireEffect(Effect.EffectWithValue(action.theValue))) }
        }

        override fun reduce(state: State, mutation: Mutation): State = when (mutation) {
            is Mutation.SimpleActionMutation -> state.copy(simpleAction = true)

            is Mutation.ActionWithValueMutation -> state.copy(actionWithValue = mutation.theValue)

            is Mutation.FireEffect -> state // This will never happen, but need to be exhaustive
        }
    }
}
