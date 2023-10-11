import { useState, useEffect } from 'react';

import { statFile } from '@playerdata/react-native-mcu-manager';

const useFileStat = (
    bleId: string | null,
    fileUri: string | null
) => {

    const [state, setState] = useState('');
    const [fileSize, setFileSize] = useState<number>(0)

    useEffect(() => {
        if (!bleId || !fileUri) {
            return () => null;
        }

        return function cleanup() {
        };
    }, [bleId, fileUri]);

    const startStat = async (): Promise<void> => {
        console.log(`starting file stat, file=${fileUri}, bleId=${bleId}`);
        
        if (bleId === null || fileUri === null) return
        
        try {
            const size = await statFile(bleId, fileUri)
            setFileSize(size)
        } catch (ex: any) {
            setState(ex.message);
        }
    };

    return { startStat, fileStatState: state, fileSize };
};

export default useFileStat;
