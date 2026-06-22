package com.gpsspy.gpstracker.utils

import com.gpsspy.gpstracker.data.database.LocationPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxGeneratorTest {

    @Test
    fun testGenerateGpx_emptyList() {
        val result = GpxGenerator.generateGpx(emptyList(), "Empty Session")

        // Assert base XML structure
        assertTrue("XML header missing", result.contains("<?xml version=\"1.0\" encoding=\"UTF-8\""))
        assertTrue("gpx open tag missing", result.contains("<gpx"))
        assertTrue("gpx close tag missing", result.contains("</gpx>"))

        // Assert session name is in metadata and track
        assertTrue("Session name missing", result.contains("<name>Empty Session</name>"))

        // Assert time is absent since list is empty
        assertFalse("Time tag should not be present for empty list", result.contains("<time>"))

        // Assert no track points
        assertFalse("Track points should not be present", result.contains("<trkpt"))
    }

    @Test
    fun testGenerateGpx_withPoints() {
        val points = listOf(
            LocationPoint(
                id = 1,
                sessionId = 1,
                latitude = 37.7749,
                longitude = -122.4194,
                altitude = 10.0,
                speed = 0f,
                bearing = 0f,
                timestamp = 1609459200000L // 2021-01-01T00:00:00Z
            )
        )

        val result = GpxGenerator.generateGpx(points, "Test Session")

        // Assert track point is present with correct attributes
        assertTrue("Latitude/longitude missing", result.contains("<trkpt lat=\"37.7749\" lon=\"-122.4194\">"))
        assertTrue("Elevation missing", result.contains("<ele>10.0</ele>"))
        assertTrue("Time missing", result.contains("<time>2021-01-01T00:00:00Z</time>"))
    }
}
