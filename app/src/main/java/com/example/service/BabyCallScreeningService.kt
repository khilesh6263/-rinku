package com.example.service

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import com.example.BabyAssistantApp
import com.example.domain.model.CallLifecycleState
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.N)
class BabyCallScreeningService : CallScreeningService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        if (!isIncoming) {
            respondAllow(callDetails)
            return
        }

        val app = application as? BabyAssistantApp
        val coordinator = app?.container?.coordinator
        val callerLabel = callDetails.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
            ?: "Incoming Call"

        val responded = AtomicBoolean(false)

        coordinator?.actionExecutor?.pendingCallScreeningRejectHandler = {
            if (responded.compareAndSet(false, true)) {
                val rejectResponse = CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSilenceCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                respondToCall(callDetails, rejectResponse)
                true
            } else {
                false
            }
        }

        coordinator?.onLiveCallStateChanged(CallLifecycleState.RINGING, callerLabel)

        // Android requires CallScreeningService.respondToCall() promptly (< 5 seconds),
        // so release the call cleanly if no voice reject happens within 3.5 seconds.
        handler.postDelayed({
            if (responded.compareAndSet(false, true)) {
                coordinator?.actionExecutor?.pendingCallScreeningRejectHandler = null
                respondAllow(callDetails)
            }
        }, 3500L)
    }

    private fun respondAllow(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }
}
