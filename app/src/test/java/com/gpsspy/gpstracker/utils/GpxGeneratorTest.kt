package com.gpsspy.gpstracker.utils

import com.gpsspy.gpstracker.data.database.LocationPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxGeneratorTest {

    @Test
    fun generateGpx_emptyList_returnsValidBaseXml() {
        // Arrange
        val emptyPoints = emptyList<LocationPoint>()
        val sessionName = "Test Empty Session"

        // Act
        val result = GpxGenerator.generateGpx(emptyPoints, sessionName)

        // Assert
        assertTrue("Should contain XML declaration", result.contains("<?xml version=\"1.0\" encoding=\"UTF-8\""))
        assertTrue("Should contain GPX root tag", result.contains("<gpx xmlns="))
        assertTrue("Should contain metadata block", result.contains("<metadata>"))
        assertTrue("Should contain session name", result.contains("<name>Test Empty Session</name>"))
        assertTrue("Should contain track start", result.contains("<trk>"))
        assertTrue("Should contain track segment start", result.contains("<trkseg>"))
        assertTrue("Should contain track segment end", result.contains("</trkseg>"))
        assertTrue("Should contain track end", result.contains("</trk>"))
        assertTrue("Should contain GPX root end", result.contains("</gpx>"))

        // Ensure that edge cases logic in GpxGenerator works
        // The time tag in metadata shouldn't be included if points list is empty
        assertFalse("Should not contain time tag in metadata for empty points", result.contains("<time>"))

        // No track points should be generated
        assertFalse("Should not contain track point tags", result.contains("<trkpt"))
    }
}
