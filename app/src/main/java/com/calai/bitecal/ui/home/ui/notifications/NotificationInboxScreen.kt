package com.calai.bitecal.ui.home.ui.notifications

import android.os.SystemClock
import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.core.time.UtcTimeFormatter
import com.calai.bitecal.data.notifications.api.NotificationItemDto
import com.calai.bitecal.i18n.currentLocaleKey
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame
import com.calai.bitecal.ui.common.design.BiteCalTopBar
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationInboxScreen(
    loading: Boolean,
    error: String?,
    items: List<NotificationItemDto>,
    markingReadIds: Set<Long>,
    onRetry: () -> Unit,
    onMarkRead: (Long) -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminanceForUi() < 0.45f
    val bg = if (isDark) colorScheme.background else Color(0xFFF6F7F9)
    val backClick = rememberDebouncedClick(onClick = onBack)
    val retryClick = rememberDebouncedClick(onClick = onRetry)

    Scaffold(
        containerColor = bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BiteCalTopBar(
                title = stringResource(R.string.notification_inbox_title),
                onBack = backClick
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                loading -> NotificationInboxLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = NotificationInboxMaxWidth)
                )

                error != null -> NotificationInboxErrorState(
                    onRetry = retryClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = NotificationInboxMaxWidth)
                )

                items.isEmpty() -> NotificationInboxEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = NotificationInboxMaxWidth)
                )

                else -> NotificationInboxSuccessState(
                    items = items,
                    markingReadIds = markingReadIds,
                    onMarkRead = onMarkRead,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = NotificationInboxMaxWidth)
                )
            }
        }
    }
}

@Composable
private fun NotificationInboxSuccessState(
    items: List<NotificationItemDto>,
    markingReadIds: Set<Long>,
    onMarkRead: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NotificationInboxHeaderCard()
        }
        items(
            items = items,
            key = { item -> item.id }
        ) { item ->
            NotificationCard(
                item = item,
                markingRead = item.id in markingReadIds,
                onMarkRead = onMarkRead
            )
        }
    }
}

@Composable
private fun NotificationInboxHeaderCard() {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminanceForUi() < 0.45f
    val container = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color.White
    val border = if (isDark) colorScheme.outline.copy(alpha = 0.22f) else Color(0xFFE5E7EB)
    val titleColor = if (isDark) colorScheme.onSurface else Color(0xFF111114)
    val bodyColor = if (isDark) colorScheme.onSurfaceVariant else Color(0xFF6B7280)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(notificationBellBackgroundColor(isDark)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = notificationBellIconColor(isDark),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notification_inbox_header_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.notification_inbox_header_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItemDto,
    markingRead: Boolean,
    onMarkRead: (Long) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminanceForUi() < 0.45f
    val container = if (isDark) colorScheme.surface else Color.White
    val border = if (isDark) colorScheme.outline.copy(alpha = 0.22f) else Color(0xFFE5E7EB)
    val titleColor = if (isDark) colorScheme.onSurface else Color(0xFF111114)
    val bodyColor = if (isDark) colorScheme.onSurfaceVariant else Color(0xFF4B5563)
    val metaColor = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.78f) else Color(0xFF8A8F98)
    val unreadAccentColor = if (isDark) Color(0xFFFF8A8A) else Color(0xFFE5484D)

    val markReadClick = rememberDebouncedClick {
        if (!item.read && !markingRead) {
            onMarkRead(item.id)
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!item.read && !markingRead) {
                    Modifier.biteCalClickable(onClick = markReadClick)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.Top
        ) {
            val showUnreadDot = !item.read && !markingRead

            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (showUnreadDot) {
                            unreadAccentColor
                        } else {
                            Color.Transparent
                        }
                    )
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = if (item.read) FontWeight.Medium else FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    NotificationReadPill(
                        read = item.read,
                        markingRead = markingRead
                    )
                }

                Spacer(Modifier.height(7.dp))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(12.dp))

                val localeTag = currentLocaleKey()
                val locale = remember(localeTag) { localeFromTag(localeTag) }
                val use24HourTime = AndroidDateFormat.is24HourFormat(LocalContext.current)

                NotificationDateText(
                    text = formatNotificationCreatedAt(
                        raw = item.createdAtUtc,
                        todayText = stringResource(R.string.common_today),
                        yesterdayText = stringResource(R.string.common_yesterday),
                        locale = locale,
                        use24HourTime = use24HourTime
                    ),
                    color = metaColor
                )
            }
        }
    }
}

@Composable
private fun NotificationReadPill(
    read: Boolean,
    markingRead: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminanceForUi() < 0.45f
    val displayAsRead = read || markingRead

    val container = when {
        displayAsRead && isDark -> Color(0xFF143524)
        displayAsRead -> Color(0xFFEAF7EF)
        isDark -> colorScheme.error.copy(alpha = 0.20f)
        else -> Color(0xFFFFECEF)
    }

    val content = when {
        displayAsRead && isDark -> Color(0xFF7EE2A8)
        displayAsRead -> Color(0xFF2E9E5B)
        isDark -> Color(0xFFFF8A8A)
        else -> Color(0xFFE5484D)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(
                if (displayAsRead) {
                    R.string.notification_inbox_status_read
                } else {
                    R.string.notification_inbox_status_unread
                }
            ),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun NotificationInboxLoadingState(
    modifier: Modifier = Modifier
) {
    NotificationCenteredState(
        modifier = modifier,
        title = stringResource(R.string.notification_inbox_loading_title),
        body = stringResource(R.string.notification_inbox_loading),
        icon = {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                strokeWidth = 3.dp
            )
        },
        action = null
    )
}

@Composable
private fun NotificationInboxEmptyState(
    modifier: Modifier = Modifier
) {
    NotificationCenteredState(
        modifier = modifier,
        title = stringResource(R.string.notification_inbox_empty_title),
        body = stringResource(R.string.notification_inbox_empty_body),
        icon = { NotificationStateIcon(unread = false) },
        action = null
    )
}

@Composable
private fun NotificationInboxErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    NotificationCenteredState(
        modifier = modifier,
        title = stringResource(R.string.notification_inbox_error_title),
        body = stringResource(R.string.notification_inbox_error_body),
        icon = { NotificationStateIcon(unread = true) },
        action = {
            Button(
                onClick = rememberClickWithHaptic(onClick = onRetry),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111114),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Text(
                    text = stringResource(R.string.cta_retry),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun NotificationCenteredState(
    title: String,
    body: String,
    icon: @Composable () -> Unit,
    action: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminanceForUi() < 0.45f
    val titleColor = if (isDark) colorScheme.onSurface else Color(0xFF111114)
    val bodyColor = if (isDark) colorScheme.onSurfaceVariant else Color(0xFF6B7280)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BiteCalScreenFrame.contentHorizontalLarge)
            .padding(bottom = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()

        Spacer(Modifier.height(18.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(7.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = bodyColor,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.fillMaxWidth()
        )

        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
private fun NotificationStateIcon(unread: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminanceForUi() < 0.45f
    val background = notificationBellBackgroundColor(isDark)
    val iconColor = notificationBellIconColor(isDark)

    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(29.dp)
        )
        if (unread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFFFF8A8A) else Color(0xFFE5484D))
            )
        }
    }
}

@Composable
private fun NotificationDateText(
    text: String,
    color: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        letterSpacing = 0.sp
    )
}

@Composable
private fun rememberDebouncedClick(
    intervalMs: Long = 650L,
    onClick: () -> Unit
): () -> Unit {
    val currentOnClick by rememberUpdatedState(onClick)
    var lastClickAt by remember { mutableStateOf(0L) }
    return remember(intervalMs) {
        {
            val now = SystemClock.elapsedRealtime()
            if (now - lastClickAt >= intervalMs) {
                lastClickAt = now
                currentOnClick()
            }
        }
    }
}

private fun formatNotificationCreatedAt(
    raw: String,
    todayText: String,
    yesterdayText: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
    use24HourTime: Boolean = false
): String {
    val instant = UtcTimeFormatter.parseBackendUtcInstantOrNull(raw) ?: return raw.take(10)
    val localDateTime = instant.atZone(zoneId)
    val today = LocalDate.now(zoneId)
    val notificationDate = localDateTime.toLocalDate()

    val dateText = when (notificationDate) {
        today -> todayText
        today.minusDays(1) -> yesterdayText
        else -> formatNotificationDate(
            dateTime = localDateTime,
            today = today,
            locale = locale
        )
    }
    val timeText = formatNotificationTime(
        dateTime = localDateTime,
        locale = locale,
        use24HourTime = use24HourTime
    )

    return "$dateText · $timeText"
}

private fun formatNotificationDate(
    dateTime: ZonedDateTime,
    today: LocalDate,
    locale: Locale
): String {
    val skeleton = if (dateTime.year == today.year) {
        "MMMMd"
    } else {
        "yMMMMd"
    }
    val pattern = AndroidDateFormat.getBestDateTimePattern(locale, skeleton)
    return DateTimeFormatter
        .ofPattern(pattern, locale)
        .format(dateTime)
}

private fun formatNotificationTime(
    dateTime: ZonedDateTime,
    locale: Locale,
    use24HourTime: Boolean
): String {
    val skeleton = if (use24HourTime) {
        "Hm"
    } else {
        "hm"
    }
    val pattern = AndroidDateFormat.getBestDateTimePattern(locale, skeleton)
    return DateTimeFormatter
        .ofPattern(pattern, locale)
        .format(dateTime)
}

private fun localeFromTag(tag: String): Locale {
    val normalized = tag.trim().ifBlank { Locale.getDefault().toLanguageTag() }
    return Locale.forLanguageTag(normalized)
}

private fun Color.luminanceForUi(): Float =
    (0.299f * red) + (0.587f * green) + (0.114f * blue)

private fun notificationBellBackgroundColor(isDark: Boolean): Color =
    if (isDark) Color(0xFF3A2A12) else Color(0xFFFFF4DE)

private fun notificationBellIconColor(isDark: Boolean): Color =
    if (isDark) Color(0xFFFFC46B) else Color(0xFFD99017)

private val NotificationInboxMaxWidth = 520.dp
