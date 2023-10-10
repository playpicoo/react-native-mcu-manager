package uk.co.playerdata.reactnativemcumanager

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrTransport
import io.runtime.mcumgr.managers.FsManager
import io.runtime.mcumgr.response.fs.McuMgrFsDownloadResponse

class FsManagerExt(transporter: McuMgrTransport) : FsManager(transporter) {

    private val ID_STATUS = 1

    fun stat(
        name: String,
        callback: McuMgrCallback<McuMgrFsDownloadResponse?>
    ) {
        val payloadMap = HashMap<String, Any>()
        payloadMap["name"] = name

        send(
            OP_READ, ID_STATUS, payloadMap, SHORT_TIMEOUT,
            McuMgrFsDownloadResponse::class.java, callback
        )
    }

}