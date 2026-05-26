package com.example.llmwatcher.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.llmwatcher.model.PlatformUsage
import com.example.llmwatcher.model.UsageIssue
import com.example.llmwatcher.model.UsageMetric
import com.example.llmwatcher.model.WatcherUiState

@Composable
fun DashboardScreen(
    uiState: WatcherUiState,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DashboardHeader(
                uiState = uiState,
                onOpenSettings = onOpenSettings,
                onRefresh = onRefresh,
            )
            PlatformUsageGrid(usages = uiState.usageSnapshot.platforms)
        }
    }
}

@Composable
private fun DashboardHeader(
    uiState: WatcherUiState,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "LLM Watcher",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "横屏常亮看板 · ${uiState.usageSnapshot.generatedAtLabel}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(
            label = if (uiState.isDimmed) "屏幕已调暗" else "屏幕高亮",
            supportingText = "未变化 ${uiState.unchangedRefreshes} 次",
        )
        OutlinedButton(onClick = onRefresh) {
            Text("立即刷新")
        }
        Button(onClick = onOpenSettings) {
            Text("设置")
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    supportingText: String,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = supportingText, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PlatformUsageGrid(usages: List<PlatformUsage>) {
    if (usages.isEmpty()) {
        EmptyDashboard()
        return
    }

    usages.chunked(2).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            rowItems.forEach { usage ->
                PlatformUsageCard(
                    usage = usage,
                    modifier = Modifier.weight(1f),
                )
            }
            if (rowItems.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyDashboard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "没有启用的平台",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "打开设置并启用至少一个 LLM 平台后，看板会显示对应的用量进度。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlatformUsageCard(
    usage: PlatformUsage,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = usage.platform.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${usage.platform.description} · ${usage.updatedAtLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            usage.issue?.let { IssueBanner(issue = it) }

            usage.metrics.forEach { metric ->
                MetricProgress(metric = metric)
            }
        }
    }
}

@Composable
private fun IssueBanner(issue: UsageIssue) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = issue.type.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = issue.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MetricProgress(metric: UsageMetric) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Text(
                text = metric.percentLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { metric.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
        )
        Text(
            text = metric.amountLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
