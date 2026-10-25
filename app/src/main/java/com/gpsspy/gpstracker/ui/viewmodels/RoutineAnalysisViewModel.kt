package com.gpsspy.gpstracker.ui.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gpsspy.gpstracker.data.database.routine.RoutineAnalysisDao
import com.gpsspy.gpstracker.data.database.routine.RoutineState
import com.gpsspy.gpstracker.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class RoutineSummary(
    val stateDurations: Map<RoutineState, Long> // state to duration in milliseconds
)

@HiltViewModel
class RoutineAnalysisViewModel @Inject constructor(
    private val routineAnalysisDao: RoutineAnalysisDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isSetupComplete: StateFlow<Boolean> = combine(
        settingsRepository.homeLocationFlow,
        settingsRepository.workLocationFlow
    ) { home, work ->
        home.first != null && work.first != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val routineSummary: StateFlow<RoutineSummary?> = _currentDate
        .flatMapLatest { date ->
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            routineAnalysisDao.getAnalysesForDay(startOfDay, endOfDay)
                .map { routines ->
                    val durations = mutableMapOf<RoutineState, Long>()
                    for (routine in routines) {
                        val effectiveStart = maxOf(routine.startTime, startOfDay)
                        val effectiveEnd = minOf(routine.endTime ?: System.currentTimeMillis(), endOfDay)

                        if (effectiveEnd > effectiveStart) {
                            val duration = effectiveEnd - effectiveStart
                            durations[routine.state] = (durations[routine.state] ?: 0L) + duration
                        }
                    }
                    RoutineSummary(durations)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun nextDay() {
        _currentDate.value = _currentDate.value.plusDays(1)
    }

    fun previousDay() {
        _currentDate.value = _currentDate.value.minusDays(1)
    }

    fun exportAndShareJson(context: Context) {
        val summary = routineSummary.value ?: return
        val jsonArray = JSONArray()

        summary.stateDurations.forEach { (state, durationMs) ->
            val obj = JSONObject()
            obj.put("state", state.name)
            obj.put("duration_minutes", TimeUnit.MILLISECONDS.toMinutes(durationMs))
            jsonArray.put(obj)
        }

        val jsonString = jsonArray.toString(2)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, jsonString)
            type = "application/json"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Routine Analysis")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
