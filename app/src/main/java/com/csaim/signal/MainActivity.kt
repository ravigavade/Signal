package com.csaim.signal

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.csaim.signal.ui.theme.SignalTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    // Opens the Health Connect permission screen

    // Permission Signal currently needs
    // Declared here so both onCreate and Compose UI can access it
    //step 2
    val permissions =
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(
                HeartRateVariabilityRmssdRecord::class
            ),
            HealthPermission.getReadPermission(
                RestingHeartRateRecord::class
            )
        )

    private val requestPermissions =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions ->

            // Check if READ_STEPS was granted
            val hasAllPermissions =
                grantedPermissions.containsAll(permissions)

            Log.d(
                "SIGNAL_HEALTH",
                "Permission result = $grantedPermissions"
            )

            Log.d(
                "SIGNAL_HEALTH",
                "Has READ_STEPS = $hasAllPermissions"
            )

            if (hasAllPermissions) {

                // Permission was granted → read steps
                lifecycleScope.launch {

                    val healthConnectClient =
                        HealthConnectClient.getOrCreate(this@MainActivity)

                    loadHealthData(healthConnectClient)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()


        // Check if Health Connect is available
        //step 1
        val status =
            HealthConnectClient.getSdkStatus(this)

        Log.d(
            "SIGNAL_HEALTH",
            "SDK status = $status"
        )

        if (status == HealthConnectClient.SDK_AVAILABLE) {

            // Client used to communicate with Health Connect
            val healthConnectClient =
                HealthConnectClient.getOrCreate(this)

            lifecycleScope.launch {

                // Check permissions already granted
                val grantedPermissions =
                    healthConnectClient
                        .permissionController
                        .getGrantedPermissions()

                val hasAllPermissions =
                    grantedPermissions.containsAll(permissions)

                Log.d(
                    "SIGNAL_HEALTH",
                    "Granted permissions = $grantedPermissions"
                )

                Log.d(
                    "SIGNAL_HEALTH",
                    "Has READ_STEPS = $hasAllPermissions"
                )

                if (hasAllPermissions) {
                    loadHealthData(healthConnectClient)
                }
            }
        }

        // Compose UI
        setContent {
            SignalTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Button(
                        modifier = Modifier.padding(innerPadding),

                        onClick = {
                            requestPermissions.launch(permissions)
                        }
                    ) {
                        Text("Grant step permission")
                    }
                }
            }
        }
    }

    private suspend fun readTodaysSteps(
        healthConnectClient: HealthConnectClient
    ): Long {

        // Today's midnight
        val startOfDay =
            LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()

        // Current time
        val now =
            Instant.now()

        // DOC CODE:
        // Aggregate total steps between startOfDay and now
        val response =
            healthConnectClient.aggregate(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsRecord.COUNT_TOTAL
                        ),

                    timeRangeFilter =
                        TimeRangeFilter.between(
                            startOfDay,
                            now
                        )
                )
            )

        // Return total steps.
        // If nothing exists, return 0.
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    private suspend fun readSleepSessions(
        healthConnectClient: HealthConnectClient
    ){
        val startTime = LocalDate.now()
            .minusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val endTime =
            Instant.now()

        val response =
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

        Log.d(
            "SIGNAL_HEALTH",
            "Sleep records found = ${response.records.size}"
        )

        for (sleepRecord in response.records) {

            Log.d(
                "SIGNAL_HEALTH",
                "Sleep start = ${sleepRecord.startTime}"
            )

            Log.d(
                "SIGNAL_HEALTH",
                "Sleep end = ${sleepRecord.endTime}"
            )

            Log.d(
                "SIGNAL_HEALTH",
                "Sleep stages = ${sleepRecord.stages}"
            )
        }
    }

    private suspend fun readHrv(
        healthConnectClient: HealthConnectClient
    ) {

        val startTime =
            LocalDate.now()
                .minusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()

        val endTime =
            Instant.now()

        val response =
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter =
                        TimeRangeFilter.between(
                            startTime,
                            endTime
                        )
                )
            )

        Log.d(
            "SIGNAL_HEALTH",
            "HRV records found = ${response.records.size}"
        )

        for (hrvRecord in response.records) {

            Log.d(
                "SIGNAL_HEALTH",
                "HRV = ${hrvRecord.heartRateVariabilityMillis} ms"
            )

            Log.d(
                "SIGNAL_HEALTH",
                "HRV time = ${hrvRecord.time}"
            )
        }
    }

    private suspend fun readRestingHeartRate(
        healthConnectClient: HealthConnectClient
    ) {

        val startTime =
            LocalDate.now()
                .minusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()

        val endTime =
            Instant.now()

        val response =
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    RestingHeartRateRecord::class,
                    timeRangeFilter =
                        TimeRangeFilter.between(
                            startTime,
                            endTime
                        )
                )
            )

        Log.d(
            "SIGNAL_HEALTH",
            "Resting HR records found = ${response.records.size}"
        )

        for (record in response.records) {

            Log.d(
                "SIGNAL_HEALTH",
                "Resting HR = ${record.beatsPerMinute} bpm"
            )

            Log.d(
                "SIGNAL_HEALTH",
                "Resting HR time = ${record.time}"
            )
        }
    }

    private suspend fun loadHealthData(
        healthConnectClient: HealthConnectClient
    ) {

        val steps =
            readTodaysSteps(healthConnectClient)

        readSleepSessions(healthConnectClient)
        readHrv(healthConnectClient)
        readRestingHeartRate(healthConnectClient)

        Log.d(
            "SIGNAL_HEALTH",
            "Today's steps = $steps"
        )
    }
}