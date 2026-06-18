package com.gpsspy.gpstracker.utils

import com.gpsspy.gpstracker.data.database.LocationPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxGeneratorTest {

    @Test
    fun `generateGpx with empty list returns valid empty gpx`() {
        val gpxString = GpxGenerator.generateGpx(emptyList(), "Empty Session")

        assertTrue(gpxString.contains("<?xml version=\"1.0\" encoding=\"UTF-8\""))
        assertTrue(gpxString.contains("<gpx "))
        assertTrue(gpxString.contains("<name>Empty Session</name>"))
        assertFalse(gpxString.contains("<trkpt"))
        assertTrue(gpxString.contains("</gpx>"))
    }

    @Test
    fun `generateGpx with points returns correct trkpt tags`() {
        val points = listOf(
            LocationPoint(
                id = 1L,
                sessionId = 100L,
                latitude = 25.0330,
                longitude = 121.5654,
                altitude = 100.0,
                speed = 0f,
                bearing = 0f,
                timestamp = 1672531200000L // 2023-01-01T00:00:00Z
            ),
            LocationPoint(
                id = 2L,
                sessionId = 100L,
                latitude = 25.0340,
                longitude = 121.5660,
                altitude = 110.5,
                speed = 2.0f,
                bearing = 90f,
                timestamp = 1672531260000L // 2023-01-01T00:01:00Z
            )
        )

        val gpxString = GpxGenerator.generateGpx(points, "Test Route")

        assertTrue(gpxString.contains("<name>Test Route</name>"))
        assertTrue(gpxString.contains("<trkpt lat=\"25.033\" lon=\"121.5654\">"))
        assertTrue(gpxString.contains("<ele>100.0</ele>"))
        assertTrue(gpxString.contains("<time>2023-01-01T00:00:00Z</time>"))

        assertTrue(gpxString.contains("<trkpt lat=\"25.034\" lon=\"121.566\">"))
        assertTrue(gpxString.contains("<ele>110.5</ele>"))
        assertTrue(gpxString.contains("<time>2023-01-01T00:01:00Z</time>"))
    }

    @Test
    fun `generateGpx correctly escapes special xml characters`() {
        val points = listOf(
            LocationPoint(
                id = 1L,
                sessionId = 200L,
                latitude = 0.0,
                longitude = 0.0,
                altitude = 0.0,
                speed = 0f,
                bearing = 0f,
                timestamp = 1672531200000L
            )
        )

        val dangerousName = "A & B < C > D \" E ' F"
        val gpxString = GpxGenerator.generateGpx(points, dangerousName)

        val expectedEscapedName = "A &amp; B &lt; C &gt; D &quot; E &apos; F"
        assertTrue(gpxString.contains("<name>$expectedEscapedName</name>"))
        assertFalse(gpxString.contains("<name>$dangerousName</name>"))
    }
}
