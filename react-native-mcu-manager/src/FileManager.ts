import ReactNativeMcuManager from './ReactNativeMcuManagerModule';

declare const FileManagerIdSymbol: unique symbol;
type FileManagerID = string & { [FileManagerIdSymbol]: never };

class FileManager {
  private id: FileManagerID;

  constructor(
    bleId: string
  ) {
    this.id = String(
      Math.floor(100000000 + Math.random() * 900000000)
    ) as FileManagerID;

    ReactNativeMcuManager.createFileManager(this.id, bleId)
  }

  upload = async (sourceFileUriString: string, targetFilePath: string, onProgress?: (progress: number, bytesSent: number) => void): Promise<void> =>
    ReactNativeMcuManager.uploadFile(this.id, sourceFileUriString, targetFilePath, (id: string, progress: number, bytesSent: number) => {
      onProgress?.(progress, bytesSent);
    })
  write = async (data: number[], targetFilePath: string): Promise<void> =>
    ReactNativeMcuManager.writeFile(this.id, data, targetFilePath);
  stat = async (filePath: string): Promise<number> =>
    ReactNativeMcuManager.statFile(this.id, filePath);
  sha256 = async (filePath: string): Promise<string | null> =>
    ReactNativeMcuManager.getFileSha256Hash(this.id, filePath);

  /**
   * release native FileManager
   */
  destroy = () => {
    ReactNativeMcuManager.destroyFileManager(this.id);
  };
}

export default FileManager;
