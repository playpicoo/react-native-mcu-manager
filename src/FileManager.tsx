import {
  NativeModules,
  NativeEventEmitter,
  EmitterSubscription,
} from 'react-native';

import { v4 as uuidv4 } from 'uuid';

const { McuManager } = NativeModules;

const McuManagerEvents = new NativeEventEmitter(McuManager);

declare const FileManagerIdSymbol: unique symbol;
type FileManagerID = string & { [FileManagerIdSymbol]: never };

type AddFileUploadListener = {
  (
    eventType: 'fileUploadProgress',
    listener: ({ progress, bytesSent }: { progress: number, bytesSent: number }) => void,
    context?: any
  ): EmitterSubscription;
};

class FileManager {
  private id: FileManagerID;

  constructor(
    bleId: string
  ) {
    this.id = uuidv4() as FileManagerID;
    McuManager.createFileManager(this.id, bleId);
  }

  upload = async (sourceFileUriString: string, targetFilePath: string): Promise<void> => McuManager.uploadFile(this.id, sourceFileUriString, targetFilePath);
  write = async (data: number[], targetFilePath: string): Promise<void> => McuManager.writeFile(this.id, data, targetFilePath);
  stat = async (filePath: string): Promise<number> => McuManager.statFile(this.id, filePath);
  getSha256Hash = async (filePath: string): Promise<string | null> => McuManager.getFileHash(this.id, filePath);
  cancelUpload = () => { McuManager.cancelUpload(this.id) }

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
   * reset the file manager. this cancels running uploads and releases the connection.
   */
  reset = () => {
    McuManager.resetFileManager(this.id);
  }

  /**
   * release native FileManager
   */
  destroy = () => {
    McuManager.destroyFileManager(this.id);
  };
}

export default FileManager;
