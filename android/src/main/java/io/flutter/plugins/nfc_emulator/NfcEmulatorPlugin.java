package io.flutter.plugins.nfc_emulator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** NfcEmulatorPlugin */
public class NfcEmulatorPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler {

    private Activity activity;

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;

    @Override
    public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "nfc_emulator");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        }
        else if (call.method.equals("getNfcStatus")) {
            int nfcStatus = getNfcStatus();
            result.success(nfcStatus);
        }
        else if (call.method.equals("startNfcEmulator")) {
            String cardAid = call.argument("cardAid");
            String cardUid = call.argument("cardUid");
            String aesKey  = call.argument("aesKey");
            startNfcEmulator(cardAid, cardUid, aesKey);
            result.success(null);
        }
        else if (call.method.equals("stopNfcEmulator")) {
            stopNfcEmulator();
            result.success(null);
        }
        else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }

    private int getNfcStatus() {
        if(activity==null) {
            return -1;
        }
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            // This device does not support NFC
            return 1;
        }
        if (nfcAdapter!=null && !nfcAdapter.isEnabled()) {
            // NFC not enabled
            return 2;
        }
        return 0;
    }

    private void startNfcEmulator(String cardAid, String cardUid, String aesKey) {
        SharedPreferences sharePerf = activity.getSharedPreferences("NfcEmulator", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharePerf.edit();
        editor.putString("cardAid", cardAid);
        editor.putString("cardUid", cardUid);
        editor.putString("aesKey", aesKey);
        editor.apply();

        Intent intent = new Intent(activity,  NfcEmulatorService.class);
        activity.startService(intent);
    }

    private void stopNfcEmulator() {
        Intent intent = new Intent(activity, NfcEmulatorService.class);
        activity.stopService(intent);

        SharedPreferences sharePerf = activity.getSharedPreferences("NfcEmulator", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharePerf.edit();
        editor.remove("cardAid");
        editor.remove("cardUid");
        editor.remove("aesKey");
        editor.apply();
    }

}
