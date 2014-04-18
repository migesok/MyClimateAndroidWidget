package com.migesok.myclimate;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class MyClimateAppWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("MyClimateWidget", "On update!");
        PendingResult pendingResult = goAsync();
        new UpdateTemperatureTask(context, pendingResult).execute();
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
}
