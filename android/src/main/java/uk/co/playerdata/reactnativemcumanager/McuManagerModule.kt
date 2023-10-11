package uk.co.playerdata.reactnativemcumanager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.FsManager
import io.runtime.mcumgr.managers.ImageManager
import io.runtime.mcumgr.response.fs.McuMgrFsDownloadResponse
import io.runtime.mcumgr.transfer.UploadCallback
import java.io.IOException

class McuManagerModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
    private val TAG = "McuManagerModule"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val upgrades: MutableMap<String, DeviceUpgrade> = mutableMapOf()
    private val uploads: MutableMap<String, FileUpload> = mutableMapOf()

    override fun getName(): String {
        return "McuManager"
    }

    @ReactMethod
    fun statFile(
        macAddress: String?,
        filePath: String?,
        promise: Promise
    ) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }
        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)
            var transport = McuMgrBleTransport(reactContext, device)
            transport.connect(device).timeout(60000).await()

            val fsManager = FsManagerExt(transport)
            fsManager.status(filePath!!, object : McuMgrCallback<McuMgrFsDownloadResponse?> {
                override fun onResponse(p0: McuMgrFsDownloadResponse) {
                    Log.v("StatusResponse", "len=${p0.len}, rc=${p0.rc}")
                    promise.resolve(p0.len)
                }

                override fun onError(p0: McuMgrException) {
                    Log.e("StatusResponse", "error=${p0.localizedMessage}")
                    promise.reject(p0)
                }
            })
        } catch (err: McuMgrException) {
            Log.e("StatusResponse", "error=${err.localizedMessage}")
            promise.reject(err)
        }
    }

    @ReactMethod
    fun uploadFile(
        macAddress: String?,
        sourceFileUriString: String?,
        targetFilePath: String?,
        promise: Promise
    ) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }

        try {
            val stream =
                reactContext.contentResolver.openInputStream(Uri.parse(sourceFileUriString))
            val fileData = ByteArray(stream!!.available())
            stream.read(fileData)

            Log.v(TAG, "file size=${fileData.size}")

            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)

            var transport = McuMgrBleTransport(reactContext, device)
            transport.connect(device).timeout(60000).await()

            val fsManager = FsManager(transport)

            fsManager.fileUpload(targetFilePath!!, fileData, object : UploadCallback {
                override fun onUploadProgressChanged(p0: Int, p1: Int, p2: Long) {
                    Log.v("UploadCallback", "onUploadProgressChanged")
                }

                override fun onUploadFailed(p0: McuMgrException) {
                    Log.v("UploadCallback", "onUploadFailed, ${p0.localizedMessage}")
                    promise.reject(p0)
                }

                override fun onUploadCanceled() {
                    Log.v("UploadCallback", "onUploadCanceled")
                    promise.resolve(null)
                }

                override fun onUploadCompleted() {
                    Log.v("UploadCallback", "onUploadCompleted")
                    promise.resolve(null)
                }
            })
        } catch (e: IOException) {
            Log.v(this.TAG, "IOException")
            promise.reject(e)
        } catch (e: McuMgrException) {
            Log.v(this.TAG, "McuMgrException")
            promise.reject(e)
        }
    }

    @ReactMethod
    fun eraseImage(macAddress: String?, promise: Promise) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)

            var transport = McuMgrBleTransport(reactContext, device)
            transport.connect(device).timeout(60000).await()

            val imageManager = ImageManager(transport);
            imageManager.erase()

            promise.resolve(null)
        } catch (e: Throwable) {
            promise.reject(e)
        }
    }

    @ReactMethod
    fun confirmImage(macAddress: String?, promise: Promise) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)

            var transport = McuMgrBleTransport(reactContext, device)
            transport.connect(device).timeout(60000).await()

            val imageManager = ImageManager(transport);
            imageManager.confirm(null)

            transport.release()

            promise.resolve(null)
        } catch (e: Throwable) {
            promise.reject(e)
        }
    }

    @ReactMethod
    fun createFileUpload(
        id: String,
        macAddress: String?,
        uploadFileUriString: String?,
        uploadFilePath:String?
    ) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }

        if (uploads.contains(id)) {
            throw Exception("Update ID already present")
        }

        val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)
        val uploadFileUri = Uri.parse(uploadFileUriString)

        this.uploads[id] = FileUpload(id, device, reactContext, uploadFileUri, uploadFilePath)
    }

    @ReactMethod
    fun runFileUpload(id: String, promise: Promise) {
        if (!uploads.contains(id)) {
            promise.reject(Exception("upload ID not present"))
        }
        uploads[id]!!.start(promise)
    }

    @ReactMethod
    fun destroyFileUpload(id: String) {
        if (!uploads.contains(id)) {
            Log.w(this.TAG, "can't destroy upload ID ($id} not present")
            return
        }
        Log.v(this.TAG, "destroying upload ID ($id}")
        uploads[id]?.cancel()
        uploads.remove(id)
    }

    @ReactMethod
    fun createUpgrade(
        id: String,
        macAddress: String?,
        updateFileUriString: String?,
        updateOptions: ReadableMap
    ) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }

        if (upgrades.contains(id)) {
            throw Exception("Update ID already present")
        }

        val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)
        val updateFileUri = Uri.parse(updateFileUriString)

        val upgrade = DeviceUpgrade(id, device, reactContext, updateFileUri, updateOptions, this)
        this.upgrades[id] = upgrade
    }

    @ReactMethod
    fun runUpgrade(id: String, promise: Promise) {
        if (!upgrades.contains(id)) {
            promise.reject(Exception("update ID not present"))
        }

        upgrades[id]!!.startUpgrade(promise)
    }

    @ReactMethod
    fun cancelUpgrade(id: String) {
        if (!upgrades.contains(id)) {
            Log.w(this.TAG, "can't cancel update ID ($id} not present")
            return
        }

        upgrades[id]!!.cancel()
    }

    @ReactMethod
    fun destroyUpgrade(id: String) {
        if (!upgrades.contains(id)) {
            Log.w(this.TAG, "can't destroy update ID ($id} not present")
            return
        }

        upgrades[id]!!.cancel()
        upgrades.remove(id)
    }

    fun updateProgressCB(progress: WritableMap?) {
        reactContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit("uploadProgress", progress)
    }

    fun upgradeStateCB(state: WritableMap?) {
        reactContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit("upgradeStateChanged", state)
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Keep: Required for RN built in Event Emitter Calls.
    }

    @ReactMethod
    fun removeListeners(count: Integer) {
        // Keep: Required for RN built in Event Emitter Calls.
    }
}
