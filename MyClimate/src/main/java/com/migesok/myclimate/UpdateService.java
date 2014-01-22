package com.migesok.myclimate;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class UpdateService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new UpdateTemperatureTask().execute();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class UpdateTemperatureTask extends AsyncTask<Object, Void, Double> {
        @Override
        protected Double doInBackground(Object... params) {
            Log.d("MyClimateWidget.UpdateTemperatureTask", "task started");
            try {
                return new WeatherNsuClient().getCurrentTemperature();
            } catch (IOException e) {
                Log.e("MyClimateWidget.UpdateTemperatureTask", "temperature acquisition failed", e);
            } catch (XmlPullParserException e) {
                Log.e("MyClimateWidget.UpdateTemperatureTask", "temperature acquisition failed", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Double outsideTemperature) {
            //TODO: what if outside temperature == null?
            RemoteViews updateViews = buildViewUpdate(outsideTemperature);
            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(getApplication(), MyClimateAppWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(getApplication());
            manager.updateAppWidget(thisWidget, updateViews);
            Log.d("MyClimateWidget.UpdateTemperatureTask", "widget updated");
        }

        public RemoteViews buildViewUpdate(double outsideTemperature) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.myclimate_appwidget);
            views.setTextViewText(R.id.textView, String.format("%.1f °C", outsideTemperature));
            return views;
        }
    }
}
