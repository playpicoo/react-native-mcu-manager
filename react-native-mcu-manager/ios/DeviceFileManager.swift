//
//  FileManager..swift
//  react-native-mcu-manager
//
//  Created by Sander on 07/11/2023.
//

import Foundation
import ExpoModulesCore
import iOSMcuManagerLibrary

class DeviceFileManager {
    private let id: String
    private let bleId: String
    private let logDelegate : McuMgrLogDelegate
    
    private var fileManager: FileSystemManager
    private var bleTransport: McuMgrBleTransport
    
    private var uploadProgressHandler: ((Int, Int) -> Void)?
    
    private var uploadPromise: Promise?
    
    init?(id: String, bleId: String) {
        self.id = id
        self.bleId = bleId
        self.logDelegate = FileManagerLogDelegate();
        
        guard let bleUuid = UUID(uuidString: bleId) else {
            print("invalid BLE UUID")
            return nil
        }

        self.bleTransport = McuMgrBleTransport(bleUuid)
        self.fileManager = FileSystemManager(transport: self.bleTransport)
    }

    func upload(sourceFileURI: String, targetFilePath: String, progressHandler: @escaping (Int, Int) -> Void, _ promise: Promise) {
        
        self.uploadPromise = promise
        self.uploadProgressHandler = progressHandler
        
        guard let fileUrl = URL(string: sourceFileURI) else {
            return promise.reject(Exception(name: "URIParseError", description: "Failed to parse source file URI"))
        }
        
        do {
            let filehandle = try FileHandle(forReadingFrom: fileUrl)
            let file = Data(filehandle.availableData)
            filehandle.closeFile()
            
            let success = self.fileManager.upload(name: targetFilePath, data: file, delegate: self)
            
            if (!success) {
                return promise.reject(Exception(name: "FileError", description: "Failed to upload file"))
            }
        }
        catch {
            promise.reject(UnexpectedException(error))
        }
    }

    func write(data: [UInt8], filePath: String, _ promise: Promise) {
                
        let bytes = Data(data)

        self.uploadPromise = promise
        
        let success = self.fileManager.upload(name: filePath, data: bytes, delegate: self)
        if (!success) {
            promise.reject(Exception(name: "FileError", description: "Failed to write file"))
        }
    }

    func status(filePath: String, _ promise:Promise) {
        
        self.fileManager.status(name: filePath) { [weak self] (response, error) -> Void in
            guard self != nil else {
                return
            }
                                 
            if let error = error {
                promise.reject(Exception(name: "FileError", description: "Failed to stat file"))
            }
            
            if let len = response?.len {
                promise.resolve(len)
            }
            else if response?.rc == .noEntry {
                // in case the file does not exist, resolve with a value of -1
                promise.resolve(-1)
            }
            else {
                promise.reject(Exception(name: "FileError", description: "Failed to stat file"))
            }
        }
        
    }

    func hash(filePath: String, _ promise:Promise) {
        
        self.fileManager.sha256(name: filePath, offset: 0, length: 0) { [weak self] (response, error) -> Void in
            guard self != nil else {
                return
            }
            
            if let error = error {
                promise.reject(Exception(name: "FileError", description: "Failed to get sha256 hash"))
            }
            
            if let hash = response?.output {
                let hashHexString = hash.map { String(format: "%02X", $0)}.joined().lowercased()
                promise.resolve(hashHexString)
            }
            else if response?.rc == .noEntry {
                // in case the file does not exist resolve with null
                promise.resolve(NSNull())
            }
            else {
                promise.reject(Exception(name: "FileError", description: "Failed to get sha256 hash"))
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
    private func getFirmwareUpgradeException(_ error: Error) -> Exception {
        return Exception(
            name: "McuMgr_\(String(describing: error.self))", description: error.localizedDescription)
    }

    func uploadProgressDidChange(bytesSent: Int, fileSize: Int, timestamp: Date) {
            let progress = bytesSent * 100 / fileSize
        self.uploadProgressHandler?(progress, bytesSent)
    }
    
    func uploadDidFail(with error: Error) {
        self.uploadPromise?.reject(getFirmwareUpgradeException(error))
    }
    
    func uploadDidCancel() {
        self.uploadPromise?.reject(Exception(name: "UploadCancelled", description: "Upload cancelled"))
    }
    
    func uploadDidFinish() {
        self.uploadPromise?.resolve()
    }
}

