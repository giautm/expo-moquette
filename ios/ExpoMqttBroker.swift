@objc(ExpoMoquette)
class ExpoMoquette: NSObject {

    @objc(startServerAsync:withB:withResolver:withRejecter:)
    func startServerAsync(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
        resolve(a*b)
    }
}
