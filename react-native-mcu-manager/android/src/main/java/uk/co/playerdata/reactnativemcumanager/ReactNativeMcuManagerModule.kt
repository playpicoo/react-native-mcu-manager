package uk.co.playerdata.reactnativemcumanager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.Uri
import android.util.Log
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.jni.JavaScriptFunction
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.DefaultManager
import io.runtime.mcumgr.managers.ImageManager
import io.runtime.mcumgr.response.dflt.McuMgrOsResponse
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse
import androidx.core.net.toUri

private const val MODULE_NAME = "ReactNativeMcuManager"
private val TAG = "McuManagerModule"

class UpdateOptions : Record {
  @Field val estimatedSwapTime: Int = 0
  @Field val upgradeFileType: Int = 0
  @Field val upgradeMode: Int? = null
  @Field val memoryAlignment: Int? = null
  @Field val windowUploadCapacity: Int? = null
}

class BootloaderInfo : Record {
  @Field var bootloader: String? = null
  @Field var mode: Int? = null
  @Field var noDowngrade: Boolean = false
}

class ReactNativeMcuManagerModule() : Module() {
  private val MCUBOOT = "MCUboot"

  private val upgrades: MutableMap<String, DeviceUpgrade<Any?>> = mutableMapOf()
  private val fileManagers: MutableMap<String, FileManager> = mutableMapOf()
  private val context
    get() = requireNotNull(appContext.reactContext) { "React Application Context is null" }

  private fun getBluetoothDevice(macAddress: String?): BluetoothDevice {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager?.adapter ?: throw Exception("No bluetooth adapter")

    return adapter.getRemoteDevice(macAddress)
  }

  override fun definition() = ModuleDefinition {
    Name(MODULE_NAME)

    AsyncFunction("eraseImage") { macAddress: String?, promise: Promise ->
      try {
        val device: BluetoothDevice = getBluetoothDevice(macAddress)

        val transport = McuMgrBleTransport(context, device)
        transport.connect(device).timeout(60000).await()

        val imageManager = ImageManager(transport)
        imageManager.erase()

        promise.resolve(null)
      } catch (e: McuMgrException) {
        promise.reject(ReactNativeMcuMgrException.fromMcuMgrException(e))
      }
    }

    Function("createUpgrade") {
        id: String,
        macAddress: String?,
        updateFileUriString: String?,
        updateOptions: UpdateOptions,
        progressCallback: JavaScriptFunction<Unit>,
        stateCallback: JavaScriptFunction<Unit> ->
      if (upgrades.contains(id)) {
        throw Exception("Update ID already present")
      }

      val device: BluetoothDevice = getBluetoothDevice(macAddress)
      val updateFileUri = Uri.parse(updateFileUriString)

      val upgrade = DeviceUpgrade<Any?>(
          device,
          context,
          updateFileUri,
          updateOptions,
          { progress ->
            appContext.executeOnJavaScriptThread {
              progressCallback(id, progress)
            }
          },
          { state ->
            appContext.executeOnJavaScriptThread {
              stateCallback(id, state)
            }
          }
      )
      this@ReactNativeMcuManagerModule.upgrades[id] = upgrade
    }

    AsyncFunction("runUpgrade") { id: String, promise: Promise ->
      val upgrade = upgrades[id]

      if (upgrade == null) {
        promise.reject(CodedException("UPGRADE_ID_MISSING", "Upgrade ID $id not present", null))
        return@AsyncFunction
      }

      upgrade.startUpgrade(promise)
    }

    AsyncFunction("cancelUpgrade") { id: String, promise: Promise ->
      val upgrade = upgrades[id]

      if (upgrade == null) {
        promise.reject(CodedException("UPGRADE_ID_MISSING", "Upgrade ID $id not present", null))
        return@AsyncFunction
      }

      upgrade.startUpgrade(promise)
    }

    Function("destroyUpgrade") { id: String ->
      val upgrade = upgrades[id]

      if (upgrade == null) {
        Log.w(TAG, "Can't destroy update ID ($id} not present")
        return@Function
      }

      upgrade.cancel()
      upgrades.remove(id)
    }

    AsyncFunction("resetDevice") { macAddress: String, promise: Promise ->
      val device: BluetoothDevice = getBluetoothDevice(macAddress)

      val transport = McuMgrBleTransport(context, device)
      transport.connect(device).timeout(60000).await()

      val manager = DefaultManager(transport)

      val callback = object: McuMgrCallback<McuMgrOsResponse> {
        override fun onResponse(response: McuMgrOsResponse) {
          transport.release()
          promise.resolve()
        }

        override fun onError(error: McuMgrException) {
          transport.release()
          promise.reject(CodedException("RESET_DEVICE_FAILED", "Failed to reset device", error))
        }
      }

      manager.reset(callback)
    }

    AsyncFunction("confirmImage") { macAddress: String, promise:Promise ->
      val device : BluetoothDevice = getBluetoothDevice(macAddress)

      val transport = McuMgrBleTransport(context, device)
      transport.connect(device).timeout(60000).await()

      val imageManager = ImageManager(transport);

      val callback = object : McuMgrCallback<McuMgrImageStateResponse?> {
        override fun onResponse(response: McuMgrImageStateResponse) {
          transport.release()
          promise.resolve()
        }

        override fun onError(error: McuMgrException) {
          transport.release()
          promise.reject(CodedException("CONFIRM_IMAGE_FAILED", "Failed to confirm image", error))
        }
      }

      imageManager.confirm(null, callback)
    }

    //region FileManager
    Function("createFileManager") {
        id: String,
        macAddress: String? ->
      if (fileManagers.contains(id)) {
        throw Exception("Update ID already present")
      }

      val device: BluetoothDevice = getBluetoothDevice(macAddress)

      val fileManager = FileManager(id, device, context)

      this@ReactNativeMcuManagerModule.fileManagers[id] = fileManager
    }

    AsyncFunction("uploadFile") { id: String, sourceFileURIString: String, targetFilePath: String, progressHandler: JavaScriptFunction<Unit>, promise:Promise ->
      val fileManager = fileManagers[id]

      if (fileManager == null) {
        promise.reject(CodedException("FILE_MANAGER_ID_MISSING", "FileManager ID $id not present", null))
        return@AsyncFunction
      }

      val sourceFileUri = sourceFileURIString.toUri()

      fileManager.upload(promise, sourceFileUri, targetFilePath, { percentage: Int, bytesSent: Int ->
        appContext.executeOnJavaScriptThread( {
          progressHandler(percentage, bytesSent)
        })
      })
    }

    AsyncFunction("writeFile") { id: String, data:IntArray, filePath: String, promise:Promise ->
      val fileManager = fileManagers[id]

      if (fileManager == null) {
        promise.reject(CodedException("FILE_MANAGER_ID_MISSING", "FileManager ID $id not present", null))
        return@AsyncFunction
      }

      fileManager.write(promise, data, filePath)
    }

    AsyncFunction("statFile") { id: String, filePath: String, promise:Promise ->
      val fileManager = fileManagers[id]

      if (fileManager == null) {
        promise.reject(CodedException("FILE_MANAGER_ID_MISSING", "FileManager ID $id not present", null))
        return@AsyncFunction
      }

      fileManager.status(promise, filePath)
    }

    AsyncFunction("getFileSha256Hash") { id: String, filePath: String, promise:Promise ->
      val fileManager = fileManagers[id]

      if (fileManager == null) {
        promise.reject(CodedException("FILE_MANAGER_ID_MISSING", "FileManager ID $id not present", null))
        return@AsyncFunction
      }

      fileManager.hash(promise, filePath)
    }

    Function("destroyFileManager") { id: String ->
      val fileManager = fileManagers[id]

      if (fileManager == null) {
        Log.w(TAG, "Can't destroy file manager ID ($id} not present")
        return@Function
      }

      fileManager.tearDown()
      fileManagers.remove(id)
    }

    //endregion

  }
}
