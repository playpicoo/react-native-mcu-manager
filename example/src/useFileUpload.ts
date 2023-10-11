import { useState, useEffect, useRef } from 'react';

import { FileUpload } from '@playerdata/react-native-mcu-manager';

const useFileUpload = (
    bleId: string | null,
    fileUri: string | null,
    filePath: string | null
) => {

    const [state, setState] = useState('');
    const [progress, setProgress] = useState<number>(0);

    const fileUploadRef = useRef<FileUpload>();

    useEffect(() => {
        if (!bleId || !fileUri || !filePath) {
            return () => null;
        }

        const upload = new FileUpload(bleId, fileUri, filePath)

        fileUploadRef.current = upload

        const fileUploadProgressListener = upload.addListener('fileUploadProgress', ({ progress }) => {
            setProgress(progress)
        })

        return function cleanup() {
            fileUploadProgressListener.remove()
            upload.destroy()
        };
    }, [bleId, fileUri, filePath]);

    const startUpload = async (): Promise<void> => {
        console.log(`starting file upload, file=${fileUri}, bleId=${bleId}, path=${filePath}`);

        try {
            if (!fileUploadRef.current) {
                throw new Error("unable to start upload, are all parameters set?")
            }

            await fileUploadRef.current.start()
        }
        catch (err: any) {
            setState(err.message);
        }
    };

    return { startUpload, fileUploadState: state, fileUploadProgress: progress };
};

export default useFileUpload;
