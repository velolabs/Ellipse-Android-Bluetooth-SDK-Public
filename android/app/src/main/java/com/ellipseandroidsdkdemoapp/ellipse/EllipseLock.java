package com.ellipseandroidsdkdemoapp.ellipse;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import io.lattis.ellipse.sdk.manager.IEllipseManager;
import io.lattis.ellipse.sdk.manager.EllipseManager;
import io.lattis.ellipse.sdk.model.BluetoothLock;
import io.lattis.ellipse.sdk.exception.BluetoothException;
import io.lattis.ellipse.sdk.model.Status;
import static io.lattis.ellipse.sdk.model.Status.DISCONNECTED;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;

public class EllipseLock extends ReactContextBaseJavaModule {

  // React app context
  private ReactContext _reactContext;

  // Android context
  private Context _context;

  public EllipseLock(ReactApplicationContext reactContext) {
    super(reactContext);
    this._context = reactContext;
    this._reactContext = reactContext;
  }

  // Implement ReactContextBaseJavaModule
  @Override
  public String getName() {
    return "EllipseLock";
  }

  // Lock to Map
  private static WritableMap lockToMap(BluetoothLock lock) {
    WritableMap lockMap = Arguments.createMap();
    lockMap.putString("id", lock.getLockId());
    lockMap.putString("macAddress", lock.getMacAddress());
    lockMap.putString("macId", lock.getMacId());
    lockMap.putString("signedMessage", lock.getSignedMessage());
    lockMap.putString("publicKey", lock.getPublicKey());
    lockMap.putString("userId", lock.getUserId());
    lockMap.putBoolean("autoLockActive", lock.isAutoLockActive());
    lockMap.putBoolean("autoUnlockActive", lock.isAutoUnLockActive());

    return lockMap;
  }

  // Locks to Array
  private static WritableArray locksToArray(List<BluetoothLock> locks) {
    WritableArray locksArray = Arguments.createArray();

    for (BluetoothLock _lock : locks) {
      locksArray.pushMap(EllipseLock.lockToMap(_lock));
    }

    return locksArray;
  }

  // Ellipse access
  private IEllipseManager _ellipseManager;

  private IEllipseManager _getEllipseManager() {
    if (this._ellipseManager == null) {
      this._ellipseManager = EllipseManager.newInstance(this.getCurrentActivity());
    }
    return this._ellipseManager;
  }

  // Set API token
  private String _apiToken;

  @ReactMethod
  public void setApiToken(String apiToken) {
    this._apiToken = apiToken;
  }

  // Scan Ellipses
  @ReactMethod
  public void startScan(int duration, Promise promise) {
    List<BluetoothLock> locks = new ArrayList<>();

    this._getEllipseManager().startScan(duration).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    .subscribeWith(new DisposableObserver<BluetoothLock>() {
      @Override
      public void onStart() {
        WritableMap onEllipseLockEventMap = Arguments.createMap();
        onEllipseLockEventMap.putBoolean("started", true);
        sendEvent("onEllipseLockEvent", onEllipseLockEventMap);

        sendEvent("onScanStart", null);
      }

      @Override
      public void onComplete() {
        WritableMap onEllipseLockEventMap = Arguments.createMap();
        onEllipseLockEventMap.putBoolean("complete", true);
        onEllipseLockEventMap.putArray("locks", EllipseLock.locksToArray(locks));
        sendEvent("onEllipseLockEvent", onEllipseLockEventMap);

        WritableMap onScanCompleteMap = Arguments.createMap();
        onScanCompleteMap.putBoolean("complete", true);
        onEllipseLockEventMap.putArray("locks", EllipseLock.locksToArray(locks));
        sendEvent("onScanComplete", onScanCompleteMap);

        WritableMap resolveMap = Arguments.createMap();
        resolveMap.putBoolean("complete", true);
        onEllipseLockEventMap.putArray("locks", EllipseLock.locksToArray(locks));
        promise.resolve(resolveMap);
      }

      @Override
      public void onError(Throwable error) {
        WritableMap onEllipseLockEventMap = Arguments.createMap();
        onEllipseLockEventMap.putString("error", error.toString());
        sendEvent("onEllipseLockEvent", onEllipseLockEventMap);

        WritableMap onScanErrorMap = Arguments.createMap();
        onScanErrorMap.putString("error", error.toString());
        sendEvent("onScanError", onScanErrorMap);

        promise.reject(error.toString());
      }

      @Override
      public void onNext(BluetoothLock _lock) {
        if (!locks.contains(_lock)) {
          locks.add(_lock);
        }

        WritableMap onEllipseLockEventMap = Arguments.createMap();
        onEllipseLockEventMap.putMap("lock", EllipseLock.lockToMap(_lock));
        sendEvent("onEllipseLockEvent", onEllipseLockEventMap);

        WritableMap onScanLockFoundMap = Arguments.createMap();
        onScanLockFoundMap.putMap("lock", EllipseLock.lockToMap(_lock));
        sendEvent("onScanLockFound", onScanLockFoundMap);
      }
    });
  }

  // Connect to lock
  @ReactMethod
  public void connect(String macId, Promise promise) {
    BluetoothLock lock = new BluetoothLock();
    lock.setMacId(macId);
    this._getEllipseManager().connect(this._apiToken, lock).subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread()).subscribeWith(new DisposableObserver<Status>() {
      @Override
      public void onStart() {
        WritableMap map = Arguments.createMap();
        map.putString("connect", "onStart");
        sendEvent("onEllipseLockEvent", map);
      }

      @Override
      public void onComplete() {
        WritableMap map = Arguments.createMap();
        map.putString("connect", "onComplete");
        sendEvent("onEllipseLockEvent", map);
      }

      @Override
      public void onError(Throwable e) {
        WritableMap map = Arguments.createMap();
        if(e!=null && e instanceof BluetoothException){
          BluetoothException exception = (BluetoothException) e;
          if (exception != null) {
            if(exception.getStatus()!=null){
              if(exception.getStatus() == BluetoothException.Status.BLUETOOTH_DISABLED){
                map.putString("connect", "BluetoothOFFError");
                sendEvent("onEllipseLockEvent", map);
              } else if(exception.getStatus() == BluetoothException.Status.DEVICE_NOT_FOUND){
                connect(macId,promise);    // in case if it is required to connect when Ellipse is in range
              }
            }
          }
        }
        promise.reject(e.toString());
      }

      @Override
      public void onNext(Status status) {
        WritableMap map = Arguments.createMap();
        if (status.isAuthenticated()) {
          map.putString("connect", "connected");
          sendEvent("onEllipseLockEvent", map);
          promise.resolve(true);
        } else if (status == DISCONNECTED) {
          promise.reject("Disconnected");
          map.putString("connect", "disconnected");
          sendEvent("onEllipseLockEvent", map);
        }
      }
    });
  }

  // Disconnect all locks
  @ReactMethod
  public void disconnectAllLocks(Promise promise) {
    this._getEllipseManager().disconnectAllLocks().subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread()).subscribeWith(new DisposableObserver<Boolean>() {
      @Override
      public void onStart() {
      }

      @Override
      public void onComplete() {
        WritableMap map = Arguments.createMap();
        map.putString("disconnect", "onComplete");
        sendEvent("onEllipseLockEvent", map);
      }

      @Override
      public void onError(Throwable error) {
        promise.reject(error.toString());
      }

      @Override
      public void onNext(Boolean status) {
        promise.resolve(status);
      }
    });
  }

  // Set lock position
  @ReactMethod
  public void setPosition(String macId, Boolean shouldLock, Promise promise) {
    BluetoothLock lock = new BluetoothLock();
    lock.setMacId(macId);
    this._getEllipseManager().setPosition(lock, shouldLock).subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread()).subscribeWith(new DisposableObserver<Boolean>() {
      @Override
      public void onStart() {
        WritableMap map = Arguments.createMap();
        map.putString("position", "onStart");
        sendEvent("onEllipseLockEvent", map);
      }

      @Override
      public void onComplete() {
        WritableMap map = Arguments.createMap();
        map.putString("position", "onComplete");
        sendEvent("onEllipseLockEvent", map);
        promise.resolve(true);
      }

      @Override
      public void onError(Throwable error) {
        promise.reject(error.toString());
      }

      @Override
      public void onNext(Boolean status) {
        WritableMap map = Arguments.createMap();
        map.putString("position", "onNext");
        map.putBoolean("status", status);
        sendEvent("onEllipseLockEvent", map);
      }
    });
  }

  // Events
  public void sendEvent(String eventName, @Nullable WritableMap params) {
    this._reactContext.getJSModule(RCTNativeAppEventEmitter.class).emit(eventName, params);
  }

}
