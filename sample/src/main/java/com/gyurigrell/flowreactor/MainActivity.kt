package com.gyurigrell.flowreactor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.gyurigrell.flowreactor.databinding.ActivityMainBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.widget.textChanges
import reactivecircus.flowbinding.lifecycle.events
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var reactor: SampleReactor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reactor = SampleReactor(lifecycleScope)

        bindActions(reactor)
        bindViewState(reactor)
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
            .onEach(binding.login::setEnabled)
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.isBusy }
            .onEach(binding.progressBar.visibility())
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.usernameEnabled }
            .onEach(binding.username::setEnabled)
            .launchIn(lifecycleScope)

        reactor.state
            .map { it.passwordEnabled }
            .onEach(binding.password::setEnabled)
            .launchIn(lifecycleScope)
    }
}
