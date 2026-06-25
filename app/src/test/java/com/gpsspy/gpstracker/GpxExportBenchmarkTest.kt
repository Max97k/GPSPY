package com.gpsspy.gpstracker

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GpxExportBenchmarkTest {

    @Test
    fun benchmarkGpxExport() {
        val gpxContent = "A".repeat(10 * 1024 * 1024) // 10MB string
        val tempFile = File.createTempFile("test_gpx", ".gpx")

        val timeBaseline = measureTimeMillis {
            FileOutputStream(tempFile).use {
                it.write(gpxContent.toByteArray())
            }
        }

        val timeOptimized = measureTimeMillis {
            runBlocking {
                withContext(Dispatchers.IO) {
                    FileOutputStream(tempFile).use {
                        it.write(gpxContent.toByteArray())
                    }
                }
            }
        }

        println("Baseline I/O time: $timeBaseline ms")
        println("Optimized IO time (withContext): $timeOptimized ms")
        println("Note: The primary improvement is moving off the Main thread, not necessarily reducing raw I/O time. Blocking the Main thread causes UI freezes.")
    }
}
