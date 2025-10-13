package com.calai.app.ui.landing

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 一張投影片資料
 */
data class SlideItem(
    @DrawableRes val imageResId: Int,
    val contentDescription: String // for a11y
)

/**
 * Landing 幻燈片
 * - 支援自動輪播（可關）
 * - 支援點擊左/右半邊快速切換
 * - 支援手勢左右滑
 */
@Composable
fun LandingSlideshow(
    modifier: Modifier = Modifier,
    slides: List<SlideItem>,
    autoPlay: Boolean = true,
    autoPlayIntervalMs: Long = 2800L,
    indicatorSpacing: Dp = 6.dp,
    indicatorSize: Dp = 6.dp,
    indicatorActiveWidth: Dp = 18.dp
) {
    val safeSlides = if (slides.isEmpty()) {
        // 預覽或資產未就緒時的保護
        listOf(SlideItem(android.R.drawable.ic_menu_gallery, "placeholder"))
    } else slides

    val pagerState = rememberPagerState(initialPage = 0) { safeSlides.size }
    val scope = rememberCoroutineScope()

    // 自動輪播（在 Preview / Inspect 模式停用）
    val inPreview = LocalInspectionMode.current
    LaunchedEffect(autoPlay, safeSlides.size) {
        if (!autoPlay || inPreview || safeSlides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(autoPlayIntervalMs)
            val next = (pagerState.currentPage + 1) % safeSlides.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 點擊：左半邊上一張，右半邊下一張
            .pointerInput(safeSlides.size) {
                detectTapGestures { offset ->
                    val width = size.width
                    if (width > 0) {
                        val isLeft = offset.x < width / 2f
                        scope.launch {
                            val target = if (isLeft) {
                                (pagerState.currentPage - 1 + safeSlides.size) % safeSlides.size
                            } else {
                                (pagerState.currentPage + 1) % safeSlides.size
                            }
                            pagerState.animateScrollToPage(target)
                        }
                    }
                }
            }
            // 無障礙語意：整體視為影像輪播
            .semantics {
                role = Role.Image
                contentDescription = "Landing slideshow"
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = safeSlides[page]
            Image(
                painter = painterResource(id = item.imageResId),
                contentDescription = item.contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 指示器
        SlideIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            pageCount = safeSlides.size,
            currentPage = pagerState.currentPage,
            spacing = indicatorSpacing,
            dotSize = indicatorSize,
            activeWidth = indicatorActiveWidth
        )
    }
}

@Composable
private fun SlideIndicator(
    modifier: Modifier = Modifier,
    pageCount: Int,
    currentPage: Int,
    spacing: Dp,
    dotSize: Dp,
    activeWidth: Dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val targetAlpha = if (isActive) 1f else 0.35f
            val alpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(durationMillis = 220, easing = LinearEasing),
                label = "dotAlpha"
            )

            Box(
                modifier = Modifier
                    .height(dotSize)
                    .width(if (isActive) activeWidth else dotSize)
                    .alpha(alpha)
                    .semantics {
                        contentDescription = "slide_indicator_${index + 1}_of_$pageCount"
                    }
                    .padding(horizontal = spacing / 2)
                    .then(
                        Modifier
                            .wrapContentSize()
                            .padding(vertical = 2.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.92f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension / 2)
                    )
                }
            }
        }
    }
}
