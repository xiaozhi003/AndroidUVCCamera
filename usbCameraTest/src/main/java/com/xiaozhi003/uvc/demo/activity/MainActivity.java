package com.xiaozhi003.uvc.demo.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.eyecool.uvc.demo.R;


public class MainActivity extends Activity {

    public static final int REQUEST_PERMISSIONS_CODE = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.simpleBtn).setOnClickListener(v -> startActivity(new Intent(this, CommonActivity.class)));

        findViewById(R.id.glBtn).setOnClickListener(v -> startActivity(new Intent(this, GLActivity.class)));

        findViewById(R.id.testBtn).setOnClickListener(v -> startActivity(new Intent(this, TestActivity.class)));

        requestPermissions();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = null;
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                permissions = new ArrayList<>();
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (checkSelfPermission(Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                if (permissions == null) {
                    permissions = new ArrayList<>();
                }
                permissions.add(Manifest.permission.CAMERA);
            }
            if (permissions == null) {
//                initFaceSDK();
            } else {
                String[] permissionArray = new String[permissions.size()];
                permissions.toArray(permissionArray);
                // Request the permission. The result will be received
                // in onRequestPermissionResult()
                requestPermissions(permissionArray, REQUEST_PERMISSIONS_CODE);
            }
        } else {
//            initFaceSDK();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            // Request for WRITE_EXTERNAL_STORAGE permission.
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                initFaceSDK();
            } else {
                // Permission request was denied.
                Toast.makeText(this, "用户未授权", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}