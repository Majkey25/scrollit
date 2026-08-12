package cz.teply.scrollit

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun touchModeIsSelectedByDefault() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ScrollSettingsStore.save(context, ScrollSettings.defaults)

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.modeTouchButton)).check(matches(isChecked()))
            onView(withId(R.id.modeAutoScrollButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun selectingAutoScrollModePersistsIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ScrollSettingsStore.save(context, ScrollSettings.defaults)

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                onView(withId(R.id.modeAutoScrollButton)).perform(click())
                assertEquals(ScrollMode.AUTO_SCROLL, ScrollSettingsStore.load(context).mode)
            }
        } finally {
            ScrollSettingsStore.save(context, ScrollSettings.defaults)
        }
    }

    @Test
    fun mainScreenShowsPrimaryControl() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.launchOverlayButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mainScreenPrioritizesSpeedAndCollapsesMotionDetails() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.speedSeekBar)).check(matches(isDisplayed()))
            onView(withId(R.id.advancedSettingsButton)).check(matches(isDisplayed()))
            onView(withId(R.id.advancedSettingsContent)).check(matches(withEffectiveVisibility(GONE)))
        }
    }

    @Test
    fun mainScreenRefreshesSpeedAfterOverlayChange() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ScrollSettingsStore.save(context, ScrollSettings.defaults)

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.moveToState(Lifecycle.State.CREATED)
                ScrollSettingsStore.save(
                    context,
                    ScrollSettings.defaults.copy(speedLevel = 16),
                )
                scenario.moveToState(Lifecycle.State.RESUMED)

                onView(withId(R.id.speedValueText)).check(matches(withText("16 / 30")))
            }
        } finally {
            ScrollSettingsStore.save(context, ScrollSettings.defaults)
        }
    }

    @Test
    fun overlayAccessibilityWarning_isHiddenByDefault() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val themedContext = ContextThemeWrapper(context, R.style.Theme_ScrollIt)
        val view = LayoutInflater.from(themedContext).inflate(R.layout.overlay_controls, null, false)

        assertEquals(View.GONE, view.findViewById<View>(R.id.permissionStatusText).visibility)
    }
}
