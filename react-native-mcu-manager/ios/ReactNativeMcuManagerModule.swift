import CoreBluetooth
import ExpoModulesCore
import iOSMcuManagerLibrary
import os

private let MODULE_NAME = "ReactNativeMcuManager"
private let TAG = "McuManagerModule"

public class ReactNativeMcuManagerModule: Module {
    private var upgrades: [String: DeviceUpgrade] = [:]
    private var fileManagers: [String: DeviceFileManager] = [:]
    
    public func definition() -> ModuleDefinition {
        Name(MODULE_NAME)
        
        // MARK: - DeviceUpgrade
               
        Function("createUpgrade") {
            (
                id: String,
                bleId: String,
                updateFileUriString: String,
                updateOptions: UpdateOptions,
                progressCallback: JavaScriptFunction<ExpressibleByNilLiteral>,
                stateCallback: JavaScriptFunction<ExpressibleByNilLiteral>
            ) in
            upgrades[id] = DeviceUpgrade(
                id: id,
                bleId: bleId,
                fileURI: updateFileUriString,
                options: updateOptions,
                progressHandler: { progress in
                    self.appContext?.executeOnJavaScriptThread {
                        do {
                            let _ = try progressCallback.call(id, progress)
                        } catch let err {
                            print("Failed to call progress callback: \(err.localizedDescription)")
                        }
                    }
                },
                stateHandler: { state in
                    self.appContext?.executeOnJavaScriptThread {
                        do {
                            let _ = try stateCallback.call(id, state)
                        } catch let err {
                            print("Failed to call state callback: \(err.localizedDescription)")
                        }
                    }
                }
            )
        }
        
        AsyncFunction("runUpgrade") { (id: String, promise: Promise) in
            guard let upgrade = self.upgrades[id] else {
                promise.reject(Exception(name: "UpgradeNotFound", description: "Upgrade object not found"))
                return
            }
            
            upgrade.startUpgrade(promise)
        }
        
        Function("cancelUpgrade") { (id: String) in
            guard let upgrade = self.upgrades[id] else {
                return
            }
            
            upgrade.cancel()
        }
        
        Function("destroyUpgrade") { (id: String) in
            guard let upgrade = self.upgrades[id] else {
                return
            }
            
            upgrade.cancel()
            self.upgrades[id] = nil
        }
        
        // MARK: - Device functions
        
        AsyncFunction("eraseImage") { (bleId: String, promise: Promise) in
            guard let bleUuid = UUID(uuidString: bleId) else {
                promise.reject(Exception(name: "UUIDParseError", description: "Failed to parse UUID"))
                return
            }
            
            let bleTransport = McuMgrBleTransport(bleUuid)
            let imageManager = ImageManager(transport: bleTransport)
            
            imageManager.erase { (response: McuMgrResponse?, err: Error?) in
                bleTransport.close()
                
                if err != nil {
                    promise.reject(Exception(name: "EraseError", description: err!.localizedDescription))
                    return
                }
                
                promise.resolve(nil)
                return
            }
        }

        AsyncFunction("resetDevice") { (bleId: String, promise: Promise) in
            guard let bleUuid = UUID(uuidString: bleId) else {
                promise.reject(Exception(name: "UUIDParseError", description: "Failed to parse UUID"))
                return
            }
            
            let bleTransport = McuMgrBleTransport(bleUuid)
            let manager = DefaultManager(transport: bleTransport)
            
            manager.reset { (response: McuMgrResponse?, err: Error?) in
                bleTransport.close()
                
                if err != nil {
                    promise.reject(Exception(name: "ResetError", description: err!.localizedDescription))
                    return
                }
                
                let smpErr = response?.getError()
                if (smpErr != nil) {
                    promise.reject(Exception(name: "ResetError", description: smpErr!.localizedDescription))
                    return
                }
                
                promise.resolve()
            }
        }
        
        AsyncFunction("confirmImage") { (bleId: String, promise: Promise) in
            guard let bleUuid = UUID(uuidString: bleId) else {
                promise.reject(Exception(name: "UUIDParseError", description: "Failed to parse UUID"))
                return
            }
            
            let bleTransport = McuMgrBleTransport(bleUuid)
            let imageManager = ImageManager(transport: bleTransport)
            
            imageManager.confirm { (response: McuMgrResponse?, err: Error?) in
                bleTransport.close()
                
                if (err != nil) {
                    promise.reject(Exception(name: "ConfirmError", description: err!.localizedDescription))
                    return
                }
                
                let smpErr = response?.getError()
                if (smpErr != nil) {
                    promise.reject(Exception(name: "ConfirmError", description: smpErr!.localizedDescription))
                    return
                }
                
                promise.resolve()
            }
        }
        
        // MARK: - FileManager
        
        Function("createFileManager") { (id: String,bleId: String) in
            fileManagers[id] = DeviceFileManager(id: id, bleId: bleId)
        }
        
        AsyncFunction("uploadFile") { (id: String, sourceFileURI: String, targetFilePath: String, progressHandler:JavaScriptFunction<ExpressibleByNilLiteral>, promise: Promise) in
            guard let fileManager = self.fileManagers[id] else {
                promise.reject(Exception(name: "FileManagerNotFound", description: "FileManager object not found"))
                return
            }
            
            fileManager.upload(sourceFileURI: sourceFileURI, targetFilePath: targetFilePath, progressHandler: { (progress, bytesSent) in
                
                self.appContext?.executeOnJavaScriptThread {
                    do {
                        let _ = try progressHandler.call(id, progress, bytesSent)
                    } catch let err {
                        print("Failed to call progress callback: \(err.localizedDescription)")
                    }
                }
                
            }, promise)
        }

        AsyncFunction("writeFile") { (id: String, data: [UInt8], filePath: String, promise: Promise) in
            guard let fileManager = self.fileManagers[id] else {
                promise.reject(Exception(name: "FileManagerNotFound", description: "FileManager object not found"))
                return
            }
            
            fileManager.write(data: data, filePath: filePath, promise)
        }
        
        AsyncFunction("statFile") { (id: String, filePath: String, promise: Promise) in
            guard let fileManager = self.fileManagers[id] else {
                promise.reject(Exception(name: "FileManagerNotFound", description: "FileManager object not found"))
                return
            }
            
            fileManager.status(filePath: filePath, promise)
        }

        AsyncFunction("getFileSha256Hash") { (id: String, filePath: String, promise: Promise) in
            guard let fileManager = self.fileManagers[id] else {
                promise.reject(Exception(name: "FileManagerNotFound", description: "FileManager object not found"))
                return
            }
            
            fileManager.hash(filePath: filePath, promise)
        }

        Function("destroyFileManager") { (id: String) in
            guard let fileManager = self.fileManagers[id] else {
                return
            }
            
            fileManager.tearDown()
            self.fileManagers[id] = nil
        }
    }
}
