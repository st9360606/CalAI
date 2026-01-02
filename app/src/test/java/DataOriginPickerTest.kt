import com.calai.app.data.activity.sync.DataOriginPrefs
import com.calai.app.data.activity.sync.pickFirstExistingOrigin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DataOriginPickerTest {

    @Test
    fun pick_prefersGoogleFit_thenSamsung_thenAndroid() = runBlocking {
        val chosen = pickFirstExistingOrigin(DataOriginPrefs.preferred) { pkg ->
            pkg == DataOriginPrefs.SAMSUNG_HEALTH
        }
        assertEquals(DataOriginPrefs.SAMSUNG_HEALTH, chosen)
    }
}
