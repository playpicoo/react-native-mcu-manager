package uk.co.playerdata.reactnativemcumanager

import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.exception.McuMgrErrorException
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.response.fs.McuMgrFsDownloadResponse
import io.runtime.mcumgr.transfer.FileUploader
import io.runtime.mcumgr.transfer.TransferController
import io.runtime.mcumgr.transfer.UploadCallback

class FileManager(
    private val id: String,
    device: BluetoothDevice,
    private val context: ReactApplicationContext
) : UploadCallback {
    private val TAG = "FileManager"
    private var transferController: TransferController? = null
    private var unsafePromise: Promise? = null
    private var transport = McuMgrBleTransport(context, device)
    private var fsManager = FsManagerExt(transport)
    private var promisePending = false

    @Synchronized
    fun withSafePromise(block: (promise: Promise) -> Unit) {
        val promise = unsafePromise
        if (promise != null && promisePending) {
            promisePending = false
            block(promise)
        }
    }

    fun upload(promise: Promise, uploadFileUri: Uri, uploadFilePath: String?) {
        Log.d(TAG, "upload, source=${uploadFileUri}, target=${uploadFilePath}")
        Log.v(TAG, "transport isConnected=${transport.isConnected}")

        if (promisePending) {
            promise.reject(Exception("file manager is busy"))
            return
        }

        promisePending = true
        unsafePromise = promise

        val stream = context.contentResolver.openInputStream(uploadFileUri)
        val imageData = ByteArray(stream!!.available())

        stream.read(imageData)
        stream.close()

//        transferController = fsManager.fileUpload(uploadFilePath!!, imageData, this)

        val uploader = FileUploader(fsManager, uploadFilePath!!, imageData)
        transferController = uploader.uploadAsync(this)
    }

    fun status(promise: Promise, filePath: String) {
        Log.d(TAG, "status, file=${filePath}")
        Log.v(TAG, "transport isConnected=${transport.isConnected}")

        if (promisePending) {
            promise.reject(Exception("file manager is busy"))
            return
        }

        promisePending = true
        unsafePromise = promise

        fsManager.status(filePath, object : McuMgrCallback<McuMgrFsDownloadResponse?> {
            override fun onResponse(p0: McuMgrFsDownloadResponse) {
                Log.v(TAG, "status: len=${p0.len}, rc=${p0.rc}")
                withSafePromise { promise -> promise.resolve(p0.len) }
            }

            override fun onError(p0: McuMgrException) {
                if (p0 is McuMgrErrorException && p0.code == McuMgrErrorCode.NO_ENTRY) {
                    Log.d(TAG, "status: file does not exist")
                    withSafePromise { promise -> promise.resolve(-1) }
                }
                else {
                    Log.e(TAG, "status: error=${p0.localizedMessage}")
                    withSafePromise { promise -> promise.reject(p0) }
                }
            }
        })
    }

    fun cancelUpload() {
        transferController?.cancel()
    }

    fun tearDown() {
        transferController?.cancel()
        transport.release()
    }

    override fun onUploadProgressChanged(current: Int, total: Int, timestamp: Long) {
        val progressMap = Arguments.createMap()
        val progressPercent = current * 100 / total

        progressMap.putString("id", id)
        progressMap.putInt("progress", progressPercent)
        progressMap.putInt("bytesSent", current)

        context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("fileUploadProgress", progressMap)
    }

    override fun onUploadFailed(error: McuMgrException) {
        withSafePromise { promise -> promise.reject(error) }
    }

    override fun onUploadCanceled() {
        withSafePromise { promise -> promise.reject(InterruptedException("file upload is canceled")) }
    }

    override fun onUploadCompleted() {
        withSafePromise { promise -> promise.resolve(null) }
    }
}