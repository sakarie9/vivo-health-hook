package com.sakari.vivohealthxposed.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakari.vivohealthxposed.R
import com.sakari.vivohealthxposed.data.AppItem
import com.sakari.vivohealthxposed.data.buildAppItems
import com.sakari.vivohealthxposed.data.isValidPackageName
import com.sakari.vivohealthxposed.ui.theme.StatusConnected
import com.sakari.vivohealthxposed.ui.theme.StatusDisconnected
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** 整个模块 App 的 Compose 界面：在主列表与「关于」页之间切换。 */
@Composable
fun WhitelistApp(viewModel: WhitelistViewModel = viewModel()) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    // 根节点铺满整个窗口并填充主题背景，保证系统栏区域也是应用背景色而不是黑色。
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (showAbout) {
            BackHandler { showAbout = false }
            AboutScreen(viewModel = viewModel, onBack = { showAbout = false })
        } else {
            WhitelistListScreen(viewModel = viewModel, onOpenAbout = { showAbout = true })
        }
    }
}

/** 白名单主列表。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhitelistListScreen(
    viewModel: WhitelistViewModel,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var showManualDialog by rememberSaveable { mutableStateOf(false) }

    fun showMessage(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    LaunchedEffect(viewModel.remoteError) {
        val error = viewModel.remoteError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(R.string.remote_failed, error))
        viewModel.consumeRemoteError()
    }

    val items = remember(viewModel.apps, viewModel.builtInPackages, viewModel.selected) {
        buildAppItems(viewModel.apps, viewModel.builtInPackages, viewModel.selected)
    }
    val allCount = items.count { it.installed }
    val selectedCount = items.count { it.selected }
    val builtInCount = items.count { it.builtIn }

    val listState = rememberLazyListState()
    val scrollingDown = rememberIsScrollingDown(listState)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    // 标题较长且右侧有刷新/关于/更多三个操作，压缩到 titleMedium 避免被截断。
                    Text(
                        text = stringResource(R.string.screen_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = viewModel::refreshApps) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                        )
                    }
                    // 关于 / 使用说明：直接放在右上角，而不是收进二级菜单。
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.about_title),
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.more),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_manual)) },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showManualDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_clear_all)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                enabled = selectedCount > 0,
                                onClick = {
                                    menuExpanded = false
                                    viewModel.clearAll()
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 只避让顶部 AppBar；底部不内缩，列表内容才能画到导航栏（手势条）下面。
                .padding(top = padding.calculateTopPadding()),
        ) {
            StatusBanner(
                status = viewModel.serviceStatus,
                selectedCount = selectedCount,
                builtInCount = builtInCount,
            )
            // 向下滚动列表时收起搜索栏，向上滚动时再展开。
            AnimatedVisibility(visible = !scrollingDown) {
                SearchField(query = viewModel.query, onQueryChange = { viewModel.query = it })
            }
            FilterRow(
                filter = viewModel.filter,
                onFilterChange = { viewModel.filter = it },
                totalCount = allCount,
                selectedCount = selectedCount,
                builtInCount = builtInCount,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            AppList(
                modifier = Modifier.weight(1f),
                allItems = items,
                filter = viewModel.filter,
                query = viewModel.query.trim(),
                loading = viewModel.loading,
                state = listState,
                // 让最后一项能滚到导航栏上方，同时列表本身铺满到屏幕底部。
                bottomContentPadding = padding.calculateBottomPadding() + 24.dp,
                onToggle = viewModel::toggle,
                onBuiltInInfo = { showMessage(context.getString(R.string.builtin_info, it.label)) },
            )
        }
    }

    if (showManualDialog) {
        ManualPackageDialog(
            onDismiss = { showManualDialog = false },
            onSubmit = { packageName ->
                val result = viewModel.addManual(packageName)
                if (result == AddResult.ADDED) {
                    showMessage(context.getString(R.string.manual_added, packageName))
                }
                result
            },
        )
    }
}

@Composable
private fun StatusBanner(status: ServiceStatus, selectedCount: Int, builtInCount: Int) {
    val connected = status.connected
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (connected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (connected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(10.dp)
                    .background(
                        color = if (connected) StatusConnected else StatusDisconnected,
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (connected) {
                        stringResource(
                            R.string.status_connected,
                            status.frameworkName,
                            status.frameworkVersion,
                            status.apiVersion,
                        )
                    } else {
                        stringResource(R.string.status_disconnected_title)
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (connected) {
                        stringResource(R.string.status_connected_detail, selectedCount, builtInCount)
                    } else {
                        stringResource(R.string.status_disconnected_detail)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_clear),
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun FilterRow(
    filter: AppFilter,
    onFilterChange: (AppFilter) -> Unit,
    totalCount: Int,
    selectedCount: Int,
    builtInCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = filter == AppFilter.ALL,
            onClick = { onFilterChange(AppFilter.ALL) },
            label = { Text(stringResource(R.string.filter_all, totalCount)) },
        )
        FilterChip(
            selected = filter == AppFilter.SELECTED,
            onClick = { onFilterChange(AppFilter.SELECTED) },
            label = { Text(stringResource(R.string.filter_selected, selectedCount)) },
            leadingIcon = if (filter == AppFilter.SELECTED) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
        )
        FilterChip(
            selected = filter == AppFilter.BUILT_IN,
            onClick = { onFilterChange(AppFilter.BUILT_IN) },
            label = { Text(stringResource(R.string.filter_builtin, builtInCount)) },
        )
    }
}

/**
 * 监听列表滚动方向：向下滚动（内容上移，firstVisibleItem 变大）时返回 true。
 * 用 snapshotFlow 读取滚动状态，避免在 derivedStateOf 里写状态。
 */
@Composable
private fun rememberIsScrollingDown(state: LazyListState): Boolean {
    var scrollingDown by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        var previousIndex = state.firstVisibleItemIndex
        var previousOffset = state.firstVisibleItemScrollOffset
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index != previousIndex || offset != previousOffset) {
                    scrollingDown = index > previousIndex ||
                        (index == previousIndex && offset > previousOffset)
                    previousIndex = index
                    previousOffset = offset
                }
            }
    }
    return scrollingDown
}

@Composable
private fun AppList(
    modifier: Modifier,
    allItems: List<AppItem>,
    filter: AppFilter,
    query: String,
    loading: Boolean,
    state: LazyListState,
    bottomContentPadding: Dp,
    onToggle: (String) -> Unit,
    onBuiltInInfo: (AppItem) -> Unit,
) {
    val filtered = remember(allItems, filter, query) {
        allItems.filter { item ->
            val matchesFilter = when (filter) {
                // 「全部」只列已安装应用；未安装的内置包名只在「内置」里出现。
                AppFilter.ALL -> item.installed
                AppFilter.SELECTED -> item.selected
                AppFilter.BUILT_IN -> item.builtIn
            }
            matchesFilter && (
                query.isEmpty() ||
                    item.label.contains(query, ignoreCase = true) ||
                    item.packageName.contains(query, ignoreCase = true)
                )
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (loading && allItems.none { it.installed }) {
            LoadingState(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomContentPadding),
        ) {
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = when {
                            query.isNotEmpty() -> stringResource(R.string.empty_search, query)
                            filter == AppFilter.SELECTED -> stringResource(R.string.empty_selected)
                            filter == AppFilter.BUILT_IN -> stringResource(R.string.empty_builtin)
                            else -> stringResource(R.string.empty_no_apps)
                        },
                        hint = if (filter == AppFilter.SELECTED && query.isEmpty()) {
                            stringResource(R.string.empty_selected_hint)
                        } else {
                            null
                        },
                    )
                }
            } else {
                items(filtered, key = { it.packageName }) { item ->
                    AppRow(
                        item = item,
                        onToggle = { onToggle(item.packageName) },
                        onBuiltInInfo = { onBuiltInInfo(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(item: AppItem, onToggle: () -> Unit, onBuiltInInfo: () -> Unit) {
    val background = when {
        item.selected -> MaterialTheme.colorScheme.secondaryContainer
        item.builtIn -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .clickable { if (item.builtIn) onBuiltInInfo() else onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(item = item)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.label,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    item.builtIn -> {
                        Spacer(Modifier.width(6.dp))
                        StatusBadge(
                            text = stringResource(R.string.badge_builtin),
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }

                    !item.installed -> {
                        Spacer(Modifier.width(6.dp))
                        StatusBadge(
                            text = stringResource(R.string.badge_uninstalled),
                            container = MaterialTheme.colorScheme.surfaceVariant,
                            content = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(1.dp))
            Text(
                text = item.packageName,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.builtIn) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.badge_builtin),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Checkbox(checked = item.selected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun AppIcon(item: AppItem) {
    val bitmap = remember(item.packageName, item.icon) {
        item.icon?.let { drawable ->
            runCatching { drawable.toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(12.dp)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, container: Color, content: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyState(title: String, hint: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (hint != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.loading_apps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ManualPackageDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> AddResult,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    fun submit() {
        val value = text.trim()
        if (!isValidPackageName(value)) {
            errorRes = R.string.manual_invalid
            return
        }
        when (onSubmit(value)) {
            AddResult.ADDED -> onDismiss()
            AddResult.DUPLICATE -> errorRes = R.string.manual_duplicate
            AddResult.BUILT_IN -> errorRes = R.string.manual_builtin
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        errorRes = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.input_label)) },
                    placeholder = { Text(stringResource(R.string.manual_dialog_hint)) },
                    singleLine = true,
                    isError = errorRes != null,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                )
                errorRes?.let { res ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) {
                Text(stringResource(R.string.manual_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.manual_dialog_cancel))
            }
        },
    )
}
