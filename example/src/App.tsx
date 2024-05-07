import React, { useState } from 'react';

import {
  Button,
  FlatList,
  Image,
  Modal,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { UpgradeMode, reset } from '@playpicoo/react-native-mcu-manager';

import useBluetoothDevices from './useBluetoothDevices';
import useFilePicker from './useFilePicker';
import useFirmwareUpdate from './useFirmwareUpdate';
import { MemoryAlignment } from '../../src/Upgrade';
import useFileManager from './useFileManager';

const styles = StyleSheet.create({
  root: {
    padding: 16,
  },

  block: {
    marginBottom: 16,
  },
  input: {
    backgroundColor: 'white',
    color: 'black'
  },
  list: {
    backgroundColor: 'black',
    padding: 16,
  },
  text: {
    color:'red'
  }
});

export default function App() {
  const [devicesListVisible, setDevicesListVisible] = useState(false);
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
  const [selectedDeviceName, setSelectedDeviceName] = useState<string | null>(
    null
  );

  const [uploadFilePath, onChangeUploadFilePath] = useState<string>("/ext/file.bin");
  const [statFilePath, onChangeStatFilePath] = useState<string>("/ext/file.bin");
  const [writeFileData, onChangeWriteFileData] = useState<string>("");

  const [upgradeMode, setUpgradeMode] = useState<UpgradeMode | undefined>(
    undefined
  );
  const [windowUploadCapacity, setWindowUploadCapacity] = useState<number | undefined>(
    undefined
  );
  const [memoryAlignment, setMemoryAlignment] = useState<MemoryAlignment | undefined>(
    undefined
  );
  const [requestConnectionPriority, setRquestConnectionPriority] = useState<boolean | undefined>(
    undefined
  );


  const { devices, error: scanError } = useBluetoothDevices();
  const { selectedFile, filePickerError, pickFile } = useFilePicker();
  const { cancelUpdate, runUpdate, progress, state } = useFirmwareUpdate(
    selectedDeviceId,
    selectedFile?.uri || null,
    upgradeMode
  );

  const { uploadFile, writeFile, statFile, getFileHash, fileSize, fileHash, fileUploadProgress, fileManagerState } = useFileManager(selectedDeviceId, selectedFile?.uri ?? null, uploadFilePath, writeFileData)

  const resetDevice = async () => {
    console.log(`resetting device ${selectedDeviceId}`);
    if (selectedDeviceId != null)
      await reset(selectedDeviceId)
  }

  return (
    <SafeAreaView>
      <ScrollView contentContainerStyle={styles.root}>
        <Text style={styles.block}>Step 1 - Select Device to Update</Text>

        <View style={styles.block}>
          {selectedDeviceId && (
            <>
              <Text>Selected:</Text>
              <Text>{selectedDeviceName}</Text>
            </>
          )}
          <Button
            onPress={() => setDevicesListVisible(true)}
            title="Select Device"
          />
        </View>

        <Modal visible={devicesListVisible}>
          <FlatList
            contentContainerStyle={styles.list}
            data={devices}
            keyExtractor={({ id }) => id}
            renderItem={({ item }) => (
              <View>
                <Text style={styles.text}>{item.name || item.id}</Text>

                <Button
                  title="Select"
                  onPress={() => {
                    setSelectedDeviceId(item.id);
                    setSelectedDeviceName(item.name);
                    setDevicesListVisible(false);
                  }}
                />
              </View>
            )}
            ListHeaderComponent={() => <Text>{scanError}</Text>}
          />
        </Modal>

        <Text style={styles.block}>Step 2 - Select Update File</Text>

        <View style={styles.block}>
          <Text>
            {selectedFile?.name} {filePickerError}
          </Text>
          <Button onPress={() => pickFile()} title="Pick File" />
        </View>

        <Text style={styles.block}>Step 3 - Upgrade Mode</Text>

        <View style={styles.block}>
          <Button
            disabled={upgradeMode === undefined}
            title="undefined"
            onPress={() => setUpgradeMode(undefined)}
          />
          <Button
            disabled={upgradeMode === UpgradeMode.TEST_AND_CONFIRM}
            title="TEST_AND_CONFIRM"
            onPress={() => setUpgradeMode(UpgradeMode.TEST_AND_CONFIRM)}
          />
          <Button
            disabled={upgradeMode === UpgradeMode.CONFIRM_ONLY}
            title="CONFIRM_ONLY"
            onPress={() => setUpgradeMode(UpgradeMode.CONFIRM_ONLY)}
          />
          <Button
            disabled={upgradeMode === UpgradeMode.TEST_ONLY}
            title="TEST_ONLY"
            onPress={() => setUpgradeMode(UpgradeMode.TEST_ONLY)}
          />
        </View>

        <Text style={styles.block}>Step 4 - Update</Text>

        <View style={styles.block}>
          <Text>Update State / Progress:</Text>
          <Text>
            {state}: {progress}
          </Text>

          <Button
            disabled={!selectedFile || !selectedDeviceId}
            onPress={() => selectedFile && runUpdate()}
            title="Start Update"
          />

          <Button
            disabled={!selectedFile || !selectedDeviceId}
            onPress={() => cancelUpdate()}
            title="Cancel Update"
          />
        </View>

        <Text>File Manager</Text>
        <Text>State: {fileManagerState}</Text>
        <View style={styles.block}>
          <TextInput style={[styles.block, styles.input]} autoComplete='off' autoCorrect={false} value={uploadFilePath} onChangeText={onChangeUploadFilePath} />
          <TextInput style={[styles.block, styles.input]} autoComplete='off' autoCorrect={false} value={writeFileData} onChangeText={onChangeWriteFileData} />
          <Text>Progress:</Text>
          <Text>
            {fileUploadProgress}
          </Text>
          <Button
            disabled={!selectedFile || !selectedDeviceId}
            onPress={() => uploadFile()}
            title="Upload File"
          />
          <Button
            onPress={() => writeFile()}
            title="Write File"
          />
        </View>

        <TextInput style={[styles.block, styles.input]} autoComplete='off' autoCorrect={false} value={statFilePath} onChangeText={onChangeStatFilePath} />
        <View style={styles.block}>
          <Button
            disabled={!selectedDeviceId}
            onPress={() => statFile()}
            title="Stat File"
          />
          <Text>Size: {fileSize}</Text>
        </View>

        <View style={styles.block}>
          <Button
            disabled={!selectedDeviceId}
            onPress={() => getFileHash()}
            title="Get Hash"
          />
          <Text>SHA256 hash: {fileHash}</Text>
        </View>

        <View style={styles.block}>
          <Button
            onPress={resetDevice}
            title='Reset'>
          </Button>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}
