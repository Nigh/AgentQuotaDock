package com.example.llmwatcher.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.llmwatcher.model.PlatformUsage
import com.example.llmwatcher.model.UsageIssue
import com.example.llmwatcher.model.UsageLevel
import com.example.llmwatcher.model.UsageMetric
import com.example.llmwatcher.model.ThemePreference
import com.example.llmwatcher.model.WatcherUiState
import com.example.llmwatcher.ui.theme.XianiiError
import com.example.llmwatcher.ui.theme.XianiiSuccess
import com.example.llmwatcher.ui.theme.XianiiWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    uiState: WatcherUiState,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val oled = uiState.settings.themePreference == ThemePreference.OLED_BLACK
    var driftX by remember { mutableStateOf(0) }
    var driftY by remember { mutableStateOf(0) }
    val now = rememberCurrentTime()
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Surface(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val driftMargin = if (oled) minOf(24, (maxHeight.value / 10).toInt(), (maxWidth.value / 10).toInt()) else 0
            LaunchedEffect(oled, driftMargin) {
                if (oled && driftMargin > 0) {
                    while (true) {
                        val half = (driftMargin + 1) / 2
                        driftX = if (driftX > 0) Random.nextInt(-driftMargin, -half + 1) else Random.nextInt(half, driftMargin + 1)
                        driftY = if (driftY > 0) Random.nextInt(-driftMargin, -half + 1) else Random.nextInt(half, driftMargin + 1)
                        delay(60_000L)
                    }
                } else {
                    driftX = 0
                    driftY = 0
                }
            }
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(driftMargin.dp)
                    .offset(driftX.coerceIn(-driftMargin, driftMargin).dp, driftY.coerceIn(-driftMargin, driftMargin).dp),
            ) {
            val enabled = uiState.settings.platforms
                .filter { it.enabled && it.credential.isNotBlank() }
                .map { it.platform }.toSet()
            val usages = uiState.usageSnapshot.platforms.filter { it.platform in enabled }
            if (usages.isEmpty()) {
                EmptyDashboard(onOpenSettings = onOpenSettings, oled = oled, modifier = Modifier.fillMaxSize())
            } else {
                val pagerState = rememberPagerState(pageCount = { usages.size })
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        key = { usages[it].platform },
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        PlatformUsagePage(
                            usage = usages[page],
                            currentTime = formatter.format(Date(now)),
                            now = now,
                            oled = oled,
                            showRemaining = uiState.settings.showRemaining,
                            onRefresh = onRefresh,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.fillMaxSize().padding(bottom = if (usages.size > 1) 28.dp else 0.dp),
                        )
                    }
                    PageIndicator(
                        pageCount = usages.size,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 18.dp),
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun EmptyDashboard(
    onOpenSettings: () -> Unit,
    oled: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxHeight < 320.dp
        Box(
            modifier = Modifier.fillMaxSize().padding(if (compact) 8.dp else 32.dp),
            contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(if (compact) 0.9f else 0.72f).then(
                if (oled) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CardDefaults.elevatedShape) else Modifier,
            ),
            colors = if (oled) CardDefaults.elevatedCardColors(containerColor = Color.Black) else CardDefaults.elevatedCardColors(),
            elevation = if (oled) CardDefaults.elevatedCardElevation(defaultElevation = 0.dp) else CardDefaults.elevatedCardElevation(),
        ) {
            Column(
                modifier = Modifier.padding(if (compact) 12.dp else 32.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "暂无显示的平台",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "在设置中登录或填写凭据，并选择要在看板显示的平台。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (oled) {
                    OutlinedButton(onClick = onOpenSettings) { Text("打开设置") }
                } else {
                    Button(onClick = onOpenSettings) { Text("打开设置") }
                }
            }
        }
    }
    }
}

@Composable
private fun PlatformUsagePage(
    usage: PlatformUsage,
    currentTime: String,
    now: Long,
    oled: Boolean,
    showRemaining: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxHeight < 320.dp
        Box(
            modifier = Modifier.fillMaxSize().padding(
                horizontal = if (compact) 12.dp else 44.dp,
                vertical = if (compact) 8.dp else 30.dp,
            ),
            contentAlignment = Alignment.Center,
        ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(8.dp)
                .then(if (oled) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CardDefaults.elevatedShape) else Modifier),
            colors = if (oled) CardDefaults.elevatedCardColors(containerColor = Color.Black) else CardDefaults.elevatedCardColors(),
            elevation = if (oled) CardDefaults.elevatedCardElevation(defaultElevation = 0.dp) else CardDefaults.elevatedCardElevation(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (compact) 16.dp else 40.dp, vertical = if (compact) 8.dp else 28.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = usage.platform.displayName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CardActions(
                            currentTime = currentTime,
                            onRefresh = onRefresh,
                            onOpenSettings = onOpenSettings,
                        )
                    }

                    usage.metrics.forEach { metric ->
                        key(usage.platform, metric.label) {
                            MetricProgress(metric = metric, now = now, oled = oled, showRemaining = showRemaining)
                        }
                    }
            }
                usage.issue?.let { issue ->
                    IssueBubble(
                        issue = issue,
                        oled = oled,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
                    )
                }
        }
        }
    }
}
}

@Composable
private fun CardActions(
    currentTime: String,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTextButton(
            text = "↻",
            contentDescription = "刷新",
            onClick = onRefresh,
        )
        IconTextButton(
            text = "⚙",
            contentDescription = "设置",
            onClick = onOpenSettings,
        )
        Text(
            text = currentTime,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IconTextButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            this.contentDescription = contentDescription
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IssueBubble(
    issue: UsageIssue,
    oled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(0.62f),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (oled) 0.dp else 8.dp,
        border = if (oled) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        color = if (oled) Color.Black else MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = issue.type.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = issue.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MetricProgress(metric: UsageMetric, now: Long, oled: Boolean, showRemaining: Boolean) {
    val progressColor = progressColorFor(metric.usageLevel)
    val highlight = remember { Animatable(0f) }
    var previousUsed by remember { mutableStateOf(metric.used) }
    LaunchedEffect(metric.used) {
        if (metric.used != previousUsed) {
            previousUsed = metric.used
            highlight.snapTo(1f)
            highlight.animateTo(0f, tween(900))
        }
    }
    val remaining = showRemaining && metric.limit > 0.0
    val displayedMetric = displayedUsage(metric, showRemaining)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            key(remaining) {
                AnimatedContent(
                    targetState = metric.used,
                    transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(350)) },
                    label = "用量变化",
                ) { used ->
                    val amount = displayedUsage(metric.copy(used = used), showRemaining).amountLabel
                    Text(
                        text = (if (remaining) "剩余 " else "") + amount,
                        modifier = Modifier.graphicsLayer {
                            scaleX = 1f + 0.18f * highlight.value
                            scaleY = 1f + 0.18f * highlight.value
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (oled) progressColor.copy(alpha = 0.8f + 0.2f * highlight.value) else progressColor,
                    )
                }
            }
        }
        metric.resetsAtEpochMillis?.let { reset ->
            Text(
                text = resetTimeLabel(reset, now),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (metric.limit > 0.0) {
            key(remaining) {
                val progress by animateFloatAsState(
                    targetValue = displayedMetric.progressFraction,
                    animationSpec = tween(900),
                    label = "进度变化",
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = androidx.compose.ui.graphics.lerp(progressColor, Color.White, highlight.value * 0.55f),
                    trackColor = progressColor.copy(alpha = 0.18f + highlight.value * 0.25f),
                )
            }
        }
    }
}

internal fun displayedUsage(metric: UsageMetric, showRemaining: Boolean): UsageMetric =
    if (showRemaining && metric.limit > 0.0) {
        metric.copy(used = (metric.limit - metric.used).coerceAtLeast(0.0))
    } else {
        metric
    }

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 10.dp else 7.dp)
                    .aspectRatio(1f)
                    .background(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun progressColorFor(level: UsageLevel): Color {
    return when (level) {
        UsageLevel.HEALTHY -> XianiiSuccess   // #7FC08C
        UsageLevel.WARNING -> XianiiWarning   // #EEBC4A
        UsageLevel.CRITICAL -> XianiiError    // #FA6863
    }
}

@Composable
private fun rememberCurrentTime(): Long {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    return currentTime
}
