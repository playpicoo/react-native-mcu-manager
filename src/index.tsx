import { NativeModules } from 'react-native';

const { McuManager } = NativeModules;

import Upgrade, {
  FirmwareUpgradeState,
  UpgradeOptions,
  UpgradeMode,
  MemoryAlignment
} from './Upgrade';

import FileManager from './FileManager';

export const eraseImage = McuManager?.eraseImage as (
  bleId: string
) => Promise<void>;

export const confirmImage = McuManager?.confirmImage as (
  bleId: string
) => Promise<void>;

export { Upgrade, FirmwareUpgradeState, UpgradeOptions, UpgradeMode, MemoryAlignment, FileManager };
