package com.migesok.myclimate;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.RemoteViews;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_DISABLED;
import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static java.lang.String.format;

public class MyClimateAppWidgetProvider extends BroadcastReceiver {
    public static final String ACTION_FETCH_RESULT = "com.migesok.myclimate.FETCH_RESULT";
    public static final String EXTRA_FETCH_SUCCESS = "com.migesok.myclimate.isFetchSuccess";
    public static final String EXTRA_OUTSIDE_TEMP = "com.migesok.myclimate.outsideTemperature";

    private static final long MAX_TIME_WITHOUT_UPDATE_MS = 1000L * 60L * 60L * 5L; // 5h
    private static final long MIN_TIME_BETWEEN_UPDATES = 1000L * 60L * 20L; // 20m
    private static final String WIDGET_STATE_PREF = "com.migesok.myclimate.WIDGET_STATE_PREF_FILE";
    private static final String LAST_UPDATE_TS_KEY = "LAST_UPDATE_TS";
    private static final String OUTSIDE_TEMP_KEY = "OUTSIDE_TEMPERATURE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            onWidgetUpdate(context);
        } else if (ACTION_APPWIDGET_DISABLED.equals(intent.getAction())) {
            onAllWidgetsDeleted(context);
        } else if (ACTION_FETCH_RESULT.equals(intent.getAction())) {
            boolean success = intent.getBooleanExtra(EXTRA_FETCH_SUCCESS, false);
            float outsideTemperature = intent.getFloatExtra(EXTRA_OUTSIDE_TEMP, Float.NaN);
            onFetchResult(context, success, outsideTemperature);
        }
    }

    private void onWidgetUpdate(Context context) {
        Log.d("MyClimateWidget", "Widget update");
        SharedPreferences preferences = context.getSharedPreferences(
                WIDGET_STATE_PREF, Context.MODE_PRIVATE);
        long lastUpdateTs = preferences.getLong(LAST_UPDATE_TS_KEY, 0L);
        if (isInfoOutdated(lastUpdateTs) || !preferences.contains(OUTSIDE_TEMP_KEY)) {
            updateViewsWithNoInfo(context);
        } else {
            float outsideTemperature = preferences.getFloat(OUTSIDE_TEMP_KEY, Float.NaN);
            updateViewsWithInfo(context, outsideTemperature);
        }
        if (isNetworkAvailable(context)) {
            /*
            when disabling network state listener an odd widget update intent can be received
            resulting in two subsequent network calls
            */
            if (isEnoughTimePassedSinceLastUpdate(lastUpdateTs)) {
                context.startService(new Intent(context, FetchDataIntentService.class));
            }
        } else {
            ensureNetworkStateListenerEnabled(context);
        }
    }

    private void onFetchResult(Context context, boolean success, float outsideTemperature) {
        Log.d("MyClimateWidget", "data fetch result received, success: " + success);
        if (success) {
            updateState(context, outsideTemperature);
            updateViewsWithInfo(context, outsideTemperature);
            ensureNetworkStateListenerDisabled(context);
        }
    }

    private void onAllWidgetsDeleted(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                WIDGET_STATE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
        Log.d("MyClimateWidget", "Last widget deleted, state cleared");
    }

    private void ensureNetworkStateListenerEnabled(Context context) {
        ComponentName networkStateListener = new ComponentName(context, NetworkStateListener.class);
        PackageManager pm = context.getPackageManager();
        int enabledSetting = pm.getComponentEnabledSetting(networkStateListener);
        if (enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            pm.setComponentEnabledSetting(networkStateListener,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    private void ensureNetworkStateListenerDisabled(Context context) {
        ComponentName networkStateListener = new ComponentName(context, NetworkStateListener.class);
        PackageManager pm = context.getPackageManager();
        int enabledSetting = pm.getComponentEnabledSetting(networkStateListener);
        if (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            pm.setComponentEnabledSetting(networkStateListener,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    private boolean isInfoOutdated(long lastUpdateTs) {
        return (System.currentTimeMillis() - lastUpdateTs) > MAX_TIME_WITHOUT_UPDATE_MS;
    }

    private boolean isEnoughTimePassedSinceLastUpdate(long lastUpdateTs) {
        return (System.currentTimeMillis() - lastUpdateTs) > MIN_TIME_BETWEEN_UPDATES;
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void updateState(Context context, float outsideTemperature) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                WIDGET_STATE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(LAST_UPDATE_TS_KEY, System.currentTimeMillis());
        editor.putFloat(OUTSIDE_TEMP_KEY, outsideTemperature);
        editor.commit();
    }

    private void updateViewsWithInfo(Context context, float outsideTemperature) {
        RemoteViews updatedViews =
                new RemoteViews(context.getPackageName(), R.layout.myclimate_appwidget);
        updatedViews.setTextViewText(R.id.textView,
                format(context.getString(R.string.info_widget_text), outsideTemperature));
        ComponentName widgetName = new ComponentName(context, MyClimateAppWidgetProvider.class);
        AppWidgetManager.getInstance(context).updateAppWidget(widgetName, updatedViews);
        Log.d("MyClimateWidget", "widget updated with info");
    }

    private void updateViewsWithNoInfo(Context context) {
        RemoteViews updatedViews =
                new RemoteViews(context.getPackageName(), R.layout.myclimate_appwidget);
        updatedViews.setTextViewText(R.id.textView, context.getString(R.string.no_info_widget_text));
        ComponentName widgetName = new ComponentName(context, MyClimateAppWidgetProvider.class);
        AppWidgetManager.getInstance(context).updateAppWidget(widgetName, updatedViews);
        Log.d("MyClimateWidget", "widget updated with no info");
    }
}
