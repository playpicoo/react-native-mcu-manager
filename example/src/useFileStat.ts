import { useState, useEffect } from 'react';

import { statFile } from '@playerdata/react-native-mcu-manager';

const useFileStat = (
    bleId: string | null,
    fileUri: string | null
) => {

    const [state, setState] = useState('');

    useEffect(() => {
        if (!bleId || !fileUri) {
            return () => null;
        }

        return function cleanup() {
        };
    }, [bleId, fileUri]);

    const startStat = async (): Promise<void> => {
        console.log(`starting file stat, file=${fileUri}, bleId=${bleId}`);
        
        if (bleId === null) return
        
        try {
            await statFile(bleId, '/ext/file.bin')
        } catch (ex: any) {
            setState(ex.message);
        }
    };

    return { startStat, fileStatState: state };
};

export default useFileStat;
