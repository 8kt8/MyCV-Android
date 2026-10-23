package com.katlewski.cv.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.katlewski.cv.data.Education
import com.katlewski.cv.android.ui.icons.CvIcons
import com.katlewski.cv.android.ui.theme.spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsSection(skills: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xSmall),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xSmall),
    ) {
        skills.forEach { Tag(it) }
    }
}

@Composable
fun EducationCard(education: Education, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val uriHandler = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.largeIncreased,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = spacing.medium, top = spacing.medium, bottom = spacing.medium, end = spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Logo(url = education.resolvedLogoUrl, name = education.schoolName)
            Spacer(Modifier.width(spacing.small))
            Column(Modifier.weight(1f)) {
                Text(education.schoolName, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    "${education.degree} · ${education.fieldOfStudy}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    education.period.displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            education.url?.let { url ->
                IconButton(onClick = { uriHandler.openUri(url) }) {
                    Icon(
                        CvIcons.OpenInNew,
                        contentDescription = "Open ${education.schoolName} website",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun Footer(modifier: Modifier = Modifier) {
    Text(
        text = "Built with Jetpack Compose & Kotlin Multiplatform",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.xLarge),
    )
}
