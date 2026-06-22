package com.gpsspy.gpstracker.utils

import com.gpsspy.gpstracker.data.database.LocationPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxGenerator {

    /**
     * Generates a valid GPX 1.1 XML string from a list of LocationPoints.
     * Includes <ele> tags for 3D trajectory rendering.
     */
    fun generateGpx(points: List<LocationPoint>, sessionName: String = "Tracking Session"): String {
        val sb = StringBuilder()

        // XML Header and GPX Root
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n")
        sb.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" ")
        sb.append("creator=\"GPSTrackerApp\" version=\"1.1\" ")
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
        sb.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")

        // Metadata
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(sessionName)).append("</name>\n")
        if (points.isNotEmpty()) {
            sb.append("    <time>").append(formatTime(points.first().timestamp)).append("</time>\n")
        }
        sb.append("  </metadata>\n")

        // Track Start
        sb.append("  <trk>\n")
        sb.append("    <name>").append(escapeXml(sessionName)).append("</name>\n")
        sb.append("    <trkseg>\n")

        // Track Points
        for (point in points) {
            sb.append("      <trkpt lat=\"").append(point.latitude).append("\" lon=\"").append(point.longitude).append("\">\n")
            // Crucial for 3D: elevation
            sb.append("        <ele>").append(point.altitude).append("</ele>\n")
            // Time
            sb.append("        <time>").append(formatTime(point.timestamp)).append("</time>\n")
            // Optional: speed and bearing (often rendered by advanced tools as extensions or not at all,
            // but speed is sometimes included in extensions or standard elements depending on schema).
            // We'll stick to standard GPX 1.1 <ele> and <time>.
            sb.append("      </trkpt>\n")
        }

        // Track End
        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")

        return sb.toString()
    }

    private fun formatTime(timeMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timeMs))
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}


