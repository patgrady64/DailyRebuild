package com.pgdevhouse.dailyrebuild

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CarePlace
import com.pgdevhouse.dailyrebuild.data.local.CareProvider
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val CARE_APPOINTMENT_STATUSES = listOf(
    "Scheduled",
    "Confirmed",
    "Completed",
    "Cancelled",
    "Rescheduled",
    "Missed"
)

val CARE_TRANSPORTATION_MODES = listOf(
    "Not planned",
    "Walk",
    "Bus",
    "Medical transportation",
    "Family or friend",
    "Taxi / rideshare",
    "Drive myself",
    "Other"
)

/** Keeps the appointment workflow out of the already-large MainActivity state graph. */
class AppointmentWorkflowState {
    var showStart by mutableStateOf(false)
    var showProviderPicker by mutableStateOf(false)
    var showEditor by mutableStateOf(false)
    var showHistory by mutableStateOf(false)
    var selectedPlace by mutableStateOf<CarePlace?>(null)
    var selectedProvider by mutableStateOf<CareProvider?>(null)
    var editingAppointment by mutableStateOf<CareAppointment?>(null)
    var deletingAppointment by mutableStateOf<CareAppointment?>(null)
    var convertingAppointment by mutableStateOf<CareAppointment?>(null)
    var oneTimeAppointment by mutableStateOf(false)
    var returnToHistoryAfterSave by mutableStateOf(false)
    var placeEditorActive by mutableStateOf(false)
    var providerEditorActive by mutableStateOf(false)
    var continueAfterPlaceSave by mutableStateOf(false)
    var continueAfterProviderSave by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
    var isDeleting by mutableStateOf(false)

    fun clearSelection() {
        selectedPlace = null
        selectedProvider = null
        editingAppointment = null
        oneTimeAppointment = false
    }
}

data class AppointmentDraft(
    val id: Long = 0L,
    val placeId: Long? = null,
    val providerId: Long? = null,
    val date: String,
    val scheduledAt: Long,
    val status: String,
    val visitCategory: String,
    val visitFormat: String,
    val placeName: String,
    val placeCategory: String,
    val providerName: String,
    val providerCredentials: String,
    val providerSpecialty: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val placePhone: String,
    val providerPhone: String,
    val reasonForAppointment: String,
    val transportationMode: String,
    val transportationDetails: String,
    val leaveByAt: Long?,
    val transportationConfirmed: Boolean,
    val questionsToAsk: String,
    val documentsToBring: String,
    val preparationNotes: String,
    val remindOneDayBefore: Boolean,
    val remindTwoHoursBefore: Boolean,
    val convertedVisitId: Long?,
    val createdAt: Long = System.currentTimeMillis()
)


fun recentAppointmentProviders(
    place: CarePlace?,
    providers: List<CareProvider>,
    visits: List<CareVisit>,
    appointments: List<CareAppointment>
): List<CareProvider> {
    val placeId = place?.id ?: return emptyList()
    val lastUseByProviderId =
        buildMap<Long, Long> {
            visits.filter { it.providerId != null }
                .forEach { visit ->
                    val providerId = visit.providerId ?: return@forEach
                    put(
                        providerId,
                        maxOf(
                            get(providerId) ?: Long.MIN_VALUE,
                            visit.startedAt
                        )
                    )
                }
            appointments.filter { it.providerId != null }
                .forEach { appointment ->
                    val providerId =
                        appointment.providerId ?: return@forEach
                    put(
                        providerId,
                        maxOf(
                            get(providerId) ?: Long.MIN_VALUE,
                            appointment.scheduledAt
                        )
                    )
                }
        }

    return providers
        .filter { it.placeId == placeId }
        .sortedWith(
            compareByDescending<CareProvider> {
                lastUseByProviderId[it.id] ?: Long.MIN_VALUE
            }.thenBy {
                it.name.lowercase(Locale.US)
            }
        )
}

fun nextUpcomingAppointment(
    appointments: List<CareAppointment>,
    now: Long = System.currentTimeMillis()
): CareAppointment? =
    appointments
        .asSequence()
        .filter {
            it.scheduledAt >= now &&
                it.status in setOf("Scheduled", "Confirmed")
        }
        .minByOrNull { it.scheduledAt }

@Composable
fun HomeAppointmentCard(
    appointment: CareAppointment?,
    onSchedule: () -> Unit,
    onView: (CareAppointment) -> Unit
) {
    RebuildSectionCard(
        title = "Doctor appointment",
        subtitle =
            appointment?.let {
                appointmentCountdown(it)
            } ?: "No upcoming care appointment is currently scheduled.",
        accentColor = RebuildBlue
    ) {
        if (appointment == null) {
            RebuildInsetPanel {
                Text(
                    text = "Schedule a doctor's visit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keep an upcoming medical, vision, dental, or other care appointment on the calendar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onSchedule,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Schedule Appointment")
            }
        } else {
            AppointmentIdentitySummary(appointment)

            if (
                appointment.transportationMode != "Not planned" ||
                appointment.leaveByAt != null
            ) {
                val transportation =
                    buildList {
                        if (appointment.transportationMode != "Not planned") {
                            add(appointment.transportationMode)
                        }
                        appointment.leaveByAt?.let {
                            add("Leave by ${formatAppointmentTime(it)}")
                        }
                        if (appointment.transportationConfirmed) {
                            add("Confirmed")
                        }
                    }.joinToString(" · ")

                Text(
                    text = "Transportation: $transportation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onView(appointment) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("View Appointment")
                }
                OutlinedButton(
                    onClick = onSchedule,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Schedule Another")
                }
            }
        }
    }
}

@Composable
fun CareAppointmentTrackerCard(
    appointments: List<CareAppointment>,
    onSchedule: () -> Unit,
    onOpenHistory: () -> Unit,
    onViewAppointment: (CareAppointment) -> Unit
) {
    val next = nextUpcomingAppointment(appointments)

    RebuildSectionCard(
        title = "Upcoming appointments",
        subtitle =
            next?.let(::appointmentCountdown)
                ?: "No current upcoming appointment.",
        accentColor = RebuildBlue
    ) {
        if (next != null) {
            AppointmentIdentitySummary(next)
            TextButton(
                onClick = { onViewAppointment(next) }
            ) {
                Text("Open next appointment")
            }
        } else {
            Text(
                text = "Schedule the next medical, vision, dental, or other care appointment when you know it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSchedule,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Schedule")
            }
            OutlinedButton(
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("All Appointments")
            }
        }

        Text(
            text = "Appointment planning does not affect daily completion.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppointmentIdentitySummary(
    appointment: CareAppointment
) {
    RebuildInsetPanel {
        AppointmentSummaryLine(
            label = "Who",
            value = appointmentProviderDisplay(appointment)
                .ifBlank { "Provider not specified" }
        )
        AppointmentSummaryLine(
            label = "What",
            value = listOf(
                appointment.visitCategory,
                appointment.reasonForAppointment
            ).filter(String::isNotBlank)
                .joinToString(" · ")
        )
        AppointmentSummaryLine(
            label = "Where",
            value = listOf(
                appointment.placeName,
                appointmentLocationText(
                    appointment.address,
                    appointment.city,
                    appointment.state,
                    appointment.zipCode
                )
            ).filter(String::isNotBlank)
                .joinToString(" · ")
        )
        AppointmentSummaryLine(
            label = "When",
            value = formatAppointmentDateTime(
                appointment.scheduledAt
            )
        )
    }
}

@Composable
private fun AppointmentSummaryLine(
    label: String,
    value: String
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun AppointmentStartDialog(
    recentPlaces: List<CarePlace>,
    appointments: List<CareAppointment>,
    visits: List<CareVisit>,
    onSelectPlace: (CarePlace) -> Unit,
    onEditPlace: (CarePlace) -> Unit,
    onAddPlace: () -> Unit,
    onOneTimeAppointment: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Schedule Appointment",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose a recent care place or enter a one-time appointment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                RebuildSectionCard(
                    title = "Recent Places",
                    subtitle = "Places used most recently appear first.",
                    accentColor = RebuildBlue
                ) {
                    if (recentPlaces.isEmpty()) {
                        Text(
                            text = "No saved care places yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recentPlaces.forEachIndexed { index, place ->
                            val lastUsed =
                                listOfNotNull(
                                    appointments
                                        .filter { it.placeId == place.id }
                                        .maxOfOrNull { it.scheduledAt },
                                    visits
                                        .filter { it.placeId == place.id }
                                        .maxOfOrNull { it.startedAt }
                                ).maxOrNull()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlace(place) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = listOf(place.city, place.state)
                                            .filter(String::isNotBlank)
                                            .joinToString(", ")
                                            .ifBlank { place.placeCategory },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    lastUsed?.let {
                                        Text(
                                            text = "Last used ${formatAppointmentDate(it)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { onEditPlace(place) }
                                ) {
                                    Text("Edit")
                                }
                            }
                            if (index < recentPlaces.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }

                Button(
                    onClick = onAddPlace,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Add New Place")
                }

                OutlinedButton(
                    onClick = onOneTimeAppointment,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("One-Time Appointment")
                }
            }
        }
    }
}

private class AppointmentFormState(
    selectedDate: String,
    selectedTime: String,
    status: String,
    category: String,
    format: String,
    placeName: String,
    placeCategory: String,
    providerName: String,
    providerCredentials: String,
    providerSpecialty: String,
    address: String,
    city: String,
    state: String,
    zipCode: String,
    placePhone: String,
    providerPhone: String,
    reason: String,
    transportationMode: String,
    transportationDetails: String,
    leaveByDate: String,
    leaveByTime: String,
    transportationConfirmed: Boolean,
    questions: String,
    documents: String,
    preparationNotes: String,
    remindOneDay: Boolean,
    remindTwoHours: Boolean
) {
    var selectedDate by mutableStateOf(selectedDate)
    var selectedTime by mutableStateOf(selectedTime)
    var status by mutableStateOf(status)
    var category by mutableStateOf(category)
    var format by mutableStateOf(format)
    var placeName by mutableStateOf(placeName)
    var placeCategory by mutableStateOf(placeCategory)
    var providerName by mutableStateOf(providerName)
    var providerCredentials by mutableStateOf(providerCredentials)
    var providerSpecialty by mutableStateOf(providerSpecialty)
    var address by mutableStateOf(address)
    var city by mutableStateOf(city)
    var state by mutableStateOf(state)
    var zipCode by mutableStateOf(zipCode)
    var placePhone by mutableStateOf(placePhone)
    var providerPhone by mutableStateOf(providerPhone)
    var reason by mutableStateOf(reason)
    var transportationMode by mutableStateOf(transportationMode)
    var transportationDetails by mutableStateOf(transportationDetails)
    var leaveByDate by mutableStateOf(leaveByDate)
    var leaveByTime by mutableStateOf(leaveByTime)
    var transportationConfirmed by mutableStateOf(transportationConfirmed)
    var questions by mutableStateOf(questions)
    var documents by mutableStateOf(documents)
    var preparationNotes by mutableStateOf(preparationNotes)
    var remindOneDay by mutableStateOf(remindOneDay)
    var remindTwoHours by mutableStateOf(remindTwoHours)
}

private fun createAppointmentFormState(
    existing: CareAppointment?,
    savedPlace: CarePlace?,
    savedProvider: CareProvider?
): AppointmentFormState {
    val defaultDateTime =
        ZonedDateTime.now()
            .plusDays(7)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

    val appointmentDateTime =
        existing?.let {
            Instant.ofEpochMilli(it.scheduledAt)
                .atZone(ZoneId.systemDefault())
        } ?: defaultDateTime

    val leaveBy =
        existing?.leaveByAt?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
        }

    return AppointmentFormState(
        selectedDate = existing?.date ?: appointmentDateTime.toLocalDate().toString(),
        selectedTime = appointmentDateTime.toLocalTime().toString(),
        status = existing?.status ?: "Scheduled",
        category = existing?.visitCategory ?: defaultAppointmentCategory(savedPlace, savedProvider),
        format = existing?.visitFormat ?: "In person",
        placeName = existing?.placeName ?: savedPlace?.name.orEmpty(),
        placeCategory = existing?.placeCategory ?: savedPlace?.placeCategory.orEmpty(),
        providerName = existing?.providerName ?: savedProvider?.name.orEmpty(),
        providerCredentials = existing?.providerCredentials ?: savedProvider?.credentials.orEmpty(),
        providerSpecialty = existing?.providerSpecialty ?: savedProvider?.specialty.orEmpty(),
        address = existing?.address ?: savedPlace?.address.orEmpty(),
        city = existing?.city ?: savedPlace?.city.orEmpty(),
        state = existing?.state ?: savedPlace?.state.orEmpty(),
        zipCode = existing?.zipCode ?: savedPlace?.zipCode.orEmpty(),
        placePhone = existing?.placePhone ?: savedPlace?.phone.orEmpty(),
        providerPhone = existing?.providerPhone ?: savedProvider?.phone.orEmpty(),
        reason = existing?.reasonForAppointment.orEmpty(),
        transportationMode = existing?.transportationMode ?: "Not planned",
        transportationDetails = existing?.transportationDetails.orEmpty(),
        leaveByDate = leaveBy?.toLocalDate()?.toString().orEmpty(),
        leaveByTime = leaveBy?.toLocalTime()?.withSecond(0)?.withNano(0)?.toString().orEmpty(),
        transportationConfirmed = existing?.transportationConfirmed ?: false,
        questions = existing?.questionsToAsk.orEmpty(),
        documents = existing?.documentsToBring.orEmpty(),
        preparationNotes = existing?.preparationNotes.orEmpty(),
        remindOneDay = existing?.remindOneDayBefore ?: true,
        remindTwoHours = existing?.remindTwoHoursBefore ?: true
    )
}

private fun AppointmentFormState.toDraft(
    existing: CareAppointment?,
    savedPlace: CarePlace?,
    savedProvider: CareProvider?
): AppointmentDraft {
    val scheduledAt =
        LocalDate.parse(selectedDate)
            .atTime(LocalTime.parse(selectedTime))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    val leaveByAt =
        if (leaveByDate.isBlank() || leaveByTime.isBlank()) {
            null
        } else {
            LocalDate.parse(leaveByDate)
                .atTime(LocalTime.parse(leaveByTime))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

    return AppointmentDraft(
        id = existing?.id ?: 0L,
        placeId = existing?.placeId ?: savedPlace?.id,
        providerId = existing?.providerId ?: savedProvider?.id,
        date = selectedDate,
        scheduledAt = scheduledAt,
        status = status,
        visitCategory = category.trim(),
        visitFormat = format,
        placeName = placeName.trim(),
        placeCategory = placeCategory.trim(),
        providerName = providerName.trim(),
        providerCredentials = providerCredentials.trim(),
        providerSpecialty = providerSpecialty.trim(),
        address = address.trim(),
        city = city.trim(),
        state = state.trim(),
        zipCode = zipCode.trim(),
        placePhone = placePhone.trim(),
        providerPhone = providerPhone.trim(),
        reasonForAppointment = reason.trim(),
        transportationMode = transportationMode,
        transportationDetails = transportationDetails.trim(),
        leaveByAt = leaveByAt,
        transportationConfirmed = transportationConfirmed,
        questionsToAsk = questions.trim(),
        documentsToBring = documents.trim(),
        preparationNotes = preparationNotes.trim(),
        remindOneDayBefore = remindOneDay,
        remindTwoHoursBefore = remindTwoHours,
        convertedVisitId = existing?.convertedVisitId,
        createdAt = existing?.createdAt ?: System.currentTimeMillis()
    )
}

@Composable
fun AppointmentEditorDialog(
    savedPlace: CarePlace?,
    savedProvider: CareProvider?,
    existingAppointment: CareAppointment?,
    isOneTimeAppointment: Boolean,
    isSaving: Boolean,
    onSave: (AppointmentDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val form =
        remember(
            existingAppointment?.id,
            savedPlace?.id,
            savedProvider?.id
        ) {
            createAppointmentFormState(
                existingAppointment,
                savedPlace,
                savedProvider
            )
        }

    var showPlanning by rememberSaveable { mutableStateOf(true) }
    var showPreparation by rememberSaveable { mutableStateOf(true) }
    var showReminders by rememberSaveable { mutableStateOf(true) }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text =
                                if (existingAppointment == null) {
                                    "Schedule Appointment"
                                } else {
                                    "Edit Appointment"
                                },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Plan the visit, transportation, questions, documents, and reminders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text("Close")
                    }
                }

                AppointmentDateTimeFields(form)
                AppointmentIdentityFields(
                    form = form,
                    editable = isOneTimeAppointment
                )
                AppointmentCategoryFields(form)
                AppointmentStatusFields(form)

                OutlinedTextField(
                    value = form.reason,
                    onValueChange = { form.reason = it },
                    label = { Text("Reason / what the appointment is for *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                ExpandableAppointmentSection(
                    title = "Transportation planning",
                    expanded = showPlanning,
                    onToggle = { showPlanning = !showPlanning }
                ) {
                    AppointmentTransportationFields(form)
                }

                ExpandableAppointmentSection(
                    title = "Questions and documents",
                    expanded = showPreparation,
                    onToggle = { showPreparation = !showPreparation }
                ) {
                    AppointmentPreparationFields(form)
                }

                ExpandableAppointmentSection(
                    title = "Reminder notifications",
                    expanded = showReminders,
                    onToggle = { showReminders = !showReminders }
                ) {
                    AppointmentReminderFields(form)
                }

                val valid =
                    form.placeName.isNotBlank() &&
                        form.reason.isNotBlank() &&
                        form.category.isNotBlank()

                Button(
                    onClick = {
                        onSave(
                            form.toDraft(
                                existingAppointment,
                                savedPlace,
                                savedProvider
                            )
                        )
                    },
                    enabled = valid && !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (isSaving) {
                            "Saving…"
                        } else if (existingAppointment == null) {
                            "Save Appointment"
                        } else {
                            "Update Appointment"
                        }
                    )
                }

                if (!valid) {
                    Text(
                        text = "Place, appointment category, and reason are required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentDateTimeFields(
    form: AppointmentFormState
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                val date = LocalDate.parse(form.selectedDate)
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        form.selectedDate =
                            LocalDate.of(year, month + 1, day)
                                .toString()
                    },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(formatAppointmentDateOnly(form.selectedDate))
        }

        OutlinedButton(
            onClick = {
                val time = LocalTime.parse(form.selectedTime)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        form.selectedTime =
                            LocalTime.of(hour, minute).toString()
                    },
                    time.hour,
                    time.minute,
                    false
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(formatAppointmentTimeOnly(form.selectedTime))
        }
    }
}

@Composable
private fun AppointmentIdentityFields(
    form: AppointmentFormState,
    editable: Boolean
) {
    if (editable) {
        OutlinedTextField(
            value = form.placeName,
            onValueChange = { form.placeName = it },
            label = { Text("Place / practice name *") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.providerName,
            onValueChange = { form.providerName = it },
            label = { Text("Doctor / provider") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.providerCredentials,
                onValueChange = { form.providerCredentials = it },
                label = { Text("Credentials") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = form.providerSpecialty,
                onValueChange = { form.providerSpecialty = it },
                label = { Text("Specialty") },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = form.address,
            onValueChange = { form.address = it },
            label = { Text("Street address") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.city,
                onValueChange = { form.city = it },
                label = { Text("City") },
                modifier = Modifier.weight(1.4f)
            )
            OutlinedTextField(
                value = form.state,
                onValueChange = { form.state = it.uppercase(Locale.US).take(2) },
                label = { Text("State") },
                modifier = Modifier.weight(0.7f)
            )
            OutlinedTextField(
                value = form.zipCode,
                onValueChange = { form.zipCode = it },
                label = { Text("ZIP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = form.placePhone,
            onValueChange = { form.placePhone = it },
            label = { Text("Place phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        RebuildInsetPanel {
            Text(
                text = form.placeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val provider =
                listOf(
                    listOf(
                        form.providerName,
                        form.providerCredentials
                    ).filter(String::isNotBlank).joinToString(", "),
                    form.providerSpecialty
                ).filter(String::isNotBlank).joinToString(" · ")
            if (provider.isNotBlank()) Text(provider)
            val location =
                appointmentLocationText(
                    form.address,
                    form.city,
                    form.state,
                    form.zipCode
                )
            if (location.isNotBlank()) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppointmentCategoryFields(
    form: AppointmentFormState
) {
    var showCategories by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showCategories = !showCategories },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Appointment type: ${form.category}")
    }

    if (showCategories) {
        RebuildInsetPanel {
            CARE_VISIT_CATEGORIES.forEach { choice ->
                TextButton(
                    onClick = {
                        form.category = choice
                        showCategories = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(choice)
                }
            }
        }
    }

    Text(
        text = "Appointment format",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    CARE_VISIT_FORMATS.forEach { choice ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { form.format = choice },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = form.format == choice,
                onCheckedChange = { checked ->
                    if (checked) form.format = choice
                }
            )
            Text(choice)
        }
    }
}

@Composable
private fun AppointmentStatusFields(
    form: AppointmentFormState
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Status: ${form.status}")
    }

    if (expanded) {
        RebuildInsetPanel {
            CARE_APPOINTMENT_STATUSES.forEach { status ->
                TextButton(
                    onClick = {
                        form.status = status
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(status)
                }
            }
        }
    }
}

@Composable
private fun ExpandableAppointmentSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text((if (expanded) "▾ " else "▸ ") + title)
    }
    if (expanded) {
        RebuildInsetPanel {
            content()
        }
    }
}

@Composable
private fun AppointmentTransportationFields(
    form: AppointmentFormState
) {
    val context = LocalContext.current
    var showModes by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showModes = !showModes },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Transportation: ${form.transportationMode}")
    }

    if (showModes) {
        CARE_TRANSPORTATION_MODES.forEach { mode ->
            TextButton(
                onClick = {
                    form.transportationMode = mode
                    showModes = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(mode)
            }
        }
    }

    OutlinedTextField(
        value = form.transportationDetails,
        onValueChange = { form.transportationDetails = it },
        label = { Text("Transportation details") },
        supportingText = {
            Text("Bus route, pickup person, confirmation number, fare, or other plan")
        },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                val base =
                    form.leaveByDate
                        .takeIf(String::isNotBlank)
                        ?.let(LocalDate::parse)
                        ?: LocalDate.parse(form.selectedDate)
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        form.leaveByDate =
                            LocalDate.of(year, month + 1, day).toString()
                    },
                    base.year,
                    base.monthValue - 1,
                    base.dayOfMonth
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                if (form.leaveByDate.isBlank()) {
                    "Leave-by date"
                } else {
                    formatAppointmentDateOnly(form.leaveByDate)
                }
            )
        }

        OutlinedButton(
            onClick = {
                val base =
                    form.leaveByTime
                        .takeIf(String::isNotBlank)
                        ?.let(LocalTime::parse)
                        ?: LocalTime.parse(form.selectedTime)
                            .minusHours(1)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        form.leaveByTime =
                            LocalTime.of(hour, minute).toString()
                    },
                    base.hour,
                    base.minute,
                    false
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                if (form.leaveByTime.isBlank()) {
                    "Leave-by time"
                } else {
                    formatAppointmentTimeOnly(form.leaveByTime)
                }
            )
        }
    }

    if (form.leaveByDate.isNotBlank() || form.leaveByTime.isNotBlank()) {
        TextButton(
            onClick = {
                form.leaveByDate = ""
                form.leaveByTime = ""
            }
        ) {
            Text("Remove leave-by time")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                form.transportationConfirmed =
                    !form.transportationConfirmed
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = form.transportationConfirmed,
            onCheckedChange = {
                form.transportationConfirmed = it
            }
        )
        Text("Transportation is confirmed")
    }
}

@Composable
private fun AppointmentPreparationFields(
    form: AppointmentFormState
) {
    OutlinedTextField(
        value = form.questions,
        onValueChange = { form.questions = it },
        label = { Text("Questions to ask") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = form.documents,
        onValueChange = { form.documents = it },
        label = { Text("Documents or items to bring") },
        supportingText = {
            Text("ID, insurance card, medication list, referral, records, forms, glasses, or other items")
        },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = form.preparationNotes,
        onValueChange = { form.preparationNotes = it },
        label = { Text("Other preparation notes") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AppointmentReminderFields(
    form: AppointmentFormState
) {
    ReminderToggleRow(
        checked = form.remindOneDay,
        label = "Remind me about 1 day before",
        onCheckedChange = { form.remindOneDay = it }
    )
    ReminderToggleRow(
        checked = form.remindTwoHours,
        label = "Remind me about 2 hours before",
        onCheckedChange = { form.remindTwoHours = it }
    )
    Text(
        text = "Android may deliver inexact alarms a little later to protect battery life. Notification permission must be allowed.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReminderToggleRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(label)
    }
}

@Composable
fun AppointmentHistoryDialog(
    appointments: List<CareAppointment>,
    onEdit: (CareAppointment) -> Unit,
    onConvertToVisit: (CareAppointment) -> Unit,
    onDelete: (CareAppointment) -> Unit,
    onSchedule: () -> Unit,
    onDismiss: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val now = System.currentTimeMillis()

    val visible =
        appointments
            .filter { appointment ->
                val matchesSearch =
                    search.isBlank() ||
                        listOf(
                            appointment.placeName,
                            appointment.providerName,
                            appointment.providerSpecialty,
                            appointment.visitCategory,
                            appointment.reasonForAppointment,
                            appointment.status
                        ).any {
                            it.contains(search, ignoreCase = true)
                        }

                val matchesFilter =
                    when (filter) {
                        "Upcoming" ->
                            appointment.scheduledAt >= now &&
                                appointment.status in setOf("Scheduled", "Confirmed")
                        "Completed" ->
                            appointment.status == "Completed"
                        "Cancelled" ->
                            appointment.status in setOf("Cancelled", "Rescheduled", "Missed")
                        else -> true
                    }

                matchesSearch && matchesFilter
            }
            .sortedWith { first, second ->
                val firstUpcoming =
                    first.scheduledAt >= now &&
                        first.status in setOf("Scheduled", "Confirmed")
                val secondUpcoming =
                    second.scheduledAt >= now &&
                        second.status in setOf("Scheduled", "Confirmed")

                when {
                    firstUpcoming && !secondUpcoming -> -1
                    !firstUpcoming && secondUpcoming -> 1
                    firstUpcoming -> first.scheduledAt.compareTo(second.scheduledAt)
                    else -> second.scheduledAt.compareTo(first.scheduledAt)
                }
            }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Appointments",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Upcoming plans and previous appointment statuses.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                Button(
                    onClick = onSchedule,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Schedule Appointment")
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search appointments") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        listOf("All", "Upcoming"),
                        listOf("Completed", "Cancelled")
                    ).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowOptions.forEach { option ->
                                OutlinedButton(
                                    onClick = { filter = option },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(option)
                                }
                            }
                        }
                    }
                }

                if (visible.isEmpty()) {
                    RebuildInsetPanel {
                        Text("No appointments match this view.")
                    }
                }

                visible.forEach { appointment ->
                    val expanded = expandedId == appointment.id
                    RebuildInsetPanel {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedId =
                                        if (expanded) null else appointment.id
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appointment.placeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatAppointmentDateTime(appointment.scheduledAt),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = listOf(
                                        appointmentProviderDisplay(appointment),
                                        appointment.visitCategory
                                    ).filter(String::isNotBlank)
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RebuildStatusBadge(appointment.status)
                        }

                        if (expanded) {
                            HorizontalDivider()
                            AppointmentDetailLine(
                                "Reason",
                                appointment.reasonForAppointment
                            )
                            AppointmentDetailLine(
                                "Location",
                                appointmentLocationText(
                                    appointment.address,
                                    appointment.city,
                                    appointment.state,
                                    appointment.zipCode
                                )
                            )
                            AppointmentDetailLine(
                                "Transportation",
                                listOf(
                                    appointment.transportationMode
                                        .takeUnless { it == "Not planned" }
                                        .orEmpty(),
                                    appointment.transportationDetails,
                                    appointment.leaveByAt?.let {
                                        "Leave by ${formatAppointmentDateTime(it)}"
                                    }.orEmpty(),
                                    if (appointment.transportationConfirmed) {
                                        "Confirmed"
                                    } else {
                                        ""
                                    }
                                ).filter(String::isNotBlank)
                                    .joinToString(" · ")
                            )
                            AppointmentDetailLine(
                                "Questions",
                                appointment.questionsToAsk
                            )
                            AppointmentDetailLine(
                                "Documents",
                                appointment.documentsToBring
                            )
                            AppointmentDetailLine(
                                "Preparation",
                                appointment.preparationNotes
                            )
                            AppointmentDetailLine(
                                "Reminders",
                                buildList {
                                    if (appointment.remindOneDayBefore) {
                                        add("1 day before")
                                    }
                                    if (appointment.remindTwoHoursBefore) {
                                        add("2 hours before")
                                    }
                                }.joinToString(" and ")
                                    .ifBlank { "Off" }
                            )
                            if (appointment.convertedVisitId != null) {
                                Text(
                                    text = "Converted into a completed care visit.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onEdit(appointment) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Edit")
                                }

                                val canConvert =
                                    appointment.convertedVisitId == null &&
                                        appointment.status !in setOf(
                                            "Cancelled",
                                            "Rescheduled",
                                            "Missed"
                                        )

                                if (canConvert) {
                                    Button(
                                        onClick = {
                                            onConvertToVisit(appointment)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Complete as Visit")
                                    }
                                }
                            }

                            TextButton(
                                onClick = { onDelete(appointment) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete Appointment")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AppointmentDetailLine(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DeleteAppointmentDialog(
    appointment: CareAppointment,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        title = { Text("Delete appointment?") },
        text = {
            Text(
                "${appointment.placeName} on " +
                    formatAppointmentDateTime(appointment.scheduledAt) +
                    " will be removed. Saved places, providers, and completed visits remain."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                Text(if (isDeleting) "Deleting…" else "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun defaultAppointmentCategory(
    place: CarePlace?,
    provider: CareProvider?
): String {
    val combined =
        listOf(
            place?.placeCategory.orEmpty(),
            provider?.specialty.orEmpty()
        ).joinToString(" ").lowercase(Locale.US)

    return when {
        "cardio" in combined -> "Cardiology"
        "diabet" in combined || "endocr" in combined -> "Diabetes / endocrinology"
        "vision" in combined || "optom" in combined || "ophthal" in combined -> "Vision"
        "dental" in combined || "dentist" in combined -> "Dental"
        "mental" in combined || "therapy" in combined || "psychi" in combined -> "Mental health"
        "physical therapy" in combined -> "Physical therapy"
        "lab" in combined -> "Laboratory"
        "imaging" in combined || "radiology" in combined -> "Imaging"
        else -> "Primary care"
    }
}

fun appointmentProviderDisplay(
    appointment: CareAppointment
): String =
    listOf(
        listOf(
            appointment.providerName,
            appointment.providerCredentials
        ).filter(String::isNotBlank).joinToString(", "),
        appointment.providerSpecialty
    ).filter(String::isNotBlank).joinToString(" · ")

fun appointmentLocationText(
    address: String,
    city: String,
    state: String,
    zipCode: String
): String =
    listOf(
        address,
        listOf(
            city,
            state,
            zipCode
        ).filter(String::isNotBlank)
            .joinToString(" ")
    ).filter(String::isNotBlank)
        .joinToString(" · ")

fun appointmentCountdown(
    appointment: CareAppointment
): String {
    val appointmentDate =
        Instant.ofEpochMilli(appointment.scheduledAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    val days =
        ChronoUnit.DAYS.between(
            LocalDate.now(),
            appointmentDate
        )

    return when (days) {
        0L -> "Today"
        1L -> "Tomorrow · 1 day away"
        else -> "$days days away"
    }
}

fun formatAppointmentDateTime(
    timestamp: Long
): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "EEEE, MMMM d, yyyy 'at' h:mm a",
                Locale.US
            )
        )

fun formatAppointmentDate(
    timestamp: Long
): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "MMM d, yyyy",
                Locale.US
            )
        )

fun formatAppointmentTime(
    timestamp: Long
): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "h:mm a",
                Locale.US
            )
        )

private fun formatAppointmentDateOnly(
    date: String
): String =
    LocalDate.parse(date)
        .format(
            DateTimeFormatter.ofPattern(
                "MMM d, yyyy",
                Locale.US
            )
        )

private fun formatAppointmentTimeOnly(
    time: String
): String =
    LocalTime.parse(time)
        .format(
            DateTimeFormatter.ofPattern(
                "h:mm a",
                Locale.US
            )
        )
