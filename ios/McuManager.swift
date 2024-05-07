import os
import CoreBluetooth
import iOSMcuManagerLibrary


@objc(RNMcuManager)
class RNMcuManager: RCTEventEmitter {
    var upgrades: Dictionary<String, DeviceUpgrade>
    var fileManagers: Dictionary<String, DeviceFileManager>

    override init() {
        self.upgrades = [:]
        self.fileManagers = [:]

        super.init()
    }

    @objc override func supportedEvents() -> [String] {
        return [
            "uploadProgress",
            "upgradeStateChanged",
            "fileUploadProgress"
        ]
    }
    
    @objc
    func reset(_ bleId:String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {
        guard let bleUuid = UUID(uuidString: bleId) else {
            // TODO: return proper error
            let error = NSError(domain: "", code: 200, userInfo: nil)
            return reject("error", "failed to parse uuid", error)
        }

        let bleTransport = McuMgrBleTransport(bleUuid)
        let manager = DefaultManager(transporter: bleTransport)
        
        manager.reset { (response: McuMgrResponse?, err:Error?)  in
            bleTransport.close()
            
            if err != nil {
                reject("RESET_ERR", err?.localizedDescription, err)
                return
            }
            
            resolve(nil)
        }
    }

    @objc
    func eraseImage(_ bleId: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {
        guard let bleUuid = UUID(uuidString: bleId) else {
            // TODO: return proper error
            let error = NSError(domain: "", code: 200, userInfo: nil)
            return reject("error", "failed to parse uuid", error);
        }

        let bleTransport = McuMgrBleTransport(bleUuid)
        let imageManager = ImageManager(transporter: bleTransport)

        imageManager.erase { (response: McuMgrResponse?, err: Error?) in
            bleTransport.close()

            if (err != nil) {
                reject("ERASE_ERR", err?.localizedDescription, err)
                return
            }

            resolve(nil)
            return
        }
    }

    @objc
    func confirmImage(_ bleId: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {
        guard let bleUuid = UUID(uuidString: bleId) else {
            // TODO: return proper error
            let error = NSError(domain: "", code: 200, userInfo: nil)
            return reject("error", "failed to parse uuid", error);
        }

        let bleTransport = McuMgrBleTransport(bleUuid)
        let imageManager = ImageManager(transporter: bleTransport)

        imageManager.confirm { (response: McuMgrResponse?, err: Error?) in
            bleTransport.close()

            if (err != nil) {
                reject("CONFIRM_ERR", err?.localizedDescription, err)
                return
            }

            resolve(nil)
            return
        }
    }

    @objc
    func createFileManager(_ id:String, bleId: String) -> Void {
        fileManagers[id] = DeviceFileManager(id: id, bleId: bleId, eventEmitter: self)
    }
    
    @objc
    func uploadFile(_ id:String, sourceFileUriString: String, targetFilePath: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {
        
        guard let fileManager = self.fileManagers[id] else {
            reject("ID_NOT_FOUND", "File manager object not found", nil)
            return
        }
        
        fileManager.upload(sourceFileURI: sourceFileUriString, targetFilePath: targetFilePath, resolver: resolve, rejecter: reject)
    }

    @objc
    func statFile(_ id: String, filePath: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {

        guard let fileManager = self.fileManagers[id] else {
            reject("ID_NOT_FOUND", "File manager object not found", nil)
            return
        }
        
        fileManager.status(filePath: filePath, resolver: resolve, rejecter: reject)
    }

    @objc
    func getFileHash(_ id: String, filePath: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {

        guard let fileManager = self.fileManagers[id] else {
            reject("ID_NOT_FOUND", "File manager object not found", nil)
            return
        }
        
        fileManager.hash(filePath: filePath, resolver: resolve, rejecter: reject)
    }

    @objc
    func destroyFileManager(_ id: String) {
        
        guard let fileManager = self.fileManagers[id] else {
            return
        }

        fileManager.tearDown()
        
        self.fileManagers[id] = nil
    }
    
    @objc
    func createUpgrade(_ id: String, bleId: String, updateFileUriString: String, updateOptions: Dictionary<String, Any>) -> Void {
        upgrades[id] = DeviceUpgrade(id: id, bleId: bleId, fileURI: updateFileUriString, options: updateOptions, eventEmitter: self)
    }

    @objc
    func runUpgrade(_ id: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {
        guard let upgrade = self.upgrades[id] else {
            reject("ID_NOT_FOUND", "Upgrade object not found", nil)
            return
        }

        upgrade.startUpgrade(resolver: resolve, rejecter: reject)
    }

    @objc
    func cancelUpgrade(_ id: String) -> Void {
        guard let upgrade = self.upgrades[id] else {
            return
        }

        upgrade.cancel();
    }

    @objc
    func destroyUpgrade(_ id: String) -> Void {
        guard let upgrade = self.upgrades[id] else {
            return
        }

        upgrade.cancel();
        self.upgrades[id] = nil
    }
}
