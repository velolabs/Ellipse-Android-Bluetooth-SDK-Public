package com.ellipse.ellipsesdkdemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import io.lattis.ellipse.sdk.Ellipse;
import io.lattis.ellipse.sdk.exception.BluetoothException;
import io.lattis.ellipse.sdk.manager.EllipseManager;
import io.lattis.ellipse.sdk.manager.IEllipseManager;
import io.lattis.ellipse.sdk.model.BluetoothLock;
import io.lattis.ellipse.sdk.model.Status;
import io.lattis.ellipse.sdk.util.BluetoothUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static android.app.Activity.RESULT_OK;
import static com.ellipse.ellipsesdkdemo.ScanEllipseActivity.ELLIPSE_NAME;
import static io.lattis.ellipse.sdk.model.Status.DISCONNECTED;

/**
 * A placeholder fragment containing a simple view.
 */
@RuntimePermissions
public class HomeActivityFragment extends Fragment {

    private String TAG = HomeActivityFragment.class.getSimpleName();
    private static final int LAYOUT_CONNECT = 0;
    private static final int LAYOUT_LOCK_UNLOCK = 1;
    private IEllipseManager ellipseManager=null;
    private BluetoothLock lock=null;
    private TextView tv_connect_lock;
    private EditText et_ellipse_name_or_mac_id;
    private EditText et_token;
    private ViewFlipper viewFlipper;
    private TextView tv_ellipse_touch_cap_off;
    private TextView tv_ellipse_touch_cap_on;
    private TextView tv_ellipse_lock_unlock;
    private TextView tv_lock_title;
    private TextView tv_scan_ellipse;
    private TextView tv_ellipse_rssi;
    private TextView tv_ellipse_battery;
    private TextView tv_ellipse_version;
    private TextView tv_ellipse_auto_lock_on_shackle_on;
    private TextView tv_ellipse_auto_lock_on_shackle_off;
    private TextView tv_turn_off_auto_lock;
    private TextView tv_turn_on_auto_lock;
    private TextView tv_shacke_position;
    private TextView tv_disconnect;
    private ProgressBar progressBar;
    private static final int REQUEST_CODE_SCAN_ACTIVITY = 101;
    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 102;
    private Disposable hardwareStateDisposable;
    private Disposable connectToDisposable;
    private Disposable lockPositionDisposable;
    private Disposable shackleDisposable;
    private Ellipse.Hardware.Position lockPosition;
    private boolean isReconnectionRequired=false;

    public HomeActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureViews(view);
    }


    public void configureViews(View view) {

        viewFlipper = (ViewFlipper) view.findViewById(R.id.view_flipper);
        tv_connect_lock =  (TextView) view.findViewById(R.id.tv_connect_lock);
        tv_lock_title=  (TextView) view.findViewById(R.id.tv_lock_title);
        tv_ellipse_touch_cap_off=  (TextView) view.findViewById(R.id.tv_ellipse_touch_cap_off);
        tv_ellipse_touch_cap_on=  (TextView) view.findViewById(R.id.tv_ellipse_touch_cap_on);
        tv_ellipse_auto_lock_on_shackle_on=  (TextView) view.findViewById(R.id.tv_ellipse_auto_lock_on_shackle_on);
        tv_ellipse_auto_lock_on_shackle_off=  (TextView) view.findViewById(R.id.tv_ellipse_auto_lock_on_shackle_off);
        tv_scan_ellipse=  (TextView) view.findViewById(R.id.tv_scan_ellipse);
        tv_ellipse_lock_unlock=  (TextView) view.findViewById(R.id.tv_ellipse_lock_unlock);
        tv_ellipse_rssi=  (TextView) view.findViewById(R.id.tv_ellipse_rssi);
        tv_ellipse_battery=  (TextView) view.findViewById(R.id.tv_ellipse_battery);
        et_ellipse_name_or_mac_id= (EditText) view.findViewById(R.id.et_ellipse_name);
        tv_ellipse_version= (TextView) view.findViewById(R.id.tv_ellipse_version);
        et_token= (EditText) view.findViewById(R.id.et_token);
        tv_turn_on_auto_lock=  (TextView) view.findViewById(R.id.tv_turn_on_auto_lock);
        tv_turn_off_auto_lock=  (TextView) view.findViewById(R.id.tv_turn_off_auto_lock);
        tv_shacke_position=  (TextView) view.findViewById(R.id.tv_shacke_position);
        tv_disconnect=  (TextView) view.findViewById(R.id.tv_disconnect);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        viewFlipper.setDisplayedChild(LAYOUT_CONNECT);

        tv_connect_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(et_token.getText()==null || et_token.getText().toString()==null || et_token.getText().toString().isEmpty()
                || et_ellipse_name_or_mac_id.getText()==null || et_ellipse_name_or_mac_id.getText().toString()==null  || et_ellipse_name_or_mac_id.getText().toString().isEmpty()){
                    Toast.makeText(getActivity(),"Please enter token and Ellipse's name or mac id",Toast.LENGTH_LONG).show();
                    return;
                }
                disconnectAndConnectIfRequired(true);
            }
        });


        tv_ellipse_touch_cap_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTouchCap(lock,true);
            }
        });

        tv_ellipse_touch_cap_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTouchCap(lock,false);
            }
        });


        tv_ellipse_auto_lock_on_shackle_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAutoLockWithShackleInsert(lock,true);
            }
        });

        tv_ellipse_auto_lock_on_shackle_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAutoLockWithShackleInsert(lock,false);
            }
        });

        tv_ellipse_lock_unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(lockPosition==null)
                    return;

                if(lockPosition== Ellipse.Hardware.Position.LOCKED){
                    setPosition(lock,false);
                }else if(lockPosition== Ellipse.Hardware.Position.UNLOCKED){
                    setPosition(lock,true);
                }
            }
        });

        tv_scan_ellipse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(),ScanEllipseActivity.class);
                startActivityForResult(intent,REQUEST_CODE_SCAN_ACTIVITY);
            }
        });

        tv_turn_on_auto_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAutoLock(true);
            }
        });

        tv_turn_off_auto_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAutoLock(false);
            }
        });

        tv_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectAndConnectIfRequired(false);
            }
        });

        HomeActivityFragmentPermissionsDispatcher.getLocationPermissionWithCheck(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_ACTIVITY && resultCode == RESULT_OK){
            if(data!=null) {
                if (data.hasExtra(ELLIPSE_NAME)) {
                    et_ellipse_name_or_mac_id.setText(data.getExtras().getString(ELLIPSE_NAME));
                }
            }
        }else if(requestCode == REQUEST_CODE_ENABLE_BLUETOOTH && resultCode == RESULT_OK){
            connectToLock();
        }
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void getLocationPermission() {

    }

    IEllipseManager getEllipseManager(){
        if(ellipseManager==null){
            ellipseManager= EllipseManager.newInstance(getActivity());;
        }
        return ellipseManager;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }else{

        }
        HomeActivityFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }



    private void connectToLock(){
        progressBar.setVisibility(View.VISIBLE);
        lock = new BluetoothLock();
        if(et_ellipse_name_or_mac_id.getText().toString().contains(" ") || et_ellipse_name_or_mac_id.getText().toString().contains("-")){
            lock.setMacId(BluetoothUtil.getMacIdFromName(et_ellipse_name_or_mac_id.getText().toString()));
        }else{
            lock.setMacId(et_ellipse_name_or_mac_id.getText().toString());
        }
        connectToDisposable = getEllipseManager().connect(et_token.getText().toString(),lock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Status>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error occurred: " + e.getMessage());

                        if(e!=null && e instanceof BluetoothException){
                            BluetoothException exception = (BluetoothException) e;
                            if (exception != null) {
                                if(exception.getStatus()!=null){
                                    if(exception.getStatus() == BluetoothException.Status.BLUETOOTH_DISABLED){
                                        startBluetooth();
                                    } else if(exception.getStatus() == BluetoothException.Status.DEVICE_NOT_FOUND){
                                        connectToLock();    // in case if it is required to connect when Ellipse is in range
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onNext(Status status) {
                        Log.e(TAG,"Status: "+status);
                        if(status.isAuthenticated()){
                            onLockConnected();
                        } else if(status == DISCONNECTED){
                            onLockDisconnected();
                            disconnectAndConnectIfRequired(isReconnectionRequired);
                        }
                    }
                });
    }

    private void startBluetooth(){
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                REQUEST_CODE_ENABLE_BLUETOOTH);
    }


    public void setTouchCap(BluetoothLock lock, boolean active) {
        progressBar.setVisibility(View.VISIBLE);
        getEllipseManager().setTouchCap(lock, active)
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


    public void setAutoLockWithShackleInsert(BluetoothLock lock, boolean active) {
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

    private void observeHardwareState(){
        hardwareStateDisposable = getEllipseManager().observeHardwareState(lock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Ellipse.Hardware.State>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Ellipse.Hardware.State state) {

                        progressBar.setVisibility(View.GONE);
                        lockPosition = state.getPosition();
                        if(lockPosition== Ellipse.Hardware.Position.LOCKED){
                            tv_ellipse_lock_unlock.setText(getString(R.string.ellipse_unlock_label));
                        }else if(lockPosition== Ellipse.Hardware.Position.UNLOCKED){
                            tv_ellipse_lock_unlock.setText(getString(R.string.ellipse_lock_label));
                        }else {
                            Log.e(TAG,"It looks like shackle jam ");
                        }

                        tv_ellipse_battery.setText("Battery: "+ setBatteryLevel(state.getBatteryLevel()) + " %");
                        tv_ellipse_rssi.setText("Rssi Level: "+setRssiLevel(state.getRssiLevel())+" %");

                        tv_shacke_position.setText(getString(R.string.shackle_position_label)+ " "+state.isShackleInserted());

                    }
                });
    }


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


    private void observeLockPosition(){
        lockPositionDisposable = getEllipseManager().observeLockPosition(lock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Ellipse.Hardware.Position>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Ellipse.Hardware.Position position) {
                        if(position== Ellipse.Hardware.Position.LOCKED){

                        }else if(position== Ellipse.Hardware.Position.UNLOCKED){

                        }else {
                            Log.e(TAG,"It looks like shackle jam ");
                        }

                    }
                });
    }


    public void setAutoLock(boolean active) {
        progressBar.setVisibility(View.VISIBLE);
        getEllipseManager().setAutoLock(lock,active)
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



    private void getEllipseVersion(){
        getEllipseManager().getFirmwareVersion(lock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Ellipse.Boot.Version>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Ellipse.Boot.Version version) {
                        tv_ellipse_version.setText("Current FW Version: "+version.getApplicationVersion()+"."+version.getApplicationRevision());
                    }
                });
    }


    public int setRssiLevel(int level) {
        if (level >= -50) {
            return 100;
        } else if (-50 >= level && level >= -70) {
            return 75;
        } else if (-70 >= level && level >= -90) {
            return 50;
        } else if (-90 >= level) {
            return 25;
        }
        return 0;
    }

    public int setBatteryLevel(int level) {

        if (level > 3175) {
            return 100;
        } else if (level > 3050) {
            return 75;
        } else if (level > 2925) {
            return 50;
        } else if (level > 2800) {
            return 25;
        }
        return 0;
    }




    private void onLockConnected(){
        progressBar.setVisibility(View.GONE);
        viewFlipper.setDisplayedChild(LAYOUT_LOCK_UNLOCK);
        tv_lock_title.setText("Lock connected: "+ lock.getMacId() );
        observeHardwareState();
        observeLockPosition();
        observeShacklePosition();
        getEllipseVersion();
        observeConnection();
    }

    private void onLockDisconnected(){
        Toast.makeText(getActivity(),getString(R.string.ellipse_disconnect_toast_label),Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.GONE);
        disposeAllDisposable();
        viewFlipper.setDisplayedChild(LAYOUT_CONNECT);
    }

    public void observeConnection(){
        getEllipseManager().observeLockState(lock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Status>() {

                    @Override
                    public void onNext(Status status) {
                            // get status for connection
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    @Override
    public void onStop() {
        disposeAllDisposable();
        isReconnectionRequired=false;
        super.onStop();
    }


    private void disposeAllDisposable(){
        if(hardwareStateDisposable!=null && !hardwareStateDisposable.isDisposed()){
            hardwareStateDisposable.dispose();
        }

        if(connectToDisposable!=null && !connectToDisposable.isDisposed()){
            connectToDisposable.dispose();
        }

        if(lockPositionDisposable!=null && !lockPositionDisposable.isDisposed()){
            lockPositionDisposable.dispose();
        }

        if(shackleDisposable!=null && !shackleDisposable.isDisposed()){
            shackleDisposable.dispose();
        }
    }

    public void disconnectAndConnectIfRequired(boolean reconnectionRequired) {
        isReconnectionRequired = reconnectionRequired;
        getEllipseManager().disconnectAllLocks()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Boolean>() {

                    @Override
                    public void onNext(Boolean bool) {
                        if(reconnectionRequired){
                            connectToLock();
                        }else{
                            onLockDisconnected();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(reconnectionRequired){
                            connectToLock();
                        }else{
                            onLockDisconnected();
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
