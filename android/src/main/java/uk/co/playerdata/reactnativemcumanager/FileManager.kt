package uk.co.playerdata.reactnativemcumanager

import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.exception.McuMgrErrorException
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.FsManager
import io.runtime.mcumgr.response.fs.McuMgrFsSha256Response
import io.runtime.mcumgr.response.fs.McuMgrFsStatusResponse
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
    private var fsManager = FsManager(transport)
    private var promisePending = false

    init {
        Log.d(TAG, "enable ble transport logging")
        transport.setLoggingEnabled(true)
    }

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

        val uploader = FileUploader(fsManager, uploadFilePath!!, imageData)
        transferController = uploader.uploadAsync(this)
    }

    fun write(promise: Promise, data: ReadableArray, filePath: String) {
        Log.d(TAG, "write, data=${data}, path=${filePath}")

        if (promisePending) {
            promise.reject(Exception("file manager is busy"))
        }

        promisePending = true
        unsafePromise = promise

        val bytes = ByteArray(data.size())

        for (i in 0 until data.size()) {
            bytes[i] = data.getInt(i).toByte()
        }

        val uploader = FileUploader(fsManager, filePath, bytes)

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

        fsManager.status(filePath, object : McuMgrCallback<McuMgrFsStatusResponse?> {
            override fun onResponse(p0: McuMgrFsStatusResponse) {
                Log.v(TAG, "status: len=${p0.len}, rc=${p0.rc}")
                withSafePromise { promise -> promise.resolve(p0.len) }
            }

            override fun onError(p0: McuMgrException) {
                if (p0 is McuMgrErrorException && p0.code == McuMgrErrorCode.NO_ENTRY) {
                    Log.d(TAG, "status: file does not exist")
                    withSafePromise { promise -> promise.resolve(-1) }
                } else {
                    Log.e(TAG, "status: error=${p0.localizedMessage}")
                    withSafePromise { promise -> promise.reject(p0) }
                }
            }
        })
    }

    fun hash(promise: Promise, filePath: String) {
        Log.d(TAG, "hash, file=${filePath}")
        Log.v(TAG, "transport isConnected=${transport.isConnected}")

        if (promisePending) {
            promise.reject(Exception("file manager is busy"))
            return
        }

        promisePending = true
        unsafePromise = promise

        fsManager.sha256(filePath, object : McuMgrCallback<McuMgrFsSha256Response?> {
            override fun onResponse(p0: McuMgrFsSha256Response) {
                Log.v(
                    TAG,
                    "hash: output=${p0.output}, type=${p0.type} len=${p0.len}, rc=${p0.rc}"
                )
                val hashString =
                    p0.output.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
                withSafePromise { promise -> promise.resolve(hashString) }
            }

            override fun onError(p0: McuMgrException) {
                if (p0 is McuMgrErrorException && p0.code == McuMgrErrorCode.NO_ENTRY) {
                    Log.d(TAG, "hash: file does not exist")
                    withSafePromise { promise -> promise.resolve(null) }
                } else {
                    Log.e(TAG, "hash: error=${p0.localizedMessage}")
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
        progressMap.putInt("totalSize", total)

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