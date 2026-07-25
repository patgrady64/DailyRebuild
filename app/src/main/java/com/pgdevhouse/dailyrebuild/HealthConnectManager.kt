package com.pgdevhouse.dailyrebuild

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Current activity totals read from Health Connect.
 *
 * Daily Rebuild only reads these totals. It never writes to, edits, or deletes
 * records stored by Google Fit, a watch, the phone, or another health app.
 */
data class HealthActivityData(
    val steps: Long = 0L,
    val distanceMiles: Double = 0.0,
    val activityMinutes: Long = 0L
)

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

class HealthConnectManager(
    private val context: Context
) {

    companion object {
        const val PROVIDER_PACKAGE_NAME =
            "com.google.android.apps.healthdata"

        val permissions: Set<String> =
            setOf(
                HealthPermission.getReadPermission(
                    StepsRecord::class
                ),
                HealthPermission.getReadPermission(
                    DistanceRecord::class
                ),
                HealthPermission.getReadPermission(
                    ExerciseSessionRecord::class
                )
            )
    }

    fun getAvailability(): HealthConnectAvailability {
        return when (
            HealthConnectClient.getSdkStatus(
                context
            )
        ) {
            HealthConnectClient.SDK_AVAILABLE ->
                HealthConnectAvailability.AVAILABLE

            HealthConnectClient
                .SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.UPDATE_REQUIRED

            else ->
                HealthConnectAvailability.UNAVAILABLE
        }
    }

    private fun getClient(): HealthConnectClient {
        return HealthConnectClient.getOrCreate(
            context
        )
    }

    suspend fun hasAllPermissions(): Boolean {
        if (getAvailability() != HealthConnectAvailability.AVAILABLE) {
            return false
        }

        val grantedPermissions =
            getClient()
                .permissionController
                .getGrantedPermissions()

        return grantedPermissions.containsAll(
            permissions
        )
    }

    suspend fun readTodayActivity(): HealthActivityData {
        val zoneId = ZoneId.systemDefault()
        val startTime =
            LocalDate.now()
                .atStartOfDay(zoneId)
                .toInstant()

        val endTime = Instant.now()

        val response =
            getClient().aggregate(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsRecord.COUNT_TOTAL,
                            DistanceRecord.DISTANCE_TOTAL,
                            ExerciseSessionRecord
                                .EXERCISE_DURATION_TOTAL
                        ),
                    timeRangeFilter =
                        TimeRangeFilter.between(
                            startTime,
                            endTime
                        )
                )
            )

        val distanceMeters =
            response[
                DistanceRecord.DISTANCE_TOTAL
            ]?.inMeters ?: 0.0

        val activityMinutes =
            response[
                ExerciseSessionRecord
                    .EXERCISE_DURATION_TOTAL
            ]?.toMinutes() ?: 0L

        return HealthActivityData(
            steps =
                response[
                    StepsRecord.COUNT_TOTAL
                ] ?: 0L,
            distanceMiles =
                distanceMeters / 1609.344,
            activityMinutes =
                activityMinutes
        )
    }

    fun openHealthConnectSettings() {
        val intent =
            Intent(
                HealthConnectClient
                    .ACTION_HEALTH_CONNECT_SETTINGS
            ).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

        context.startActivity(intent)
    }

    fun openInstallOrUpdate() {
        val marketIntent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "market://details?id=$PROVIDER_PACKAGE_NAME"
                )
            ).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://play.google.com/store/apps/details?id=$PROVIDER_PACKAGE_NAME"
                    )
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
        }
    }
}
