package dev.giautm.moquette;



import java.util.Arrays;
import javax.annotation.Nullable;
import io.moquette.broker.security.IAuthenticator;


public class MqttEmbeddedBrokerUserAuthenticator implements IAuthenticator {
  final String username;
  final byte[] password;

  public MqttEmbeddedBrokerUserAuthenticator(String username, byte[] password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public boolean checkValid(@Nullable String clientId, @Nullable String username, byte[] password) {
    return this.username.equals(username) && Arrays.equals(this.password, password);
  }
}

