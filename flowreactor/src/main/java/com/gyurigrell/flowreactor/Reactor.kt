package com.gyurigrell.flowreactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan

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
//@FlowPreview
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

//    private val stateChannel = MutableStateFlow(initialState)
//
//    /**
//     * The state stream output from the reactor, emitting every time the state is modified via a mutation.
//     */
//    val state: Flow<State> = stateChannel

    val state: Flow<State> by lazy { createStateStream(scope).receiveAsFlow() }

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
     *
     */
    open fun transformAction(action: Flow<Action>): Flow<Action> =
        action.onEach { println("[${Thread.currentThread().name}] transformAction: $it") }

    /**
     *
     */
    open fun transformMutation(mutation: Flow<Mutation>): Flow<Mutation> =
        mutation.onEach { println("[${Thread.currentThread().name}] transformMutation: $it") }

    /**
     *
     */
    open fun transformState(state: Flow<State>): Flow<State> =
        state.onEach { println("[${Thread.currentThread().name}] transformState: $it") }

    private fun createStateStream(scope: CoroutineScope): ReceiveChannel<State> {
        val actionFlow = action.asFlow()
        val transformedActionFlow = transformAction(actionFlow)
        val mutationFlow = transformedActionFlow
            .flatMapLatest { action ->
                try {
                    mutate(action)
                        .catch {
                            println("Encountered error in flow while processing action: $it")
                        }
                } catch (ex: Throwable) {
                    println("Encountered error processing action: $ex")
                    emptyFlow()
                }
            }
        val transformedMutationFlow = transformMutation(mutationFlow)
        val stateFlow = transformedMutationFlow
            .scan(initialState) { state, mutation ->
                try {
                    println("[${Thread.currentThread().name}] State before reduce: $state")
                    val newState = reduce(state, mutation)
                    println("[${Thread.currentThread().name}] State after reduce: $newState")
                    newState
                } catch (ex: Throwable) {
                    println("Encountered error mutating state: $ex")
                    state
                }
            }
            .catch {
                println("Encountered error in flow while mutating state: $it")
            }
        val transformedState = transformState(stateFlow)
            .onCompletion {
                println("[${Thread.currentThread().name}] state flow completed, cancelling action")
            }
            .onEach {
                currentState = it
                println("[${Thread.currentThread().name}] currentState changed to: $currentState")
            }
            .broadcastIn(scope)
        return transformedState.openSubscription()
    }
}
