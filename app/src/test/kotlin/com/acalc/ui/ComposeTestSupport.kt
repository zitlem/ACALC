package com.acalc.ui

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue

/**
 * Shared helpers for the Compose UI tests. These run on the JVM through Robolectric, so the whole
 * suite (`./gradlew test`) is runnable without an emulator or a connected device.
 */

/** Phone-sized screen so portrait layouts lay out the way they do on a real device. */
const val PHONE_QUALIFIERS = "w411dp-h891dp"

/** The same phone rotated — drives the separate `if (isLandscape)` layouts. */
const val LANDSCAPE_QUALIFIERS = "w891dp-h411dp-land"

/** A short screen, so the converter rows fall back to their compact display modes. */
const val SHORT_PHONE_QUALIFIERS = "w411dp-h540dp"

/** Matches tappable controls (buttons, chips, tabs) while excluding editable text fields. */
private fun isTappableControl() = SemanticsMatcher("is a tappable control") { node ->
    node.config.contains(SemanticsActions.OnClick) && !node.config.contains(SemanticsActions.SetText)
}

/** The app stores all of its UI preferences in one file; wipe it so tests start from defaults. */
fun clearAppPreferences() {
    ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .commit()
}

/**
 * Taps the button/chip/tab labelled [label], scrolling it into view first. Scrollable tab rows
 * compose their off-screen tabs, and a click on a node outside the viewport is never delivered —
 * so the scroll is what makes tapping e.g. the last CNC tab work.
 */
fun ComposeContentTestRule.tap(label: String) {
    val node = onAllNodes(hasText(label) and isTappableControl()).onFirst()
    runCatching { node.performScrollTo() }   // no-op when there is no scrollable ancestor
    node.performClick()
    waitForIdle()
}

/** Replaces the contents of the text field whose label contains [fieldLabel]. */
fun ComposeContentTestRule.enterText(fieldLabel: String, text: String) {
    val field = onAllNodes(hasSetTextAction() and hasText(fieldLabel, substring = true)).onFirst()
    field.performTextClearance()
    field.performTextInput(text)
    waitForIdle()
}

/** Empties the text field whose label contains [fieldLabel]. */
fun ComposeContentTestRule.clearText(fieldLabel: String) {
    onAllNodes(hasSetTextAction() and hasText(fieldLabel, substring = true))
        .onFirst()
        .performTextClearance()
    waitForIdle()
}

private fun ComposeContentTestRule.countOf(text: String, substring: Boolean = false) =
    onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().size

/** Asserts that at least one node on screen shows exactly [text]. */
fun ComposeContentTestRule.assertShows(text: String) {
    assertTrue("expected a node showing \"$text\"", countOf(text) > 0)
}

/** Asserts that at least one node on screen contains [text]. */
fun ComposeContentTestRule.assertShowsContaining(text: String) {
    assertTrue("expected a node containing \"$text\"", countOf(text, substring = true) > 0)
}

/** Asserts that nothing on screen shows [text]. */
fun ComposeContentTestRule.assertDoesNotShow(text: String) {
    assertTrue("expected no node showing \"$text\"", countOf(text) == 0)
}

/** Asserts that nothing on screen contains [text]. */
fun ComposeContentTestRule.assertDoesNotShowContaining(text: String) {
    assertTrue("expected no node containing \"$text\"", countOf(text, substring = true) == 0)
}
