package com.android.scheduletask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.Calendar;
import java.util.HashMap;

/**
 * Utility class for Schedule Task.
 */
public class ScheduleTask {

    private static final String TAG = "ScheduleTask";
    private static HashMap<IBinder, PendingIntent> sAlarms;

    /**
     * Schedule a periodic task by pass a Calendar time, to run a callback task.
     *
     * @param context will help get Alarm Service startup a scheduled task.
     * @param alarmTime is a fixed Calendar time that you want to run a upcoming aciton.
     * @param period startup a periodic task, the period unit is day, 30 indicate task run once every 30 days.
     * @param callback is the function entrance you want to run the action while the scheduled time timeout.
     * @return token record the correspoding pendingIntent, cancel it will use this token.
     */
    public static synchronized IBinder installPeriodic(Context context, Calendar alarmTime,
                                                       int period, ICallBack callback) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        Intent intent = new Intent("com.opzoon.scheduletask");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * period, pendingIntent);

        final int MESSAGE_RUN = 0;
        BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "installPeriodic() onReceive()");

                HandlerThread task = new HandlerThread("Scheduler");
                task.start();
                new Handler(task.getLooper()) {
                    public void handleMessage(Message msg) {
                        if (msg.what == MESSAGE_RUN) {
                            callback.call();
                        }
                    }
                }.sendEmptyMessage(MESSAGE_RUN);
            }
        };

        IntentFilter intentFilter = new IntentFilter("com.opzoon.scheduletask");
        context.registerReceiver(myBroadcastReceiver, intentFilter);

        IBinder token = new Binder();
        if (sAlarms == null) {
            sAlarms = new HashMap<IBinder, PendingIntent>();
        }
        sAlarms.put(token, pendingIntent);

        return token;
    }

    /**
     * Cancels the scheduled task.
     *
     * @param context get context to cancel pendingIntent.
     * @param token and pendingIntent is a mapping relationship, get this token ID to cancel
     *              the pendingIntent, and need remove token itself at the same time.
     */
    public static synchronized void cancel(Context context, IBinder token) {

        if (sAlarms != null) {
            PendingIntent pendingIntent = sAlarms.get(token);
            if (pendingIntent != null) {
                sAlarms.remove(token);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
            } else {
                return;
            }
        }
    }

    /**
     * Schedule a task by pass a Calendar time, to run a callback task.
     *
     * @param context will help get Alarm Service startup a scheduled task.
     * @param alarmTime is a fixed Calendar time that you want to run an upcoming task.
     * @param callback is the function entrance you want to run the action while the scheduled time timeout.
     * @return token record the correspoding pendingIntent, cancel it will use this token.
     */
    public static synchronized IBinder install(Context context, Calendar alarmTime, ICallBack callback) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        Intent intent = new Intent("com.opzoon.scheduletask");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Log.d(TAG, "install() alarmTime.getTimeInMillis() " + alarmTime.getTimeInMillis());
        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);

        IBinder token = new Binder();
        if (sAlarms == null) {
            sAlarms = new HashMap<IBinder, PendingIntent>();
        }
        sAlarms.put(token, pendingIntent);
        BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "install() onReceive()");
                sAlarms.remove(token);
                context.unregisterReceiver(this);
                HandlerThread task = new HandlerThread("Scheduler");
                task.start();
                final int MESSAGE_RUN = 1;
                new Handler(task.getLooper()) {
                    public void handleMessage(Message msg) {
                        if (msg.what == MESSAGE_RUN) {
                            callback.call();
                        }
                    }
                }.sendEmptyMessage(MESSAGE_RUN);
            }
        };

        IntentFilter intentFilter = new IntentFilter("com.opzoon.scheduletask");
        context.registerReceiver(myBroadcastReceiver, intentFilter);

        return token;
    }
}
