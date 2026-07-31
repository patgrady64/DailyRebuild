package com.pgdevhouse.dailyrebuild

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import java.time.format.DateTimeFormatter
import java.util.Locale

val CARE_VISIT_CATEGORIES = listOf(
    "Primary care",
    "Cardiology",
    "Diabetes / endocrinology",
    "Vision",
    "Dental",
    "Mental health",
    "Urgent care",
    "Emergency room",
    "Laboratory",
    "Imaging",
    "Physical therapy",
    "Specialist",
    "Other"
)

val CARE_VISIT_FORMATS = listOf(
    "In person",
    "Telehealth",
    "Phone"
)

data class CarePlaceDraft(
    val id: Long = 0L,
    val name: String,
    val placeCategory: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val phone: String,
    val website: String,
    val patientPortal: String,
    val notes: String,
    val active: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

data class CareProviderDraft(
    val id: Long = 0L,
    val placeId: Long,
    val name: String,
    val credentials: String,
    val specialty: String,
    val phone: String,
    val notes: String,
    val active: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

data class CareVisitDraft(
    val id: Long = 0L,
    val placeId: Long? = null,
    val providerId: Long? = null,
    val date: String,
    val startedAt: Long,
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
    val reasonForVisit: String,
    val visitSummary: String,
    val testsProcedures: String,
    val resultsDiscussed: String,
    val instructions: String,
    val medicationChanges: String,
    val referrals: String,
    val followUpDate: String?,
    val notes: String,
    val weightPounds: Double?,
    val systolic: Int?,
    val diastolic: Int?,
    val a1c: Double?,
    val bloodGlucose: Double?,
    val cholesterolTotal: Double?,
    val cholesterolLdl: Double?,
    val cholesterolHdl: Double?,
    val triglycerides: Double?,
    val copyCompatibleMeasurements: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Composable
fun CareVisitTrackerCard(
    visits: List<CareVisit>,
    onLogVisit: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val mostRecent = visits.maxByOrNull { it.startedAt }
    val today = LocalDate.now()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weekEnd = weekStart.plusDays(6)
    val visitsThisWeek = visits.count {
        it.date >= weekStart.toString() && it.date <= weekEnd.toString()
    }

    RebuildSectionCard(
        title = "Care visits",
        subtitle =
            mostRecent?.let {
                "Last: ${formatCareVisitDate(it.startedAt)} · ${it.visitCategory}"
            } ?: "Log completed medical, vision, dental, and other care visits.",
        accentColor = RebuildBlue
    ) {
        if (mostRecent != null) {
            RebuildInsetPanel {
                Text(
                    text = mostRecent.placeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val provider = careVisitProviderDisplay(mostRecent)
                if (provider.isNotBlank()) {
                    Text(
                        text = provider,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (mostRecent.reasonForVisit.isNotBlank()) {
                    Text(
                        text = mostRecent.reasonForVisit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        /* Stay silent during weeks with no care visits. */
        if (visitsThisWeek > 0) {
            Text(
                text = "$visitsThisWeek care ${if (visitsThisWeek == 1) "visit" else "visits"} this week",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLogVisit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Log Visit")
            }

            OutlinedButton(
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Visit History")
            }
        }

        Text(
            text = "Places and providers are remembered for fast reuse. Care visits do not affect daily completion.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CareVisitStartDialog(
    recentPlaces: List<CarePlace>,
    visits: List<CareVisit>,
    onSelectPlace: (CarePlace) -> Unit,
    onEditPlace: (CarePlace) -> Unit,
    onAddPlace: () -> Unit,
    onOneTimeVisit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
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
                            text = "Log Care Visit",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose a recent place or start a new one-time visit.",
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recentPlaces.forEachIndexed { index, place ->
                            val lastVisit = visits.firstOrNull { it.placeId == place.id }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlace(place) }
                                    .padding(vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                        lastVisit?.let {
                                            Text(
                                                text = "Last visited ${formatCareVisitDate(it.startedAt)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    TextButton(onClick = { onEditPlace(place) }) {
                                        Text("Edit")
                                    }
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
                    onClick = onOneTimeVisit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Log One-Time Visit")
                }
            }
        }
    }
}

@Composable
fun CareProviderPickerDialog(
    place: CarePlace,
    providers: List<CareProvider>,
    onSelectProvider: (CareProvider) -> Unit,
    onEditProvider: (CareProvider) -> Unit,
    onAddProvider: () -> Unit,
    onContinueWithoutProvider: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    prompt: String = "Who did you see?"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
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
                    OutlinedButton(onClick = onBack) { Text("‹ Places") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                Text(
                    text = place.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (providers.isEmpty()) {
                    RebuildInsetPanel {
                        Text(
                            text = "No providers saved for this place yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    providers.forEachIndexed { index, provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProvider(provider) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = careProviderDisplay(provider),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (provider.specialty.isNotBlank()) {
                                    Text(
                                        text = provider.specialty,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(onClick = { onEditProvider(provider) }) {
                                Text("Edit")
                            }
                        }
                        if (index < providers.lastIndex) HorizontalDivider()
                    }
                }

                Button(
                    onClick = onAddProvider,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Add Provider / Doctor")
                }

                OutlinedButton(
                    onClick = onContinueWithoutProvider,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue Without a Provider")
                }
            }
        }
    }
}
@Composable
fun CarePlaceEditorDialog(
    existingPlace: CarePlace?,
    isSaving: Boolean,
    onSave: (CarePlaceDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.name.orEmpty()) }
    var category by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.placeCategory ?: "Medical") }
    var address by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.address.orEmpty()) }
    var city by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.city.orEmpty()) }
    var state by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.state.orEmpty()) }
    var zip by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.zipCode.orEmpty()) }
    var phone by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.phone.orEmpty()) }
    var website by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.website.orEmpty()) }
    var portal by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.patientPortal.orEmpty()) }
    var notes by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.notes.orEmpty()) }
    var active by rememberSaveable(existingPlace?.id) { mutableStateOf(existingPlace?.active ?: true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    RebuildInputDialog(
        title = if (existingPlace == null) "Add care place" else "Edit care place",
        subtitle = "Save the location once so appointments and visits can reuse it.",
        onDismissRequest = { if (!isSaving) onDismiss() },
        primaryActionText = if (isSaving) "Saving…" else if (existingPlace == null) "Add place" else "Save changes",
        onPrimaryAction = {
            if (name.isBlank() || address.isBlank() || city.isBlank() || state.isBlank()) {
                error = "Enter the place name, address, city, and state."
            } else {
                onSave(
                    CarePlaceDraft(
                        id = existingPlace?.id ?: 0L,
                        name = name.trim(),
                        placeCategory = category.trim().ifBlank { "Medical" },
                        address = address.trim(),
                        city = city.trim(),
                        state = state.trim(),
                        zipCode = zip.trim(),
                        phone = phone.trim(),
                        website = website.trim(),
                        patientPortal = portal.trim(),
                        notes = notes.trim(),
                        active = active,
                        createdAt = existingPlace?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }
        },
        primaryActionEnabled = !isSaving,
        secondaryActionEnabled = !isSaving
    ) {
        Text(
            text = "Required information",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(name, { name = it; error = null }, label = { Text("Place or practice name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(category, { category = it }, label = { Text("Care category") }, placeholder = { Text("Medical, vision, dental…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it; error = null }, label = { Text("Street address *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(city, { city = it; error = null }, label = { Text("City *") }, singleLine = true, modifier = Modifier.weight(1.25f))
            OutlinedTextField(state, { state = it.uppercase().take(2); error = null }, label = { Text("State *") }, singleLine = true, modifier = Modifier.weight(0.7f))
        }
        OutlinedTextField(zip, { zip = it.filter(Char::isDigit).take(10) }, label = { Text("ZIP code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

        Text(
            text = "Contact and reference details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(website, { website = it }, label = { Text("Website") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(portal, { portal = it }, label = { Text("Patient portal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())

        RebuildInsetPanel {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { active = !active },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = active, onCheckedChange = { active = it })
                Column {
                    Text("Active place", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Inactive places stay in history but are hidden from normal pickers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
fun CareProviderEditorDialog(
    place: CarePlace,
    existingProvider: CareProvider?,
    isSaving: Boolean,
    onSave: (CareProviderDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(existingProvider?.id) { mutableStateOf(existingProvider?.name.orEmpty()) }
    var credentials by rememberSaveable(existingProvider?.id) { mutableStateOf(existingProvider?.credentials.orEmpty()) }
    var specialty by rememberSaveable(existingProvider?.id) { mutableStateOf(existingProvider?.specialty.orEmpty()) }
    var phone by rememberSaveable(existingProvider?.id) { mutableStateOf(existingProvider?.phone.orEmpty()) }
    var notes by rememberSaveable(existingProvider?.id) { mutableStateOf(existingProvider?.notes.orEmpty()) }
    var active by rememberSaveable(existingProvider?.id) { mutableStateOf(existingProvider?.active ?: true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    RebuildInputDialog(
        title = if (existingProvider == null) "Add provider" else "Edit provider",
        subtitle = place.name,
        onDismissRequest = { if (!isSaving) onDismiss() },
        primaryActionText = if (isSaving) "Saving…" else if (existingProvider == null) "Add provider" else "Save changes",
        onPrimaryAction = {
            if (name.isBlank()) {
                error = "Enter the provider name."
            } else {
                onSave(
                    CareProviderDraft(
                        id = existingProvider?.id ?: 0L,
                        placeId = place.id,
                        name = name.trim(),
                        credentials = credentials.trim(),
                        specialty = specialty.trim(),
                        phone = phone.trim(),
                        notes = notes.trim(),
                        active = active,
                        createdAt = existingProvider?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }
        },
        primaryActionEnabled = !isSaving,
        secondaryActionEnabled = !isSaving
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = { Text("Provider or doctor name *") },
            singleLine = true,
            isError = error != null && name.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(credentials, { credentials = it }, label = { Text("Credentials") }, placeholder = { Text("MD, DO, NP…") }, singleLine = true, modifier = Modifier.weight(0.8f))
            OutlinedTextField(specialty, { specialty = it }, label = { Text("Specialty") }, singleLine = true, modifier = Modifier.weight(1.2f))
        }
        OutlinedTextField(phone, { phone = it }, label = { Text("Direct phone or extension") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Provider notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())

        RebuildInsetPanel {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { active = !active },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = active, onCheckedChange = { active = it })
                Column {
                    Text("Active provider", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Inactive providers remain attached to older appointments and visits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private class CareVisitFormState(
    selectedDate: String,
    selectedTime: String,
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
    summary: String,
    tests: String,
    results: String,
    instructions: String,
    medicationChanges: String,
    referrals: String,
    followUpDate: String,
    notes: String,
    weight: String,
    systolic: String,
    diastolic: String,
    a1c: String,
    glucose: String,
    cholesterolTotal: String,
    cholesterolLdl: String,
    cholesterolHdl: String,
    triglycerides: String,
    copyMeasurements: Boolean
) {
    var selectedDate by mutableStateOf(selectedDate)
    var selectedTime by mutableStateOf(selectedTime)
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
    var summary by mutableStateOf(summary)
    var tests by mutableStateOf(tests)
    var results by mutableStateOf(results)
    var instructions by mutableStateOf(instructions)
    var medicationChanges by mutableStateOf(medicationChanges)
    var referrals by mutableStateOf(referrals)
    var followUpDate by mutableStateOf(followUpDate)
    var notes by mutableStateOf(notes)
    var weight by mutableStateOf(weight)
    var systolic by mutableStateOf(systolic)
    var diastolic by mutableStateOf(diastolic)
    var a1c by mutableStateOf(a1c)
    var glucose by mutableStateOf(glucose)
    var cholesterolTotal by mutableStateOf(cholesterolTotal)
    var cholesterolLdl by mutableStateOf(cholesterolLdl)
    var cholesterolHdl by mutableStateOf(cholesterolHdl)
    var triglycerides by mutableStateOf(triglycerides)
    var copyMeasurements by mutableStateOf(copyMeasurements)
}

private fun createCareVisitFormState(
    existingVisit: CareVisit?,
    savedPlace: CarePlace?,
    savedProvider: CareProvider?,
    appointmentPrefill: CareAppointment?
): CareVisitFormState {
    val initialTimestamp =
        existingVisit?.startedAt
            ?: appointmentPrefill?.scheduledAt
            ?: System.currentTimeMillis()

    val initialDateTime =
        Instant.ofEpochMilli(initialTimestamp)
            .atZone(ZoneId.systemDefault())

    val appointmentNotes =
        appointmentPrefill?.let { appointment ->
            buildList {
                if (appointment.questionsToAsk.isNotBlank()) {
                    add("Questions prepared: ${appointment.questionsToAsk}")
                }
                if (appointment.documentsToBring.isNotBlank()) {
                    add("Documents brought/planned: ${appointment.documentsToBring}")
                }
                if (appointment.preparationNotes.isNotBlank()) {
                    add("Preparation notes: ${appointment.preparationNotes}")
                }
            }.joinToString("\n")
        }.orEmpty()

    return CareVisitFormState(
        selectedDate =
            existingVisit?.date
                ?: appointmentPrefill?.date
                ?: initialDateTime.toLocalDate().toString(),
        selectedTime = initialDateTime.toLocalTime().withSecond(0).withNano(0).toString(),
        category =
            existingVisit?.visitCategory
                ?: appointmentPrefill?.visitCategory
                ?: defaultVisitCategory(savedPlace, savedProvider),
        format =
            existingVisit?.visitFormat
                ?: appointmentPrefill?.visitFormat
                ?: "In person",
        placeName =
            existingVisit?.placeName
                ?: appointmentPrefill?.placeName
                ?: savedPlace?.name.orEmpty(),
        placeCategory =
            existingVisit?.placeCategory
                ?: appointmentPrefill?.placeCategory
                ?: savedPlace?.placeCategory.orEmpty(),
        providerName =
            existingVisit?.providerName
                ?: appointmentPrefill?.providerName
                ?: savedProvider?.name.orEmpty(),
        providerCredentials =
            existingVisit?.providerCredentials
                ?: appointmentPrefill?.providerCredentials
                ?: savedProvider?.credentials.orEmpty(),
        providerSpecialty =
            existingVisit?.providerSpecialty
                ?: appointmentPrefill?.providerSpecialty
                ?: savedProvider?.specialty.orEmpty(),
        address =
            existingVisit?.address
                ?: appointmentPrefill?.address
                ?: savedPlace?.address.orEmpty(),
        city =
            existingVisit?.city
                ?: appointmentPrefill?.city
                ?: savedPlace?.city.orEmpty(),
        state =
            existingVisit?.state
                ?: appointmentPrefill?.state
                ?: savedPlace?.state.orEmpty(),
        zipCode =
            existingVisit?.zipCode
                ?: appointmentPrefill?.zipCode
                ?: savedPlace?.zipCode.orEmpty(),
        placePhone =
            existingVisit?.placePhone
                ?: appointmentPrefill?.placePhone
                ?: savedPlace?.phone.orEmpty(),
        providerPhone =
            existingVisit?.providerPhone
                ?: appointmentPrefill?.providerPhone
                ?: savedProvider?.phone.orEmpty(),
        reason =
            existingVisit?.reasonForVisit
                ?: appointmentPrefill?.reasonForAppointment.orEmpty(),
        summary = existingVisit?.visitSummary.orEmpty(),
        tests = existingVisit?.testsProcedures.orEmpty(),
        results = existingVisit?.resultsDiscussed.orEmpty(),
        instructions = existingVisit?.instructions.orEmpty(),
        medicationChanges = existingVisit?.medicationChanges.orEmpty(),
        referrals = existingVisit?.referrals.orEmpty(),
        followUpDate = existingVisit?.followUpDate.orEmpty(),
        notes = existingVisit?.notes ?: appointmentNotes,
        weight = existingVisit?.weightPounds?.toString().orEmpty(),
        systolic = existingVisit?.systolic?.toString().orEmpty(),
        diastolic = existingVisit?.diastolic?.toString().orEmpty(),
        a1c = existingVisit?.a1c?.toString().orEmpty(),
        glucose = existingVisit?.bloodGlucose?.toString().orEmpty(),
        cholesterolTotal = existingVisit?.cholesterolTotal?.toString().orEmpty(),
        cholesterolLdl = existingVisit?.cholesterolLdl?.toString().orEmpty(),
        cholesterolHdl = existingVisit?.cholesterolHdl?.toString().orEmpty(),
        triglycerides = existingVisit?.triglycerides?.toString().orEmpty(),
        copyMeasurements = true
    )
}

private fun CareVisitFormState.toDraft(
    existingVisit: CareVisit?,
    savedPlace: CarePlace?,
    savedProvider: CareProvider?,
    appointmentPrefill: CareAppointment?
): CareVisitDraft {
    val dateValue = LocalDate.parse(selectedDate)
    val timeValue = LocalTime.parse(selectedTime)
    val timestamp =
        dateValue.atTime(timeValue)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    return CareVisitDraft(
        id = existingVisit?.id ?: 0L,
        placeId =
            existingVisit?.placeId
                ?: appointmentPrefill?.placeId
                ?: savedPlace?.id,
        providerId =
            existingVisit?.providerId
                ?: appointmentPrefill?.providerId
                ?: savedProvider?.id,
        date = selectedDate,
        startedAt = timestamp,
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
        reasonForVisit = reason.trim(),
        visitSummary = summary.trim(),
        testsProcedures = tests.trim(),
        resultsDiscussed = results.trim(),
        instructions = instructions.trim(),
        medicationChanges = medicationChanges.trim(),
        referrals = referrals.trim(),
        followUpDate = followUpDate.trim().takeIf { it.isNotEmpty() },
        notes = notes.trim(),
        weightPounds = weight.toDoubleOrNull(),
        systolic = systolic.toIntOrNull(),
        diastolic = diastolic.toIntOrNull(),
        a1c = a1c.toDoubleOrNull(),
        bloodGlucose = glucose.toDoubleOrNull(),
        cholesterolTotal = cholesterolTotal.toDoubleOrNull(),
        cholesterolLdl = cholesterolLdl.toDoubleOrNull(),
        cholesterolHdl = cholesterolHdl.toDoubleOrNull(),
        triglycerides = triglycerides.toDoubleOrNull(),
        copyCompatibleMeasurements = copyMeasurements,
        createdAt = existingVisit?.createdAt ?: System.currentTimeMillis()
    )
}

@Composable
private fun CareVisitDateTimeFields(form: CareVisitFormState) {
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
                        form.selectedDate = LocalDate.of(year, month + 1, day).toString()
                    },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(formatCareDateOnly(form.selectedDate))
        }

        OutlinedButton(
            onClick = {
                val time = LocalTime.parse(form.selectedTime)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        form.selectedTime = LocalTime.of(hour, minute).toString()
                    },
                    time.hour,
                    time.minute,
                    false
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(formatCareTimeOnly(form.selectedTime))
        }
    }
}

@Composable
private fun CareVisitIdentityFields(
    form: CareVisitFormState,
    canEditSnapshot: Boolean
) {
    if (canEditSnapshot) {
        OutlinedTextField(
            value = form.placeName,
            onValueChange = { form.placeName = it },
            label = { Text("Place / practice *") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.placeCategory,
            onValueChange = { form.placeCategory = it },
            label = { Text("Place category") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.providerName,
            onValueChange = { form.providerName = it },
            label = { Text("Provider / doctor") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.providerCredentials,
                onValueChange = { form.providerCredentials = it },
                label = { Text("Credentials") },
                modifier = Modifier.weight(0.8f)
            )
            OutlinedTextField(
                value = form.providerSpecialty,
                onValueChange = { form.providerSpecialty = it },
                label = { Text("Specialty") },
                modifier = Modifier.weight(1.2f)
            )
        }
        OutlinedTextField(
            value = form.address,
            onValueChange = { form.address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.city,
                onValueChange = { form.city = it },
                label = { Text("City") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = form.state,
                onValueChange = { form.state = it },
                label = { Text("State") },
                modifier = Modifier.weight(0.65f)
            )
        }
        OutlinedTextField(
            value = form.zipCode,
            onValueChange = { form.zipCode = it },
            label = { Text("ZIP code") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.placePhone,
            onValueChange = { form.placePhone = it },
            label = { Text("Place phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.providerPhone,
            onValueChange = { form.providerPhone = it },
            label = { Text("Provider phone / extension") },
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
            val providerText =
                listOf(form.providerName, form.providerCredentials)
                    .filter(String::isNotBlank)
                    .joinToString(", ")
            if (providerText.isNotBlank()) {
                Text(providerText)
            }
            if (form.providerSpecialty.isNotBlank()) {
                Text(
                    text = form.providerSpecialty,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val location =
                careLocationText(
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
private fun CareVisitCategoryAndFormatFields(form: CareVisitFormState) {
    var showCategories by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showCategories = !showCategories },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Visit category: ${form.category}")
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
        text = "Visit format",
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
private fun CareVisitCoreFields(form: CareVisitFormState) {
    OutlinedTextField(
        value = form.reason,
        onValueChange = { form.reason = it },
        label = { Text("Reason for visit *") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = form.summary,
        onValueChange = { form.summary = it },
        label = { Text("Visit summary") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CareVisitMoreDetailsFields(form: CareVisitFormState) {
    val context = LocalContext.current

    OutlinedTextField(
        value = form.tests,
        onValueChange = { form.tests = it },
        label = { Text("Tests or procedures") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = form.results,
        onValueChange = { form.results = it },
        label = { Text("Results discussed") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = form.instructions,
        onValueChange = { form.instructions = it },
        label = { Text("Instructions and next steps") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = form.medicationChanges,
        onValueChange = { form.medicationChanges = it },
        label = { Text("Medication changes") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = form.referrals,
        onValueChange = { form.referrals = it },
        label = { Text("Referrals") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedButton(
        onClick = {
            val base =
                form.followUpDate
                    .takeIf(String::isNotBlank)
                    ?.let(LocalDate::parse)
                    ?: LocalDate.now()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    form.followUpDate = LocalDate.of(year, month + 1, day).toString()
                },
                base.year,
                base.monthValue - 1,
                base.dayOfMonth
            ).show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (form.followUpDate.isBlank()) {
                "Add Follow-Up Date"
            } else {
                "Follow-up: ${formatCareDateOnly(form.followUpDate)}"
            }
        )
    }
    if (form.followUpDate.isNotBlank()) {
        TextButton(onClick = { form.followUpDate = "" }) {
            Text("Remove follow-up date")
        }
    }
    OutlinedTextField(
        value = form.notes,
        onValueChange = { form.notes = it },
        label = { Text("Additional notes") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CareVisitMeasurementFields(form: CareVisitFormState) {
    OutlinedTextField(
        value = form.weight,
        onValueChange = { form.weight = numericInput(it) },
        label = { Text("Weight (lb)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = form.systolic,
            onValueChange = { form.systolic = integerInput(it) },
            label = { Text("Systolic") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = form.diastolic,
            onValueChange = { form.diastolic = integerInput(it) },
            label = { Text("Diastolic") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = form.a1c,
            onValueChange = { form.a1c = numericInput(it) },
            label = { Text("A1C %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = form.glucose,
            onValueChange = { form.glucose = numericInput(it) },
            label = { Text("Glucose") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        value = form.cholesterolTotal,
        onValueChange = { form.cholesterolTotal = numericInput(it) },
        label = { Text("Total cholesterol") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = form.cholesterolLdl,
            onValueChange = { form.cholesterolLdl = numericInput(it) },
            label = { Text("LDL") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = form.cholesterolHdl,
            onValueChange = { form.cholesterolHdl = numericInput(it) },
            label = { Text("HDL") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        value = form.triglycerides,
        onValueChange = { form.triglycerides = numericInput(it) },
        label = { Text("Triglycerides") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { form.copyMeasurements = !form.copyMeasurements },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = form.copyMeasurements,
            onCheckedChange = { form.copyMeasurements = it }
        )
        Text(
            text = "Also add compatible readings to Health Measurements",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Text(
        text = "Weight, blood pressure, A1C, and cholesterol can be copied. Glucose remains attached to this visit in the first version.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun CareVisitEditorDialog(
    savedPlace: CarePlace?,
    savedProvider: CareProvider?,
    existingVisit: CareVisit?,
    isOneTimeVisit: Boolean,
    isSaving: Boolean,
    onSave: (CareVisitDraft) -> Unit,
    onDismiss: () -> Unit,
    appointmentPrefill: CareAppointment? = null
) {
    val form =
        remember(
            existingVisit?.id,
            savedPlace?.id,
            savedProvider?.id,
            appointmentPrefill?.id,
            isOneTimeVisit
        ) {
            createCareVisitFormState(
                existingVisit = existingVisit,
                savedPlace = savedPlace,
                savedProvider = savedProvider,
                appointmentPrefill = appointmentPrefill
            )
        }
    val canEditSnapshot =
        isOneTimeVisit ||
            (existingVisit != null && existingVisit.placeId == null)

    var showMoreDetails by remember { mutableStateOf(false) }
    var showMeasurements by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding().imePadding(),
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
                            text = when {
                                existingVisit != null -> "Edit Care Visit"
                                appointmentPrefill != null -> "Complete Appointment as Visit"
                                else -> "Log Care Visit"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text =
                                if (appointmentPrefill != null) {
                                    "Appointment details are prefilled; add what happened."
                                } else {
                                    "Completed visit details"
                                },
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

                CareVisitDateTimeFields(form)
                CareVisitIdentityFields(form, canEditSnapshot)
                CareVisitCategoryAndFormatFields(form)
                CareVisitCoreFields(form)

                OutlinedButton(
                    onClick = { showMoreDetails = !showMoreDetails },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (showMoreDetails) {
                            "Hide More Details"
                        } else {
                            "Add More Details"
                        }
                    )
                }
                if (showMoreDetails) {
                    CareVisitMoreDetailsFields(form)
                }

                OutlinedButton(
                    onClick = { showMeasurements = !showMeasurements },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (showMeasurements) {
                            "Hide Visit Measurements"
                        } else {
                            "Add Visit Measurements"
                        }
                    )
                }
                if (showMeasurements) {
                    CareVisitMeasurementFields(form)
                }

                error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        if (
                            form.placeName.isBlank() ||
                            form.reason.isBlank() ||
                            form.category.isBlank()
                        ) {
                            error =
                                "Enter the place, visit category, and reason for visit."
                        } else {
                            error = null
                            onSave(
                                form.toDraft(
                                    existingVisit = existingVisit,
                                    savedPlace = savedPlace,
                                    savedProvider = savedProvider,
                                    appointmentPrefill = appointmentPrefill
                                )
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isSaving) "Saving Visit…" else "Save Visit")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CareVisitHistoryDialog(
    visits: List<CareVisit>,
    onEdit: (CareVisit) -> Unit,
    onDelete: (CareVisit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    var categoryFilter by rememberSaveable { mutableStateOf("All") }
    var showCategories by rememberSaveable { mutableStateOf(false) }

    val categories = remember(visits) {
        listOf("All") + visits.map { it.visitCategory }.distinct().sorted()
    }
    val filtered = visits.filter { visit ->
        val query = search.trim().lowercase(Locale.US)
        val matchesSearch = query.isBlank() || listOf(
            visit.placeName,
            visit.providerName,
            visit.providerSpecialty,
            visit.visitCategory,
            visit.reasonForVisit,
            visit.visitSummary
        ).any { it.lowercase(Locale.US).contains(query) }
        val matchesCategory = categoryFilter == "All" || visit.visitCategory == categoryFilter
        matchesSearch && matchesCategory
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Care Visit History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${visits.size} total ${if (visits.size == 1) "visit" else "visits"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search place, provider, specialty, or details") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showCategories = !showCategories },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Category: $categoryFilter")
                }

                if (showCategories) {
                    RebuildInsetPanel {
                        categories.forEach { category ->
                            TextButton(
                                onClick = {
                                    categoryFilter = category
                                    showCategories = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(category) }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filtered.isEmpty()) {
                        RebuildInsetPanel {
                            Text(
                                text = if (visits.isEmpty()) "No care visits have been logged yet." else "No visits match this search.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        filtered.forEach { visit ->
                            RebuildInsetPanel {
                                Text(visit.placeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${formatCareVisitDateTime(visit.startedAt)} · ${visit.visitCategory}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val provider = careVisitProviderDisplay(visit)
                                if (provider.isNotBlank()) {
                                    Text(provider, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(visit.reasonForVisit, style = MaterialTheme.typography.bodyMedium)
                                val location = careLocationText(
                                    visit.address,
                                    visit.city,
                                    visit.state,
                                    visit.zipCode
                                )
                                if (location.isNotBlank()) {
                                    Text(
                                        text = location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (visit.visitSummary.isNotBlank()) {
                                    Text(visit.visitSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                careVisitHistoryDetail("Tests / procedures", visit.testsProcedures)
                                careVisitHistoryDetail("Results", visit.resultsDiscussed)
                                careVisitHistoryDetail("Instructions", visit.instructions)
                                careVisitHistoryDetail("Medication changes", visit.medicationChanges)
                                careVisitHistoryDetail("Referrals", visit.referrals)
                                careVisitHistoryDetail("Notes", visit.notes)
                                visit.followUpDate?.let {
                                    Text(
                                        text = "Follow-up: ${formatCareDateOnly(it)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                val measurementSummary = buildList {
                                    visit.weightPounds?.let { add("Weight ${trimCareNumber(it)} lb") }
                                    if (visit.systolic != null && visit.diastolic != null) {
                                        add("BP ${visit.systolic}/${visit.diastolic}")
                                    }
                                    visit.a1c?.let { add("A1C ${trimCareNumber(it)}%") }
                                    visit.bloodGlucose?.let { add("Glucose ${trimCareNumber(it)}") }
                                    visit.cholesterolTotal?.let { add("Total cholesterol ${trimCareNumber(it)}") }
                                    visit.cholesterolLdl?.let { add("LDL ${trimCareNumber(it)}") }
                                    visit.cholesterolHdl?.let { add("HDL ${trimCareNumber(it)}") }
                                    visit.triglycerides?.let { add("Triglycerides ${trimCareNumber(it)}") }
                                }
                                if (measurementSummary.isNotEmpty()) {
                                    Text(
                                        text = measurementSummary.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (visit.address.isNotBlank() || visit.city.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                val location = careLocationText(visit.address, visit.city, visit.state, visit.zipCode)
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("geo:0,0?q=${Uri.encode(location)}")
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Directions") }
                                    }
                                    val phone = visit.providerPhone.ifBlank { visit.placePhone }
                                    if (phone.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Call") }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(onClick = { onEdit(visit) }, modifier = Modifier.weight(1f)) { Text("Edit") }
                                    OutlinedButton(onClick = { onDelete(visit) }, modifier = Modifier.weight(1f)) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun careVisitHistoryDetail(
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
fun DeleteCareVisitDialog(
    visit: CareVisit,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text("Delete care visit?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${visit.placeName} · ${formatCareVisitDateTime(visit.startedAt)}")
                Text("The saved place and provider remain available for future visits.")
                Text(
                    text = "Measurements previously copied to Health Measurements are not automatically deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isDeleting) {
                Text(if (isDeleting) "Deleting…" else "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Cancel") }
        }
    )
}

private fun defaultVisitCategory(
    place: CarePlace?,
    provider: CareProvider?
): String {
    val combined = listOf(place?.placeCategory.orEmpty(), provider?.specialty.orEmpty())
        .joinToString(" ")
        .lowercase(Locale.US)
    return when {
        "vision" in combined || "eye" in combined || "optom" in combined || "ophthal" in combined -> "Vision"
        "dental" in combined || "dent" in combined -> "Dental"
        "card" in combined -> "Cardiology"
        "diabet" in combined || "endocr" in combined -> "Diabetes / endocrinology"
        "mental" in combined || "psychi" in combined || "therap" in combined -> "Mental health"
        "lab" in combined -> "Laboratory"
        "imag" in combined || "radiol" in combined -> "Imaging"
        else -> "Primary care"
    }
}

private fun careProviderDisplay(provider: CareProvider): String =
    listOf(provider.name, provider.credentials)
        .filter(String::isNotBlank)
        .joinToString(", ")

private fun careVisitProviderDisplay(visit: CareVisit): String {
    val name = listOf(visit.providerName, visit.providerCredentials)
        .filter(String::isNotBlank)
        .joinToString(", ")
    return listOf(name, visit.providerSpecialty)
        .filter(String::isNotBlank)
        .joinToString(" · ")
}

fun careLocationText(
    address: String,
    city: String,
    state: String,
    zipCode: String
): String {
    val cityLine = listOf(city, state).filter(String::isNotBlank).joinToString(", ") +
        zipCode.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
    return listOf(address, cityLine.trim()).filter(String::isNotBlank).joinToString(" · ")
}

fun formatCareVisitDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

fun formatCareVisitDateTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy · h:mm a", Locale.US))

private fun formatCareDateOnly(date: String): String =
    runCatching {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
    }.getOrDefault(date)

private fun formatCareTimeOnly(time: String): String =
    runCatching {
        LocalTime.parse(time).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }.getOrDefault(time)

private fun trimCareNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun numericInput(value: String): String =
    value.filterIndexed { index, char -> char.isDigit() || (char == '.' && index > 0) }

private fun integerInput(value: String): String = value.filter(Char::isDigit)
