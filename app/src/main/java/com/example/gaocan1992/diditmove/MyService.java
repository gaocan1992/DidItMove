package com.example.gaocan1992.diditmove;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.example.gaocan1992.diditmove.MyServiceTask.ResultCallback;


public class MyService extends Service {

    private static final String LOG_TAG = "MyService";

    // Handle to notification manager.
    private NotificationManager notificationManager;
    private int ONGOING_NOTIFICATION_ID = 1; // This cannot be 0. So 1 is a good candidate.

    // Motion detector thread and runnable.
    private Thread myThread;
    private MyServiceTask myTask;

    // Binder given to clients
    private final IBinder myBinder = new MyBinder();

    PowerManager.WakeLock wakelock;

    public long first_accel_time;
    public long service_start_time;
    public boolean moved;

    // Binder class.
    public class MyBinder extends Binder {
        MyService getService() {
            // Returns the underlying service.
            return MyService.this;
        }
    }

    public MyService() {
        service_start_time = System.currentTimeMillis();
        first_accel_time = -1;
        moved = false;
    }

    public void initializeService() {
        service_start_time = System.currentTimeMillis();
        first_accel_time = -1;
        moved = false;
    }

    @Override
    public void onCreate() {

        Log.i(LOG_TAG, "Service is being created");
        // Display a notification about us starting.  We put an icon in the status bar.
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showMyNotification();

        // Creates the thread running the camera service.
        myTask = new MyServiceTask(getApplicationContext());
        myThread = new Thread(myTask);
        myThread.start();

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "Service is being bound");
        // Returns the binder to this service.
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakelock.acquire();
        // We start the task thread.
        if (!myThread.isAlive()) {
            myThread.start();
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        wakelock.release();
        // Cancel the persistent notification.
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);
        Log.i(LOG_TAG, "Stopping.");
        // Stops MyServiceTask.
        myTask.stopProcessing();
        Log.i(LOG_TAG, "Stopped.");
    }

    public void didItMove(ServiceResult result) {
        long detected_time = result.time;
        if(first_accel_time == -1 && result.intValue == 1 && detected_time - service_start_time > 30000) {
            first_accel_time = detected_time;
            moved = true;
        }
    }

//     Interface to be able to subscribe to the bitmaps by the service.

    public void releaseResult(ServiceResult result) {
        myTask.releaseResult(result);
    }

    public void addResultCallback(ResultCallback resultCallback) {
        myTask.addResultCallback(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {
        myTask.removeResultCallback(resultCallback);
    }


    /**
     * Show a notification while this service is running.
     */
    @SuppressWarnings("deprecation")
    private void showMyNotification() {

        // Creates a notification.
        Notification notification = new Notification(
                R.mipmap.notification,
                getString(R.string.my_service_started),
                System.currentTimeMillis());

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getText(R.string.notification_title),
                getText(R.string.my_service_running), pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

}
