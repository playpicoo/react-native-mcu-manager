//
//  FileSystemManager+Status.swift
//  react-native-mcu-manager
//
//  Created by Sander on 07/11/2023.
//

import Foundation
import iOSMcuManagerLibrary
import SwiftCBOR

public class McuMgrFsStatusResponse: McuMgrResponse {
    
    /// The size of the file in bytes.
    public var len: UInt64?
    
    public required init(cbor: CBOR?) throws {
        try super.init(cbor: cbor)
        if case let CBOR.unsignedInt(len)? = cbor?["len"] {self.len = len}
    }
}

extension FileSystemManager {
    enum FileSystemCommandId:UInt8 {
        case Status = 1
    }
    
    func status(name:String, callback: @escaping McuMgrCallback<McuMgrFsStatusResponse>) {
        // Build the request payload.
        var payload: [String:CBOR] = ["name": CBOR.utf8String(name)]
        
        self.send(op: .read, commandId: FileSystemCommandId.Status, payload: payload, callback: callback)
    }
}
