import { NativeModules } from 'react-native';

const { McuManager } = NativeModules;

import Upgrade, {
  FirmwareUpgradeState,
  UpgradeOptions,
  UpgradeMode,
  MemoryAlignment
} from './Upgrade';

export const eraseImage = McuManager?.eraseImage as (
  bleId: string
) => Promise<void>;

export const confirmImage = McuManager?.confirmImage as (
  bleId: string
) => Promise<void>;

export const uploadFile = McuManager?.uploadFile as (
  bleId: string,
  source: string,
  target: string
) => Promise<void>

export const statFile = McuManager?.statFile as (
  bleId: string,
  path: string
) => Promise<void>

export { Upgrade, FirmwareUpgradeState, UpgradeOptions, UpgradeMode, MemoryAlignment };
