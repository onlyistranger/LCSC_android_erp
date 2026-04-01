package com.example.lcsc_android_erp.feature.inventory

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.lcsc_android_erp.LcscApplication
import com.example.lcsc_android_erp.R
import com.example.lcsc_android_erp.core.ui.performCopyFeedback
import com.example.lcsc_android_erp.domain.model.ComponentDetail
import com.example.lcsc_android_erp.domain.model.LocationInventoryItem
import com.example.lcsc_android_erp.domain.model.StockLocationCell
import com.example.lcsc_android_erp.domain.model.StorageLocation
import com.example.lcsc_android_erp.domain.model.StorageLocationSortMode
import com.example.lcsc_android_erp.feature.inbound.ExistingStockReminderCard
import com.example.lcsc_android_erp.feature.inbound.MaterialInboundDialog
import com.example.lcsc_android_erp.feature.inbound.ScannerCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun InventoryRoute(
    resetToOverviewSignal: Int = 0,
    modifier: Modifier = Modifier
) {
    val appContainer = (LocalContext.current.applicationContext as LcscApplication).appContainer
    val viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModel.factory(appContainer)
    )
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(resetToOverviewSignal) {
        viewModel.dismissLocationDetail()
    }

    InventoryScreen(
        modifier = modifier,
        uiState = uiState.value,
        onLocationSelected = viewModel::onLocationSelected,
        onDismissLocationDetail = viewModel::dismissLocationDetail,
        onOpenLocationSettings = viewModel::openLocationSettings,
        onCloseLocationSettings = viewModel::closeLocationSettings,
        onAddLocation = viewModel::addStorageLocation,
        onClearAddLocationError = viewModel::clearAddLocationError,
        onUpdateLocation = viewModel::updateLocation,
        onClearUpdateLocationError = viewModel::clearUpdateLocationError,
        onDeleteLocation = viewModel::deleteLocation,
        onForceDeleteLocation = viewModel::forceDeleteLocation,
        onLookupScannedComponent = viewModel::lookupScannedComponent,
        onAddScannedInbound = viewModel::addScannedInbound,
        onUpdateInventoryItemQuantity = viewModel::updateInventoryItemQuantity,
        onTransferInventoryItem = viewModel::transferInventoryItem,
        onDeleteInventoryItem = viewModel::deleteInventoryItem,
        onTransferInventoryItems = viewModel::transferInventoryItems,
        onDeleteInventoryItems = viewModel::deleteInventoryItems
    )
}

@Composable
fun InventoryScreen(
    uiState: InventoryUiState,
    onLocationSelected: (StockLocationCell) -> Unit,
    onDismissLocationDetail: () -> Unit,
    onOpenLocationSettings: (Long) -> Unit,
    onCloseLocationSettings: () -> Unit,
    onAddLocation: (String, String?, String?) -> Unit,
    onClearAddLocationError: () -> Unit,
    onUpdateLocation: (Long, String, String?, String?, String, (String?) -> Unit) -> Unit,
    onClearUpdateLocationError: () -> Unit,
    onDeleteLocation: (Long, (String?) -> Unit) -> Unit,
    onForceDeleteLocation: (Long, (String?) -> Unit) -> Unit,
    onLookupScannedComponent: (String, (InventoryScanLookupResult) -> Unit) -> Unit,
    onAddScannedInbound: (ComponentDetail, Int, String, String?, () -> Unit) -> Unit,
    onUpdateInventoryItemQuantity: (Long, Int, (String?) -> Unit) -> Unit,
    onTransferInventoryItem: (Long, String, (String?) -> Unit) -> Unit,
    onDeleteInventoryItem: (Long, (String?) -> Unit) -> Unit,
    onTransferInventoryItems: (List<Long>, String, (String?) -> Unit) -> Unit,
    onDeleteInventoryItems: (List<Long>, (String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var addLocationDialogVisible by remember { mutableStateOf(false) }
    var addLocationSubmitted by remember { mutableStateOf(false) }
    var settingsTargetCell by remember { mutableStateOf<StockLocationCell?>(null) }
    val context = LocalContext.current
    val rows = groupLocationRows(uiState.cells)

    LaunchedEffect(uiState.cells.size, uiState.addLocationError, addLocationSubmitted) {
        if (addLocationSubmitted && uiState.addLocationError == null) {
            addLocationDialogVisible = false
            addLocationSubmitted = false
        } else if (uiState.addLocationError != null) {
            addLocationSubmitted = false
        }
    }

    LaunchedEffect(uiState.addLocationError) {
        uiState.addLocationError?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (uiState.selectedLocation == null) {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.inventory_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(rows, key = { it.first }) { (letter, cells) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            cells.forEach { cell ->
                                StockLocationCard(
                                    cell = cell,
                                    onClick = { onLocationSelected(cell) },
                                    onLongClick = {
                                        settingsTargetCell = cell
                                        onOpenLocationSettings(cell.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { addLocationDialogVisible = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.inventory_add_location)
                )
            }
        }
    } else {
        InventoryLocationDetailScreen(
            uiState = uiState,
            cell = uiState.selectedLocation,
            items = uiState.selectedLocationItems,
            onBack = onDismissLocationDetail,
            onSave = onUpdateLocation,
            onClearUpdateLocationError = onClearUpdateLocationError,
            onDeleteLocation = onDeleteLocation,
            onForceDeleteLocation = onForceDeleteLocation,
            onLookupScannedComponent = onLookupScannedComponent,
            onAddScannedInbound = onAddScannedInbound,
            onUpdateInventoryItemQuantity = onUpdateInventoryItemQuantity,
            onTransferInventoryItem = onTransferInventoryItem,
            onDeleteInventoryItem = onDeleteInventoryItem,
            onTransferInventoryItems = onTransferInventoryItems,
            onDeleteInventoryItems = onDeleteInventoryItems,
            modifier = modifier
        )
    }

    if (addLocationDialogVisible) {
        AddLocationDialog(
            errorMessage = uiState.addLocationError,
            onDismiss = {
                onClearAddLocationError()
                addLocationDialogVisible = false
                addLocationSubmitted = false
            },
            onConfirm = { code, displayName, colorHex ->
                addLocationSubmitted = true
                onAddLocation(code, displayName, colorHex)
            }
        )
    }

    settingsTargetCell?.let { cell ->
        LocationSettingsDialog(
            cell = cell,
            errorMessage = uiState.updateLocationError,
            availableSecondaryAttributes = uiState.settingsLocationSortAttributes,
            onDismiss = {
                onClearUpdateLocationError()
                onCloseLocationSettings()
                settingsTargetCell = null
            },
            onSave = { code, displayName, colorHex, sortMode ->
                onUpdateLocation(cell.id, code, displayName, colorHex, sortMode) { error ->
                    if (error == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.inventory_location_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        settingsTargetCell = null
                        onCloseLocationSettings()
                        onClearUpdateLocationError()
                    }
                }
            },
            onDelete = {
                onDeleteLocation(cell.id) { error ->
                    if (error == null) {
                        settingsTargetCell = null
                        onCloseLocationSettings()
                        onClearUpdateLocationError()
                    }
                }
            },
            onForceDelete = {
                onForceDeleteLocation(cell.id) { error ->
                    if (error == null) {
                        settingsTargetCell = null
                        onCloseLocationSettings()
                        onClearUpdateLocationError()
                    }
                }
            }
        )
    }
}

@Composable
private fun AddLocationDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var locationCode by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("") }
    var showColorWheelDialog by remember { mutableStateOf(false) }
    val quickColors = listOf("#FFE082", "#C8E6C9", "#B3E5FC", "#F8BBD0", "#D1C4E9")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.inventory_add_location)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = locationCode,
                    onValueChange = { locationCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(2) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.inventory_add_location_label)) },
                    supportingText = {
                        Text(text = stringResource(R.string.inventory_add_location_hint))
                    }
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.inventory_location_name)) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.inventory_location_color),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        quickColors.forEach { color ->
                            ColorQuickButton(
                                colorHex = color,
                                selected = colorHex == color,
                                onClick = { colorHex = color }
                            )
                        }
                        IconButton(
                            onClick = { showColorWheelDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.inventory_location_pick_color)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(parseColorOrDefault(colorHex))
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        )
                    }
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(locationCode, displayName, colorHex) }) {
                Text(text = stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    )

    if (showColorWheelDialog) {
        ColorWheelDialog(
            initialColorHex = colorHex,
            onDismiss = { showColorWheelDialog = false },
            onConfirm = { pickedColor ->
                colorHex = pickedColor
                showColorWheelDialog = false
            }
        )
    }
}

@Composable
private fun StockLocationCard(
    cell: StockLocationCell,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = parseColorOrDefault(cell.colorHex)
    val contentColor = contentColorForLocationCard(backgroundColor)
    val secondaryContentColor = contentColor.copy(alpha = 0.82f)

    Card(
        modifier = Modifier
            .width(120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = cell.displayName ?: cell.code,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(contentColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                text = cell.code,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor
            )
            Text(
                text = stringResource(R.string.inventory_cell_count, cell.inventoryItemCount),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor
            )
        }
    }
}

@Composable
private fun SelectableLocationCard(
    cell: StockLocationCell,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = parseColorOrDefault(cell.colorHex)
    val contentColor = contentColorForLocationCard(backgroundColor)
    val secondaryContentColor = contentColor.copy(alpha = 0.82f)

    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = cell.displayName ?: cell.code,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(contentColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                text = cell.code,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor
            )
            Text(
                text = stringResource(R.string.inventory_cell_count, cell.inventoryItemCount),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor
            )
        }
    }
}

@Composable
private fun InventoryLocationDetailScreen(
    uiState: InventoryUiState,
    cell: StockLocationCell,
    items: List<LocationInventoryItem>,
    onBack: () -> Unit,
    onSave: (Long, String, String?, String?, String, (String?) -> Unit) -> Unit,
    onClearUpdateLocationError: () -> Unit,
    onDeleteLocation: (Long, (String?) -> Unit) -> Unit,
    onForceDeleteLocation: (Long, (String?) -> Unit) -> Unit,
    onLookupScannedComponent: (String, (InventoryScanLookupResult) -> Unit) -> Unit,
    onAddScannedInbound: (ComponentDetail, Int, String, String?, () -> Unit) -> Unit,
    onUpdateInventoryItemQuantity: (Long, Int, (String?) -> Unit) -> Unit,
    onTransferInventoryItem: (Long, String, (String?) -> Unit) -> Unit,
    onDeleteInventoryItem: (Long, (String?) -> Unit) -> Unit,
    onTransferInventoryItems: (List<Long>, String, (String?) -> Unit) -> Unit,
    onDeleteInventoryItems: (List<Long>, (String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSettingsDialog by remember(cell.id) { mutableStateOf(false) }
    var showScanAddDialog by remember(cell.id) { mutableStateOf(false) }
    val headerBadgeColor = parseColorOrDefault(cell.colorHex)
    val headerBadgeContentColor = contentColorForLocationCard(headerBadgeColor)
    var selectedItem by remember(cell.id) {
        mutableStateOf<com.example.lcsc_android_erp.domain.model.LocationInventoryItem?>(null)
    }
    var selectedItemIds by remember(cell.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var showBatchTransferPicker by remember(cell.id) { mutableStateOf(false) }
    var showBatchDeleteDialog by remember(cell.id) { mutableStateOf(false) }
    var batchActionError by remember(cell.id) { mutableStateOf<String?>(null) }
    var batchSubmitting by remember(cell.id) { mutableStateOf(false) }
    val targetLocationOptions = remember(uiState.cells) { uiState.cells }
    val targetLocationRows = remember(targetLocationOptions) { groupLocationRows(targetLocationOptions) }
    var selectedBatchTransferLocationCode by remember(cell.id, targetLocationOptions) {
        mutableStateOf(
            targetLocationOptions
                .firstOrNull { it.id == cell.id }
                ?.code
                ?: targetLocationOptions.firstOrNull()?.code.orEmpty()
        )
    }

    LaunchedEffect(items) {
        val validIds = items.mapTo(mutableSetOf()) { it.inventoryItemId }
        selectedItemIds = selectedItemIds.filterTo(linkedSetOf()) { it in validIds }
        if (selectedItem?.inventoryItemId !in validIds) {
            selectedItem = null
        }
    }

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = if (selectedItemIds.isNotEmpty()) 132.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.common_back))
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cell.code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerBadgeContentColor,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(headerBadgeColor)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        cell.displayName?.takeIf { it.isNotBlank() && it != cell.code }?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.inventory_location_settings)
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.inventory_location_items),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.inventory_location_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(items, key = { it.inventoryItemId }) { item ->
                    LocationInventoryItemCard(
                        item = item,
                        selected = item.inventoryItemId in selectedItemIds,
                        selectionMode = selectedItemIds.isNotEmpty(),
                        onClick = {
                            batchActionError = null
                            if (selectedItemIds.isNotEmpty()) {
                                selectedItemIds = toggleInventorySelection(selectedItemIds, item.inventoryItemId)
                            } else {
                                selectedItem = item
                            }
                        },
                        onLongClick = {
                            batchActionError = null
                            selectedItemIds = toggleInventorySelection(selectedItemIds, item.inventoryItemId)
                        }
                    )
                }
            }
        }

        if (selectedItemIds.isEmpty()) {
            FloatingActionButton(
                onClick = { showScanAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = 16.dp
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.inventory_location_scan_add)
                )
            }
        }

        if (selectedItemIds.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.inventory_batch_selected_count, selectedItemIds.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    batchActionError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                batchActionError = null
                                selectedItemIds = emptySet()
                            },
                            enabled = !batchSubmitting
                        ) {
                            Text(text = stringResource(R.string.common_cancel))
                        }
                        Button(
                            onClick = {
                                batchActionError = null
                                if (targetLocationOptions.isEmpty()) {
                                    batchActionError = context.getString(R.string.inventory_no_available_locations)
                                } else {
                                    showBatchTransferPicker = true
                                }
                            },
                            enabled = !batchSubmitting && targetLocationOptions.isNotEmpty()
                        ) {
                            Text(text = stringResource(R.string.inventory_batch_transfer))
                        }
                        Button(
                            onClick = {
                                batchActionError = null
                                showBatchDeleteDialog = true
                            },
                            enabled = !batchSubmitting
                        ) {
                            Text(text = stringResource(R.string.common_delete))
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        LocationSettingsDialog(
            cell = cell,
            errorMessage = uiState.updateLocationError,
            availableSecondaryAttributes = supportedSortAttributes(items, cell.sortMode),
            onDismiss = {
                onClearUpdateLocationError()
                showSettingsDialog = false
            },
            onSave = { code, displayName, colorHex, sortMode ->
                onSave(cell.id, code, displayName, colorHex, sortMode) { error ->
                    if (error == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.inventory_location_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        showSettingsDialog = false
                        onClearUpdateLocationError()
                    }
                }
            },
            onDelete = {
                onDeleteLocation(cell.id) { error ->
                    if (error == null) {
                        showSettingsDialog = false
                        onBack()
                    }
                }
            },
            onForceDelete = {
                onForceDeleteLocation(cell.id) { error ->
                    if (error == null) {
                        showSettingsDialog = false
                        onBack()
                    }
                }
            }
        )
    }

    if (showScanAddDialog) {
        LocationScanAddDialog(
            locationCode = cell.code,
            onDismiss = { showScanAddDialog = false },
            onLookupScannedComponent = onLookupScannedComponent,
            onConfirmInbound = { component, quantity, rawPayload ->
                onAddScannedInbound(component, quantity, cell.code, rawPayload) {
                    showScanAddDialog = false
                }
            }
        )
    }

    selectedItem?.let { item ->
        InventoryItemManageDialog(
            item = item,
            currentLocation = cell,
            availableLocations = uiState.cells,
            onUpdateQuantity = onUpdateInventoryItemQuantity,
            onTransfer = onTransferInventoryItem,
            onDelete = onDeleteInventoryItem,
            onDismiss = { selectedItem = null }
        )
    }

    if (showBatchTransferPicker) {
        AlertDialog(
            onDismissRequest = { showBatchTransferPicker = false },
            modifier = Modifier.fillMaxWidth(),
            title = { Text(text = stringResource(R.string.inventory_select_target_location)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(targetLocationRows, key = { it.first }) { (letter, cells) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(24.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                cells.forEach { option ->
                                    SelectableLocationCard(
                                        cell = option,
                                        selected = option.code == selectedBatchTransferLocationCode,
                                        onClick = { selectedBatchTransferLocationCode = option.code }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showBatchTransferPicker = false },
                        enabled = !batchSubmitting
                    ) {
                        Text(text = stringResource(R.string.common_cancel))
                    }
                    TextButton(
                        onClick = {
                            batchActionError = null
                            val targetCode = selectedBatchTransferLocationCode
                            if (targetCode.isBlank()) {
                                batchActionError = context.getString(R.string.inventory_select_target_location_error)
                                return@TextButton
                            }
                            batchSubmitting = true
                            onTransferInventoryItems(selectedItemIds.toList(), targetCode) { error ->
                                batchSubmitting = false
                                batchActionError = error
                                if (error == null) {
                                    showBatchTransferPicker = false
                                    selectedItemIds = emptySet()
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.inventory_batch_transfer_completed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = !batchSubmitting &&
                            selectedItemIds.isNotEmpty() &&
                            selectedBatchTransferLocationCode.isNotBlank() &&
                            selectedBatchTransferLocationCode != cell.code
                    ) {
                        Text(text = stringResource(R.string.inventory_batch_transfer_confirm))
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text(text = stringResource(R.string.inventory_batch_delete_title)) },
            text = { Text(text = stringResource(R.string.inventory_batch_delete_message, selectedItemIds.size)) },
            dismissButton = {
                TextButton(
                    onClick = { showBatchDeleteDialog = false },
                    enabled = !batchSubmitting
                ) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        batchActionError = null
                        batchSubmitting = true
                        onDeleteInventoryItems(selectedItemIds.toList()) { error ->
                            batchSubmitting = false
                            batchActionError = error
                            if (error == null) {
                                showBatchDeleteDialog = false
                                selectedItemIds = emptySet()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.inventory_batch_delete_completed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !batchSubmitting && selectedItemIds.isNotEmpty()
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

@Composable
private fun LocationScanAddDialog(
    locationCode: String,
    onDismiss: () -> Unit,
    onLookupScannedComponent: (String, (InventoryScanLookupResult) -> Unit) -> Unit,
    onConfirmInbound: (ComponentDetail, Int, String?) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    var scannerPaused by rememberSaveable { mutableStateOf(false) }
    var lookupInProgress by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
    var scanResult by remember { mutableStateOf<InventoryScanLookupResult?>(null) }

    if (lookupInProgress || scanResult != null || scanErrorMessage != null) {
        MaterialInboundDialog(
            title = stringResource(R.string.inventory_location_scan_add),
            component = scanResult?.component,
            isLoading = lookupInProgress,
            loadingText = stringResource(R.string.inbound_component_loading),
            errorMessage = scanErrorMessage,
            existingStockLocations = scanResult?.existingStockLocations.orEmpty(),
            quantityText = (scanResult?.quantity ?: 0).toString(),
            quantityEditable = false,
            quantityLabel = stringResource(R.string.inbound_quantity_label),
            onQuantityChange = {},
            selectedLocationCode = locationCode,
            availableLocations = emptyList(),
            onLocationSelected = {},
            onDismiss = onDismiss,
            onConfirm = {
                scanResult?.component?.let { component ->
                    onConfirmInbound(component, scanResult?.quantity ?: 0, scanResult?.rawPayload)
                }
            },
            confirmEnabled = scanResult?.component != null && !lookupInProgress,
            confirmText = stringResource(R.string.common_confirm),
            locationPickerEnabled = false,
            selectedLocationLabelOverride = locationCode,
            leadingActionText = if (lookupInProgress.not() && (scanResult != null || scanErrorMessage != null)) {
                stringResource(R.string.inbound_continue_scan)
            } else {
                null
            },
            onLeadingAction = {
                scannerPaused = false
                lookupInProgress = false
                scanErrorMessage = null
                scanResult = null
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(R.string.inventory_location_scan_add)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.inventory_location_scan_target, locationCode),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ScannerCard(
                        hasCameraPermission = hasCameraPermission,
                        scannerPaused = scannerPaused,
                        onQrScanned = { rawText ->
                            if (lookupInProgress || scanResult != null) {
                                return@ScannerCard
                            }
                            scannerPaused = true
                            lookupInProgress = true
                            onLookupScannedComponent(rawText) { result ->
                                lookupInProgress = false
                                if (result.component != null) {
                                    scanResult = result
                                    scanErrorMessage = null
                                } else {
                                    scannerPaused = false
                                    scanResult = null
                                    scanErrorMessage = result.errorMessage
                                }
                            }
                        },
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ColorQuickButton(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .size(28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(parseColorOrDefault(colorHex))
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small)
                } else {
                    Modifier
                }
            )
    ) {
        Text(text = "")
    }
}

@Composable
private fun SortModeOption(
    label: String,
    priority: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (priority != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
                .border(
                    1.dp,
                    if (priority != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = priority?.toString().orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = if (priority != null) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun LocationSettingsDialog(
    cell: StockLocationCell,
    errorMessage: String?,
    availableSecondaryAttributes: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String) -> Unit,
    onDelete: () -> Unit,
    onForceDelete: () -> Unit
) {
    var code by remember(cell.id) { mutableStateOf(cell.code) }
    var displayName by remember(cell.id) { mutableStateOf(cell.displayName ?: "") }
    var colorHex by remember(cell.id) { mutableStateOf(cell.colorHex ?: "") }
    var sortPriorities by remember(cell.id) { mutableStateOf(StorageLocationSortMode.priorities(cell.sortMode)) }
    var showColorWheelDialog by remember(cell.id) { mutableStateOf(false) }
    var deleteSubmitted by remember(cell.id) { mutableStateOf(false) }
    var deleteBlockedMessage by remember(cell.id) { mutableStateOf<String?>(null) }
    val quickColors = listOf("#FFE082", "#C8E6C9", "#B3E5FC", "#F8BBD0", "#D1C4E9")
    val specificationSortOptions = remember(availableSecondaryAttributes, sortPriorities) {
        buildList {
            availableSecondaryAttributes
                .map(String::trim)
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
                .forEach(::add)
            sortPriorities
                .mapNotNull(StorageLocationSortMode::specificationKey)
                .filter { it.isNotBlank() && it !in this }
                .forEach(::add)
        }
    }

    LaunchedEffect(deleteSubmitted, errorMessage) {
        if (!deleteSubmitted) {
            return@LaunchedEffect
        }
        if (!errorMessage.isNullOrBlank()) {
            deleteBlockedMessage = errorMessage
            deleteSubmitted = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.inventory_location_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(2)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.inventory_location_code)) }
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.inventory_location_name)) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.inventory_location_color),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    quickColors.forEach { color ->
                        ColorQuickButton(
                            colorHex = color,
                            selected = colorHex == color,
                            onClick = { colorHex = color }
                        )
                    }
                        IconButton(
                            onClick = { showColorWheelDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.inventory_location_pick_color)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(parseColorOrDefault(colorHex))
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.inventory_location_sort_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortModeOption(
                            label = stringResource(R.string.inventory_sort_mode_name),
                            priority = sortPriorities.indexOf(StorageLocationSortMode.NAME).takeIf { it >= 0 }?.plus(1),
                            onClick = {
                                sortPriorities = toggleSortPriority(
                                    current = sortPriorities,
                                    target = StorageLocationSortMode.NAME
                                )
                            }
                        )
                        SortModeOption(
                            label = stringResource(R.string.inventory_sort_mode_quantity),
                            priority = sortPriorities.indexOf(StorageLocationSortMode.QUANTITY).takeIf { it >= 0 }?.plus(1),
                            onClick = {
                                sortPriorities = toggleSortPriority(
                                    current = sortPriorities,
                                    target = StorageLocationSortMode.QUANTITY
                                )
                            }
                        )
                        SortModeOption(
                            label = stringResource(R.string.inventory_sort_mode_inbound_time),
                            priority = sortPriorities.indexOf(StorageLocationSortMode.INBOUND_TIME).takeIf { it >= 0 }?.plus(1),
                            onClick = {
                                sortPriorities = toggleSortPriority(
                                    current = sortPriorities,
                                    target = StorageLocationSortMode.INBOUND_TIME
                                )
                            }
                        )
                        specificationSortOptions.forEach { specificationKey ->
                            val priorityToken = StorageLocationSortMode.bySpecification(specificationKey)
                            SortModeOption(
                                label = specificationKey,
                                priority = sortPriorities.indexOf(priorityToken).takeIf { it >= 0 }?.plus(1),
                                onClick = {
                                    sortPriorities = toggleSortPriority(
                                        current = sortPriorities,
                                        target = priorityToken
                                    )
                                }
                            )
                        }
                        if (specificationSortOptions.isEmpty()) {
                            Text(
                                text = stringResource(R.string.inventory_sort_mode_no_secondary_attributes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        code,
                        displayName,
                        colorHex,
                        StorageLocationSortMode.serialize(sortPriorities)
                    )
                }
            ) {
                Text(text = stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        deleteSubmitted = true
                        onDelete()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        }
    )

    if (showColorWheelDialog) {
        ColorWheelDialog(
            initialColorHex = colorHex,
            onDismiss = { showColorWheelDialog = false },
            onConfirm = { pickedColor ->
                colorHex = pickedColor
                showColorWheelDialog = false
            }
        )
    }

    deleteBlockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteBlockedMessage = null },
            title = { Text(text = stringResource(R.string.inventory_delete_location_blocked_title)) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteBlockedMessage = null
                        deleteSubmitted = false
                    }
                ) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteBlockedMessage = null
                        onForceDelete()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_force_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

@Composable
private fun ColorWheelDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedHue by remember(initialColorHex) { mutableStateOf(initialHue(initialColorHex)) }
    val selectedColor = remember(selectedHue) { Color.hsv(selectedHue, 0.75f, 1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.inventory_location_pick_color)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HueWheel(
                    hue = selectedHue,
                    onHueChange = { selectedHue = it }
                )
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(36.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(selectedColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                )
                Text(
                    text = colorToHex(selectedColor),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(colorToHex(selectedColor)) }) {
                Text(text = stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun HueWheel(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    val rainbow = listOf(
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFFFF00FF),
        Color(0xFFFF0000)
    )
    BoxWithConstraints(
        modifier = Modifier
            .size(220.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onHueChange(offset.toHue(size.width / 2f)) },
                    onDrag = { change, _ ->
                        onHueChange(change.position.toHue(size.width / 2f))
                    }
                )
            }
    ) {
        val density = LocalDensity.current
        val wheelSizePx = with(density) { maxWidth.toPx().coerceAtMost(maxHeight.toPx()) }
        val wheelRadiusPx = wheelSizePx / 2f
        val wheelStrokePx = wheelRadiusPx * 0.28f
        val markerTrackRadiusPx = wheelRadiusPx - wheelStrokePx / 2f
        val markerAngle = Math.toRadians(hue.toDouble() - 90.0)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val strokeWidth = radius * 0.28f
            rotate(degrees = -90f) {
                drawCircle(
                    brush = Brush.sweepGradient(rainbow),
                    radius = radius - strokeWidth / 2f,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (cos(markerAngle) * markerTrackRadiusPx).roundToInt(),
                        y = (sin(markerAngle) * markerTrackRadiusPx).roundToInt()
                    )
                }
                .align(Alignment.Center)
                .size(16.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.hsv(hue, 0.75f, 1f))
                .border(2.dp, Color.White, MaterialTheme.shapes.small)
        )
    }
}

@Composable
private fun LocationInventoryItemCard(
    item: LocationInventoryItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val imageModel = item.imageLocalPath
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf { it.exists() && it.length() > 0L }
        ?: item.imageUrl?.takeIf { it.isNotBlank() }
    val secondarySummary = locationItemSecondarySummary(item)

    Card(
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "x${displayQuantity(item.quantity)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name ?: item.mpn ?: item.partNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = listOfNotNull(item.brand, item.packageName, item.category).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    secondarySummary?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.inventory_item_quantity, displayQuantity(item.quantity)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryItemManageDialog(
    item: LocationInventoryItem,
    currentLocation: StockLocationCell,
    availableLocations: List<StockLocationCell>,
    onUpdateQuantity: (Long, Int, (String?) -> Unit) -> Unit,
    onTransfer: (Long, String, (String?) -> Unit) -> Unit,
    onDelete: (Long, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val imageModel = item.imageLocalPath
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf { it.exists() && it.length() > 0L }
        ?: item.imageUrl?.takeIf { it.isNotBlank() }
    val dialogScrollState = rememberScrollState()
    val targetLocationOptions = availableLocations
    val targetLocationRows = groupLocationRows(targetLocationOptions)
    var quantityText by remember(item.inventoryItemId) { mutableStateOf(item.quantity.toString()) }
    var showTransferPicker by remember(item.inventoryItemId) { mutableStateOf(false) }
    var selectedTransferLocationCode by remember(item.inventoryItemId) {
        mutableStateOf(
            targetLocationOptions
                .firstOrNull { it.id == currentLocation.id }
                ?.code
                ?: targetLocationOptions.firstOrNull()?.code
                ?: currentLocation.code
        )
    }
    var actionError by remember(item.inventoryItemId) { mutableStateOf<String?>(null) }
    var isSubmitting by remember(item.inventoryItemId) { mutableStateOf(false) }
    var firstPropertyHeightPx by remember(item.inventoryItemId) { mutableStateOf(0) }
    val firstPropertyRows = listOf(
        stringResource(R.string.inbound_component_number) to item.partNumber,
        stringResource(R.string.inbound_component_brand) to (item.brand ?: stringResource(R.string.inbound_field_empty)),
        stringResource(R.string.inbound_component_package) to (item.packageName ?: stringResource(R.string.inbound_field_empty)),
        stringResource(R.string.inbound_component_category) to (item.category ?: stringResource(R.string.inbound_field_empty))
    )
    val secondPropertyRows = buildList {
        add(stringResource(R.string.inbound_component_name) to (item.name ?: stringResource(R.string.inbound_field_empty)))
        item.specifications.forEach { (key, value) ->
            val normalizedKey = key.trim()
            val normalizedValue = value.trim()
            if (normalizedKey.isNotEmpty() && normalizedValue.isNotEmpty()) {
                add(normalizedKey to normalizedValue)
            }
        }
        item.description?.takeIf { it.isNotBlank() }?.let {
            add(stringResource(R.string.inbound_component_description) to it)
        }
        add(
            stringResource(R.string.inventory_current_location) to (
                currentLocation.displayName?.takeIf { it.isNotBlank() && it != currentLocation.code }
                    ?.let { "${currentLocation.code}:${it}" }
                    ?: currentLocation.code
                )
        )
        add(stringResource(R.string.inventory_inbound_time) to formatDateTime(item.lastInboundAt))
        add(stringResource(R.string.inventory_quantity_label) to displayQuantity(item.quantity))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(text = item.name ?: item.mpn ?: item.partNumber)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(dialogScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    InventoryItemDetailImageCard(
                        imageModel = imageModel,
                        contentDescription = item.name,
                        fallbackText = item.partNumber,
                        imageHeight = with(density) {
                            if (firstPropertyHeightPx > 0) {
                                firstPropertyHeightPx.toDp()
                            } else {
                                168.dp
                            }
                        }
                    )
                    InventoryFirstPropertyCard(
                        rows = firstPropertyRows,
                        modifier = Modifier
                            .weight(1f)
                            .onSizeChanged { size -> firstPropertyHeightPx = size.height }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
                InventoryInfoSection(
                    rows = secondPropertyRows
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.inventory_edit_quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    onClick = {
                        actionError = null
                        val quantity = quantityText.toIntOrNull()
                        if (quantity == null) {
                            actionError = context.getString(R.string.inventory_edit_quantity_error)
                            return@Button
                        }
                        isSubmitting = true
                        onUpdateQuantity(item.inventoryItemId, quantity) { error ->
                            isSubmitting = false
                            actionError = error
                            if (error == null) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text(text = stringResource(R.string.inventory_save_quantity))
                }
                actionError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text(text = stringResource(R.string.common_close))
                }
                TextButton(
                    onClick = {
                        actionError = null
                        if (targetLocationOptions.isEmpty()) {
                            actionError = context.getString(R.string.inventory_no_available_locations)
                            return@TextButton
                        }
                        showTransferPicker = true
                    },
                    enabled = !isSubmitting && targetLocationOptions.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.inventory_transfer_location))
                }
                TextButton(
                    onClick = {
                        actionError = null
                        isSubmitting = true
                        onDelete(item.inventoryItemId) { error ->
                            isSubmitting = false
                            actionError = error
                            if (error == null) {
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text(
                        text = stringResource(R.string.inventory_delete_item),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {}
    )

    if (showTransferPicker) {
        AlertDialog(
            onDismissRequest = { showTransferPicker = false },
            modifier = Modifier.fillMaxWidth(),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(text = stringResource(R.string.inventory_select_target_location)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(targetLocationRows, key = { it.first }) { (letter, cells) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(24.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                cells.forEach { cell ->
                                    SelectableLocationCard(
                                        cell = cell,
                                        selected = cell.code == selectedTransferLocationCode,
                                        onClick = { selectedTransferLocationCode = cell.code }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showTransferPicker = false },
                        enabled = !isSubmitting
                    ) {
                        Text(text = stringResource(R.string.common_cancel))
                    }
                    TextButton(
                        onClick = {
                            actionError = null
                            val targetCode = selectedTransferLocationCode
                            if (targetCode.isBlank()) {
                                actionError = context.getString(R.string.inventory_select_target_location_error)
                                return@TextButton
                            }
                            isSubmitting = true
                            onTransfer(item.inventoryItemId, targetCode) { error ->
                                isSubmitting = false
                                actionError = error
                                if (error == null) {
                                    showTransferPicker = false
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isSubmitting &&
                            selectedTransferLocationCode.isNotBlank() &&
                            selectedTransferLocationCode != currentLocation.code
                    ) {
                        Text(text = stringResource(R.string.inventory_batch_transfer_confirm))
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun InventoryItemDetailImageCard(
    imageModel: Any?,
    contentDescription: String?,
    fallbackText: String,
    imageHeight: androidx.compose.ui.unit.Dp
) {
    Card(
        modifier = Modifier.width(168.dp)
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fallbackText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun InventoryFirstPropertyCard(
    rows: List<Pair<String, String>>
    ,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, (label, value) ->
                InventoryFirstPropertyCell(
                    label = label,
                    value = value,
                    modifier = Modifier.fillMaxWidth()
                )
                if (index != rows.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun InventoryFirstPropertyCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(value))
                    performCopyFeedback(context, hapticFeedback)
                    Toast.makeText(
                        context,
                        context.getString(R.string.common_copied, value),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2
        )
    }
}

@Composable
private fun InventoryInfoSection(
    rows: List<Pair<String, String>>
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            rows.forEachIndexed { index, (label, value) ->
                InventoryInfoRow(
                    label = label,
                    value = value,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                if (index != rows.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun InventoryInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(value))
                    performCopyFeedback(context, hapticFeedback)
                    Toast.makeText(
                        context,
                        context.getString(R.string.common_copied, value),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun supportedSortAttributes(
    items: List<LocationInventoryItem>,
    currentSortMode: String
): List<String> {
    return buildList {
        items.asSequence()
            .flatMap { it.specifications.keys.asSequence() }
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .forEach(::add)
        StorageLocationSortMode.specificationKey(currentSortMode)
            ?.takeIf { it.isNotBlank() && it !in this }
            ?.let(::add)
    }
}

private fun toggleSortPriority(
    current: List<String>,
    target: String
): List<String> {
    return if (target in current) {
        current.filterNot { it == target }
    } else {
        current + target
    }
}

private fun toggleInventorySelection(
    current: Set<Long>,
    inventoryItemId: Long
): Set<Long> {
    return if (inventoryItemId in current) {
        current - inventoryItemId
    } else {
        current + inventoryItemId
    }
}

private fun initialHue(colorHex: String): Float {
    val hsv = FloatArray(3)
    runCatching {
        android.graphics.Color.colorToHSV(
            android.graphics.Color.parseColor(colorHex.ifBlank { "#B3E5FC" }),
            hsv
        )
    }.getOrElse {
        android.graphics.Color.colorToHSV(android.graphics.Color.parseColor("#B3E5FC"), hsv)
    }
    return hsv[0]
}

private fun colorToHex(color: Color): String {
    return String.format("#%06X", 0xFFFFFF and color.toArgb())
}

@Composable
private fun displayQuantity(quantity: Int): String {
    return if (quantity == 0) stringResource(R.string.inventory_unknown_quantity) else quantity.toString()
}

private fun formatDateTime(timestamp: Long): String {
    if (timestamp <= 0L) {
        return "-"
    }
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

private fun locationItemSecondarySummary(item: LocationInventoryItem): String? {
    val preferredKeys = listOf("电阻类型", "阻值", "精度", "功率")
    val specificationSummary = buildList {
        preferredKeys.forEach { key ->
            item.specifications[key]
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(::add)
        }
        item.specifications
            .filterKeys { it !in preferredKeys }
            .toSortedMap()
            .values
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .forEach(::add)
    }.distinct().joinToString(" · ")

    return specificationSummary.takeIf { it.isNotBlank() }
        ?: item.mpn?.trim()?.takeIf { it.isNotEmpty() }
        ?: item.description?.trim()?.takeIf { it.isNotEmpty() }
}

private fun Offset.toHue(radius: Float): Float {
    val dx = x - radius
    val dy = y - radius
    return (((Math.toDegrees(atan2(dy, dx).toDouble()) + 90.0) + 360.0) % 360.0).toFloat()
}

@Composable
private fun parseColorOrDefault(colorHex: String?): Color {
    val fallback = MaterialTheme.colorScheme.surfaceVariant
    return try {
        if (colorHex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: IllegalArgumentException) {
        fallback
    }
}

private fun contentColorForLocationCard(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.62f) {
        Color(0xFF111827)
    } else {
        Color.White
    }
}

private fun locationRowLabel(code: String): String {
    return code.takeWhile { it.isLetter() }.ifBlank { "#" }
}

private fun locationRowIndex(code: String): Int {
    return code.firstOrNull()?.uppercaseChar()?.code ?: Int.MAX_VALUE
}

private fun locationColumnIndex(code: String): Int {
    return code.dropWhile { it.isLetter() }.toIntOrNull() ?: Int.MAX_VALUE
}

private fun groupLocationRows(cells: List<StockLocationCell>): List<Pair<String, List<StockLocationCell>>> {
    return cells
        .sortedWith(compareBy({ locationRowIndex(it.code) }, { locationColumnIndex(it.code) }, { it.code }))
        .groupBy { locationRowLabel(it.code) }
        .toList()
        .sortedBy { (rowLabel, _) -> rowLabel }
}
