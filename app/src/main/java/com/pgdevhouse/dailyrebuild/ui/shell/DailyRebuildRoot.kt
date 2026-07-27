package com.pgdevhouse.dailyrebuild

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CarePlace
import com.pgdevhouse.dailyrebuild.data.local.CareProvider
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.local.PantryEssentialStatus
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
import com.pgdevhouse.dailyrebuild.data.repository.DailyRebuildRepositories
import com.pgdevhouse.dailyrebuild.domain.isSupportedFoodBarcode
import com.pgdevhouse.dailyrebuild.domain.normalizeFoodBarcode
import com.pgdevhouse.dailyrebuild.ui.food.BarcodeManualSelection
import com.pgdevhouse.dailyrebuild.ui.food.BarcodeSavePolicy
import com.pgdevhouse.dailyrebuild.ui.food.FoodBarcodeViewModel
import com.pgdevhouse.dailyrebuild.ui.food.LocalSavedFoodChoiceDialog
import com.pgdevhouse.dailyrebuild.ui.food.OnlineSavedFoodDecisionDialog
import com.pgdevhouse.dailyrebuild.ui.food.PantryViewModel
import com.pgdevhouse.dailyrebuild.ui.history.HistoryViewModel
import com.pgdevhouse.dailyrebuild.ui.navigation.AppNavigationViewModel
import com.pgdevhouse.dailyrebuild.ui.state.FeatureLoadFailure
import com.pgdevhouse.dailyrebuild.ui.state.captureFeatureLoad
import com.pgdevhouse.dailyrebuild.ui.state.summaryMessage
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DailyRebuildApp(
    healthRefreshToken: Int = 0
) {
    val context = LocalContext.current

    val database = remember {
        DailyRebuildDatabase.getDatabase(context)
    }

    val repositories = remember(database) {
        DailyRebuildRepositories.create(database)
    }

    val dailyRecordDao = repositories.dailyRecords
    val foodDao = repositories.food
    val mealDao = repositories.meals
    val dailyActivityDao = repositories.activity
    val mobilitySessionDao = repositories.mobility
    val showerLogDao = repositories.showers
    val migraineLogDao = repositories.migraines
    val meetingDao = repositories.meetings
    val careVisitDao = repositories.careVisits
    val careAppointmentDao = repositories.appointments
    val healthProfileDao = repositories.healthProfile

    val navigationViewModel: AppNavigationViewModel = viewModel()
    val barcodeViewModel: FoodBarcodeViewModel = viewModel()
    val pantryViewModel: PantryViewModel = viewModel(
        factory = PantryViewModel.factory(repositories.pantry)
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(repositories)
    )

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

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message =
                        if (granted) {
                            "Appointment notifications enabled."
                        } else {
                            "Appointment saved, but notification permission was not allowed."
                        }
                )
            }
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
    val selectedMainTab = navigationViewModel.selectedMainTab

    var showQuickWaterDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showQuickPainDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val selectedFoodSection = navigationViewModel.selectedFoodSection

    var currentCalorieGoal by remember {
        mutableStateOf<Int?>(null)
    }

    BackHandler(
        enabled = selectedMainTab != 0
    ) {
        navigationViewModel.selectMainTab(0)
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

    var manualBarcodeSavePolicy by remember {
        mutableStateOf(BarcodeSavePolicy.NORMAL)
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

    val pantryEssentials = pantryViewModel.state.items
    val isSavingPantryEssential = pantryViewModel.state.isWorking

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

    var careAppointments by remember {
        mutableStateOf<List<CareAppointment>>(emptyList())
    }

    val appointmentWorkflow = remember {
        AppointmentWorkflowState()
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
     * One daily pain value: the highest pain experienced so far today.
     * Existing back/shin database columns are retained for compatibility;
     * new saves store the highest value in backPain and zero in shinPain.
     */
    var highestPain by rememberSaveable {
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

    /* Global history is loaded and deleted by HistoryViewModel. */
    var showDailyHistoryDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val historyState = historyViewModel.state
    val isLoadingDailyHistory = historyState.isLoading
    val isDeletingDailyHistoryDay = historyState.isDeleting
    val dailyHistoryDays = historyState.days

    /* Incremented after deleting the active date so all daily fields reload. */
    var dayReloadToken by remember {
        mutableIntStateOf(0)
    }

    /*
     * Load today's daily record and food entries.
     */
    LaunchedEffect(todayDate, dayReloadToken) {
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

        highestPain = 0f

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

        val loadFailures = mutableListOf<FeatureLoadFailure>()

        suspend fun <T> loadFeature(
            feature: String,
            userMessage: String,
            block: suspend () -> T
        ): T? {
            val (value, failure) = captureFeatureLoad(
                feature = feature,
                userMessage = userMessage,
                block = block
            )
            failure?.let(loadFailures::add)
            return value
        }

        try {
            loadFeature(
                feature = "daily record",
                userMessage = "Could not load today’s daily record."
            ) {
                dailyRecordDao.getRecordByDate(todayDate)
            }?.let { savedRecord ->
                foodRecorded = savedRecord.foodRecorded
                walkCompleted = savedRecord.walkCompleted
                painRecorded = savedRecord.painRecorded
                mobilityCompleted = savedRecord.mobilityCompleted
                highestPain = maxOf(savedRecord.backPain, savedRecord.shinPain)
                plainReusableBottleCount = savedRecord.plainReusableBottleCount
                mioReusableBottleCount = savedRecord.mioReusableBottleCount
                plainDisposableBottleCount = savedRecord.plainDisposableBottleCount
                mioDisposableBottleCount = savedRecord.mioDisposableBottleCount
                morningAspirinTaken = savedRecord.morningAspirinTaken
                morningIbuprofenTaken = savedRecord.morningIbuprofenTaken
                morningNaproxenTaken = savedRecord.morningNaproxenTaken
                morningAcetaminophenTaken = savedRecord.morningAcetaminophenTaken
                nightIbuprofenTaken = savedRecord.nightIbuprofenTaken
                nightNaproxenTaken = savedRecord.nightNaproxenTaken
                nightAcetaminophenTaken = savedRecord.nightAcetaminophenTaken
                journalText = savedRecord.journalText
            }

            savedActivitySnapshot = loadFeature(
                feature = "saved activity",
                userMessage = "Could not load the saved walking snapshot."
            ) {
                dailyActivityDao.getSnapshotByDate(todayDate)
            }

            mobilitySessionsToday = loadFeature(
                feature = "mobility",
                userMessage = "Could not load today’s mobility sessions."
            ) {
                mobilitySessionDao.getSessionsForDate(todayDate)
            } ?: emptyList()

            if (mobilitySessionsToday.isNotEmpty()) {
                mobilityCompleted = true
            }

            val activeDate = LocalDate.parse(todayDate)
            val weekStart = activeDate.minusDays(
                (activeDate.dayOfWeek.value - 1).toLong()
            )
            val weekEnd = weekStart.plusDays(6)

            val weeklyShowerLogs = loadFeature(
                feature = "shower tracking",
                userMessage = "Could not load this week’s shower tracking."
            ) {
                showerLogDao.getLogsBetween(
                    weekStart.toString(),
                    weekEnd.toString()
                )
            } ?: emptyList()

            showerDatesThisWeek = weeklyShowerLogs.map { it.date }
            showeredToday = weeklyShowerLogs.any { it.date == todayDate }

            migraineLogs = loadFeature(
                feature = "migraine history",
                userMessage = "Could not load migraine and visual-aura history."
            ) {
                migraineLogDao.getAllLogs()
            } ?: emptyList()

            savedMeetings = loadFeature(
                feature = "saved meetings",
                userMessage = "Could not load saved meetings."
            ) {
                meetingDao.getActiveMeetings()
            } ?: emptyList()

            meetingAttendanceHistory = loadFeature(
                feature = "meeting attendance",
                userMessage = "Could not load meeting attendance."
            ) {
                meetingDao.getAllAttendance()
            } ?: emptyList()

            carePlaces = loadFeature(
                feature = "care places",
                userMessage = "Could not load saved care places."
            ) {
                careVisitDao.getActivePlaces()
            } ?: emptyList()

            careProviders = loadFeature(
                feature = "care providers",
                userMessage = "Could not load saved doctors and providers."
            ) {
                careVisitDao.getActiveProviders()
            } ?: emptyList()

            careVisits = loadFeature(
                feature = "care visits",
                userMessage = "Could not load completed care visits."
            ) {
                careVisitDao.getAllVisits()
            } ?: emptyList()

            careAppointments = loadFeature(
                feature = "appointments",
                userMessage = "Could not load upcoming appointments."
            ) {
                careAppointmentDao.getAllAppointments()
            } ?: emptyList()

            careAppointments.forEach { appointment ->
                AppointmentReminderScheduler.schedule(context, appointment)
            }

            foodEntries = loadFeature(
                feature = "today’s food",
                userMessage = "Could not load today’s food entries."
            ) {
                foodDao.getEntriesForDate(todayDate)
            } ?: emptyList()

            savedProducts = loadFeature(
                feature = "saved foods",
                userMessage = "Could not load Saved Foods."
            ) {
                foodDao.getAllProducts()
            } ?: emptyList()

            savedMeals = loadFeature(
                feature = "saved meals",
                userMessage = "Could not load Saved Meals."
            ) {
                mealDao.getAllMealsWithIngredients()
            } ?: emptyList()

            loadFeature(
                feature = "pantry essentials",
                userMessage = "Could not load Pantry Essentials."
            ) {
                pantryViewModel.refresh()
            }

            if (foodEntries.isNotEmpty()) {
                foodRecorded = true
            }

            loadFailures.summaryMessage()?.let { message ->
                snackbarHostState.showSnackbar(message)
            }
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
        remember(
            carePlaces,
            careVisits,
            careAppointments
        ) {
            val lastUseByPlaceId =
                buildMap<Long, Long> {
                    careVisits
                        .filter { it.placeId != null }
                        .forEach { visit ->
                            val placeId = visit.placeId ?: return@forEach
                            put(
                                placeId,
                                maxOf(
                                    get(placeId) ?: Long.MIN_VALUE,
                                    visit.startedAt
                                )
                            )
                        }

                    careAppointments
                        .filter { it.placeId != null }
                        .forEach { appointment ->
                            val placeId =
                                appointment.placeId
                                    ?: return@forEach
                            put(
                                placeId,
                                maxOf(
                                    get(placeId) ?: Long.MIN_VALUE,
                                    appointment.scheduledAt
                                )
                            )
                        }
                }

            carePlaces.sortedWith(
                compareByDescending<CarePlace> {
                    lastUseByPlaceId[it.id]
                        ?: Long.MIN_VALUE
                }.thenBy {
                    it.name.lowercase(Locale.US)
                }
            )
        }

    val upcomingCareAppointment =
        remember(careAppointments) {
            nextUpcomingAppointment(
                careAppointments
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

    val providersForSelectedAppointmentPlace =
        remember(
            appointmentWorkflow.selectedPlace,
            careProviders,
            careVisits,
            careAppointments
        ) {
            recentAppointmentProviders(
                place = appointmentWorkflow.selectedPlace,
                providers = careProviders,
                visits = careVisits,
                appointments = careAppointments
            )
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

                if (appointmentWorkflow.placeEditorActive) {
                    if (
                        appointmentWorkflow.continueAfterPlaceSave &&
                        savedPlace != null
                    ) {
                        appointmentWorkflow.selectedPlace = savedPlace
                        appointmentWorkflow.selectedProvider = null
                        appointmentWorkflow.showStart = false
                        appointmentWorkflow.showProviderPicker = true
                    } else {
                        appointmentWorkflow.showStart = true
                    }

                    appointmentWorkflow.placeEditorActive = false
                    appointmentWorkflow.continueAfterPlaceSave = false
                } else {
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
                }

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

                if (appointmentWorkflow.providerEditorActive) {
                    if (
                        appointmentWorkflow.continueAfterProviderSave &&
                        savedProvider != null
                    ) {
                        appointmentWorkflow.selectedProvider = savedProvider
                        appointmentWorkflow.editingAppointment = null
                        appointmentWorkflow.oneTimeAppointment = false
                        appointmentWorkflow.showProviderPicker = false
                        appointmentWorkflow.showEditor = true
                    } else {
                        appointmentWorkflow.showProviderPicker = true
                    }

                    appointmentWorkflow.providerEditorActive = false
                    appointmentWorkflow.continueAfterProviderSave = false
                } else {
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
                }

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
                val convertedAppointment =
                    appointmentWorkflow.convertingAppointment
                var savedVisitId = draft.id

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
                        savedVisitId =
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

                    convertedAppointment?.let { appointment ->
                        careAppointmentDao.updateAppointment(
                            appointment.copy(
                                status = "Completed",
                                convertedVisitId = savedVisitId,
                                updatedAt = now
                            )
                        )
                    }
                }

                careVisits = careVisitDao.getAllVisits()
                careAppointments =
                    careAppointmentDao.getAllAppointments()
                convertedAppointment?.let { appointment ->
                    AppointmentReminderScheduler.cancel(
                        context,
                        appointment.id
                    )
                }
                healthFeatureRefreshKey++

                showCareVisitEditorDialog = false
                showCareVisitStartDialog = false
                showCareProviderPickerDialog = false
                selectedCarePlaceForVisit = null
                selectedCareProviderForVisit = null
                careVisitBeingEdited = null
                isOneTimeCareVisit = false

                if (convertedAppointment != null) {
                    appointmentWorkflow.convertingAppointment = null
                    appointmentWorkflow.showHistory = true
                } else if (returnToCareHistoryAfterVisitSave) {
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
                database.withTransaction {
                    careVisitDao.deleteVisitById(visit.id)
                    careAppointmentDao.clearConvertedVisitLink(
                        visitId = visit.id,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                careVisits = careVisitDao.getAllVisits()
                careAppointments =
                    careAppointmentDao.getAllAppointments()
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

    fun requestAppointmentNotificationPermissionIfNeeded(
        remindersEnabled: Boolean
    ) {
        if (
            remindersEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    fun openAppointmentStart() {
        appointmentWorkflow.clearSelection()
        appointmentWorkflow.returnToHistoryAfterSave = false
        appointmentWorkflow.showHistory = false
        appointmentWorkflow.showStart = true
    }

    fun openAppointmentEditor(
        appointment: CareAppointment,
        returnToHistory: Boolean
    ) {
        appointmentWorkflow.editingAppointment = appointment
        appointmentWorkflow.selectedPlace =
            appointment.placeId?.let { id ->
                carePlaces.firstOrNull { it.id == id }
            }
        appointmentWorkflow.selectedProvider =
            appointment.providerId?.let { id ->
                careProviders.firstOrNull { it.id == id }
            }
        appointmentWorkflow.oneTimeAppointment =
            appointment.placeId == null
        appointmentWorkflow.returnToHistoryAfterSave =
            returnToHistory
        appointmentWorkflow.showHistory = false
        appointmentWorkflow.showEditor = true
    }

    fun saveCareAppointment(
        draft: AppointmentDraft
    ) {
        coroutineScope.launch {
            appointmentWorkflow.isSaving = true

            try {
                val duplicate =
                    careAppointmentDao.findPotentialDuplicate(
                        date = draft.date,
                        placeName = draft.placeName,
                        providerName = draft.providerName,
                        scheduledAt = draft.scheduledAt,
                        excludedId = draft.id
                    )

                if (duplicate != null) {
                    snackbarHostState.showSnackbar(
                        message =
                            "A similar appointment is already scheduled within 30 minutes."
                    )
                    return@launch
                }

                val now = System.currentTimeMillis()
                val appointment =
                    CareAppointment(
                        id = draft.id,
                        placeId = draft.placeId,
                        providerId = draft.providerId,
                        date = draft.date,
                        scheduledAt = draft.scheduledAt,
                        status = draft.status,
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
                        reasonForAppointment =
                            draft.reasonForAppointment,
                        transportationMode =
                            draft.transportationMode,
                        transportationDetails =
                            draft.transportationDetails,
                        leaveByAt = draft.leaveByAt,
                        transportationConfirmed =
                            draft.transportationConfirmed,
                        questionsToAsk = draft.questionsToAsk,
                        documentsToBring = draft.documentsToBring,
                        preparationNotes = draft.preparationNotes,
                        remindOneDayBefore =
                            draft.remindOneDayBefore,
                        remindTwoHoursBefore =
                            draft.remindTwoHoursBefore,
                        convertedVisitId = draft.convertedVisitId,
                        createdAt = draft.createdAt,
                        updatedAt = now
                    )

                val savedId =
                    if (draft.id == 0L) {
                        careAppointmentDao.insertAppointment(
                            appointment
                        )
                    } else {
                        careAppointmentDao.updateAppointment(
                            appointment
                        )
                        draft.id
                    }

                val savedAppointment =
                    careAppointmentDao.getAppointmentById(
                        savedId
                    ) ?: appointment.copy(id = savedId)

                AppointmentReminderScheduler.schedule(
                    context,
                    savedAppointment
                )

                careAppointments =
                    careAppointmentDao.getAllAppointments()

                requestAppointmentNotificationPermissionIfNeeded(
                    remindersEnabled =
                        savedAppointment.scheduledAt > now &&
                            savedAppointment.status in
                                setOf("Scheduled", "Confirmed") &&
                            (
                                savedAppointment.remindOneDayBefore ||
                                    savedAppointment.remindTwoHoursBefore
                            )
                )

                appointmentWorkflow.showEditor = false
                appointmentWorkflow.showStart = false
                appointmentWorkflow.showProviderPicker = false
                appointmentWorkflow.clearSelection()

                if (appointmentWorkflow.returnToHistoryAfterSave) {
                    appointmentWorkflow.showHistory = true
                }
                appointmentWorkflow.returnToHistoryAfterSave = false

                snackbarHostState.showSnackbar(
                    message =
                        if (draft.id == 0L) {
                            "Appointment scheduled."
                        } else {
                            "Appointment updated."
                        }
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not save the appointment."
                )
            } finally {
                appointmentWorkflow.isSaving = false
            }
        }
    }

    fun deleteCareAppointment(
        appointment: CareAppointment
    ) {
        coroutineScope.launch {
            appointmentWorkflow.isDeleting = true

            try {
                AppointmentReminderScheduler.cancel(
                    context,
                    appointment.id
                )
                careAppointmentDao.deleteAppointmentById(
                    appointment.id
                )
                careAppointments =
                    careAppointmentDao.getAllAppointments()
                appointmentWorkflow.deletingAppointment = null

                snackbarHostState.showSnackbar(
                    message = "Appointment removed."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not remove the appointment."
                )
            } finally {
                appointmentWorkflow.isDeleting = false
            }
        }
    }

    fun convertAppointmentToVisit(
        appointment: CareAppointment
    ) {
        appointmentWorkflow.convertingAppointment = appointment
        appointmentWorkflow.showHistory = false
        selectedCarePlaceForVisit =
            appointment.placeId?.let { id ->
                carePlaces.firstOrNull { it.id == id }
            }
        selectedCareProviderForVisit =
            appointment.providerId?.let { id ->
                careProviders.firstOrNull { it.id == id }
            }
        careVisitBeingEdited = null
        isOneTimeCareVisit = appointment.placeId == null
        returnToCareHistoryAfterVisitSave = false
        showCareVisitEditorDialog = true
    }

    fun openDailyHistory() {
        showDailyHistoryDialog = true
        historyViewModel.refresh { message ->
            if (message != null) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
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

    fun openBarcodeManualSelection(
        selection: BarcodeManualSelection
    ) {
        scannedFoodPrefill = selection.prefill
        manualBarcodeSavePolicy = selection.policy
        isCreatingFoodForMeal = selection.forMealBuilder
        showManualFoodDialog = true
    }

    fun lookupFoodBarcode(
        barcodeText: String,
        forMealBuilder: Boolean,
        skipLocalCheck: Boolean = false
    ) {
        val verifiedBarcode = normalizeFoodBarcode(barcodeText)

        if (!isSupportedFoodBarcode(verifiedBarcode)) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Enter the 8, 12, or 13 digits printed below the barcode."
                )
            }
            return
        }

        isCreatingFoodForMeal = forMealBuilder
        lastScannedBarcode = verifiedBarcode
        isScanningBarcode = true

        val productIdBeingEdited =
            if (isEditingSavedFood) scannedFoodPrefill?.productId else null

        coroutineScope.launch {
            try {
                val existingProduct =
                    foodDao.getProductByBarcode(verifiedBarcode)

                if (!skipLocalCheck && existingProduct != null) {
                    if (
                        productIdBeingEdited != null &&
                        productIdBeingEdited != existingProduct.id
                    ) {
                        snackbarHostState.showSnackbar(
                            message = "That barcode already belongs to ${existingProduct.name}."
                        )
                        return@launch
                    }

                    barcodeViewModel.beginLocalChoice(
                        barcode = verifiedBarcode,
                        product = existingProduct,
                        forMealBuilder = forMealBuilder
                    )
                    return@launch
                }

                when (
                    val result = OpenFoodFactsLookup.findProduct(verifiedBarcode)
                ) {
                    is FoodLookupResult.Found -> {
                        val onlinePrefill = result.food.copy(
                            productId = productIdBeingEdited,
                            barcode = verifiedBarcode
                        )

                        if (barcodeViewModel.localMatch != null) {
                            barcodeViewModel.recordOnlineResult(onlinePrefill)
                        } else {
                            scannedFoodPrefill = onlinePrefill
                            manualBarcodeSavePolicy = BarcodeSavePolicy.NORMAL
                            showManualFoodDialog = true
                            snackbarHostState.showSnackbar("Product found.")
                        }
                    }

                    FoodLookupResult.NotFound -> {
                        if (barcodeViewModel.localMatch != null) {
                            snackbarHostState.showSnackbar(
                                "No online result was found. Your saved food is still available."
                            )
                        } else {
                            scannedFoodPrefill = ScannedFoodPrefill(
                                productId = productIdBeingEdited,
                                barcode = verifiedBarcode
                            )
                            manualBarcodeSavePolicy = BarcodeSavePolicy.NORMAL
                            showManualFoodDialog = true
                            snackbarHostState.showSnackbar(
                                "Product not found. Enter its label manually."
                            )
                        }
                    }

                    is FoodLookupResult.Failed -> {
                        if (barcodeViewModel.localMatch != null) {
                            snackbarHostState.showSnackbar(
                                "Online lookup failed. Your saved food is still available."
                            )
                        } else {
                            scannedFoodPrefill = ScannedFoodPrefill(
                                productId = productIdBeingEdited,
                                barcode = verifiedBarcode
                            )
                            manualBarcodeSavePolicy = BarcodeSavePolicy.NORMAL
                            showManualFoodDialog = true
                            snackbarHostState.showSnackbar(
                                "Lookup failed. You can enter the label manually."
                            )
                        }
                    }
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not check this barcode. Try again or enter the food manually."
                )
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
                        backPain = highestPain,
                        shinPain = 0f,
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

    fun showPantryResult(message: String?) {
        if (message == null) return
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun savePantryEssential(item: PantryEssential) {
        pantryViewModel.save(item, ::showPantryResult)
    }

    fun deletePantryEssential(item: PantryEssential) {
        pantryViewModel.delete(item, ::showPantryResult)
    }

    fun updatePantryEssentialStatus(
        item: PantryEssential,
        status: String
    ) {
        pantryViewModel.updateStatus(item, status, ::showPantryResult)
    }

    fun markNeededPantryPurchased() {
        pantryViewModel.markNeededPurchased(::showPantryResult)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isLoading) {
                DailyRebuildBottomNavigation(
                    selectedTab = selectedMainTab,
                    onTabSelected = {
                        navigationViewModel.selectMainTab(it)
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
                0 -> TodayScreen(
                    state = TodayScreenState(
                        date = todayDate,
                        completedTasks = completedTasks,
                        foodRecorded = foodRecorded,
                        walkCompleted = walkCompleted,
                        painRecorded = painRecorded,
                        mobilityCompleted = mobilityCompleted,
                        highestPain = highestPain,
                        calories = totalCaloriesToday,
                        calorieGoal = currentCalorieGoal,
                        waterOunces = totalWaterOunces,
                        showerDatesThisWeek = showerDatesThisWeek,
                        showeredToday = showeredToday,
                        showersThisWeek = showerCountThisWeek,
                        meetingsThisWeek = meetingCountThisWeek,
                        appointment = upcomingCareAppointment,
                        activity = displayedActivity,
                        activitySourceLabel = activitySourceLabel,
                        morningAspirinTaken = morningAspirinTaken,
                        morningIbuprofenTaken = morningIbuprofenTaken,
                        morningNaproxenTaken = morningNaproxenTaken,
                        morningAcetaminophenTaken = morningAcetaminophenTaken,
                        nightIbuprofenTaken = nightIbuprofenTaken,
                        nightNaproxenTaken = nightNaproxenTaken,
                        nightAcetaminophenTaken = nightAcetaminophenTaken,
                        journalText = journalText,
                        isSaving = isSaving
                    ),
                    actions = TodayScreenActions(
                        onOpenHistory = { openDailyHistory() },
                        onOpenFood = { navigationViewModel.selectMainTab(1) },
                        onOpenWater = { showQuickWaterDialog = true },
                        onOpenMobility = { navigationViewModel.selectMainTab(2) },
                        onOpenPain = { showQuickPainDialog = true },
                        onOpenMeetings = { navigationViewModel.selectMainTab(3) },
                        onOpenHealth = { navigationViewModel.selectMainTab(4) },
                        onScheduleAppointment = { openAppointmentStart() },
                        onViewAppointment = { appointment ->
                            openAppointmentEditor(
                                appointment,
                                returnToHistory = false
                            )
                        },
                        onLogMeeting = { showMeetingPickerDialog = true },
                        onLogShower = { logShowerToday() },
                        onRemoveShower = { removeTodayShower() },
                        onFoodRecordedChange = { foodRecorded = it },
                        onWalkCompletedChange = { walkCompleted = it },
                        onPainRecordedChange = { painRecorded = it },
                        onMobilityCompletedChange = { mobilityCompleted = it },
                        onMorningAspirinChange = { morningAspirinTaken = it },
                        onMorningIbuprofenChange = { morningIbuprofenTaken = it },
                        onMorningNaproxenChange = { morningNaproxenTaken = it },
                        onMorningAcetaminophenChange = { morningAcetaminophenTaken = it },
                        onNightIbuprofenChange = { nightIbuprofenTaken = it },
                        onNightNaproxenChange = { nightNaproxenTaken = it },
                        onNightAcetaminophenChange = { nightAcetaminophenTaken = it },
                        onJournalTextChange = { journalText = it },
                        onSaveToday = saveToday
                    ),
                    modifier = Modifier.padding(innerPadding)
                )

                1 -> FoodHubScreen(
                    state = FoodHubState(
                        selectedSection = selectedFoodSection,
                        totalWaterOunces = totalWaterOunces,
                        totalBottleCount =
                            plainReusableBottleCount +
                                mioReusableBottleCount +
                                plainDisposableBottleCount +
                                mioDisposableBottleCount,
                        entries = foodEntries,
                        savedFoodCount = savedProducts.size,
                        savedMealCount = savedMeals.size,
                        lastScannedBarcode = lastScannedBarcode,
                        isScanningBarcode = isScanningBarcode,
                        currentCalorieGoal = currentCalorieGoal,
                        pantryItems = pantryEssentials,
                        isSavingPantry = isSavingPantryEssential
                    ),
                    actions = FoodHubActions(
                        onSectionChange = { navigationViewModel.selectFoodSection(it) },
                        onOpenHistory = { openDailyHistory() },
                        onAddWater = { showQuickWaterDialog = true },
                        onScanFood = {
                            startFoodBarcodeScan(forMealBuilder = false)
                        },
                        onAddFoodManually = {
                            isCreatingFoodForMeal = false
                            scannedFoodPrefill = null
                            showManualFoodDialog = true
                        },
                        onOpenSavedFoods = { openSavedFoodsScreen() },
                        onBuildMeal = { openMealBuilderScreen() },
                        onOpenSavedMeals = { openSavedMealsScreen() },
                        onDeleteEntry = { deleteFoodEntry(it) },
                        onDeleteMealLog = { deleteMealLog(it) },
                        onSavePantryItem = { item -> savePantryEssential(item) },
                        onDeletePantryItem = { item -> deletePantryEssential(item) },
                        onPantryStatusChange = { item, status ->
                            updatePantryEssentialStatus(item, status)
                        },
                        onMarkNeededPurchased = { markNeededPantryPurchased() }
                    ),
                    modifier = Modifier.padding(innerPadding)
                )

                2 -> MobilityHubScreen(
                    state = MobilityHubState(
                        selectedSection = navigationViewModel.selectedMobilitySection,
                        availability = healthAvailability,
                        hasPermissions = hasHealthPermissions,
                        isLoadingActivity = isLoadingHealthActivity,
                        activity = displayedActivity,
                        sourceLabel = activitySourceLabel,
                        sessions = mobilitySessionsToday,
                        isSaving = isSavingMobility
                    ),
                    actions = MobilityHubActions(
                        onSectionChange = { navigationViewModel.selectMobilitySection(it) },
                        onOpenHistory = { openDailyHistory() },
                        onRefresh = {
                            refreshHealthActivity(showFeedback = true)
                        },
                        onManageHealth = { navigationViewModel.selectMainTab(4) },
                        onSaveSession = { saveMobilitySession(it) },
                        onDeleteSession = { deleteMobilitySession(it) }
                    ),
                    modifier = Modifier.padding(innerPadding)
                )

                3 -> MeetingsHubScreen(
                    weeklyAttendance = weeklyMeetingAttendance,
                    isSaving = isSavingMeeting,
                    onOpenHistory = { openDailyHistory() },
                    onLogMeeting = { showMeetingPickerDialog = true },
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
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                else -> HealthHubScreen(
                    state = HealthHubState(
                        appointments = careAppointments,
                        visits = careVisits,
                        migraineLogs = migraineLogs,
                        availability = healthAvailability,
                        hasPermissions = hasHealthPermissions,
                        isLoadingActivity = isLoadingHealthActivity,
                        activity = displayedActivity,
                        activitySourceLabel = activitySourceLabel
                    ),
                    actions = HealthHubActions(
                        onOpenHistory = { openDailyHistory() },
                        onScheduleAppointment = { openAppointmentStart() },
                        onOpenAppointmentHistory = {
                            appointmentWorkflow.showHistory = true
                        },
                        onViewAppointment = { appointment ->
                            openAppointmentEditor(
                                appointment,
                                returnToHistory = false
                            )
                        },
                        onLogVisit = { showCareVisitStartDialog = true },
                        onOpenVisitHistory = {
                            showCareVisitHistoryDialog = true
                        },
                        onLogMigraine = { showMigraineLogDialog = true },
                        onDeleteMigraine = { deleteMigraineEvent(it) },
                        onLogPain = { showQuickPainDialog = true },
                        onConnectHealth = {
                            healthPermissionsLauncher.launch(
                                HealthConnectManager.permissions
                            )
                        },
                        onRefreshActivity = {
                            refreshHealthActivity(showFeedback = true)
                        },
                        onManageAccess = {
                            healthConnectManager.openHealthConnectSettings()
                        },
                        onInstallOrUpdate = {
                            healthConnectManager.openInstallOrUpdate()
                        }
                    ),
                    profileContent = {
                        key(healthFeatureRefreshKey) {
                            HealthProfileFeature(repositories)
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (appointmentWorkflow.showStart) {
        AppointmentStartDialog(
            recentPlaces = recentCarePlaces,
            appointments = careAppointments,
            visits = careVisits,
            onSelectPlace = { place ->
                appointmentWorkflow.selectedPlace = place
                appointmentWorkflow.selectedProvider = null
                appointmentWorkflow.showStart = false
                appointmentWorkflow.showProviderPicker = true
            },
            onEditPlace = { place ->
                carePlaceBeingEdited = place
                appointmentWorkflow.placeEditorActive = true
                appointmentWorkflow.continueAfterPlaceSave = false
                appointmentWorkflow.showStart = false
                showCarePlaceEditorDialog = true
            },
            onAddPlace = {
                carePlaceBeingEdited = null
                appointmentWorkflow.placeEditorActive = true
                appointmentWorkflow.continueAfterPlaceSave = true
                appointmentWorkflow.showStart = false
                showCarePlaceEditorDialog = true
            },
            onOneTimeAppointment = {
                appointmentWorkflow.clearSelection()
                appointmentWorkflow.oneTimeAppointment = true
                appointmentWorkflow.returnToHistoryAfterSave = false
                appointmentWorkflow.showStart = false
                appointmentWorkflow.showEditor = true
            },
            onDismiss = {
                appointmentWorkflow.showStart = false
            }
        )
    }

    if (
        appointmentWorkflow.showProviderPicker &&
        appointmentWorkflow.selectedPlace != null
    ) {
        CareProviderPickerDialog(
            place = appointmentWorkflow.selectedPlace!!,
            providers = providersForSelectedAppointmentPlace,
            onSelectProvider = { provider ->
                appointmentWorkflow.selectedProvider = provider
                appointmentWorkflow.editingAppointment = null
                appointmentWorkflow.oneTimeAppointment = false
                appointmentWorkflow.returnToHistoryAfterSave = false
                appointmentWorkflow.showProviderPicker = false
                appointmentWorkflow.showEditor = true
            },
            onEditProvider = { provider ->
                careProviderBeingEdited = provider
                appointmentWorkflow.providerEditorActive = true
                appointmentWorkflow.continueAfterProviderSave = false
                appointmentWorkflow.showProviderPicker = false
                showCareProviderEditorDialog = true
            },
            onAddProvider = {
                careProviderBeingEdited = null
                appointmentWorkflow.providerEditorActive = true
                appointmentWorkflow.continueAfterProviderSave = true
                appointmentWorkflow.showProviderPicker = false
                showCareProviderEditorDialog = true
            },
            onContinueWithoutProvider = {
                appointmentWorkflow.selectedProvider = null
                appointmentWorkflow.editingAppointment = null
                appointmentWorkflow.oneTimeAppointment = false
                appointmentWorkflow.returnToHistoryAfterSave = false
                appointmentWorkflow.showProviderPicker = false
                appointmentWorkflow.showEditor = true
            },
            onBack = {
                appointmentWorkflow.showProviderPicker = false
                appointmentWorkflow.selectedPlace = null
                appointmentWorkflow.showStart = true
            },
            onDismiss = {
                appointmentWorkflow.showProviderPicker = false
                appointmentWorkflow.selectedPlace = null
            },
            prompt = "Who will you see?"
        )
    }

    if (appointmentWorkflow.showEditor) {
        AppointmentEditorDialog(
            savedPlace = appointmentWorkflow.selectedPlace,
            savedProvider = appointmentWorkflow.selectedProvider,
            existingAppointment =
                appointmentWorkflow.editingAppointment,
            isOneTimeAppointment =
                appointmentWorkflow.oneTimeAppointment,
            isSaving = appointmentWorkflow.isSaving,
            onSave = {
                saveCareAppointment(it)
            },
            onDismiss = {
                if (!appointmentWorkflow.isSaving) {
                    appointmentWorkflow.showEditor = false
                    appointmentWorkflow.clearSelection()
                    if (
                        appointmentWorkflow
                            .returnToHistoryAfterSave
                    ) {
                        appointmentWorkflow.showHistory = true
                    }
                    appointmentWorkflow.returnToHistoryAfterSave = false
                }
            }
        )
    }

    if (appointmentWorkflow.showHistory) {
        AppointmentHistoryDialog(
            appointments = careAppointments,
            onEdit = { appointment ->
                openAppointmentEditor(
                    appointment,
                    returnToHistory = true
                )
            },
            onConvertToVisit = { appointment ->
                convertAppointmentToVisit(appointment)
            },
            onDelete = { appointment ->
                appointmentWorkflow.deletingAppointment =
                    appointment
            },
            onSchedule = {
                appointmentWorkflow.showHistory = false
                openAppointmentStart()
            },
            onDismiss = {
                appointmentWorkflow.showHistory = false
            }
        )
    }

    appointmentWorkflow.deletingAppointment?.let { appointment ->
        DeleteAppointmentDialog(
            appointment = appointment,
            isDeleting = appointmentWorkflow.isDeleting,
            onConfirm = {
                deleteCareAppointment(appointment)
            },
            onDismiss = {
                if (!appointmentWorkflow.isDeleting) {
                    appointmentWorkflow.deletingAppointment = null
                }
            }
        )
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

                    if (appointmentWorkflow.placeEditorActive) {
                        appointmentWorkflow.placeEditorActive = false
                        appointmentWorkflow.continueAfterPlaceSave = false
                        appointmentWorkflow.showStart = true
                    } else {
                        continueVisitAfterPlaceSave = false
                        showCareVisitStartDialog = true
                    }
                }
            }
        )
    }

    val carePlaceForProviderEditor =
        if (appointmentWorkflow.providerEditorActive) {
            appointmentWorkflow.selectedPlace
        } else {
            selectedCarePlaceForVisit
        }

    if (
        showCareProviderEditorDialog &&
        carePlaceForProviderEditor != null
    ) {
        CareProviderEditorDialog(
            place = carePlaceForProviderEditor,
            existingProvider = careProviderBeingEdited,
            isSaving = isSavingCareVisit,
            onSave = {
                saveCareProvider(it)
            },
            onDismiss = {
                if (!isSavingCareVisit) {
                    showCareProviderEditorDialog = false
                    careProviderBeingEdited = null

                    if (appointmentWorkflow.providerEditorActive) {
                        appointmentWorkflow.providerEditorActive = false
                        appointmentWorkflow.continueAfterProviderSave = false
                        appointmentWorkflow.showProviderPicker = true
                    } else {
                        continueVisitAfterProviderSave = false
                        showCareProviderPickerDialog = true
                    }
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

                    if (
                        appointmentWorkflow
                            .convertingAppointment != null
                    ) {
                        appointmentWorkflow.convertingAppointment = null
                        appointmentWorkflow.showHistory = true
                    } else if (returnToCareHistoryAfterVisitSave) {
                        showCareVisitHistoryDialog = true
                    }
                    returnToCareHistoryAfterVisitSave = false
                }
            },
            appointmentPrefill =
                appointmentWorkflow.convertingAppointment
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
        HighestPainDialog(
            currentHighestPain = highestPain,
            wasRecordedToday = painRecorded,
            onSave = { value ->
                highestPain = value
                painRecorded = true
                showQuickPainDialog = false
            },
            onDismiss = {
                showQuickPainDialog = false
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

    barcodeViewModel.localMatch?.let { localProduct ->
        if (barcodeViewModel.showLocalChoice) {
            LocalSavedFoodChoiceDialog(
                product = localProduct,
                isLookingUp = isScanningBarcode,
                onUseSavedFood = {
                    barcodeViewModel.chooseLocal()
                        ?.let(::openBarcodeManualSelection)
                },
                onLookupAnyway = {
                    lookupFoodBarcode(
                        barcodeText = barcodeViewModel.verifiedBarcode.orEmpty(),
                        forMealBuilder = barcodeViewModel.forMealBuilder,
                        skipLocalCheck = true
                    )
                },
                onCancel = {
                    barcodeViewModel.cancel()
                    isScanningBarcode = false
                }
            )
        }

        val onlineProduct = barcodeViewModel.onlineResult
        if (barcodeViewModel.showOnlineChoice && onlineProduct != null) {
            OnlineSavedFoodDecisionDialog(
                local = localProduct,
                online = onlineProduct,
                allowOnlineOnce = !barcodeViewModel.forMealBuilder,
                onUseOnlineOnce = {
                    barcodeViewModel.chooseOnlineOnce()
                        ?.let(::openBarcodeManualSelection)
                },
                onUpdateSavedFood = {
                    barcodeViewModel.chooseOnlineAndUpdate()
                        ?.let(::openBarcodeManualSelection)
                },
                onKeepLocal = {
                    barcodeViewModel.chooseLocal()
                        ?.let(::openBarcodeManualSelection)
                },
                onCancel = barcodeViewModel::cancel
            )
        }
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
            selectedFilter = historyState.selectedFilter,
            onFilterChange = historyViewModel::selectFilter,
            isLoading = isLoadingDailyHistory,
            isDeletingDay =
                isDeletingDailyHistoryDay,

            onDeleteDay = { day ->
                historyViewModel.deleteDay(day) { message, success ->
                    coroutineScope.launch {
                        if (success) {
                            day.careAppointments.forEach { appointment ->
                                AppointmentReminderScheduler.cancel(
                                    context,
                                    appointment.id
                                )
                            }

                            val activeDate = LocalDate.parse(todayDate)
                            val weekStart = activeDate.minusDays(
                                (activeDate.dayOfWeek.value - 1).toLong()
                            )
                            val weekEnd = weekStart.plusDays(6)

                            showerDatesThisWeek =
                                showerLogDao.getLogsBetween(
                                    weekStart.toString(),
                                    weekEnd.toString()
                                ).map { it.date }

                            migraineLogs = migraineLogDao.getAllLogs()
                            meetingAttendanceHistory = meetingDao.getAllAttendance()
                            careVisits = careVisitDao.getAllVisits()
                            careAppointments = careAppointmentDao.getAllAppointments()

                            if (day.date == todayDate) {
                                dayReloadToken++
                            }
                        }

                        snackbarHostState.showSnackbar(message)
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
                    manualBarcodeSavePolicy = BarcodeSavePolicy.NORMAL
                    barcodeViewModel.resetSavePolicy()
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
                        val savePolicyUsed = manualBarcodeSavePolicy
                        val preserveSavedProduct =
                            savePolicyUsed ==
                                BarcodeSavePolicy.USE_LOCAL_WITHOUT_UPDATE ||
                                savePolicyUsed ==
                                    BarcodeSavePolicy.USE_ONLINE_ONCE

                        val productId =
                            if (preserveSavedProduct) {
                                existingProduct?.id
                                    ?: error(
                                        "The saved food was removed before the online result was logged."
                                    )
                            } else if (existingProduct == null) {
                                foodDao.addProduct(
                                    draft.product.copy(id = 0)
                                )
                            } else {
                                foodDao.updateProduct(
                                    draft.product.copy(
                                        id = existingProduct.id,
                                        createdAt = existingProduct.createdAt,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                existingProduct.id
                            }

                        savedProducts =
                            foodDao.getAllProducts()

                        if (isEditingSavedFood) {
                            showManualFoodDialog = false
                            scannedFoodPrefill = null
                            manualBarcodeSavePolicy = BarcodeSavePolicy.NORMAL
                            barcodeViewModel.resetSavePolicy()
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
                            manualBarcodeSavePolicy = BarcodeSavePolicy.NORMAL
                            barcodeViewModel.resetSavePolicy()
                            isCreatingFoodForMeal = false

                            snackbarHostState
                                .showSnackbar(
                                    message =
                                        if (
                                            savePolicyUsed ==
                                                BarcodeSavePolicy.USE_LOCAL_WITHOUT_UPDATE
                                        ) {
                                            "Saved food selected. Choose it as a meal ingredient."
                                        } else {
                                            "Saved food created. Choose it as a meal ingredient."
                                        }
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

                        showManualFoodDialog = false
                        scannedFoodPrefill = null
                        manualBarcodeSavePolicy = BarcodeSavePolicy.NORMAL
                        barcodeViewModel.resetSavePolicy()

                        snackbarHostState.showSnackbar(
                            message =
                                when (savePolicyUsed) {
                                    BarcodeSavePolicy.USE_LOCAL_WITHOUT_UPDATE ->
                                        "Food added using your saved local food. Saved Food was not changed."
                                    BarcodeSavePolicy.USE_ONLINE_ONCE ->
                                        "Food added using the online values. Saved Food was not changed."
                                    BarcodeSavePolicy.UPDATE_LOCAL ->
                                        "Saved Food updated and food added."
                                    BarcodeSavePolicy.NORMAL ->
                                        "Food added."
                                }
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

