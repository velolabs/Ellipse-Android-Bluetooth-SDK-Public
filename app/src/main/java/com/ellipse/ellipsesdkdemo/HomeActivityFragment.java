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
import io.lattis.ellipse.sdk.network.model.response.GetLatestFirmwareInfoResponse;
import io.lattis.ellipse.sdk.network.model.response.GetLatestFirmwareLogResponse;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static android.app.Activity.RESULT_OK;
import static com.ellipse.ellipsesdkdemo.ScanEllipseActivity.ELLIPSE_MAC_ID;
import static io.lattis.ellipse.sdk.model.Status.DISCONNECTED;

/**
 * A placeholder fragment containing a simple view.
 */
@RuntimePermissions
public class HomeActivityFragment extends Fragment {

    private String TAG = HomeActivityFragment.class.getSimpleName();
    private static final int LAYOUT_CONNECT = 0;
    private static final int LAYOUT_LOCK_UNLOCK = 1;
    IEllipseManager ellipseManager=null;
    BluetoothLock lock=null;
    TextView tv_connect_lock;
    EditText et_connect_mac_address;
    EditText et_token;
    ViewFlipper viewFlipper;
    TextView tv_ellipse_touch_cap_off;
    TextView tv_ellipse_touch_cap_on;
    TextView tv_ellipse_lock_unlock;
    TextView tv_lock_title;
    TextView tv_scan_ellipse;
    TextView tv_ellipse_rssi;
    TextView tv_ellipse_battery;
    TextView tv_ellipse_firmwarelogs;
    TextView tv_ellipse_version;
    TextView tv_ellipse_latest_version;
    ProgressBar progressBar;
    private static final int REQUEST_CODE_SCAN_ACTIVITY = 101;
    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 102;


    private Ellipse.Hardware.Position lockPosition;

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
        tv_scan_ellipse=  (TextView) view.findViewById(R.id.tv_scan_ellipse);
        tv_ellipse_lock_unlock=  (TextView) view.findViewById(R.id.tv_ellipse_lock_unlock);
        tv_ellipse_rssi=  (TextView) view.findViewById(R.id.tv_ellipse_rssi);
        tv_ellipse_battery=  (TextView) view.findViewById(R.id.tv_ellipse_battery);
        et_connect_mac_address= (EditText) view.findViewById(R.id.et_lock_macaddress);
        tv_ellipse_firmwarelogs=  (TextView) view.findViewById(R.id.tv_ellipse_firmwarelogs);
        tv_ellipse_version= (TextView) view.findViewById(R.id.tv_ellipse_version);
        tv_ellipse_latest_version= (TextView) view.findViewById(R.id.tv_ellipse_latest_version);
        et_token= (EditText) view.findViewById(R.id.et_token);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        viewFlipper.setDisplayedChild(LAYOUT_CONNECT);

        tv_connect_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(et_token.getText()==null || et_token.getText().toString()==null || et_token.getText().toString().isEmpty()
                || et_connect_mac_address.getText()==null || et_connect_mac_address.getText().toString()==null  || et_connect_mac_address.getText().toString().isEmpty()){
                    Toast.makeText(getActivity(),"Please enter token and Ellipse's mac address",Toast.LENGTH_LONG).show();
                    return;
                }
                connectToLock();
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

        HomeActivityFragmentPermissionsDispatcher.getLocationPermissionWithCheck(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_ACTIVITY && resultCode == RESULT_OK){
            if(data!=null) {
                if (data.hasExtra(ELLIPSE_MAC_ID)) {
                    et_connect_mac_address.setText(data.getExtras().getString(ELLIPSE_MAC_ID));
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
        getEllipseManager(). observeHardwareState(lock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Ellipse.Hardware.State>() {

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
                        }

                        tv_ellipse_battery.setText("Battery: "+ setBatteryLevel(state.getBatteryLevel()) + " %");
                        tv_ellipse_rssi.setText("Rssi Level: "+setRssiLevel(state.getRssiLevel())+" %");

                    }
                });
    }



    private void getFirmwareLog(){
        progressBar.setVisibility(View.VISIBLE);
        getEllipseManager().getFirmwareLog()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<GetLatestFirmwareLogResponse>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(GetLatestFirmwareLogResponse getLatestFirmwareLogResponse) {
                        progressBar.setVisibility(View.GONE);

                        String[] logs = getLatestFirmwareLogResponse.getData();
                        String info = "";
                        if (logs != null) {
                            for (String value : logs) {
                                info = info + "-\t" + value + "\n";
                            }
                        }

                        tv_ellipse_firmwarelogs.setText("Version ChangeLog: "+ info);

                    }
                });
    }

    private void getLatestFirmwareVersionInfo(){
        progressBar.setVisibility(View.VISIBLE);
        getEllipseManager().getLatestFirmwareInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<GetLatestFirmwareInfoResponse>() {

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error occurred: " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(GetLatestFirmwareInfoResponse getLatestFirmwareInfoResponse) {
                        progressBar.setVisibility(View.GONE);
                        String[] versions = getLatestFirmwareInfoResponse.getVersions();
                        if(versions != null && versions.length > 0){
                            String versionString = versions[versions.length -1];
                            tv_ellipse_latest_version.setText(versionString);
                        }
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
                        tv_ellipse_version.setText("Current Version: "+version.getApplicationVersion()+"."+version.getApplicationRevision());
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
        getFirmwareLog();
        getLatestFirmwareVersionInfo();
        getEllipseVersion();
        observeConnection();
    }

    private void onLockDisconnected(){
        Toast.makeText(getActivity(),"Ellipse disconnected. Please try again!",Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.GONE);
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



}
