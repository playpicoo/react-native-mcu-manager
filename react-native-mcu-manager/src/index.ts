import McuManagerModule from './ReactNativeMcuManagerModule'
import type { FirmwareUpgradeState, UpgradeOptions } from './Upgrade'
import Upgrade, { UpgradeMode, UpgradeFileType, MemoryAlignment } from './Upgrade'

import FileManager from './FileManager';

export const eraseImage = McuManagerModule?.eraseImage as (
  bleId: string
) => Promise<void>;

export const confirmImage = McuManagerModule?.confirmImage as (
  bleId: string
) => Promise<void>;

export const resetDevice = McuManagerModule?.resetDevice as (
  bleId: string
) => Promise<void>;

export { Upgrade, UpgradeMode, FileManager, UpgradeFileType, MemoryAlignment }
export type { FirmwareUpgradeState, UpgradeOptions } 
