package com.katlewski.cv.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.katlewski.cv.android.ui.components.EducationCard
import com.katlewski.cv.android.ui.components.ExperienceItem
import com.katlewski.cv.android.ui.components.Footer
import com.katlewski.cv.android.ui.components.ProfileHeader
import com.katlewski.cv.android.ui.components.SectionHeader
import com.katlewski.cv.android.ui.components.SkillsSection
import com.katlewski.cv.android.ui.components.icon
import com.katlewski.cv.android.ui.components.title
import com.katlewski.cv.android.ui.icons.CvIcons
import com.katlewski.cv.android.ui.theme.MaxContentWidth
import com.katlewski.cv.android.ui.theme.spacing
import com.katlewski.cv.data.ContactLink
import com.katlewski.cv.data.Cv

@Composable
fun CvScreen(viewModel: CvViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CvScreen(
        state = state,
        onRefresh = viewModel::onRefresh,
        onMessageShown = viewModel::onMessageShown,
    )
}

/** Bottom edges of the app bar and of the header's name, in root coordinates, updated on layout. */
@Stable
private class HeaderPositions {
    var barBottom by mutableFloatStateOf(0f)
    var nameBottom by mutableFloatStateOf(Float.POSITIVE_INFINITY)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CvScreen(state: CvUiState, onRefresh: () -> Unit, onMessageShown: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    val listState = rememberLazyListState()
    val positions = remember { HeaderPositions() }
    // The bar takes over the name (and the contact actions) as soon as the name scrolls under it.
    val showTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || positions.nameBottom < positions.barBottom }
    }

    // Edge-to-edge: the header gradient runs under the status bar and the (still empty, transparent) app bar.
    Scaffold(
        topBar = {
            CvTopBar(
                title = state.cv?.profile?.fullName,
                contacts = state.cv?.contacts.orEmpty(),
                showTitle = showTitle,
                modifier = Modifier.onGloballyPositioned {
                    positions.barBottom = it.positionInRoot().y + it.size.height
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val cv = state.cv
        if (cv == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding()),
                )
            },
        ) {
            CvContent(cv, listState, positions)
        }
    }
}

/**
 * Small app bar. Transparent and empty over the header; once the name has scrolled under it, it takes
 * the tonal container color and shows the name and a contact menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CvTopBar(
    title: String?,
    contacts: List<ContactLink>,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme
    val containerColor by animateColorAsState(
        targetValue = if (showTitle) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "topBarContainer",
    )
    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = title != null && showTitle,
                enter = fadeIn(motion.defaultEffectsSpec()) + slideInVertically(motion.defaultSpatialSpec()) { it / 2 },
                exit = fadeOut(motion.fastEffectsSpec()) + slideOutVertically(motion.fastSpatialSpec()) { it / 2 },
            ) {
                Text(title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            AnimatedVisibility(
                visible = showTitle && contacts.isNotEmpty(),
                enter = fadeIn(motion.defaultEffectsSpec()),
                exit = fadeOut(motion.fastEffectsSpec()),
            ) {
                ContactMenu(contacts)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
        ),
        modifier = modifier,
    )
}

/** All contact actions in one menu, for when the header's buttons are scrolled away. */
@Composable
private fun ContactMenu(contacts: List<ContactLink>) {
    val uriHandler = LocalUriHandler.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, shapes = IconButtonDefaults.shapes()) {
            Icon(CvIcons.Mail, contentDescription = "Contact")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            contacts.forEach { contact ->
                DropdownMenuItem(
                    text = { Text(contact.type.title()) },
                    leadingIcon = { Icon(contact.type.icon(), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    onClick = {
                        expanded = false
                        uriHandler.openUri(contact.url)
                    },
                )
            }
        }
    }
}

@Composable
private fun CvContent(cv: Cv, listState: LazyListState, positions: HeaderPositions) {
    val spacing = MaterialTheme.spacing
    val headerBrush = Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface),
    )

    LazyColumn(
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(headerBrush),
                contentAlignment = Alignment.TopCenter,
            ) {
                ProfileHeader(
                    profile = cv.profile,
                    contacts = cv.contacts,
                    // Starts under the empty app bar, so there's no bar-sized gap above the photo.
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .contentWidth()
                        .padding(horizontal = spacing.medium)
                        .padding(top = spacing.large),
                    nameModifier = Modifier.onGloballyPositioned {
                        positions.nameBottom = it.positionInRoot().y + it.size.height
                    },
                )
            }
        }
        cvSections(cv)
        item(key = "footer") { Footer(Modifier.contentWidth().padding(horizontal = spacing.medium)) }
    }
}

private fun LazyListScope.cvSections(cv: Cv) {
    if (cv.experience.isNotEmpty()) {
        item(key = "experience-header") { SectionHeader("Experience", CvIcons.Work, Modifier.contentPadding()) }
        itemsIndexed(cv.experience, key = { _, it -> "exp-${it.companyName}-${it.period.start}" }) { index, experience ->
            ExperienceItem(experience, isLast = index == cv.experience.lastIndex, modifier = Modifier.contentPadding())
        }
    }
    if (cv.skills.isNotEmpty()) {
        item(key = "skills-header") { SectionHeader("Skills", CvIcons.Code, Modifier.contentPadding()) }
        item(key = "skills") { SkillsSection(cv.skills, Modifier.contentPadding()) }
    }
    if (cv.education.isNotEmpty()) {
        item(key = "education-header") { SectionHeader("Education", CvIcons.School, Modifier.contentPadding()) }
        itemsIndexed(cv.education, key = { _, it -> "edu-${it.schoolName}-${it.degree}" }) { index, education ->
            EducationCard(
                education,
                Modifier
                    .contentPadding()
                    .padding(bottom = if (index == cv.education.lastIndex) 0.dp else MaterialTheme.spacing.small),
            )
        }
    }
}

/** Centers content at a readable width on tablets / landscape. */
private fun Modifier.contentWidth(): Modifier = widthIn(max = MaxContentWidth).fillMaxWidth()

@Composable
private fun Modifier.contentPadding(): Modifier = contentWidth().padding(horizontal = MaterialTheme.spacing.medium)
