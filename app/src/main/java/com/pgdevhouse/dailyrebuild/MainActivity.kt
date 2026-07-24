package com.pgdevhouse.dailyrebuild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.ui.theme.DailyRebuildTheme
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.pgdevhouse.dailyrebuild.data.remote.FoodLookupResult
import com.pgdevhouse.dailyrebuild.data.remote.OpenFoodFactsLookup
import com.pgdevhouse.dailyrebuild.data.remote.ScannedFoodPrefill

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyRebuildTheme {
                DailyRebuildApp()
            }
        }
    }
}

@Composable
fun DailyRebuildApp() {
    val context = LocalContext.current

    val database = remember {
        DailyRebuildDatabase.getDatabase(context)
    }

    val dailyRecordDao = remember {
        database.dailyRecordDao()
    }

    val foodDao = remember {
        database.foodDao()
    }

    val barcodeScannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13
            )
            .enableAutoZoom()
            .build()
    }

    val barcodeScanner = remember(
        context,
        barcodeScannerOptions
    ) {
        GmsBarcodeScanning.getClient(
            context,
            barcodeScannerOptions
        )
    }

    val todayDate = remember {
        LocalDate.now().toString()
    }

    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var isAddingFood by remember {
        mutableStateOf(false)
    }

    var showManualFoodDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var isScanningBarcode by remember {
        mutableStateOf(false)
    }

    var lastScannedBarcode by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var scannedFoodPrefill by remember {
        mutableStateOf<ScannedFoodPrefill?>(
            null
        )
    }

    var foodEntries by remember {
        mutableStateOf<List<FoodLogEntry>>(
            emptyList()
        )
    }

    /*
     * Daily tasks
     */
    var foodRecorded by rememberSaveable {
        mutableStateOf(false)
    }

    var walkCompleted by rememberSaveable {
        mutableStateOf(false)
    }

    var painRecorded by rememberSaveable {
        mutableStateOf(false)
    }

    var mobilityCompleted by rememberSaveable {
        mutableStateOf(false)
    }

    /*
     * Pain
     */
    var backPain by rememberSaveable {
        mutableStateOf(0f)
    }

    var shinPain by rememberSaveable {
        mutableStateOf(0f)
    }

    /*
     * Water
     */
    var nextBottleHasMio by rememberSaveable {
        mutableStateOf(false)
    }

    var plainReusableBottleCount by rememberSaveable {
        mutableStateOf(0)
    }

    var mioReusableBottleCount by rememberSaveable {
        mutableStateOf(0)
    }

    var plainDisposableBottleCount by rememberSaveable {
        mutableStateOf(0)
    }

    var mioDisposableBottleCount by rememberSaveable {
        mutableStateOf(0)
    }

    /*
     * Morning pain relievers
     */
    var morningAspirinTaken by rememberSaveable {
        mutableStateOf(true)
    }

    var morningIbuprofenTaken by rememberSaveable {
        mutableStateOf(true)
    }

    var morningNaproxenTaken by rememberSaveable {
        mutableStateOf(true)
    }

    var morningAcetaminophenTaken by rememberSaveable {
        mutableStateOf(true)
    }

    /*
     * Night pain relievers
     */
    var nightIbuprofenTaken by rememberSaveable {
        mutableStateOf(true)
    }

    var nightNaproxenTaken by rememberSaveable {
        mutableStateOf(true)
    }

    var nightAcetaminophenTaken by rememberSaveable {
        mutableStateOf(true)
    }

    /*
     * Journal
     */
    var journalText by rememberSaveable {
        mutableStateOf("")
    }

    /*
     * Load today's daily record and food entries.
     */
    LaunchedEffect(todayDate) {
        try {
            val savedRecord =
                dailyRecordDao.getRecordByDate(
                    todayDate
                )

            if (savedRecord != null) {
                foodRecorded =
                    savedRecord.foodRecorded

                walkCompleted =
                    savedRecord.walkCompleted

                painRecorded =
                    savedRecord.painRecorded

                mobilityCompleted =
                    savedRecord.mobilityCompleted

                backPain =
                    savedRecord.backPain

                shinPain =
                    savedRecord.shinPain

                plainReusableBottleCount =
                    savedRecord
                        .plainReusableBottleCount

                mioReusableBottleCount =
                    savedRecord
                        .mioReusableBottleCount

                plainDisposableBottleCount =
                    savedRecord
                        .plainDisposableBottleCount

                mioDisposableBottleCount =
                    savedRecord
                        .mioDisposableBottleCount

                morningAspirinTaken =
                    savedRecord
                        .morningAspirinTaken

                morningIbuprofenTaken =
                    savedRecord
                        .morningIbuprofenTaken

                morningNaproxenTaken =
                    savedRecord
                        .morningNaproxenTaken

                morningAcetaminophenTaken =
                    savedRecord
                        .morningAcetaminophenTaken

                nightIbuprofenTaken =
                    savedRecord
                        .nightIbuprofenTaken

                nightNaproxenTaken =
                    savedRecord
                        .nightNaproxenTaken

                nightAcetaminophenTaken =
                    savedRecord
                        .nightAcetaminophenTaken

                journalText =
                    savedRecord.journalText
            }

            foodEntries =
                foodDao.getEntriesForDate(
                    todayDate
                )

            /*
             * Existing food entries count as recording food,
             * even if Save Today was not pressed afterward.
             */
            if (foodEntries.isNotEmpty()) {
                foodRecorded = true
            }
        } catch (exception: Exception) {
            snackbarHostState.showSnackbar(
                message =
                    "Could not load today's record."
            )
        } finally {
            isLoading = false
        }
    }

    val completedTasks = listOf(
        foodRecorded,
        walkCompleted,
        painRecorded,
        mobilityCompleted
    ).count { it }

    val progressPercent =
        completedTasks * 25

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

        if (isLoading) {
            LoadingScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {
                HeaderSection()

                ProgressSection(
                    completedTasks =
                        completedTasks,

                    progressPercent =
                        progressPercent
                )

                DailyTasksSection(
                    foodRecorded =
                        foodRecorded,

                    onFoodRecordedChange = {
                        foodRecorded = it
                    },

                    walkCompleted =
                        walkCompleted,

                    onWalkCompletedChange = {
                        walkCompleted = it
                    },

                    painRecorded =
                        painRecorded,

                    onPainRecordedChange = {
                        painRecorded = it
                    },

                    mobilityCompleted =
                        mobilityCompleted,

                    onMobilityCompletedChange = {
                        mobilityCompleted = it
                    }
                )

                /*
                 * This calls the new FoodSection in FoodUi.kt.
                 *
                 * The old no-argument FoodSection farther down
                 * in MainActivity can remain for now. Kotlin
                 * selects this version because the parameters differ.
                 */
                FoodSection(
                    entries = foodEntries,

                    lastScannedBarcode =
                        lastScannedBarcode,

                    isScanningBarcode =
                        isScanningBarcode,

                    onScanFood = {
                        if (!isScanningBarcode) {
                            isScanningBarcode = true

                            barcodeScanner
                                .startScan()
                                .addOnSuccessListener { barcode ->

                                    val scannedValue =
                                        barcode.rawValue

                                    if (scannedValue.isNullOrBlank()) {
                                        isScanningBarcode = false

                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message =
                                                    "The barcode could not be read."
                                            )
                                        }

                                        return@addOnSuccessListener
                                    }

                                    lastScannedBarcode =
                                        scannedValue

                                    coroutineScope.launch {
                                        try {
                                            /*
                                             * Use our locally saved copy first.
                                             */
                                            val existingProduct =
                                                foodDao.getProductByBarcode(
                                                    scannedValue
                                                )

                                            if (existingProduct != null) {
                                                scannedFoodPrefill =
                                                    ScannedFoodPrefill(
                                                        barcode =
                                                            scannedValue,

                                                        name =
                                                            existingProduct.name,

                                                        brand =
                                                            existingProduct.brand,

                                                        caloriesPerServing =
                                                            existingProduct
                                                                .caloriesPerServing,

                                                        proteinGramsPerServing =
                                                            existingProduct
                                                                .proteinGramsPerServing,

                                                        carbohydrateGramsPerServing =
                                                            existingProduct
                                                                .carbohydrateGramsPerServing,

                                                        fatGramsPerServing =
                                                            existingProduct
                                                                .fatGramsPerServing,

                                                        sodiumMilligramsPerServing =
                                                            existingProduct
                                                                .sodiumMilligramsPerServing,

                                                        servingQuantity =
                                                            existingProduct
                                                                .servingQuantity,

                                                        servingUnit =
                                                            existingProduct
                                                                .servingUnit,

                                                        packageUnit =
                                                            existingProduct
                                                                .packageUnit
                                                                .orEmpty(),

                                                        isFavorite =
                                                            existingProduct
                                                                .isFavorite,

                                                        originalServingSize =
                                                            "${existingProduct.servingQuantity} " +
                                                                    existingProduct.servingUnit
                                                    )

                                                showManualFoodDialog = true

                                                snackbarHostState.showSnackbar(
                                                    message =
                                                        "Loaded saved product."
                                                )
                                            } else {
                                                when (
                                                    val result =
                                                        OpenFoodFactsLookup
                                                            .findProduct(
                                                                scannedValue
                                                            )
                                                ) {
                                                    is FoodLookupResult.Found -> {
                                                        scannedFoodPrefill =
                                                            result.food

                                                        showManualFoodDialog =
                                                            true

                                                        snackbarHostState
                                                            .showSnackbar(
                                                                message =
                                                                    "Product found."
                                                            )
                                                    }

                                                    FoodLookupResult.NotFound -> {
                                                        /*
                                                         * Keep the barcode, but allow
                                                         * manual product creation.
                                                         */
                                                        scannedFoodPrefill =
                                                            ScannedFoodPrefill(
                                                                barcode =
                                                                    scannedValue
                                                            )

                                                        showManualFoodDialog =
                                                            true

                                                        snackbarHostState
                                                            .showSnackbar(
                                                                message =
                                                                    "Product not found. " +
                                                                            "Enter its label manually."
                                                            )
                                                    }

                                                    is FoodLookupResult.Failed -> {
                                                        scannedFoodPrefill =
                                                            ScannedFoodPrefill(
                                                                barcode =
                                                                    scannedValue
                                                            )

                                                        showManualFoodDialog =
                                                            true

                                                        snackbarHostState
                                                            .showSnackbar(
                                                                message =
                                                                    "Lookup failed. " +
                                                                            "You can enter the label manually."
                                                            )
                                                    }
                                                }
                                            }
                                        } finally {
                                            isScanningBarcode = false
                                        }
                                    }
                                }
                                .addOnCanceledListener {
                                    isScanningBarcode = false
                                }
                                .addOnFailureListener {
                                    isScanningBarcode = false

                                    coroutineScope.launch {
                                        snackbarHostState
                                            .showSnackbar(
                                                message =
                                                    "Could not open the barcode scanner."
                                            )
                                    }
                                }
                        }
                    },

                    onAddFoodManually = {
                        scannedFoodPrefill = null
                        showManualFoodDialog = true
                    },

                    onDeleteEntry = { entry ->
                        coroutineScope.launch {
                            try {
                                foodDao
                                    .deleteFoodEntryById(
                                        entry.id
                                    )

                                foodEntries =
                                    foodDao
                                        .getEntriesForDate(
                                            todayDate
                                        )

                                snackbarHostState
                                    .showSnackbar(
                                        message =
                                            "Food entry deleted."
                                    )
                            } catch (
                                exception: Exception
                            ) {
                                snackbarHostState
                                    .showSnackbar(
                                        message =
                                            "Could not delete food."
                                    )
                            }
                        }
                    }
                )

                WaterSection(
                    nextBottleHasMio =
                        nextBottleHasMio,

                    onNextBottleHasMioChange = {
                        nextBottleHasMio = it
                    },

                    plainReusableBottleCount =
                        plainReusableBottleCount,

                    mioReusableBottleCount =
                        mioReusableBottleCount,

                    plainDisposableBottleCount =
                        plainDisposableBottleCount,

                    mioDisposableBottleCount =
                        mioDisposableBottleCount,

                    onAddReusableBottle = {
                        if (nextBottleHasMio) {
                            mioReusableBottleCount++
                        } else {
                            plainReusableBottleCount++
                        }

                        foodRecorded = true
                    },

                    onAddDisposableBottle = {
                        if (nextBottleHasMio) {
                            mioDisposableBottleCount++
                        } else {
                            plainDisposableBottleCount++
                        }

                        foodRecorded = true
                    },

                    onRemovePlainReusableBottle = {
                        if (
                            plainReusableBottleCount > 0
                        ) {
                            plainReusableBottleCount--
                        }
                    },

                    onRemoveMioReusableBottle = {
                        if (
                            mioReusableBottleCount > 0
                        ) {
                            mioReusableBottleCount--
                        }
                    },

                    onRemovePlainDisposableBottle = {
                        if (
                            plainDisposableBottleCount > 0
                        ) {
                            plainDisposableBottleCount--
                        }
                    },

                    onRemoveMioDisposableBottle = {
                        if (
                            mioDisposableBottleCount > 0
                        ) {
                            mioDisposableBottleCount--
                        }
                    }
                )

                PainSection(
                    backPain = backPain,

                    onBackPainChange = {
                        backPain = it
                        painRecorded = true
                    },

                    shinPain = shinPain,

                    onShinPainChange = {
                        shinPain = it
                        painRecorded = true
                    }
                )

                MedicationSection(
                    morningAspirinTaken =
                        morningAspirinTaken,

                    onMorningAspirinChange = {
                        morningAspirinTaken = it
                    },

                    morningIbuprofenTaken =
                        morningIbuprofenTaken,

                    onMorningIbuprofenChange = {
                        morningIbuprofenTaken = it
                    },

                    morningNaproxenTaken =
                        morningNaproxenTaken,

                    onMorningNaproxenChange = {
                        morningNaproxenTaken = it
                    },

                    morningAcetaminophenTaken =
                        morningAcetaminophenTaken,

                    onMorningAcetaminophenChange = {
                        morningAcetaminophenTaken = it
                    },

                    nightIbuprofenTaken =
                        nightIbuprofenTaken,

                    onNightIbuprofenChange = {
                        nightIbuprofenTaken = it
                    },

                    nightNaproxenTaken =
                        nightNaproxenTaken,

                    onNightNaproxenChange = {
                        nightNaproxenTaken = it
                    },

                    nightAcetaminophenTaken =
                        nightAcetaminophenTaken,

                    onNightAcetaminophenChange = {
                        nightAcetaminophenTaken = it
                    }
                )

                JournalSection(
                    journalText = journalText,

                    onJournalTextChange = {
                        journalText = it
                    }
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSaving = true

                            try {
                                val record =
                                    DailyRecord(
                                        date =
                                            todayDate,

                                        foodRecorded =
                                            foodRecorded,

                                        walkCompleted =
                                            walkCompleted,

                                        painRecorded =
                                            painRecorded,

                                        mobilityCompleted =
                                            mobilityCompleted,

                                        backPain =
                                            backPain,

                                        shinPain =
                                            shinPain,

                                        plainReusableBottleCount =
                                            plainReusableBottleCount,

                                        mioReusableBottleCount =
                                            mioReusableBottleCount,

                                        plainDisposableBottleCount =
                                            plainDisposableBottleCount,

                                        mioDisposableBottleCount =
                                            mioDisposableBottleCount,

                                        morningAspirinTaken =
                                            morningAspirinTaken,

                                        morningIbuprofenTaken =
                                            morningIbuprofenTaken,

                                        morningNaproxenTaken =
                                            morningNaproxenTaken,

                                        morningAcetaminophenTaken =
                                            morningAcetaminophenTaken,

                                        nightIbuprofenTaken =
                                            nightIbuprofenTaken,

                                        nightNaproxenTaken =
                                            nightNaproxenTaken,

                                        nightAcetaminophenTaken =
                                            nightAcetaminophenTaken,

                                        journalText =
                                            journalText,

                                        updatedAt =
                                            System.currentTimeMillis()
                                    )

                                dailyRecordDao.saveRecord(
                                    record
                                )

                                snackbarHostState
                                    .showSnackbar(
                                        message =
                                            "Today saved."
                                    )
                            } catch (
                                exception: Exception
                            ) {
                                snackbarHostState
                                    .showSnackbar(
                                        message =
                                            "Could not save today."
                                    )
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        Text("Saving...")
                    } else {
                        Text("Save Today")
                    }
                }

                Text(
                    text =
                        "You can continue editing today " +
                                "and press Save Today again. " +
                                "The existing record will be updated.",
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }

    if (showManualFoodDialog) {
        ManualFoodDialog(
            initialFood = scannedFoodPrefill,
            isSaving = isAddingFood,

            onDismiss = {
                if (!isAddingFood) {
                    showManualFoodDialog = false
                    scannedFoodPrefill = null
                }
            },

            onSave = { draft ->
                coroutineScope.launch {
                    isAddingFood = true

                    try {
                        val existingProduct =
                            draft.product.barcode?.let {
                                foodDao.getProductByBarcode(it)
                            }

                        val productId =
                            if (existingProduct == null) {
                                foodDao.addProduct(
                                    draft.product
                                )
                            } else {
                                foodDao.updateProduct(
                                    draft.product.copy(
                                        id = existingProduct.id,
                                        createdAt =
                                            existingProduct.createdAt,
                                        updatedAt =
                                            System.currentTimeMillis()
                                    )
                                )

                                existingProduct.id
                            }

                        val entry =
                            FoodLogEntry(
                                date =
                                    todayDate,

                                productId =
                                    productId,

                                quantity =
                                    draft.quantityEaten,

                                unit =
                                    draft.unit,

                                mealName =
                                    draft.mealName,

                                productNameSnapshot =
                                    draft.product.name,

                                calories =
                                    draft.calories,

                                proteinGrams =
                                    draft.proteinGrams,

                                carbohydrateGrams =
                                    draft
                                        .carbohydrateGrams,

                                fatGrams =
                                    draft.fatGrams,

                                sodiumMilligrams =
                                    draft
                                        .sodiumMilligrams
                            )

                        foodDao.addFoodEntry(
                            entry
                        )

                        foodEntries =
                            foodDao
                                .getEntriesForDate(
                                    todayDate
                                )

                        foodRecorded = true

                        showManualFoodDialog =
                            false

                        scannedFoodPrefill = null

                        snackbarHostState
                            .showSnackbar(
                                message =
                                    "Food added."
                            )
                    } catch (
                        exception: Exception
                    ) {
                        snackbarHostState
                            .showSnackbar(
                                message =
                                    "Could not add food."
                            )
                    } finally {
                        isAddingFood = false
                    }
                }
            }
        )
    }
}

@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text("Loading today...")
    }
}

@Composable
private fun HeaderSection() {
    val today = LocalDate.now()

    val dayFormatter =
        DateTimeFormatter.ofPattern("EEEE")

    val dateFormatter =
        DateTimeFormatter.ofPattern(
            "MMMM d, yyyy"
        )

    Column {
        Text(
            text = "Daily Rebuild",
            style =
                MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = today.format(
                dayFormatter
            ),
            style =
                MaterialTheme.typography.titleLarge
        )

        Text(
            text = today.format(
                dateFormatter
            ),
            style =
                MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ProgressSection(
    completedTasks: Int,
    progressPercent: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Progress",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "$completedTasks of 4 tasks completed"
            )

            Text(
                text = "$progressPercent%",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DailyTasksSection(
    foodRecorded: Boolean,
    onFoodRecordedChange: (Boolean) -> Unit,

    walkCompleted: Boolean,
    onWalkCompletedChange: (Boolean) -> Unit,

    painRecorded: Boolean,
    onPainRecordedChange: (Boolean) -> Unit,

    mobilityCompleted: Boolean,
    onMobilityCompletedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Next Steps",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            TaskCheckbox(
                text =
                    "Record food and drinks",
                checked = foodRecorded,
                onCheckedChange =
                    onFoodRecordedChange
            )

            TaskCheckbox(
                text =
                    "Complete today's walk or movement",
                checked = walkCompleted,
                onCheckedChange =
                    onWalkCompletedChange
            )

            TaskCheckbox(
                text =
                    "Record pain levels",
                checked = painRecorded,
                onCheckedChange =
                    onPainRecordedChange
            )

            TaskCheckbox(
                text =
                    "Complete mobility routine",
                checked = mobilityCompleted,
                onCheckedChange =
                    onMobilityCompletedChange
            )
        }
    }
}

@Composable
private fun TaskCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange =
                onCheckedChange
        )

        Text(
            text = text,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FoodSection() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Fuel",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                "No food recorded yet."
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = {
                    /*
                     * Barcode scanning will be added
                     * after the food database.
                     */
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Scan Food Barcode"
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = {
                    /*
                     * Manual food entry will be added
                     * after the food database.
                     */
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Add Food Manually"
                )
            }
        }
    }
}

@Composable
private fun WaterSection(
    nextBottleHasMio: Boolean,
    onNextBottleHasMioChange: (Boolean) -> Unit,

    plainReusableBottleCount: Int,
    mioReusableBottleCount: Int,
    plainDisposableBottleCount: Int,
    mioDisposableBottleCount: Int,

    onAddReusableBottle: () -> Unit,
    onAddDisposableBottle: () -> Unit,

    onRemovePlainReusableBottle: () -> Unit,
    onRemoveMioReusableBottle: () -> Unit,
    onRemovePlainDisposableBottle: () -> Unit,
    onRemoveMioDisposableBottle: () -> Unit
) {
    val reusableBottleTotal =
        plainReusableBottleCount +
                mioReusableBottleCount

    val disposableBottleTotal =
        plainDisposableBottleCount +
                mioDisposableBottleCount

    val totalWaterOunces =
        (reusableBottleTotal * 24.0) +
                (disposableBottleTotal * 16.9)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Water",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "${formatOunces(totalWaterOunces)} oz today",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Checkbox(
                    checked =
                        nextBottleHasMio,
                    onCheckedChange =
                        onNextBottleHasMioChange
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "MiO added",
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        text =
                            "Applies to the next bottle logged",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick =
                    onAddReusableBottle,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "Add Reusable Bottle — 24 oz"
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick =
                    onAddDisposableBottle,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "Add Water Bottle — 16.9 oz"
                )
            }

            if (
                reusableBottleTotal > 0 ||
                disposableBottleTotal > 0
            ) {
                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical = 16.dp
                        )
                )

                Text(
                    text =
                        "Today's Water Entries",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (
                    plainReusableBottleCount > 0
                ) {
                    WaterCountRow(
                        label =
                            "20 oz plain water",
                        count =
                            plainReusableBottleCount,
                        onRemoveOne =
                            onRemovePlainReusableBottle
                    )
                }

                if (
                    mioReusableBottleCount > 0
                ) {
                    WaterCountRow(
                        label =
                            "20 oz MiO water",
                        count =
                            mioReusableBottleCount,
                        onRemoveOne =
                            onRemoveMioReusableBottle
                    )
                }

                if (
                    plainDisposableBottleCount > 0
                ) {
                    WaterCountRow(
                        label =
                            "16.9 oz plain water",
                        count =
                            plainDisposableBottleCount,
                        onRemoveOne =
                            onRemovePlainDisposableBottle
                    )
                }

                if (
                    mioDisposableBottleCount > 0
                ) {
                    WaterCountRow(
                        label =
                            "16.9 oz MiO water",
                        count =
                            mioDisposableBottleCount,
                        onRemoveOne =
                            onRemoveMioDisposableBottle
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterCountRow(
    label: String,
    count: Int,
    onRemoveOne: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = "$label × $count",
            modifier = Modifier.weight(1f)
        )

        TextButton(
            onClick = onRemoveOne
        ) {
            Text("Remove One")
        }
    }
}

private fun formatOunces(
    ounces: Double
): String {
    return if (
        ounces % 1.0 == 0.0
    ) {
        ounces.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.1f",
            ounces
        )
    }
}

@Composable
private fun PainSection(
    backPain: Float,
    onBackPainChange: (Float) -> Unit,

    shinPain: Float,
    onShinPainChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pain",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            PainSlider(
                label =
                    "Lower Back Pain",
                painValue =
                    backPain,
                onPainValueChange =
                    onBackPainChange
            )

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        vertical = 12.dp
                    )
            )

            PainSlider(
                label = "Shin Pain",
                painValue = shinPain,
                onPainValueChange =
                    onShinPainChange
            )
        }
    }
}

@Composable
private fun PainSlider(
    label: String,
    painValue: Float,
    onPainValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    painValue.toInt().toString(),
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = painValue,
            onValueChange =
                onPainValueChange,
            valueRange = 0f..10f,
            steps = 9
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text("No pain")
            Text("Worst pain")
        }
    }
}

@Composable
private fun MedicationSection(
    morningAspirinTaken: Boolean,
    onMorningAspirinChange: (Boolean) -> Unit,

    morningIbuprofenTaken: Boolean,
    onMorningIbuprofenChange: (Boolean) -> Unit,

    morningNaproxenTaken: Boolean,
    onMorningNaproxenChange: (Boolean) -> Unit,

    morningAcetaminophenTaken: Boolean,
    onMorningAcetaminophenChange: (Boolean) -> Unit,

    nightIbuprofenTaken: Boolean,
    onNightIbuprofenChange: (Boolean) -> Unit,

    nightNaproxenTaken: Boolean,
    onNightNaproxenChange: (Boolean) -> Unit,

    nightAcetaminophenTaken: Boolean,
    onNightAcetaminophenChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pain Relievers",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Prefilled from your normal pill organizer. " +
                            "Uncheck any dose you did not take.",
                style =
                    MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Morning / Day",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            MedicationCheckbox(
                name = "Aspirin",
                dose = "325 mg",
                checked =
                    morningAspirinTaken,
                onCheckedChange =
                    onMorningAspirinChange
            )

            MedicationCheckbox(
                name = "Ibuprofen",
                dose =
                    "400 mg — 2 × 200 mg",
                checked =
                    morningIbuprofenTaken,
                onCheckedChange =
                    onMorningIbuprofenChange
            )

            MedicationCheckbox(
                name =
                    "Naproxen sodium",
                dose = "220 mg",
                checked =
                    morningNaproxenTaken,
                onCheckedChange =
                    onMorningNaproxenChange
            )

            MedicationCheckbox(
                name = "Acetaminophen",
                dose =
                    "1,000 mg — 2 × 500 mg",
                checked =
                    morningAcetaminophenTaken,
                onCheckedChange =
                    onMorningAcetaminophenChange
            )

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        vertical = 12.dp
                    )
            )

            Text(
                text = "Night",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            MedicationCheckbox(
                name = "Ibuprofen",
                dose =
                    "400 mg — 2 × 200 mg",
                checked =
                    nightIbuprofenTaken,
                onCheckedChange =
                    onNightIbuprofenChange
            )

            MedicationCheckbox(
                name =
                    "Naproxen sodium",
                dose = "220 mg",
                checked =
                    nightNaproxenTaken,
                onCheckedChange =
                    onNightNaproxenChange
            )

            MedicationCheckbox(
                name = "Acetaminophen",
                dose =
                    "1,000 mg — 2 × 500 mg",
                checked =
                    nightAcetaminophenTaken,
                onCheckedChange =
                    onNightAcetaminophenChange
            )
        }
    }
}

@Composable
private fun MedicationCheckbox(
    name: String,
    dose: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange =
                onCheckedChange
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text = dose,
                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun JournalSection(
    journalText: String,
    onJournalTextChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Journal",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Use this for AA meeting notes, activities " +
                            "that caused pain, progress, or anything " +
                            "else from today.",
                style =
                    MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = journalText,
                onValueChange =
                    onJournalTextChange,
                label = {
                    Text("Today's journal")
                },
                placeholder = {
                    Text(
                        "Example: The meeting was about acceptance. " +
                                "My back hurt more while doing dishes..."
                    )
                },
                minLines = 7,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}