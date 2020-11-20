package com.gyurigrell.flowreactor

import android.accounts.Account
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.Serializable
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Do not check in without adding a comment!
 */
@ExperimentalTime
@ExperimentalCoroutinesApi
class SampleReactor(
    scope: CoroutineScope,
    private val contactService: ContactService,
    initialState: State = State()
) : ReactorWithEffects<SampleReactor.Action, SampleReactor.Mutation, SampleReactor.State, SampleReactor.Effect>(
    scope,
    initialState
) {

    sealed class Action {
        object EnterScreen : Action()
        data class UsernameChanged(val username: String) : Action()
        data class PasswordChanged(val password: String) : Action()
        object Login : Action()
        object PopulateAutoComplete : Action()
    }

    sealed class Mutation {
        data class SetUsername(val username: String) : Mutation()
        data class SetPassword(val password: String) : Mutation()
        data class SetBusy(val busy: Boolean) : Mutation()
        data class SetAutoCompleteEmails(val emails: List<String>) : Mutation()
    }

    sealed class Effect {
        data class ShowError(@StringRes val messageId: Int) : Effect()
        data class LoggedIn(val account: Account) : Effect()
    }

    data class State(
        val username: String = "",
        @StringRes val usernameMessage: Int = 0,
        val password: String = "",
        @StringRes val passwordMessage: Int = 0,
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
            println("[${Thread.currentThread().name}] Set to busy, about to log in")
            val success = login(currentState.username, currentState.password)
            if (success) {
                emitEffect(Effect.LoggedIn(Account(currentState.username, "Sample")))
            } else {
                emitEffect(Effect.ShowError(R.string.login_error))
            }
            emit(Mutation.SetBusy(false))
        }

        is Action.PopulateAutoComplete -> flow {
            val emails = contactService.loadEmails()
            emit(Mutation.SetAutoCompleteEmails(emails))
        }
        else -> emptyFlow()
    }

    private suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        println("[${Thread.currentThread().name}] Logging in user $username with password $password")
        delay(1.seconds)
        println("[${Thread.currentThread().name}] login completed")
        Random.nextBoolean()
    }

    override fun reduce(state: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetBusy ->
            state.copy(isBusy = mutation.busy)

        is Mutation.SetUsername -> {
            val isUsernameValid = mutation.username.isNotBlank()
            state.copy(
                username = mutation.username,
                isUsernameValid = isUsernameValid,
                usernameMessage = if (isUsernameValid) 0 else R.string.empty_username
            )
        }

        is Mutation.SetPassword -> {
            val isPasswordValid = mutation.password.isNotBlank()
            state.copy(
                password = mutation.password,
                isPasswordValid = isPasswordValid,
                passwordMessage = if (isPasswordValid) 0 else R.string.empty_password
            )
        }

        is Mutation.SetAutoCompleteEmails ->
            state.copy(autoCompleteEmails = mutation.emails)
    }
}