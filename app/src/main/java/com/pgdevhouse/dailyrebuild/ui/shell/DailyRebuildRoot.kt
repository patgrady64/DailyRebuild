package com.pgdevhouse.dailyrebuild

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceTasks
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import com.pgdevhouse.dailyrebuild.data.local.IopMissedOccurrence
import kotlinx.coroutines.Job
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
import com.pgdevhouse.dailyrebuild.data.preferences.AppPreferencesRepository
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferenceIds
import com.pgdevhouse.dailyrebuild.domain.isSupportedFoodBarcode
import com.pgdevhouse.dailyrebuild.domain.normalizeFoodBarcode
import com.pgdevhouse.dailyrebuild.ui.food.BarcodeManualSelection
import com.pgdevhouse.dailyrebuild.ui.food.BarcodeSavePolicy
import com.pgdevhouse.dailyrebuild.ui.food.FoodBarcodeViewModel
import com.pgdevhouse.dailyrebuild.ui.food.LocalSavedFoodChoiceDialog
import com.pgdevhouse.dailyrebuild.ui.food.OnlineSavedFoodDecisionDialog
import com.pgdevhouse.dailyrebuild.ui.food.PantryViewModel
import com.pgdevhouse.dailyrebuild.ui.history.HistoryViewModel
import com.pgdevhouse.dailyrebuild.ui.stats.StatsViewModel
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
    val lifeMaintenanceDao = repositories.lifeMaintenance
    val iopGroupDao = repositories.iopGroups
    val iopAttendanceDao = repositories.iopAttendance

    val appPreferencesRepository = remember(context) {
        AppPreferencesRepository(context)
    }

    var appPreferences by remember {
        mutableStateOf(appPreferencesRepository.load())
    }

    var showGlobalSearch by rememberSaveable {
        mutableStateOf(false)
    }

    var globalSearchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var globalSearchFilter by remember {
        mutableStateOf(GlobalSearchFilter.ALL)
    }

    var globalSearchSnapshot by remember {
        mutableStateOf(GlobalSearchSnapshot())
    }

    var isLoadingGlobalSearch by remember {
        mutableStateOf(false)
    }

    var recentGlobalSearches by remember {
        mutableStateOf(appPreferencesRepository.loadRecentSearches())
    }

    var requestedHealthFeature by remember {
        mutableStateOf<String?>(null)
    }

    var requestedHealthFeatureToken by remember {
        mutableIntStateOf(0)
    }

    var selectedMoreFeature by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val navigationViewModel: AppNavigationViewModel = viewModel()
    val barcodeViewModel: FoodBarcodeViewModel = viewModel()
    val pantryViewModel: PantryViewModel = viewModel(
        factory = PantryViewModel.factory(repositories.pantry)
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(repositories)
    )
    val statsViewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.factory(repositories, appPreferences)
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

    var dataQualityWarnings by remember {
        mutableStateOf<List<DataQualityWarning>>(emptyList())
    }

    var keptDataQualityWarningIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    var ignoredDataQualityValueCount by remember {
        mutableIntStateOf(
            appPreferencesRepository.loadIgnoredDataQualitySignatures().size
        )
    }

    var dataQualityRefreshToken by remember {
        mutableIntStateOf(0)
    }

    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            notificationPermissionGranted = granted
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message =
                        if (granted) {
                            "Daily Rebuild notifications are allowed."
                        } else {
                            "Android did not allow notifications. You can change this later in Settings."
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

    var showClearWaterConfirmation by rememberSaveable {
        mutableStateOf(false)
    }

    var showQuickPainDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var currentCalorieGoal by remember {
        mutableStateOf<Int?>(null)
    }

    BackHandler(
        enabled = showGlobalSearch
    ) {
        showGlobalSearch = false
    }

    BackHandler(
        enabled = !showGlobalSearch &&
            selectedMainTab == AppNavigationViewModel.MORE_TAB &&
            selectedMoreFeature != null
    ) {
        selectedMoreFeature = null
    }

    BackHandler(
        enabled = !showGlobalSearch &&
            selectedMainTab == AppNavigationViewModel.LOG_TAB &&
            navigationViewModel.selectedLogSection != AppNavigationViewModel.LOG_HOME_SECTION
    ) {
        navigationViewModel.selectLogSection(AppNavigationViewModel.LOG_HOME_SECTION)
    }

    BackHandler(
        enabled = !showGlobalSearch &&
            selectedMainTab != AppNavigationViewModel.TODAY_TAB &&
            !(
                selectedMainTab == AppNavigationViewModel.MORE_TAB &&
                    selectedMoreFeature != null
                ) &&
            !(
                selectedMainTab == AppNavigationViewModel.LOG_TAB &&
                    navigationViewModel.selectedLogSection != AppNavigationViewModel.LOG_HOME_SECTION
                )
    ) {
        navigationViewModel.selectMainTab(AppNavigationViewModel.TODAY_TAB)
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

    /*
     * Full food history is kept in memory for the Today screen's recent and
     * frequently used shortcuts. It is refreshed after every food change so
     * corrections and deletions are reflected immediately.
     */
    var allFoodEntries by remember {
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

    /*
     * Today activity entries open the same focused editors used by their
     * feature screens. These temporary values are UI-only and never change
     * the database until the user confirms an edit.
     */
    var activityFoodBeingEdited by remember {
        mutableStateOf<FoodLogEntry?>(null)
    }

    var activityMealLogBeingEdited by remember {
        mutableStateOf<String?>(null)
    }

    var activityMobilityBeingEdited by remember {
        mutableStateOf<MobilitySession?>(null)
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

    var showerLogToday by remember {
        mutableStateOf<ShowerLog?>(null)
    }

    var showerDatesThisWeek by remember {
        mutableStateOf<List<String>>(
            emptyList()
        )
    }

    /*
     * Occasional life maintenance is history-only. It has no schedule, due
     * date, inventory count, or overdue state.
     */
    var lifeMaintenanceLogs by remember {
        mutableStateOf<List<LifeMaintenanceLog>>(emptyList())
    }

    var isSavingLifeMaintenance by remember {
        mutableStateOf(false)
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

    var migraineBeingEdited by remember {
        mutableStateOf<MigraineLog?>(null)
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

    var iopGroups by remember {
        mutableStateOf<List<IopGroup>>(emptyList())
    }

    var isSavingIopGroup by remember {
        mutableStateOf(false)
    }

    var iopMissedOccurrences by remember {
        mutableStateOf<List<IopMissedOccurrence>>(emptyList())
    }

    var isSavingIopAttendance by remember {
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
     * One daily highest value for each pain area currently tracked.
     * These are not before/after-workout measurements. Quick logging can raise
     * either value later in the day but cannot accidentally lower it.
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
     * available. The app stores a separate Daily Rebuild snapshot automatically.
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

    var initialDailyHistoryDate by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val historyState = historyViewModel.state
    val isLoadingDailyHistory = historyState.isLoading
    val isDeletingDailyHistoryDay = historyState.isDeleting
    val dailyHistoryDays = historyState.days

    /* Incremented after deleting the active date so all daily fields reload. */
    var dayReloadToken by remember {
        mutableIntStateOf(0)
    }

    var hasLoadedActiveDay by remember {
        mutableStateOf(false)
    }

    var lastAutoSavedRecord by remember {
        mutableStateOf<DailyRecord?>(null)
    }

    var lastAutoSavedActivityFingerprint by remember {
        mutableStateOf<List<Any?>?>(null)
    }

    var historicalFoodLogDate by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var isUpdatingHistoryDay by remember {
        mutableStateOf(false)
    }

    var historicalWaterSaveJob by remember {
        mutableStateOf<Job?>(null)
    }

    fun buildCurrentDailyRecord(
        updatedAt: Long = 0L
    ): DailyRecord {
        return DailyRecord(
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
            updatedAt = updatedAt
        )
    }

    fun buildEmptyHistoricalDailyRecord(
        date: String
    ): DailyRecord {
        return DailyRecord(
            date = date,
            morningAspirinTaken = false,
            morningIbuprofenTaken = false,
            morningNaproxenTaken = false,
            morningAcetaminophenTaken = false,
            nightIbuprofenTaken = false,
            nightNaproxenTaken = false,
            nightAcetaminophenTaken = false
        )
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
        hasLoadedActiveDay = false

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
        allFoodEntries = emptyList()
        mobilitySessionsToday = emptyList()
        showeredToday = false
        showerLogToday = null
        showerDatesThisWeek = emptyList()
        lifeMaintenanceLogs = emptyList()
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
                backPain = savedRecord.backPain
                shinPain = savedRecord.shinPain
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
            showerLogToday = weeklyShowerLogs.firstOrNull { it.date == todayDate }
            showeredToday = showerLogToday != null

            lifeMaintenanceLogs = loadFeature(
                feature = "life maintenance",
                userMessage = "Could not load life-maintenance history."
            ) {
                lifeMaintenanceDao.getAllLogs()
            } ?: emptyList()

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

            if (!appPreferencesRepository.areIopDefaultsInitialized()) {
                val existingIopGroups = loadFeature(
                    feature = "IOP groups",
                    userMessage = "Could not initialize the IOP group schedule."
                ) {
                    iopGroupDao.getAll()
                }

                if (existingIopGroups != null) {
                    if (existingIopGroups.isEmpty()) {
                        val insertedDefaults = loadFeature(
                            feature = "IOP groups",
                            userMessage = "Could not create the default IOP group schedule."
                        ) {
                            iopGroupDao.insertAll(defaultIopGroupSchedule())
                        }
                        if (insertedDefaults != null) {
                            appPreferencesRepository.markIopDefaultsInitialized()
                        }
                    } else {
                        appPreferencesRepository.markIopDefaultsInitialized()
                    }
                }
            }

            iopGroups = loadFeature(
                feature = "IOP groups",
                userMessage = "Could not load the IOP group schedule."
            ) {
                iopGroupDao.getAll()
            } ?: emptyList()

            iopMissedOccurrences = loadFeature(
                feature = "IOP attendance",
                userMessage = "Could not load missed IOP groups."
            ) {
                iopAttendanceDao.getAllMissed()
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

            foodEntries = loadFeature(
                feature = "today’s food",
                userMessage = "Could not load today’s food entries."
            ) {
                foodDao.getEntriesForDate(todayDate)
            } ?: emptyList()

            allFoodEntries = loadFeature(
                feature = "food history shortcuts",
                userMessage = "Could not load recent and frequently used foods."
            ) {
                foodDao.getAllEntries()
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

            val savedProductsById = savedProducts.associateBy { it.id }
            foodRecorded = foodEntries.any { entry ->
                savedProductsById[entry.productId]?.isCondiment != true
            }

            loadFailures.summaryMessage()?.let { message ->
                snackbarHostState.showSnackbar(message)
            }
        } finally {
            lastAutoSavedRecord =
                buildCurrentDailyRecord(updatedAt = 0L)

            lastAutoSavedActivityFingerprint =
                savedActivitySnapshot?.let { snapshot ->
                    listOf(
                        snapshot.steps,
                        snapshot.distanceMiles,
                        snapshot.activityMinutes
                    )
                }

            hasLoadedActiveDay = true
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

    LaunchedEffect(selectedMainTab) {
        if (selectedMainTab == AppNavigationViewModel.STATS_TAB) {
            statsViewModel.refresh()
        }
    }

    LaunchedEffect(
        appPreferences,
        careAppointments,
        savedMeetings,
        iopGroups
    ) {
        DailyRebuildReminderCoordinator.sync(
            context = context,
            preferences = appPreferences,
            appointments = careAppointments,
            meetings = savedMeetings,
            iopGroups = iopGroups
        )
    }

    val visibleLogSections = listOf(
        Triple(
            DailyRebuildPreferenceIds.LOG_FOOD,
            "Food",
            AppNavigationViewModel.LOG_FOOD_SECTION
        ),
        Triple(
            DailyRebuildPreferenceIds.LOG_MOVEMENT,
            "Movement",
            AppNavigationViewModel.LOG_MOVEMENT_SECTION
        ),
        Triple(
            DailyRebuildPreferenceIds.LOG_MEETINGS,
            "Meetings & IOP",
            AppNavigationViewModel.LOG_MEETINGS_SECTION
        ),
        Triple(
            DailyRebuildPreferenceIds.LOG_HEALTH,
            "Health",
            AppNavigationViewModel.LOG_HEALTH_SECTION
        ),
        Triple(
            DailyRebuildPreferenceIds.LOG_MAINTENANCE,
            "Maintenance",
            AppNavigationViewModel.LOG_MAINTENANCE_SECTION
        )
    ).filter { it.first in appPreferences.enabledLogSections }

    LaunchedEffect(appPreferences.enabledLogSections) {
        if (
            navigationViewModel.selectedLogSection != AppNavigationViewModel.LOG_HOME_SECTION &&
            visibleLogSections.none {
                it.third == navigationViewModel.selectedLogSection
            }
        ) {
            visibleLogSections.firstOrNull()?.let {
                navigationViewModel.selectLogSection(it.third)
            }
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

    val totalProteinToday =
        foodEntries.sumOf { it.proteinGrams }

    val todayRepeatShortcuts =
        remember(allFoodEntries) {
            buildTodayRepeatShortcuts(allFoodEntries)
        }

    val todayActivityItems =
        remember(
            todayDate,
            foodEntries,
            mobilitySessionsToday,
            showerLogToday,
            lifeMaintenanceLogs,
            meetingAttendanceHistory
        ) {
            buildTodayActivityItems(
                date = todayDate,
                foodEntries = foodEntries,
                mobilitySessions = mobilitySessionsToday,
                showerLog = showerLogToday,
                maintenanceLogs = lifeMaintenanceLogs,
                meetingAttendance = meetingAttendanceHistory
            )
        }

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

    val nextIopOccurrence = findNextIopOccurrence(
        groups = iopGroups,
        nowDate = runCatching { LocalDate.parse(todayDate) }
            .getOrDefault(LocalDate.now())
    )

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

    /*
     * A walk is one of the four daily anchors. Health Connect already gives
     * Daily Rebuild the day's step and distance totals, but the anchor used to
     * be a completely separate manual checkbox. Mark it automatically after
     * connected activity reaches a small, meaningful walking amount. The user
     * can still correct the checkbox manually afterward.
     */
    val walkDetectedFromActivity =
        displayedActivity.steps >= 500L ||
            displayedActivity.distanceMiles >= 0.25

    LaunchedEffect(
        todayDate,
        displayedActivity.steps,
        displayedActivity.distanceMiles
    ) {
        if (
            todayDate == LocalDate.now().toString() &&
            walkDetectedFromActivity &&
            !walkCompleted
        ) {
            walkCompleted = true
        }
    }

    /*
     * Data-quality checks are rebuilt from the database instead of being
     * stored as records. That keeps them current after edits, deletes, Undo,
     * restores, and Health/Profile changes without adding another Room table.
     */
    LaunchedEffect(
        todayDate,
        selectedMainTab,
        hasLoadedActiveDay,
        allFoodEntries,
        savedProducts,
        careAppointments,
        careVisits,
        meetingAttendanceHistory,
        mobilitySessionsToday,
        migraineLogs,
        lifeMaintenanceLogs,
        plainReusableBottleCount,
        mioReusableBottleCount,
        plainDisposableBottleCount,
        mioDisposableBottleCount,
        healthFeatureRefreshKey,
        dayReloadToken,
        dailyHistoryDays,
        dataQualityRefreshToken
    ) {
        if (!hasLoadedActiveDay) return@LaunchedEffect

        delay(300L)

        try {
            val allDailyRecords = dailyRecordDao.getAllRecords()
                .filterNot { it.date == todayDate } +
                buildCurrentDailyRecord(updatedAt = System.currentTimeMillis())

            val ignoredSignatures =
                appPreferencesRepository.loadIgnoredDataQualitySignatures()

            val builtWarnings = DataQualityWarningEngine.build(
                snapshot = DataQualitySnapshot(
                    dailyRecords = allDailyRecords,
                    foodEntries = foodDao.getAllEntries(),
                    foodProducts = foodDao.getAllProducts(),
                    activitySnapshots = dailyActivityDao.getAllSnapshots(),
                    mobilitySessions = mobilitySessionDao.getAllSessions(),
                    showerLogs = showerLogDao.getAllLogs(),
                    migraineLogs = migraineLogDao.getAllLogs(),
                    meetingAttendance = meetingDao.getAllAttendance(),
                    careVisits = careVisitDao.getAllVisits(),
                    appointments = careAppointmentDao.getAllAppointments(),
                    healthMeasurements = healthProfileDao.getAllMeasurements(),
                    medications = healthProfileDao.getMedications(),
                    lifeMaintenanceLogs = lifeMaintenanceDao.getAllLogs()
                ),
                today = runCatching { LocalDate.parse(todayDate) }
                    .getOrDefault(LocalDate.now()),
                ignoredSignatures = ignoredSignatures
            )

            val activeWarningIds = builtWarnings.mapTo(mutableSetOf()) { it.id }
            keptDataQualityWarningIds =
                keptDataQualityWarningIds.intersect(activeWarningIds)
            ignoredDataQualityValueCount = ignoredSignatures.size
            dataQualityWarnings = builtWarnings.filterNot {
                it.id in keptDataQualityWarningIds
            }
        } catch (_: Exception) {
            // Data checks must never block the rest of Today from loading.
            dataQualityWarnings = emptyList()
        }
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
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showUndoableDelete(
                    message = "Meeting attendance removed.",
                    restoredMessage = "Meeting attendance restored."
                ) {
                    meetingDao.insertAttendance(attendance)
                    meetingAttendanceHistory =
                        meetingDao.getAllAttendance()
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not remove meeting attendance."
                )
            } finally {
                isDeletingMeetingAttendance = false
            }
        }
    }

    fun saveIopGroup(group: IopGroup) {
        coroutineScope.launch {
            isSavingIopGroup = true
            try {
                if (group.id == 0L) {
                    iopGroupDao.insert(group)
                } else {
                    iopGroupDao.update(group)
                }
                iopGroups = iopGroupDao.getAll()
                snackbarHostState.showSnackbar(
                    message = if (group.id == 0L) {
                        "IOP group added."
                    } else {
                        "IOP group updated."
                    }
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not save the IOP group."
                )
            } finally {
                isSavingIopGroup = false
            }
        }
    }

    fun deleteIopGroup(group: IopGroup) {
        coroutineScope.launch {
            isSavingIopGroup = true
            try {
                iopGroupDao.delete(group)
                iopGroups = iopGroupDao.getAll()
                snackbarHostState.showUndoableDelete(
                    message = "IOP group removed.",
                    restoredMessage = "IOP group restored."
                ) {
                    iopGroupDao.insert(group)
                    iopGroups = iopGroupDao.getAll()
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not remove the IOP group."
                )
            } finally {
                isSavingIopGroup = false
            }
        }
    }

    fun markIopMissed(
        occurrence: IopOccurrence,
        reason: String
    ) {
        coroutineScope.launch {
            isSavingIopAttendance = true
            try {
                val now = System.currentTimeMillis()
                val existing = iopAttendanceDao.getMissedForOccurrence(
                    groupId = occurrence.group.id,
                    occurrenceDate = occurrence.date.toString()
                )
                iopAttendanceDao.saveMissed(
                    IopMissedOccurrence(
                        id = existing?.id ?: 0L,
                        groupId = occurrence.group.id,
                        occurrenceDate = occurrence.date.toString(),
                        groupNameSnapshot = occurrence.group.name,
                        startMinutesSnapshot = occurrence.group.startMinutes,
                        endMinutesSnapshot = occurrence.group.endMinutes,
                        reason = reason.trim(),
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                iopMissedOccurrences = iopAttendanceDao.getAllMissed()
                statsViewModel.refresh()
                snackbarHostState.showSnackbar(
                    if (existing == null) {
                        "IOP group marked missed."
                    } else {
                        "Missed reason updated."
                    }
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar("Could not update IOP attendance.")
            } finally {
                isSavingIopAttendance = false
            }
        }
    }

    fun markIopAttended(occurrence: IopOccurrence) {
        coroutineScope.launch {
            isSavingIopAttendance = true
            try {
                val existing = iopAttendanceDao.getMissedForOccurrence(
                    groupId = occurrence.group.id,
                    occurrenceDate = occurrence.date.toString()
                )
                iopAttendanceDao.markAttended(
                    groupId = occurrence.group.id,
                    occurrenceDate = occurrence.date.toString()
                )
                iopMissedOccurrences = iopAttendanceDao.getAllMissed()
                statsViewModel.refresh()

                if (existing == null) {
                    snackbarHostState.showSnackbar("IOP group is already marked attended.")
                } else {
                    snackbarHostState.showUndoableDelete(
                        message = "IOP group changed to attended.",
                        restoredMessage = "IOP group changed back to missed."
                    ) {
                        iopAttendanceDao.saveMissed(existing)
                        iopMissedOccurrences = iopAttendanceDao.getAllMissed()
                        statsViewModel.refresh()
                    }
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar("Could not update IOP attendance.")
            } finally {
                isSavingIopAttendance = false
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
                val linkedAppointment = careAppointments.firstOrNull {
                    it.convertedVisitId == visit.id
                }

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
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showUndoableDelete(
                    message = "Care visit removed.",
                    restoredMessage = "Care visit restored."
                ) {
                    database.withTransaction {
                        careVisitDao.insertVisit(visit)
                        linkedAppointment?.let { appointment ->
                            careAppointmentDao.updateAppointment(
                                appointment.copy(
                                    convertedVisitId = visit.id,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    careVisits = careVisitDao.getAllVisits()
                    careAppointments =
                        careAppointmentDao.getAllAppointments()
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not remove the care visit."
                )
            } finally {
                isDeletingCareVisit = false
            }
        }
    }

    fun requestNotificationPermissionIfNeeded(
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

                if (appPreferences.appointmentRemindersEnabled) {
                    AppointmentReminderScheduler.schedule(
                        context,
                        savedAppointment
                    )
                } else {
                    AppointmentReminderScheduler.cancel(
                        context,
                        savedAppointment.id
                    )
                }

                careAppointments =
                    careAppointmentDao.getAllAppointments()

                requestNotificationPermissionIfNeeded(
                    remindersEnabled =
                        appPreferences.notificationsEnabled &&
                            appPreferences.appointmentRemindersEnabled &&
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
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showUndoableDelete(
                    message = "Appointment removed.",
                    restoredMessage = "Appointment restored."
                ) {
                    careAppointmentDao.insertAppointment(appointment)
                    careAppointments =
                        careAppointmentDao.getAllAppointments()
                    if (appPreferences.appointmentRemindersEnabled) {
                        AppointmentReminderScheduler.schedule(
                            context,
                            appointment
                        )
                    }
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
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

    fun openDailyHistory(initialDate: String? = null) {
        initialDailyHistoryDate = initialDate
        if (initialDate != null) {
            historyViewModel.selectFilter(DailyHistoryFilter.ALL)
        }
        showDailyHistoryDialog = true
        historyViewModel.refresh { message ->
            if (message != null) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    fun reviewDataQualityWarning(warning: DataQualityWarning) {
        when (warning.target) {
            DataQualityWarningTarget.FOOD -> {
                if (warning.date != null && warning.date != todayDate) {
                    openDailyHistory(warning.date)
                } else {
                    navigationViewModel.openLogSection(
                        AppNavigationViewModel.LOG_FOOD_SECTION
                    )
                }
            }

            DataQualityWarningTarget.SAVED_FOODS -> {
                navigationViewModel.openLogSection(
                    AppNavigationViewModel.LOG_FOOD_SECTION
                )
                showSavedFoodsDialog = true
            }

            DataQualityWarningTarget.WATER -> {
                if (warning.date != null && warning.date != todayDate) {
                    openDailyHistory(warning.date)
                } else {
                    showQuickWaterDialog = true
                }
            }

            DataQualityWarningTarget.APPOINTMENTS -> {
                selectedMoreFeature = "appointments"
                navigationViewModel.selectMainTab(
                    AppNavigationViewModel.MORE_TAB
                )
            }

            DataQualityWarningTarget.HEALTH -> {
                selectedMoreFeature = "profile"
                navigationViewModel.selectMainTab(
                    AppNavigationViewModel.MORE_TAB
                )
            }

            DataQualityWarningTarget.MEETINGS -> {
                navigationViewModel.openLogSection(
                    AppNavigationViewModel.LOG_MEETINGS_SECTION
                )
            }

            DataQualityWarningTarget.MOBILITY -> {
                navigationViewModel.openLogSection(
                    AppNavigationViewModel.LOG_MOVEMENT_SECTION
                )
            }

            DataQualityWarningTarget.HISTORY -> {
                openDailyHistory(warning.date)
            }
        }
    }

    fun keepDataQualityWarning(warning: DataQualityWarning) {
        keptDataQualityWarningIds = keptDataQualityWarningIds + warning.id
        dataQualityWarnings = dataQualityWarnings.filterNot { it.id == warning.id }
    }

    fun ignoreExactDataQualityWarning(warning: DataQualityWarning) {
        appPreferencesRepository.ignoreDataQualitySignature(warning.signature)
        keptDataQualityWarningIds = keptDataQualityWarningIds + warning.id
        dataQualityWarnings = dataQualityWarnings.filterNot { it.id == warning.id }
        ignoredDataQualityValueCount =
            appPreferencesRepository.loadIgnoredDataQualitySignatures().size

        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "This exact value will no longer trigger that warning."
            )
        }
    }

    fun resetIgnoredDataQualityWarnings() {
        appPreferencesRepository.clearIgnoredDataQualitySignatures()
        ignoredDataQualityValueCount = 0
        keptDataQualityWarningIds = emptySet()
        dataQualityRefreshToken++

        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "Hidden exact-value warnings restored."
            )
        }
    }

    fun logShowerToday() {
        coroutineScope.launch {
            try {
                val savedShowerLog = ShowerLog(
                    date = todayDate
                )
                showerLogDao.save(savedShowerLog)

                showerLogToday = savedShowerLog
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
                val removedLog = showerLogToday ?: ShowerLog(date = todayDate)

                showerLogDao.deleteByDate(
                    todayDate
                )

                showerLogToday = null
                showeredToday = false
                showerDatesThisWeek =
                    showerDatesThisWeek.filterNot {
                        it == todayDate
                    }
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showUndoableDelete(
                    message = "Today’s shower log removed.",
                    restoredMessage = "Today’s shower log restored."
                ) {
                    showerLogDao.save(removedLog)
                    showerLogToday = removedLog
                    showeredToday = true
                    showerDatesThisWeek =
                        (showerDatesThisWeek + todayDate).distinct().sorted()
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    message =
                        "Could not remove today’s shower."
                )
            }
        }
    }


    fun saveLifeMaintenanceCompletion(
        taskKey: String,
        date: String
    ) {
        coroutineScope.launch {
            isSavingLifeMaintenance = true
            try {
                lifeMaintenanceDao.save(taskKey, date)
                lifeMaintenanceLogs = lifeMaintenanceDao.getAllLogs()
                historyViewModel.refresh()
                statsViewModel.refresh()
                snackbarHostState.showSnackbar(
                    "${LifeMaintenanceTasks.labelFor(taskKey)} completed on ${formatMaintenanceDate(date)}."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    "Could not save that life-maintenance completion."
                )
            } finally {
                isSavingLifeMaintenance = false
            }
        }
    }

    fun moveLifeMaintenanceCompletion(
        log: LifeMaintenanceLog,
        newDate: String
    ) {
        coroutineScope.launch {
            isSavingLifeMaintenance = true
            try {
                lifeMaintenanceDao.move(log, newDate)
                lifeMaintenanceLogs = lifeMaintenanceDao.getAllLogs()
                historyViewModel.refresh()
                statsViewModel.refresh()
                snackbarHostState.showSnackbar(
                    "Completion moved to ${formatMaintenanceDate(newDate)}."
                )
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    "Could not change that completion date."
                )
            } finally {
                isSavingLifeMaintenance = false
            }
        }
    }

    fun deleteLifeMaintenanceCompletion(
        log: LifeMaintenanceLog
    ) {
        coroutineScope.launch {
            isSavingLifeMaintenance = true
            try {
                lifeMaintenanceDao.delete(log)
                lifeMaintenanceLogs = lifeMaintenanceDao.getAllLogs()
                historyViewModel.refresh()
                statsViewModel.refresh()
                snackbarHostState.showUndoableDelete(
                    message = "Life-maintenance completion removed.",
                    restoredMessage = "Life-maintenance completion restored."
                ) {
                    lifeMaintenanceDao.restore(log)
                    lifeMaintenanceLogs = lifeMaintenanceDao.getAllLogs()
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    "Could not remove that completion."
                )
            } finally {
                isSavingLifeMaintenance = false
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
                        id = draft.id,
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
                        notes = draft.notes,
                        createdAt = draft.createdAt
                    )
                )

                migraineLogs =
                    migraineLogDao.getAllLogs()
                showMigraineLogDialog = false
                migraineBeingEdited = null
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showSnackbar(
                    message =
                        if (draft.id == 0L) {
                            "Migraine / visual-aura event logged."
                        } else {
                            "Migraine / visual-aura event updated."
                        }
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
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showUndoableDelete(
                    message = "Migraine event removed.",
                    restoredMessage = "Migraine event restored."
                ) {
                    migraineLogDao.save(log)
                    migraineLogs = migraineLogDao.getAllLogs()
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
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

    suspend fun persistActiveDayAutomatically(
        recordSnapshot: DailyRecord,
        activityFingerprint: List<Any?>?
    ) {
        isSaving = true

        try {
            val now = System.currentTimeMillis()
            val recordToSave = recordSnapshot.copy(updatedAt = now)

            val snapshotToSave =
                when {
                    hasHealthPermissions &&
                        hasLiveHealthActivity ->
                        DailyActivitySnapshot(
                            date = recordSnapshot.date,
                            steps = liveHealthActivity.steps,
                            distanceMiles = liveHealthActivity.distanceMiles,
                            activityMinutes = liveHealthActivity.activityMinutes,
                            updatedAt = now
                        )

                    savedActivitySnapshot != null ->
                        savedActivitySnapshot?.copy(updatedAt = now)

                    else -> null
                }

            database.withTransaction {
                dailyRecordDao.saveRecord(recordToSave)
                snapshotToSave?.let {
                    dailyActivityDao.saveSnapshot(it)
                }
            }

            savedActivitySnapshot = snapshotToSave
            lastAutoSavedRecord = recordSnapshot.copy(updatedAt = 0L)
            lastAutoSavedActivityFingerprint = activityFingerprint
        } catch (exception: Exception) {
            snackbarHostState.showSnackbar(
                message = "Could not save your latest changes automatically."
            )
        } finally {
            isSaving = false
        }
    }

    val pendingAutoSaveRecord =
        buildCurrentDailyRecord(updatedAt = 0L)

    val pendingActivityFingerprint =
        if (hasHealthPermissions && hasLiveHealthActivity) {
            listOf(
                liveHealthActivity.steps,
                liveHealthActivity.distanceMiles,
                liveHealthActivity.activityMinutes
            )
        } else {
            null
        }

    LaunchedEffect(
        hasLoadedActiveDay,
        pendingAutoSaveRecord,
        pendingActivityFingerprint
    ) {
        if (!hasLoadedActiveDay || isLoading) {
            return@LaunchedEffect
        }

        val dailyRecordChanged =
            pendingAutoSaveRecord != lastAutoSavedRecord

        val activityChanged =
            pendingActivityFingerprint != null &&
                pendingActivityFingerprint !=
                    lastAutoSavedActivityFingerprint

        if (!dailyRecordChanged && !activityChanged) {
            return@LaunchedEffect
        }

        // Journal typing and repeated water taps are combined into one write.
        delay(350L)

        persistActiveDayAutomatically(
            recordSnapshot = pendingAutoSaveRecord,
            activityFingerprint = pendingActivityFingerprint
        )
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

    fun updateMobilitySession(
        session: MobilitySession
    ) {
        coroutineScope.launch {
            isSavingMobility = true
            try {
                mobilitySessionDao.addSession(session)
                mobilitySessionsToday =
                    mobilitySessionDao.getSessionsForDate(todayDate)
                mobilityCompleted = mobilitySessionsToday.isNotEmpty()
                historyViewModel.refresh()
                statsViewModel.refresh()
                snackbarHostState.showSnackbar(
                    message = "Mobility session updated."
                )
            } catch (_: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not update mobility session."
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
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showUndoableDelete(
                    message = "Mobility session deleted.",
                    restoredMessage = "Mobility session restored."
                ) {
                    mobilitySessionDao.addSession(session)
                    mobilitySessionsToday =
                        mobilitySessionDao.getSessionsForDate(todayDate)
                    mobilityCompleted = mobilitySessionsToday.isNotEmpty()
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
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

    val activeFoodLogDate = historicalFoodLogDate ?: todayDate

    suspend fun hasNonCondimentFood(
        entries: List<FoodLogEntry>
    ): Boolean {
        if (entries.isEmpty()) return false
        val productsById = foodDao.getAllProducts().associateBy { it.id }
        return entries.any { entry ->
            productsById[entry.productId]?.isCondiment != true
        }
    }

    suspend fun synchronizeFoodRecordedForDate(
        date: String
    ): List<FoodLogEntry> {
        val entries = foodDao.getEntriesForDate(date)
        val substantiveFoodRecorded = hasNonCondimentFood(entries)
        allFoodEntries = foodDao.getAllEntries()

        if (date == todayDate) {
            foodEntries = entries
            foodRecorded = substantiveFoodRecorded
        } else {
            val existingRecord =
                dailyRecordDao.getRecordByDate(date)
                    ?: buildEmptyHistoricalDailyRecord(date)

            dailyRecordDao.saveRecord(
                existingRecord.copy(
                    foodRecorded = substantiveFoodRecorded,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        return entries
    }

    suspend fun synchronizeFoodRecordedForProduct(
        productId: Long
    ) {
        foodDao.getAllEntries()
            .asSequence()
            .filter { it.productId == productId }
            .map { it.date }
            .distinct()
            .toList()
            .forEach { date ->
                synchronizeFoodRecordedForDate(date)
            }
    }

    fun scaleFoodEntryToQuantity(
        entry: FoodLogEntry,
        newQuantity: Double
    ): FoodLogEntry {
        require(newQuantity > 0.0) {
            "Food quantity must be greater than zero."
        }

        val nutritionScale =
            if (entry.quantity > 0.0) {
                newQuantity / entry.quantity
            } else {
                1.0
            }

        return entry.copy(
            quantity = newQuantity,
            calories = entry.calories * nutritionScale,
            proteinGrams = entry.proteinGrams * nutritionScale,
            carbohydrateGrams = entry.carbohydrateGrams * nutritionScale,
            fatGrams = entry.fatGrams * nutritionScale,
            sodiumMilligrams = entry.sodiumMilligrams * nutritionScale
        )
    }

    suspend fun addOrMergeIndividualFoodEntry(
        entry: FoodLogEntry
    ): Pair<FoodLogEntry, Boolean> {
        if (!entry.mealLogId.isNullOrBlank()) {
            val newId = foodDao.addFoodEntry(entry)
            return entry.copy(id = newId) to false
        }

        return database.withTransaction {
            val existing =
                foodDao.findMergeableIndividualEntry(
                    date = entry.date,
                    productId = entry.productId,
                    productNameSnapshot = entry.productNameSnapshot,
                    unit = entry.unit,
                    mealName = entry.mealName
                )

            if (existing == null) {
                val newId = foodDao.addFoodEntry(entry)
                entry.copy(id = newId) to false
            } else {
                val merged =
                    existing.copy(
                        quantity = existing.quantity + entry.quantity,
                        productNameSnapshot = entry.productNameSnapshot,
                        calories = existing.calories + entry.calories,
                        proteinGrams =
                            existing.proteinGrams +
                                entry.proteinGrams,
                        carbohydrateGrams =
                            existing.carbohydrateGrams +
                                entry.carbohydrateGrams,
                        fatGrams =
                            existing.fatGrams +
                                entry.fatGrams,
                        sodiumMilligrams =
                            existing.sodiumMilligrams +
                                entry.sodiumMilligrams
                    )

                foodDao.updateFoodEntry(merged)
                merged to true
            }
        }
    }

    fun repeatTodayShortcut(
        shortcut: TodayRepeatShortcut,
        requestedQuantity: Double
    ) {
        if (requestedQuantity <= 0.0) return

        coroutineScope.launch {
            try {
                when (shortcut.type) {
                    TodayRepeatShortcutType.FOOD -> {
                        val source = shortcut.sourceEntries.firstOrNull()
                            ?: error("The saved food shortcut is no longer available.")

                        val repeatedEntry = scaleFoodEntryToQuantity(
                            entry = source,
                            newQuantity = requestedQuantity
                        ).copy(
                            id = 0L,
                            date = todayDate,
                            mealLogId = null,
                            savedMealId = null,
                            mealQuantity = 1.0,
                            createdAt = System.currentTimeMillis()
                        )

                        val (savedEntry, merged) =
                            addOrMergeIndividualFoodEntry(repeatedEntry)

                        synchronizeFoodRecordedForDate(todayDate)

                        snackbarHostState.showSnackbar(
                            if (merged) {
                                "${savedEntry.productNameSnapshot} quantity increased."
                            } else {
                                "${savedEntry.productNameSnapshot} added to today."
                            }
                        )
                    }

                    TodayRepeatShortcutType.MEAL -> {
                        val sourceEntries = shortcut.sourceEntries
                        val firstSource = sourceEntries.firstOrNull()
                            ?: error("The saved meal shortcut is no longer available.")

                        val sourceMealQuantity =
                            sourceEntries.maxOfOrNull { it.mealQuantity }
                                ?.takeIf { it > 0.0 }
                                ?: 1.0
                        val scale = requestedQuantity / sourceMealQuantity
                        val mealName =
                            firstSource.mealName?.takeIf { it.isNotBlank() }
                                ?: shortcut.title
                        val savedMealId = firstSource.savedMealId

                        fun ingredientKey(entry: FoodLogEntry): String =
                            "${entry.productId}|${entry.unit.trim().lowercase(Locale.US)}"

                        val expectedIngredientKeys =
                            sourceEntries.map(::ingredientKey).sorted()

                        val currentMealGroups =
                            foodDao.getEntriesForDate(todayDate)
                                .filter { !it.mealLogId.isNullOrBlank() }
                                .groupBy { it.mealLogId.orEmpty() }
                                .values

                        val existingMealGroup =
                            currentMealGroups.firstOrNull { group ->
                                val first = group.firstOrNull()
                                    ?: return@firstOrNull false

                                when {
                                    savedMealId != null &&
                                        first.savedMealId == savedMealId -> true

                                    first.savedMealId == null &&
                                        first.mealName?.trim()
                                            ?.equals(
                                                mealName.trim(),
                                                ignoreCase = true
                                            ) == true -> {
                                        group.map(::ingredientKey).sorted() ==
                                            expectedIngredientKeys
                                    }

                                    else -> false
                                }
                            }

                        val existingMealQuantity =
                            existingMealGroup
                                ?.maxOfOrNull { it.mealQuantity }
                                ?.takeIf { it > 0.0 }
                                ?: if (existingMealGroup == null) 0.0 else 1.0

                        val updatedMealQuantity =
                            existingMealQuantity + requestedQuantity
                        val mealLogId =
                            existingMealGroup
                                ?.firstOrNull()
                                ?.mealLogId
                                ?.takeIf { it.isNotBlank() }
                                ?: UUID.randomUUID().toString()
                        val now = System.currentTimeMillis()

                        val entriesToAdd = sourceEntries.map { source ->
                            source.copy(
                                id = 0L,
                                date = todayDate,
                                quantity = source.quantity * scale,
                                mealName = mealName,
                                mealLogId = mealLogId,
                                savedMealId = savedMealId,
                                mealQuantity = updatedMealQuantity,
                                calories = source.calories * scale,
                                proteinGrams = source.proteinGrams * scale,
                                carbohydrateGrams =
                                    source.carbohydrateGrams * scale,
                                fatGrams = source.fatGrams * scale,
                                sodiumMilligrams =
                                    source.sodiumMilligrams * scale,
                                createdAt = now
                            )
                        }

                        database.withTransaction {
                            if (existingMealGroup == null) {
                                entriesToAdd.forEach { entry ->
                                    foodDao.addFoodEntry(entry)
                                }
                            } else {
                                val availableExisting =
                                    existingMealGroup.toMutableList()

                                entriesToAdd.forEach { addedEntry ->
                                    val existingEntry =
                                        availableExisting.firstOrNull { current ->
                                            current.productId == addedEntry.productId &&
                                                current.unit.trim().equals(
                                                    addedEntry.unit.trim(),
                                                    ignoreCase = true
                                                )
                                        }

                                    if (existingEntry == null) {
                                        foodDao.addFoodEntry(addedEntry)
                                    } else {
                                        foodDao.updateFoodEntry(
                                            existingEntry.copy(
                                                quantity =
                                                    existingEntry.quantity +
                                                        addedEntry.quantity,
                                                mealName = mealName,
                                                savedMealId = savedMealId,
                                                mealQuantity =
                                                    updatedMealQuantity,
                                                calories =
                                                    existingEntry.calories +
                                                        addedEntry.calories,
                                                proteinGrams =
                                                    existingEntry.proteinGrams +
                                                        addedEntry.proteinGrams,
                                                carbohydrateGrams =
                                                    existingEntry.carbohydrateGrams +
                                                        addedEntry.carbohydrateGrams,
                                                fatGrams =
                                                    existingEntry.fatGrams +
                                                        addedEntry.fatGrams,
                                                sodiumMilligrams =
                                                    existingEntry.sodiumMilligrams +
                                                        addedEntry.sodiumMilligrams
                                            )
                                        )
                                        availableExisting.remove(existingEntry)
                                    }
                                }

                                availableExisting.forEach { existingEntry ->
                                    foodDao.updateFoodEntry(
                                        existingEntry.copy(
                                            mealName = mealName,
                                            savedMealId = savedMealId,
                                            mealQuantity = updatedMealQuantity
                                        )
                                    )
                                }
                            }
                        }

                        synchronizeFoodRecordedForDate(todayDate)

                        snackbarHostState.showSnackbar(
                            "$mealName quantity updated to " +
                                formatCompactNumber(updatedMealQuantity) +
                                "."
                        )
                    }
                }
            } catch (exception: Exception) {
                snackbarHostState.showSnackbar(
                    "Could not repeat that food or meal."
                )
            }
        }
    }

    fun returnToHistoricalDay(
        successMessage: String? = null
    ) {
        val editedDate = historicalFoodLogDate ?: return
        historicalFoodLogDate = null

        historyViewModel.refresh { errorMessage ->
            initialDailyHistoryDate = editedDate
            historyViewModel.selectFilter(DailyHistoryFilter.ALL)
            showDailyHistoryDialog = true

            val message = errorMessage ?: successMessage
            if (message != null) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    fun beginHistoricalFoodEdit(
        date: String
    ) {
        historicalFoodLogDate = date
        showDailyHistoryDialog = false
        initialDailyHistoryDate = null
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

    fun updateFoodEntryQuantity(
        entry: FoodLogEntry,
        newQuantity: Double
    ) {
        coroutineScope.launch {
            try {
                val updatedEntry =
                    scaleFoodEntryToQuantity(
                        entry = entry,
                        newQuantity = newQuantity
                    )

                foodDao.updateFoodEntry(updatedEntry)
                synchronizeFoodRecordedForDate(entry.date)

                if (selectedMainTab == AppNavigationViewModel.STATS_TAB) {
                    statsViewModel.refresh()
                }

                snackbarHostState.showSnackbar(
                    message = "Food quantity updated."
                )
            } catch (
                exception: Exception
            ) {
                snackbarHostState.showSnackbar(
                    message = "Could not update the food quantity."
                )
            }
        }
    }

    fun updateMealLogQuantity(
        mealLogId: String,
        newQuantity: Double,
        date: String = todayDate
    ) {
        coroutineScope.launch {
            try {
                require(newQuantity > 0.0) {
                    "Meal quantity must be greater than zero."
                }

                val mealEntries = foodDao.getEntriesForDate(date).filter {
                    it.mealLogId == mealLogId
                }
                require(mealEntries.isNotEmpty()) {
                    "That meal entry no longer exists."
                }

                val currentQuantity = mealEntries
                    .maxOfOrNull { it.mealQuantity }
                    ?.takeIf { it > 0.0 }
                    ?: 1.0
                val scale = newQuantity / currentQuantity

                database.withTransaction {
                    mealEntries.forEach { entry ->
                        foodDao.updateFoodEntry(
                            entry.copy(
                                quantity = entry.quantity * scale,
                                mealQuantity = newQuantity,
                                calories = entry.calories * scale,
                                proteinGrams = entry.proteinGrams * scale,
                                carbohydrateGrams = entry.carbohydrateGrams * scale,
                                fatGrams = entry.fatGrams * scale,
                                sodiumMilligrams = entry.sodiumMilligrams * scale
                            )
                        )
                    }
                }

                synchronizeFoodRecordedForDate(date)
                historyViewModel.refresh()
                statsViewModel.refresh()
                snackbarHostState.showSnackbar(
                    message = "Meal quantity updated."
                )
            } catch (_: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not update the meal quantity."
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

                synchronizeFoodRecordedForDate(entry.date)
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showUndoableDelete(
                    message = "Food entry deleted.",
                    restoredMessage = "Food entry restored."
                ) {
                    foodDao.addFoodEntry(entry)
                    synchronizeFoodRecordedForDate(entry.date)
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
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
                val removedEntries = foodEntries.filter {
                    it.mealLogId == mealLogId
                }
                val entryDate = removedEntries.firstOrNull()?.date ?: todayDate

                foodDao
                    .deleteFoodEntriesByMealLogId(
                        mealLogId
                    )

                synchronizeFoodRecordedForDate(entryDate)
                historyViewModel.refresh()
                statsViewModel.refresh()

                snackbarHostState.showUndoableDelete(
                    message = "Meal deleted.",
                    restoredMessage = "Meal restored."
                ) {
                    database.withTransaction {
                        removedEntries.forEach { entry ->
                            foodDao.addFoodEntry(entry)
                        }
                    }
                    synchronizeFoodRecordedForDate(entryDate)
                    historyViewModel.refresh()
                    statsViewModel.refresh()
                }
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

    fun openGlobalSearch() {
        showGlobalSearch = true
        globalSearchQuery = ""
        globalSearchFilter = GlobalSearchFilter.ALL
        recentGlobalSearches = appPreferencesRepository.loadRecentSearches()

        coroutineScope.launch {
            isLoadingGlobalSearch = true
            try {
                globalSearchSnapshot = GlobalSearchSnapshot(
                    products = foodDao.getAllProducts(),
                    meals = mealDao.getAllMealsWithIngredients(),
                    foodEntries = foodDao.getAllEntries(),
                    dailyRecords = dailyRecordDao.getAllRecords(),
                    carePlaces = careVisitDao.getActivePlaces(),
                    careProviders = careVisitDao.getActiveProviders(),
                    careVisits = careVisitDao.getAllVisits(),
                    careAppointments = careAppointmentDao.getAllAppointments(),
                    medications = healthProfileDao.getMedications(),
                    measurements = healthProfileDao.getAllMeasurements(),
                    migraineLogs = migraineLogDao.getAllLogs(),
                    savedMeetings = meetingDao.getActiveMeetings(),
                    meetingAttendance = meetingDao.getAllAttendance(),
                    iopGroups = iopGroupDao.getAll(),
                    mobilitySessions = mobilitySessionDao.getAllSessions(),
                    mobilityMovements = MobilityMovementLibrary.movements.map { movement ->
                        GlobalSearchMobilityMovement(
                            id = movement.id,
                            name = movement.name,
                            primaryCategory = movement.primaryCategory.label,
                            categories = movement.categories.joinToString { it.label },
                            positions = movement.positions.joinToString { it.label },
                            instructions = movement.instructions
                        )
                    },
                    maintenanceLogs = lifeMaintenanceDao.getAllLogs(),
                    pantryItems = repositories.pantry.getAll(),
                    showerLogs = showerLogDao.getAllLogs()
                )
            } catch (_: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Could not load Global Search."
                )
            } finally {
                isLoadingGlobalSearch = false
            }
        }
    }

    fun closeGlobalSearch() {
        showGlobalSearch = false
    }

    fun rememberCurrentGlobalSearch() {
        if (globalSearchQuery.isNotBlank()) {
            recentGlobalSearches =
                appPreferencesRepository.rememberSearch(globalSearchQuery)
        }
    }

    fun openGlobalSearchResult(result: GlobalSearchResult) {
        rememberCurrentGlobalSearch()
        showGlobalSearch = false

        when (val target = result.target) {
            is GlobalSearchTarget.SavedFood -> {
                globalSearchSnapshot.products
                    .firstOrNull { it.id == target.id }
                    ?.let { product ->
                        navigationViewModel.openLogSection(
                            AppNavigationViewModel.LOG_FOOD_SECTION
                        )
                        isCreatingFoodForMeal = false
                        isEditingSavedFood = true
                        scannedFoodPrefill = product.toFoodPrefill()
                        showManualFoodDialog = true
                    }
            }

            is GlobalSearchTarget.SavedMeal -> {
                globalSearchSnapshot.meals
                    .firstOrNull { it.meal.id == target.id }
                    ?.let { savedMeal ->
                        navigationViewModel.openPlanSection(
                            AppNavigationViewModel.PLAN_MEALS_SECTION
                        )
                        savedProducts = globalSearchSnapshot.products
                        mealBeingEdited = savedMeal
                        showMealBuilderDialog = true
                    }
            }

            is GlobalSearchTarget.HistoryDate -> {
                openDailyHistory(target.date)
            }

            is GlobalSearchTarget.CarePlaceTarget,
            is GlobalSearchTarget.CareProviderTarget -> {
                selectedMoreFeature = "visits"
                navigationViewModel.selectMainTab(
                    AppNavigationViewModel.MORE_TAB
                )
            }

            is GlobalSearchTarget.MedicationTarget,
            is GlobalSearchTarget.MeasurementTarget -> {
                selectedMoreFeature = "profile"
                navigationViewModel.selectMainTab(
                    AppNavigationViewModel.MORE_TAB
                )
            }

            is GlobalSearchTarget.CareVisitTarget -> {
                selectedMoreFeature = "visits"
                navigationViewModel.selectMainTab(
                    AppNavigationViewModel.MORE_TAB
                )
                globalSearchSnapshot.careVisits
                    .firstOrNull { it.id == target.id }
                    ?.let { visit ->
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
                        returnToCareHistoryAfterVisitSave = false
                        showCareVisitEditorDialog = true
                    }
            }

            is GlobalSearchTarget.AppointmentTarget -> {
                selectedMoreFeature = "appointments"
                navigationViewModel.selectMainTab(
                    AppNavigationViewModel.MORE_TAB
                )
                globalSearchSnapshot.careAppointments
                    .firstOrNull { it.id == target.id }
                    ?.let { appointment ->
                        openAppointmentEditor(
                            appointment = appointment,
                            returnToHistory = false
                        )
                    }
            }

            is GlobalSearchTarget.MigraineTarget -> {
                navigationViewModel.openLogSection(
                    AppNavigationViewModel.LOG_HEALTH_SECTION
                )
                globalSearchSnapshot.migraineLogs
                    .firstOrNull { it.id == target.id }
                    ?.let { log ->
                        migraineBeingEdited = log
                        showMigraineLogDialog = true
                    }
            }

            is GlobalSearchTarget.SavedMeetingTarget -> {
                globalSearchSnapshot.savedMeetings
                    .firstOrNull { it.id == target.id }
                    ?.let { meeting ->
                        navigationViewModel.openLogSection(
                            AppNavigationViewModel.LOG_MEETINGS_SECTION
                        )
                        meetingBeingEdited = meeting
                        logAttendanceAfterMeetingSave = false
                        showMeetingEditorDialog = true
                    }
            }

            is GlobalSearchTarget.MeetingAttendanceTarget -> {
                globalSearchSnapshot.meetingAttendance
                    .firstOrNull { it.id == target.id }
                    ?.let { attendance ->
                        navigationViewModel.openLogSection(
                            AppNavigationViewModel.LOG_MEETINGS_SECTION
                        )
                        attendanceBeingEdited = attendance
                        meetingForAttendance = attendance.savedMeetingId
                            ?.let { savedMeetingId ->
                                savedMeetings.firstOrNull {
                                    it.id == savedMeetingId
                                }
                            }
                        isOneTimeMeetingAttendance =
                            attendance.savedMeetingId == null
                        showMeetingAttendanceDialog = true
                    }
            }

            is GlobalSearchTarget.IopGroupTarget -> {
                navigationViewModel.openIopGroups()
            }

            is GlobalSearchTarget.MobilitySessionTarget -> {
                globalSearchSnapshot.mobilitySessions
                    .firstOrNull { it.id == target.id }
                    ?.let { session ->
                        navigationViewModel.openLogSection(
                            AppNavigationViewModel.LOG_MOVEMENT_SECTION
                        )
                        activityMobilityBeingEdited = session
                    }
            }

            is GlobalSearchTarget.MobilityMovementTarget -> {
                navigationViewModel.selectMobilitySection(
                    AppNavigationViewModel.MOBILITY_ROUTINES_SECTION
                )
                navigationViewModel.openLogSection(
                    AppNavigationViewModel.LOG_MOVEMENT_SECTION
                )
            }

            is GlobalSearchTarget.MaintenanceTarget -> {
                navigationViewModel.openLogSection(
                    AppNavigationViewModel.LOG_MAINTENANCE_SECTION
                )
            }

            is GlobalSearchTarget.PantryTarget -> {
                selectedMoreFeature = "pantry"
                navigationViewModel.selectMainTab(
                    AppNavigationViewModel.MORE_TAB
                )
            }
        }
    }

    val foodHubState = FoodHubState(
        selectedSection = 0,
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
    )

    val foodHubActions = FoodHubActions(
        onSectionChange = {},
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
        onUpdateEntryQuantity = { entry, quantity ->
            updateFoodEntryQuantity(entry, quantity)
        },
        onUpdateMealQuantity = { mealLogId, quantity ->
            updateMealLogQuantity(mealLogId, quantity)
        },
        onDeleteEntry = { deleteFoodEntry(it) },
        onDeleteMealLog = { deleteMealLog(it) },
        onSavePantryItem = { item -> savePantryEssential(item) },
        onDeletePantryItem = { item -> deletePantryEssential(item) },
        onPantryStatusChange = { item, status ->
            updatePantryEssentialStatus(item, status)
        },
        onMarkNeededPurchased = { markNeededPantryPurchased() }
    )

    val mobilityHubState = MobilityHubState(
        selectedSection = navigationViewModel.selectedMobilitySection,
        availability = healthAvailability,
        hasPermissions = hasHealthPermissions,
        isLoadingActivity = isLoadingHealthActivity,
        activity = displayedActivity,
        sourceLabel = activitySourceLabel,
        sessions = mobilitySessionsToday,
        isSaving = isSavingMobility
    )

    val mobilityHubActions = MobilityHubActions(
        onSectionChange = { navigationViewModel.selectMobilitySection(it) },
        onOpenHistory = { openDailyHistory() },
        onRefresh = {
            refreshHealthActivity(showFeedback = true)
        },
        onManageHealth = {
            selectedMoreFeature = "activity"
            navigationViewModel.selectMainTab(AppNavigationViewModel.MORE_TAB)
        },
        onSaveSession = { saveMobilitySession(it) },
        onUpdateSession = { updateMobilitySession(it) },
        onDeleteSession = { deleteMobilitySession(it) }
    )

    val healthHubState = HealthHubState(
        appointments = careAppointments,
        visits = careVisits,
        migraineLogs = migraineLogs,
        availability = healthAvailability,
        hasPermissions = hasHealthPermissions,
        isLoadingActivity = isLoadingHealthActivity,
        activity = displayedActivity,
        activitySourceLabel = activitySourceLabel
    )

    val healthHubActions = HealthHubActions(
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
        onLogMigraine = {
            migraineBeingEdited = null
            showMigraineLogDialog = true
        },
        onEditMigraine = { log ->
            migraineBeingEdited = log
            showMigraineLogDialog = true
        },
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
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isLoading && !showGlobalSearch) {
                DailyRebuildBottomNavigation(
                    selectedTab = selectedMainTab,
                    onTabSelected = { destination ->
                        when (destination) {
                            AppNavigationViewModel.LOG_TAB -> {
                                navigationViewModel.openLogHome()
                            }
                            AppNavigationViewModel.HISTORY_TAB -> {
                                navigationViewModel.selectMainTab(destination)
                                openDailyHistory()
                            }
                            AppNavigationViewModel.MORE_TAB -> {
                                selectedMoreFeature = null
                                navigationViewModel.selectMainTab(destination)
                            }
                            else -> navigationViewModel.selectMainTab(destination)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isLoading && !showGlobalSearch) {
                ExtendedFloatingActionButton(
                    onClick = { navigationViewModel.openLogHome() },
                    text = { Text("Add") },
                    icon = {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
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
        } else if (showGlobalSearch) {
            GlobalSearchScreen(
                query = globalSearchQuery,
                selectedFilter = globalSearchFilter,
                snapshot = globalSearchSnapshot,
                recentSearches = recentGlobalSearches,
                isLoading = isLoadingGlobalSearch,
                onQueryChange = { globalSearchQuery = it },
                onFilterChange = { globalSearchFilter = it },
                onRecentSearch = { recent ->
                    globalSearchQuery = recent
                    globalSearchFilter = GlobalSearchFilter.ALL
                },
                onClearRecentSearches = {
                    appPreferencesRepository.clearRecentSearches()
                    recentGlobalSearches = emptyList()
                },
                onSearchSubmitted = ::rememberCurrentGlobalSearch,
                onResultClick = ::openGlobalSearchResult,
                onBack = ::closeGlobalSearch,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (selectedMainTab) {
                AppNavigationViewModel.TODAY_TAB -> TodayScreen(
                    state = TodayScreenState(
                        date = todayDate,
                        completedTasks = completedTasks,
                        foodRecorded = foodRecorded,
                        walkCompleted = walkCompleted,
                        painRecorded = painRecorded,
                        mobilityCompleted = mobilityCompleted,
                        backPain = backPain,
                        shinPain = shinPain,
                        calories = totalCaloriesToday,
                        proteinGrams = totalProteinToday,
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
                        maintenanceCompletedToday = lifeMaintenanceLogs
                            .filter { it.date == todayDate }
                            .map { LifeMaintenanceTasks.labelFor(it.taskKey) }
                            .sorted(),
                        iopOccurrence = nextIopOccurrence,
                        repeatShortcuts = todayRepeatShortcuts,
                        activityItems = todayActivityItems,
                        dataQualityWarnings = dataQualityWarnings,
                        preferences = appPreferences,
                        isSaving = isSaving || isSavingLifeMaintenance || isSavingIopGroup
                    ),
                    actions = TodayScreenActions(
                        onOpenHistory = { openDailyHistory() },
                        onOpenSearch = ::openGlobalSearch,
                        onOpenFood = {
                            navigationViewModel.openLogSection(
                                AppNavigationViewModel.LOG_FOOD_SECTION
                            )
                        },
                        onOpenWater = { showQuickWaterDialog = true },
                        onOpenMobility = {
                            navigationViewModel.openLogSection(
                                AppNavigationViewModel.LOG_MOVEMENT_SECTION
                            )
                        },
                        onOpenPain = { showQuickPainDialog = true },
                        onOpenMeetings = {
                            navigationViewModel.openLogSection(
                                AppNavigationViewModel.LOG_MEETINGS_SECTION
                            )
                        },
                        onOpenHealth = {
                            selectedMoreFeature = "profile"
                            navigationViewModel.selectMainTab(
                                AppNavigationViewModel.MORE_TAB
                            )
                        },
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
                        onOpenLifeMaintenance = {
                            navigationViewModel.openLogSection(
                                AppNavigationViewModel.LOG_MAINTENANCE_SECTION
                            )
                        },
                        onOpenIopGroups = {
                            navigationViewModel.openIopGroups()
                        },
                        onRepeatShortcut = { shortcut, quantity ->
                            repeatTodayShortcut(shortcut, quantity)
                        },
                        onEditActivityItem = { item ->
                            when {
                                item.key.startsWith("food:") -> {
                                    val entryId = item.key
                                        .substringAfter("food:")
                                        .toLongOrNull()
                                    activityFoodBeingEdited = foodEntries
                                        .firstOrNull { it.id == entryId }
                                }

                                item.key.startsWith("meal:") -> {
                                    activityMealLogBeingEdited = item.key
                                        .substringAfter("meal:")
                                        .takeIf { it.isNotBlank() }
                                }

                                item.key.startsWith("mobility:") -> {
                                    val sessionId = item.key
                                        .substringAfter("mobility:")
                                        .toLongOrNull()
                                    activityMobilityBeingEdited = mobilitySessionsToday
                                        .firstOrNull { it.id == sessionId }
                                }

                                item.key.startsWith("meeting:") -> {
                                    val attendanceId = item.key
                                        .substringAfter("meeting:")
                                        .toLongOrNull()
                                    meetingAttendanceHistory
                                        .firstOrNull { it.id == attendanceId }
                                        ?.let { attendance ->
                                            attendanceBeingEdited = attendance
                                            meetingForAttendance = attendance.savedMeetingId
                                                ?.let { savedMeetingId ->
                                                    savedMeetings.firstOrNull {
                                                        it.id == savedMeetingId
                                                    }
                                                }
                                            isOneTimeMeetingAttendance =
                                                attendance.savedMeetingId == null
                                            showMeetingAttendanceDialog = true
                                        }
                                }

                                item.key.startsWith("maintenance:") -> {
                                    navigationViewModel.openLogSection(
                                        AppNavigationViewModel.LOG_MAINTENANCE_SECTION
                                    )
                                }
                            }
                        },
                        onDeleteActivityItem = { item ->
                            when {
                                item.key.startsWith("food:") -> {
                                    val entryId = item.key
                                        .substringAfter("food:")
                                        .toLongOrNull()
                                    foodEntries
                                        .firstOrNull { it.id == entryId }
                                        ?.let(::deleteFoodEntry)
                                }

                                item.key.startsWith("meal:") -> {
                                    item.key
                                        .substringAfter("meal:")
                                        .takeIf { it.isNotBlank() }
                                        ?.let(::deleteMealLog)
                                }

                                item.key.startsWith("mobility:") -> {
                                    val sessionId = item.key
                                        .substringAfter("mobility:")
                                        .toLongOrNull()
                                    mobilitySessionsToday
                                        .firstOrNull { it.id == sessionId }
                                        ?.let(::deleteMobilitySession)
                                }

                                item.key.startsWith("meeting:") -> {
                                    val attendanceId = item.key
                                        .substringAfter("meeting:")
                                        .toLongOrNull()
                                    meetingAttendanceHistory
                                        .firstOrNull { it.id == attendanceId }
                                        ?.let(::deleteMeetingAttendance)
                                }

                                item.key.startsWith("maintenance:") -> {
                                    val maintenanceIdentity = item.key
                                        .removePrefix("maintenance:")
                                        .split(":", limit = 2)
                                    val taskKey = maintenanceIdentity.getOrNull(0)
                                    val date = maintenanceIdentity.getOrNull(1)
                                    lifeMaintenanceLogs
                                        .firstOrNull { log ->
                                            log.taskKey == taskKey && log.date == date
                                        }
                                        ?.let(::deleteLifeMaintenanceCompletion)
                                }

                                item.key.startsWith("shower:") -> {
                                    removeTodayShower()
                                }
                            }
                        },
                        onReviewDataQualityWarning = ::reviewDataQualityWarning,
                        onKeepDataQualityWarning = ::keepDataQualityWarning,
                        onIgnoreDataQualityWarning = ::ignoreExactDataQualityWarning,
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
                        onJournalTextChange = { journalText = it }
                    ),
                    modifier = Modifier.padding(innerPadding)
                )

                AppNavigationViewModel.LOG_TAB -> DailyRebuildLogScreen(
                    selectedSection = navigationViewModel.selectedLogSection,
                    enabledSections = appPreferences.enabledLogSections,
                    onSelectSection = navigationViewModel::selectLogSection,
                    onOpenSearch = ::openGlobalSearch,
                    onOpenHistory = { openDailyHistory() },
                    onLogWater = { showQuickWaterDialog = true },
                    onLogPain = { showQuickPainDialog = true },
                    onLogShower = { logShowerToday() },
                    onLogMeeting = { showMeetingPickerDialog = true },
                    onOpenIopAttendance = {
                        navigationViewModel.selectMeetingsSection(
                            AppNavigationViewModel.MEETINGS_IOP_SECTION
                        )
                        navigationViewModel.selectLogSection(
                            AppNavigationViewModel.LOG_MEETINGS_SECTION
                        )
                    },
                    onLogMigraine = {
                        migraineBeingEdited = null
                        showMigraineLogDialog = true
                    },
                    onLogCareVisit = { showCareVisitStartDialog = true },
                    onOpenMeasurements = {
                        selectedMoreFeature = "profile"
                        navigationViewModel.selectMainTab(AppNavigationViewModel.MORE_TAB)
                    },
                    onOpenPantry = {
                        selectedMoreFeature = "pantry"
                        navigationViewModel.selectMainTab(AppNavigationViewModel.MORE_TAB)
                    },
                    modifier = Modifier.padding(innerPadding)
                ) {
                    when (navigationViewModel.selectedLogSection) {
                        AppNavigationViewModel.LOG_FOOD_SECTION -> FoodHubScreen(
                            state = foodHubState.copy(selectedSection = 0),
                            actions = foodHubActions,
                            showHeader = false,
                            showSectionTabs = false,
                            modifier = Modifier.fillMaxSize()
                        )

                        AppNavigationViewModel.LOG_MOVEMENT_SECTION -> MobilityHubScreen(
                            state = mobilityHubState,
                            actions = mobilityHubActions,
                            showHeader = false,
                            modifier = Modifier.fillMaxSize()
                        )

                        AppNavigationViewModel.LOG_MEETINGS_SECTION -> MeetingsHubScreen(
                            weeklyAttendance = weeklyMeetingAttendance,
                            iopGroups = iopGroups,
                            iopMissedOccurrences = iopMissedOccurrences,
                            selectedSection = navigationViewModel.selectedMeetingsSection,
                            isSaving = isSavingMeeting,
                            isSavingIop = isSavingIopGroup,
                            isSavingIopAttendance = isSavingIopAttendance,
                            onSectionChange = navigationViewModel::selectMeetingsSection,
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
                                isOneTimeMeetingAttendance = it.savedMeetingId == null
                                showMeetingAttendanceDialog = true
                            },
                            onDeleteAttendance = {
                                meetingAttendancePendingDeletion = it
                            },
                            onViewFullHistory = {
                                showMeetingHistoryDialog = true
                            },
                            onSaveIopGroup = ::saveIopGroup,
                            onDeleteIopGroup = ::deleteIopGroup,
                            onMarkIopMissed = ::markIopMissed,
                            onMarkIopAttended = ::markIopAttended,
                            showHeader = false,
                            modifier = Modifier.fillMaxSize()
                        )

                        AppNavigationViewModel.LOG_HEALTH_SECTION -> HealthQuickLogScreen(
                            onLogHighestPain = { showQuickPainDialog = true },
                            onLogMigraine = {
                                migraineBeingEdited = null
                                showMigraineLogDialog = true
                            },
                            onLogCareVisit = { showCareVisitStartDialog = true },
                            onLogShower = { logShowerToday() },
                            onOpenMeasurements = {
                                selectedMoreFeature = "profile"
                                navigationViewModel.selectMainTab(
                                    AppNavigationViewModel.MORE_TAB
                                )
                            },
                            onOpenHealth = {
                                selectedMoreFeature = "profile"
                                navigationViewModel.selectMainTab(
                                    AppNavigationViewModel.MORE_TAB
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        else -> LifeMaintenanceScreen(
                            logs = lifeMaintenanceLogs,
                            isSaving = isSavingLifeMaintenance,
                            onMarkDone = ::saveLifeMaintenanceCompletion,
                            onMoveLog = ::moveLifeMaintenanceCompletion,
                            onDeleteLog = ::deleteLifeMaintenanceCompletion
                        )
                    }
                }

                AppNavigationViewModel.HISTORY_TAB -> DailyRebuildHistoryHomeScreen(
                    onOpenHistory = { openDailyHistory() },
                    onOpenSearch = ::openGlobalSearch,
                    modifier = Modifier.padding(innerPadding)
                )

                AppNavigationViewModel.INSIGHTS_TAB -> StatsScreen(
                    state = statsViewModel.state,
                    preferences = appPreferences,
                    onRangeSelected = { range ->
                        statsViewModel.selectRange(range)
                        val updated = appPreferences.copy(statsDefaultRange = range.name)
                        appPreferences = updated
                        appPreferencesRepository.save(updated)
                    },
                    onCustomRangeSelected = statsViewModel::selectCustomRange,
                    onFilterSelected = statsViewModel::selectFilter,
                    onPreviousPeriod = statsViewModel::movePrevious,
                    onNextPeriod = statsViewModel::moveNext,
                    onRefresh = statsViewModel::refresh,
                    onOpenHistory = { openDailyHistory() },
                    onOpenHistoryDate = { date -> openDailyHistory(date) },
                    onOpenSearch = ::openGlobalSearch,
                    dataQualityWarnings = dataQualityWarnings,
                    onReviewDataQualityWarning = ::reviewDataQualityWarning,
                    onKeepDataQualityWarning = ::keepDataQualityWarning,
                    onIgnoreDataQualityWarning = ::ignoreExactDataQualityWarning,
                    modifier = Modifier.padding(innerPadding)
                )

                else -> when (selectedMoreFeature) {
                    null -> DailyRebuildMoreScreen(
                        onOpenSearch = ::openGlobalSearch,
                        onOpenHistory = { openDailyHistory() },
                        onOpenFoodLibrary = { openSavedFoodsScreen() },
                        onOpenSavedMeals = { openSavedMealsScreen() },
                        onOpenMobilityRoutines = {
                            navigationViewModel.selectMobilitySection(
                                AppNavigationViewModel.MOBILITY_ROUTINES_SECTION
                            )
                            navigationViewModel.openLogSection(
                                AppNavigationViewModel.LOG_MOVEMENT_SECTION
                            )
                        },
                        onOpenMedications = { selectedMoreFeature = "profile" },
                        onOpenPantry = { selectedMoreFeature = "pantry" },
                        onOpenIopGroups = {
                            navigationViewModel.openIopGroups()
                        },
                        onOpenMeetings = {
                            navigationViewModel.selectMeetingsSection(
                                AppNavigationViewModel.MEETINGS_ATTENDANCE_SECTION
                            )
                            navigationViewModel.openLogSection(
                                AppNavigationViewModel.LOG_MEETINGS_SECTION
                            )
                        },
                        onOpenCareDirectory = { selectedMoreFeature = "visits" },
                        onOpenAppointments = { selectedMoreFeature = "appointments" },
                        onOpenCustomization = { selectedMoreFeature = "customize" },
                        onOpenNotifications = { selectedMoreFeature = "notifications" },
                        onOpenConnectedActivity = { selectedMoreFeature = "activity" },
                        onOpenBackup = { selectedMoreFeature = "backup" },
                        onOpenHelp = { selectedMoreFeature = "help" },
                        modifier = Modifier.padding(innerPadding)
                    )

                    "pantry" -> RebuildFeaturePage(
                        title = "Pantry Essentials",
                        subtitle = "Manage essentials and shopping status without mixing them into everyday logging.",
                        onBack = { selectedMoreFeature = null },
                        onSearch = ::openGlobalSearch,
                        onHistory = { openDailyHistory() },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        FoodHubScreen(
                            state = foodHubState.copy(selectedSection = 2),
                            actions = foodHubActions,
                            showHeader = false,
                            showSectionTabs = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    "profile" -> RebuildFeaturePage(
                        title = "Medications & Measurements",
                        subtitle = "Health profile, goals, measurement history, and medication reference.",
                        onBack = { selectedMoreFeature = null },
                        onSearch = ::openGlobalSearch,
                        onHistory = { openDailyHistory() },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        key(healthFeatureRefreshKey) {
                            HealthProfileFeature(repositories)
                        }
                    }

                    "appointments" -> RebuildFeaturePage(
                        title = "Appointments",
                        subtitle = "Schedule, prepare, manage reminders, and review appointment history.",
                        onBack = { selectedMoreFeature = null },
                        onSearch = ::openGlobalSearch,
                        onHistory = { appointmentWorkflow.showHistory = true },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CareAppointmentTrackerCard(
                                appointments = careAppointments,
                                onSchedule = { openAppointmentStart() },
                                onOpenHistory = { appointmentWorkflow.showHistory = true },
                                onViewAppointment = { appointment ->
                                    openAppointmentEditor(appointment, returnToHistory = false)
                                }
                            )
                        }
                    }

                    "visits" -> RebuildFeaturePage(
                        title = "Providers & Care Places",
                        subtitle = "Reusable care directory and completed visit history.",
                        onBack = { selectedMoreFeature = null },
                        onSearch = ::openGlobalSearch,
                        onHistory = { showCareVisitHistoryDialog = true },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CareVisitTrackerCard(
                                visits = careVisits,
                                onLogVisit = { showCareVisitStartDialog = true },
                                onOpenHistory = { showCareVisitHistoryDialog = true }
                            )
                        }
                    }

                    "activity" -> RebuildFeaturePage(
                        title = "Connected Activity",
                        subtitle = "Health Connect permissions, walking source, and the latest stored totals.",
                        onBack = { selectedMoreFeature = null },
                        onSearch = ::openGlobalSearch,
                        onHistory = { openDailyHistory() },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
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
                                onRefresh = { refreshHealthActivity(showFeedback = true) },
                                onManageAccess = {
                                    healthConnectManager.openHealthConnectSettings()
                                },
                                onInstallOrUpdate = {
                                    healthConnectManager.openInstallOrUpdate()
                                }
                            )
                        }
                    }

                    "customize", "notifications" -> RebuildFeaturePage(
                        title = if (selectedMoreFeature == "notifications") {
                            "Notifications & Reminders"
                        } else {
                            "Customize Daily Rebuild"
                        },
                        subtitle = if (selectedMoreFeature == "notifications") {
                            "Control every notification, reminder type, timing choice, and Android permission."
                        } else {
                            "Choose what appears, how it is ordered, units, and interface preferences."
                        },
                        onBack = { selectedMoreFeature = null },
                        onSearch = ::openGlobalSearch,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        CustomizeDailyRebuildScreen(
                            preferences = appPreferences,
                            onPreferencesChange = { updated ->
                                val notificationSettingsChanged =
                                    updated.notificationsEnabled != appPreferences.notificationsEnabled ||
                                        updated.appointmentRemindersEnabled != appPreferences.appointmentRemindersEnabled ||
                                        updated.meetingRemindersEnabled != appPreferences.meetingRemindersEnabled ||
                                        updated.iopRemindersEnabled != appPreferences.iopRemindersEnabled ||
                                        updated.iopAttendanceFollowUpEnabled != appPreferences.iopAttendanceFollowUpEnabled ||
                                        updated.meetingReminderLeadMinutes != appPreferences.meetingReminderLeadMinutes ||
                                        updated.iopReminderLeadMinutes != appPreferences.iopReminderLeadMinutes ||
                                        updated.notificationSnoozeMinutes != appPreferences.notificationSnoozeMinutes

                                appPreferences = updated
                                appPreferencesRepository.save(updated)
                                statsViewModel.updatePreferences(updated)

                                if (notificationSettingsChanged) {
                                    if (!updated.notificationsEnabled) {
                                        DailyRebuildReminderScheduler.cancelAllScheduled(context)
                                        DailyRebuildReminderScheduler.cancelAllDisplayed(context)
                                    } else {
                                        requestNotificationPermissionIfNeeded(
                                            updated.appointmentRemindersEnabled ||
                                                updated.meetingRemindersEnabled ||
                                                updated.iopRemindersEnabled ||
                                                updated.iopAttendanceFollowUpEnabled
                                        )
                                    }
                                }
                            },
                            ignoredDataQualityValueCount = ignoredDataQualityValueCount,
                            onResetIgnoredDataQualityValues = ::resetIgnoredDataQualityWarnings,
                            appointments = careAppointments,
                            savedMeetings = savedMeetings,
                            iopGroups = iopGroups,
                            notificationPermissionGranted = notificationPermissionGranted,
                            onRequestNotificationPermission = {
                                requestNotificationPermissionIfNeeded(true)
                            },
                            onOpenAndroidNotificationSettings = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                )
                            },
                            notificationsOnly = selectedMoreFeature == "notifications",
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    "backup" -> RebuildFeaturePage(
                        title = "Data & Backup",
                        subtitle = "Create, inspect, restore, and verify portable Daily Rebuild backups.",
                        onBack = { selectedMoreFeature = null },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        BackupRestoreFeature(database = database)
                    }

                    else -> DailyRebuildHelpScreen(
                        onBack = { selectedMoreFeature = null },
                        onOpenCustomization = { selectedMoreFeature = "customize" },
                        onOpenConnectedActivity = { selectedMoreFeature = "activity" },
                        onOpenBackup = { selectedMoreFeature = "backup" },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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
            initial = migraineBeingEdited,
            onDismiss = {
                showMigraineLogDialog = false
                migraineBeingEdited = null
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
                            showQuickWaterDialog = false
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
                            showQuickWaterDialog = false
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
            },
            dismissButton = {
                if (
                    plainReusableBottleCount +
                        mioReusableBottleCount +
                        plainDisposableBottleCount +
                        mioDisposableBottleCount > 0
                ) {
                    TextButton(
                        onClick = {
                            showQuickWaterDialog = false
                            showClearWaterConfirmation = true
                        }
                    ) {
                        Text(
                            text = "Clear today’s water",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    if (showClearWaterConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearWaterConfirmation = false
                showQuickWaterDialog = true
            },
            title = { Text("Clear today’s water?") },
            text = {
                Text(
                    "All four bottle counts for today will return to zero. You can immediately restore them with Undo."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val removedPlainReusable = plainReusableBottleCount
                        val removedMioReusable = mioReusableBottleCount
                        val removedPlainDisposable = plainDisposableBottleCount
                        val removedMioDisposable = mioDisposableBottleCount

                        plainReusableBottleCount = 0
                        mioReusableBottleCount = 0
                        plainDisposableBottleCount = 0
                        mioDisposableBottleCount = 0
                        showClearWaterConfirmation = false

                        coroutineScope.launch {
                            try {
                                val currentRecord = dailyRecordDao
                                    .getRecordByDate(todayDate)
                                    ?: buildCurrentDailyRecord()
                                dailyRecordDao.saveRecord(
                                    currentRecord.copy(
                                        plainReusableBottleCount = 0,
                                        mioReusableBottleCount = 0,
                                        plainDisposableBottleCount = 0,
                                        mioDisposableBottleCount = 0,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                historyViewModel.refresh()
                                statsViewModel.refresh()
                                snackbarHostState.showUndoableDelete(
                                    message = "Today’s water cleared.",
                                    restoredMessage = "Today’s water restored."
                                ) {
                                    plainReusableBottleCount = removedPlainReusable
                                    mioReusableBottleCount = removedMioReusable
                                    plainDisposableBottleCount = removedPlainDisposable
                                    mioDisposableBottleCount = removedMioDisposable
                                    val latestRecord = dailyRecordDao
                                        .getRecordByDate(todayDate)
                                        ?: buildCurrentDailyRecord()
                                    dailyRecordDao.saveRecord(
                                        latestRecord.copy(
                                            plainReusableBottleCount = removedPlainReusable,
                                            mioReusableBottleCount = removedMioReusable,
                                            plainDisposableBottleCount = removedPlainDisposable,
                                            mioDisposableBottleCount = removedMioDisposable,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                    historyViewModel.refresh()
                                    statsViewModel.refresh()
                                }
                            } catch (_: Exception) {
                                plainReusableBottleCount = removedPlainReusable
                                mioReusableBottleCount = removedMioReusable
                                plainDisposableBottleCount = removedPlainDisposable
                                mioDisposableBottleCount = removedMioDisposable
                                snackbarHostState.showSnackbar(
                                    message = "Could not clear today’s water."
                                )
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Clear water",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearWaterConfirmation = false
                        showQuickWaterDialog = true
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQuickPainDialog) {
        DailyPainDialog(
            currentBackPain = backPain,
            currentShinPain = shinPain,
            wasRecordedToday = painRecorded,
            onSaveDailyHighs = { newBackPain, newShinPain ->
                backPain = newBackPain
                shinPain = newShinPain
                painRecorded = true
                showQuickPainDialog = false
            },
            onCorrectValues = { correctedBackPain, correctedShinPain ->
                backPain = correctedBackPain
                shinPain = correctedShinPain
                painRecorded = true
                showQuickPainDialog = false

                // The automatic daily save persists this correction immediately.
            },
            onDeleteRecord = if (painRecorded) {
                {
                    val removedBackPain = backPain
                    val removedShinPain = shinPain
                    backPain = 0f
                    shinPain = 0f
                    painRecorded = false
                    showQuickPainDialog = false

                    coroutineScope.launch {
                        try {
                            val currentRecord = dailyRecordDao
                                .getRecordByDate(todayDate)
                                ?: buildCurrentDailyRecord()
                            dailyRecordDao.saveRecord(
                                currentRecord.copy(
                                    backPain = 0f,
                                    shinPain = 0f,
                                    painRecorded = false,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            historyViewModel.refresh()
                            statsViewModel.refresh()
                            snackbarHostState.showUndoableDelete(
                                message = "Today’s pain entry deleted.",
                                restoredMessage = "Today’s pain entry restored."
                            ) {
                                backPain = removedBackPain
                                shinPain = removedShinPain
                                painRecorded = true
                                val latestRecord = dailyRecordDao
                                    .getRecordByDate(todayDate)
                                    ?: buildCurrentDailyRecord()
                                dailyRecordDao.saveRecord(
                                    latestRecord.copy(
                                        backPain = removedBackPain,
                                        shinPain = removedShinPain,
                                        painRecorded = true,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                historyViewModel.refresh()
                                statsViewModel.refresh()
                            }
                        } catch (_: Exception) {
                            backPain = removedBackPain
                            shinPain = removedShinPain
                            painRecorded = true
                            snackbarHostState.showSnackbar(
                                message = "Could not delete today’s pain entry."
                            )
                        }
                    }
                }
            } else {
                null
            },
            onDismiss = {
                showQuickPainDialog = false
            }
        )
    }

    if (showBarcodeVerificationDialog) {
        val barcodeLengthIsValid =
            pendingBarcodeText.length == 8 ||
                pendingBarcodeText.length == 12 ||
                pendingBarcodeText.length == 13

        RebuildInputDialog(
            title = "Verify barcode",
            subtitle = "Compare the scanned number with the digits printed beneath the barcode.",
            onDismissRequest = {
                showBarcodeVerificationDialog = false
            },
            primaryActionText = "Look up food",
            onPrimaryAction = {
                val verifiedBarcode = pendingBarcodeText
                val forMealBuilder = pendingScanForMealBuilder

                showBarcodeVerificationDialog = false

                lookupFoodBarcode(
                    barcodeText = verifiedBarcode,
                    forMealBuilder = forMealBuilder
                )
            },
            primaryActionEnabled = barcodeLengthIsValid
        ) {
            RebuildDialogInfoPanel {
                Text(
                    text = "Correct any digits the scanner missed before continuing.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Most food barcodes contain 8, 12, or 13 digits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = pendingBarcodeText,
                onValueChange = { newValue ->
                    pendingBarcodeText = newValue.filter(Char::isDigit)
                },
                label = { Text("Printed barcode digits") },
                supportingText = {
                    Text(
                        if (barcodeLengthIsValid) {
                            "${pendingBarcodeText.length} digits — ready to look up."
                        } else {
                            "${pendingBarcodeText.length} digits entered."
                        }
                    )
                },
                isError = pendingBarcodeText.isNotBlank() && !barcodeLengthIsValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    val forMealBuilder = pendingScanForMealBuilder
                    showBarcodeVerificationDialog = false
                    startFoodBarcodeScan(forMealBuilder = forMealBuilder)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Scan barcode again")
            }
        }
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

            destinationLabel =
                historicalFoodLogDate?.let { date ->
                    runCatching {
                        LocalDate.parse(date).format(
                            DateTimeFormatter.ofPattern("MMM d", Locale.US)
                        )
                    }.getOrDefault(date)
                } ?: "today",

            onAddToToday = { savedMeal, multiplier, selectedOptionalIngredientIds ->
                coroutineScope.launch {
                    isAddingSavedMeal = true

                    try {
                        val productsById =
                            savedProducts.associateBy {
                                it.id
                            }

                        val sortedIngredients =
                            savedMeal.ingredients
                                .filter { ingredient ->
                                    !ingredient.isOptional ||
                                        ingredient.id in selectedOptionalIngredientIds
                                }
                                .sortedBy {
                                    it.sortOrder
                                }

                        if (sortedIngredients.isEmpty()) {
                            error(
                                "Saved meal has no ingredients."
                            )
                        }

                        fun unitForIngredient(
                            ingredient: SavedMealIngredient,
                            product: FoodProduct
                        ): String =
                            if (
                                ingredient.amountMode ==
                                MealAmountMode.LABEL_SERVINGS
                            ) {
                                "servings"
                            } else {
                                product.servingUnit
                            }

                        fun normalizedIngredientKey(
                            productId: Long,
                            unit: String
                        ): String =
                            "$productId|${unit.trim().lowercase(Locale.US)}"

                        val expectedIngredientKeys =
                            sortedIngredients.map { ingredient ->
                                val product =
                                    productsById[ingredient.productId]
                                        ?: error(
                                            "Saved meal contains a missing food."
                                        )

                                normalizedIngredientKey(
                                    productId = product.id,
                                    unit = unitForIngredient(
                                        ingredient,
                                        product
                                    )
                                )
                            }.sorted()

                        val existingMealGroup =
                            foodDao.getEntriesForDate(activeFoodLogDate)
                                .filter {
                                    !it.mealLogId.isNullOrBlank()
                                }
                                .groupBy {
                                    it.mealLogId.orEmpty()
                                }
                                .values
                                .firstOrNull { group ->
                                    val first = group.firstOrNull()
                                        ?: return@firstOrNull false

                                    if (
                                        first.savedMealId ==
                                        savedMeal.meal.id
                                    ) {
                                        true
                                    } else if (
                                        first.savedMealId == null &&
                                        first.mealName?.trim()
                                            ?.equals(
                                                savedMeal.meal.name.trim(),
                                                ignoreCase = true
                                            ) == true
                                    ) {
                                        group.map { entry ->
                                            normalizedIngredientKey(
                                                productId = entry.productId,
                                                unit = entry.unit
                                            )
                                        }.sorted() == expectedIngredientKeys
                                    } else {
                                        false
                                    }
                                }

                        val mealLogId =
                            existingMealGroup
                                ?.firstOrNull()
                                ?.mealLogId
                                ?.takeIf { it.isNotBlank() }
                                ?: UUID.randomUUID().toString()

                        val existingMealQuantity =
                            if (existingMealGroup == null) {
                                0.0
                            } else {
                                val first = existingMealGroup.first()

                                if (first.savedMealId != null) {
                                    existingMealGroup.maxOf {
                                        it.mealQuantity
                                    }.coerceAtLeast(0.0)
                                } else {
                                    /*
                                     * Version-15 meal logs do not have a
                                     * savedMealId. Infer their original
                                     * multiplier from ingredient amounts so
                                     * the first merge keeps the correct total.
                                     */
                                    val inferredMultipliers =
                                        sortedIngredients.mapNotNull { ingredient ->
                                            val product =
                                                productsById[ingredient.productId]
                                                    ?: return@mapNotNull null
                                            val unit = unitForIngredient(
                                                ingredient,
                                                product
                                            )
                                            val existingEntry =
                                                existingMealGroup.firstOrNull { entry ->
                                                    entry.productId == product.id &&
                                                        entry.unit.trim().equals(
                                                            unit.trim(),
                                                            ignoreCase = true
                                                        )
                                                }

                                            if (
                                                existingEntry == null ||
                                                ingredient.amount <= 0.0
                                            ) {
                                                null
                                            } else {
                                                existingEntry.quantity /
                                                    ingredient.amount
                                            }
                                        }.filter { it > 0.0 }

                                    inferredMultipliers
                                        .average()
                                        .takeIf { !it.isNaN() && it > 0.0 }
                                        ?: 1.0
                                }
                            }

                        val updatedMealQuantity =
                            existingMealQuantity + multiplier
                        val updatedMealQuantityText =
                            if (updatedMealQuantity % 1.0 == 0.0) {
                                updatedMealQuantity.toLong().toString()
                            } else {
                                String.format(
                                    Locale.US,
                                    "%.2f",
                                    updatedMealQuantity
                                ).trimEnd('0').trimEnd('.')
                            }

                        val entriesToAdd =
                            sortedIngredients.map { ingredient ->
                                val product =
                                    productsById[ingredient.productId]
                                        ?: error(
                                            "Saved meal contains a missing food."
                                        )

                                val totalAmount =
                                    ingredient.amount * multiplier

                                val servings =
                                    when (ingredient.amountMode) {
                                        MealAmountMode.LABEL_SERVINGS -> {
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
                                    date = activeFoodLogDate,
                                    productId = product.id,
                                    quantity = totalAmount,
                                    unit = unitForIngredient(
                                        ingredient,
                                        product
                                    ),
                                    mealName = savedMeal.meal.name,
                                    mealLogId = mealLogId,
                                    savedMealId = savedMeal.meal.id,
                                    mealQuantity = updatedMealQuantity,
                                    productNameSnapshot = product.name,
                                    calories =
                                        product.caloriesPerServing * servings,
                                    proteinGrams =
                                        product.proteinGramsPerServing * servings,
                                    carbohydrateGrams =
                                        product.carbohydrateGramsPerServing * servings,
                                    fatGrams =
                                        product.fatGramsPerServing * servings,
                                    sodiumMilligrams =
                                        product.sodiumMilligramsPerServing * servings
                                )
                            }

                        database.withTransaction {
                            if (existingMealGroup == null) {
                                entriesToAdd.forEach { entry ->
                                    foodDao.addFoodEntry(entry)
                                }
                            } else {
                                val availableExisting =
                                    existingMealGroup.toMutableList()

                                entriesToAdd.forEach { addedEntry ->
                                    val existingEntry =
                                        availableExisting.firstOrNull { current ->
                                            current.productId ==
                                                addedEntry.productId &&
                                                current.unit.trim().equals(
                                                    addedEntry.unit.trim(),
                                                    ignoreCase = true
                                                )
                                        }

                                    if (existingEntry == null) {
                                        foodDao.addFoodEntry(
                                            addedEntry.copy(
                                                mealLogId = mealLogId
                                            )
                                        )
                                    } else {
                                        foodDao.updateFoodEntry(
                                            existingEntry.copy(
                                                quantity =
                                                    existingEntry.quantity +
                                                        addedEntry.quantity,
                                                mealName = savedMeal.meal.name,
                                                savedMealId =
                                                    savedMeal.meal.id,
                                                mealQuantity =
                                                    updatedMealQuantity,
                                                calories =
                                                    existingEntry.calories +
                                                        addedEntry.calories,
                                                proteinGrams =
                                                    existingEntry.proteinGrams +
                                                        addedEntry.proteinGrams,
                                                carbohydrateGrams =
                                                    existingEntry.carbohydrateGrams +
                                                        addedEntry.carbohydrateGrams,
                                                fatGrams =
                                                    existingEntry.fatGrams +
                                                        addedEntry.fatGrams,
                                                sodiumMilligrams =
                                                    existingEntry.sodiumMilligrams +
                                                        addedEntry.sodiumMilligrams
                                            )
                                        )
                                        availableExisting.remove(
                                            existingEntry
                                        )
                                    }
                                }

                                /*
                                 * Keep every ingredient row in the group on
                                 * the same accumulated meal quantity, even if
                                 * the saved-meal recipe changed after the
                                 * original log was created.
                                 */
                                availableExisting.forEach { existingEntry ->
                                    foodDao.updateFoodEntry(
                                        existingEntry.copy(
                                            mealName = savedMeal.meal.name,
                                            savedMealId = savedMeal.meal.id,
                                            mealQuantity =
                                                updatedMealQuantity
                                        )
                                    )
                                }
                            }
                        }

                        synchronizeFoodRecordedForDate(activeFoodLogDate)
                        showSavedMealsDialog = false

                        if (historicalFoodLogDate != null) {
                            returnToHistoricalDay(
                                if (existingMealGroup == null) {
                                    "${savedMeal.meal.name} added to the selected day."
                                } else {
                                    "${savedMeal.meal.name} quantity updated to " +
                                        "$updatedMealQuantityText on the selected day."
                                }
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                message =
                                    if (existingMealGroup == null) {
                                        "${savedMeal.meal.name} added to today."
                                    } else {
                                        "${savedMeal.meal.name} quantity updated to " +
                                            "$updatedMealQuantityText."
                                    }
                            )
                        }
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
                    if (historicalFoodLogDate != null) {
                        returnToHistoricalDay()
                    }
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

                                            isOptional =
                                                ingredient
                                                    .isOptional,

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
                if (historicalFoodLogDate != null) {
                    returnToHistoricalDay()
                }
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

            onSetCondiment = { product, isCondiment ->
                coroutineScope.launch {
                    try {
                        foodDao.updateProduct(
                            product.copy(
                                isCondiment = isCondiment,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        savedProducts = foodDao.getAllProducts()
                        synchronizeFoodRecordedForProduct(product.id)
                        historyViewModel.refresh()
                        statsViewModel.refresh()
                        snackbarHostState.showSnackbar(
                            if (isCondiment) {
                                "${product.name} is now a condiment."
                            } else {
                                "${product.name} is now a regular food."
                            }
                        )
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar(
                            "Could not update the condiment setting."
                        )
                    }
                }
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

    activityFoodBeingEdited?.let { entry ->
        FoodQuantityEditDialog(
            entry = entry,
            isSaving = false,
            onDismiss = { activityFoodBeingEdited = null },
            onSave = { newQuantity ->
                activityFoodBeingEdited = null
                updateFoodEntryQuantity(entry, newQuantity)
            }
        )
    }

    activityMealLogBeingEdited?.let { mealLogId ->
        val mealEntries = foodEntries.filter {
            it.mealLogId == mealLogId
        }
        val firstMealEntry = mealEntries.firstOrNull()
        if (firstMealEntry != null) {
            val currentMealQuantity = mealEntries
                .maxOfOrNull { it.mealQuantity }
                ?.takeIf { it > 0.0 }
                ?: 1.0
            LoggedMealQuantityEditDialog(
                mealName = firstMealEntry.mealName
                    ?.takeIf(String::isNotBlank)
                    ?: "Saved meal",
                currentQuantity = currentMealQuantity,
                isSaving = false,
                onDismiss = { activityMealLogBeingEdited = null },
                onSave = { newQuantity ->
                    activityMealLogBeingEdited = null
                    updateMealLogQuantity(mealLogId, newQuantity)
                }
            )
        } else {
            LaunchedEffect(mealLogId) {
                activityMealLogBeingEdited = null
                snackbarHostState.showSnackbar(
                    message = "That meal entry is no longer available."
                )
            }
        }
    }

    activityMobilityBeingEdited?.let { session ->
        MobilitySessionEditDialog(
            session = session,
            isSaving = isSavingMobility,
            onDismiss = {
                if (!isSavingMobility) {
                    activityMobilityBeingEdited = null
                }
            },
            onSave = { updatedSession ->
                activityMobilityBeingEdited = null
                updateMobilitySession(updatedSession)
            }
        )
    }

    if (showDailyHistoryDialog) {
        DailyHistoryDialog(
            days = dailyHistoryDays,
            initialSelectedDate = initialDailyHistoryDate,
            selectedFilter = historyState.selectedFilter,
            onFilterChange = historyViewModel::selectFilter,
            isLoading = isLoadingDailyHistory,
            isDeletingDay =
                isDeletingDailyHistoryDay,
            isUpdatingDay = isUpdatingHistoryDay,

            onAddSavedFood = { day ->
                beginHistoricalFoodEdit(day.date)
                openSavedFoodsScreen()
            },

            onAddSavedMeal = { day ->
                beginHistoricalFoodEdit(day.date)
                openSavedMealsScreen()
            },

            onAddFoodManually = { day ->
                beginHistoricalFoodEdit(day.date)
                isCreatingFoodForMeal = false
                isEditingSavedFood = false
                scannedFoodPrefill = null
                showManualFoodDialog = true
            },

            onUpdateWater = { day, counts ->
                historicalWaterSaveJob?.cancel()
                historicalWaterSaveJob = coroutineScope.launch {
                    delay(250L)
                    isUpdatingHistoryDay = true

                    try {
                        val existingRecord =
                            dailyRecordDao.getRecordByDate(day.date)
                                ?: if (day.date == todayDate) {
                                    buildCurrentDailyRecord()
                                } else {
                                    buildEmptyHistoricalDailyRecord(day.date)
                                }

                        dailyRecordDao.saveRecord(
                            existingRecord.copy(
                                plainReusableBottleCount = counts.plainReusable,
                                mioReusableBottleCount = counts.mioReusable,
                                plainDisposableBottleCount = counts.plainDisposable,
                                mioDisposableBottleCount = counts.mioDisposable,
                                updatedAt = System.currentTimeMillis()
                            )
                        )

                        if (day.date == todayDate) {
                            dayReloadToken++
                        }

                        historyViewModel.refresh()
                        if (selectedMainTab == AppNavigationViewModel.STATS_TAB) {
                            statsViewModel.refresh()
                        }
                    } catch (exception: Exception) {
                        snackbarHostState.showSnackbar(
                            "Could not update water for that day."
                        )
                    } finally {
                        isUpdatingHistoryDay = false
                    }
                }
            },

            onUpdateFoodEntryQuantity = { day, entry, newQuantity ->
                coroutineScope.launch {
                    isUpdatingHistoryDay = true
                    try {
                        val updatedEntry =
                            scaleFoodEntryToQuantity(
                                entry = entry,
                                newQuantity = newQuantity
                            )

                        foodDao.updateFoodEntry(updatedEntry)
                        synchronizeFoodRecordedForDate(day.date)

                        if (day.date == todayDate) {
                            dayReloadToken++
                        }

                        historyViewModel.refresh()
                        statsViewModel.refresh()

                        snackbarHostState.showSnackbar(
                            "Food quantity updated."
                        )
                    } catch (
                        exception: Exception
                    ) {
                        snackbarHostState.showSnackbar(
                            "Could not update that food quantity."
                        )
                    } finally {
                        isUpdatingHistoryDay = false
                    }
                }
            },

            onUpdateMealQuantity = { day, mealLogId, newQuantity ->
                updateMealLogQuantity(
                    mealLogId = mealLogId,
                    newQuantity = newQuantity,
                    date = day.date
                )
            },

            onDeleteFoodEntry = { day, entry ->
                coroutineScope.launch {
                    isUpdatingHistoryDay = true
                    try {
                        foodDao.deleteFoodEntryById(entry.id)
                        synchronizeFoodRecordedForDate(day.date)
                        if (day.date == todayDate) dayReloadToken++
                        historyViewModel.refresh()
                        statsViewModel.refresh()
                        snackbarHostState.showUndoableDelete(
                            message = "Food entry removed.",
                            restoredMessage = "Food entry restored."
                        ) {
                            foodDao.addFoodEntry(entry)
                            synchronizeFoodRecordedForDate(day.date)
                            if (day.date == todayDate) dayReloadToken++
                            historyViewModel.refresh()
                            statsViewModel.refresh()
                        }
                    } catch (exception: Exception) {
                        snackbarHostState.showSnackbar("Could not remove that food entry.")
                    } finally {
                        isUpdatingHistoryDay = false
                    }
                }
            },

            onDeleteMealLog = { day, mealLogId ->
                coroutineScope.launch {
                    isUpdatingHistoryDay = true
                    try {
                        val removedEntries = day.foodEntries.filter {
                            it.mealLogId == mealLogId
                        }
                        foodDao.deleteFoodEntriesByMealLogId(mealLogId)
                        synchronizeFoodRecordedForDate(day.date)
                        if (day.date == todayDate) dayReloadToken++
                        historyViewModel.refresh()
                        statsViewModel.refresh()
                        snackbarHostState.showUndoableDelete(
                            message = "Logged meal removed.",
                            restoredMessage = "Logged meal restored."
                        ) {
                            database.withTransaction {
                                removedEntries.forEach { entry ->
                                    foodDao.addFoodEntry(entry)
                                }
                            }
                            synchronizeFoodRecordedForDate(day.date)
                            if (day.date == todayDate) dayReloadToken++
                            historyViewModel.refresh()
                            statsViewModel.refresh()
                        }
                    } catch (exception: Exception) {
                        snackbarHostState.showSnackbar("Could not remove that logged meal.")
                    } finally {
                        isUpdatingHistoryDay = false
                    }
                }
            },

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
                            lifeMaintenanceLogs = lifeMaintenanceDao.getAllLogs()

                            if (day.date == todayDate) {
                                dayReloadToken++
                            }

                            if (selectedMainTab == AppNavigationViewModel.STATS_TAB) {
                                statsViewModel.refresh()
                            }
                        }

                        snackbarHostState.showSnackbar(message)
                    }
                }
            },

            onDismiss = {
                if (!isDeletingDailyHistoryDay) {
                    showDailyHistoryDialog = false
                    initialDailyHistoryDate = null
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
                    historicalFoodLogDate?.let { date ->
                        val formattedDate =
                            runCatching {
                                LocalDate.parse(date).format(
                                    DateTimeFormatter.ofPattern("MMM d", Locale.US)
                                )
                            }.getOrDefault(date)

                        "Add Food to $formattedDate"
                    }
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
                    } else if (historicalFoodLogDate != null) {
                        returnToHistoricalDay()
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

                            synchronizeFoodRecordedForProduct(productId)
                            historyViewModel.refresh()
                            statsViewModel.refresh()

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
                                    activeFoodLogDate,

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

                        val (
                            savedEntry,
                            mergedWithExisting
                        ) = addOrMergeIndividualFoodEntry(entry)

                        synchronizeFoodRecordedForDate(activeFoodLogDate)

                        showManualFoodDialog = false
                        scannedFoodPrefill = null
                        manualBarcodeSavePolicy = BarcodeSavePolicy.NORMAL
                        barcodeViewModel.resetSavePolicy()

                        val successMessage =
                            if (mergedWithExisting) {
                                "${savedEntry.productNameSnapshot} was already logged, so its quantity was increased."
                            } else {
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
                            }

                        if (historicalFoodLogDate != null) {
                            returnToHistoricalDay(
                                if (mergedWithExisting) {
                                    "${savedEntry.productNameSnapshot} quantity increased on the selected day."
                                } else {
                                    successMessage.replace(
                                        "Food added",
                                        "Food added to the selected day"
                                    )
                                }
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                message = successMessage
                            )
                        }
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

