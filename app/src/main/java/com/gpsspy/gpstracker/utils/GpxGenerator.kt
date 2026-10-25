package com.gpsspy.gpstracker.utils

import com.gpsspy.gpstracker.data.database.LocationPoint
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object GpxGenerator {

    private val isoFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    /**
     * Generates a valid GPX 1.1 XML string from a list of LocationPoints.
     * Includes <ele> tags for 3D trajectory rendering.
     */
    fun generateGpx(points: List<LocationPoint>, sessionName: String = "Tracking Session"): String {
        val sb = StringBuilder()
        writeGpx(sb, points, sessionName)
        return sb.toString()
    }

    /**
     * Streams a valid GPX 1.1 XML directly to an Appendable (e.g. Writer or StringBuilder).
     */
    fun writeGpx(writer: Appendable, points: List<LocationPoint>, sessionName: String = "Tracking Session") {
        // XML Header and GPX Root
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n")
        writer.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" ")
        writer.append("creator=\"GPSTrackerApp\" version=\"1.1\" ")
        writer.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
        writer.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")

        // Metadata
        writer.append("  <metadata>\n")
        writer.append("    <name>").append(escapeXml(sessionName)).append("</name>\n")
        if (points.isNotEmpty()) {
            writer.append("    <time>").append(formatTime(points.first().timestamp)).append("</time>\n")
        }
        writer.append("  </metadata>\n")

        // Track Start
        writer.append("  <trk>\n")
        writer.append("    <name>").append(escapeXml(sessionName)).append("</name>\n")
        writer.append("    <trkseg>\n")

        // Track Points
        for (point in points) {
            writer.append("      <trkpt lat=\"").append(point.latitude.toString()).append("\" lon=\"").append(point.longitude.toString()).append("\">\n")
            // Crucial for 3D: elevation
            writer.append("        <ele>").append(point.altitude.toString()).append("</ele>\n")
            // Time
            writer.append("        <time>").append(formatTime(point.timestamp)).append("</time>\n")
            writer.append("      </trkpt>\n")
        }

        // Track End
        writer.append("    </trkseg>\n")
        writer.append("  </trk>\n")
        writer.append("</gpx>\n")
    }

    private fun formatTime(timeMs: Long): String {
        return isoFormatter.format(Instant.ofEpochMilli(timeMs))
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
