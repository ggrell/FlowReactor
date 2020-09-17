package com.gyurigrell.flowreactor

import android.accounts.Account
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.Serializable

/**
 * Do not check in without adding a comment!
 */
@FlowPreview
@ExperimentalCoroutinesApi
class SampleReactor(
    scope: CoroutineScope
) : ReactorWithEffects<SampleReactor.Action, SampleReactor.Mutation, SampleReactor.State, SampleReactor.Effect>(scope, State()) {

    sealed class Action {
        object EnterScreen : Action()
        data class UsernameChanged(val username: String) : Action()
        data class PasswordChanged(val password: String) : Action()
        object Login : Action()
        object PopulateAutoComplete : Action()
    }

    sealed class Mutation: MutationWithEffect<Effect> {
        data class SetUsername(val username: String) : Mutation()
        data class SetPassword(val password: String) : Mutation()
        data class SetBusy(val busy: Boolean) : Mutation()
        data class SetAutoCompleteEmails(val emails: List<String>) : Mutation()
        data class EmitEffect(override val effect: Effect) : Mutation()
    }

    sealed class Effect {
        data class ShowError(@StringRes val messageId: Int) : Effect()
        data class LoggedIn(val account: Account) : Effect()
    }

    data class State(
        val username: String = "",
        val password: String = "",
        val environment: String = "",
        val isUsernameValid: Boolean = true,
        val isPasswordValid: Boolean = true,
        val isBusy: Boolean = false,
        val autoCompleteEmails: List<String>? = null
    ) : Serializable {
        val loginEnabled: Boolean
            get() = (isUsernameValid && username.isNotEmpty()) && (isPasswordValid && password.isNotEmpty()) && !isBusy

        val usernameEnabled: Boolean
            get() = !isBusy

        val passwordEnabled: Boolean
            get() = !isBusy
    }

    override fun mutate(action: Action): Flow<Mutation> = when (action) {
        is Action.UsernameChanged -> flow {
            emit(Mutation.SetUsername(action.username))
        }

        is Action.PasswordChanged -> flow {
            emit(Mutation.SetPassword(action.password))
        }

        is Action.Login -> flow {
            emit(Mutation.SetBusy(true))
            val success = withContext(Dispatchers.IO) {
                login(currentState.username, currentState.password)
            }
            if (success) {
                emit(Mutation.EmitEffect(Effect.LoggedIn(Account(currentState.username,  "sample"))))
            } else {
                emit(Mutation.EmitEffect(Effect.ShowError(R.string.app_name)))
            }
            emit(Mutation.SetBusy(false))
        }

        else -> emptyFlow()
    }

    private suspend fun login(username: String, password: String): Boolean {
        println("[${Thread.currentThread().name}] Logging in user $username with password $password, takes 1 second")
        delay(1000)
        return true
    }

    override fun reduce(state: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetBusy ->
            state.copy(isBusy = mutation.busy)

        is Mutation.SetUsername ->
            state.copy(
                username = mutation.username,
                isUsernameValid = mutation.username.isNotBlank()
            )

        is Mutation.SetPassword ->
            state.copy(
                password = mutation.password,
                isUsernameValid = mutation.password.isNotBlank()
            )

        else ->
            state
    }

//    override fun transformAction(action: Flow<Action>): Flow<Action> =
//        action.onEach { println("[${Thread.currentThread().name}] transformAction: $it") }

//    override fun transformMutation(mutation: Flow<Mutation>): Flow<Mutation> =
//        mutation.onEach { println("[${Thread.currentThread().name}] transformMutation: $it") }

    override fun transformState(state: Flow<State>): Flow<State> =
        state.onEach { println("[${Thread.currentThread().name}] transformState: $it") }
}