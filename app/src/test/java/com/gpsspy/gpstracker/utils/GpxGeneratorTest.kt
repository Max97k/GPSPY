package com.gpsspy.gpstracker.utils

import com.gpsspy.gpstracker.data.database.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxGeneratorTest {

    @Test
    fun generateGpx_emptyList_returnsValidGpxWithoutPoints() {
        val gpx = GpxGenerator.generateGpx(emptyList(), "Test Session")

        assertTrue(gpx.contains("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>"))
        assertTrue(gpx.contains("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\""))
        assertTrue(gpx.contains("<name>Test Session</name>"))
        assertTrue(gpx.contains("<trk>"))
        assertTrue(gpx.contains("<trkseg>"))
        assertTrue(gpx.contains("</trkseg>"))
        assertTrue(gpx.contains("</trk>"))
        assertTrue(gpx.contains("</gpx>"))

        // Should not contain track points or time tag in metadata if list is empty
        assertFalse(gpx.contains("<trkpt"))
        assertFalse(gpx.contains("<time>"))
    }

    @Test
    fun generateGpx_withPoints_returnsValidGpx() {
        // Assume epoch is UTC, no timezone conversion needed for formatting assertion if we know the UTC output
        val timestamp1 = 1704067200000L // 2024-01-01T00:00:00Z
        val timestamp2 = 1704067260000L // 2024-01-01T00:01:00Z

        val points = listOf(
            LocationPoint(
                id = 1,
                sessionId = 1,
                latitude = 25.0330,
                longitude = 121.5654,
                altitude = 10.5,
                speed = 0f,
                bearing = 0f,
                timestamp = timestamp1
            ),
            LocationPoint(
                id = 2,
                sessionId = 1,
                latitude = 25.0335,
                longitude = 121.5659,
                altitude = 12.0,
                speed = 1.5f,
                bearing = 45f,
                timestamp = timestamp2
            )
        )

        val gpx = GpxGenerator.generateGpx(points, "Taipei Walk")

        // Metadata checks
        assertTrue(gpx.contains("<name>Taipei Walk</name>"))
        assertTrue(gpx.contains("  <metadata>\n    <name>Taipei Walk</name>\n    <time>2024-01-01T00:00:00Z</time>\n  </metadata>"))

        // Track points checks
        assertTrue(gpx.contains("<trkpt lat=\"25.033\" lon=\"121.5654\">"))
        assertTrue(gpx.contains("<ele>10.5</ele>"))
        assertTrue(gpx.contains("<time>2024-01-01T00:00:00Z</time>"))

        assertTrue(gpx.contains("<trkpt lat=\"25.0335\" lon=\"121.5659\">"))
        assertTrue(gpx.contains("<ele>12.0</ele>"))
        assertTrue(gpx.contains("<time>2024-01-01T00:01:00Z</time>"))
    }

    @Test
    fun generateGpx_withSpecialCharactersInSessionName_escapesXml() {
        val sessionName = "Test & <Run> \"1\" '2'"
        val gpx = GpxGenerator.generateGpx(emptyList(), sessionName)

        assertTrue(gpx.contains("<name>Test &amp; &lt;Run&gt; &quot;1&quot; &apos;2&apos;</name>"))
    }
}
