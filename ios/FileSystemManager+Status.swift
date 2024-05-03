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

public class McuMgrFsHashResponse: McuMgrResponse {

    /// type of the used hash function
    public var type: String!
    
    /// offset that hash/checksum calculation started at
    public var off: UInt64?
    
    /// length of input data used for hash/checksum generation
    public var len: UInt64!
    
    /// output hash/checksum
    public var output: [UInt8]!
    
    public required init(cbor: CBOR?) throws {
        try super.init(cbor: cbor)
        if case let CBOR.unsignedInt(len)? = cbor?["len"] {self.len = len}
        if case let CBOR.utf8String(type)? = cbor?["type"] {self.type = type}
        if case let CBOR.unsignedInt(off)? = cbor?["off"] {self.off = off}
        if case let CBOR.byteString(output)? = cbor?["output"] {self.output = output}
    }
}

extension FileSystemManager {
    enum FileSystemCommandId:UInt8 {
        case Status = 1
        case Hash = 2
    }
    
    func status(name:String, callback: @escaping McuMgrCallback<McuMgrFsStatusResponse>) {
        let payload: [String:CBOR] = ["name": CBOR.utf8String(name)]
        self.send(op: .read, commandId: FileSystemCommandId.Status, payload: payload, callback: callback)
    }
    
    func hash(name:String, callback: @escaping McuMgrCallback<McuMgrFsHashResponse>) {
        let payload: [String:CBOR] = ["name": CBOR.utf8String(name), "type": CBOR.utf8String("sha256")]
        self.send(op: .read, commandId: FileSystemCommandId.Hash, payload: payload, callback: callback)
    }
}
