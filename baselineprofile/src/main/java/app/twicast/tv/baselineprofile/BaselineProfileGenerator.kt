package app.twicast.tv.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records what ART should compile ahead of time: cold start, home with the remote, a stream, the sections. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TargetPackage, includeInStartupProfile = true) {
        startAndWaitForHome()
        browseWithRemote()
    }
}
