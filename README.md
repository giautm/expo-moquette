# expo-moquette

MQTT Broker for Expo application

## Installation

```sh
npm install expo-moquette
```

## Usage

```js
import { startServerAsync } from "expo-moquette";

// ...

const result = await startServerAsync({
  host: '0.0.0.0',
  port: 1883,
  wsPort: 8080,
  username: 'admin',
  password: 'admin',
});
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
