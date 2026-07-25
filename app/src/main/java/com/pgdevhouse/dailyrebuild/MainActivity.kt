package com.pgdevhouse.dailyrebuild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.pgdevhouse.dailyrebuild.data.local.DailyActivitySnapshot
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
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

    private val healthRefreshToken =
        mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyRebuildAppTheme {
                DailyRebuildApp(
                    healthRefreshToken =
                        healthRefreshToken.intValue
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /*
         * Recheck Health Connect after returning from its
         * permissions or settings screen.
         */
        healthRefreshToken.intValue++
    }
}

@Composable
fun DailyRebuildApp(
    healthRefreshToken: Int = 0
) {
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

    val dailyActivityDao = remember {
        database.dailyActivityDao()
    }

    val healthConnectManager = remember(
        context
    ) {
        HealthConnectManager(context)
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
     * Health Connect activity.
     *
     * The dashboard shows the current live totals when permission is
     * available. Save Today stores a separate Daily Rebuild snapshot.
     */
    var healthAvailability by remember {
        mutableStateOf(
            healthConnectManager.getAvailability()
        )
    }

    var hasHealthPermissions by remember {
        mutableStateOf(false)
    }

    var isLoadingHealthActivity by remember {
        mutableStateOf(false)
    }

    var hasLiveHealthActivity by remember {
        mutableStateOf(false)
    }

    var liveHealthActivity by remember {
        mutableStateOf(
            HealthActivityData()
        )
    }

    var savedActivitySnapshot by remember {
        mutableStateOf<DailyActivitySnapshot?>(
            null
        )
    }

    fun refreshHealthActivity(
        showFeedback: Boolean = false
    ) {
        coroutineScope.launch {
            healthAvailability =
                healthConnectManager.getAvailability()

            if (
                healthAvailability !=
                    HealthConnectAvailability.AVAILABLE
            ) {
                hasHealthPermissions = false
                hasLiveHealthActivity = false
                return@launch
            }

            isLoadingHealthActivity = true

            try {
                hasHealthPermissions =
                    healthConnectManager
                        .hasAllPermissions()

                if (hasHealthPermissions) {
                    liveHealthActivity =
                        healthConnectManager
                            .readTodayActivity()

                    hasLiveHealthActivity = true

                    if (showFeedback) {
                        snackbarHostState.showSnackbar(
                            message =
                                "Activity refreshed."
                        )
                    }
                } else {
                    hasLiveHealthActivity = false
                }
            } catch (
                exception: Exception
            ) {
                hasLiveHealthActivity = false

                if (showFeedback) {
                    snackbarHostState.showSnackbar(
                        message =
                            "Could not refresh activity."
                    )
                }
            } finally {
                isLoadingHealthActivity = false
            }
        }
    }

    val healthPermissionsLauncher =
        rememberLauncherForActivityResult(
            contract =
                PermissionController
                    .createRequestPermissionResultContract()
        ) { grantedPermissions ->
            if (
                grantedPermissions.containsAll(
                    HealthConnectManager.permissions
                )
            ) {
                refreshHealthActivity(
                    showFeedback = true
                )
            } else {
                hasHealthPermissions = false
                hasLiveHealthActivity = false

                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message =
                            "Activity permission was not granted."
                    )
                }
            }
        }

    /*
     * Read-only daily history.
     *
     * The first version scans the most recent 365 dates by using the
     * existing date-based DAO methods. This avoids a schema change and
     * preserves every existing record.
     */
    var showDailyHistoryDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var isLoadingDailyHistory by remember {
        mutableStateOf(false)
    }

    var isDeletingDailyHistoryDay by remember {
        mutableStateOf(false)
    }

    var dailyHistoryDays by remember {
        mutableStateOf<List<DailyHistoryDay>>(
            emptyList()
        )
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

            savedActivitySnapshot =
                dailyActivityDao.getSnapshotByDate(
                    todayDate
                )

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

    LaunchedEffect(
        healthRefreshToken
    ) {
        refreshHealthActivity()
    }

    val completedTasks = listOf(
        foodRecorded,
        walkCompleted,
        painRecorded,
        mobilityCompleted
    ).count { it }

    val progressPercent =
        completedTasks * 25

    val totalWaterOunces =
        (plainReusableBottleCount + mioReusableBottleCount) * 24.0 +
            (plainDisposableBottleCount + mioDisposableBottleCount) * 16.9

    val totalCaloriesToday =
        foodEntries.sumOf { it.calories }

    val displayedActivity =
        when {
            hasLiveHealthActivity ->
                liveHealthActivity

            savedActivitySnapshot != null ->
                HealthActivityData(
                    steps =
                        savedActivitySnapshot
                            ?.steps ?: 0L,
                    distanceMiles =
                        savedActivitySnapshot
                            ?.distanceMiles ?: 0.0,
                    activeCalories =
                        savedActivitySnapshot
                            ?.activeCalories ?: 0.0
                )

            else ->
                HealthActivityData()
        }

    val activitySourceLabel =
        when {
            hasLiveHealthActivity ->
                "Live from Health Connect"

            savedActivitySnapshot != null ->
                "Saved with today's record"

            else ->
                null
        }

    fun openDailyHistory() {
        showDailyHistoryDialog = true
        isLoadingDailyHistory = true

        coroutineScope.launch {
            try {
                val loadedDays =
                    mutableListOf<DailyHistoryDay>()

                val today = LocalDate.now()

                for (dayOffset in 0L until 365L) {
                    val date =
                        today
                            .minusDays(dayOffset)
                            .toString()

                    val record =
                        dailyRecordDao.getRecordByDate(
                            date
                        )

                    val entries =
                        foodDao.getEntriesForDate(
                            date
                        )

                    val activitySnapshot =
                        dailyActivityDao
                            .getSnapshotByDate(
                                date
                            )

                    if (
                        record != null ||
                        entries.isNotEmpty() ||
                        activitySnapshot != null
                    ) {
                        loadedDays.add(
                            DailyHistoryDay(
                                date = date,
                                record = record,
                                foodEntries = entries,
                                activitySnapshot =
                                    activitySnapshot
                            )
                        )
                    }
                }

                dailyHistoryDays = loadedDays
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message =
                        "Could not load daily history."
                )
            } finally {
                isLoadingDailyHistory = false
            }
        }
    }

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

    val saveToday: () -> Unit = {
        coroutineScope.launch {
            isSaving = true

            try {
                val record =
                    DailyRecord(
                        date = todayDate,
                        foodRecorded = foodRecorded,
                        walkCompleted = walkCompleted,
                        painRecorded = painRecorded,
                        mobilityCompleted = mobilityCompleted,
                        backPain = backPain,
                        shinPain = shinPain,
                        plainReusableBottleCount = plainReusableBottleCount,
                        mioReusableBottleCount = mioReusableBottleCount,
                        plainDisposableBottleCount = plainDisposableBottleCount,
                        mioDisposableBottleCount = mioDisposableBottleCount,
                        morningAspirinTaken = morningAspirinTaken,
                        morningIbuprofenTaken = morningIbuprofenTaken,
                        morningNaproxenTaken = morningNaproxenTaken,
                        morningAcetaminophenTaken = morningAcetaminophenTaken,
                        nightIbuprofenTaken = nightIbuprofenTaken,
                        nightNaproxenTaken = nightNaproxenTaken,
                        nightAcetaminophenTaken = nightAcetaminophenTaken,
                        journalText = journalText,
                        updatedAt = System.currentTimeMillis()
                    )

                val snapshotToSave =
                    when {
                        hasHealthPermissions &&
                            hasLiveHealthActivity ->
                            DailyActivitySnapshot(
                                date = todayDate,
                                steps =
                                    liveHealthActivity
                                        .steps,
                                distanceMiles =
                                    liveHealthActivity
                                        .distanceMiles,
                                activeCalories =
                                    liveHealthActivity
                                        .activeCalories,
                                updatedAt =
                                    System.currentTimeMillis()
                            )

                        savedActivitySnapshot != null ->
                            savedActivitySnapshot
                                ?.copy(
                                    updatedAt =
                                        System.currentTimeMillis()
                                )

                        else ->
                            null
                    }

                database.withTransaction {
                    dailyRecordDao.saveRecord(
                        record
                    )

                    snapshotToSave?.let {
                        dailyActivityDao
                            .saveSnapshot(it)
                    }
                }

                savedActivitySnapshot =
                    snapshotToSave

                snackbarHostState.showSnackbar(
                    message = "Today saved."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not save today."
                )
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isLoading) {
                DailyActionBar(
                    isSaving = isSaving,
                    onSave = saveToday,
                    onOpenHistory = {
                        openDailyHistory()
                    }
                )
            }
        },
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
                    completedTasks = completedTasks,
                    progressPercent = progressPercent,
                    waterOunces = totalWaterOunces,
                    calories = totalCaloriesToday,
                    backPain = backPain
                )

                ActivitySection(
                    availability = healthAvailability,
                    hasPermissions = hasHealthPermissions,
                    isLoading = isLoadingHealthActivity,
                    activity = displayedActivity,
                    sourceLabel = activitySourceLabel,
                    onConnect = {
                        healthPermissionsLauncher.launch(
                            HealthConnectManager.permissions
                        )
                    },
                    onRefresh = {
                        refreshHealthActivity(
                            showFeedback = true
                        )
                    },
                    onManageAccess = {
                        healthConnectManager
                            .openHealthConnectSettings()
                    },
                    onInstallOrUpdate = {
                        healthConnectManager
                            .openInstallOrUpdate()
                    }
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
                    },

                    onDeleteMealLog = { mealLogId ->
                        coroutineScope.launch {
                            try {
                                foodDao
                                    .deleteFoodEntriesByMealLogId(
                                        mealLogId
                                    )

                                foodEntries =
                                    foodDao
                                        .getEntriesForDate(
                                            todayDate
                                        )

                                snackbarHostState
                                    .showSnackbar(
                                        message =
                                            "Meal deleted from today."
                                    )
                            } catch (
                                exception: Exception
                            ) {
                                snackbarHostState
                                    .showSnackbar(
                                        message =
                                            "Could not delete meal."
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

                Text(
                    text = "Your changes stay editable until you save. Use the action bar below whenever today is ready.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
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

                        /*
                         * Every press of Add to Today gets a different ID.
                         * Two PBJ additions therefore remain separate groups.
                         */
                        val mealLogId =
                            UUID.randomUUID().toString()

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

                                        mealLogId =
                                            mealLogId,

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

    if (showDailyHistoryDialog) {
        DailyHistoryDialog(
            days = dailyHistoryDays,
            isLoading = isLoadingDailyHistory,
            isDeletingDay =
                isDeletingDailyHistoryDay,

            onDeleteDay = { day ->
                coroutineScope.launch {
                    isDeletingDailyHistoryDay = true

                    try {
                        database.withTransaction {
                            day.foodEntries.forEach { entry ->
                                foodDao.deleteFoodEntryById(
                                    entry.id
                                )
                            }

                            day.record?.let { record ->
                                dailyRecordDao.deleteRecord(
                                    record
                                )
                            }

                            day.activitySnapshot
                                ?.let { snapshot ->
                                    dailyActivityDao
                                        .deleteSnapshot(
                                            snapshot
                                        )
                                }
                        }

                        dailyHistoryDays =
                            dailyHistoryDays.filterNot {
                                it.date == day.date
                            }

                        if (day.date == todayDate) {
                            foodEntries = emptyList()

                            foodRecorded = false
                            walkCompleted = false
                            painRecorded = false
                            mobilityCompleted = false

                            backPain = 0f
                            shinPain = 0f

                            nextBottleHasMio = false
                            plainReusableBottleCount = 0
                            mioReusableBottleCount = 0
                            plainDisposableBottleCount = 0
                            mioDisposableBottleCount = 0

                            morningAspirinTaken = true
                            morningIbuprofenTaken = true
                            morningNaproxenTaken = true
                            morningAcetaminophenTaken = true

                            nightIbuprofenTaken = true
                            nightNaproxenTaken = true
                            nightAcetaminophenTaken = true

                            journalText = ""
                            savedActivitySnapshot = null
                        }

                        snackbarHostState.showSnackbar(
                            message = "Entire day deleted."
                        )
                    } catch (
                        exception: Exception
                    ) {
                        snackbarHostState.showSnackbar(
                            message =
                                "Could not delete the entire day."
                        )
                    } finally {
                        isDeletingDailyHistoryDay = false
                    }
                }
            },

            onDismiss = {
                if (!isDeletingDailyHistoryDay) {
                    showDailyHistoryDialog = false
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
private fun DailyActionBar(
    isSaving: Boolean,
    onSave: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RebuildSecondaryAction(
                text = "Calendar",
                onClick = onOpenHistory,
                modifier = Modifier.weight(0.42f)
            )
            RebuildPrimaryAction(
                text = if (isSaving) "Saving…" else "Save Today",
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.weight(0.58f)
            )
        }
    }
}

@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        RebuildInsetPanel(
            modifier = Modifier.padding(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    strokeWidth = 4.dp
                )

                Column {
                    Text(
                        text = "Preparing your day",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Loading your latest progress and entries…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = RebuildNavy,
        contentColor = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Daily Rebuild",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            RebuildStatusBadge(
                text = today.format(dayFormatter),
                backgroundColor = Color.White.copy(alpha = 0.14f),
                contentColor = Color.White
            )

            Text(
                text = today.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.86f)
            )

            Text(
                text = "Small actions count. Record what happened, keep what helps, and build from there.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun ProgressSection(
    completedTasks: Int,
    progressPercent: Int,
    waterOunces: Double,
    calories: Double,
    backPain: Float
) {
    RebuildSectionCard(
        title = "Today at a glance",
        subtitle = "A quick snapshot of the information you have recorded.",
        accentColor = RebuildTeal
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            RebuildProgressRing(
                progress = progressPercent / 100f,
                centerText = "$progressPercent%"
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$completedTasks of 4 daily anchors complete",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (completedTasks) {
                        4 -> "All four anchors are recorded. Nice work."
                        0 -> "Start anywhere. One completed anchor changes the day."
                        else -> "You are building momentum—keep going."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "water",
                value = "${formatOunces(waterOunces)} oz",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primaryContainer
            )
            RebuildMetricPill(
                label = "calories",
                value = calories.toInt().toString(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            RebuildMetricPill(
                label = "back pain",
                value = "${backPain.toInt()}/10",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun ActivitySection(
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    isLoading: Boolean,
    activity: HealthActivityData,
    sourceLabel: String?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onManageAccess: () -> Unit,
    onInstallOrUpdate: () -> Unit
) {
    RebuildSectionCard(
        title = "Today's activity",
        subtitle =
            "Steps, distance, and active calories from Health Connect.",
        accentColor = RebuildBlue
    ) {
        when (availability) {
            HealthConnectAvailability.AVAILABLE -> {
                if (
                    hasPermissions ||
                    sourceLabel != null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        RebuildMetricPill(
                            label = "steps",
                            value =
                                String.format(
                                    Locale.US,
                                    "%,d",
                                    activity.steps
                                ),
                            modifier = Modifier.weight(1f),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                        )

                        RebuildMetricPill(
                            label = "miles",
                            value =
                                formatActivityMiles(
                                    activity.distanceMiles
                                ),
                            modifier = Modifier.weight(1f),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .secondaryContainer,
                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSecondaryContainer
                        )

                        RebuildMetricPill(
                            label = "active cal",
                            value =
                                activity
                                    .activeCalories
                                    .toInt()
                                    .toString(),
                            modifier = Modifier.weight(1f),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .tertiaryContainer,
                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onTertiaryContainer
                        )
                    }

                    sourceLabel?.let {
                        Text(
                            text = it,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        RebuildSecondaryAction(
                            text =
                                if (isLoading) {
                                    "Refreshing…"
                                } else if (
                                    hasPermissions
                                ) {
                                    "Refresh"
                                } else {
                                    "Reconnect"
                                },
                            onClick =
                                if (hasPermissions) {
                                    onRefresh
                                } else {
                                    onConnect
                                },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        )

                        RebuildSecondaryAction(
                            text = "Manage access",
                            onClick = onManageAccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text =
                            "Connect Health Connect to show activity " +
                                "recorded by Google Fit, your phone, " +
                                "or a compatible wearable.",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    RebuildPrimaryAction(
                        text = "Connect Health Connect",
                        onClick = onConnect,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HealthConnectAvailability.UPDATE_REQUIRED -> {
                Text(
                    text =
                        "Health Connect must be installed or updated " +
                            "before Daily Rebuild can read activity.",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                RebuildPrimaryAction(
                    text = "Install or update",
                    onClick = onInstallOrUpdate,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HealthConnectAvailability.UNAVAILABLE -> {
                Text(
                    text =
                        "Health Connect is not available on this device. " +
                            "Daily Rebuild will continue working without it.",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }

        Text(
            text =
                "Daily Rebuild has read-only access. Save Today stores " +
                    "a separate snapshot for your calendar and future stats.",
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

private fun formatActivityMiles(
    miles: Double
): String {
    return String.format(
        Locale.US,
        "%.2f",
        miles
    )
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
    RebuildSectionCard(
        title = "Daily anchors",
        subtitle = "Four simple checks keep the day visible and measurable.",
        accentColor = RebuildBlue
    ) {
        TaskCheckbox(
            title = "Food and water recorded",
            supportingText = "Log meals, snacks, and hydration.",
            checked = foodRecorded,
            onCheckedChange = onFoodRecordedChange
        )
        TaskCheckbox(
            title = "Walk or intentional movement",
            supportingText = "Count what was realistic for today.",
            checked = walkCompleted,
            onCheckedChange = onWalkCompletedChange
        )
        TaskCheckbox(
            title = "Pain levels recorded",
            supportingText = "Capture back and shin pain honestly.",
            checked = painRecorded,
            onCheckedChange = onPainRecordedChange
        )
        TaskCheckbox(
            title = "Mobility or stretching",
            supportingText = "Small mobility sessions still count.",
            checked = mobilityCompleted,
            onCheckedChange = onMobilityCompletedChange
        )
    }
}

@Composable
private fun TaskCheckbox(
    title: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(18.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RebuildStatusBadge(
                text = if (checked) "Done" else "Open",
                backgroundColor = if (checked) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (checked) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
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
        plainReusableBottleCount + mioReusableBottleCount
    val disposableBottleTotal =
        plainDisposableBottleCount + mioDisposableBottleCount
    val totalOunces =
        reusableBottleTotal * 24.0 +
            disposableBottleTotal * 16.9

    RebuildSectionCard(
        title = "Hydration",
        subtitle = "Quick-add your usual bottles and keep the total visible.",
        accentColor = RebuildTeal,
        trailing = {
            RebuildStatusBadge(
                text = "${formatOunces(totalOunces)} oz"
            )
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable {
                    onNextBottleHasMioChange(!nextBottleHasMio)
                },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = nextBottleHasMio,
                    onCheckedChange = onNextBottleHasMioChange
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Next bottle includes MiO",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "This changes how the next quick-add is labeled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RebuildPrimaryAction(
                text = "+ 24 oz bottle",
                onClick = onAddReusableBottle,
                modifier = Modifier.weight(1f)
            )
            RebuildSecondaryAction(
                text = "+ 16.9 oz bottle",
                onClick = onAddDisposableBottle,
                modifier = Modifier.weight(1f)
            )
        }

        if (reusableBottleTotal == 0 && disposableBottleTotal == 0) {
            RebuildInsetPanel {
                Text(
                    text = "No water recorded yet.",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Use either quick-add button when you finish a bottle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Today’s bottles",
                    style = MaterialTheme.typography.titleMedium
                )
                if (plainReusableBottleCount > 0) {
                    WaterCountRow(
                        label = "24 oz plain water",
                        count = plainReusableBottleCount,
                        onRemoveOne = onRemovePlainReusableBottle
                    )
                }
                if (mioReusableBottleCount > 0) {
                    WaterCountRow(
                        label = "24 oz MiO water",
                        count = mioReusableBottleCount,
                        onRemoveOne = onRemoveMioReusableBottle
                    )
                }
                if (plainDisposableBottleCount > 0) {
                    WaterCountRow(
                        label = "16.9 oz plain water",
                        count = plainDisposableBottleCount,
                        onRemoveOne = onRemovePlainDisposableBottle
                    )
                }
                if (mioDisposableBottleCount > 0) {
                    WaterCountRow(
                        label = "16.9 oz MiO water",
                        count = mioDisposableBottleCount,
                        onRemoveOne = onRemoveMioDisposableBottle
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RebuildStatusBadge(
                text = "× $count",
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onRemoveOne) {
                Text("Remove")
            }
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
    RebuildSectionCard(
        title = "Pain check-in",
        subtitle = "Record the number that best matches how it feels right now.",
        accentColor = RebuildAmber
    ) {
        PainSlider(
            label = "Lower back pain",
            painValue = backPain,
            onPainValueChange = onBackPainChange
        )
        PainSlider(
            label = "Shin pain",
            painValue = shinPain,
            onPainValueChange = onShinPainChange
        )
    }
}

@Composable
private fun PainSlider(
    label: String,
    painValue: Float,
    onPainValueChange: (Float) -> Unit
) {
    val badgeColor = when {
        painValue < 4f -> MaterialTheme.colorScheme.secondaryContainer
        painValue < 7f -> Color(0xFFFFEDC2)
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val badgeContentColor = when {
        painValue < 4f -> MaterialTheme.colorScheme.onSecondaryContainer
        painValue < 7f -> Color(0xFF604400)
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    RebuildInsetPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when {
                        painValue == 0f -> "No pain recorded"
                        painValue < 4f -> "Mild"
                        painValue < 7f -> "Moderate"
                        else -> "High"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RebuildStatusBadge(
                text = "${painValue.toInt()} / 10",
                backgroundColor = badgeColor,
                contentColor = badgeContentColor
            )
        }

        Slider(
            value = painValue,
            onValueChange = onPainValueChange,
            valueRange = 0f..10f,
            steps = 9
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "No pain",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Worst pain",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    RebuildSectionCard(
        title = "Pain relievers",
        subtitle = "Your usual organizer is prefilled. Uncheck anything you did not take.",
        accentColor = RebuildGreen
    ) {
        RebuildInsetPanel(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        ) {
            Text(
                text = "Morning / day",
                style = MaterialTheme.typography.titleMedium
            )
            MedicationCheckbox(
                name = "Aspirin", dose = "325 mg",
                checked = morningAspirinTaken,
                onCheckedChange = onMorningAspirinChange
            )
            MedicationCheckbox(
                name = "Ibuprofen", dose = "400 mg — 2 × 200 mg",
                checked = morningIbuprofenTaken,
                onCheckedChange = onMorningIbuprofenChange
            )
            MedicationCheckbox(
                name = "Naproxen sodium", dose = "220 mg",
                checked = morningNaproxenTaken,
                onCheckedChange = onMorningNaproxenChange
            )
            MedicationCheckbox(
                name = "Acetaminophen", dose = "1,000 mg — 2 × 500 mg",
                checked = morningAcetaminophenTaken,
                onCheckedChange = onMorningAcetaminophenChange
            )
        }

        RebuildInsetPanel(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
        ) {
            Text(
                text = "Night",
                style = MaterialTheme.typography.titleMedium
            )
            MedicationCheckbox(
                name = "Ibuprofen", dose = "400 mg — 2 × 200 mg",
                checked = nightIbuprofenTaken,
                onCheckedChange = onNightIbuprofenChange
            )
            MedicationCheckbox(
                name = "Naproxen sodium", dose = "220 mg",
                checked = nightNaproxenTaken,
                onCheckedChange = onNightNaproxenChange
            )
            MedicationCheckbox(
                name = "Acetaminophen", dose = "1,000 mg — 2 × 500 mg",
                checked = nightAcetaminophenTaken,
                onCheckedChange = onNightAcetaminophenChange
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dose,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (checked) "Taken" else "Skipped",
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun JournalSection(
    journalText: String,
    onJournalTextChange: (String) -> Unit
) {
    RebuildSectionCard(
        title = "Daily notes",
        subtitle = "Capture meetings, pain triggers, progress, or anything worth remembering.",
        accentColor = RebuildBlue
    ) {
        OutlinedTextField(
            value = journalText,
            onValueChange = onJournalTextChange,
            label = { Text("What should future-you remember?") },
            placeholder = {
                Text(
                    "Example: The meeting was about acceptance. My back hurt more while doing dishes…"
                )
            },
            minLines = 7,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Saved with the rest of today’s record.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}