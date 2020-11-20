package com.gyurigrell.flowreactor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.gyurigrell.flowreactor.databinding.ActivityLoginBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.widget.textChanges
import reactivecircus.flowbinding.lifecycle.events
import kotlin.time.ExperimentalTime

private const val REQUEST_READ_CONTACTS = 0

@ExperimentalTime
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var reactor: SampleReactor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reactor = SampleReactor(lifecycleScope, ContactServiceImpl(this))

        bindActions(reactor)
        bindViewState(reactor)
        populateAutoComplete()
    }

    private fun populateAutoComplete() {
        if (!mayRequestContacts()) {
            return
        }

        lifecycleScope.launch {
            reactor.action.emit(SampleReactor.Action.PopulateAutoComplete)
        }
    }

    private fun mayRequestContacts(): Boolean {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            Snackbar.make(binding.username, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok) {
                    requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS)
                }
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS)
        }
        return false
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }
    }

    private fun addEmailsToAutoComplete(possibleEmails: List<String>?) {
        val email = possibleEmails ?: return
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_dropdown_item_1line,
            email)

        binding.username.setAdapter(adapter)
    }

    private fun bindActions(reactor: SampleReactor) {
        lifecycle.events()
            .filter { it == Lifecycle.Event.ON_START }
            .onEach { reactor.action.emit(SampleReactor.Action.EnterScreen) }
            .launchIn(lifecycleScope)

        binding.username.textChanges()
            .skipInitialValue()
            .onEach { reactor.action.emit(SampleReactor.Action.UsernameChanged(it.toString())) }
            .launchIn(lifecycleScope)

        binding.password.textChanges()
            .skipInitialValue()
            .onEach { reactor.action.emit(SampleReactor.Action.PasswordChanged(it.toString())) }
            .launchIn(lifecycleScope)

        binding.login.clicks()
            .onEach { reactor.action.emit(SampleReactor.Action.Login) }
            .launchIn(lifecycleScope)
    }

    private fun bindViewState(reactor: SampleReactor) {
        reactor.state
            .map { it.loginEnabled }
            .distinctUntilChanged()
            .onEach(binding.login::setEnabled)
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.isBusy }
            .distinctUntilChanged()
            .onEach(binding.progressBar.visibility())
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.usernameMessage }
            .distinctUntilChanged()
            .onEach(binding.usernameLayout.errorRes())
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.usernameEnabled }
            .distinctUntilChanged()
            .onEach(binding.username::setEnabled)
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.passwordMessage }
            .distinctUntilChanged()
            .onEach(binding.passwordLayout.errorRes())
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.passwordEnabled }
            .distinctUntilChanged()
            .onEach(binding.password::setEnabled)
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.autoCompleteEmails }
            .distinctUntilChanged()
            .onEach(this::addEmailsToAutoComplete)
            .launchIn(lifecycleScope)

        reactor.effect
            .onEach(::println)
            .onEach(this::handleEffect)
            .launchIn(lifecycleScope)
    }

    private fun handleEffect(effect: SampleReactor.Effect) = when (effect) {
        is SampleReactor.Effect.ShowError ->
            Snackbar.make(binding.username, effect.messageId, Snackbar.LENGTH_LONG).show()

        is SampleReactor.Effect.LoggedIn ->
            Snackbar.make(binding.username, "Login succeeded", Snackbar.LENGTH_SHORT)
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        //finish()
                    }
                })
                .show()
    }
}
