import {
    UpgradeFileType,
    UpgradeMode,
} from '@playpicoo/react-native-mcu-manager'

import React, { useState } from 'react';
import {
    Button,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    View,
} from 'react-native';

import useFilePicker from '../hooks/useFilePicker';
import { useSelectedDevice } from '../context/selectedDevice';
import useFileManager from 'src/hooks/useFileManager';

const styles = StyleSheet.create({
    root: {
        padding: 16,
    },

    block: {
        marginBottom: 16,
    },

    input: {
        alignSelf: 'stretch',
        height: 50,
        backgroundColor: 'white',
        borderRadius: 15,
        paddingHorizontal: 22,
    }
});

const Update = () => {
    const { selectedDevice } = useSelectedDevice();

    const { selectedFile, filePickerError, pickFile } = useFilePicker();
    const [uploadTarget, setUploadTarget] = useState<string | null>("/ext/file.bin")
    const [infoFilePath, setInfoFilePath] = useState<string | null>("/ext/file.bin")
    const [writeFilePath, setWriteFilePath] = useState<string | null>("/ext/data.bin")
    const [writeFileData, setWriteFileData] = useState<string | null>("")

    const {
        uploadFile,
        getFileHash,
        statFile,
        writeFile,
        fileHash,
        fileManagerState,
        fileSize,
        fileUploadProgress
    } = useFileManager(
        selectedDevice?.deviceId || null
    )

    const onUploadPressed = async () => {
        if (selectedFile?.uri && uploadTarget) {
            await uploadFile(selectedFile?.uri, uploadTarget)
        }
    }

    const onGetHashPressed = async () => {
        if (infoFilePath) {
            await getFileHash(infoFilePath)
        }
    }

    const onWritePressed = async () => {
        if (writeFilePath && writeFileData) {
            await writeFile(writeFileData, writeFilePath)
        }
    }

    const onGetSizePressed = async () => {
        if (infoFilePath) {
            await statFile(infoFilePath)
        }
    }

    return (
        <SafeAreaView>
            <ScrollView contentContainerStyle={styles.root}>
                <Text style={styles.block}>Device</Text>

                <View style={styles.block}>
                    {selectedDevice?.deviceId && (
                        <>
                            <Text>Selected:</Text>
                            <Text>{selectedDevice.deviceName}</Text>
                        </>
                    )}
                </View>

                <Text style={styles.block}>Upload File</Text>

                <View style={styles.block}>
                    <Text>
                        {selectedFile?.name} {filePickerError}
                    </Text>
                    <Button onPress={() => pickFile()} title="Pick File" />
                    <TextInput style={styles.input} autoCapitalize='none' autoComplete='off' autoCorrect={false} placeholder='Target Path' value={uploadTarget ?? undefined} onChangeText={setUploadTarget} />
                    <Button onPress={onUploadPressed} title="Upload" disabled={!selectedFile || !uploadTarget || !selectedDevice} />
                </View>

                <View style={styles.block}>
                    <Text>Upload progress: {fileUploadProgress?.percentage} : {fileUploadProgress?.bytesSent}</Text>
                    <Text>State: {fileManagerState}</Text>
                </View>

                <Text style={styles.block}>File Info</Text>

                <View style={styles.block}>
                    <TextInput style={styles.input} autoCapitalize='none' autoComplete='off' autoCorrect={false} placeholder='File Path' value={infoFilePath ?? undefined} onChangeText={setInfoFilePath} />
                    <Button onPress={onGetSizePressed} title="Get Size" disabled={!infoFilePath || !selectedDevice} />
                    <Button onPress={onGetHashPressed} title="Get Hash" disabled={!infoFilePath || !selectedDevice} />
                </View>

                <View style={styles.block}>
                    <Text>Size: {fileSize}</Text>
                    <Text>SHA256 hash: {fileHash}</Text>
                </View>

                <Text style={styles.block}>Write File</Text>
                <View style={styles.block}>
                    <TextInput style={styles.input} autoCapitalize='none' autoComplete='off' autoCorrect={false} placeholder='File Path' value={writeFilePath ?? undefined} onChangeText={setWriteFilePath} />
                    <TextInput style={styles.input} autoCapitalize='none' autoComplete='off' autoCorrect={false} placeholder='File Data' value={writeFileData ?? undefined} onChangeText={setWriteFileData} />
                    <Button onPress={onWritePressed} title="Write" disabled={!writeFileData || !writeFilePath || !selectedDevice} />
                </View>


            </ScrollView>
        </SafeAreaView>
    );
}

export default Update;
