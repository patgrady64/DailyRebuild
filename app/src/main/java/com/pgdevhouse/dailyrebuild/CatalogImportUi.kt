package com.pgdevhouse.dailyrebuild

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.catalog.CatalogJsonImportManager
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Import surface for the normalized JSON created by Catalog Assistant. */
@Composable
fun CatalogImportSection(
    database: DailyRebuildDatabase,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember(context, database) {
        CatalogJsonImportManager(
            context = context.applicationContext,
            database = database
        )
    }
    val scope = rememberCoroutineScope()

    var isWorking by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var inspection by remember {
        mutableStateOf<CatalogJsonImportManager.Inspection?>(null)
    }
    var currentStatus by remember {
        mutableStateOf<CatalogJsonImportManager.CurrentStatus?>(null)
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshStatus() {
        scope.launch {
            currentStatus = runCatching { manager.currentStatus() }.getOrNull()
        }
    }

    LaunchedEffect(Unit) {
        currentStatus = manager.currentStatus()
    }

    val openCatalogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                statusMessage = null
                errorMessage = null
                runCatching {
                    manager.inspectUri(uri)
                }.onSuccess { result ->
                    pendingUri = uri
                    inspection = result
                }.onFailure { error ->
                    errorMessage = error.catalogImportMessage(
                        "Could not inspect this catalog JSON."
                    )
                }
                isWorking = false
            }
        }
    }

    fun importSelectedCatalog() {
        val uri = pendingUri ?: return
        scope.launch {
            inspection = null
            isWorking = true
            statusMessage = null
            errorMessage = null
            runCatching {
                manager.importFromUri(uri)
            }.onSuccess { result ->
                pendingUri = null
                statusMessage = buildString {
                    append("Catalog imported: ${result.activeProductCount} active products, ")
                    append("${result.roleCount} active roles, and ")
                    append("${result.pantryItemCount} pantry items")
                    if (result.warningCount > 0) {
                        append(" (${result.warningCount} warnings reviewed).")
                    } else {
                        append(".")
                    }
                }
                refreshStatus()
            }.onFailure { error ->
                errorMessage = error.catalogImportMessage(
                    "The catalog was not imported. No catalog changes were committed."
                )
            }
            isWorking = false
        }
    }

    RebuildSectionCard(
        title = "Food Catalog Import",
        subtitle = "Load the normalized JSON created by DailyRebuildCatalogAssistant.",
        accentColor = RebuildGreen,
        modifier = modifier
    ) {
        Text(
            text = "This imports exact foods, desserts, snacks and sides, product roles, package nutrition, and exact pantry quantities. It does not replace your daily logs or backup data.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        currentStatus?.let { status ->
            RebuildInsetPanel {
                Text("Current imported catalog", fontWeight = FontWeight.SemiBold)
                Text(
                    "${status.activeProductCount} active of ${status.productCount} products · " +
                        "${status.roleCount} roles · ${status.pantryItemCount} pantry items"
                )
                Text(
                    text = status.lastImportedAtEpochMillis?.let {
                        "Last imported ${formatCatalogDate(it)}"
                    } ?: "No catalog JSON has been imported yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = {
                openCatalogLauncher.launch(
                    arrayOf(
                        "application/json",
                        "text/json",
                        "text/plain",
                        "application/octet-stream"
                    )
                )
            },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Catalog JSON")
        }

        if (isWorking) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Checking catalog…")
            }
        }

        statusMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    inspection?.let { result ->
        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    inspection = null
                    pendingUri = null
                }
            },
            title = {
                Text(if (result.canImport) "Import Food Catalog?" else "Catalog Cannot Be Imported")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(result.sourceLabel, fontWeight = FontWeight.SemiBold)
                    Text("Created: ${formatCatalogDate(result.exportedAtEpochMillis)}")
                    Text("Schema: ${result.schemaVersion} · ${result.generator}")
                    Text("Source workbooks: ${result.sourceFileCount}")
                    Text(
                        "Products: ${result.productCount} (${result.activeProductCount} active)",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Active product roles: ${result.roleCount}")
                    Text("Pantry items: ${result.pantryItemCount}")

                    if (result.blockingErrors.isNotEmpty()) {
                        Text("Blocking errors", fontWeight = FontWeight.Bold)
                        result.blockingErrors.take(10).forEach { message ->
                            Text("• $message", color = MaterialTheme.colorScheme.error)
                        }
                        if (result.blockingErrors.size > 10) {
                            Text("• ${result.blockingErrors.size - 10} more errors")
                        }
                    }

                    if (result.warnings.isNotEmpty()) {
                        Text("Warnings", fontWeight = FontWeight.Bold)
                        result.warnings.take(8).forEach { message ->
                            Text("• $message", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (result.warnings.size > 8) {
                            Text("• ${result.warnings.size - 8} more warnings")
                        }
                    }

                    if (result.canImport) {
                        Text(
                            "Products are synchronized by stable Product ID. Older products omitted from this bundle are retained but deactivated. Product roles, exact pantry quantities, and source fingerprints are replaced in one transaction. Daily logs are untouched.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (result.canImport) {
                    TextButton(
                        onClick = ::importSelectedCatalog,
                        enabled = !isWorking
                    ) {
                        Text("Import")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        inspection = null
                        pendingUri = null
                    },
                    enabled = !isWorking
                ) {
                    Text(if (result.canImport) "Cancel" else "Close")
                }
            }
        )
    }
}

private fun Throwable.catalogImportMessage(fallback: String): String {
    val detail = generateSequence(this) { it.cause }
        .mapNotNull { it.message?.takeIf(String::isNotBlank) }
        .firstOrNull()
    return if (detail == null) fallback else "$fallback $detail"
}

private fun formatCatalogDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "MMM d, yyyy 'at' h:mm a",
                Locale.US
            )
        )
