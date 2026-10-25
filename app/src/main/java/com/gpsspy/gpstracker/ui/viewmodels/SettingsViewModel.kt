package com.gpsspy.gpstracker.ui.viewmodels

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gpsspy.gpstracker.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val homeLocation: StateFlow<Pair<Double?, Double?>> = settingsRepository.homeLocationFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Pair(null, null)
    )

    val workLocation: StateFlow<Pair<Double?, Double?>> = settingsRepository.workLocationFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Pair(null, null)
    )

    val analysisRadius: StateFlow<Float> = settingsRepository.analysisRadiusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_ANALYSIS_RADIUS
    )

    fun updateAnalysisRadius(radius: Float) {
        viewModelScope.launch {
            settingsRepository.updateAnalysisRadius(radius)
        }
    }

    fun geocodeAddress(addressStr: String, isHome: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = suspendCancellableCoroutine<List<Address>> { continuation ->
                    geocoder.getFromLocationName(addressStr, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IOException(errorMessage ?: "Geocoding failed")
                                )
                            }
                        }
                    })
                }
                handleGeocodeResult(addresses, isHome, onResult)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Geocoding failed")
            }
        }
    }

    private suspend fun handleGeocodeResult(addresses: List<Address>?, isHome: Boolean, onResult: (Boolean, String?) -> Unit) {
        if (!addresses.isNullOrEmpty()) {
            val location = addresses[0]
            if (location.hasLatitude() && location.hasLongitude()) {
                val lat = location.latitude
                val lng = location.longitude
                if (isHome) {
                    settingsRepository.updateHomeLocation(lat, lng)
                } else {
                    settingsRepository.updateWorkLocation(lat, lng)
                }
                onResult(true, null)
            } else {
                onResult(false, "Address coordinates not found")
            }
        } else {
            onResult(false, "Address not found")
        }
    }
}
