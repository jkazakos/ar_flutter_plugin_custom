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
        fun onCloudTaskComplete(anchorName: String?, anchor: Anchor?, state: CloudAnchorState, cloudAnchorId: String)
    }

    private val TAG: String = CloudAnchorHandler::class.java.simpleName
    private val session: Session = arSession

    @Synchronized
    fun hostCloudAnchor(anchorName: String, anchor: Anchor?, listener: CloudAnchorListener?) {
        if (anchor == null) return
        val callback = BiConsumer<String, CloudAnchorState> { cloudAnchorId, state ->
            Log.d(TAG, "State " + state.toString() + " for " + anchorName + " with id " + cloudAnchorId)
            if (state == CloudAnchorState.SUCCESS) {
                listener!!.onCloudTaskComplete(anchorName, anchor, state, cloudAnchorId)
            }
        }
        session.hostCloudAnchorAsync(anchor, /* ttlDays = */ 1, callback)
    }

    @Synchronized
    fun hostCloudAnchorWithTtl(
        anchorName: String,
        anchor: Anchor?,
        listener: CloudAnchorListener?,
        ttl: Int
    ) {
        if (anchor == null) return
        val callback = BiConsumer<String, CloudAnchorState> { cloudAnchorId, state ->
            Log.d(TAG, "State " + state.toString() + " for " + anchorName + " with id " + cloudAnchorId)
            if (state == CloudAnchorState.SUCCESS) {
                listener!!.onCloudTaskComplete(anchorName, anchor, state, cloudAnchorId)
            }
        }
        session.hostCloudAnchorAsync(anchor, ttl, callback)
    }

    @Synchronized
    fun resolveCloudAnchor(anchorId: String?, listener: CloudAnchorListener?) {
        if (anchorId == null) return
        val callback = BiConsumer<com.google.ar.core.Anchor, com.google.ar.core.Anchor.CloudAnchorState> { resultAnchor, state ->
            Log.d(TAG, "State " + state.toString() + " for " + anchorId)
            if (state == CloudAnchorState.SUCCESS) {
                listener?.onCloudTaskComplete(anchorId, resultAnchor, state, anchorId)
            }
        }
        session.resolveCloudAnchorAsync(anchorId, callback)
    }

    // Updating function no longer strictly needed, but kept for compatibility
    @Synchronized
    fun onUpdate(@Suppress("UNUSED_PARAMETER") updatedAnchors: Collection<Anchor>) {
        // No-op, since async callbacks now handle completion
        // But we keep this method so external code doesn't break
    }
}
