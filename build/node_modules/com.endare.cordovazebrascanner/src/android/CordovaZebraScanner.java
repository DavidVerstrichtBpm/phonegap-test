package com.endare.cordovazebrascanner;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.ContactsContract;

/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaZebraScanner extends CordovaPlugin {

    // <editor-fold desc="Constants">
    private static final class Methods {
        public static final String TEST_SCAN = "testScan";
        public static final String SCAN_ONCE = "scanOnce";
        public static final String START_SCANNING = "startScanning";
        public static final String STOP_SCANNING = "stopScanning";
        public static final String FAKE_SCAN = "fakeScan";
    }

    private static final class ZebraKeys {
        public static final String INTENT_FILTER_ACTION = "com.endare.cordovazebrascanner.BARCODE_SCANNED";
        public static final String DATA = "com.symbol.datawedge.data_string";
        public static final String PROFILE_NAME = "CordovaZebraScannerModule";
    }

    private static final class ZebraActions {
        public static final String DATA_WEDGE = "com.symbol.datawedge.api.ACTION";
        public static final String RESULT_NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION";
        public static final String RESULT = "RESULT";
    }

    private static final class ZebraExtras {
        public static final String CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE";
        public static final String SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";
        public static final String SEND_RESULT = "SEND_RESULT";
        public static final String GET_VERSION_INFO = "com.symbol.datawedge.api.GET_VERSION_INFO";
        public static final String EMPTY = "";
        public static final String REGISTER_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION";
        public static final String APPLICATION_NAME = "com.symbol.datawedge.api.APPLICATION_NAME";
        public static final String NOTIFICATION_TYPE = "com.symbol.datawedge.api.NOTIFICATION_TYPE";
        public static final String SCANNER_STATUS = "SCANNER_STATUS";
        public static final String UNREGISTER_NOTIFICATION = "com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION";
    }
    // </editor-fold>

    // <editor-fold desc="Properties">
    private CallbackContext scanOnceCallback;
    private CallbackContext scanMultipleCallback;
    private boolean isScanningMultiple;
    // </editor-fold>

    // <editor-fold desc="Broadcast receiver">
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            String action = intent.getAction();
            if (action == null) {
                return;
            }

            if (ZebraKeys.INTENT_FILTER_ACTION.equals(action)) {
                String barcode = intent.getStringExtra(ZebraKeys.DATA);
                if (barcode == null) {
                    return;
                }

                if (scanOnceCallback != null) {
                    scanOnceCallback.success(barcode);
                    scanOnceCallback = null;
                }

                if (isScanningMultiple && scanMultipleCallback != null) {
                    PluginResult pluginResult = new  PluginResult(PluginResult.Status.OK, barcode);
                    pluginResult.setKeepCallback(true);
                    scanMultipleCallback.sendPluginResult(pluginResult);
                }
            }
        }
    };
    // </editor-fold>

    // <editor-fold desc="Initialisation">
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }
    // </editor-fold>

    // <editor-fold desc="Lifecycle">
    @Override
    public void onResume(boolean multitasking) {
        init();
        registerReceivers();
    }

    @Override
    public void onPause(boolean multitasking) {
        unregisterReceivers();
    }
    // </editor-fold>

    // <editor-fold desc="Public interface">
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action == null) {
            return false;
        }

        switch (action) {
            case Methods.TEST_SCAN:
                boolean shouldSucceed = args.getBoolean(0);
                this.testScan(shouldSucceed, callbackContext);

                return true;
            case Methods.SCAN_ONCE:
                this.scanOnce(callbackContext);

                return true;
            case Methods.START_SCANNING:
                this.startScanning(callbackContext);

                return true;
            case Methods.STOP_SCANNING:
                this.stopScanning(callbackContext);

                return true;
            case Methods.FAKE_SCAN:
                String barcode = args.getString(0);
                this.fakeScan(barcode, callbackContext);

                return true;
            default:
                return false;
        }
    }

    private void testScan(boolean shouldSucceed, CallbackContext callbackContext) {
        if (shouldSucceed) {
            callbackContext.success("TestScan Success");
        } else {
            callbackContext.error("TestScan Failure");
        }
    }

    private void scanOnce(CallbackContext callbackContext) {
        scanOnceCallback = callbackContext;
    }

    private void startScanning(CallbackContext callbackContext) {
        scanMultipleCallback = callbackContext;
        isScanningMultiple = true;
    }

    private void stopScanning(CallbackContext callbackContext) {
        isScanningMultiple = false;
        scanMultipleCallback = null;
    }

    private void fakeScan(String barcode, CallbackContext callbackContext) {
        Intent intent = new Intent();
        intent.setAction(ZebraKeys.INTENT_FILTER_ACTION);
        intent.putExtra(ZebraKeys.DATA, barcode);

        broadcastReceiver.onReceive(null, intent);
    }
    // </editor-fold>

    // <editor-fold desc="Helpers">
    private void init() {
        // Create the profile
        sendDataWedgeIntent(ZebraActions.DATA_WEDGE, ZebraExtras.CREATE_PROFILE, ZebraKeys.PROFILE_NAME);

        Bundle paramList = new Bundle();
        paramList.putString("scanner_selection", "auto");
        paramList.putString("scanner_input_enabled", "true");
        paramList.putString("decoder_code128", "true");
        paramList.putString("decoder_code39", "true");
        paramList.putString("decoder_ean13", "true");
        paramList.putString("decoder_upca", "true");

        Bundle barcodePlugin = new Bundle();
        barcodePlugin.putString("PLUGIN_NAME", "BARCODE");
        barcodePlugin.putString("RESET_CONFIG", "true");
        barcodePlugin.putBundle("PARAM_LIST", paramList);

        Bundle app = new Bundle();
        app.putString("PACKAGE_NAME", appContext().getPackageName());
        app.putStringArray("ACTIVITY_LIST", new String[] {"*"});

        Bundle bundle = new Bundle();
        bundle.putString("PROFILE_NAME", ZebraKeys.PROFILE_NAME);
        bundle.putString("PROFILE_ENABLED", "true");
        bundle.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");
        bundle.putBundle("PLUGIN_CONFIG", barcodePlugin);
        bundle.putParcelableArray("APP_LIST", new Bundle[] {app});

        // Configure the profile
        sendDataWedgeIntent(ZebraActions.DATA_WEDGE, ZebraExtras.SET_CONFIG, bundle);

        bundle.remove("PLUGIN_CONFIG");

        Bundle intentParamList = new Bundle();
        intentParamList.putString("intent_output_enabled", "true");
        intentParamList.putString("intent_action", ZebraKeys.INTENT_FILTER_ACTION);
        intentParamList.putString("intent_delivery", "2");

        Bundle intentBundle = new Bundle();
        intentBundle.putString("PLUGIN_NAME", "INTENT");
        intentBundle.putString("RESET_CONFIG", "false");
        intentBundle.putBundle("PARAM_LIST", intentParamList);

        bundle.putBundle("PLUGIN_CONFIG", intentBundle);
        sendDataWedgeIntent(ZebraActions.DATA_WEDGE, ZebraExtras.SET_CONFIG, bundle);

        // Notifications
        Bundle notificationBundle = new Bundle();
        notificationBundle.putString(ZebraExtras.APPLICATION_NAME, appContext().getPackageName());
        notificationBundle.putString(ZebraExtras.NOTIFICATION_TYPE, "SCANNER_STATUS");
        sendDataWedgeIntent(ZebraActions.DATA_WEDGE, ZebraExtras.REGISTER_NOTIFICATION, notificationBundle);
    }

    private void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ZebraActions.RESULT_NOTIFICATION);
        intentFilter.addAction(ZebraActions.RESULT);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction(ZebraKeys.INTENT_FILTER_ACTION);

        appContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    private void unregisterReceivers() {
        appContext().unregisterReceiver(broadcastReceiver);

        Intent unregisterIntent = new Intent();
        unregisterIntent.setAction(ContactsContract.Intents.Insert.ACTION);

        Bundle extras = new Bundle();
        extras.putString(ZebraExtras.APPLICATION_NAME, appContext().getPackageName());
        extras.putString(ZebraExtras.NOTIFICATION_TYPE, ZebraExtras.SCANNER_STATUS);
        unregisterIntent.putExtra(ZebraExtras.UNREGISTER_NOTIFICATION, extras);

        appContext().sendBroadcast(unregisterIntent);
    }

    private void sendDataWedgeIntent(String action, String extraKey, Bundle extras) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(extraKey, extras);
        intent.putExtra(ZebraExtras.SEND_RESULT, "true");

        appContext().sendBroadcast(intent);
    }

    private void sendDataWedgeIntent(String action, String extraKey, String extraValue) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(extraKey, extraValue);
        intent.putExtra(ZebraExtras.SEND_RESULT, "true");

        appContext().sendBroadcast(intent);
    }

    private Context appContext() {
        return this.cordova.getActivity().getApplicationContext();
    }
    // </editor-fold>
}
