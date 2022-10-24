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
import io.moquette.broker.security.IAuthenticator;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;

import static java.util.Arrays.asList;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

@ReactModule(name = ExpoMoquetteModule.NAME)
public class ExpoMoquetteModule extends ReactContextBaseJavaModule {
  public static final String NAME = "ExpoMoquette";
  public static final int NETTY_MAX_BYTES = 100 * 1024 - 100;

  private Server mServer = null;
  private List<? extends InterceptHandler> userHandlers;

  public ExpoMoquetteModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mServer = new Server();
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
      String username = initialConfig.getString("username");
      String password = initialConfig.getString("password");
      int maxBytes = initialConfig.hasKey("nettyMaxBytes")
        ? Math.max(initialConfig.getInt("nettyMaxBytes"), NETTY_MAX_BYTES)
        : NETTY_MAX_BYTES;

      host = host.isEmpty() ? "0.0.0.0" : host;
      port = port.isEmpty() ? "1883" : port;
      wssPort = wssPort.isEmpty() ? "8080" : wssPort;

      final IConfig config = new MemoryConfig(new Properties());
      config.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, wssPort);
      config.setProperty(BrokerConstants.HOST_PROPERTY_NAME, host);
      config.setProperty(BrokerConstants.PORT_PROPERTY_NAME, port);
      config.setProperty(BrokerConstants.NETTY_MAX_BYTES_PROPERTY_NAME, String.valueOf(maxBytes));

      userHandlers = asList(new PublisherListener(mServer, this.getReactApplicationContext()));

      // Authentication
      IAuthenticator authenticator = null;
      if (username != null && password != null && username.length() > 0 && password.length() > 0) {
        config.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "false");
        config.setProperty(BrokerConstants.AUTHENTICATOR_CLASS_NAME,
          MqttEmbeddedBrokerUserAuthenticator.class.getName());
        authenticator = new MqttEmbeddedBrokerUserAuthenticator(username, password.getBytes());
      } else {
        config.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "true");
      }

      mServer.startServer(config, userHandlers, null, authenticator, null);
      promise.resolve(this.serverInfo());
    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  @ReactMethod
  public void stopServerAsync(Promise promise) {
    try {
      mServer.stopServer();
      promise.resolve(this.serverInfo());
    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  @ReactMethod
  public void restartServerAsync(Promise promise) {
    try {
      mServer.stopServer();
      mServer.startServer();
      promise.resolve(this.serverInfo());
    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  @ReactMethod
  public void getConnectedClientsAsync(Promise promise) {
    try {
      Collection<ClientDescriptor> list = mServer.listConnectedClients();
      WritableArray result = Arguments.createArray();
      for (ClientDescriptor client : list) {
        WritableMap info = Arguments.createMap();
        info.putString("id", client.getClientID());
        info.putString("address", client.getAddress());
        info.putInt("port", client.getPort());

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
      promise.resolve(this.serverInfo());
    } catch (Exception e) {
      promise.reject("ERR_MOQUETTE", e);
    }
  }

  private ReadableMap serverInfo() {
    WritableMap result = Arguments.createMap();
    result.putString("port", String.valueOf(mServer.getPort()));
    result.putString("sslPort", String.valueOf(mServer.getSslPort()));
    return result;
  }

  /**
   * This class is used to listen to messages on topics and manage them.
   */
  class PublisherListener extends AbstractInterceptHandler {
    public static final String ON_MESSAGE = "ON_MESSAGE";
    public static final String ON_CONNECT = "ON_CONNECT";
    public static final String ON_DISCONNECT = "ON_DISCONNECT";
    public static final String ON_CONNECTION_LOST = "ON_CONNECTION_LOST";

    private Server server;
    private ReactContext reactContext;
    private int mClientCount = 0;

    PublisherListener(Server server, ReactContext context) {
      this.server = server;
      this.reactContext = context;
      this.mClientCount = 0;
    }

    private void sendEvent(
      String eventName,
      @Nullable WritableMap params) {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
    }

    @Override
    public String getID() {
      return "JSPublisherListener";
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
      mClientCount++;
      WritableMap payloadMap = Arguments.createMap();
      payloadMap.putString("clientID", msg.getClientID());
      payloadMap.putString("username", msg.getUsername());
      payloadMap.putInt("totalClients", mClientCount);
      sendEvent(ON_CONNECT, payloadMap);
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
      mClientCount--;
      WritableMap payloadMap = Arguments.createMap();
      payloadMap.putString("clientID", msg.getClientID());
      payloadMap.putString("username", msg.getUsername());
      payloadMap.putInt("totalClients", mClientCount);
      sendEvent(ON_DISCONNECT, payloadMap);
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
      WritableMap payloadMap = Arguments.createMap();
      payloadMap.putString("clientID", msg.getClientID());
      payloadMap.putString("username", msg.getUsername());
      payloadMap.putInt("totalClients", mClientCount);
      sendEvent(ON_CONNECTION_LOST, payloadMap);
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
      try {
        String topic = msg.getTopicName();
        String payload = new String(msg.getPayload().array(), "UTF-8");
        if (!payload.isEmpty()) {
          WritableMap payloadMap = Arguments.createMap();
          payloadMap.putString("message", payload);
          payloadMap.putString("topic", topic);
          sendEvent(ON_MESSAGE, payloadMap);
        }
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      } finally {
        super.onPublish(msg);
      }
    }
  }
}
