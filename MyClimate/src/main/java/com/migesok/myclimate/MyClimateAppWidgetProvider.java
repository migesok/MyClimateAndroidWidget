package com.migesok.myclimate;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class MyClimateAppWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        if (isNetworkAvailable(context)) {
            new UpdateTemperatureTask(context).execute();
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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
            ComponentName thisWidget = new ComponentName(context, MyClimateAppWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.updateAppWidget(thisWidget, updateViews);
            Log.d("MyClimateWidget.UpdateTemperatureTask", "widget updated");
        }

        public RemoteViews buildViewUpdate(double outsideTemperature) {
            RemoteViews views =
                    new RemoteViews(context.getPackageName(), R.layout.myclimate_appwidget);
            views.setTextViewText(R.id.textView, String.format("%.1f °C", outsideTemperature));
            return views;
        }
    }
}
