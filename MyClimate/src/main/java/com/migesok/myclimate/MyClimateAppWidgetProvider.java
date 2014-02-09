package com.migesok.myclimate;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static java.lang.String.format;

public class MyClimateAppWidgetProvider extends AppWidgetProvider {
    private static final long MAX_TIME_WITHOUT_UPDATE_MS = 1000L * 60L * 60L * 5L; // 5h

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (isNetworkAvailable(context)) {
            new UpdateTemperatureTask(context).execute();
        } else if (isInfoOutdated(context)) {
            updateViewsWithNoInfo(context, appWidgetManager);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.widget_state_pref_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
        Log.d("MyClimateWidget.onDeleted", "widget state cleared");
    }

    private boolean isInfoOutdated(Context context) {
        return (System.currentTimeMillis() - getLastUpdateTs(context)) > MAX_TIME_WITHOUT_UPDATE_MS;
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private long getLastUpdateTs(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.widget_state_pref_file), Context.MODE_PRIVATE);
        return sharedPref.getLong(context.getString(R.string.last_update_ts_key), 0L);
    }

    private void updateLastUpdateTs(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.widget_state_pref_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(context.getString(R.string.last_update_ts_key), System.currentTimeMillis());
        editor.commit();
        Log.d("MyClimateWidget", "last update timestamp updated");
    }

    private void updateViewsWithInfo(Context context,
                                     AppWidgetManager manager,
                                     double outsideTemperature) {
        RemoteViews updatedViews =
                new RemoteViews(context.getPackageName(), R.layout.myclimate_appwidget);
        updatedViews.setTextViewText(R.id.textView,
                format(context.getString(R.string.info_widget_text), outsideTemperature));
        ComponentName widgetName = new ComponentName(context, MyClimateAppWidgetProvider.class);
        manager.updateAppWidget(widgetName, updatedViews);
        Log.d("MyClimateWidget", "widget updated with info");
    }

    private void updateViewsWithNoInfo(Context context,
                                       AppWidgetManager manager) {
        RemoteViews updatedViews =
                new RemoteViews(context.getPackageName(), R.layout.myclimate_appwidget);
        updatedViews.setTextViewText(R.id.textView, context.getString(R.string.no_info_widget_text));
        ComponentName widgetName = new ComponentName(context, MyClimateAppWidgetProvider.class);
        manager.updateAppWidget(widgetName, updatedViews);
        Log.d("MyClimateWidget", "widget updated with no info");
    }

    private class UpdateTemperatureTask extends AsyncTask<Object, Void, Double> {
        private final Context context;

        private UpdateTemperatureTask(Context context) {
            this.context = context;
        }

        @Override
        protected Double doInBackground(Object... params) {
            Log.d("MyClimateWidget.UpdateTemperatureTask", "task started");
            try {
                return new WeatherNsuClient().getCurrentTemperature();
            } catch (IOException e) {
                Log.d("MyClimateWidget.UpdateTemperatureTask", "temperature acquisition failed", e);
            } catch (XmlPullParserException e) {
                Log.d("MyClimateWidget.UpdateTemperatureTask", "temperature acquisition failed", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Double outsideTemperature) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            if (outsideTemperature != null) {
                updateViewsWithInfo(context, manager, outsideTemperature);
                updateLastUpdateTs(context);
            } else if (isInfoOutdated(context)) {
                updateViewsWithNoInfo(context, manager);
            }
            Log.d("MyClimateWidget.UpdateTemperatureTask", "task finished");
        }
    }
}
