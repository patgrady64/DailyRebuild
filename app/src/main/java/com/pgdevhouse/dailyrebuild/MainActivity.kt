package com.pgdevhouse.dailyrebuild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.ShowerLog
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.SavedMeeting
import com.pgdevhouse.dailyrebuild.data.local.CarePlace
import com.pgdevhouse.dailyrebuild.data.local.CareProvider
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurement
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurementType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
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

    val mobilitySessionDao = remember {
        database.mobilitySessionDao()
    }

    val showerLogDao = remember {
        database.showerLogDao()
    }

    val migraineLogDao = remember {
        database.migraineLogDao()
    }

    val meetingDao = remember {
        database.meetingDao()
    }

    val careVisitDao = remember {
        database.careVisitDao()
    }

    val healthProfileDao = remember {
        database.healthProfileDao()
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

    var todayDate by remember {
        mutableStateOf(
            LocalDate.now().toString()
        )
    }

    /*
     * Keep the active day synchronized even when the app remains open
     * across midnight. A second check runs from onResume below so a
     * backgrounded app also switches days immediately when reopened.
     */
    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextDay =
                now.toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(now.zone)
                    .plusSeconds(1)

            val waitMilliseconds =
                Duration.between(
                    now,
                    nextDay
                ).toMillis()
                    .coerceAtLeast(1_000L)

            delay(waitMilliseconds)

            todayDate =
                LocalDate.now().toString()
        }
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

    /*
     * Main navigation and compact Home interactions.
     */
    var selectedMainTab by rememberSaveable {
        mutableIntStateOf(0)
    }

    var showMoreToday by rememberSaveable {
        mutableStateOf(false)
    }

    var expandedTodaySection by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var showQuickWaterDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showQuickPainDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var currentCalorieGoal by remember {
        mutableStateOf<Int?>(null)
    }

    val homeScrollState =
        rememberScrollState()

    BackHandler(
        enabled = selectedMainTab != 0
    ) {
        selectedMainTab = 0

        coroutineScope.launch {
            homeScrollState.animateScrollTo(0)
        }
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

    var mobilitySessionsToday by remember {
        mutableStateOf<List<MobilitySession>>(
            emptyList()
        )
    }

    var isSavingMobility by remember {
        mutableStateOf(false)
    }

    /*
     * Showering is a weekly habit, not a daily anchor.
     * A date can contain at most one shower log.
     */
    var showeredToday by remember {
        mutableStateOf(false)
    }

    var showerDatesThisWeek by remember {
        mutableStateOf<List<String>>(
            emptyList()
        )
    }

    /*
     * Migraine / visual-aura events are occasional health events, not daily
     * anchors. The Health tab always keeps the log button available, while
     * weekly summaries only include this category when an event exists.
     */
    var migraineLogs by remember {
        mutableStateOf<List<MigraineLog>>(
            emptyList()
        )
    }

    var showMigraineLogDialog by rememberSaveable {
        mutableStateOf(false)
    }

    /*
     * Recovery meetings are weekly support events rather than daily anchors.
     * SavedMeeting stores reusable location details; MeetingAttendance stores
     * one historical attendance snapshot.
     */
    var savedMeetings by remember {
        mutableStateOf<List<SavedMeeting>>(emptyList())
    }

    var meetingAttendanceHistory by remember {
        mutableStateOf<List<MeetingAttendance>>(emptyList())
    }

    var showMeetingPickerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showMeetingEditorDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var meetingBeingEdited by remember {
        mutableStateOf<SavedMeeting?>(null)
    }

    var logAttendanceAfterMeetingSave by rememberSaveable {
        mutableStateOf(false)
    }

    var showMeetingAttendanceDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var meetingForAttendance by remember {
        mutableStateOf<SavedMeeting?>(null)
    }

    var attendanceBeingEdited by remember {
        mutableStateOf<MeetingAttendance?>(null)
    }

    var isOneTimeMeetingAttendance by rememberSaveable {
        mutableStateOf(false)
    }

    var showMeetingHistoryDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var meetingAttendancePendingDeletion by remember {
        mutableStateOf<MeetingAttendance?>(null)
    }

    var isSavingMeeting by remember {
        mutableStateOf(false)
    }

    var isDeletingMeetingAttendance by remember {
        mutableStateOf(false)
    }

    /*
     * Completed care visits live in Health and never affect daily anchors.
     * Places and providers are reusable; every visit stores a historical
     * snapshot so future directory edits cannot rewrite the past.
     */
    var carePlaces by remember {
        mutableStateOf<List<CarePlace>>(emptyList())
    }

    var careProviders by remember {
        mutableStateOf<List<CareProvider>>(emptyList())
    }

    var careVisits by remember {
        mutableStateOf<List<CareVisit>>(emptyList())
    }

    var showCareVisitStartDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showCareProviderPickerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showCarePlaceEditorDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showCareProviderEditorDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showCareVisitEditorDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showCareVisitHistoryDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedCarePlaceForVisit by remember {
        mutableStateOf<CarePlace?>(null)
    }

    var selectedCareProviderForVisit by remember {
        mutableStateOf<CareProvider?>(null)
    }

    var carePlaceBeingEdited by remember {
        mutableStateOf<CarePlace?>(null)
    }

    var careProviderBeingEdited by remember {
        mutableStateOf<CareProvider?>(null)
    }

    var careVisitBeingEdited by remember {
        mutableStateOf<CareVisit?>(null)
    }

    var careVisitPendingDeletion by remember {
        mutableStateOf<CareVisit?>(null)
    }

    var continueVisitAfterPlaceSave by rememberSaveable {
        mutableStateOf(false)
    }

    var continueVisitAfterProviderSave by rememberSaveable {
        mutableStateOf(false)
    }

    var returnToCareHistoryAfterVisitSave by rememberSaveable {
        mutableStateOf(false)
    }

    var isOneTimeCareVisit by rememberSaveable {
        mutableStateOf(false)
    }

    var isSavingCareVisit by remember {
        mutableStateOf(false)
    }

    var isDeletingCareVisit by remember {
        mutableStateOf(false)
    }

    var healthFeatureRefreshKey by remember {
        mutableIntStateOf(0)
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
        /*
         * Always begin a newly selected calendar day from clean daily
         * defaults. Without this reset, fields from the previous day
         * remain visible when no DailyRecord exists for the new date.
         * Reusable saved foods and saved meals are intentionally kept.
         */
        isLoading = true

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
        foodEntries = emptyList()
        mobilitySessionsToday = emptyList()
        showeredToday = false
        showerDatesThisWeek = emptyList()
        savedActivitySnapshot = null
        hasLiveHealthActivity = false
        liveHealthActivity = HealthActivityData()

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

            mobilitySessionsToday =
                mobilitySessionDao.getSessionsForDate(
                    todayDate
                )

            if (mobilitySessionsToday.isNotEmpty()) {
                mobilityCompleted = true
            }

            val activeDate =
                LocalDate.parse(todayDate)

            val weekStart =
                activeDate.minusDays(
                    (activeDate.dayOfWeek.value - 1)
                        .toLong()
                )

            val weekEnd =
                weekStart.plusDays(6)

            val weeklyShowerLogs =
                showerLogDao.getLogsBetween(
                    weekStart.toString(),
                    weekEnd.toString()
                )

            showerDatesThisWeek =
                weeklyShowerLogs.map { it.date }

            showeredToday =
                weeklyShowerLogs.any {
                    it.date == todayDate
                }

            migraineLogs =
                migraineLogDao.getAllLogs()

            savedMeetings =
                meetingDao.getActiveMeetings()

            meetingAttendanceHistory =
                meetingDao.getAllAttendance()

            carePlaces =
                careVisitDao.getActivePlaces()

            careProviders =
                careVisitDao.getActiveProviders()

            careVisits =
                careVisitDao.getAllVisits()

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

        /*
         * Health Connect totals are also date-scoped, so refresh them
         * whenever the dashboard advances to a new day.
         */
        refreshHealthActivity()
    }

    LaunchedEffect(
        healthRefreshToken
    ) {
        val currentDeviceDate =
            LocalDate.now().toString()

        if (todayDate != currentDeviceDate) {
            todayDate = currentDeviceDate
        }

        refreshHealthActivity()
    }

    /*
     * Refresh the active calorie goal when Home is opened again after
     * editing Health Profile & Goals in the More tab.
     */
    LaunchedEffect(
        selectedMainTab,
        todayDate
    ) {
        if (selectedMainTab == 0) {
            currentCalorieGoal =
                healthProfileDao
                    .getProfile()
                    ?.currentCalorieGoal
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

    val totalWaterOunces =
        (plainReusableBottleCount + mioReusableBottleCount) * 24.0 +
            (plainDisposableBottleCount + mioDisposableBottleCount) * 16.9

    val totalCaloriesToday =
        foodEntries.sumOf { it.calories }

    val showerCountThisWeek =
        showerDatesThisWeek.size

    val showerIsDueNow =
        !showeredToday &&
            showerCountThisWeek < 2 &&
            runCatching {
                LocalDate.parse(todayDate)
                    .dayOfWeek.value >= 5
            }.getOrDefault(false)

    val meetingWeekStart =
        runCatching {
            val activeDate = LocalDate.parse(todayDate)
            activeDate.minusDays(
                (activeDate.dayOfWeek.value - 1).toLong()
            )
        }.getOrDefault(LocalDate.now())

    val meetingWeekEnd =
        meetingWeekStart.plusDays(6)

    val weeklyMeetingAttendance =
        meetingAttendanceHistory
            .filter {
                it.date >= meetingWeekStart.toString() &&
                    it.date <= meetingWeekEnd.toString()
            }
            .sortedByDescending { it.startedAt }

    val meetingCountThisWeek =
        weeklyMeetingAttendance.size

    val meetingGoalNeedsAttention =
        meetingCountThisWeek < DEFAULT_WEEKLY_MEETING_GOAL &&
            runCatching {
                LocalDate.parse(todayDate)
                    .dayOfWeek.value >= 5
            }.getOrDefault(false)

    val recentMeetingsForPicker =
        remember(
            savedMeetings,
            meetingAttendanceHistory
        ) {
            val lastAttendanceByMeetingId =
                meetingAttendanceHistory
                    .filter { it.savedMeetingId != null }
                    .groupBy { it.savedMeetingId }
                    .mapValues { (_, attendance) ->
                        attendance.maxOf { it.startedAt }
                    }

            savedMeetings.sortedWith(
                compareByDescending<SavedMeeting> {
                    lastAttendanceByMeetingId[it.id]
                        ?: Long.MIN_VALUE
                }.thenByDescending {
                    it.favorite
                }.thenBy {
                    it.name.lowercase(Locale.US)
                }
            )
        }

    val recentCarePlaces =
        remember(carePlaces, careVisits) {
            val lastVisitByPlaceId =
                careVisits
                    .filter { it.placeId != null }
                    .groupBy { it.placeId }
                    .mapValues { (_, visits) ->
                        visits.maxOf { it.startedAt }
                    }

            carePlaces.sortedWith(
                compareByDescending<CarePlace> {
                    lastVisitByPlaceId[it.id]
                        ?: Long.MIN_VALUE
                }.thenBy {
                    it.name.lowercase(Locale.US)
                }
            )
        }

    val providersForSelectedCarePlace =
        remember(
            selectedCarePlaceForVisit,
            careProviders,
            careVisits
        ) {
            val placeId = selectedCarePlaceForVisit?.id
            if (placeId == null) {
                emptyList()
            } else {
                val lastVisitByProviderId =
                    careVisits
                        .filter { it.providerId != null }
                        .groupBy { it.providerId }
                        .mapValues { (_, visits) ->
                            visits.maxOf { it.startedAt }
                        }

                careProviders
                    .filter { it.placeId == placeId }
                    .sortedWith(
                        compareByDescending<CareProvider> {
                            lastVisitByProviderId[it.id]
                                ?: Long.MIN_VALUE
                        }.thenBy {
                            it.name.lowercase(Locale.US)
                        }
                    )
            }
        }

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
                    activityMinutes =
                        savedActivitySnapshot
                            ?.activityMinutes ?: 0L
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

    fun refreshMeetingData() {
        coroutineScope.launch {
            try {
                savedMeetings =
                    meetingDao.getActiveMeetings()

                meetingAttendanceHistory =
                    meetingDao.getAllAttendance()
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not refresh meeting information."
                )
            }
        }
    }

    fun saveMeeting(
        draft: SavedMeetingDraft
    ) {
        coroutineScope.launch {
            isSavingMeeting = true

            try {
                val now = System.currentTimeMillis()
                val existing =
                    draft.id.takeIf { it > 0L }
                        ?.let { meetingDao.getMeetingById(it) }

                val savedId =
                    if (existing == null) {
                        meetingDao.insertMeeting(
                            SavedMeeting(
                                name = draft.name,
                                address = draft.address,
                                city = draft.city,
                                state = draft.state,
                                zipCode = draft.zipCode,
                                typicalDurationMinutes =
                                    draft.typicalDurationMinutes,
                                favorite = draft.favorite,
                                notes = draft.notes,
                                createdAt = draft.createdAt,
                                updatedAt = now
                            )
                        )
                    } else {
                        meetingDao.updateMeeting(
                            existing.copy(
                                name = draft.name,
                                address = draft.address,
                                city = draft.city,
                                state = draft.state,
                                zipCode = draft.zipCode,
                                typicalDurationMinutes =
                                    draft.typicalDurationMinutes,
                                favorite = draft.favorite,
                                notes = draft.notes,
                                updatedAt = now
                            )
                        )
                        existing.id
                    }

                savedMeetings =
                    meetingDao.getActiveMeetings()

                val savedMeeting =
                    meetingDao.getMeetingById(savedId)

                showMeetingEditorDialog = false
                meetingBeingEdited = null

                if (
                    logAttendanceAfterMeetingSave &&
                    savedMeeting != null
                ) {
                    meetingForAttendance = savedMeeting
                    attendanceBeingEdited = null
                    isOneTimeMeetingAttendance = false
                    showMeetingAttendanceDialog = true
                }

                logAttendanceAfterMeetingSave = false

                snackbarHostState.showSnackbar(
                    message =
                        if (existing == null) {
                            "Meeting saved."
                        } else {
                            "Meeting updated."
                        }
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not save the meeting."
                )
            } finally {
                isSavingMeeting = false
            }
        }
    }

    fun saveMeetingAttendance(
        draft: MeetingAttendanceDraft
    ) {
        coroutineScope.launch {
            isSavingMeeting = true

            try {
                val duplicate =
                    meetingDao.findPotentialDuplicate(
                        date = draft.date,
                        meetingName = draft.meetingName,
                        startedAt = draft.startedAt,
                        excludedId = draft.id
                    )

                if (duplicate != null) {
                    snackbarHostState.showSnackbar(
                        message =
                            "A similar meeting is already logged within 30 minutes."
                    )
                    return@launch
                }

                val attendance =
                    MeetingAttendance(
                        id = draft.id,
                        savedMeetingId = draft.savedMeetingId,
                        date = draft.date,
                        startedAt = draft.startedAt,
                        durationMinutes = draft.durationMinutes,
                        meetingName = draft.meetingName,
                        address = draft.address,
                        city = draft.city,
                        state = draft.state,
                        zipCode = draft.zipCode,
                        role = draft.role,
                        notes = draft.notes,
                        createdAt = draft.createdAt
                    )

                val savedAttendanceId =
                    if (draft.id == 0L) {
                        meetingDao.insertAttendance(attendance)
                    } else {
                        meetingDao.updateAttendance(attendance)
                        draft.id
                    }

                meetingAttendanceHistory =
                    meetingDao.getAllAttendance()

                showMeetingAttendanceDialog = false
                showMeetingPickerDialog = false
                attendanceBeingEdited = null
                meetingForAttendance = null
                isOneTimeMeetingAttendance = false
                isSavingMeeting = false

                val snackbarResult =
                    snackbarHostState.showSnackbar(
                        message =
                            if (draft.id == 0L) {
                                "Meeting attendance logged."
                            } else {
                                "Meeting attendance updated."
                            },
                        actionLabel =
                            if (draft.id == 0L) {
                                "Undo"
                            } else {
                                null
                            },
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )

                if (
                    draft.id == 0L &&
                    snackbarResult == SnackbarResult.ActionPerformed
                ) {
                    meetingDao.deleteAttendanceById(
                        savedAttendanceId
                    )
                    meetingAttendanceHistory =
                        meetingDao.getAllAttendance()
                    snackbarHostState.showSnackbar(
                        message = "Meeting attendance undone."
                    )
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not save meeting attendance."
                )
            } finally {
                isSavingMeeting = false
            }
        }
    }

    fun deleteMeetingAttendance(
        attendance: MeetingAttendance
    ) {
        coroutineScope.launch {
            isDeletingMeetingAttendance = true

            try {
                meetingDao.deleteAttendanceById(attendance.id)
                meetingAttendanceHistory =
                    meetingDao.getAllAttendance()
                meetingAttendancePendingDeletion = null

                snackbarHostState.showSnackbar(
                    message = "Meeting attendance removed."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not remove meeting attendance."
                )
            } finally {
                isDeletingMeetingAttendance = false
            }
        }
    }

    fun saveCarePlace(
        draft: CarePlaceDraft
    ) {
        coroutineScope.launch {
            isSavingCareVisit = true

            try {
                val now = System.currentTimeMillis()
                val existing =
                    draft.id.takeIf { it > 0L }
                        ?.let { careVisitDao.getPlaceById(it) }

                val savedId =
                    if (existing == null) {
                        careVisitDao.insertPlace(
                            CarePlace(
                                name = draft.name,
                                placeCategory = draft.placeCategory,
                                address = draft.address,
                                city = draft.city,
                                state = draft.state,
                                zipCode = draft.zipCode,
                                phone = draft.phone,
                                website = draft.website,
                                patientPortal = draft.patientPortal,
                                notes = draft.notes,
                                active = draft.active,
                                createdAt = draft.createdAt,
                                updatedAt = now
                            )
                        )
                    } else {
                        careVisitDao.updatePlace(
                            existing.copy(
                                name = draft.name,
                                placeCategory = draft.placeCategory,
                                address = draft.address,
                                city = draft.city,
                                state = draft.state,
                                zipCode = draft.zipCode,
                                phone = draft.phone,
                                website = draft.website,
                                patientPortal = draft.patientPortal,
                                notes = draft.notes,
                                active = draft.active,
                                updatedAt = now
                            )
                        )
                        existing.id
                    }

                carePlaces = careVisitDao.getActivePlaces()
                val savedPlace = careVisitDao.getPlaceById(savedId)

                showCarePlaceEditorDialog = false
                carePlaceBeingEdited = null

                if (
                    continueVisitAfterPlaceSave &&
                    savedPlace != null
                ) {
                    selectedCarePlaceForVisit = savedPlace
                    selectedCareProviderForVisit = null
                    showCareVisitStartDialog = false
                    showCareProviderPickerDialog = true
                } else {
                    showCareVisitStartDialog = true
                }

                continueVisitAfterPlaceSave = false
                snackbarHostState.showSnackbar(
                    message =
                        if (existing == null) {
                            "Care place saved."
                        } else {
                            "Care place updated."
                        }
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not save the care place."
                )
            } finally {
                isSavingCareVisit = false
            }
        }
    }

    fun saveCareProvider(
        draft: CareProviderDraft
    ) {
        coroutineScope.launch {
            isSavingCareVisit = true

            try {
                val now = System.currentTimeMillis()
                val existing =
                    draft.id.takeIf { it > 0L }
                        ?.let { careVisitDao.getProviderById(it) }

                val savedId =
                    if (existing == null) {
                        careVisitDao.insertProvider(
                            CareProvider(
                                placeId = draft.placeId,
                                name = draft.name,
                                credentials = draft.credentials,
                                specialty = draft.specialty,
                                phone = draft.phone,
                                notes = draft.notes,
                                active = draft.active,
                                createdAt = draft.createdAt,
                                updatedAt = now
                            )
                        )
                    } else {
                        careVisitDao.updateProvider(
                            existing.copy(
                                placeId = draft.placeId,
                                name = draft.name,
                                credentials = draft.credentials,
                                specialty = draft.specialty,
                                phone = draft.phone,
                                notes = draft.notes,
                                active = draft.active,
                                updatedAt = now
                            )
                        )
                        existing.id
                    }

                careProviders = careVisitDao.getActiveProviders()
                val savedProvider = careVisitDao.getProviderById(savedId)

                showCareProviderEditorDialog = false
                careProviderBeingEdited = null

                if (
                    continueVisitAfterProviderSave &&
                    savedProvider != null
                ) {
                    selectedCareProviderForVisit = savedProvider
                    careVisitBeingEdited = null
                    isOneTimeCareVisit = false
                    showCareProviderPickerDialog = false
                    showCareVisitEditorDialog = true
                } else {
                    showCareProviderPickerDialog = true
                }

                continueVisitAfterProviderSave = false
                snackbarHostState.showSnackbar(
                    message =
                        if (existing == null) {
                            "Provider saved."
                        } else {
                            "Provider updated."
                        }
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not save the provider."
                )
            } finally {
                isSavingCareVisit = false
            }
        }
    }

    fun saveCareVisit(
        draft: CareVisitDraft
    ) {
        coroutineScope.launch {
            isSavingCareVisit = true

            try {
                val duplicate =
                    careVisitDao.findPotentialDuplicate(
                        date = draft.date,
                        placeName = draft.placeName,
                        providerName = draft.providerName,
                        startedAt = draft.startedAt,
                        excludedId = draft.id
                    )

                if (duplicate != null) {
                    snackbarHostState.showSnackbar(
                        message =
                            "A similar care visit is already logged within 30 minutes."
                    )
                    return@launch
                }

                val now = System.currentTimeMillis()
                val visit =
                    CareVisit(
                        id = draft.id,
                        placeId = draft.placeId,
                        providerId = draft.providerId,
                        date = draft.date,
                        startedAt = draft.startedAt,
                        visitCategory = draft.visitCategory,
                        visitFormat = draft.visitFormat,
                        placeName = draft.placeName,
                        placeCategory = draft.placeCategory,
                        providerName = draft.providerName,
                        providerCredentials = draft.providerCredentials,
                        providerSpecialty = draft.providerSpecialty,
                        address = draft.address,
                        city = draft.city,
                        state = draft.state,
                        zipCode = draft.zipCode,
                        placePhone = draft.placePhone,
                        providerPhone = draft.providerPhone,
                        reasonForVisit = draft.reasonForVisit,
                        visitSummary = draft.visitSummary,
                        testsProcedures = draft.testsProcedures,
                        resultsDiscussed = draft.resultsDiscussed,
                        instructions = draft.instructions,
                        medicationChanges = draft.medicationChanges,
                        referrals = draft.referrals,
                        followUpDate = draft.followUpDate,
                        notes = draft.notes,
                        weightPounds = draft.weightPounds,
                        systolic = draft.systolic,
                        diastolic = draft.diastolic,
                        a1c = draft.a1c,
                        bloodGlucose = draft.bloodGlucose,
                        cholesterolTotal = draft.cholesterolTotal,
                        cholesterolLdl = draft.cholesterolLdl,
                        cholesterolHdl = draft.cholesterolHdl,
                        triglycerides = draft.triglycerides,
                        createdAt = draft.createdAt,
                        updatedAt = now
                    )

                database.withTransaction {
                    if (draft.id == 0L) {
                        careVisitDao.insertVisit(visit)
                    } else {
                        careVisitDao.updateVisit(visit)
                    }

                    if (draft.copyCompatibleMeasurements) {
                        val measurementNote =
                            "Recorded during ${draft.visitCategory.lowercase(Locale.US)} visit at ${draft.placeName}."

                        draft.weightPounds?.let { value ->
                            healthProfileDao.addMeasurement(
                                HealthMeasurement(
                                    recordedDate = draft.date,
                                    type = HealthMeasurementType.WEIGHT,
                                    primaryValue = value,
                                    notes = measurementNote
                                )
                            )
                        }

                        if (
                            draft.systolic != null &&
                            draft.diastolic != null
                        ) {
                            healthProfileDao.addMeasurement(
                                HealthMeasurement(
                                    recordedDate = draft.date,
                                    type = HealthMeasurementType.BLOOD_PRESSURE,
                                    primaryValue = draft.systolic.toDouble(),
                                    secondaryValue = draft.diastolic.toDouble(),
                                    notes = measurementNote
                                )
                            )
                        }

                        draft.a1c?.let { value ->
                            healthProfileDao.addMeasurement(
                                HealthMeasurement(
                                    recordedDate = draft.date,
                                    type = HealthMeasurementType.A1C,
                                    primaryValue = value,
                                    notes = measurementNote
                                )
                            )
                        }

                        draft.cholesterolTotal?.let { total ->
                            healthProfileDao.addMeasurement(
                                HealthMeasurement(
                                    recordedDate = draft.date,
                                    type = HealthMeasurementType.CHOLESTEROL,
                                    primaryValue = total,
                                    secondaryValue = draft.cholesterolLdl,
                                    tertiaryValue = draft.cholesterolHdl,
                                    quaternaryValue = draft.triglycerides,
                                    notes = measurementNote
                                )
                            )
                        }
                    }
                }

                careVisits = careVisitDao.getAllVisits()
                healthFeatureRefreshKey++

                showCareVisitEditorDialog = false
                showCareVisitStartDialog = false
                showCareProviderPickerDialog = false
                selectedCarePlaceForVisit = null
                selectedCareProviderForVisit = null
                careVisitBeingEdited = null
                isOneTimeCareVisit = false

                if (returnToCareHistoryAfterVisitSave) {
                    showCareVisitHistoryDialog = true
                }
                returnToCareHistoryAfterVisitSave = false

                snackbarHostState.showSnackbar(
                    message =
                        if (draft.id == 0L) {
                            "Care visit logged."
                        } else {
                            "Care visit updated."
                        }
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not save the care visit."
                )
            } finally {
                isSavingCareVisit = false
            }
        }
    }

    fun deleteCareVisit(
        visit: CareVisit
    ) {
        coroutineScope.launch {
            isDeletingCareVisit = true

            try {
                careVisitDao.deleteVisitById(visit.id)
                careVisits = careVisitDao.getAllVisits()
                careVisitPendingDeletion = null
                snackbarHostState.showSnackbar(
                    message = "Care visit removed."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not remove the care visit."
                )
            } finally {
                isDeletingCareVisit = false
            }
        }
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

                    val mobilitySessions =
                        mobilitySessionDao
                            .getSessionsForDate(
                                date
                            )

                    val showerLog =
                        showerLogDao.getLogByDate(
                            date
                        )

                    val migraineEvents =
                        migraineLogDao.getLogsForDate(
                            date
                        )

                    val meetingAttendance =
                        meetingDao.getAttendanceForDate(
                            date
                        )

                    val careVisitsForDate =
                        careVisitDao.getVisitsForDate(
                            date
                        )

                    if (
                        record != null ||
                        entries.isNotEmpty() ||
                        activitySnapshot != null ||
                        mobilitySessions.isNotEmpty() ||
                        showerLog != null ||
                        migraineEvents.isNotEmpty() ||
                        meetingAttendance.isNotEmpty() ||
                        careVisitsForDate.isNotEmpty()
                    ) {
                        loadedDays.add(
                            DailyHistoryDay(
                                date = date,
                                record = record,
                                foodEntries = entries,
                                activitySnapshot =
                                    activitySnapshot,
                                mobilitySessions =
                                    mobilitySessions,
                                showerLogged =
                                    showerLog != null,
                                migraineLogs =
                                    migraineEvents,
                                meetingAttendance =
                                    meetingAttendance,
                                careVisits =
                                    careVisitsForDate
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

    fun logShowerToday() {
        coroutineScope.launch {
            try {
                showerLogDao.save(
                    ShowerLog(
                        date = todayDate
                    )
                )

                showeredToday = true

                if (todayDate !in showerDatesThisWeek) {
                    showerDatesThisWeek =
                        (showerDatesThisWeek + todayDate)
                            .sorted()
                }

                snackbarHostState.showSnackbar(
                    message =
                        "Shower logged for today."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message =
                        "Could not log today’s shower."
                )
            }
        }
    }

    fun removeTodayShower() {
        coroutineScope.launch {
            try {
                showerLogDao.deleteByDate(
                    todayDate
                )

                showeredToday = false
                showerDatesThisWeek =
                    showerDatesThisWeek.filterNot {
                        it == todayDate
                    }

                snackbarHostState.showSnackbar(
                    message =
                        "Today’s shower log removed."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message =
                        "Could not remove today’s shower."
                )
            }
        }
    }


    fun saveMigraineEvent(
        draft: MigraineLogDraft
    ) {
        coroutineScope.launch {
            try {
                migraineLogDao.save(
                    MigraineLog(
                        date = draft.date,
                        occurredAt = draft.occurredAt,
                        auraDurationMinutes =
                            draft.auraDurationMinutes,
                        visualAura =
                            draft.visualAura,
                        headPain =
                            draft.headPain,
                        foggyAfterward =
                            draft.foggyAfterward,
                        notes = draft.notes
                    )
                )

                migraineLogs =
                    migraineLogDao.getAllLogs()
                showMigraineLogDialog = false

                snackbarHostState.showSnackbar(
                    message =
                        "Migraine / visual-aura event logged."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message =
                        "Could not save the migraine event."
                )
            }
        }
    }

    fun deleteMigraineEvent(
        log: MigraineLog
    ) {
        coroutineScope.launch {
            try {
                migraineLogDao.deleteById(
                    log.id
                )

                migraineLogs =
                    migraineLogs.filterNot {
                        it.id == log.id
                    }

                snackbarHostState.showSnackbar(
                    message =
                        "Migraine event removed."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message =
                        "Could not remove the migraine event."
                )
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
                                activityMinutes =
                                    liveHealthActivity
                                        .activityMinutes,
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


    fun saveMobilitySession(
        draft: MobilitySessionDraft
    ) {
        coroutineScope.launch {
            isSavingMobility = true

            try {
                val session =
                    MobilitySession(
                        date = todayDate,
                        routineName =
                            draft.routineName,
                        plannedMovementIds =
                            encodeMovementIds(
                                draft.plannedMovementIds
                            ),
                        completedMovementIds =
                            encodeMovementIds(
                                draft.completedMovementIds
                            ),
                        skippedMovementIds =
                            encodeMovementIds(
                                draft.skippedMovementIds
                            ),
                        movementSeconds =
                            draft.movementSeconds,
                        elapsedSeconds =
                            draft.elapsedSeconds,
                        notes = draft.notes
                    )

                val sessionId =
                    mobilitySessionDao
                        .addSession(
                            session
                        )

                mobilitySessionsToday =
                    listOf(
                        session.copy(
                            id = sessionId
                        )
                    ) + mobilitySessionsToday

                mobilityCompleted = true

                snackbarHostState
                    .showSnackbar(
                        message =
                            "Mobility session saved."
                    )
            } catch (
                exception: Exception
            ) {
                snackbarHostState
                    .showSnackbar(
                        message =
                            "Could not save mobility session."
                    )
            } finally {
                isSavingMobility = false
            }
        }
    }

    fun deleteMobilitySession(
        session: MobilitySession
    ) {
        coroutineScope.launch {
            isSavingMobility = true

            try {
                mobilitySessionDao
                    .deleteSession(
                        session
                    )

                mobilitySessionsToday =
                    mobilitySessionsToday
                        .filterNot {
                            it.id == session.id
                        }

                if (
                    mobilitySessionsToday.isEmpty()
                ) {
                    mobilityCompleted = false
                }

                snackbarHostState
                    .showSnackbar(
                        message =
                            "Mobility session deleted."
                    )
            } catch (
                exception: Exception
            ) {
                snackbarHostState
                    .showSnackbar(
                        message =
                            "Could not delete mobility session."
                    )
            } finally {
                isSavingMobility = false
            }
        }
    }

    fun openSavedFoodsScreen() {
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
    }

    fun openMealBuilderScreen() {
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
    }

    fun openSavedMealsScreen() {
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
    }

    fun deleteFoodEntry(
        entry: FoodLogEntry
    ) {
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

    fun deleteMealLog(
        mealLogId: String
    ) {
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isLoading) {
                DailyRebuildBottomNavigation(
                    selectedTab = selectedMainTab,
                    onTabSelected = {
                        selectedMainTab = it

                        if (it == 0) {
                            coroutineScope.launch {
                                homeScrollState
                                    .animateScrollTo(0)
                            }
                        }
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
            when (selectedMainTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(
                                homeScrollState
                            )
                            .padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp)
                    ) {
                        HeaderSection(
                            todayDate = todayDate,
                            onOpenHistory = {
                                openDailyHistory()
                            }
                        )

                        TodayNextStepCard(
                            foodRecorded =
                                foodEntries.isNotEmpty(),
                            waterOunces =
                                totalWaterOunces,
                            mobilityCompleted =
                                mobilityCompleted,
                            painRecorded =
                                painRecorded,
                            showerDue =
                                showerIsDueNow,
                            showersThisWeek =
                                showerCountThisWeek,
                            meetingDue =
                                meetingGoalNeedsAttention,
                            meetingsThisWeek =
                                meetingCountThisWeek,
                            completedTasks =
                                completedTasks,
                            isSaving =
                                isSaving,
                            onOpenFood = {
                                selectedMainTab = 1
                            },
                            onOpenWater = {
                                showQuickWaterDialog = true
                            },
                            onOpenMobility = {
                                selectedMainTab = 2
                            },
                            onOpenPain = {
                                showQuickPainDialog = true
                            },
                            onLogShower = {
                                logShowerToday()
                            },
                            onOpenMeetings = {
                                selectedMainTab = 3
                            },
                            onOpenAnchors = {
                                showMoreToday = true
                                expandedTodaySection =
                                    "anchors"

                                coroutineScope.launch {
                                    delay(120)
                                    homeScrollState
                                        .animateScrollTo(
                                            homeScrollState
                                                .maxValue
                                        )
                                }
                            },
                            onSave = saveToday
                        )

                        TodayOverviewCard(
                            completedTasks =
                                completedTasks,
                            totalTasks = 4,
                            calories =
                                totalCaloriesToday,
                            calorieGoal =
                                currentCalorieGoal,
                            waterOunces =
                                totalWaterOunces,
                            mobilityCompleted =
                                mobilityCompleted,
                            showersThisWeek =
                                showerCountThisWeek
                        )

                        HomeMeetingsCard(
                            meetingsThisWeek =
                                meetingCountThisWeek,
                            onOpenMeetings = {
                                selectedMainTab = 3
                            },
                            onLogMeeting = {
                                showMeetingPickerDialog = true
                            }
                        )

                        TodayQuickActionsCard(
                            onAddFood = {
                                selectedMainTab = 1
                            },
                            onLogWater = {
                                showQuickWaterDialog = true
                            },
                            onOpenMobility = {
                                selectedMainTab = 2
                            },
                            onLogPain = {
                                showQuickPainDialog = true
                            }
                        )

                        CompactActivityCard(
                            availability =
                                healthAvailability,
                            hasPermissions =
                                hasHealthPermissions,
                            isLoading =
                                isLoadingHealthActivity,
                            activity =
                                displayedActivity,
                            sourceLabel =
                                activitySourceLabel,
                            onOpenActivitySettings = {
                                selectedMainTab = 4
                            }
                        )

                        Button(
                            onClick = saveToday,
                            enabled = !isSaving,
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(18.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(
                                    Modifier.width(10.dp)
                                )
                            }

                            Text(
                                if (isSaving) {
                                    "Saving today…"
                                } else {
                                    "Save Today"
                                }
                            )
                        }

                        TodayDetailsCard(
                            expanded = showMoreToday,
                            onToggle = {
                                showMoreToday =
                                    !showMoreToday
                            }
                        ) {
                            TodayDetailToggleRow(
                                title =
                                    "Daily anchors",
                                summary =
                                    "$completedTasks of 4 complete",
                                expanded =
                                    expandedTodaySection ==
                                        "anchors",
                                onClick = {
                                    expandedTodaySection =
                                        if (
                                            expandedTodaySection ==
                                                "anchors"
                                        ) {
                                            null
                                        } else {
                                            "anchors"
                                        }
                                }
                            )

                            if (
                                expandedTodaySection ==
                                    "anchors"
                            ) {
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
                            }

                            TodayDetailToggleRow(
                                title =
                                    "Showering",
                                summary =
                                    "$showerCountThisWeek this week · goal 2–3",
                                expanded =
                                    expandedTodaySection ==
                                        "showering",
                                onClick = {
                                    expandedTodaySection =
                                        if (
                                            expandedTodaySection ==
                                                "showering"
                                        ) {
                                            null
                                        } else {
                                            "showering"
                                        }
                                }
                            )

                            if (
                                expandedTodaySection ==
                                    "showering"
                            ) {
                                WeeklyShowerControls(
                                    showerDates =
                                        showerDatesThisWeek,
                                    showeredToday =
                                        showeredToday,
                                    onLogToday = {
                                        logShowerToday()
                                    },
                                    onRemoveToday = {
                                        removeTodayShower()
                                    }
                                )
                            }

                            TodayDetailToggleRow(
                                title =
                                    "Medication check-in",
                                summary =
                                    "Morning and night reference checks",
                                expanded =
                                    expandedTodaySection ==
                                        "medication",
                                onClick = {
                                    expandedTodaySection =
                                        if (
                                            expandedTodaySection ==
                                                "medication"
                                        ) {
                                            null
                                        } else {
                                            "medication"
                                        }
                                }
                            )

                            if (
                                expandedTodaySection ==
                                    "medication"
                            ) {
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
                            }

                            TodayDetailToggleRow(
                                title =
                                    "Journal",
                                summary =
                                    if (
                                        journalText.isBlank()
                                    ) {
                                        "No note yet"
                                    } else {
                                        "Today has a note"
                                    },
                                expanded =
                                    expandedTodaySection ==
                                        "journal",
                                onClick = {
                                    expandedTodaySection =
                                        if (
                                            expandedTodaySection ==
                                                "journal"
                                        ) {
                                            null
                                        } else {
                                            "journal"
                                        }
                                }
                            )

                            if (
                                expandedTodaySection ==
                                    "journal"
                            ) {
                                JournalSection(
                                    journalText =
                                        journalText,
                                    onJournalTextChange = {
                                        journalText = it
                                    }
                                )
                            }
                        }

                        Text(
                            text =
                                "Food, mobility, history, and health tools now live in their own tabs below.",
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            modifier =
                                Modifier.padding(
                                    horizontal = 6.dp
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )
                    }
                }

                1 -> {
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
                        TabScreenHeader(
                            title = "Food",
                            subtitle =
                                "Log today’s meal, manage saved foods, and build reusable meals.",
                            onOpenHistory = {
                                openDailyHistory()
                            }
                        )

                        FoodHydrationCard(
                            totalWaterOunces =
                                totalWaterOunces,
                            totalBottleCount =
                                plainReusableBottleCount +
                                    mioReusableBottleCount +
                                    plainDisposableBottleCount +
                                    mioDisposableBottleCount,
                            onAddWater = {
                                showQuickWaterDialog = true
                            }
                        )

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
                                isCreatingFoodForMeal =
                                    false
                                scannedFoodPrefill = null
                                showManualFoodDialog =
                                    true
                            },
                            onOpenSavedFoods = {
                                openSavedFoodsScreen()
                            },
                            onBuildMeal = {
                                openMealBuilderScreen()
                            },
                            onOpenSavedMeals = {
                                openSavedMealsScreen()
                            },
                            onDeleteEntry = {
                                deleteFoodEntry(it)
                            },
                            onDeleteMealLog = {
                                deleteMealLog(it)
                            }
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )
                    }
                }

                2 -> {
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
                        TabScreenHeader(
                            title = "Mobility",
                            subtitle =
                                "See today’s walking, generate a balanced routine, or record independent stretching.",
                            onOpenHistory = {
                                openDailyHistory()
                            }
                        )

                        MobilityWalkingCard(
                            availability =
                                healthAvailability,
                            hasPermissions =
                                hasHealthPermissions,
                            isLoading =
                                isLoadingHealthActivity,
                            activity =
                                displayedActivity,
                            sourceLabel =
                                activitySourceLabel,
                            onRefresh = {
                                refreshHealthActivity(
                                    showFeedback = true
                                )
                            },
                            onManage = {
                                selectedMainTab = 4
                            }
                        )

                        MobilitySection(
                            sessions =
                                mobilitySessionsToday,
                            isSaving =
                                isSavingMobility,
                            onSaveSession = {
                                saveMobilitySession(it)
                            },
                            onDeleteSession = {
                                deleteMobilitySession(it)
                            }
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )
                    }
                }

                3 -> {
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
                        TabScreenHeader(
                            title = "Meetings",
                            subtitle =
                                "Track a goal of at least three recovery meetings each week.",
                            onOpenHistory = {
                                openDailyHistory()
                            }
                        )

                        MeetingsTab(
                            weeklyAttendance =
                                weeklyMeetingAttendance,
                            isSaving =
                                isSavingMeeting,
                            onLogMeeting = {
                                showMeetingPickerDialog = true
                            },
                            onAddMeeting = {
                                meetingBeingEdited = null
                                logAttendanceAfterMeetingSave = false
                                showMeetingEditorDialog = true
                            },
                            onEditAttendance = {
                                attendanceBeingEdited = it
                                meetingForAttendance =
                                    it.savedMeetingId?.let { id ->
                                        savedMeetings.firstOrNull { meeting ->
                                            meeting.id == id
                                        }
                                    }
                                isOneTimeMeetingAttendance =
                                    it.savedMeetingId == null
                                showMeetingAttendanceDialog = true
                            },
                            onDeleteAttendance = {
                                meetingAttendancePendingDeletion = it
                            },
                            onViewFullHistory = {
                                showMeetingHistoryDialog = true
                            }
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )
                    }
                }

                else -> {
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
                        TabScreenHeader(
                            title = "Health",
                            subtitle =
                                "Care visits, profile, measurements, medication reference, health events, and connected activity.",
                            onOpenHistory = {
                                openDailyHistory()
                            }
                        )

                        CareVisitTrackerCard(
                            visits = careVisits,
                            onLogVisit = {
                                showCareVisitStartDialog = true
                            },
                            onOpenHistory = {
                                showCareVisitHistoryDialog = true
                            }
                        )

                        MigraineTrackerCard(
                            logs = migraineLogs,
                            onLogMigraine = {
                                showMigraineLogDialog = true
                            },
                            onDeleteLog = {
                                deleteMigraineEvent(it)
                            }
                        )

                        key(healthFeatureRefreshKey) {
                            HealthProfileFeature()
                        }

                        ActivitySection(
                            availability =
                                healthAvailability,
                            hasPermissions =
                                hasHealthPermissions,
                            isLoading =
                                isLoadingHealthActivity,
                            activity =
                                displayedActivity,
                            sourceLabel =
                                activitySourceLabel,
                            onConnect = {
                                healthPermissionsLauncher
                                    .launch(
                                        HealthConnectManager
                                            .permissions
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
                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCareVisitStartDialog) {
        CareVisitStartDialog(
            recentPlaces = recentCarePlaces,
            visits = careVisits,
            onSelectPlace = { place ->
                selectedCarePlaceForVisit = place
                selectedCareProviderForVisit = null
                showCareVisitStartDialog = false
                showCareProviderPickerDialog = true
            },
            onEditPlace = { place ->
                carePlaceBeingEdited = place
                continueVisitAfterPlaceSave = false
                showCareVisitStartDialog = false
                showCarePlaceEditorDialog = true
            },
            onAddPlace = {
                carePlaceBeingEdited = null
                continueVisitAfterPlaceSave = true
                showCareVisitStartDialog = false
                showCarePlaceEditorDialog = true
            },
            onOneTimeVisit = {
                selectedCarePlaceForVisit = null
                selectedCareProviderForVisit = null
                careVisitBeingEdited = null
                isOneTimeCareVisit = true
                returnToCareHistoryAfterVisitSave = false
                showCareVisitStartDialog = false
                showCareVisitEditorDialog = true
            },
            onDismiss = {
                showCareVisitStartDialog = false
            }
        )
    }

    if (
        showCareProviderPickerDialog &&
        selectedCarePlaceForVisit != null
    ) {
        CareProviderPickerDialog(
            place = selectedCarePlaceForVisit!!,
            providers = providersForSelectedCarePlace,
            onSelectProvider = { provider ->
                selectedCareProviderForVisit = provider
                careVisitBeingEdited = null
                isOneTimeCareVisit = false
                returnToCareHistoryAfterVisitSave = false
                showCareProviderPickerDialog = false
                showCareVisitEditorDialog = true
            },
            onEditProvider = { provider ->
                careProviderBeingEdited = provider
                continueVisitAfterProviderSave = false
                showCareProviderPickerDialog = false
                showCareProviderEditorDialog = true
            },
            onAddProvider = {
                careProviderBeingEdited = null
                continueVisitAfterProviderSave = true
                showCareProviderPickerDialog = false
                showCareProviderEditorDialog = true
            },
            onContinueWithoutProvider = {
                selectedCareProviderForVisit = null
                careVisitBeingEdited = null
                isOneTimeCareVisit = false
                returnToCareHistoryAfterVisitSave = false
                showCareProviderPickerDialog = false
                showCareVisitEditorDialog = true
            },
            onBack = {
                showCareProviderPickerDialog = false
                selectedCarePlaceForVisit = null
                showCareVisitStartDialog = true
            },
            onDismiss = {
                showCareProviderPickerDialog = false
                selectedCarePlaceForVisit = null
            }
        )
    }

    if (showCarePlaceEditorDialog) {
        CarePlaceEditorDialog(
            existingPlace = carePlaceBeingEdited,
            isSaving = isSavingCareVisit,
            onSave = {
                saveCarePlace(it)
            },
            onDismiss = {
                if (!isSavingCareVisit) {
                    showCarePlaceEditorDialog = false
                    carePlaceBeingEdited = null
                    continueVisitAfterPlaceSave = false
                    showCareVisitStartDialog = true
                }
            }
        )
    }

    if (
        showCareProviderEditorDialog &&
        selectedCarePlaceForVisit != null
    ) {
        CareProviderEditorDialog(
            place = selectedCarePlaceForVisit!!,
            existingProvider = careProviderBeingEdited,
            isSaving = isSavingCareVisit,
            onSave = {
                saveCareProvider(it)
            },
            onDismiss = {
                if (!isSavingCareVisit) {
                    showCareProviderEditorDialog = false
                    careProviderBeingEdited = null
                    continueVisitAfterProviderSave = false
                    showCareProviderPickerDialog = true
                }
            }
        )
    }

    if (showCareVisitEditorDialog) {
        CareVisitEditorDialog(
            savedPlace = selectedCarePlaceForVisit,
            savedProvider = selectedCareProviderForVisit,
            existingVisit = careVisitBeingEdited,
            isOneTimeVisit = isOneTimeCareVisit,
            isSaving = isSavingCareVisit,
            onSave = {
                saveCareVisit(it)
            },
            onDismiss = {
                if (!isSavingCareVisit) {
                    showCareVisitEditorDialog = false
                    careVisitBeingEdited = null
                    selectedCarePlaceForVisit = null
                    selectedCareProviderForVisit = null
                    isOneTimeCareVisit = false
                    if (returnToCareHistoryAfterVisitSave) {
                        showCareVisitHistoryDialog = true
                    }
                    returnToCareHistoryAfterVisitSave = false
                }
            }
        )
    }

    if (showCareVisitHistoryDialog) {
        CareVisitHistoryDialog(
            visits = careVisits,
            onEdit = { visit ->
                careVisitBeingEdited = visit
                selectedCarePlaceForVisit =
                    visit.placeId?.let { id ->
                        carePlaces.firstOrNull { it.id == id }
                    }
                selectedCareProviderForVisit =
                    visit.providerId?.let { id ->
                        careProviders.firstOrNull { it.id == id }
                    }
                isOneTimeCareVisit = visit.placeId == null
                returnToCareHistoryAfterVisitSave = true
                showCareVisitHistoryDialog = false
                showCareVisitEditorDialog = true
            },
            onDelete = {
                careVisitPendingDeletion = it
            },
            onDismiss = {
                showCareVisitHistoryDialog = false
            }
        )
    }

    careVisitPendingDeletion?.let { visit ->
        DeleteCareVisitDialog(
            visit = visit,
            isDeleting = isDeletingCareVisit,
            onConfirm = {
                deleteCareVisit(visit)
            },
            onDismiss = {
                if (!isDeletingCareVisit) {
                    careVisitPendingDeletion = null
                }
            }
        )
    }

    if (showMigraineLogDialog) {
        MigraineLogDialog(
            onDismiss = {
                showMigraineLogDialog = false
            },
            onSave = {
                saveMigraineEvent(it)
            }
        )
    }

    if (showMeetingPickerDialog) {
        MeetingPickerDialog(
            recentMeetings =
                recentMeetingsForPicker,
            onSelectMeeting = { meeting ->
                meetingForAttendance = meeting
                attendanceBeingEdited = null
                isOneTimeMeetingAttendance = false
                showMeetingPickerDialog = false
                showMeetingAttendanceDialog = true
            },
            onEditMeeting = { meeting ->
                meetingBeingEdited = meeting
                logAttendanceAfterMeetingSave = false
                showMeetingPickerDialog = false
                showMeetingEditorDialog = true
            },
            onAddMeeting = {
                meetingBeingEdited = null
                logAttendanceAfterMeetingSave = true
                showMeetingPickerDialog = false
                showMeetingEditorDialog = true
            },
            onLogOneTime = {
                meetingForAttendance = null
                attendanceBeingEdited = null
                isOneTimeMeetingAttendance = true
                showMeetingPickerDialog = false
                showMeetingAttendanceDialog = true
            },
            onDismiss = {
                showMeetingPickerDialog = false
            }
        )
    }

    if (showMeetingEditorDialog) {
        MeetingEditorDialog(
            existingMeeting = meetingBeingEdited,
            isSaving = isSavingMeeting,
            onSave = {
                saveMeeting(it)
            },
            onDismiss = {
                if (!isSavingMeeting) {
                    showMeetingEditorDialog = false
                    meetingBeingEdited = null
                    logAttendanceAfterMeetingSave = false
                }
            }
        )
    }

    if (showMeetingAttendanceDialog) {
        MeetingAttendanceDialog(
            savedMeeting = meetingForAttendance,
            existingAttendance = attendanceBeingEdited,
            isOneTimeMeeting =
                isOneTimeMeetingAttendance,
            isSaving = isSavingMeeting,
            onSave = {
                saveMeetingAttendance(it)
            },
            onDismiss = {
                if (!isSavingMeeting) {
                    showMeetingAttendanceDialog = false
                    meetingForAttendance = null
                    attendanceBeingEdited = null
                    isOneTimeMeetingAttendance = false
                }
            }
        )
    }

    if (showMeetingHistoryDialog) {
        MeetingHistoryDialog(
            attendance = meetingAttendanceHistory,
            onEdit = { attendance ->
                attendanceBeingEdited = attendance
                meetingForAttendance =
                    attendance.savedMeetingId?.let { id ->
                        savedMeetings.firstOrNull { meeting ->
                            meeting.id == id
                        }
                    }
                isOneTimeMeetingAttendance =
                    attendance.savedMeetingId == null
                showMeetingHistoryDialog = false
                showMeetingAttendanceDialog = true
            },
            onDelete = {
                meetingAttendancePendingDeletion = it
            },
            onDismiss = {
                showMeetingHistoryDialog = false
            }
        )
    }

    meetingAttendancePendingDeletion?.let { attendance ->
        DeleteMeetingAttendanceDialog(
            attendance = attendance,
            isDeleting = isDeletingMeetingAttendance,
            onConfirm = {
                deleteMeetingAttendance(attendance)
            },
            onDismiss = {
                if (!isDeletingMeetingAttendance) {
                    meetingAttendancePendingDeletion = null
                }
            }
        )
    }

    if (showQuickWaterDialog) {
        AlertDialog(
            onDismissRequest = {
                showQuickWaterDialog = false
            },
            title = {
                Text("Log Water")
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .heightIn(max = 520.dp)
                            .verticalScroll(
                                rememberScrollState()
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text =
                            "${formatOunces(totalWaterOunces)} oz recorded today",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    nextBottleHasMio =
                                        !nextBottleHasMio
                                }
                                .padding(8.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked =
                                nextBottleHasMio,
                            onCheckedChange = {
                                nextBottleHasMio = it
                            }
                        )

                        Text(
                            text =
                                "The next bottle has MiO",
                            style =
                                MaterialTheme.typography
                                    .bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            if (nextBottleHasMio) {
                                mioReusableBottleCount++
                            } else {
                                plainReusableBottleCount++
                            }

                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(16.dp)
                    ) {
                        Text("Add 24 oz reusable bottle")
                    }

                    OutlinedButton(
                        onClick = {
                            if (nextBottleHasMio) {
                                mioDisposableBottleCount++
                            } else {
                                plainDisposableBottleCount++
                            }

                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(16.dp)
                    ) {
                        Text("Add 16.9 oz disposable bottle")
                    }

                    if (
                        plainReusableBottleCount +
                            mioReusableBottleCount +
                            plainDisposableBottleCount +
                            mioDisposableBottleCount >
                        0
                    ) {
                        HorizontalDivider()

                        Text(
                            text = "Today’s bottles",
                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )

                        if (
                            plainReusableBottleCount >
                            0
                        ) {
                            WaterCountRow(
                                label =
                                    "24 oz plain water",
                                count =
                                    plainReusableBottleCount,
                                onRemoveOne = {
                                    plainReusableBottleCount--
                                }
                            )
                        }

                        if (
                            mioReusableBottleCount >
                            0
                        ) {
                            WaterCountRow(
                                label =
                                    "24 oz MiO water",
                                count =
                                    mioReusableBottleCount,
                                onRemoveOne = {
                                    mioReusableBottleCount--
                                }
                            )
                        }

                        if (
                            plainDisposableBottleCount >
                            0
                        ) {
                            WaterCountRow(
                                label =
                                    "16.9 oz plain water",
                                count =
                                    plainDisposableBottleCount,
                                onRemoveOne = {
                                    plainDisposableBottleCount--
                                }
                            )
                        }

                        if (
                            mioDisposableBottleCount >
                            0
                        ) {
                            WaterCountRow(
                                label =
                                    "16.9 oz MiO water",
                                count =
                                    mioDisposableBottleCount,
                                onRemoveOne = {
                                    mioDisposableBottleCount--
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showQuickWaterDialog = false
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showQuickPainDialog) {
        AlertDialog(
            onDismissRequest = {
                showQuickPainDialog = false
            },
            title = {
                Text("Record Pain")
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .heightIn(max = 520.dp)
                            .verticalScroll(
                                rememberScrollState()
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text =
                            "Record what is true right now. You can change it later before saving today.",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    PainSlider(
                        label = "Back pain",
                        painValue =
                            backPain,
                        onPainValueChange = {
                            backPain = it
                            painRecorded = true
                        }
                    )

                    PainSlider(
                        label = "Shin pain",
                        painValue =
                            shinPain,
                        onPainValueChange = {
                            shinPain = it
                            painRecorded = true
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        painRecorded = true
                        showQuickPainDialog = false
                    }
                ) {
                    Text("Done")
                }
            }
        )
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

                            mobilitySessionDao
                                .deleteSessionsForDate(
                                    day.date
                                )

                            showerLogDao.deleteByDate(
                                day.date
                            )

                            migraineLogDao.deleteByDate(
                                day.date
                            )

                            meetingDao.deleteAttendanceByDate(
                                day.date
                            )

                            careVisitDao.deleteVisitsByDate(
                                day.date
                            )
                        }

                        dailyHistoryDays =
                            dailyHistoryDays.filterNot {
                                it.date == day.date
                            }

                        showerDatesThisWeek =
                            showerDatesThisWeek.filterNot {
                                it == day.date
                            }

                        migraineLogs =
                            migraineLogs.filterNot {
                                it.date == day.date
                            }

                        meetingAttendanceHistory =
                            meetingAttendanceHistory.filterNot {
                                it.date == day.date
                            }

                        careVisits =
                            careVisits.filterNot {
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
                            mobilitySessionsToday = emptyList()
                            showeredToday = false
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


private data class DailyRebuildNavigationItem(
    val label: String,
    val symbol: String
)

private val dailyRebuildNavigationItems =
    listOf(
        DailyRebuildNavigationItem(
            label = "Home",
            symbol = "⌂"
        ),
        DailyRebuildNavigationItem(
            label = "Food",
            symbol = "F"
        ),
        DailyRebuildNavigationItem(
            label = "Mobility",
            symbol = "M"
        ),
        DailyRebuildNavigationItem(
            label = "Meetings",
            symbol = "G"
        ),
        DailyRebuildNavigationItem(
            label = "Health",
            symbol = "+"
        )
    )

@Composable
private fun DailyRebuildBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor =
            MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        dailyRebuildNavigationItems
            .forEachIndexed { index, item ->
                NavigationBarItem(
                    selected =
                        selectedTab == index,
                    onClick = {
                        onTabSelected(index)
                    },
                    icon = {
                        Text(
                            text = item.symbol,
                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )
                    },
                    label = {
                        Text(item.label)
                    },
                    alwaysShowLabel = true
                )
            }
    }
}

@Composable
private fun TabScreenHeader(
    title: String,
    subtitle: String,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 4.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography
                        .headlineMedium,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onOpenHistory
            ) {
                Text("History")
            }
        }

        Text(
            text = subtitle,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun TodayNextStepCard(
    foodRecorded: Boolean,
    waterOunces: Double,
    mobilityCompleted: Boolean,
    painRecorded: Boolean,
    showerDue: Boolean,
    showersThisWeek: Int,
    meetingDue: Boolean,
    meetingsThisWeek: Int,
    completedTasks: Int,
    isSaving: Boolean,
    onOpenFood: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenMobility: () -> Unit,
    onOpenPain: () -> Unit,
    onLogShower: () -> Unit,
    onOpenMeetings: () -> Unit,
    onOpenAnchors: () -> Unit,
    onSave: () -> Unit
) {
    val title: String
    val description: String
    val buttonText: String
    val onClick: () -> Unit

    when {
        !foodRecorded -> {
            title = "Log today’s food"
            description =
                "Start with the meal or food you actually ate."
            buttonText = "Open Food"
            onClick = onOpenFood
        }

        waterOunces <= 0.0 -> {
            title = "Start today’s water"
            description =
                "Record the first bottle without opening the full daily form."
            buttonText = "Log Water"
            onClick = onOpenWater
        }

        !mobilityCompleted -> {
            title = "Complete mobility"
            description =
                "Generate a seated or bed-compatible routine."
            buttonText = "Open Mobility"
            onClick = onOpenMobility
        }

        !painRecorded -> {
            title = "Record current pain"
            description =
                "A quick back and shin check keeps the day measurable."
            buttonText = "Log Pain"
            onClick = onOpenPain
        }

        showerDue -> {
            title = "Shower goal needs attention"
            description =
                "$showersThisWeek of 2 minimum showers logged this week."
            buttonText = "Log Today’s Shower"
            onClick = onLogShower
        }

        meetingDue -> {
            title = "Weekly meeting goal"
            description =
                "$meetingsThisWeek of $DEFAULT_WEEKLY_MEETING_GOAL meetings logged this week."
            buttonText = "Open Meetings"
            onClick = onOpenMeetings
        }

        completedTasks < 4 -> {
            title = "Review daily anchors"
            description =
                "One or more daily checks still need your attention."
            buttonText = "Review Anchors"
            onClick = onOpenAnchors
        }

        else -> {
            title = "Today is ready"
            description =
                "Review anything you need, then save the day."
            buttonText =
                if (isSaving) {
                    "Saving…"
                } else {
                    "Save Today"
                }
            onClick = onSave
        }
    }

    RebuildSectionCard(
        title = "Next step",
        subtitle = title,
        accentColor = RebuildAmber
    ) {
        Text(
            text = description,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        Button(
            onClick = onClick,
            enabled = !isSaving,
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(16.dp)
        ) {
            Text(buttonText)
        }
    }
}

@Composable
private fun TodayOverviewCard(
    completedTasks: Int,
    totalTasks: Int,
    calories: Double,
    calorieGoal: Int?,
    waterOunces: Double,
    mobilityCompleted: Boolean,
    showersThisWeek: Int
) {
    val progress =
        if (totalTasks <= 0) {
            0f
        } else {
            completedTasks
                .toFloat()
                .div(totalTasks.toFloat())
                .coerceIn(0f, 1f)
        }

    RebuildSectionCard(
        title = "Today",
        subtitle =
            "A compact view of the information you use most.",
        accentColor = RebuildBlue
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    "$completedTasks of $totalTasks anchors",
                style =
                    MaterialTheme.typography
                        .titleMedium
            )

            Text(
                text =
                    "${(progress * 100).toInt()}%",
                style =
                    MaterialTheme.typography
                        .labelLarge,
                color =
                    MaterialTheme.colorScheme
                        .primary
            )
        }

        LinearProgressIndicator(
            progress = progress,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(
                        RoundedCornerShape(50)
                    )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "calories",
                value =
                    if (calorieGoal != null) {
                        "${calories.toInt()} / $calorieGoal"
                    } else {
                        calories.toInt().toString()
                    },
                modifier =
                    Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme
                        .tertiaryContainer,
                contentColor =
                    MaterialTheme.colorScheme
                        .onTertiaryContainer
            )

            RebuildMetricPill(
                label = "water",
                value =
                    "${formatOunces(waterOunces)} oz",
                modifier =
                    Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme
                        .primaryContainer
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "mobility",
                value =
                    if (mobilityCompleted) {
                        "Done"
                    } else {
                        "Not yet"
                    },
                modifier =
                    Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme
                        .secondaryContainer,
                contentColor =
                    MaterialTheme.colorScheme
                        .onSecondaryContainer
            )

            RebuildMetricPill(
                label = "showers this week",
                value =
                    "$showersThisWeek / 2 minimum",
                modifier =
                    Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme
                        .surfaceVariant,
                contentColor =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }

        if (calorieGoal == null) {
            Text(
                text =
                    "No calorie goal is set yet. Add one under More → Health profile & goals after enough days are logged.",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayQuickActionsCard(
    onAddFood: () -> Unit,
    onLogWater: () -> Unit,
    onOpenMobility: () -> Unit,
    onLogPain: () -> Unit
) {
    RebuildSectionCard(
        title = "Quick actions",
        subtitle =
            "Open the right tool without searching through the entire page.",
        accentColor = RebuildGreen
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddFood,
                modifier =
                    Modifier.weight(1f),
                shape =
                    RoundedCornerShape(16.dp)
            ) {
                Text("Add Food")
            }

            OutlinedButton(
                onClick = onLogWater,
                modifier =
                    Modifier.weight(1f),
                shape =
                    RoundedCornerShape(16.dp)
            ) {
                Text("Water")
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenMobility,
                modifier =
                    Modifier.weight(1f),
                shape =
                    RoundedCornerShape(16.dp)
            ) {
                Text("Mobility")
            }

            OutlinedButton(
                onClick = onLogPain,
                modifier =
                    Modifier.weight(1f),
                shape =
                    RoundedCornerShape(16.dp)
            ) {
                Text("Log Pain")
            }
        }
    }
}

@Composable
private fun MobilityWalkingCard(
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    isLoading: Boolean,
    activity: HealthActivityData,
    sourceLabel: String?,
    onRefresh: () -> Unit,
    onManage: () -> Unit
) {
    RebuildSectionCard(
        title = "Today’s walking",
        subtitle =
            sourceLabel
                ?.let { "$it • Google Fit through Health Connect" }
                ?: "Steps, miles, and recorded activity time from Google Fit through Health Connect.",
        accentColor = RebuildTeal,
        trailing = {
            Row {
                TextButton(
                    onClick = onRefresh,
                    enabled =
                        availability ==
                            HealthConnectAvailability.AVAILABLE &&
                            hasPermissions &&
                            !isLoading
                ) {
                    Text("Refresh")
                }

                TextButton(
                    onClick = onManage
                ) {
                    Text("Manage")
                }
            }
        }
    ) {
        when {
            isLoading -> {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )

                    Text(
                        text = "Refreshing walking data…",
                        style =
                            MaterialTheme.typography
                                .bodyMedium
                    )
                }
            }

            availability !=
                HealthConnectAvailability.AVAILABLE -> {
                Text(
                    text =
                        "Health Connect is not currently available. Open Health to install, update, or review access.",
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            !hasPermissions -> {
                Text(
                    text =
                        "Walking access is not connected. Open Health to connect Google Fit through Health Connect.",
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            else -> {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
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
                        modifier =
                            Modifier.weight(1f)
                    )

                    RebuildMetricPill(
                        label = "miles",
                        value =
                            formatActivityMiles(
                                activity.distanceMiles
                            ),
                        modifier =
                            Modifier.weight(1f),
                        color =
                            MaterialTheme.colorScheme
                                .secondaryContainer,
                        contentColor =
                            MaterialTheme.colorScheme
                                .onSecondaryContainer
                    )

                    RebuildMetricPill(
                        label = "time",
                        value =
                            formatActivityTime(
                                activity.activityMinutes
                            ),
                        modifier =
                            Modifier.weight(1f),
                        color =
                            MaterialTheme.colorScheme
                                .tertiaryContainer,
                        contentColor =
                            MaterialTheme.colorScheme
                                .onTertiaryContainer
                    )
                }

                if (
                    sourceLabel ==
                        "Saved with today's record"
                ) {
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Showing the last snapshot saved for today. Tap Refresh for current Fit data.",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactActivityCard(
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    isLoading: Boolean,
    activity: HealthActivityData,
    sourceLabel: String?,
    onOpenActivitySettings: () -> Unit
) {
    RebuildSectionCard(
        title = "Today’s activity",
        subtitle =
            sourceLabel
                ?: "Steps, miles, and recorded activity time.",
        accentColor = RebuildTeal,
        trailing = {
            TextButton(
                onClick =
                    onOpenActivitySettings
            ) {
                Text("Manage")
            }
        }
    ) {
        when {
            isLoading -> {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )

                    Text(
                        text =
                            "Refreshing activity…",
                        style =
                            MaterialTheme.typography
                                .bodyMedium
                    )
                }
            }

            availability !=
                HealthConnectAvailability.AVAILABLE -> {
                Text(
                    text =
                        "Health Connect is not currently available. Open More to install, update, or review access.",
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            !hasPermissions -> {
                Text(
                    text =
                        "Activity access is not connected. Open More when you are ready to connect it.",
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            else -> {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
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
                        modifier =
                            Modifier.weight(1f)
                    )

                    RebuildMetricPill(
                        label = "miles",
                        value =
                            formatActivityMiles(
                                activity.distanceMiles
                            ),
                        modifier =
                            Modifier.weight(1f),
                        color =
                            MaterialTheme.colorScheme
                                .secondaryContainer,
                        contentColor =
                            MaterialTheme.colorScheme
                                .onSecondaryContainer
                    )

                    RebuildMetricPill(
                        label = "time",
                        value =
                            formatActivityTime(
                                activity.activityMinutes
                            ),
                        modifier =
                            Modifier.weight(1f),
                        color =
                            MaterialTheme.colorScheme
                                .tertiaryContainer,
                        contentColor =
                            MaterialTheme.colorScheme
                                .onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayDetailsCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        RebuildSectionCard(
            title = "More today",
            subtitle =
                if (expanded) {
                    "Daily anchors, medication checks, and journal."
                } else {
                    "Less-frequent daily controls stay out of the way until needed."
                },
            accentColor =
                MaterialTheme.colorScheme
                    .outline,
            trailing = {
                TextButton(
                    onClick = onToggle
                ) {
                    Text(
                        if (expanded) {
                            "Hide"
                        } else {
                            "Expand"
                        }
                    )
                }
            }
        ) {
            if (!expanded) {
                Text(
                    text =
                        "Expand only when you need the detailed daily controls.",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }

        if (expanded) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun TodayDetailToggleRow(
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(18.dp)
                )
                .clickable(
                    onClick = onClick
                ),
        shape =
            RoundedCornerShape(18.dp),
        color =
            MaterialTheme.colorScheme
                .surfaceVariant
                .copy(alpha = 0.56f)
    ) {
        Row(
            modifier =
                Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )

                Text(
                    text = summary,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Text(
                text =
                    if (expanded) {
                        "−"
                    } else {
                        "+"
                    },
                style =
                    MaterialTheme.typography
                        .headlineMedium,
                color =
                    MaterialTheme.colorScheme
                        .primary
            )
        }
    }
}

@Composable
private fun HistoryHubCard(
    isLoading: Boolean,
    onOpenCalendar: () -> Unit
) {
    RebuildSectionCard(
        title = "Calendar & day details",
        subtitle =
            "Open any saved day to review food, activity, mobility, water, pain, medication checks, and journal entries.",
        accentColor = RebuildBlue
    ) {
        Button(
            onClick = onOpenCalendar,
            enabled = !isLoading,
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    Modifier.width(10.dp)
                )
            }

            Text(
                if (isLoading) {
                    "Loading history…"
                } else {
                    "Open Calendar"
                }
            )
        }

        Text(
            text =
                "Deleting an entire day remains inside the calendar details screen with confirmation.",
            style =
                MaterialTheme.typography
                    .bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
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
private fun HeaderSection(
    todayDate: String,
    onOpenHistory: () -> Unit
) {
    val today = remember(todayDate) {
        LocalDate.parse(todayDate)
    }
    val dayFormatter =
        DateTimeFormatter.ofPattern("EEEE")
    val dateFormatter =
        DateTimeFormatter.ofPattern(
            "MMMM d, yyyy"
        )

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(28.dp),
        color = RebuildNavy,
        contentColor = Color.White,
        shadowElevation = 5.dp
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = 22.dp,
                    vertical = 18.dp
                ),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Daily Rebuild",
                    style =
                        MaterialTheme.typography
                            .headlineMedium,
                    color = Color.White
                )

                Text(
                    text =
                        today.format(
                            dayFormatter
                        ),
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    color =
                        Color.White.copy(
                            alpha = 0.94f
                        )
                )

                Text(
                    text =
                        today.format(
                            dateFormatter
                        ),
                    style =
                        MaterialTheme.typography
                            .bodyLarge,
                    color =
                        Color.White.copy(
                            alpha = 0.78f
                        )
                )
            }

            TextButton(
                onClick = onOpenHistory
            ) {
                Text(
                    text = "History",
                    color = Color.White
                )
            }
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
            "Steps, distance, and recorded activity time from Health Connect.",
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
                            label = "time",
                            value =
                                formatActivityTime(
                                    activity.activityMinutes
                                ),
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

private fun formatActivityTime(
    totalMinutes: Long
): String {
    if (totalMinutes <= 0L) {
        return "0m"
    }

    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L

    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
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