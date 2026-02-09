import com.calai.bitecal.data.foodlog.repo.MultipartParts
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class MultipartPartsTest {

    @Test
    fun imagePartFromFile_shouldCreatePart() {
        val tmp = File.createTempFile("t", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val part = MultipartParts.imagePartFromFile("file", "photo.jpg", tmp)

        assertNotNull(part)
        assertNotNull(part.body)
        assertNotNull(part.headers)
    }
}
