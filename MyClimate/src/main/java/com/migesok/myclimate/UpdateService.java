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
        Log.d("WordWidget.UpdateService", "onStart()");
        new UpdateTemperatureTask().execute("wtf");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class UpdateTemperatureTask extends AsyncTask<String, Void, Double> {
        @Override
        protected Double doInBackground(String... urls) {
            try {
                return new WeatherNsuClient().getCurrentTemperature();
            } catch (IOException e) {
                Log.e("WordWidget.UpdateService", "we're screwed", e);
            } catch (XmlPullParserException e) {
                Log.e("WordWidget.UpdateService", "we're screwed", e);
            }
            return null;
        }

       @Override
        protected void onPostExecute(Double outsideTemperature) {
           RemoteViews updateViews = buildViewUpdate(outsideTemperature);
           // Push update for this widget to the home screen
           ComponentName thisWidget = new ComponentName(getApplication(), MyClimateAppWidgetProvider.class);
           AppWidgetManager manager = AppWidgetManager.getInstance(getApplication());
           manager.updateAppWidget(thisWidget, updateViews);
           Log.d("WordWidget.UpdateService", "widget updated");
        }

        /**
         * Build a widget update to show the current Wiktionary
         * "Word of the day." Will block until the online API returns.
         */
        public RemoteViews buildViewUpdate(double outsideTemperature) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.myclimate_appwidget);
            views.setTextViewText(R.id.textView, String.format("%.1f °C", outsideTemperature));
            return views;
        }
    }
}
