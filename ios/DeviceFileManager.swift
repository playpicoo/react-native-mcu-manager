//
//  FileManager..swift
//  react-native-mcu-manager
//
//  Created by Sander on 07/11/2023.
//

import Foundation
import iOSMcuManagerLibrary

class DeviceFileManager {
    private let id: String
    private let bleId: String
    private let eventEmitter : RCTEventEmitter
    private let logDelegate : McuMgrLogDelegate
    
    private var fileManager: FileSystemManager
    private var bleTransport: McuMgrBleTransport
    
    var uploadResolver: RCTPromiseResolveBlock?
    var uploadRejecter: RCTPromiseRejectBlock?

    init?(id: String, bleId: String, eventEmitter: RCTEventEmitter) {
        self.id = id
        self.bleId = bleId
        self.eventEmitter = eventEmitter;
        self.logDelegate = FileManagerLogDelegate();
        
        guard let bleUuid = UUID(uuidString: bleId) else {
            print("invalid BLE UUID")
            return nil
        }

        self.bleTransport = McuMgrBleTransport(bleUuid)
        self.fileManager = FileSystemManager(transporter: self.bleTransport)
    }

    func upload(sourceFileURI: String, targetFilePath: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        guard let fileUrl = URL(string: sourceFileURI) else {
            let error = NSError(domain: "", code: 200, userInfo: nil)
            return reject("error", "failed to parse file uri as url", error);
        }

        self.uploadResolver = resolve
        self.uploadRejecter = reject
        
        do {
            let filehandle = try FileHandle(forReadingFrom: fileUrl)
            let file = Data(filehandle.availableData)
            filehandle.closeFile()
            
            let success = self.fileManager.upload(name: targetFilePath, data: file, delegate: self)
            
            if (!success) {
                return reject("error", "failed to start upload", nil)
            }
        }
        catch {
            reject(error.localizedDescription, error.localizedDescription, error)
        }
    }
    
    func status(filePath: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        self.fileManager.status(name: filePath) { [weak self] (response, error) -> Void in
            guard self != nil else {
                return
            }
                      
            if let error = error {
                return reject("error", "failed to stat file", error)
            }
            
            if let len = response?.len {
                resolve(len)
            }
            else if response?.returnCode == .noEntry {
                // in case the file does not exist, resolve with a value of -1
                resolve(-1)
            }
            else {
                return reject("error", "failed to stat file", nil)
            }
        }
        
    }

    func hash(filePath: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        self.fileManager.hash(name: filePath) { [weak self] (response, error) -> Void in
            guard self != nil else {
                return
            }
            
            if let error = error {
                return reject("error", "failed to get file hash", error)
            }
            
            if let hash = response?.output {
                let hashHexString = hash.map { String(format: "%02X", $0)}.joined().lowercased()
                resolve(hashHexString)
            }
            else if response?.returnCode == .noEntry {
                // in case the file does not exist resolve with null
                resolve(NSNull())
            }
            else {
                return reject("error", "failed to get hash of file", nil)
            }
        }
        
    }

    func tearDown() -> Void {
        self.fileManager.cancelTransfer()
        self.bleTransport.close()
    }
}

class FileManagerLogDelegate : McuMgrLogDelegate {
    func log(_ msg: String, ofCategory category: McuMgrLogCategory, atLevel level: McuMgrLogLevel) {
        if(level.rawValue < McuMgrLogLevel.info.rawValue) {
            return
        }

        print(msg);
    }
}

// - MARK: FileUploadDelegate
extension DeviceFileManager : FileUploadDelegate {
    func uploadProgressDidChange(bytesSent: Int, fileSize: Int, timestamp: Date) {
        if(self.eventEmitter.bridge != nil) {
            let progressPercent = bytesSent * 100 / fileSize
            self.eventEmitter.sendEvent(
                withName: "fileUploadProgress", body: [
                    "id": self.id,
                    "progress": progressPercent
                ]
            )
        }
    }
    
    func uploadDidFail(with error: Error) {
        self.uploadRejecter?("error", "failed to upload file", error)
    }
    
    func uploadDidCancel() {
        self.uploadRejecter?("notice", "upload was canceled", nil)
    }
    
    func uploadDidFinish() {
        self.uploadResolver?(nil)
    }
}

