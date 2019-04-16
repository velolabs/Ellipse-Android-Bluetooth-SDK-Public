# Ellipse-Android-Bluetooth-SDK-Public



# Application usage:

## Authorization Token: 

Please enter the authorization token provided by Lattis.


## Ellipse name or mac id: 

If Ellipse's name (it will be Ellipse-XXXXX or Ellipse XXXXX) is known, please enter here. If you dont know, please click on 'Scan Ellipse(s)' button to start scanning the Ellipse(s). Once app show Ellipse(s), click on it to use it further for connection.


Once Ellipse is selected, click 'Connect Lock' to connect it.


## Ellipse connection methods:

In order to scan and find Ellipse(s), please use 'startScan()' method defined in ScanEllipseFragment
```
 void startScan() {
        getEllipseManager().startScan(3500)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<BluetoothLock>() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable throwable) {


                        if (throwable instanceof BluetoothException) {
                            BluetoothException exception = (BluetoothException) throwable;
                            if (exception != null) {
                                if (exception.getStatus() != null) {
                                    if (exception.getStatus().equals(BluetoothException.Status.BLUETOOTH_DISABLED)) {
                                        requestEnableBluetooth();
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onNext(BluetoothLock lock) {
                        if(!locks.contains(lock)){
                            locks.add(lock);
                        }
                        showBluetoothLocks();
                    }
                });
    }
```


In order to connect particular Ellipse, please use 'connectToLock()' method defined in HomeActivityFragment

```
 private void connectToLock(){
        progressBar.setVisibility(View.VISIBLE);
        lock = new BluetoothLock();
        lock.setMacId(et_connect_mac_address.getText().toString());
        getEllipseManager().connect(et_token.getText().toString(),lock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Status>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());

                        if(e!=null && e instanceof BluetoothException){
                            BluetoothException exception = (BluetoothException) e;
                            if (exception != null) {
                                if(exception.getStatus()!=null){
                                    if(exception.getStatus() == BluetoothException.Status.BLUETOOTH_DISABLED){
                                        startBluetooth();
                                    } else if(exception.getStatus() == BluetoothException.Status.DEVICE_NOT_FOUND){
//                                        connectToLock();    // in case if it is required to connect when Ellipse is in range
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onNext(Status status) {
                        if(status.isAuthenticated()){
                            onLockConnected();
                        } else if(status == DISCONNECTED){
                            onLockDisconnected();
                        }
                    }
                });
    }
   ```





Once Ellipse is connected, Ellipse can be lock unlock using 'setPosition(BluetoothLock lock, boolean locked)' defined in HomeActivityFragment. Put 'locked=true' if want to lock Ellipse.


```
public void setPosition(BluetoothLock lock, boolean locked) {
        progressBar.setVisibility(View.VISIBLE);
         getEllipseManager().setPosition(lock, locked)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Boolean>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(Boolean status) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

```



Ellipse SDK can be found at Ellipse-Android-Bluetooth-SDK/Ellipse-Android-Bluetooth-SDK.aar


In order to build the application, please see the app's build.gradle to include necessary dependencies like rxbinding2, retrofit2, okhttp3 & rxjava2. Please see the build.gradle of this project.


In order to observe for shackle insertion

```
private void observeShacklePosition(){
        shackleDisposable = getEllipseManager().observeShacklePosition(lock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Ellipse.Hardware.State.ShackePosition>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Ellipse.Hardware.State.ShackePosition shackePosition) {
                        tv_shacke_position.setText(getString(R.string.shackle_position_label)+ " "+shackePosition.isShackleInserted());
                    }
                });
    }

```



In order to enable / disable magnet auto lock, use following function:

```
public void setMagnetAutoLock(BluetoothLock lock, boolean active) {
        progressBar.setVisibility(View.VISIBLE);
        getEllipseManager().setAutoLockWithShackleInsert(lock, active)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Boolean>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(Boolean status) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

```



 


