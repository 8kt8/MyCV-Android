package com.katlewski.cv.android.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.katlewski.cv.android.ui.components.EducationCard
import com.katlewski.cv.android.ui.components.ExperienceItem
import com.katlewski.cv.android.ui.components.Footer
import com.katlewski.cv.android.ui.components.ProfileHeader
import com.katlewski.cv.android.ui.components.SectionHeader
import com.katlewski.cv.android.ui.components.SkillsSection
import com.katlewski.cv.android.ui.icons.CvIcons
import com.katlewski.cv.android.ui.theme.MaxContentWidth
import com.katlewski.cv.android.ui.theme.spacing
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
    // Pinned bar: transparent over the header, tonal container once content scrolls under it.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Edge-to-edge: the header gradient runs under the status and app bars.
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { CvTopBar(title = state.cv?.profile?.fullName, listState = listState, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val cv = state.cv
        if (cv == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        val topInset = padding.calculateTopPadding()
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
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = topInset),
                )
            },
        ) {
            CvContent(cv, listState, topInset)
        }
    }
}

/** Small app bar whose title (the name) appears once the profile header has scrolled away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CvTopBar(title: String?, listState: LazyListState, scrollBehavior: TopAppBarScrollBehavior) {
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = title != null && headerGone,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 2 },
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                    slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it / 2 },
            ) {
                Text(title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun CvContent(cv: Cv, listState: LazyListState, topInset: Dp) {
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
                    modifier = Modifier
                        .padding(top = topInset)
                        .contentWidth()
                        .padding(horizontal = spacing.medium)
                        .padding(top = spacing.xSmall),
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
