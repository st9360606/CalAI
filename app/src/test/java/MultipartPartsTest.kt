package com.calai.bitecal.data.foodlog.repo

import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MultipartPartsTest {

    @Test
    fun jpegImagePart_should_create_part_with_expected_headers_and_body() {
        val jpegBytes = byteArrayOf(
            0x01, 0x23, 0x45, 0x67
        )

        val part = MultipartParts.jpegImagePart(
            fieldName = "file",
            filename = "photo.jpg",
            jpegBytes = jpegBytes
        )

        val contentDisposition = part.headers?.get("Content-Disposition")
        assertNotNull(contentDisposition)
        assertTrue(contentDisposition!!.contains("name=\"file\""))
        assertTrue(contentDisposition.contains("filename=\"photo.jpg\""))

        assertEquals("image/jpeg", part.body.contentType()?.toString())

        val buffer = Buffer()
        part.body.writeTo(buffer)
        assertArrayEquals(jpegBytes, buffer.readByteArray())
    }

    @Test
    fun encodedImagePartFromFile_should_use_given_media_type_and_file_bytes() {
        val tempFile = File.createTempFile("multipart_test_", ".png")
        val fileBytes = byteArrayOf(
            0x11, 0x22, 0x33, 0x44, 0x55
        )
        tempFile.writeBytes(fileBytes)

        try {
            val part = MultipartParts.encodedImagePartFromFile(
                fieldName = "file",
                filename = "image.png",
                file = tempFile,
                mediaType = "image/png"
            )

            val contentDisposition = part.headers?.get("Content-Disposition")
            assertNotNull(contentDisposition)
            assertTrue(contentDisposition!!.contains("name=\"file\""))
            assertTrue(contentDisposition.contains("filename=\"image.png\""))

            assertEquals("image/png", part.body.contentType()?.toString())

            val buffer = Buffer()
            part.body.writeTo(buffer)
            assertArrayEquals(fileBytes, buffer.readByteArray())

        } finally {
            tempFile.delete()
        }
    }
}
