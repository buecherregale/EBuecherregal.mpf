package dev.buecherregale.ebook_reader.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.config.SettingsManager
import dev.buecherregale.ebook_reader.core.dom.*
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.ui.components.DictionaryPopup
import dev.buecherregale.ebook_reader.ui.components.rememberPopupState
import dev.buecherregale.ebook_reader.ui.dom.*
import dev.buecherregale.ebook_reader.ui.navigation.Navigator
import dev.buecherregale.ebook_reader.ui.navigation.Screen
import dev.buecherregale.ebook_reader.ui.viewmodel.BookViewModel
import ebuecherregal.composeapp.generated.resources.Res
import ebuecherregal.composeapp.generated.resources.arrow_back_24px
import ebuecherregal.composeapp.generated.resources.arrow_forward_24px
import ebuecherregal.composeapp.generated.resources.settings_24px
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private const val WINDOW_BEHIND = 2  // top-level children to load behind the anchor
private const val WINDOW_AHEAD = 4  // top-level children to load ahead of the anchor
private const val EXPAND_STEP = 2  // children added per expansion event
private const val PAGE_EDGE_THRESHOLD = 3  // pages from edge that triggers expansion

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun BookScreen(
    viewModel: BookViewModel = koinViewModel(),
    navigator: Navigator = koinInject(),
    settingsManager: SettingsManager = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var totalPages by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { totalPages })

    val config = remember(settingsManager.state.fontSize.value) {
        RenderingConfig.Default.copy(baseTextSize = settingsManager.state.fontSize.value.sp)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.book.metadata.title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.push(Screen.Settings) }) {
                        Icon(painterResource(Res.drawable.settings_24px), contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(64.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (pagerState.currentPage > 0) scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = "Previous page")
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${(state.progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LinearProgressIndicator(
                            progress = { state.progress.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    IconButton(onClick = {
                        if (pagerState.currentPage < totalPages - 1) scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }) {
                        Icon(painterResource(Res.drawable.arrow_forward_24px), contentDescription = "Next page")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading || state.dom == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                val dictionaryPopupState = rememberPopupState()

                PaginatedContent(
                    book = state.book,
                    dom = state.dom!!,
                    config = config,
                    pageWidthPx = constraints.maxWidth,
                    pageHeightPx = constraints.maxHeight,
                    pagerState = pagerState,
                    initialProgress = state.book.progress,
                    onProgressChanged = { viewModel.updateProgress(it) },
                    onTotalPagesChanged = { totalPages = it },
                    onTextSelected = { selection: SelectedText, callback: HighlightDismisser ->
                        Logger.i { "selected word: ${selection.word}" }
                        dictionaryPopupState.show(selection, callback)
                    },
                )
                state.dictionary?.let {
                    DictionaryPopup(
                        state = dictionaryPopupState,
                        dictionary = it,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PaginatedContent(
    book: Book,
    dom: Document,
    config: RenderingConfig,
    pageWidthPx: Int,
    pageHeightPx: Int,
    pagerState: PagerState,
    initialProgress: Double,
    onProgressChanged: (Double) -> Unit = {},
    onTotalPagesChanged: (Int) -> Unit = {},
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    val contentIndex = remember(dom) { dom.buildContentIndex() }
    val scope = rememberCoroutineScope()

    var viewAnchorLeafId by remember(dom) {
        mutableStateOf(contentIndex.pathAtFraction(initialProgress).peek())
    }

    var windowStart by remember(dom, pageWidthPx) {
        mutableIntStateOf(maxOf(0, dom.childIndexContaining(viewAnchorLeafId) - WINDOW_BEHIND))
    }
    var windowEnd by remember(dom, pageWidthPx) {
        mutableIntStateOf(minOf(dom.children.lastIndex, dom.childIndexContaining(viewAnchorLeafId) + WINDOW_AHEAD))
    }

    val nodeHeights = remember(dom, pageWidthPx, config.baseTextSize) { mutableStateMapOf<String, Int>() }
    val branchHeights = remember(dom, pageWidthPx, config.baseTextSize) { mutableStateMapOf<String, Int>() }

    var needsRestore by remember(dom, pageWidthPx, config.baseTextSize) { mutableStateOf(true) }
    var pagesReady by remember(dom, pageWidthPx, config.baseTextSize) { mutableStateOf(false) }

    fun navigateToDomPath(path: DomPath) {
        val leafId = contentIndex.firstLeafIdUnder(path) ?: return // link does not start with document but with chapter
        val targetChildIndex = dom.childIndexContaining(leafId)

        windowStart = maxOf(0, targetChildIndex - WINDOW_BEHIND)
        windowEnd = minOf(dom.children.lastIndex, targetChildIndex + WINDOW_AHEAD)

        viewAnchorLeafId = leafId
        needsRestore = true
        pagesReady = false
    }

    SubcomposeLayout(modifier = Modifier.fillMaxSize()) { constraints ->
        val measureConstraints = Constraints(
            minWidth = 0, maxWidth = pageWidthPx,
            minHeight = 0, maxHeight = Constraints.Infinity,
        )

        for (i in windowStart..windowEnd) {
            dom.children[i].traverse { node ->
                if (node.id !in nodeHeights) {
                    nodeHeights[node.id] = subcompose("measure_${node.id}") {
                        DomNode(book = book, node = node, config = config)
                    }.firstOrNull()?.measure(measureConstraints)?.height ?: 0

                    if (node is Branch) {
                        branchHeights[node.id] = subcompose("measure_branch_${node.id}") {
                            DomNode(
                                book = book,
                                node = node.cloneWithChildren(mutableListOf()),
                                config = config,
                            )
                        }.firstOrNull()?.measure(measureConstraints)?.height ?: 0
                    }
                }
            }
        }

        val currentPages = (dom
            .cloneWithChildren(dom.children.subList(windowStart, windowEnd + 1).toMutableList()) as Document)
            .paginate(nodeHeights, branchHeights, pageHeightPx)

        val pagerPlaceable = subcompose("pager") {
            val latestPages by rememberUpdatedState(currentPages)
            val latestAnchor by rememberUpdatedState(viewAnchorLeafId)

            if (currentPages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SideEffect {
                    if (!pagesReady) pagesReady = true
                }

                LaunchedEffect(currentPages.size) {
                    onTotalPagesChanged(currentPages.size)
                }

                LaunchedEffect(needsRestore, pagesReady) {
                    if (needsRestore && pagesReady) {
                        val target = latestPages.pageIndexForLeafId(latestAnchor).takeIf { it >= 0 } ?: 0
                        pagerState.scrollToPage(target)
                        needsRestore = false
                    }
                }

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }.collect { settledPage ->
                        latestPages.getOrNull(settledPage)?.firstLeafId()?.let { leafId ->
                            viewAnchorLeafId = leafId
                            contentIndex.fractionForLeafId(leafId)?.let { onProgressChanged(it) }
                        }

                        if (settledPage >= latestPages.size - PAGE_EDGE_THRESHOLD
                            && windowEnd < dom.children.lastIndex
                        ) {
                            windowEnd = minOf(dom.children.lastIndex, windowEnd + EXPAND_STEP)
                        }

                        if (settledPage <= PAGE_EDGE_THRESHOLD && windowStart > 0) {
                            windowStart = maxOf(0, windowStart - EXPAND_STEP)
                            needsRestore = true
                        }
                    }
                }

                ProvideDomUriHandler(
                    onDomLinkClicked = { path ->
                        scope.launch { navigateToDomPath(path) }
                    }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { pageIndex ->
                        Column(modifier = Modifier.fillMaxSize().clipToBounds()) {
                            currentPages[pageIndex].roots.forEach { node ->
                                DomNode(
                                    book = book,
                                    node = node,
                                    config = config,
                                    onTextSelected = onTextSelected,
                                )
                            }
                        }
                    }
                }
            }
        }.first().measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            pagerPlaceable.placeRelative(0, 0)
        }
    }
}

@Composable
private fun ProvideDomUriHandler(
    onDomLinkClicked: (DomPath) -> Unit,
    content: @Composable () -> Unit,
) {
    val fallback = LocalUriHandler.current
    val handler = remember(fallback, onDomLinkClicked) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val parsed = DomUrl.parse(uri)
                if (parsed is DomUrl.Link) {
                    onDomLinkClicked(parsed.path)
                } else {
                    fallback.openUri(uri)
                }
            }
        }
    }
    CompositionLocalProvider(LocalUriHandler provides handler, content = content)
}

private fun Node.containsLeafId(leafId: String): Boolean = when (this) {
    is Leaf -> id == leafId
    is Branch -> children.any { it.containsLeafId(leafId) }
}

private fun Document.childIndexContaining(leafId: String): Int =
    children.indexOfFirst { it.containsLeafId(leafId) }.coerceAtLeast(0)