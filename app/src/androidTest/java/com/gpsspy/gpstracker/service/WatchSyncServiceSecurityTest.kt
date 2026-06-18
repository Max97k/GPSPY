package com.gpsspy.gpstracker.service

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchSyncServiceSecurityTest {

    @Test
    fun testServiceNotExported() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        val serviceInfo = packageManager.getServiceInfo(
            android.content.ComponentName(context, WatchSyncService::class.java),
            PackageManager.GET_META_DATA
        )

        assertFalse("WatchSyncService should not be exported to prevent unauthorized access", serviceInfo.exported)
    }
}
