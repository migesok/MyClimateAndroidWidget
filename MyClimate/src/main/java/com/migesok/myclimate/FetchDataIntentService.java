package com.migesok.myclimate;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import static com.migesok.myclimate.MyClimateAppWidgetProvider.ACTION_FETCH_RESULT;
import static com.migesok.myclimate.MyClimateAppWidgetProvider.EXTRA_FETCH_SUCCESS;
import static com.migesok.myclimate.MyClimateAppWidgetProvider.EXTRA_OUTSIDE_TEMP;

public class FetchDataIntentService extends IntentService {
    public FetchDataIntentService() {
        super("FetchDataIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("MyClimateWidget.FetchDataIntentService", "fetching data");
        boolean success = false;
        float outsideTemperature = Float.NaN;

        try {
            outsideTemperature = new WeatherNsuClient().getCurrentTemperature();
            success = true;
        } catch (Exception e) {
            Log.d("MyClimateWidget.FetchDataIntentService", "temperature acquisition failed", e);
        }

        Intent resultIntent = new Intent();
        resultIntent.setAction(ACTION_FETCH_RESULT);
        resultIntent.setComponent(new ComponentName(this, MyClimateAppWidgetProvider.class));
        resultIntent.addCategory(Intent.CATEGORY_DEFAULT);
        resultIntent.putExtra(EXTRA_FETCH_SUCCESS, success);
        resultIntent.putExtra(EXTRA_OUTSIDE_TEMP, outsideTemperature);
        sendBroadcast(resultIntent);
        Log.d("MyClimateWidget.FetchDataIntentService", "data fetch result sent");
    }
}
