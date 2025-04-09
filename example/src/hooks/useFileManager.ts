import { useState, useEffect, useRef } from 'react';
import { FileManager } from '@playpicoo/react-native-mcu-manager';

type Progress = {
    percentage: number,
    bytesSent: number
}

const useFileManager = (
    bleId: string | null
) => {

    const [state, setState] = useState('');
    const [progress, setProgress] = useState<Progress | null>(null)
    const [fileSize, setFileSize] = useState<number | null>(null)
    const [fileHash, setFileHash] = useState<string | null>(null)

    const fileManagerRef = useRef<FileManager | null>(null);

    useEffect(() => {
        if (!bleId) {
            return () => null;
        }

        const manager = new FileManager(bleId)

        fileManagerRef.current = manager

        return function cleanup() {
            manager.destroy()
        };
    }, [bleId]);

    const upload = async (fileUri: string, filePath: string): Promise<void> => {
        console.log(`starting file upload, file=${fileUri}, bleId=${bleId}, path=${filePath}`);

        try {
            if (!fileManagerRef.current) {
                throw new Error("unable to start upload, are all parameters set?")
            }

            setState('Uploading')
            await fileManagerRef.current.upload(fileUri, filePath, (progress, bytesSent) => {
                setProgress({ percentage: progress, bytesSent: bytesSent })
            })
            setState('Ready')
        }
        catch (err: any) {
            setState(err.message);
        }
    };

    const write = async (fileData: string, filePath: string): Promise<void> => {
        if (filePath == null || fileData == null) return;

        try {
            if (!fileManagerRef.current) {
                throw new Error("unable to write to file, are all parameters set?")
            }

            const data = fileData.split(' ').map(s => parseInt(s))


            console.log(`write, data=${data}, path=${filePath}`);

            setState('Writing')
            await fileManagerRef.current.write(data, filePath)
            setState('Ready')
        }
        catch (err: any) {
            setState(err.message)
        }
    }

    const stat = async (filePath: string): Promise<void> => {
        console.log(`stat, bleId=${bleId}, path=${filePath}`);

        if (filePath == null) return;

        try {
            if (!fileManagerRef.current) {
                throw new Error("unable to start upload, are all parameters set?")
            }

            setState('Performing stat')
            const result = await fileManagerRef.current.stat(filePath)
            setFileSize(result);
            setState('Ready')
        }
        catch (err: any) {
            setState(err.message);
        }
    };

    const hash = async (filePath: string, n: number = 1): Promise<void> => {
        console.log(`hash, bleId=${bleId}, path=${filePath}, n=${n}`);

        if (filePath == null) return;

        try {
            if (!fileManagerRef.current) {
                throw new Error("unable to get file hash, are all parameters set?")
            }

            let avg = 0

            for (let i = 0; i < n; i++) {
                const t0 = performance.now()
                setState(`Getting hash: ${i}`)
                const result = await fileManagerRef.current.sha256(filePath)
                const t1 = performance.now()

                const dt = t1 - t0

                avg = ((avg * i) + dt) / (i + 1)

                console.log(`hash, received hash=${result}, dt=${dt}`);

                setFileHash(`${result}`);
            }

            setState(`Ready`)

            console.log(`hash, avg dt=${avg}`);
        }
        catch (err: any) {
            setState(err.message);
        }
    };

    return {
        uploadFile: upload,
        writeFile: write,
        statFile: stat,
        getFileHash: hash,
        fileHash,
        fileManagerState: state,
        fileUploadProgress: progress,
        fileSize
    };
};

function sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

export default useFileManager;
