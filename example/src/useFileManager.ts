import { useState, useEffect, useRef } from 'react';

import { FileManager } from '@playpicoo/react-native-mcu-manager';

const useFileManager = (
    bleId: string | null,
    fileUri: string | null,
    filePath: string | null,
    fileData: string | null
) => {

    const [state, setState] = useState('');
    const [progress, setProgress] = useState<number>(0);
    const [fileSize, setFileSize] = useState<number>(-1);
    const [fileHash, setFileHash] = useState<string | null>("")

    const fileManagerRef = useRef<FileManager>();

    useEffect(() => {
        if (!bleId) {
            return () => null;
        }

        const manager = new FileManager(bleId)

        fileManagerRef.current = manager

        const fileUploadProgressListener = manager.addListener('fileUploadProgress', ({ progress }) => {
            setProgress(progress)
        })

        return function cleanup() {
            fileUploadProgressListener.remove()
            manager.destroy()
        };
    }, [bleId]);

    const upload = async (): Promise<void> => {
        console.log(`starting file upload, file=${fileUri}, bleId=${bleId}, path=${filePath}`);

        if (fileUri == null || filePath == null) return;

        try {
            if (!fileManagerRef.current) {
                throw new Error("unable to start upload, are all parameters set?")
            }

            await fileManagerRef.current.upload(fileUri, filePath)
        }
        catch (err: any) {
            setState(err.message);
        }
    };

    const write = async (): Promise<void> => {
        if (filePath == null || fileData == null) return;

        try {
            if (!fileManagerRef.current) {
                throw new Error("unable to write to file, are all parameters set?")
            }

            const data = fileData.split(' ').map(s => parseInt(s))

            console.log(`write, data=${data}, path=${filePath}`);

            await fileManagerRef.current.write(data, filePath)
        }
        catch (err: any) {
            setState(err.message)
        }
    }

    const stat = async (): Promise<void> => {
        console.log(`stat, bleId=${bleId}, path=${filePath}`);

        if (filePath == null) return;

        try {
            if (!fileManagerRef.current) {
                throw new Error("unable to start upload, are all parameters set?")
            }

            const result = await fileManagerRef.current.stat(filePath)
            setFileSize(result);
        }
        catch (err: any) {
            setState(err.message);
        }
    };

    const hash = async (): Promise<void> => {
        console.log(`hash, bleId=${bleId}, path=${filePath}`);

        if (filePath == null) return;

        try {
            if (!fileManagerRef.current) {
                throw new Error("unable to start upload, are all parameters set?")
            }

            const result = await fileManagerRef.current.getSha256Hash(filePath)
            console.log(`hash, received hash=${result}`);

            setFileHash(result);
        }
        catch (err: any) {
            setState(err.message);
        }
    };

    return { uploadFile: upload, writeFile: write, statFile: stat, getFileHash: hash, fileHash, fileManagerState: state, fileUploadProgress: progress, fileSize };
};

export default useFileManager;
