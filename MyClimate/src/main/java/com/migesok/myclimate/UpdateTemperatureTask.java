package com.migesok.myclimate;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static java.lang.String.format;

public class UpdateTemperatureTask extends AsyncTask<Object, Void, Double> {
    private static final long MAX_TIME_WITHOUT_UPDATE_MS = 1000L * 60L * 60L * 5L; // 5h

    private final Context context;
    private final BroadcastReceiver.PendingResult pendingResult;

    public UpdateTemperatureTask(Context context, BroadcastReceiver.PendingResult pendingResult) {
        this.context = context;
        this.pendingResult = pendingResult;
    }

    @Override
    protected Double doInBackground(Object... params) {
        Log.d("MyClimateWidget.UpdateTemperatureTask", "task started");
        if (isNetworkAvailable()) {
            try {
                return new WeatherNsuClient().getCurrentTemperature();
            } catch (IOException e) {
                Log.d("MyClimateWidget.UpdateTemperatureTask", "temperature acquisition failed", e);
            } catch (XmlPullParserException e) {
                Log.d("MyClimateWidget.UpdateTemperatureTask", "temperature acquisition failed", e);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Double outsideTemperature) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (outsideTemperature != null) {
            updateViewsWithInfo(manager, outsideTemperature);
            updateLastUpdateTs();
            disableNetworkStateListener();
        } else {
            if (isInfoOutdated()) {
                updateViewsWithNoInfo(manager);
            }
            enableNetworkStateListener();
        }
        pendingResult.finish();
        Log.d("MyClimateWidget.UpdateTemperatureTask", "task finished");
    }

    private void enableNetworkStateListener() {
        ComponentName networkStateListener = new ComponentName(context, NetworkStateListener.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(networkStateListener,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void disableNetworkStateListener() {
        ComponentName networkStateListener = new ComponentName(context, NetworkStateListener.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(networkStateListener,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private boolean isInfoOutdated() {
        return (System.currentTimeMillis() - getLastUpdateTs()) > MAX_TIME_WITHOUT_UPDATE_MS;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private long getLastUpdateTs() {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.widget_state_pref_file), Context.MODE_PRIVATE);
        return sharedPref.getLong(context.getString(R.string.last_update_ts_key), 0L);
    }

    private void updateLastUpdateTs() {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.widget_state_pref_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(context.getString(R.string.last_update_ts_key), System.currentTimeMillis());
        editor.commit();
        Log.d("MyClimateWidget", "last update timestamp updated");
    }

    private void updateViewsWithInfo(AppWidgetManager manager, double outsideTemperature) {
        RemoteViews updatedViews =
                new RemoteViews(context.getPackageName(), R.layout.myclimate_appwidget);
        updatedViews.setTextViewText(R.id.textView,
                format(context.getString(R.string.info_widget_text), outsideTemperature));
        ComponentName widgetName = new ComponentName(context, MyClimateAppWidgetProvider.class);
        manager.updateAppWidget(widgetName, updatedViews);
        Log.d("MyClimateWidget", "widget updated with info");
    }

    private void updateViewsWithNoInfo(AppWidgetManager manager) {
        RemoteViews updatedViews =
                new RemoteViews(context.getPackageName(), R.layout.myclimate_appwidget);
        updatedViews.setTextViewText(R.id.textView, context.getString(R.string.no_info_widget_text));
        ComponentName widgetName = new ComponentName(context, MyClimateAppWidgetProvider.class);
        manager.updateAppWidget(widgetName, updatedViews);
        Log.d("MyClimateWidget", "widget updated with no info");
    }
}
