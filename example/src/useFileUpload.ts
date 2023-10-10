import { useState, useEffect, useRef } from 'react';

import { Upgrade, UpgradeMode, uploadFile } from '@playerdata/react-native-mcu-manager';

const useFileUpload = (
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

    const startUpload = async (): Promise<void> => {
        console.log(`starting file upload, file=${fileUri}, bleId=${bleId}`);
        
        if (bleId === null || fileUri === null) return
        
        try {
            await uploadFile(bleId, fileUri, '/ext/file.bin')
        } catch (ex: any) {
            setState(ex.message);
        }
    };

    return { startUpload, fileUploadState: state };
};

export default useFileUpload;
