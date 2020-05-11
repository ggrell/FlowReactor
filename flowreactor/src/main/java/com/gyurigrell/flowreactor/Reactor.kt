package com.gyurigrell.flowreactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

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
@FlowPreview
@ExperimentalCoroutinesApi
abstract class Reactor<Action, Mutation, State>(
    scope: CoroutineScope,
    private val initialState: State
) {
    /**
     * Accepts the actions from the view, which then potentially cause mutations of the current state.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val action = BroadcastChannel<Action>(Channel.BUFFERED)

    /**
     * The current state of the view to which the reactor is bound.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var currentState: State = initialState
        private set

    private val stateChannel = MutableStateFlow(initialState)

    /**
     * The state stream output from the reactor, emitting every time the state is modified via a mutation.
     */
    val state: Flow<State> = stateChannel

    /**
     * Commits mutation from the action. This is the best place to perform side-effects such as async tasks.
     * @param action the action initiated by the user on the view
     * @return an observable which emits 0..n mutations
     */
    open fun mutate(action: Action): Flow<Mutation> {
        return emptyFlow()
    }

    /**
     * Given the current state and a mutation, returns the mutated state.
     * @param state the current state
     * @param mutation the mutation to apply to the state
     * @return the mutated state
     */
    open fun reduce(state: State, mutation: Mutation): State {
        return state
    }

    /**
     *
     */
    open fun transformAction(action: Flow<Action>): Flow<Action> {
        return action
    }

    /**
     *
     */
    open fun transformMutation(mutation: Flow<Mutation>): Flow<Mutation> {
        return mutation
    }

    /**
     *
     */
    open fun transformState(state: Flow<State>): Flow<State> {
        return state
    }

    init {
        scope.launch {
            val actionFlow = action.asFlow()
            val transformedActionFlow = transformAction(actionFlow)
            val mutationFlow = transformedActionFlow
                .flatMapConcat { action ->
                    try {
                        mutate(action)
                    } catch (ex: Throwable) {
                        println("Encountered error executing action: $ex")
                        emptyFlow<Mutation>()
                    }
                }
            val transformedMutationFlow = transformMutation(mutationFlow)
            val stateFlow = transformedMutationFlow
                .scan(initialState) { state, mutation ->
                    try {
                        reduce(state, mutation)
                    } catch (ex: Throwable) {
                        println("Encountered error mutating state: $ex")
                        state
                    }
                }
            transformState(stateFlow)
                .onCompletion {
                    println("state flow completed, cancelling action")
                }
                .collect {
                    currentState = it
                    stateChannel.value = it
                }
        }
    }
}
