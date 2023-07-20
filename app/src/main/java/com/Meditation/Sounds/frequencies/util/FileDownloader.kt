package com.Meditation.Sounds.frequencies.util

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL


object FileDownloader {
    fun download(
        url: String,
        file: File,
        onProgressChange: ((downloaded: Long, total: Long) -> Unit)? = null
    ): Boolean {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        return try {
            val request = URL(url)
            val connection = request.openConnection()
            connection.connect()
            val fileLength: Long = connection.contentLength.toLong()
            inputStream = request.openConnection().getInputStream()
            val data = ByteArray(8 * 1024)
            var total: Long = 0
            var count: Int
            if(file.exists()){
                file.delete()
            }
            file.createNewFile()
            outputStream = FileOutputStream(file)
            while (inputStream.read(data).also { count = it } != -1) {
                // allow canceling with back button
                total += count.toLong()
                // publishing the progress....
                if (fileLength > 0) {
                    if (onProgressChange != null) {
                        onProgressChange(total, fileLength)
                    }
                }
                outputStream.write(data, 0, count)
            }
            true
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }
}
