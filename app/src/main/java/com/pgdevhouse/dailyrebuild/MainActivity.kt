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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.MealAmountMode
import androidx.room.withTransaction
import com.pgdevhouse.dailyrebuild.data.local.SavedMeal
import com.pgdevhouse.dailyrebuild.data.local.SavedMealIngredient
import com.pgdevhouse.dailyrebuild.data.local.SavedMealWithIngredients

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

    val mealDao = remember {
        database.mealDao()
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

    var savedProducts by remember {
        mutableStateOf<List<FoodProduct>>(
            emptyList()
        )
    }

    var showSavedFoodsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var savedMeals by remember {
        mutableStateOf<
                List<SavedMealWithIngredients>
                >(
            emptyList()
        )
    }

    var showMealBuilderDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSavedMealsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var isSavingMeal by remember {
        mutableStateOf(false)
    }

    var isAddingSavedMeal by remember {
        mutableStateOf(false)
    }

    var mealBeingEdited by remember {
        mutableStateOf<
            SavedMealWithIngredients?
        >(null)
    }

    var isCreatingFoodForMeal by rememberSaveable {
        mutableStateOf(false)
    }

    var isEditingSavedFood by rememberSaveable {
        mutableStateOf(false)
    }

    var savedFoodToDelete by remember {
        mutableStateOf<FoodProduct?>(null)
    }

    /*
     * Google Code Scanner returns as soon as it decodes one value.
     * On curved, glossy, or wrinkled food packaging that first value
     * can be wrong. Always let the user compare it with the digits
     * printed beneath the bars before performing a lookup.
     */
    var pendingBarcodeText by rememberSaveable {
        mutableStateOf("")
    }

    var pendingScanForMealBuilder by rememberSaveable {
        mutableStateOf(false)
    }

    var showBarcodeVerificationDialog by rememberSaveable {
        mutableStateOf(false)
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

            savedProducts =
                foodDao.getAllProducts()

            savedMeals =
                mealDao.getAllMealsWithIngredients()

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

    fun lookupFoodBarcode(
        barcodeText: String,
        forMealBuilder: Boolean
    ) {
        val verifiedBarcode =
            barcodeText.filter {
                it.isDigit()
            }

        if (
            verifiedBarcode.length != 8 &&
            verifiedBarcode.length != 12 &&
            verifiedBarcode.length != 13
        ) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message =
                        "Enter the 8, 12, or 13 digits printed below the barcode."
                )
            }

            return
        }

        isCreatingFoodForMeal =
            forMealBuilder

        val productIdBeingEdited =
            if (isEditingSavedFood) {
                scannedFoodPrefill?.productId
            } else {
                null
            }

        lastScannedBarcode =
            verifiedBarcode

        coroutineScope.launch {
            try {
                /*
                 * Use an exact locally saved copy first.
                 * FoodDao also handles the normal UPC-A / EAN-13
                 * leading-zero equivalent.
                 */
                val existingProduct =
                    foodDao.getProductByBarcode(
                        verifiedBarcode
                    )

                if (existingProduct != null) {
                    scannedFoodPrefill =
                        ScannedFoodPrefill(
                            productId =
                                productIdBeingEdited
                                    ?: existingProduct.id,

                            barcode =
                                existingProduct.barcode
                                    ?: verifiedBarcode,

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

                            packageQuantity =
                                existingProduct
                                    .packageQuantity,

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
                                    verifiedBarcode
                                )
                    ) {
                        is FoodLookupResult.Found -> {
                            /*
                             * Preserve the barcode the user verified.
                             * Some remote records normalize UPC-A as
                             * EAN-13 and may return a different-looking
                             * leading-zero form.
                             */
                            scannedFoodPrefill =
                                result.food.copy(
                                    productId =
                                        productIdBeingEdited,

                                    barcode =
                                        verifiedBarcode
                                )

                            showManualFoodDialog =
                                true

                            snackbarHostState
                                .showSnackbar(
                                    message =
                                        "Product found."
                                )
                        }

                        FoodLookupResult.NotFound -> {
                            scannedFoodPrefill =
                                ScannedFoodPrefill(
                                    productId =
                                        productIdBeingEdited,

                                    barcode =
                                        verifiedBarcode
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
                                    productId =
                                        productIdBeingEdited,

                                    barcode =
                                        verifiedBarcode
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

    fun startFoodBarcodeScan(
        forMealBuilder: Boolean
    ) {
        isCreatingFoodForMeal =
            forMealBuilder

        if (isScanningBarcode) {
            return
        }

        isScanningBarcode = true

        barcodeScanner
            .startScan()
            .addOnSuccessListener { barcode ->
                val scannedValue =
                    barcode.rawValue
                        ?.filter {
                            it.isDigit()
                        }
                        .orEmpty()

                isScanningBarcode = false

                if (scannedValue.isBlank()) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message =
                                "The barcode could not be read."
                        )
                    }

                    return@addOnSuccessListener
                }

                /*
                 * Do not look it up immediately. The permission-free
                 * Google scanner closes after its first decoded value,
                 * which can be unreliable on curved or wrinkled labels.
                 */
                pendingBarcodeText =
                    scannedValue

                pendingScanForMealBuilder =
                    forMealBuilder

                showBarcodeVerificationDialog =
                    true
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
                 * Main food card from FoodUi.kt.
                 */
                FoodSection(
                    entries =
                        foodEntries,

                    savedFoodCount =
                        savedProducts.size,

                    savedMealCount =
                        savedMeals.size,

                    lastScannedBarcode =
                        lastScannedBarcode,

                    isScanningBarcode =
                        isScanningBarcode,

                    onScanFood = {
                        startFoodBarcodeScan(
                            forMealBuilder = false
                        )
                    },

                    onAddFoodManually = {
                        isCreatingFoodForMeal = false
                        scannedFoodPrefill = null
                        showManualFoodDialog = true
                    },
                    onOpenSavedFoods = {
                        coroutineScope.launch {
                            try {
                                savedProducts =
                                    foodDao.getAllProducts()

                                showSavedFoodsDialog = true
                            } catch (
                                exception: Exception
                            ) {
                                snackbarHostState.showSnackbar(
                                    message =
                                        "Could not load saved foods."
                                )
                            }
                        }
                    },

                    onBuildMeal = {
                        mealBeingEdited = null

                        coroutineScope.launch {
                            try {
                                savedProducts =
                                    foodDao.getAllProducts()

                                if (savedProducts.isEmpty()) {
                                    snackbarHostState.showSnackbar(
                                        message =
                                            "Create or scan a Saved Food first."
                                    )
                                } else {
                                    showMealBuilderDialog = true
                                }
                            } catch (
                                exception: Exception
                            ) {
                                snackbarHostState.showSnackbar(
                                    message =
                                        "Could not open the Meal Builder."
                                )
                            }
                        }
                    },

                    onOpenSavedMeals = {
                        coroutineScope.launch {
                            try {
                                savedMeals =
                                    mealDao
                                        .getAllMealsWithIngredients()

                                savedProducts =
                                    foodDao.getAllProducts()

                                showSavedMealsDialog = true
                            } catch (
                                exception: Exception
                            ) {
                                snackbarHostState.showSnackbar(
                                    message =
                                        "Could not load saved meals."
                                )
                            }
                        }
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

    if (showBarcodeVerificationDialog) {
        AlertDialog(
            onDismissRequest = {
                showBarcodeVerificationDialog =
                    false
            },

            title = {
                Text("Verify Barcode")
            },

            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text =
                            "Compare this with the digits printed " +
                                "under the barcode. Correct any digits " +
                                "before looking up the food."
                    )

                    OutlinedTextField(
                        value =
                            pendingBarcodeText,

                        onValueChange = { newValue ->
                            pendingBarcodeText =
                                newValue.filter {
                                    it.isDigit()
                                }
                        },

                        label = {
                            Text("Printed barcode digits")
                        },

                        supportingText = {
                            Text(
                                "Food barcodes normally contain " +
                                    "8, 12, or 13 digits. " +
                                    "This one has ${pendingBarcodeText.length} digits."
                            )
                        },

                        singleLine = true,

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {
                TextButton(
                    enabled =
                        pendingBarcodeText.length == 8 ||
                            pendingBarcodeText.length == 12 ||
                            pendingBarcodeText.length == 13,

                    onClick = {
                        val verifiedBarcode =
                            pendingBarcodeText

                        val forMealBuilder =
                            pendingScanForMealBuilder

                        showBarcodeVerificationDialog =
                            false

                        lookupFoodBarcode(
                            barcodeText =
                                verifiedBarcode,

                            forMealBuilder =
                                forMealBuilder
                        )
                    }
                ) {
                    Text("Look Up")
                }
            },

            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showBarcodeVerificationDialog =
                                false
                        }
                    ) {
                        Text("Cancel")
                    }

                    TextButton(
                        onClick = {
                            val forMealBuilder =
                                pendingScanForMealBuilder

                            showBarcodeVerificationDialog =
                                false

                            startFoodBarcodeScan(
                                forMealBuilder =
                                    forMealBuilder
                            )
                        }
                    ) {
                        Text("Scan Again")
                    }
                }
            }
        )
    }

    if (showSavedMealsDialog) {
        SavedMealsDialog(
            meals = savedMeals,

            products = savedProducts,

            isAddingMeal = isAddingSavedMeal,

            onAddToToday = { savedMeal, multiplier ->
                coroutineScope.launch {
                    isAddingSavedMeal = true

                    try {
                        val productsById =
                            savedProducts.associateBy {
                                it.id
                            }

                        val entriesToAdd =
                            savedMeal.ingredients
                                .sortedBy {
                                    it.sortOrder
                                }
                                .map { ingredient ->
                                    val product =
                                        productsById[
                                            ingredient.productId
                                        ] ?: error(
                                            "Saved meal contains a missing food."
                                        )

                                    val totalAmount =
                                        ingredient.amount * multiplier

                                    val servings =
                                        when (
                                            ingredient.amountMode
                                        ) {
                                            MealAmountMode
                                                .LABEL_SERVINGS -> {
                                                totalAmount
                                            }

                                            else -> {
                                                if (
                                                    product.servingQuantity <= 0.0
                                                ) {
                                                    error(
                                                        "Saved food has an invalid serving size."
                                                    )
                                                }

                                                totalAmount /
                                                    product.servingQuantity
                                            }
                                        }

                                    FoodLogEntry(
                                        date = todayDate,

                                        productId = product.id,

                                        quantity = totalAmount,

                                        unit =
                                            if (
                                                ingredient.amountMode ==
                                                MealAmountMode
                                                    .LABEL_SERVINGS
                                            ) {
                                                "servings"
                                            } else {
                                                product.servingUnit
                                            },

                                        mealName =
                                            savedMeal.meal.name,

                                        productNameSnapshot =
                                            product.name,

                                        calories =
                                            product
                                                .caloriesPerServing *
                                                servings,

                                        proteinGrams =
                                            product
                                                .proteinGramsPerServing *
                                                servings,

                                        carbohydrateGrams =
                                            product
                                                .carbohydrateGramsPerServing *
                                                servings,

                                        fatGrams =
                                            product
                                                .fatGramsPerServing *
                                                servings,

                                        sodiumMilligrams =
                                            product
                                                .sodiumMilligramsPerServing *
                                                servings
                                    )
                                }

                        if (entriesToAdd.isEmpty()) {
                            error(
                                "Saved meal has no ingredients."
                            )
                        }

                        database.withTransaction {
                            entriesToAdd.forEach { entry ->
                                foodDao.addFoodEntry(
                                    entry
                                )
                            }
                        }

                        foodEntries =
                            foodDao.getEntriesForDate(
                                todayDate
                            )

                        foodRecorded = true
                        showSavedMealsDialog = false

                        snackbarHostState.showSnackbar(
                            message =
                                "${savedMeal.meal.name} added to today."
                        )
                    } catch (
                        exception: Exception
                    ) {
                        snackbarHostState.showSnackbar(
                            message =
                                "Could not add saved meal."
                        )
                    } finally {
                        isAddingSavedMeal = false
                    }
                }
            },

            onEdit = { savedMeal ->
                mealBeingEdited = savedMeal
                showSavedMealsDialog = false
                showMealBuilderDialog = true
            },

            onDelete = { savedMeal ->
                coroutineScope.launch {
                    try {
                        mealDao.deleteMeal(
                            savedMeal.meal.id
                        )

                        savedMeals =
                            mealDao
                                .getAllMealsWithIngredients()

                        snackbarHostState.showSnackbar(
                            message =
                                "Saved meal deleted."
                        )
                    } catch (
                        exception: Exception
                    ) {
                        snackbarHostState.showSnackbar(
                            message =
                                "Could not delete saved meal."
                        )
                    }
                }
            },

            onDismiss = {
                if (!isAddingSavedMeal) {
                    showSavedMealsDialog = false
                }
            }
        )
    }

    if (showMealBuilderDialog) {
        MealBuilderDialog(
            products = savedProducts,

            initialMeal =
                mealBeingEdited,

            isSaving = isSavingMeal,

            onCreateFood = {
                /*
                 * Keep the meal builder open underneath the
                 * food form so the unfinished meal is preserved.
                 */
                isCreatingFoodForMeal = true
                scannedFoodPrefill = null
                showManualFoodDialog = true
            },

            onDismiss = {
                if (!isSavingMeal) {
                    showMealBuilderDialog = false
                    mealBeingEdited = null
                }
            },

            onSave = { draft ->
                coroutineScope.launch {
                    isSavingMeal = true

                    try {
                        val editingMeal =
                            mealBeingEdited

                        val wasEditing =
                            editingMeal != null

                        database.withTransaction {
                            val mealId =
                                if (editingMeal == null) {
                                    mealDao.addMeal(
                                        SavedMeal(
                                            name =
                                                draft.name,

                                            isFavorite =
                                                draft.isFavorite
                                        )
                                    )
                                } else {
                                    mealDao.updateMeal(
                                        editingMeal.meal.copy(
                                            name =
                                                draft.name,

                                            isFavorite =
                                                draft.isFavorite,

                                            updatedAt =
                                                System.currentTimeMillis()
                                        )
                                    )

                                    mealDao
                                        .deleteIngredientsForMeal(
                                            editingMeal.meal.id
                                        )

                                    editingMeal.meal.id
                                }

                            val ingredientRecords =
                                draft.ingredients
                                    .mapIndexed {
                                            index,
                                            ingredient ->

                                        SavedMealIngredient(
                                            mealId =
                                                mealId,

                                            productId =
                                                ingredient
                                                    .product
                                                    .id,

                                            amountMode =
                                                ingredient
                                                    .amountMode,

                                            amount =
                                                ingredient
                                                    .amount,

                                            sortOrder =
                                                index
                                        )
                                    }

                            mealDao.addIngredients(
                                ingredientRecords
                            )
                        }

                        savedMeals =
                            mealDao
                                .getAllMealsWithIngredients()

                        showMealBuilderDialog =
                            false

                        mealBeingEdited = null

                        snackbarHostState.showSnackbar(
                            message =
                                if (wasEditing) {
                                    "Meal updated."
                                } else {
                                    "Meal saved."
                                }
                        )
                    } catch (
                        exception: Exception
                    ) {
                        snackbarHostState.showSnackbar(
                            message =
                                "Could not save meal."
                        )
                    } finally {
                        isSavingMeal = false
                    }
                }
            }
        )
    }

    if (showSavedFoodsDialog) {
        SavedFoodsDialog(
            products = savedProducts,

            onDismiss = {
                showSavedFoodsDialog = false
            },

            onUseProduct = { product ->
                isCreatingFoodForMeal = false
                isEditingSavedFood = false

                scannedFoodPrefill =
                    product.toFoodPrefill()

                showSavedFoodsDialog = false
                showManualFoodDialog = true
            },

            onEditProduct = { product ->
                isCreatingFoodForMeal = false
                isEditingSavedFood = true

                scannedFoodPrefill =
                    product.toFoodPrefill()

                showSavedFoodsDialog = false
                showManualFoodDialog = true
            },

            onDeleteProduct = { product ->
                coroutineScope.launch {
                    val mealsUsingProduct =
                        savedMeals.filter { savedMeal ->
                            savedMeal.ingredients.any { ingredient ->
                                ingredient.productId == product.id
                            }
                        }

                    if (mealsUsingProduct.isNotEmpty()) {
                        val mealNames =
                            mealsUsingProduct
                                .map {
                                    it.meal.name
                                }
                                .distinct()
                                .joinToString(", ")

                        snackbarHostState.showSnackbar(
                            message =
                                "Cannot delete ${product.name}. " +
                                    "It is used by: $mealNames."
                        )
                    } else {
                        savedFoodToDelete = product
                    }
                }
            }
        )
    }

    savedFoodToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = {
                savedFoodToDelete = null
            },

            title = {
                Text("Delete Saved Food?")
            },

            text = {
                Text(
                    text =
                        "${product.name} will be removed from Saved Foods. " +
                            "Past daily entries will never be deleted."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                foodDao.deleteProductById(
                                    product.id
                                )

                                savedProducts =
                                    foodDao.getAllProducts()

                                savedFoodToDelete = null

                                snackbarHostState.showSnackbar(
                                    message =
                                        "Saved food deleted."
                                )
                            } catch (
                                exception: Exception
                            ) {
                                savedFoodToDelete = null

                                snackbarHostState.showSnackbar(
                                    message =
                                        "Cannot delete ${product.name} because " +
                                            "it is used by a current or past food entry."
                                )
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        savedFoodToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showManualFoodDialog) {
        ManualFoodDialog(
            initialFood = scannedFoodPrefill,

            productOnlyMode =
                isCreatingFoodForMeal ||
                    isEditingSavedFood,

            dialogTitle =
                if (isEditingSavedFood) {
                    "Edit Saved Food"
                } else if (isCreatingFoodForMeal) {
                    "Create Saved Food"
                } else {
                    null
                },

            isScanningBarcode =
                isScanningBarcode,

            onScanBarcode = {
                startFoodBarcodeScan(
                    forMealBuilder =
                        isCreatingFoodForMeal
                )
            },

            isSaving = isAddingFood,

            onDismiss = {
                if (!isAddingFood) {
                    val returnToSavedFoods =
                        isEditingSavedFood

                    showManualFoodDialog = false
                    scannedFoodPrefill = null
                    isCreatingFoodForMeal = false
                    isEditingSavedFood = false

                    if (returnToSavedFoods) {
                        showSavedFoodsDialog = true
                    }
                }
            },

            onSave = { draft ->
                coroutineScope.launch {
                    isAddingFood = true

                    try {
                        val existingProduct =
                            if (draft.product.id > 0L) {
                                foodDao.getProductById(
                                    draft.product.id
                                )
                            } else {
                                draft.product.barcode
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let { barcode ->
                                        foodDao.getProductByBarcode(
                                            barcode
                                        )
                                    }
                            }
                        val productId =
                            if (existingProduct == null) {
                                foodDao.addProduct(
                                    draft.product.copy(
                                        id = 0
                                    )
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

                        savedProducts =
                            foodDao.getAllProducts()

                        if (isEditingSavedFood) {
                            showManualFoodDialog = false
                            scannedFoodPrefill = null
                            isEditingSavedFood = false
                            showSavedFoodsDialog = true

                            snackbarHostState
                                .showSnackbar(
                                    message =
                                        "Saved food updated."
                                )

                            return@launch
                        }

                        if (isCreatingFoodForMeal) {
                            showManualFoodDialog = false
                            scannedFoodPrefill = null
                            isCreatingFoodForMeal = false

                            snackbarHostState
                                .showSnackbar(
                                    message =
                                        "Saved food created. " +
                                            "Choose it as a meal ingredient."
                                )

                            return@launch
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
                                    if (
                                        isEditingSavedFood ||
                                        isCreatingFoodForMeal
                                    ) {
                                        "Could not save food. The barcode may " +
                                            "already belong to another saved food."
                                    } else {
                                        "Could not add food."
                                    }
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
                            "24 oz plain water",
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
                            "24 oz MiO water",
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