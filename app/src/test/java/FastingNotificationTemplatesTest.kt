import com.calai.bitecal.data.fasting.notifications.FastingNotificationTemplates
import org.junit.Assert.assertEquals
import org.junit.Test

class FastingNotificationTemplatesTest {

    @Test
    fun render_placeholders() {
        val tpl = "Today {planCode} starts ({startTime}-{endTime})"
        val out = FastingNotificationTemplates.render(tpl, "16:8", "09:00", "17:00")
        assertEquals("Today 16:8 starts (09:00-17:00)", out)
    }
}