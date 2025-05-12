package uk.co.playerdata.reactnativemcumanager

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import android.util.Log
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.exception.McuMgrErrorException
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.FsManager
import io.runtime.mcumgr.response.fs.McuMgrFsSha256Response
import io.runtime.mcumgr.response.fs.McuMgrFsStatusResponse
import io.runtime.mcumgr.transfer.DownloadCallback
import io.runtime.mcumgr.transfer.FileUploader
import io.runtime.mcumgr.transfer.TransferController
import io.runtime.mcumgr.transfer.UploadCallback

class FileManager(
    private val id: String,
    device: BluetoothDevice,
    private val context: Context
) : UploadCallback, DownloadCallback {
    private val TAG = "FileManager"
    private var transferController: TransferController? = null
    private var transport = McuMgrBleTransport(context, device)
    private var fsManager = FsManager(transport)
    private var unsafePromise: Promise? = null
    private var promiseComplete = true
    private var progressCallback: ((Int, Int) -> Unit)? = null

//    init {
//        Log.d(TAG, "enable ble transport logging")
//        transport.setLoggingEnabled(true)
//    }

    @Synchronized
    fun withSafePromise(block: (promise: Promise) -> Unit) {
        val promise = unsafePromise
        if (promise != null && !promiseComplete) {
            promiseComplete = true
            block(promise)
        }
    }

    fun upload(
        promise: Promise,
        uploadFileUri: Uri,
        uploadFilePath: String?,
        progressCallback: (Int, Int) -> Unit
    ) {

        Log.d(TAG, "upload, source=${uploadFileUri}, target=${uploadFilePath}")
        Log.v(TAG, "transport isConnected=${transport.isConnected}")

        if (!promiseComplete) {
            promise.resolve(CodedException("FILE_MANAGER_BUSY", "The File Manager is busy", null))
            return
        }

        promiseComplete = false
        unsafePromise = promise

        this.progressCallback = progressCallback

        val stream = context.contentResolver.openInputStream(uploadFileUri)
        val imageData = ByteArray(stream!!.available())

        stream.read(imageData)
        stream.close()

        val uploader = FileUploader(fsManager, uploadFilePath!!, imageData)
        transferController = uploader.uploadAsync(this)
    }

    fun write(promise: Promise, data: IntArray, filePath: String) {
        Log.d(TAG, "write, data=${data}, path=${filePath}")

        if (!promiseComplete) {
            promise.resolve(CodedException("FILE_MANAGER_BUSY", "The File Manager is busy", null))
            return
        }

        promiseComplete = false
        unsafePromise = promise

        val bytes = ByteArray(data.size)

        for (i in data.indices) {
            bytes[i] = data[i].toByte()
        }

        val uploader = FileUploader(fsManager, filePath, bytes)

        transferController = uploader.uploadAsync(this)
    }

    fun read(promise: Promise, filePath: String) {
        Log.d(TAG, "read, path=${filePath}")

        if (!promiseComplete) {
            promise.resolve(CodedException("FILE_MANAGER_BUSY", "The File Manager is busy", null))
            return
        }

        promiseComplete = false
        unsafePromise = promise

        transferController = fsManager.fileDownload(filePath, this)
    }

    fun status(promise: Promise, filePath: String) {
        Log.d(TAG, "status, file=${filePath}")
        Log.v(TAG, "transport isConnected=${transport.isConnected}")

        if (!promiseComplete) {
            promise.resolve(CodedException("FILE_MANAGER_BUSY", "The File Manager is busy", null))
            return
        }

        promiseComplete = false
        unsafePromise = promise

        fsManager.status(filePath, object : McuMgrCallback<McuMgrFsStatusResponse?> {
            override fun onResponse(p0: McuMgrFsStatusResponse) {
                Log.v(TAG, "status: len=${p0.len}, rc=${p0.rc}")
                withSafePromise { promise -> promise.resolve(p0.len) }
            }

            override fun onError(error: McuMgrException) {
                if (error is McuMgrErrorException && error.code == McuMgrErrorCode.NO_ENTRY) {
                    Log.d(TAG, "status: file does not exist")
                    withSafePromise { promise -> promise.resolve(-1) }
                } else {
                    Log.e(TAG, "status: error=${error.localizedMessage}")
                    withSafePromise { promise ->
                        promise.reject(
                            ReactNativeMcuMgrException.fromMcuMgrException(
                                error
                            )
                        )
                    }
                }
            }
        })
    }

    fun hash(promise: Promise, filePath: String) {
        Log.d(TAG, "hash, file=${filePath}")
        Log.v(TAG, "transport isConnected=${transport.isConnected}")

        if (!promiseComplete) {
            promise.resolve(CodedException("FILE_MANAGER_BUSY", "The File Manager is busy", null))
            return
        }

        promiseComplete = false
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

            override fun onError(error: McuMgrException) {
                if (error is McuMgrErrorException && error.code == McuMgrErrorCode.NO_ENTRY) {
                    Log.d(TAG, "hash: file does not exist")
                    withSafePromise { promise -> promise.resolve(null) }
                } else {
                    Log.e(TAG, "hash: error=${error.localizedMessage}")
                    withSafePromise { promise ->
                        promise.reject(
                            ReactNativeMcuMgrException.fromMcuMgrException(
                                error
                            )
                        )
                    }
                }
            }
        })
    }

    fun tearDown() {
        Log.d(TAG, "tearing down file manager. canceling any uploads and releasing the connection.")
        promiseComplete = true
        transferController?.cancel()
        transport.release()
    }

    //region UploadCallback Methods
    override fun onUploadProgressChanged(current: Int, total: Int, timestamp: Long) {
        Log.v(TAG, "upload progress, current=${current}, total=${total}, timestamp=${timestamp}")

        val progressPercent = current * 100 / total

        Log.v(TAG, "progressPercent=${progressPercent}")

        this.progressCallback?.let { it(progressPercent, current) }
    }

    override fun onUploadFailed(error: McuMgrException) {
        Log.e(TAG, "upload failed, error=${error}")
        withSafePromise { promise ->
            promise.reject(
                ReactNativeMcuMgrException.fromMcuMgrException(
                    error
                )
            )
        }
    }

    override fun onUploadCanceled() {
        Log.w(TAG, "upload canceled")

        withSafePromise { promise ->
            promise.reject(
                CodedException(
                    "UPLOAD_CANCELLED",
                    "Upload cancelled",
                    null
                )
            )
        }
    }

    override fun onUploadCompleted() {
        Log.d(TAG, "upload completed.")
        withSafePromise { promise -> promise.resolve(null) }
    }
    //endregion

    //region DownloadCallback Methods
    override fun onDownloadProgressChanged(p0: Int, p1: Int, p2: Long) {
        Log.d(TAG, "download progress")
    }

    override fun onDownloadFailed(error: McuMgrException) {
        Log.e(TAG, "download failed, error=${error}")

        withSafePromise { promise ->
            promise.reject(
                ReactNativeMcuMgrException.fromMcuMgrException(
                    error
                )
            )
        }
    }

    override fun onDownloadCanceled() {
        Log.w(TAG, "download canceled")

        withSafePromise { promise ->
            promise.reject(
                CodedException(
                    "DOWNLOAD_CANCELLED",
                    "Download cancelled",
                    null
                )
            )
        }
    }

    override fun onDownloadCompleted(bytes: ByteArray) {
        Log.d(TAG, "download completed")
        withSafePromise { promise -> promise.resolve(bytes) }
    }
    //endregion
}