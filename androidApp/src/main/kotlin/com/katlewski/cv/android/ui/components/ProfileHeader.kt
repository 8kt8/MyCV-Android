package com.katlewski.cv.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.katlewski.cv.android.ui.icons.CvIcons
import com.katlewski.cv.android.ui.theme.spacing
import com.katlewski.cv.data.ContactLink
import com.katlewski.cv.data.ContactType
import com.katlewski.cv.data.Profile

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileHeader(profile: Profile, contacts: List<ContactLink>, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo(
            url = profile.photoUrl,
            name = profile.fullName,
            size = 120.dp,
            // Expressive shape library: a scalloped "cookie" frame for the photo.
            shape = MaterialShapes.Cookie12Sided.toShape(),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(spacing.medium))
        Text(
            text = profile.fullName,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(spacing.xxSmall))
        Text(
            text = profile.headline,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        profile.location?.let { location ->
            Spacer(Modifier.height(spacing.xSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    CvIcons.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(spacing.xxSmall))
                Text(
                    location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (contacts.isNotEmpty()) {
            Spacer(Modifier.height(spacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.large)) {
                contacts.forEach { ContactAction(it) }
            }
        }
        Spacer(Modifier.height(spacing.large))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.largeIncreased,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = profile.about,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(spacing.medium),
            )
        }
    }
}

/**
 * Icon button + caption, like the actions on a contact card. Medium (56dp) expressive icon button
 * that morphs from round to square while pressed; full label for screen readers.
 */
@Composable
private fun ContactAction(contact: ContactLink) {
    val uriHandler = LocalUriHandler.current
    val title = contact.type.title()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 64.dp)) {
        FilledTonalIconButton(
            onClick = { uriHandler.openUri(contact.url) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
        ) {
            Icon(
                contact.type.icon(),
                contentDescription = "$title: ${contact.label}",
                modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
            )
        }
        Spacer(Modifier.height(MaterialTheme.spacing.xSmall))
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Short action name, shared by the header buttons and the app bar contact menu. */
internal fun ContactType.title(): String = when (this) {
    ContactType.GitHub -> "GitHub"
    ContactType.LinkedIn -> "LinkedIn"
    ContactType.Email -> "Email"
    ContactType.Phone -> "Call"
    ContactType.Website -> "Website"
}

internal fun ContactType.icon(): ImageVector = when (this) {
    ContactType.GitHub -> CvIcons.GitHub
    ContactType.LinkedIn -> CvIcons.LinkedIn
    ContactType.Email -> CvIcons.Mail
    ContactType.Phone -> CvIcons.Call
    ContactType.Website -> CvIcons.Language
}
