package com.example.gibdd_ochevidec.network

import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer

/** Decode locally, respecting camera Y-plane row/pixel padding and buffer position. */
object QrDecoder {
    fun decodeYPlane(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int): String? {
        if (width <= 0 || height <= 0 || pixelStride <= 0 || rowStride < width.toLong() * pixelStride || width.toLong() * height > 16_000_000) return null
        val start = buffer.position()
        val last = start.toLong() + (height - 1L) * rowStride + (width - 1L) * pixelStride
        if (last >= buffer.limit()) return null
        val bytes = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) bytes[y * width + x] = buffer.get(start + y * rowStride + x * pixelStride)
        return try {
            val source = PlanarYUVLuminanceSource(bytes, width, height, 0, 0, width, height, false)
            QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text.trim()
        } catch (_: com.google.zxing.ReaderException) { null }
    }
}
