import * as React from 'react';

import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import { startServerAsync, getConnectedList, Client } from 'expo-mqtt-broker';
import { useCallback } from 'react';

export default function App() {
  const [port, setPort] = React.useState<string | undefined>();
  const [clientList, setClientList] = React.useState<Client[]>([]);

  React.useEffect(() => {
    startServerAsync({ host: '0.0.0.0', port: '1883', wssPort: '8080' }).then(
      (r) => {
        // @ts-ignore
        setPort(r?.port);
      }
    );
  }, []);

  const handleGetClient = useCallback(async () => {
    const list = await getConnectedList();
    console.log(list);
    setClientList(list);
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: Listen is port {port}</Text>
      <TouchableOpacity onPress={handleGetClient}>
        <Text>Get Clients List</Text>
      </TouchableOpacity>
      {clientList.map(({ id, address, port: _port }) => (
        <View>
          <Text>{`${id} - ${address}:${_port}`}</Text>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
