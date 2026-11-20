package com.example.util

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Manages Google Play In-App Updates following official Android best practices:
 * - High-priority updates (>= 4) trigger an IMMEDIATE update flow.
 * - Standard updates trigger a FLEXIBLE background update flow with Snackbar restart prompt.
 * - Resumes stalled IMMEDIATE updates and prompts for DOWNLOADED updates in onResume().
 */
class InAppUpdateHelper(
    private val activity: ComponentActivity,
    private val onUpdateDownloaded: () -> Unit
) {
    companion object {
        private const val TAG = "InAppUpdateHelper"
        private const val HIGH_PRIORITY_THRESHOLD = 4
    }

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var hasCheckedThisSession = false
    private var isListenerRegistered = false

    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    Log.d(TAG, "In-app update accepted by user.")
                }
                Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "In-app update canceled by user.")
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Log.w(TAG, "In-app update flow failed.")
                }
            }
        }

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            unregisterListener()
            onUpdateDownloaded()
        }
    }

    /**
     * Checks Google Play for an available update once per session.
     */
    fun checkForUpdate() {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    onUpdateDownloaded()
                    return@addOnSuccessListener
                }

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    startAppropriateUpdateFlow(appUpdateInfo)
                }
            }
            .addOnFailureListener { e ->
                // Expected on debug builds, emulators without Play Store, or sideloaded APKs
                Log.d(TAG, "In-app update check unavailable: ${e.message}")
            }
    }

    /**
     * Checks for stalled immediate updates or already-downloaded flexible updates when returning to foreground.
     */
    fun onResume() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    onUpdateDownloaded()
                } else if (
                    appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "In-app update resume check skipped: ${e.message}")
            }
    }

    /**
     * Completes a downloaded flexible update by restarting and installing.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    /**
     * Unregisters the install state listener to prevent leaks.
     */
    fun dispose() {
        unregisterListener()
    }

    private fun startAppropriateUpdateFlow(appUpdateInfo: AppUpdateInfo) {
        val isHighPriority = appUpdateInfo.updatePriority() >= HIGH_PRIORITY_THRESHOLD

        if (isHighPriority && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            )
        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            registerListener()
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            )
        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            )
        }
    }

    private fun registerListener() {
        if (!isListenerRegistered) {
            appUpdateManager.registerListener(installStateListener)
            isListenerRegistered = true
        }
    }

    private fun unregisterListener() {
        if (isListenerRegistered) {
            appUpdateManager.unregisterListener(installStateListener)
            isListenerRegistered = false
        }
    }
}
