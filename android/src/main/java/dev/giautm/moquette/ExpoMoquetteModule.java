package dev.giautm.moquette;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import io.moquette.BrokerConstants;
import io.moquette.broker.ClientDescriptor;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.broker.Server;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.buffer.ByteBuf;

import static java.util.Arrays.asList;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@ReactModule(name = ExpoMoquetteModule.NAME)
public class ExpoMoquetteModule extends ReactContextBaseJavaModule {
  public static final String NAME = "ExpoMoquette";


  private Server server = null;
  private List<? extends InterceptHandler> userHandlers;

  public ExpoMoquetteModule(ReactApplicationContext reactContext) {
    super(reactContext);
    server = new Server();
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void startServerAsync(ReadableMap initialConfig, Promise promise) {
    try {

      String host = initialConfig.getString("host");
      String port = initialConfig.getString("port");
      String wssPort = initialConfig.getString("wssPort");

      host = host.isEmpty() ? "0.0.0.0" : host;
      port = port.isEmpty() ? "1883" : port;
      wssPort = wssPort.isEmpty() ? "8080" : wssPort;

      final IConfig config = new MemoryConfig(new Properties());
      config.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, wssPort);
      config.setProperty(BrokerConstants.HOST_PROPERTY_NAME, host);
      config.setProperty(BrokerConstants.PORT_PROPERTY_NAME, port);

      userHandlers = asList(new PublisherListener(server, this.getReactApplicationContext()));

      server.startServer(config, userHandlers);

      WritableMap result = Arguments.createMap();
      result.putString("port", String.valueOf(server.getPort()));

      promise.resolve(result);
    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  @ReactMethod
  public void stopServerAsync(Promise promise) {
    try {
      server.stopServer();
      WritableMap result = Arguments.createMap();
      result.putBoolean("OK", true);
      promise.resolve(result);
    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  @ReactMethod
  public void restartServerAsync(Promise promise) {
    try {

      server.stopServer();
      server.startServer();

      WritableMap result = Arguments.createMap();
      result.putBoolean("OK", true);
      promise.resolve(result);

    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  @ReactMethod
  public void getConnectedClientsAsync(Promise promise) {
    try {
      Collection<ClientDescriptor> list = server.listConnectedClients();
      WritableArray result = Arguments.createArray();
      for (ClientDescriptor client : list) {
        WritableMap info = Arguments.createMap();
        info.putString("address", client.getAddress());
        info.putInt("port", client.getPort());
        info.putString("id", client.getClientID());

        result.pushMap(info);

      }
      promise.resolve(result);
    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  @ReactMethod
  public void getServerStatusAsync(Promise promise) {
    try {
      WritableMap result = Arguments.createMap();
      result.putInt("port", server.getPort());
      result.putInt("sslPort", server.getSslPort());

      promise.resolve(result);

    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  /**
   * This class is used to listen to messages on topics and manage them.
   */
  class PublisherListener extends AbstractInterceptHandler {
    public static final String ON_MESSAGE = "ON_MESSAGE";

    private Server server;
    private ReactContext reactContext;

    PublisherListener(Server server, ReactContext context) {
      this.server = server;
      this.reactContext = context;
    }

    private void sendEvent(
      String eventName,
      @Nullable WritableMap params) {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
      String topic = msg.getTopicName();

      ByteBuf buffer = msg.getPayload();
      byte[] bytes = new byte[buffer.readableBytes()];
      buffer.readBytes(bytes);
      String payload = null;
      try {
        payload = new String(bytes, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      if (!payload.isEmpty()) {
        WritableMap payloadMap = Arguments.createMap();
        payloadMap.putString("message", payload);
        payloadMap.putString("topic", topic);
        sendEvent(ON_MESSAGE, payloadMap);
      }
    }

    @Override
    public String getID() {
      return "JSPublisherListener";
    }
  }
}
