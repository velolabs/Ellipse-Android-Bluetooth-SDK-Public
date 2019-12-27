package com.ellipseandroidsdkdemoapp.bluetooth;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothAdapter;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import static android.app.Activity.RESULT_OK;

public class BluetoothManager extends ReactContextBaseJavaModule implements ActivityEventListener {

    // React app context
    private ReactContext _reactContext;

    // Android context
    private Context _context;

    public BluetoothManager(ReactApplicationContext reactContext) {
        super(reactContext);
        this._context = reactContext;
        this._reactContext = reactContext;
        this._reactContext.addActivityEventListener(this);

        this._listenBluetoothStateChange();
    }

    // Implement ReactContextBaseJavaModule
    @Override
    public String getName() {
        return "BluetoothManager";
    }

    // Bluetooth access
    private BluetoothAdapter _bluetoothAdapter;

    private BluetoothAdapter _getBluetoothAdapter() {
        if (this._bluetoothAdapter == null) {
            android.bluetooth.BluetoothManager bluetoothManager = (android.bluetooth.BluetoothManager) this._context
                    .getSystemService(Context.BLUETOOTH_SERVICE);
            this._bluetoothAdapter = bluetoothManager.getAdapter();
        }
        return this._bluetoothAdapter;
    }

    // Is bluetooth supported
    @ReactMethod
    public void isBluetoothSupported(final Promise promise) {
        promise.resolve(this._getBluetoothAdapter() != null);
    }

    // Enable bluetooth
    private static final int ENABLE_REQUEST = 539;

    private Promise _enableBluetoothPromise;

    @ReactMethod
    public void enableBluetooth(final Promise promise) {
        if (this._getBluetoothAdapter() == null) {
            promise.reject("No bluetooth support");
            return;
        }
        if (!this._getBluetoothAdapter().isEnabled()) {
            this._enableBluetoothPromise = promise;
            Intent intentEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (this.getCurrentActivity() == null) {
                promise.reject("Current activity not available");
            } else {
                this.getCurrentActivity().startActivityForResult(intentEnable, ENABLE_REQUEST);
            }
        } else {
            promise.resolve(true);
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_REQUEST && this._enableBluetoothPromise != null) {
            if (resultCode == RESULT_OK) {
                this._enableBluetoothPromise.resolve(true);
            } else {
                this._enableBluetoothPromise.reject("User refused to enable");
            }
            this._enableBluetoothPromise = null;
        }
    }

    // Disable blueooth
    @ReactMethod
    void disableBluetooth(final Promise promise) {
        if (this._getBluetoothAdapter() == null) {
            promise.reject("No bluetooth support");
            return;
        }
        this._getBluetoothAdapter().disable();
        promise.resolve(true);
    }

    // Is bluetooth enabled
    @ReactMethod
    public void isBluetoothEnabled(final Promise promise) {
        if (this._getBluetoothAdapter() == null) {
            promise.reject("No bluetooth support");
        }
        promise.resolve(this._getBluetoothAdapter().isEnabled());
    }

    // Bluetooth state change
    private void _listenBluetoothStateChange() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this._context.registerReceiver(_actionReceiver, filter);
    }

    private final BroadcastReceiver _actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                String stateString = "";

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        stateString = "off";
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        stateString = "turning_off";
                        break;
                    case BluetoothAdapter.STATE_ON:
                        stateString = "on";
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        stateString = "turning_on";
                        break;
                }

                WritableMap map = Arguments.createMap();
                map.putString("state", stateString);
                sendEvent("bluetoothStateChange", map); // this.sendEvent
            }
        }
    };

    // Events
    public void sendEvent(String eventName, @Nullable WritableMap params) {
        this._reactContext.getJSModule(RCTNativeAppEventEmitter.class).emit(eventName, params);
    }

    // Implement ActivityEventListener
    @Override
    public void onNewIntent(Intent intent) {
    }

}
