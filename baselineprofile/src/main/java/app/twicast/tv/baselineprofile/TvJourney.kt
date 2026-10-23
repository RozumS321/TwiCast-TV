package app.twicast.tv.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/** The profiling build's id (see app/build.gradle.kts): it installs next to the release on a TV. */
const val TargetPackage = "app.twicast.tv.bench"

private const val ScreenTimeoutMillis = 15_000L
private const val SettleMillis = 1_500L
private const val PlaybackMillis = 6_000L

/** Cold start until home shows its rows (a signed-in profiling build: sign in once by hand). */
fun MacrobenchmarkScope.startAndWaitForHome() {
    pressHome()
    startActivityAndWait()
    device.wait(Until.hasObject(By.pkg(TargetPackage).depth(0)), ScreenTimeoutMillis)
    Thread.sleep(SettleMillis)
}

/** The everyday path with the remote: scroll home's rows, open a stream, back, then the other sections. */
fun MacrobenchmarkScope.browseWithRemote() {
    repeat(4) { device.pressDPadRight() }
    repeat(2) { device.pressDPadDown() }
    repeat(4) { device.pressDPadRight() }
    repeat(2) { device.pressDPadUp() }
    device.pressDPadCenter()
    Thread.sleep(PlaybackMillis)
    device.pressBack()
    Thread.sleep(SettleMillis)
    // Sidebar: Categories, then Following, then Settings, each opened and scrolled a little.
    device.pressDPadLeft()
    repeat(2) { device.pressDPadDown() }
    openSectionAndScroll()
    device.pressDPadLeft()
    device.pressDPadDown()
    openSectionAndScroll()
    device.pressDPadLeft()
    device.pressDPadDown()
    openSectionAndScroll()
}

private fun MacrobenchmarkScope.openSectionAndScroll() {
    device.pressDPadCenter()
    Thread.sleep(SettleMillis)
    repeat(3) { device.pressDPadDown() }
    repeat(2) { device.pressDPadRight() }
    Thread.sleep(SettleMillis)
}
