import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.calai.app.ui.VideoPlayerRaw   // 確保路徑正確

@Composable
fun LandingVideo(
    modifier: Modifier,
    @RawRes resId: Int,
    placeholderColor: Color = Color(0xFFF2F2F2)
) {
    var showVideo by remember { mutableStateOf(false) }

    // 等第一幀繪製完成再載入播放器，避免卡在啟動畫面
    LaunchedEffect(Unit) {
        withFrameNanos { _: Long -> }   // 明確標型別避免 "Cannot infer type"
        showVideo = true
    }

    if (showVideo) {
        // 真正的播放器（內部才會用到 Media3）
        VideoPlayerRaw(resId = resId, modifier = modifier)
    } else {
        // 與影片容器同尺寸的佔位，避免版面跳動
        Box(modifier = modifier.background(placeholderColor))
    }
}
