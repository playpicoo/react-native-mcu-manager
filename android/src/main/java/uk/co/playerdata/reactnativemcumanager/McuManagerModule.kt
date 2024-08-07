package uk.co.playerdata.reactnativemcumanager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.exception.McuMgrErrorException
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.DefaultManager
import io.runtime.mcumgr.managers.FsManager
import io.runtime.mcumgr.managers.ImageManager
import io.runtime.mcumgr.response.McuMgrResponse
import io.runtime.mcumgr.response.dflt.McuMgrOsResponse
import io.runtime.mcumgr.response.fs.McuMgrFsDownloadResponse
import io.runtime.mcumgr.transfer.UploadCallback
import java.io.IOException

class McuManagerModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
    private val TAG = "McuManagerModule"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val upgrades: MutableMap<String, DeviceUpgrade> = mutableMapOf()
    private val fileManagers: MutableMap<String, FileManager> = mutableMapOf()

    override fun getName(): String {
        return "McuManager"
    }

    @ReactMethod
    fun eraseImage(macAddress: String?, promise: Promise) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)

            val transport = McuMgrBleTransport(reactContext, device)
            transport.connect(device).timeout(60000).await()

            val imageManager = ImageManager(transport);
            imageManager.erase()

            transport.release()

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

            val transport = McuMgrBleTransport(reactContext, device)
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
    fun reset(macAddress: String?, promise: Promise) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)

            val transport = McuMgrBleTransport(reactContext, device)
            transport.connect(device).timeout(60000).await()

            val manager = DefaultManager(transport);

            manager.reset(object : McuMgrCallback<McuMgrOsResponse?> {
                override fun onResponse(p0: McuMgrOsResponse) {
                    transport.release()

                    if (!p0.isSuccess) {
                        promise.reject(McuMgrErrorException(p0.returnCode))
                    } else {
                        promise.resolve(null)
                    }
                }

                override fun onError(p0: McuMgrException) {
                    transport.release()
                    promise.reject(p0)
                }
            })
        } catch (e: Throwable) {
            promise.reject(e)
        }
    }

    @ReactMethod
    fun createFileManager(
        id: String,
        macAddress: String?
    ) {
        if (this.bluetoothAdapter == null) {
            throw Exception("No bluetooth adapter")
        }

        if (fileManagers.contains(id)) {
            throw Exception("file manager ID already present")
        }

        val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)

        this.fileManagers[id] = FileManager(id, device, reactContext)
    }

    @ReactMethod
    fun cancelUpload(fileManagerId: String) {
        if (!fileManagers.contains(fileManagerId)) {
            Log.w(this.TAG, "can't cancel upload. file manager ID not present")
        }
        fileManagers[fileManagerId]!!.cancelUpload()
    }

    @ReactMethod
    fun uploadFile(
        fileManagerId: String,
        sourceFileUriString: String?,
        targetFilePath: String?,
        promise: Promise
    ) {
        if (!fileManagers.contains(fileManagerId)) {
            promise.reject(Exception("file manager ID not present"))
        }
        val uploadFileUri = Uri.parse(sourceFileUriString)
        fileManagers[fileManagerId]!!.upload(promise, uploadFileUri, targetFilePath)
    }

    @ReactMethod
    fun writeFile(
        fileManagerId: String,
        data: ReadableArray?,
        filePath: String?,
        promise: Promise
    ) {
        if (!fileManagers.contains(fileManagerId)) {
            promise.reject(Exception("file manager ID not present"))
        }
        fileManagers[fileManagerId]!!.write(promise, data!!, filePath!!)
    }

    @ReactMethod
    fun statFile(fileManagerId: String, filePath: String?, promise: Promise) {
        if (!fileManagers.contains(fileManagerId)) {
            promise.reject(Exception("file manager ID not present"))
        }
        fileManagers[fileManagerId]!!.status(promise, filePath!!)
    }

    @ReactMethod
    fun getFileHash(fileManagerId: String, filePath: String?, promise: Promise) {
        if (!fileManagers.contains(fileManagerId)) {
            promise.reject(Exception("file manager ID not present"))
        }
        fileManagers[fileManagerId]!!.hash(promise, filePath!!)
    }

    @ReactMethod
    fun resetFileManager(id: String) {
        if (!fileManagers.contains(id)) {
            Log.w(this.TAG, "can't reset file manager. ID ($id} not present")
            return
        }
        Log.d(this.TAG, "resetting file manager ID ($id}")
        fileManagers[id]?.tearDown()
    }

    @ReactMethod
    fun destroyFileManager(id: String) {
        if (!fileManagers.contains(id)) {
            Log.w(this.TAG, "can't destroy file manager ID ($id} not present")
            return
        }
        Log.v(this.TAG, "destroying file manager ID ($id}")
        fileManagers[id]?.tearDown()
        fileManagers.remove(id)
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
