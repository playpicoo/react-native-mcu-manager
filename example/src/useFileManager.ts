import { useState, useEffect, useRef } from 'react';

import { FileManager } from '@playerdata/react-native-mcu-manager';

const useFileManager = (
    bleId: string | null,
    fileUri: string | null,
    filePath: string | null
) => {

    const [state, setState] = useState('');
    const [progress, setProgress] = useState<number>(0);
    const [fileSize, setFileSize] = useState<number>(-1);

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

        if(fileUri == null || filePath == null) return;

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

    const stat = async (): Promise<void> => {
        console.log(`stat, bleId=${bleId}, path=${filePath}`);

        if(filePath == null) return;

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


    return { uploadFile: upload, statFile:stat, fileManagerState: state, fileUploadProgress: progress, fileSize };
};

export default useFileManager;
