import { useCallback, useEffect, useRef } from 'react';
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
  id: string;
  address: string;
  port: number;
};

export type ServerConfig = Partial<{
  host: string;
  port: number;
  wssPort: number;
  password: string;
  username: string;
  nettyMaxBytes: number;
}>;

export type ServerStatus = {
  port: number;
  sslPort: number;
};

export function startServerAsync(config: ServerConfig): Promise<string> {
  return ExpoMoquette.startServerAsync(config);
}

export function getConnectedList(): Promise<Client[]> {
  return ExpoMoquette.getConnectedClientsAsync();
}

export function restartServerAsync(): Promise<ServerStatus> {
  return ExpoMoquette.restartServerAsync();
}

export function stopServerAsync(): Promise<ServerStatus> {
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

type EventData = {
  eventName: 'ON_CONNECT' | 'ON_DISCONNECT' | 'ON_CONNECTION_LOST';
  event: {
    clientID: string;
    totalClients: number;
    username: string;
  };
};

export function useClientEvent(handler: (event: EventData) => void) {
  const refHandler = useRef(handler);
  refHandler.current = handler;
  const createEventHandler = useCallback(
    (eventName: EventData['eventName']) => {
      return (event: any) => {
        refHandler.current({ event, eventName });
      };
    },
    []
  );
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
  }, [createEventHandler]);
}
