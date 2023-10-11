import {
    NativeModules,
    NativeEventEmitter,
    EmitterSubscription,
  } from 'react-native';
  
  import { v4 as uuidv4 } from 'uuid';
  
  const { McuManager } = NativeModules;
  
  const McuManagerEvents = new NativeEventEmitter(McuManager);
  
  declare const UpgradeIdSymbol: unique symbol;
  type UpgradeID = string & { [UpgradeIdSymbol]: never };
  
  type AddFileUploadListener = {
    (
      eventType: 'fileUploadProgress',
      listener: ({ progress }: { progress: number }) => void,
      context?: any
    ): EmitterSubscription;
  };
  
  class FileUpload {
    private id: UpgradeID;
  
    constructor(
      bleId: string,
      uploadFileUriString: string,
      uploadFilePath: string
    ) {
      this.id = uuidv4() as UpgradeID;
  
      McuManager.createFileUpload(
        this.id,
        bleId,
        uploadFileUriString,
        uploadFilePath
      );
    }
  
    start = async (): Promise<void> => McuManager.runFileUpload(this.id);
    
    addListener: AddFileUploadListener = (
      eventType: any,
      listener: (...args: any[]) => void,
      context?: any
    ): EmitterSubscription => {
      return McuManagerEvents.addListener(
        eventType,
        ({ id, ...event }) => {
          if (id === this.id) {
            listener(event);
          }
        },
        context
      );
    };
  
    /**
     * Call to release native Upgrade class.
     * Failure to do so may result in memory leaks.
     */
    destroy = () => {
      McuManager.destroyFileUpload(this.id);
    };
  }
  
  export default FileUpload;
  