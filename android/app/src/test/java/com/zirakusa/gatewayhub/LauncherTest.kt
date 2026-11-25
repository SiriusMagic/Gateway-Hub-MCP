package com.zirakusa.gatewayhub

import android.content.Intent
import com.zirakusa.gatewayhub.assistant.AssistantActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class LauncherTest {
    @Test fun launcherOpensLuciaWithoutAnOwnerSession() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        Robolectric.buildActivity(MainActivity::class.java, launcher).create().use { controller ->
            val activity = controller.get()
            assertEquals(AssistantActivity::class.java.name, shadowOf(activity).nextStartedActivity?.component?.className)
            assertTrue(activity.isFinishing)
        }
    }

    @Test fun explicitAdministrationStaysOnTheHubSignInScreen() {
        val manageHub = Intent(MainActivity.ACTION_MANAGE_HUB)
        Robolectric.buildActivity(MainActivity::class.java, manageHub).create().use { controller ->
            val activity = controller.get()
            assertNull(shadowOf(activity).nextStartedActivity)
            assertFalse(activity.isFinishing)
        }
    }

    /** One app, one icon: «Arka» (Gateway Hub and Lucía together); Lucía is still the phone's assistant and a shortcut. */
    @Test fun oneIconNamedArka() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val pm = context.packageManager
        val icons = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName), 0)
        assertEquals(listOf("com.zirakusa.gatewayhub.Home"), icons.map { it.activityInfo.name })                // «Anillos»; «Órbitas» is off
        ArkaIcon.apply(context, ArkaIcon.ORBITS)
        assertEquals(listOf("com.zirakusa.gatewayhub.HomeOrbitas"), pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName), 0).map { it.activityInfo.name })
        ArkaIcon.apply(context, ArkaIcon.RINGS)
        assertEquals("Arka", context.getString(R.string.app_name))
        val assist = pm.queryIntentActivities(Intent(Intent.ACTION_ASSIST).setPackage(context.packageName), 0)
        assertEquals(listOf(AssistantActivity::class.java.name), assist.map { it.activityInfo.name })
    }
}
