#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(ExpoMoquette, NSObject)

RCT_EXTERN_METHOD(startServerAsync:(float)a withB:(float)b
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

@end
