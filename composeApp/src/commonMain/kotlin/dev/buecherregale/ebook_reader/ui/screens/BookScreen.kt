package dev.buecherregale.ebook_reader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buecherregale.ebook_reader.core.config.SettingsManager
import dev.buecherregale.ebook_reader.core.dom.Branch
import dev.buecherregale.ebook_reader.core.dom.Document
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.ui.dom.DomNode
import dev.buecherregale.ebook_reader.ui.dom.RenderingConfig
import dev.buecherregale.ebook_reader.ui.dom.cloneWithChildren
import dev.buecherregale.ebook_reader.ui.dom.paginate
import dev.buecherregale.ebook_reader.ui.navigation.Navigator
import dev.buecherregale.ebook_reader.ui.navigation.Screen
import dev.buecherregale.ebook_reader.ui.viewmodel.BookViewModel
import ebuecherregal.composeapp.generated.resources.Res
import ebuecherregal.composeapp.generated.resources.arrow_back_24px
import ebuecherregal.composeapp.generated.resources.arrow_forward_24px
import ebuecherregal.composeapp.generated.resources.settings_24px
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BookScreen(
    viewModel: BookViewModel = koinViewModel(),
    navigator: Navigator = koinInject(),
    settingsManager: SettingsManager = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }

    val config = remember(settingsManager.state.fontSize.value) {
        RenderingConfig.Default.copy(
            baseTextSize = settingsManager.state.fontSize.value.sp,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.book.metadata.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
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
                    IconButton(onClick = { if (currentPage > 0) currentPage-- }) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_back_24px),
                            contentDescription = "Previous page"
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val progress = if (totalPages > 0) (currentPage + 1).toFloat() / totalPages else 0f
                        Text(
                            text = "${(progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    IconButton(onClick = { if (currentPage < totalPages - 1) currentPage++ }) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_forward_24px),
                            contentDescription = "Next page"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading || state.dom == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                val pageWidthPx = constraints.maxWidth
                val pageHeightPx = constraints.maxHeight

                PaginatedContent(
                    book = state.book,
                    dom = state.dom!!,
                    config = config,
                    pageWidthPx = pageWidthPx,
                    pageHeightPx = pageHeightPx,
                    currentPage = currentPage,
                    onPageChanged = { currentPage = it },
                    onTotalPagesChanged = { totalPages = it }
                )
            }
        }
    }
}

@Composable
fun PaginatedContent(
    book: Book,
    dom: Document,
    config: RenderingConfig,
    pageWidthPx: Int,
    pageHeightPx: Int,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    onTotalPagesChanged: (Int) -> Unit,
) {
    val nodeHeights: SnapshotStateMap<String, Int> =
        remember(dom, pageWidthPx) { mutableStateMapOf() }
    val branchHeights: SnapshotStateMap<String, Int> =
        remember(dom, pageWidthPx) { mutableStateMapOf() }

    // measure
    SubcomposeLayout(modifier = Modifier.fillMaxSize()) { constraints ->
        val measureConstraints = Constraints(
            minWidth = 0,
            maxWidth = pageWidthPx,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )
        dom.traverse { node ->
            if (node.id !in nodeHeights) {
                nodeHeights[node.id] = subcompose("measure_${node.id}") {
                    DomNode(
                        book = book,
                        node = node,
                        config = config,
                    )
                }.firstOrNull()
                    ?.measure(measureConstraints)
                    ?.height ?: 0
                if (node is Branch) {
                    val clone = node.cloneWithChildren(mutableListOf())
                    branchHeights[node.id] = subcompose("measure_branch_${node.id}") {
                        DomNode(
                            book = book,
                            node = clone,
                            config = config,
                        )
                    }.firstOrNull()
                        ?.measure(measureConstraints)
                        ?.height ?: 0
                }
            }
        }
        // paginate
        val pages = dom.paginate(nodeHeights, branchHeights, pageHeightPx)
        // render
        val pagerPlaceable = subcompose("pager") {
            if (pages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LaunchedEffect(pages.size) {
                    onTotalPagesChanged(pages.size)
                }
                val pagerState = rememberPagerState(pageCount = { pages.size })

                LaunchedEffect(pagerState.currentPage) {
                    onPageChanged(pagerState.currentPage)
                }

                LaunchedEffect(currentPage) {
                    if (pagerState.currentPage != currentPage) {
                        pagerState.animateScrollToPage(currentPage)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    ) {
                        pages[pageIndex].roots.forEach { node ->
                            DomNode(book = book, node = node, config = config)
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