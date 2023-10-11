package uk.co.playerdata.reactnativemcumanager

import android.bluetooth.BluetoothDevice
import android.net.Uri
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.FsManager
import io.runtime.mcumgr.transfer.TransferController
import io.runtime.mcumgr.transfer.UploadCallback

class FileUpload(
    private val id: String,
    device: BluetoothDevice,
    private val context: ReactApplicationContext,
    private val uploadFileUri: Uri,
    private val uploadFilePath: String?
) : UploadCallback {

    private var transferController: TransferController? = null
    private var unsafePromise: Promise? = null
    private var transport = McuMgrBleTransport(context, device)
    private var fsManager = FsManager(transport)
    private var promiseComplete = false

    @Synchronized
    fun withSafePromise(block: (promise: Promise) -> Unit) {
        val promise = unsafePromise
        if (promise != null && !promiseComplete) {
            promiseComplete = true
            block(promise)
        }
    }

    fun start(promise: Promise) {
        unsafePromise = promise
        val stream = context.contentResolver.openInputStream(uploadFileUri)
        val imageData = ByteArray(stream!!.available())

        stream.read(imageData)

        transferController = fsManager.fileUpload(uploadFilePath!!, imageData, this)
    }

    fun cancel() {
        transferController?.cancel()
    }

    override fun onUploadProgressChanged(current: Int, total: Int, timestamp: Long) {
        val progressMap = Arguments.createMap()
        val progressPercent = current * 100 / total

        progressMap.putString("id", id)
        progressMap.putInt("progress", progressPercent)

        context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("fileUploadProgress", progressMap)
    }

    override fun onUploadFailed(error: McuMgrException) {
        transport.release()
        withSafePromise { promise -> promise.reject(error) }
    }

    override fun onUploadCanceled() {
        transport.release()
        withSafePromise { promise -> promise.reject(InterruptedException("file upload is canceled")) }
    }

    override fun onUploadCompleted() {
        transport.release()
        withSafePromise { promise -> promise.resolve(null) }
    }

}