package com.gyurigrell.flowreactor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class SampleReactorTest {
    @FlowPreview
    @ExperimentalCoroutinesApi
    @Test
    fun `each method is invoked`() = runBlocking {
        val reactor = SampleReactor(this)
        val d = async {
            println("async coroutine: $this")
            reactor.state.collect { println(it) }
            println("completed")
        }
        reactor.action.send(SampleReactor.Action.EnterScreen)
        d.await()
    }
}
