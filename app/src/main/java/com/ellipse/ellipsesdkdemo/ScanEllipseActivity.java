package com.ellipse.ellipsesdkdemo;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import io.lattis.ellipse.sdk.model.BluetoothLock;

public class ScanEllipseActivity extends AppCompatActivity  implements ScanEllipseFragment.OnListFragmentInteractionListener {

    public static String ELLIPSE_NAME = "ELLIPSE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
    }


    @Override
    public void onListFragmentInteraction(BluetoothLock item) {
        Intent intent = new Intent();
        intent.putExtra(ELLIPSE_NAME,item.getName());
        setResult(RESULT_OK,intent);
        finish();
    }
}
