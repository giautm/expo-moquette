import { NativeModules, Platform } from 'react-native';

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

export function startServerAsync(a: number, b: number): Promise<number> {
  return ExpoMoquette.startServerAsync(a, b);
}
