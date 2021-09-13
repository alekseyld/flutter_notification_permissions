package com.vanethos.notification_permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class NotificationPermissionsPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {

    private static final String PERMISSION_GRANTED = "granted";
    private static final String PERMISSION_DENIED = "denied";

    private Context context;
    private MethodChannel methodChannel;

    /** Plugin registration. */
    @SuppressWarnings("deprecation")
    public static void registerWith(io.flutter.plugin.common.PluginRegistry.Registrar registrar) {
        final NotificationPermissionsPlugin instance = new NotificationPermissionsPlugin();
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        this.context = applicationContext;
        methodChannel =
                new MethodChannel(messenger, "notification_permissions");
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        context = null;
        methodChannel.setMethodCallHandler(null);
        methodChannel = null;
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull MethodChannel.Result result) {
        if ("getNotificationPermissionStatus".equalsIgnoreCase(call.method)) {
            result.success(getNotificationPermissionStatus());
        } else if ("requestNotificationPermissions".equalsIgnoreCase(call.method)) {
            if (PERMISSION_DENIED.equalsIgnoreCase(getNotificationPermissionStatus())) {
                openNotificationSettings(call, result);
            } else {
                result.success(PERMISSION_GRANTED);
            }
        } else if ("openSystemSettings".equalsIgnoreCase(call.method)) {
            openNotificationSettings(call, result);
        } else {
            result.notImplemented();
        }
    }

    private void openNotificationSettings(MethodCall call, MethodChannel.Result result) {
        if (context instanceof Activity) {
            // https://stackoverflow.com/a/45192258
            final Intent intent = new Intent();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // ACTION_APP_NOTIFICATION_SETTINGS was introduced in API level 26 aka Android O
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", context.getPackageName());
                intent.putExtra("app_uid", context.getApplicationInfo().uid);
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            }

            context.startActivity(intent);

            result.success(PERMISSION_DENIED);
        } else {
            result.error(call.method, "context is not instance of Activity", null);
        }
    }

    private String getNotificationPermissionStatus() {
        return (NotificationManagerCompat.from(context).areNotificationsEnabled())
                ? PERMISSION_GRANTED
                : PERMISSION_DENIED;
    }
}
