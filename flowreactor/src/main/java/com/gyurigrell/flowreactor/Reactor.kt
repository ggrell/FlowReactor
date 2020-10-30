package com.gyurigrell.flowreactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn

/**
 * A Reactor is an UI-independent layer which manages the state of a view. The foremost role of a
 * reactor is to separate control flow from a view. Every view has its corresponding reactor and
 * delegates all logic to its reactor. A reactor has no dependency to a view, so it can be easily
 * tested.
 *
 * @param Action the type of the action, which is generally either an enum or a Kotlin sealed class. Actions need to be
 * publicly available since actions are passed to the reactor via this type (using the {@see action} relay observer.
 * @param Mutation the type of the mutation. This type is only used internally in the reactor to map an action to  0..n
 * mutations.
 * @param State the type of the state that the reactor holds and modifies.
 * @param scope the coroutine scope in which to execute the reactor instance
 * @property initialState the initial state of the reactor, from which the {@see currentState} will be initialized.
 * via {@link logDebug}
 */
@ExperimentalCoroutinesApi
abstract class Reactor<Action, Mutation, State>(
    scope: CoroutineScope,
    private val initialState: State
) {
    /**
     * Accepts the actions from the view, which then potentially cause mutations of the current state.
     */
    val action = MutableSharedFlow<Action>()

    /**
     * The current state of the view to which the reactor is bound.
     */
    var currentState: State = initialState
        private set

    /**
     * The state stream output from the reactor, emitting every time the state is modified via a mutation.
     */
    val state: Flow<State> by lazy { createStateStream(scope) }

    /**
     * Commits mutation from the action. This is the best place to perform side-effects such as async tasks.
     * @param action the action initiated by the user on the view
     * @return an observable which emits 0..n mutations
     */
    open fun mutate(action: Action): Flow<Mutation> = emptyFlow()

    /**
     * Given the current state and a mutation, returns the mutated state.
     * @param state the current state
     * @param mutation the mutation to apply to the state
     * @return the mutated state
     */
    open fun reduce(state: State, mutation: Mutation): State = state

    /**
     * Override to apply transformation to each action
     */
    open fun transformAction(action: Flow<Action>): Flow<Action> = action

    /**
     * Override to apply transformation to each mutation
     */
    open fun transformMutation(mutation: Flow<Mutation>): Flow<Mutation> = mutation

    /**
     * Override to apply transformation to each state
     */
    open fun transformState(state: Flow<State>): Flow<State> = state

    private fun createStateStream(scope: CoroutineScope): SharedFlow<State> {
        val transformedActionFlow = transformAction(action)
        val mutationFlow = transformedActionFlow
            .flatMapLatest { action ->
                try {
                    // If an error is caught in mutate, emit nothing but keep going
                    mutate(action).catch {}
                } catch (ex: Throwable) {
                    emptyFlow()
                }
            }
        val transformedMutationFlow = transformMutation(mutationFlow)
        val stateFlow = transformedMutationFlow
            .scan(initialState) { state, mutation ->
                try {
                    reduce(state, mutation)
                } catch (ex: Throwable) {
                    // Caught an error, return current state
                    state
                }
            }
            .catch {} // If error caught in flow, emit nothing but keep going
        return transformState(stateFlow)
            .onEach {
                currentState = it
            }
            .shareIn(scope, SharingStarted.Lazily, replay = 1)
    }
}
