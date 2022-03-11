import { NativeModules, Platform } from 'react-native';
export type Client = {
  address: string;
  port: number;
  id: string;
};
const LINKING_ERROR =
  `The package 'expo-moquette' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const ExpoMoquette = NativeModules.ExpoMoquette
  ? NativeModules.ExpoMoquette
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

type ServerConfig = {
  host: string;
  port: string;
  wssPort: string;
};
export function startServerAsync(config: ServerConfig): Promise<string> {
  return ExpoMoquette.startServerAsync(config);
}

export function getConnectedList(): Promise<any> {
  return ExpoMoquette.getListConnectedClients();
}

export function restartServerAsync(): Promise<{ ok: Boolean }> {
  return ExpoMoquette.restartServerAsync();
}

export function stopServerAsync(): Promise<{ ok: Boolean }> {
  return ExpoMoquette.stopServerAsync();
}

export type ServerStatus = {
  port: number;
  sslPort: number;
};

export function getServerStatusAsync(): Promise<ServerStatus> {
  return ExpoMoquette.getServerStatusAsync();
}

export function isAvailable() {
  return Platform.OS === 'android';
}
