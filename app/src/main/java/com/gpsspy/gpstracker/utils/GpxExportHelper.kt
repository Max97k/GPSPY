package com.gpsspy.gpstracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.data.database.LocationPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object GpxExportHelper {

    suspend fun exportGpxFile(
        context: Context,
        points: List<LocationPoint>,
        sessionName: String,
        fileName: String
    ) {
        try {
            val file = withContext(Dispatchers.IO) {
                val path = File(context.cacheDir, "gpx_exports")
                if (!path.exists()) path.mkdirs()

                val targetFile = File(path, fileName)
                targetFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                    GpxGenerator.writeGpx(writer, points, sessionName)
                }
                targetFile
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, context.getString(R.string.chooser_export_gpx))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_export_failed, e.message.toString()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
