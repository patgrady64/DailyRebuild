package com.pgdevhouse.dailyrebuild

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.ceil

private const val MOVEMENT_ID_SEPARATOR = "|"

enum class MobilityPosition(
    val label: String
) {
    SEATED("Seated"),
    BED("Lying on bed"),
    FLOOR("Lying on floor"),
    STANDING("Standing")
}

enum class MobilityCategory(
    val label: String
) {
    BACK("Back"),
    HIPS("Hips"),
    UPPER_LEGS("Upper legs"),
    LOWER_LEGS("Lower legs"),
    ANKLES_FEET("Ankles & feet"),
    SHOULDERS_NECK("Shoulders & neck"),
    CORE_TRUNK("Core & trunk")
}

data class MobilityMovement(
    val id: String,
    val name: String,
    val primaryCategory: MobilityCategory,
    val categories: Set<MobilityCategory>,
    val positions: Set<MobilityPosition>,
    val instructions: String,
    val sideNote: String? = null
)

data class MobilitySessionDraft(
    val routineName: String,
    val plannedMovementIds: List<String>,
    val completedMovementIds: List<String>,
    val skippedMovementIds: List<String>,
    val movementSeconds: Int,
    val elapsedSeconds: Int,
    val notes: String
)

object MobilityMovementLibrary {

    val movements: List<MobilityMovement> = listOf(
        MobilityMovement(
            id = "seated_cat_cow",
            name = "Seated Cat-Cow",
            primaryCategory = MobilityCategory.BACK,
            categories = setOf(
                MobilityCategory.BACK,
                MobilityCategory.CORE_TRUNK
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Sit tall. Slowly round your upper and lower back, then gently lift your chest."
        ),
        MobilityMovement(
            id = "seated_side_bend",
            name = "Seated Side Bend",
            primaryCategory = MobilityCategory.BACK,
            categories = setOf(
                MobilityCategory.BACK,
                MobilityCategory.CORE_TRUNK
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Keep both hips supported and gently lean to one side, then the other.",
            sideNote = "Split the time evenly between sides."
        ),
        MobilityMovement(
            id = "seated_trunk_rotation",
            name = "Seated Trunk Rotation",
            primaryCategory = MobilityCategory.BACK,
            categories = setOf(
                MobilityCategory.BACK,
                MobilityCategory.CORE_TRUNK
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Sit tall and slowly turn your chest toward one side without forcing the range.",
            sideNote = "Split the time evenly between sides."
        ),
        MobilityMovement(
            id = "seated_pelvic_tilts",
            name = "Seated Pelvic Tilts",
            primaryCategory = MobilityCategory.BACK,
            categories = setOf(
                MobilityCategory.BACK,
                MobilityCategory.CORE_TRUNK
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Gently tip your pelvis forward and backward while keeping the movement small."
        ),
        MobilityMovement(
            id = "supine_pelvic_tilts",
            name = "Lying Pelvic Tilts",
            primaryCategory = MobilityCategory.BACK,
            categories = setOf(
                MobilityCategory.BACK,
                MobilityCategory.CORE_TRUNK
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Lie with knees bent. Gently flatten and release your lower back without straining."
        ),
        MobilityMovement(
            id = "bent_knee_side_to_side",
            name = "Bent-Knee Side-to-Side",
            primaryCategory = MobilityCategory.BACK,
            categories = setOf(
                MobilityCategory.BACK,
                MobilityCategory.HIPS
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "With knees bent, slowly let both knees move a short distance side to side."
        ),
        MobilityMovement(
            id = "single_knee_in",
            name = "Single Knee In",
            primaryCategory = MobilityCategory.BACK,
            categories = setOf(
                MobilityCategory.BACK,
                MobilityCategory.HIPS
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Bring one bent knee toward you only as far as comfortable, then change sides.",
            sideNote = "Split the time evenly between sides."
        ),
        MobilityMovement(
            id = "heel_slides",
            name = "Heel Slides",
            primaryCategory = MobilityCategory.UPPER_LEGS,
            categories = setOf(
                MobilityCategory.UPPER_LEGS,
                MobilityCategory.HIPS
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Slide one heel toward you and back out while the leg remains supported.",
            sideNote = "Alternate legs slowly."
        ),
        MobilityMovement(
            id = "supine_marching",
            name = "Lying Marches",
            primaryCategory = MobilityCategory.HIPS,
            categories = setOf(
                MobilityCategory.HIPS,
                MobilityCategory.CORE_TRUNK
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "With knees bent, lift one foot a small amount, lower it, and alternate."
        ),
        MobilityMovement(
            id = "side_lying_knee_open",
            name = "Side-Lying Knee Openings",
            primaryCategory = MobilityCategory.HIPS,
            categories = setOf(
                MobilityCategory.HIPS,
                MobilityCategory.UPPER_LEGS
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Lie on your side with knees bent. Keep feet together and gently open the top knee.",
            sideNote = "Change sides halfway through."
        ),
        MobilityMovement(
            id = "seated_hip_march",
            name = "Seated Hip Marches",
            primaryCategory = MobilityCategory.HIPS,
            categories = setOf(
                MobilityCategory.HIPS,
                MobilityCategory.UPPER_LEGS
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Lift one knee a comfortable amount, lower it, and alternate without leaning back."
        ),
        MobilityMovement(
            id = "seated_figure_four",
            name = "Seated Figure-Four Position",
            primaryCategory = MobilityCategory.HIPS,
            categories = setOf(
                MobilityCategory.HIPS,
                MobilityCategory.UPPER_LEGS
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Rest one ankle across the opposite lower thigh only if comfortable and sit tall.",
            sideNote = "Do not press the knee. Change sides halfway through."
        ),
        MobilityMovement(
            id = "seated_knee_extensions",
            name = "Seated Knee Extensions",
            primaryCategory = MobilityCategory.UPPER_LEGS,
            categories = setOf(
                MobilityCategory.UPPER_LEGS,
                MobilityCategory.LOWER_LEGS
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Slowly straighten one knee, lower the foot, and alternate legs."
        ),
        MobilityMovement(
            id = "seated_hamstring_reach",
            name = "Seated Hamstring Reach",
            primaryCategory = MobilityCategory.UPPER_LEGS,
            categories = setOf(MobilityCategory.UPPER_LEGS),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Extend one leg with the heel supported and gently hinge forward with a long back.",
            sideNote = "Change sides halfway through."
        ),
        MobilityMovement(
            id = "lying_hamstring_towel",
            name = "Lying Hamstring Movement",
            primaryCategory = MobilityCategory.UPPER_LEGS,
            categories = setOf(
                MobilityCategory.UPPER_LEGS,
                MobilityCategory.HIPS
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Support one thigh with your hands or a towel and slowly bend and straighten the knee.",
            sideNote = "Change sides halfway through."
        ),
        MobilityMovement(
            id = "seated_adductor_squeeze",
            name = "Gentle Knee Squeeze",
            primaryCategory = MobilityCategory.UPPER_LEGS,
            categories = setOf(
                MobilityCategory.UPPER_LEGS,
                MobilityCategory.HIPS
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Place a folded pillow between your knees, gently squeeze, then fully relax."
        ),
        MobilityMovement(
            id = "seated_heel_raises",
            name = "Seated Heel Raises",
            primaryCategory = MobilityCategory.LOWER_LEGS,
            categories = setOf(
                MobilityCategory.LOWER_LEGS,
                MobilityCategory.ANKLES_FEET
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Keep toes down, slowly raise both heels, then lower them."
        ),
        MobilityMovement(
            id = "seated_toe_raises",
            name = "Seated Toe Raises",
            primaryCategory = MobilityCategory.LOWER_LEGS,
            categories = setOf(
                MobilityCategory.LOWER_LEGS,
                MobilityCategory.ANKLES_FEET
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Keep heels down, lift the front of both feet, then lower slowly."
        ),
        MobilityMovement(
            id = "seated_calf_towel",
            name = "Seated Calf Towel Stretch",
            primaryCategory = MobilityCategory.LOWER_LEGS,
            categories = setOf(
                MobilityCategory.LOWER_LEGS,
                MobilityCategory.ANKLES_FEET
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Loop a towel around the front of one foot and gently draw the toes toward you.",
            sideNote = "Change sides halfway through."
        ),
        MobilityMovement(
            id = "lying_ankle_pumps",
            name = "Lying Ankle Pumps",
            primaryCategory = MobilityCategory.LOWER_LEGS,
            categories = setOf(
                MobilityCategory.LOWER_LEGS,
                MobilityCategory.ANKLES_FEET
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Point your toes away, then draw them back toward you in a slow rhythm."
        ),
        MobilityMovement(
            id = "seated_ankle_pumps",
            name = "Seated Ankle Pumps",
            primaryCategory = MobilityCategory.ANKLES_FEET,
            categories = setOf(
                MobilityCategory.ANKLES_FEET,
                MobilityCategory.LOWER_LEGS
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Lift one foot slightly and alternate pointing and flexing the ankle."
        ),
        MobilityMovement(
            id = "seated_ankle_circles",
            name = "Seated Ankle Circles",
            primaryCategory = MobilityCategory.ANKLES_FEET,
            categories = setOf(MobilityCategory.ANKLES_FEET),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Lift one foot slightly and make slow circles in both directions.",
            sideNote = "Change feet halfway through."
        ),
        MobilityMovement(
            id = "lying_ankle_circles",
            name = "Lying Ankle Circles",
            primaryCategory = MobilityCategory.ANKLES_FEET,
            categories = setOf(MobilityCategory.ANKLES_FEET),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Keep the leg supported and make slow circles with one ankle.",
            sideNote = "Change feet halfway through."
        ),
        MobilityMovement(
            id = "foot_alphabet",
            name = "Foot Alphabet",
            primaryCategory = MobilityCategory.ANKLES_FEET,
            categories = setOf(
                MobilityCategory.ANKLES_FEET,
                MobilityCategory.LOWER_LEGS
            ),
            positions = setOf(
                MobilityPosition.SEATED,
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Use one foot to trace small alphabet letters in the air.",
            sideNote = "Change feet halfway through."
        ),
        MobilityMovement(
            id = "shoulder_rolls",
            name = "Shoulder Rolls",
            primaryCategory = MobilityCategory.SHOULDERS_NECK,
            categories = setOf(MobilityCategory.SHOULDERS_NECK),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Roll your shoulders slowly backward, then change direction."
        ),
        MobilityMovement(
            id = "gentle_neck_turns",
            name = "Gentle Neck Turns",
            primaryCategory = MobilityCategory.SHOULDERS_NECK,
            categories = setOf(MobilityCategory.SHOULDERS_NECK),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Keep your chin level and slowly turn your head left and right."
        ),
        MobilityMovement(
            id = "chin_tucks",
            name = "Chin Tucks",
            primaryCategory = MobilityCategory.SHOULDERS_NECK,
            categories = setOf(MobilityCategory.SHOULDERS_NECK),
            positions = setOf(
                MobilityPosition.SEATED,
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Gently draw your chin straight backward, pause briefly, then release."
        ),
        MobilityMovement(
            id = "seated_chest_open",
            name = "Seated Chest Opening",
            primaryCategory = MobilityCategory.SHOULDERS_NECK,
            categories = setOf(
                MobilityCategory.SHOULDERS_NECK,
                MobilityCategory.CORE_TRUNK
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Let your arms rest slightly behind you and gently lift your chest without arching hard."
        ),
        MobilityMovement(
            id = "seated_reach_forward",
            name = "Seated Forward Reach",
            primaryCategory = MobilityCategory.CORE_TRUNK,
            categories = setOf(
                MobilityCategory.CORE_TRUNK,
                MobilityCategory.BACK,
                MobilityCategory.SHOULDERS_NECK
            ),
            positions = setOf(MobilityPosition.SEATED),
            instructions =
                "Reach both hands forward while gently allowing your upper back to widen."
        ),
        MobilityMovement(
            id = "lying_arm_reaches",
            name = "Lying Arm Reaches",
            primaryCategory = MobilityCategory.CORE_TRUNK,
            categories = setOf(
                MobilityCategory.CORE_TRUNK,
                MobilityCategory.SHOULDERS_NECK
            ),
            positions = setOf(
                MobilityPosition.BED,
                MobilityPosition.FLOOR
            ),
            instructions =
                "Reach one arm toward the ceiling, lower it, and alternate while keeping your body relaxed."
        ),
        MobilityMovement(
            id = "supported_calf_stretch",
            name = "Supported Standing Calf Stretch",
            primaryCategory = MobilityCategory.LOWER_LEGS,
            categories = setOf(
                MobilityCategory.LOWER_LEGS,
                MobilityCategory.ANKLES_FEET
            ),
            positions = setOf(MobilityPosition.STANDING),
            instructions =
                "Hold a stable surface, step one foot back, and keep the stretch gentle.",
            sideNote = "Change sides halfway through."
        ),
        MobilityMovement(
            id = "supported_weight_shift",
            name = "Supported Weight Shifts",
            primaryCategory = MobilityCategory.HIPS,
            categories = setOf(
                MobilityCategory.HIPS,
                MobilityCategory.UPPER_LEGS
            ),
            positions = setOf(MobilityPosition.STANDING),
            instructions =
                "Hold a stable surface and slowly shift your weight from one foot to the other."
        ),
        MobilityMovement(
            id = "supported_march",
            name = "Supported Standing March",
            primaryCategory = MobilityCategory.UPPER_LEGS,
            categories = setOf(
                MobilityCategory.UPPER_LEGS,
                MobilityCategory.HIPS
            ),
            positions = setOf(MobilityPosition.STANDING),
            instructions =
                "Hold a stable surface and lift one foot a small amount, then alternate."
        )
    )

    fun movementById(
        id: String
    ): MobilityMovement? =
        movements.firstOrNull {
            it.id == id
        }
}

fun encodeMovementIds(
    ids: List<String>
): String =
    ids.joinToString(MOVEMENT_ID_SEPARATOR)

fun decodeMovementIds(
    encoded: String
): List<String> =
    encoded
        .split(MOVEMENT_ID_SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotBlank() }

private fun generateBalancedRoutine(
    movementCount: Int,
    allowedPositions: Set<MobilityPosition>
): List<MobilityMovement> {
    val available =
        MobilityMovementLibrary.movements.filter { movement ->
            movement.positions.any {
                it in allowedPositions
            }
        }

    if (available.isEmpty()) {
        return emptyList()
    }

    val requiredCategories =
        listOf(
            MobilityCategory.BACK,
            MobilityCategory.HIPS,
            MobilityCategory.UPPER_LEGS,
            MobilityCategory.LOWER_LEGS,
            MobilityCategory.ANKLES_FEET,
            MobilityCategory.SHOULDERS_NECK,
            MobilityCategory.CORE_TRUNK
        )

    val chosen = mutableListOf<MobilityMovement>()
    val usedIds = mutableSetOf<String>()

    requiredCategories
        .take(movementCount.coerceAtMost(requiredCategories.size))
        .forEach { category ->
            val candidate =
                available
                    .filter {
                        category in it.categories &&
                            it.id !in usedIds
                    }
                    .shuffled()
                    .firstOrNull()

            if (candidate != null) {
                chosen += candidate
                usedIds += candidate.id
            }
        }

    while (
        chosen.size < movementCount &&
        usedIds.size < available.size
    ) {
        val categoryCounts =
            MobilityCategory.values().associateWith { category ->
                chosen.count {
                    category in it.categories
                }
            }

        val leastUsedCategories =
            categoryCounts
                .entries
                .sortedBy { it.value }
                .map { it.key }

        val next =
            leastUsedCategories
                .firstNotNullOfOrNull { category ->
                    available
                        .filter {
                            category in it.categories &&
                                it.id !in usedIds
                        }
                        .shuffled()
                        .firstOrNull()
                }
                ?: available
                    .filter { it.id !in usedIds }
                    .shuffled()
                    .firstOrNull()

        if (next == null) {
            break
        }

        chosen += next
        usedIds += next.id
    }

    return chosen.shuffled()
}

private fun replacementForMovement(
    current: MobilityMovement,
    routine: List<MobilityMovement>,
    allowedPositions: Set<MobilityPosition>
): MobilityMovement? {
    val usedIds = routine.map { it.id }.toSet()

    return MobilityMovementLibrary.movements
        .filter {
            current.primaryCategory in it.categories &&
                it.positions.any { position ->
                    position in allowedPositions
                } &&
                it.id !in usedIds
        }
        .shuffled()
        .firstOrNull()
        ?: MobilityMovementLibrary.movements
            .filter {
                it.positions.any { position ->
                    position in allowedPositions
                } &&
                    it.id !in usedIds
            }
            .shuffled()
            .firstOrNull()
}

private data class MobilityPreferenceDefaults(
    val seated: Boolean = true,
    val bed: Boolean = true,
    val floor: Boolean = false,
    val standing: Boolean = false
)

@Composable
fun MobilitySection(
    sessions: List<MobilitySession>,
    isSaving: Boolean,
    onSaveSession: (MobilitySessionDraft) -> Unit,
    onDeleteSession: (MobilitySession) -> Unit
) {
    val context = LocalContext.current
    val database = remember {
        com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
            .getDatabase(context)
    }
    val healthProfileDao = remember {
        database.healthProfileDao()
    }
    var mobilityDefaults by remember {
        mutableStateOf(MobilityPreferenceDefaults())
    }

    var showRoutineDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showQuickLog by rememberSaveable {
        mutableStateOf(false)
    }

    var sessionPendingDeletion by remember {
        mutableStateOf<MobilitySession?>(null)
    }

    LaunchedEffect(showRoutineDialog) {
        if (showRoutineDialog) {
            val profile = healthProfileDao.getProfile()
            mobilityDefaults = MobilityPreferenceDefaults(
                seated = profile?.mobilitySeatedDefault ?: true,
                bed = profile?.mobilityBedDefault ?: true,
                floor = profile?.mobilityFloorDefault ?: false,
                standing = profile?.mobilityStandingDefault ?: false
            )
        }
    }

    val totalSeconds = sessions.sumOf { it.elapsedSeconds }

    RebuildSectionCard(
        title = "Mobility & stretching",
        subtitle =
            "Generate a balanced small-space routine with back, hip, and leg coverage.",
        accentColor = RebuildTeal
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "sessions",
                value = sessions.size.toString(),
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme.primaryContainer
            )

            RebuildMetricPill(
                label = "mobility time",
                value = formatMobilityDuration(totalSeconds),
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme.secondaryContainer,
                contentColor =
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Button(
            onClick = {
                showRoutineDialog = true
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Generate random routine")
        }

        OutlinedButton(
            onClick = {
                showQuickLog = true
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Quick log independent stretching")
        }

        if (sessions.isEmpty()) {
            RebuildInsetPanel {
                Text(
                    text = "No mobility session logged today.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text =
                        "Standing movements remain off unless you deliberately enable them.",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Today's sessions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            sessions.forEach { session ->
                MobilitySessionCard(
                    session = session,
                    onDelete = {
                        sessionPendingDeletion = session
                    }
                )
            }
        }
    }

    if (showRoutineDialog) {
        RandomMobilityRoutineDialog(
            isSaving = isSaving,
            defaultAllowSeated = mobilityDefaults.seated,
            defaultAllowBed = mobilityDefaults.bed,
            defaultAllowFloor = mobilityDefaults.floor,
            defaultAllowStanding = mobilityDefaults.standing,
            onDismiss = {
                if (!isSaving) {
                    showRoutineDialog = false
                }
            },
            onSaveSession = { draft ->
                onSaveSession(draft)
                showRoutineDialog = false
            }
        )
    }

    if (showQuickLog) {
        QuickMobilityLogDialog(
            isSaving = isSaving,
            onDismiss = {
                if (!isSaving) {
                    showQuickLog = false
                }
            },
            onSave = { draft ->
                onSaveSession(draft)
                showQuickLog = false
            }
        )
    }

    sessionPendingDeletion?.let { session ->
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    sessionPendingDeletion = null
                }
            },
            title = {
                Text("Delete Mobility Session?")
            },
            text = {
                Text(
                    "Delete ${session.routineName} from today?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(session)
                        sessionPendingDeletion = null
                    },
                    enabled = !isSaving
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sessionPendingDeletion = null
                    },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MobilitySessionCard(
    session: MobilitySession,
    onDelete: () -> Unit
) {
    val completedCount =
        decodeMovementIds(
            session.completedMovementIds
        ).size

    val plannedCount =
        decodeMovementIds(
            session.plannedMovementIds
        ).size

    RebuildInsetPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.routineName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = buildString {
                        append(
                            formatMobilityDuration(
                                session.elapsedSeconds
                            )
                        )

                        if (plannedCount > 0) {
                            append(" · ")
                            append(completedCount)
                            append(" of ")
                            append(plannedCount)
                            append(" completed")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (session.notes.isNotBlank()) {
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            TextButton(
                onClick = onDelete
            ) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun RandomMobilityRoutineDialog(
    isSaving: Boolean,
    defaultAllowSeated: Boolean,
    defaultAllowBed: Boolean,
    defaultAllowFloor: Boolean,
    defaultAllowStanding: Boolean,
    onDismiss: () -> Unit,
    onSaveSession: (MobilitySessionDraft) -> Unit
) {
    var screen by rememberSaveable {
        mutableStateOf("setup")
    }

    var movementCount by rememberSaveable {
        mutableIntStateOf(6)
    }

    var movementSeconds by rememberSaveable {
        mutableIntStateOf(30)
    }

    var allowSeated by rememberSaveable {
        mutableStateOf(defaultAllowSeated)
    }

    var allowBed by rememberSaveable {
        mutableStateOf(defaultAllowBed)
    }

    var allowFloor by rememberSaveable {
        mutableStateOf(defaultAllowFloor)
    }

    var allowStanding by rememberSaveable {
        mutableStateOf(defaultAllowStanding)
    }

    var generatedRoutine by remember {
        mutableStateOf<List<MobilityMovement>>(
            emptyList()
        )
    }

    var currentIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var secondsRemaining by rememberSaveable {
        mutableIntStateOf(movementSeconds)
    }

    var elapsedSeconds by rememberSaveable {
        mutableIntStateOf(0)
    }

    var hasStartedCurrentMovement by rememberSaveable {
        mutableStateOf(false)
    }

    var isPaused by rememberSaveable {
        mutableStateOf(false)
    }

    var notes by rememberSaveable {
        mutableStateOf("")
    }

    val completedIds = remember {
        mutableStateListOf<String>()
    }

    val skippedIds = remember {
        mutableStateListOf<String>()
    }

    val allowedPositions = buildSet {
        if (allowSeated) add(MobilityPosition.SEATED)
        if (allowBed) add(MobilityPosition.BED)
        if (allowFloor) add(MobilityPosition.FLOOR)
        if (allowStanding) add(MobilityPosition.STANDING)
    }

    LaunchedEffect(
        screen,
        currentIndex,
        secondsRemaining,
        hasStartedCurrentMovement,
        isPaused
    ) {
        if (
            screen == "guided" &&
            hasStartedCurrentMovement &&
            !isPaused &&
            generatedRoutine.isNotEmpty()
        ) {
            if (secondsRemaining > 0) {
                delay(1_000L)
                secondsRemaining--
                elapsedSeconds++
            } else {
                val movement =
                    generatedRoutine[currentIndex]

                if (
                    movement.id !in completedIds &&
                    movement.id !in skippedIds
                ) {
                    completedIds += movement.id
                }

                if (
                    currentIndex >=
                    generatedRoutine.lastIndex
                ) {
                    screen = "finish"
                } else {
                    currentIndex++
                    secondsRemaining = movementSeconds
                    hasStartedCurrentMovement = false
                    isPaused = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (screen) {
                "setup" -> {
                    MobilitySetupPage(
                        movementCount = movementCount,
                        onMovementCountChange = {
                            movementCount = it
                        },
                        movementSeconds = movementSeconds,
                        onMovementSecondsChange = {
                            movementSeconds = it
                        },
                        allowSeated = allowSeated,
                        onAllowSeatedChange = {
                            allowSeated = it
                        },
                        allowBed = allowBed,
                        onAllowBedChange = {
                            allowBed = it
                        },
                        allowFloor = allowFloor,
                        onAllowFloorChange = {
                            allowFloor = it
                        },
                        allowStanding = allowStanding,
                        onAllowStandingChange = {
                            allowStanding = it
                        },
                        canGenerate =
                            allowedPositions.isNotEmpty(),
                        onGenerate = {
                            generatedRoutine =
                                generateBalancedRoutine(
                                    movementCount =
                                        movementCount,
                                    allowedPositions =
                                        allowedPositions
                                )
                            screen = "preview"
                        },
                        onDismiss = onDismiss
                    )
                }

                "preview" -> {
                    MobilityPreviewPage(
                        routine = generatedRoutine,
                        movementSeconds =
                            movementSeconds,
                        onBack = {
                            screen = "setup"
                        },
                        onGenerateAgain = {
                            generatedRoutine =
                                generateBalancedRoutine(
                                    movementCount =
                                        movementCount,
                                    allowedPositions =
                                        allowedPositions
                                )
                        },
                        onReplace = { index ->
                            val replacement =
                                replacementForMovement(
                                    current =
                                        generatedRoutine[index],
                                    routine =
                                        generatedRoutine,
                                    allowedPositions =
                                        allowedPositions
                                )

                            if (replacement != null) {
                                generatedRoutine =
                                    generatedRoutine
                                        .toMutableList()
                                        .also {
                                            it[index] =
                                                replacement
                                        }
                            }
                        },
                        onStart = {
                            currentIndex = 0
                            secondsRemaining =
                                movementSeconds
                            elapsedSeconds = 0
                            completedIds.clear()
                            skippedIds.clear()
                            notes = ""
                            hasStartedCurrentMovement = false
                            isPaused = false
                            screen = "guided"
                        }
                    )
                }

                "guided" -> {
                    MobilityGuidedPage(
                        routine = generatedRoutine,
                        currentIndex = currentIndex,
                        secondsRemaining =
                            secondsRemaining,
                        movementSeconds =
                            movementSeconds,
                        hasStartedCurrentMovement =
                            hasStartedCurrentMovement,
                        isPaused = isPaused,
                        onStartPauseToggle = {
                            if (!hasStartedCurrentMovement) {
                                hasStartedCurrentMovement = true
                                isPaused = false
                            } else {
                                isPaused = !isPaused
                            }
                        },
                        onPrevious = {
                            if (currentIndex > 0) {
                                currentIndex--
                                val previousId =
                                    generatedRoutine[
                                        currentIndex
                                    ].id
                                completedIds.remove(
                                    previousId
                                )
                                skippedIds.remove(
                                    previousId
                                )
                                secondsRemaining =
                                    movementSeconds
                                hasStartedCurrentMovement = false
                                isPaused = false
                            }
                        },
                        onSkip = {
                            val movement =
                                generatedRoutine[
                                    currentIndex
                                ]

                            completedIds.remove(
                                movement.id
                            )

                            if (
                                movement.id !in
                                skippedIds
                            ) {
                                skippedIds += movement.id
                            }

                            if (
                                currentIndex >=
                                generatedRoutine.lastIndex
                            ) {
                                screen = "finish"
                            } else {
                                currentIndex++
                                secondsRemaining =
                                    movementSeconds
                                hasStartedCurrentMovement = false
                                isPaused = false
                            }
                        },
                        onStop = {
                            screen = "finish"
                        }
                    )
                }

                else -> {
                    MobilityFinishPage(
                        completedCount =
                            completedIds.size,
                        skippedCount =
                            skippedIds.size,
                        plannedCount =
                            generatedRoutine.size,
                        elapsedSeconds =
                            elapsedSeconds,
                        notes = notes,
                        onNotesChange = {
                            notes = it
                        },
                        isSaving = isSaving,
                        onSave = {
                            onSaveSession(
                                MobilitySessionDraft(
                                    routineName =
                                        "Random Mobility",
                                    plannedMovementIds =
                                        generatedRoutine
                                            .map { it.id },
                                    completedMovementIds =
                                        completedIds.toList(),
                                    skippedMovementIds =
                                        skippedIds.toList(),
                                    movementSeconds =
                                        movementSeconds,
                                    elapsedSeconds =
                                        elapsedSeconds,
                                    notes = notes.trim()
                                )
                            )
                        },
                        onDiscard = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun MobilitySetupPage(
    movementCount: Int,
    onMovementCountChange: (Int) -> Unit,
    movementSeconds: Int,
    onMovementSecondsChange: (Int) -> Unit,
    allowSeated: Boolean,
    onAllowSeatedChange: (Boolean) -> Unit,
    allowBed: Boolean,
    onAllowBedChange: (Boolean) -> Unit,
    allowFloor: Boolean,
    onAllowFloorChange: (Boolean) -> Unit,
    allowStanding: Boolean,
    onAllowStandingChange: (Boolean) -> Unit,
    canGenerate: Boolean,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                RebuildStatusBadge(
                    text = "Balanced · Small space"
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Random mobility routine",
                    style =
                        MaterialTheme.typography.headlineMedium
                )
                Text(
                    text =
                        "The generator guarantees variety instead of choosing several movements from one body area.",
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }

        RebuildSectionCard(
            title = "Routine length",
            subtitle =
                "Every routine prioritizes back, hips, and legs."
        ) {
            OptionButtonRow(
                options = listOf(4, 6, 8, 10),
                selected = movementCount,
                label = { "$it" },
                onSelected =
                    onMovementCountChange
            )
        }

        RebuildSectionCard(
            title = "Time per movement",
            subtitle =
                "You can pause or skip at any time."
        ) {
            OptionButtonRow(
                options = listOf(20, 30, 45, 60),
                selected = movementSeconds,
                label = { "${it}s" },
                onSelected =
                    onMovementSecondsChange
            )
        }

        RebuildSectionCard(
            title = "Allowed positions",
            subtitle =
                "Standing is optional and starts turned off."
        ) {
            PositionToggle(
                label = "Seated",
                supportingText =
                    "Chair or edge of bed",
                checked = allowSeated,
                onCheckedChange =
                    onAllowSeatedChange
            )
            PositionToggle(
                label = "Lying on bed",
                supportingText =
                    "Movements suitable for a mattress",
                checked = allowBed,
                onCheckedChange =
                    onAllowBedChange
            )
            PositionToggle(
                label = "Lying on floor",
                supportingText =
                    "Only include when floor space is available",
                checked = allowFloor,
                onCheckedChange =
                    onAllowFloorChange
            )
            PositionToggle(
                label = "Standing",
                supportingText =
                    "Only supported, small-space movements",
                checked = allowStanding,
                onCheckedChange =
                    onAllowStandingChange
            )
        }

        RebuildInsetPanel(
            color =
                MaterialTheme.colorScheme.tertiaryContainer
                    .copy(alpha = 0.5f)
        ) {
            Text(
                text = "Comfort-range rule",
                style =
                    MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text =
                    "Move gently. Stop a movement for sharp pain, new numbness, weakness, dizziness, or symptoms that keep increasing.",
                style =
                    MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onGenerate,
            enabled = canGenerate,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Generate routine")
        }

        if (!canGenerate) {
            Text(
                text =
                    "Select at least one allowed position.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MobilityPreviewPage(
    routine: List<MobilityMovement>,
    movementSeconds: Int,
    onBack: () -> Unit,
    onGenerateAgain: () -> Unit,
    onReplace: (Int) -> Unit,
    onStart: () -> Unit
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
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("‹ Setup")
            }

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = onGenerateAgain
            ) {
                Text("Generate again")
            }
        }

        Text(
            text = "Today's random mobility",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text =
                "${routine.size} movements · About ${formatMobilityDuration(routine.size * movementSeconds)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        routine.forEachIndexed { index, movement ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.42f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(13.dp),
                        color =
                            MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.size(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = movement.name,
                            style =
                                MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text =
                                "${movement.primaryCategory.label} · ${movement.positions.joinToString { it.label }}",
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = {
                            onReplace(index)
                        }
                    ) {
                        Text("Replace")
                    }
                }
            }
        }

        Button(
            onClick = onStart,
            enabled = routine.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Start routine")
        }
    }
}

@Composable
private fun MobilityGuidedPage(
    routine: List<MobilityMovement>,
    currentIndex: Int,
    secondsRemaining: Int,
    movementSeconds: Int,
    hasStartedCurrentMovement: Boolean,
    isPaused: Boolean,
    onStartPauseToggle: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    val movement = routine[currentIndex]
    val progress =
        if (movementSeconds <= 0) {
            0f
        } else {
            secondsRemaining
                .toFloat()
                .div(movementSeconds.toFloat())
                .coerceIn(0f, 1f)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text =
                    "Movement ${currentIndex + 1} of ${routine.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = onStop
            ) {
                Text("Stop session")
            }
        }

        RebuildStatusBadge(
            text =
                "${movement.primaryCategory.label} · ${movement.positions.joinToString { it.label }}"
        )

        Text(
            text = movement.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        RebuildProgressRing(
            progress = progress,
            centerText = secondsRemaining.toString(),
            size = 164.dp
        )

        Text(
            text = when {
                !hasStartedCurrentMovement ->
                    "Ready — timer has not started"
                isPaused ->
                    "Paused"
                else ->
                    "Seconds remaining"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RebuildInsetPanel {
            Text(
                text = movement.instructions,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            movement.sideNote?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text =
                "Read the instructions and get into position. " +
                    "The timer begins only when you tap Start. " +
                    "Use a comfortable range; skipping is always allowed.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onStartPauseToggle,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                when {
                    !hasStartedCurrentMovement -> "Start"
                    isPaused -> "Resume"
                    else -> "Pause"
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Previous")
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Skip")
            }
        }
    }
}

@Composable
private fun MobilityFinishPage(
    completedCount: Int,
    skippedCount: Int,
    plannedCount: Int,
    elapsedSeconds: Int,
    notes: String,
    onNotesChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RebuildStatusBadge(text = "Session complete")

        Text(
            text = "Mobility logged",
            style = MaterialTheme.typography.headlineMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "completed",
                value = "$completedCount / $plannedCount",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primaryContainer
            )
            RebuildMetricPill(
                label = "skipped",
                value = skippedCount.toString(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor =
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
            RebuildMetricPill(
                label = "time",
                value = formatMobilityDuration(elapsedSeconds),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor =
                    MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = {
                Text("Optional notes")
            },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSave,
            enabled = !isSaving && elapsedSeconds > 0,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (isSaving) {
                    "Saving…"
                } else {
                    "Save mobility session"
                }
            )
        }

        TextButton(
            onClick = onDiscard,
            enabled = !isSaving
        ) {
            Text("Discard session")
        }
    }
}

@Composable
private fun QuickMobilityLogDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (MobilitySessionDraft) -> Unit
) {
    var minutesText by rememberSaveable {
        mutableStateOf("")
    }

    var notes by rememberSaveable {
        mutableStateOf("")
    }

    val minutes = minutesText.toIntOrNull()

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        title = {
            Text("Quick Log Mobility")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Use this when you stretched or completed mobility without the guided timer."
                )

                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { value ->
                        minutesText =
                            value
                                .filter { it.isDigit() }
                                .take(3)
                    },
                    label = {
                        Text("Minutes")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                    },
                    label = {
                        Text("What did you do? (optional)")
                    },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        MobilitySessionDraft(
                            routineName =
                                "Independent Mobility",
                            plannedMovementIds =
                                emptyList(),
                            completedMovementIds =
                                emptyList(),
                            skippedMovementIds =
                                emptyList(),
                            movementSeconds = 0,
                            elapsedSeconds =
                                (minutes ?: 0) * 60,
                            notes = notes.trim()
                        )
                    )
                },
                enabled =
                    !isSaving &&
                        minutes != null &&
                        minutes > 0
            ) {
                Text(
                    if (isSaving) {
                        "Saving…"
                    } else {
                        "Save"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PositionToggle(
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCheckedChange(!checked)
            },
        shape = RoundedCornerShape(16.dp),
        color =
            MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    text = label,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun <T> OptionButtonRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            if (option == selected) {
                Button(
                    onClick = {
                        onSelected(option)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(label(option))
                }
            } else {
                OutlinedButton(
                    onClick = {
                        onSelected(option)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(label(option))
                }
            }
        }
    }
}

fun formatMobilityDuration(
    totalSeconds: Int
): String {
    if (totalSeconds <= 0) {
        return "0m"
    }

    val totalMinutes =
        ceil(totalSeconds / 60.0)
            .toInt()

    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours == 0 -> "${minutes}m"
        minutes == 0 -> "${hours}h"
        else -> String.format(
            Locale.US,
            "%dh %dm",
            hours,
            minutes
        )
    }
}
