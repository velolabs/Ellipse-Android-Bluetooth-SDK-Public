import React, { Component } from 'react'
import {
  StyleSheet,
  Text,
  View,
  NativeModules,
  NativeEventEmitter,
  Button,
  PermissionsAndroid,
  Alert
} from 'react-native'

const API_TOKEN = ''
const LOCK_MAC_ID = ''
var connected = false;
var isLocked = false;

// BluetoothManager
const BluetoothManagerModule = NativeModules.BluetoothManager
const BluetoothManagerEmitter = new NativeEventEmitter(BluetoothManagerModule)

// EllipseLock
const EllipseLockModule = NativeModules.EllipseLock
const EllipseLockEmitter = new NativeEventEmitter(EllipseLockModule)

const enableBluetooth = async () => {
  try {
    await BluetoothManagerModule.enableBluetooth()
  } catch (e) {
    console.error(e)
  }
}

const disableBluetooth = async () => {
  try {
    await BluetoothManagerModule.disableBluetooth()
  } catch (e) {
    console.error(e)
  }
}

const isBluetoothSupported = async () => {
  try {
    return await BluetoothManagerModule.isBluetoothSupported()
  } catch (e) {
    console.error(e)
  }
}

const isBluetoothEnabled = async () => {
  return await BluetoothManagerModule.isBluetoothEnabled()
}

const isConnected = async () => {
  return connected
}

const isEllipseLocked = async () => {
  return isLocked
}

const addListener = async (eventType, listener) => {
  BluetoothManagerEmitter.addListener(eventType, listener)
}

const handleEllipseLockEvent = (event) => {
  console.log('Event Received:'+JSON.stringify(event))
  Object.keys(event).forEach(function(key) {

      console.log(key);
      console.log(event[key]);
      if(key == 'connect' && event[key] == 'BluetoothOFFError'){
         enableBluetoothAndConnect()
      }else if(key == 'connect' && event[key] == 'connected'){
        connected = true;
        observeLockPosition(LOCK_MAC_ID)
        window.appComponent.setConnectionStatus();
      }else if(key == 'position'){
        if(event[key] == 'LOCKED'){
          isLocked = true;
        }else if(event[key] == 'UNLOCKED'){
          isLocked = false;
        }
        window.appComponent.setConnectionStatus();
      }else if(key == 'connect' && event[key] == 'disconnected'){
        connected = false;
        window.appComponent.setConnectionStatus();
      }

    });
}


const enableBluetoothAndConnect = async() =>{
  await enableBluetooth()
  connectToLock(LOCK_MAC_ID)
}

EllipseLockEmitter.addListener('onEllipseLockEvent', handleEllipseLockEvent)



const setApiToken = async apiToken => {
  EllipseLockModule.setApiToken(apiToken)
}

const connectToLock = async macId => {
  try {
    console.log('connecting...')
    console.log('connect', await EllipseLockModule.connect(macId))
  } catch (e) {
    console.error(e)
  }
}

const setPosition = async (macId, position) => {
  try {
    console.log('setPosition')
    console.log(await EllipseLockModule.setPosition(macId, position))
  } catch (e) {
    console.error(e)
  }
}

const observeLockPosition = async (macId) => {
  try {
    console.log('setPosition')
    console.log(await EllipseLockModule.observeLockPosition(macId))
  } catch (e) {
    console.error(e)
  }
}

const setMagnetAutoLock = async (macId, active) => {
  try {
    console.log('setMagnetAutoLock')
    console.log(await EllipseLockModule.setMagnetAutoLock(macId, active))
  } catch (e) {
    console.error(e)
  }
}

const setTouchCap = async (macId, active) => {
  try {
    console.log('setTouchCap')
    console.log(await EllipseLockModule.setTouchCap(macId, active))
  } catch (e) {
    console.error(e)
  }
}

const reset = async macId => {
  try {
    console.log('reset', await EllipseLockModule.resetEllipse(macId))
  } catch (e) {
    console.error(e)
  }
}

const disconnect = async macId => {
  try {
    console.log('disconnect', await EllipseLockModule.disconnectAllLocks())
    connected = false
    window.appComponent.setConnectionStatus()
  } catch (e) {
    console.error(e)
  }
}

setApiToken(API_TOKEN)

async function requestLocationPermission() {
  try {
    const granted = await PermissionsAndroid.request(
     PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
     {
       'title': 'Location Permission',
       'message': 'App needs access to your location '
     }
   )
   if (granted === PermissionsAndroid.RESULTS.GRANTED) {
   }
   else {
   }
  } catch (err) {
    console.warn(err);
  }
}

// App
export default class App extends Component {
  constructor(props) {
    super(props)
    this.state = {connected:false, isLocked:false}
    this.updateStatus()
    window.appComponent = this
  }

  setConnectionStatus = async()=>{
    this.updateStatus()
  }

  async componentDidMount() {
   await requestLocationPermission()
 }

  enableBluetooth = async () => {
    await enableBluetooth()
    await this.updateStatus()
  }
  disableBluetooth = async () => {
    await disableBluetooth()
    await this.updateStatus()
  }
  updateStatus = async () => {
    // const enabled = await isBluetoothEnabled()
    // const supported = await isBluetoothSupported()
    const connected = await isConnected()
    const isLocked = await isEllipseLocked()
    this.setState({connected,isLocked})
    // this.setState({ enabled, supported })
  }



  render() {
    return (
      <View style={styles.container}>
        <Text style={{fontSize: 20, fontWeight: 'bold', marginBottom: 25}}>
          React Native Ellipse SDK demo
        </Text>

        {!this.state.connected && (
        <Button
          onPress={() => connectToLock(LOCK_MAC_ID)}
          title={`Connect to ${LOCK_MAC_ID}`}
        />
        )}

        {this.state.connected && (
        <Text style={{fontSize: 15, fontWeight: 'bold', marginBottom: 25}}>
          Connected to: {LOCK_MAC_ID}
        </Text>
        )}
        {this.state.connected && !this.state.isLocked && (
        <Button
          style={{fontSize: 10, marginBottom: 25}}
          onPress={() => setPosition(LOCK_MAC_ID, true)}
          title="Lock"
        />
        )}
        {this.state.connected && this.state.isLocked && (
        <Button
          onPress={() => setPosition(LOCK_MAC_ID, false)}
          title="Unlock"
        />
        )}
        {this.state.connected && (
        <Button
          onPress={() => setMagnetAutoLock(LOCK_MAC_ID,true)}
          title="Magnetic Auto Lock ON"
          color="green"
        />
        )}
        {this.state.connected && (
        <Button
          onPress={() => setMagnetAutoLock(LOCK_MAC_ID,false)}
          title="Magnetic Auto Lock OFF"
          color="green"
        />
        )}
        {this.state.connected && (
        <Button
          onPress={() => setTouchCap(LOCK_MAC_ID,true)}
          title="Touch cap ON"
          color="orange"
        />
        )}
        {this.state.connected && (
        <Button
          onPress={() => setTouchCap(LOCK_MAC_ID,false)}
          title="Touch cap OFF"
          color="orange"
        />
        )}
        {this.state.connected && (
        <Button
          onPress={() => disconnect(LOCK_MAC_ID)}
          title="Reset"
          color="red"
        />
        )}
        {this.state.connected && (
        <Button
          onPress={() => disconnect(LOCK_MAC_ID)}
          title="Disconnect"
          color="black"
        />
        )}
      </View>
    )
  }
}
const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCF3'
  }
})
