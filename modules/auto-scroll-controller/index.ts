import { requireNativeModule } from 'expo';

export type Direction = 'DOWN' | 'UP' | 'LEFT' | 'RIGHT';
export type Config = {
  delaySec: number;
  areaPct: number;
  speedX10: number;
  direction: Direction;
};
export type Side = 'RIGHT' | 'LEFT';
export type Custom = { size: number; alpha: number; peek: number; side: Side };

const Native = requireNativeModule('AutoScrollController');

export const isServiceEnabled = (): boolean => Native.isServiceEnabled();
export const isRunning = (): boolean => Native.isRunning();
export const openAccessibilitySettings = (): void => Native.openAccessibilitySettings();
export const getConfig = (): Config => Native.getConfig();
export const setConfig = (c: Config): void =>
  Native.setConfig(c.delaySec, c.areaPct, c.speedX10, c.direction);
export const getCustomize = (): Custom => Native.getCustomize();
export const setCustomize = (c: Custom): void =>
  Native.setCustomize(c.size, c.alpha, c.peek, c.side);
export const showController = (): void => Native.showController();
export const start = (): void => Native.start();
export const stop = (): void => Native.stop();
