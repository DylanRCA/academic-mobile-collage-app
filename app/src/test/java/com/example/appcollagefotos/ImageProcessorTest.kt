package com.example.appcollagefotos

import com.example.appcollagefotos.util.ImageProcessor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImageProcessorTest {

    @Test
    fun testBase64EncodingAndDecoding() {
        val originalText = "Interactive Collage Event Photo Payload"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encodedBase64 = ImageProcessor.encodeByteArrayToBase64(originalBytes)
        assertNotNull(encodedBase64)

        val decodedBytes = ImageProcessor.decodeBase64ToByteArray(encodedBase64)
        assertNotNull(decodedBytes)
        assertArrayEquals(originalBytes, decodedBytes)

        val decodedText = String(decodedBytes!!, Charsets.UTF_8)
        assertEquals(originalText, decodedText)
    }
}
