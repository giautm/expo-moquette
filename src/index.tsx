import { useEffect } from 'react';
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

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

export type Client = {
  address: string;
  port: number;
  id: string;
};

type ServerConfig = {
  host: string;
  port: string;
  wssPort: string;
  password?: string;
  username?: string;
  nettyMaxBytes?: number;
};

export type ServerStatus = {
  port: number;
  sslPort: number;
};

export function startServerAsync(config: ServerConfig): Promise<string> {
  return ExpoMoquette.startServerAsync(config);
}

export function getConnectedList(): Promise<any> {
  return ExpoMoquette.getConnectedClientsAsync();
}

export function restartServerAsync(): Promise<{ ok: Boolean }> {
  return ExpoMoquette.restartServerAsync();
}

export function stopServerAsync(): Promise<{ ok: Boolean }> {
  return ExpoMoquette.stopServerAsync();
}

export function getServerStatusAsync(): Promise<ServerStatus> {
  return ExpoMoquette.getServerStatusAsync();
}

export function isAvailable() {
  return Platform.OS === 'android';
}

const emitter = new NativeEventEmitter();

const ON_CONNECT = 'ON_CONNECT';
const ON_CONNECTION_LOST = 'ON_CONNECTION_LOST';
const ON_DISCONNECT = 'ON_DISCONNECT';
const ON_MESSAGE = 'ON_MESSAGE';

export function subscribeTopic(topic: string, callback: (event: any) => void) {
  return emitter.addListener(ON_MESSAGE, (event: any) => {
    if (event.topic === topic || event.topic.startsWith(topic)) {
      callback(event);
    }
  });
}

export function useClientEvent(handler: (event: any) => void) {
  const createEventHandler = (eventName: string) => {
    return (event: any) => {
      handler({ event, eventName });
    };
  };
  useEffect(() => {
    const subs = [
      emitter.addListener(ON_CONNECT, createEventHandler(ON_CONNECT)),
      emitter.addListener(ON_DISCONNECT, createEventHandler(ON_DISCONNECT)),
      emitter.addListener(
        ON_CONNECTION_LOST,
        createEventHandler(ON_CONNECTION_LOST)
      ),
    ];
    return () => {
      subs.forEach((sub) => sub.remove());
    };
  });
}
