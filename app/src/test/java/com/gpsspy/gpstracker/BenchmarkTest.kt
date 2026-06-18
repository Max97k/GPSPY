package com.gpsspy.gpstracker

import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.system.measureTimeMillis

class BenchmarkTest {
    @Test
    fun benchmarkSimpleDateFormat() {
        val iterations = 100_000

        // Warmup
        for (i in 1..10_000) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }

        // Baseline: create it once
        val createOnceTime = measureTimeMillis {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (i in 1..iterations) {
                val f = format
            }
        }

        // Overhead: create it repeatedly
        val createManyTime = measureTimeMillis {
            for (i in 1..iterations) {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            }
        }

        println("--------------------------------------------------")
        println("Baseline (create once, access $iterations times): $createOnceTime ms")
        println("Overhead (create $iterations times): $createManyTime ms")
        println("--------------------------------------------------")
    }
}
