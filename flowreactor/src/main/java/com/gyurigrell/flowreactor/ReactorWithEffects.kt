package com.gyurigrell.flowreactor

import com.gyurigrell.flowreactor.ReactorWithEffects.MutationWithEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transform

/**
 * A Reactor is an UI-independent layer which manages the state of a view. The foremost role of a
 * reactor is to separate control flow from a view. Every view has its corresponding reactor and
 * delegates all logic to its reactor. A reactor has no dependency to a view, so it can be easily
 * tested.
 *
 * @param Action the type of the action, which is generally either an enum or a Kotlin sealed class. Actions need to be
 * publicly available since actions are passed to the reactor via this type (using the {@see action} relay observer.
 * @param Mutation the type of the mutation. This type is only used internally in the reactor to map an action to  0..n
 * mutations. It must implement [MutationWithEffect], and a single mutation should override `effect` and provide a
 * non-null value.
 * @param State the type of the state that the reactor holds and modifies.
 * @param Effect the type of the effect that is emitted for side-effects that don't modify state
 * @property initialState the initial state of the reactor, from which the {@see currentState} will be initialized.
 */
@ExperimentalCoroutinesApi
abstract class ReactorWithEffects<Action, Mutation : MutationWithEffect<Effect>, State, Effect>(
    scope: CoroutineScope,
    initialState: State
) : Reactor<Action, Mutation, State>(scope, initialState) {
    /**
     * The effect stream output from the reactor.
     */
    val effect: Flow<Effect> by lazy { transformEffect(effectChannel) }

    private val effectChannel = MutableSharedFlow<Effect>()

    /**
     * Checks to see if the mutation has an effect set. If it does, emits it via [ReactorWithEffects.effectChannel] and
     * swallows the [Mutation], otherwise lets the [Mutation] pass through.
     */
    override fun transformMutation(mutation: Flow<Mutation>): Flow<Mutation> = mutation
        .transform { m ->
            if (m.effect == null) {
                // This is not an effect, so just emit the mutation
                emit(m)
            } else {
                // This is an effect, so emit the effect and ignore the mutation
                effectChannel.emit(m.effect!!)
            }
        }

    /**
     * Override to modify the emitted effects
     */
    open fun transformEffect(effect: Flow<Effect>): Flow<Effect> = effect

    /**
     * The interface that needs to be applied to the [Mutation] sealed class defined in this [ReactorWithEffects]. It
     * applies a field named [effect] which defaults to `null`, meaning that mutation doesn't emit effects. Generally
     * there should only be a single mutation that has an override where it provides an effect.
     * @param Effect this is just the [Effect] type defined in the reactor.
     * ```
     *     sealed class Mutation: MutationWithEffect<Effect> {
     *         object Mutation1 : Mutation()
     *         data class Mutation2(val someValue): Mutation()
     *         data class EmitEffect(override val effect: Effect): Mutation()
     *     }
     *  ```
     */
    interface MutationWithEffect<Effect> {
        val effect: Effect?
            get() = null
    }
}
