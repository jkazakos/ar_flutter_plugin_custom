package io.carius.lars.ar_flutter_plugin

import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.core.Session
import java.util.*
import java.util.function.BiConsumer
import kotlin.collections.set

// Class for handling logic regarding the Google Cloud Anchor API
internal class CloudAnchorHandler(arSession: Session) {

    // Listener that can be attached to hosting or resolving processes
    interface CloudAnchorListener {
        // Callback to invoke when cloud anchor task finishes
        fun onCloudTaskComplete(
            anchorName: String?,
            anchor: Anchor?,
            state: CloudAnchorState,
            cloudAnchorId: String
        )
    }

    private val TAG: String = CloudAnchorHandler::class.java.simpleName
    private val session: Session = arSession

    @Synchronized
    fun hostCloudAnchorWithTtl(
        anchorName: String,
        anchor: Anchor?,
        listener: CloudAnchorListener?,
        ttl: Int
    ) {
        if (anchor == null) return
        val callback = BiConsumer<String, CloudAnchorState> { cloudAnchorId, state ->
            try {
                Log.d(
                    TAG,
                    "State $state for $anchorName with id $cloudAnchorId"
                )
                if (state == CloudAnchorState.SUCCESS) {
                    listener!!.onCloudTaskComplete(anchorName, anchor, state, cloudAnchorId)
                }
            } catch (e: Exception) {
                Log.d(TAG, e.toString())
            }
        }
        try {
            session.hostCloudAnchorAsync(anchor, ttl, callback)
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
    }

    @Synchronized
    fun resolveCloudAnchor(anchorId: String?, listener: CloudAnchorListener?) {
        if (anchorId == null) return
        val callback =
            BiConsumer<Anchor?, CloudAnchorState>(fun(
                resultAnchor: Anchor?,
                state: CloudAnchorState
            ) {
                // resultAnchor is now nullable
                try {
                    Log.d(TAG, "State $state for $anchorId")
                    if (state == CloudAnchorState.SUCCESS && resultAnchor != null) {
                        listener?.onCloudTaskComplete(anchorId, resultAnchor, state, anchorId)
                    } else {
                        // Handle the case where resolution failed or resultAnchor is null
                        // You might want to pass a specific error state if available
                        // For now, we'll pass the received state, or a generic error if it's SUCCESS but anchor is null
                        val reportedState = if (state == CloudAnchorState.SUCCESS) {
                            Log.w(
                                TAG,
                                "Cloud anchor $anchorId resolved with SUCCESS state but null anchor. Reporting as ERROR_INTERNAL."
                            )
                            CloudAnchorState.ERROR_INTERNAL // Or another appropriate error state
                        } else {
                            state
                        }
                        listener?.onCloudTaskComplete(anchorId, null, reportedState, anchorId)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Error in resolveCloudAnchor callback: ${e.toString()}")
                    // Also notify listener of failure in case of an exception within the callback
                    listener?.onCloudTaskComplete(
                        anchorId,
                        null,
                        CloudAnchorState.ERROR_INTERNAL,
                        anchorId
                    )
                }
            })
        try {
            session.resolveCloudAnchorAsync(anchorId, callback)
        } catch (e: Exception) {
            Log.d(TAG, "Error calling resolveCloudAnchorAsync: ${e.toString()}")
            // Notify listener of failure if the async call itself throws an exception
            listener?.onCloudTaskComplete(anchorId, null, CloudAnchorState.ERROR_INTERNAL, anchorId)
        }
    }


    // Updating function no longer strictly needed, but kept for compatibility
    @Synchronized
    fun onUpdate(@Suppress("UNUSED_PARAMETER") updatedAnchors: Collection<Anchor>) {
        // No-op, since async callbacks now handle completion
        // But we keep this method so external code doesn't break
    }
}
