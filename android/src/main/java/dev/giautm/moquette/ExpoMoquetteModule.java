package dev.giautm.moquette;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.broker.Server;
import java.util.Properties;

@ReactModule(name = ExpoMoquetteModule.NAME)
public class ExpoMoquetteModule extends ReactContextBaseJavaModule {
    public static final String NAME = "ExpoMoquette";

    private Server server = null;

    public ExpoMoquetteModule(ReactApplicationContext reactContext) {
        super(reactContext);
        server = new Server();
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }


    // Example method
    // See https://reactnative.dev/docs/native-modules-android
    @ReactMethod
    public void startServerAsync(int a, int b, Promise promise) {
        try {
            final IConfig config = new MemoryConfig(new Properties());
            server.startServer(config);

            promise.resolve(a * b);
        } catch (Exception e) {
            promise.reject("start server failed", e);
        }
    }
}
