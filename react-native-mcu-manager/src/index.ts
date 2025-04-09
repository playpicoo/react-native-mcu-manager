import McuManagerModule from './ReactNativeMcuManagerModule'
import type { FirmwareUpgradeState, MemoryAlignment, UpgradeOptions } from './Upgrade'
import Upgrade, { UpgradeMode, UpgradeFileType } from './Upgrade'

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

export { Upgrade, UpgradeMode, FileManager, UpgradeFileType }
export type { FirmwareUpgradeState, MemoryAlignment, UpgradeOptions } 
