package uk.co.playerdata.reactnativemcumanager

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrTransport
import io.runtime.mcumgr.managers.FsManager
import uk.co.playerdata.reactnativemcumanager.response.HashResponse
import uk.co.playerdata.reactnativemcumanager.response.StatusResponse

class FsManagerExt(transporter: McuMgrTransport) : FsManager(transporter) {

    private val ID_STATUS = 1
    private val ID_HASH = 2

    /**
     * Probes the file with the provided name (asynchronous).
     */
    fun status(
        name: String,
        callback: McuMgrCallback<StatusResponse?>
    ) {
        val payloadMap = HashMap<String, Any>()
        payloadMap["name"] = name

        send(
            OP_READ, ID_STATUS, payloadMap, SHORT_TIMEOUT,
            StatusResponse::class.java, callback
        )
    }

    /**
     * Probes the file with the provided name (synchronous).
     */
    fun status(
        name: String
    ): StatusResponse {
        val payloadMap = HashMap<String, Any>()
        payloadMap["name"] = name

        return send(
            OP_READ, ID_STATUS, payloadMap, SHORT_TIMEOUT,
            StatusResponse::class.java
        )
    }

    fun hash(
        name: String,
        callback: McuMgrCallback<HashResponse?>
    ) {
        val payloadMap = HashMap<String, Any>()
        payloadMap["name"] = name
        payloadMap["type"] = "sha256"

        send(
            OP_READ, ID_HASH, payloadMap, SHORT_TIMEOUT,
            HashResponse::class.java, callback
        )
    }

    /**
     * Probes the file with the provided name (synchronous).
     */
    fun hash(
        name: String
    ): HashResponse {
        val payloadMap = HashMap<String, Any>()
        payloadMap["name"] = name
        payloadMap["type"] = "sha256"

        return send(
            OP_READ, ID_HASH, payloadMap, SHORT_TIMEOUT,
            HashResponse::class.java
        )
    }

}