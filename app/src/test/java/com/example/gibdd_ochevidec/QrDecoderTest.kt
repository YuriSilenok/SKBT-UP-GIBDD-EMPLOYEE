package com.example.gibdd_ochevidec

import com.example.gibdd_ochevidec.network.QrDecoder
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class QrDecoderTest {
    @Test fun decodes_uuid_from_camera_plane_with_padding_and_offset() {
        val expected = "7ee150d8-3157-43d7-a966-19f44dc750af"
        val qr = MultiFormatWriter().encode(expected, BarcodeFormat.QR_CODE, 240, 240)
        val stride = 240 * 2 + 17
        val buffer = ByteBuffer.allocate(23 + stride * 240)
        buffer.position(23)
        for (y in 0 until 240) for (x in 0 until 240) buffer.put(23 + y * stride + x * 2, if (qr[x, y]) 0 else 255.toByte())
        assertEquals(expected, QrDecoder.decodeYPlane(buffer, 240, 240, stride, 2))
        assertEquals(23, buffer.position())
    }
    @Test fun empty_camera_image_is_not_a_qr_code() {
        val bytes = ByteArray(240 * 240) { 255.toByte() }
        assertNull(QrDecoder.decodeYPlane(ByteBuffer.wrap(bytes), 240, 240, 240, 1))
    }
    @Test fun malformed_camera_plane_is_rejected_without_reading_past_buffer() {
        assertNull(QrDecoder.decodeYPlane(ByteBuffer.allocate(10), 240, 240, 240, 1))
        assertNull(QrDecoder.decodeYPlane(ByteBuffer.allocate(10), Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, 1))
    }
}
