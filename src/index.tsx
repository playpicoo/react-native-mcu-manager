import { NativeModules } from 'react-native';

const { McuManager } = NativeModules;

import Upgrade, {
  FirmwareUpgradeState,
  UpgradeOptions,
  UpgradeMode,
  MemoryAlignment
} from './Upgrade';

import FileUpload from './FileUpload';

export const eraseImage = McuManager?.eraseImage as (
  bleId: string
) => Promise<void>;

export const confirmImage = McuManager?.confirmImage as (
  bleId: string
) => Promise<void>;

export const statFile = McuManager?.statFile as (
  bleId: string,
  path: string
) => Promise<number>

export { Upgrade, FirmwareUpgradeState, UpgradeOptions, UpgradeMode, MemoryAlignment, FileUpload };
