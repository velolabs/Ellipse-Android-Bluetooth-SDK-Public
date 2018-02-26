package com.ellipse.ellipsesdkdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.lattis.ellipse.sdk.exception.BluetoothException;
import io.lattis.ellipse.sdk.manager.EllipseManager;
import io.lattis.ellipse.sdk.manager.IEllipseManager;
import io.lattis.ellipse.sdk.model.BluetoothLock;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ScanEllipseFragment extends Fragment {

    List<BluetoothLock> locks = new ArrayList<>();

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    IEllipseManager ellipseManager=null;
    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 5647;
    MyEllipsesRecyclerViewAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ScanEllipseFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startScan();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ellipselock_list, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        return view;
    }


    private void showBluetoothLocks(){
        if(adapter==null) {
            Context context = recyclerView.getContext();
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            adapter = new MyEllipsesRecyclerViewAdapter(locks, mListener);
            recyclerView.setAdapter(adapter);
        }else{
            adapter.updateBluetoothLocks(locks);
            adapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(BluetoothLock item);
    }



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

    public void requestEnableBluetooth() {
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                REQUEST_CODE_ENABLE_BLUETOOTH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == REQUEST_CODE_ENABLE_BLUETOOTH){
                startScan();
            }
        }
    }


    IEllipseManager getEllipseManager(){
        if(ellipseManager==null){
            ellipseManager= EllipseManager.newInstance(getActivity());;
        }
        return ellipseManager;
    }

}
