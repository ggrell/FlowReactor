package com.gyurigrell.flowreactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect

/**
 * A Reactor is an UI-independent layer which manages the state of a view. The foremost role of a
 * reactor is to separate control flow from a view. Every view has its corresponding reactor and
 * delegates all logic to its reactor. A reactor has no dependency to a view, so it can be easily
 * tested.
 *
 * @param Action the type of the action, which is generally either an enum or a Kotlin sealed class.
 * Actions need to be publicly available since actions are passed to the reactor via this type
 * (using the [action] relay observer.
 * @param Mutation the type of the mutation. This type is only used internally in the reactor to map
 * an action to  0..n mutations.
 * @param State the type of the state that the reactor holds and modifies.
 * @param Effect the type of the effect that is emitted for side-effects that don't modify state. Effects
 * can be emitted using the [emitEffect] functions.
 * @property initialState the initial state of the reactor, from which the [currentState] will be
 * initialized.
 */
@ExperimentalCoroutinesApi
abstract class ReactorWithEffects<Action, Mutation, State, Effect>(
    scope: CoroutineScope,
    initialState: State
) : Reactor<Action, Mutation, State>(scope, initialState) {
    /**
     * The effect stream output from the reactor.
     */
    val effect: Flow<Effect> by lazy { transformEffect(effectFlow) }

    /**
     * Override to modify the emitted effects
     */
    open fun transformEffect(effect: Flow<Effect>): Flow<Effect> = effect

    private val effectFlow = MutableSharedFlow<Effect>()

    /**
     * Emits all effects provided by the Observable
     * @param effect a Flow that emits effects
     */
    protected suspend fun emitEffect(effect: Flow<Effect>) {
        effect.collect { effectFlow.emit(it) }
    }

    /**
     * Simplified way to emits effects
     * @param effect one or more Effects to be emitted
     */
    protected suspend fun emitEffect(vararg effect: Effect) {
        effect.asFlow().collect { effectFlow.emit(it) }
    }
}
