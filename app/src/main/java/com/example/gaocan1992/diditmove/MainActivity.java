package com.example.gaocan1992.diditmove;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity implements MyServiceTask.ResultCallback{


    private static final String LOG_TAG = "MainActivity";
    public static final int MOVEMENT_DETECTION_NUMBER = 10;
    private Handler mHandler;

    // Service connection variables.
    private boolean serviceBound;
    private MyService myService;
    TextView tv;
    TextView count;
    Button clear;
    Button exit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.textview);
        count = (TextView) findViewById(R.id.count_textview);
        clear = (Button) findViewById(R.id.clear_button);
        exit = (Button) findViewById(R.id.exit_button);
        serviceBound = false;
        myService = new MyService();
        mHandler = new Handler(getMainLooper(), new ServiceCallback());
        Intent intent = new Intent(this, MyService.class);
        startService(intent);
        bindMyService();

    }

    /**
     * This function is called from the service thread.  To process this, we need
     * to create a message for a handler in the UI thread.
     */
    @Override
    public void onResultReady(ServiceResult result) {
        if (result != null) {
            Log.i(LOG_TAG, "Preparing a message for " + result.intValue);
        } else {
            Log.e(LOG_TAG, "Received an empty result!");
        }
        mHandler.obtainMessage(MOVEMENT_DETECTION_NUMBER, result).sendToTarget();
    }

    public void clickClear(View view) {
        myService.initializeService();
    }

    public void clickExit(View view) {
        if (serviceBound) {
            if (myService != null) {
                myService.removeResultCallback(this);
            }
            Log.i("MyService", "Unbinding");
            unbindService(serviceConnection);
            serviceBound = false;
            // If we like, stops the service.
            if (true) {
                Log.i(LOG_TAG, "Stopping.");
                Intent intent = new Intent(this, MyService.class);
                stopService(intent);
                Log.i(LOG_TAG, "Stopped.");
            }
        }
        finish();
    }

    /**
     * This Handler callback gets the message generated above.
     * It is used to display the integer on the screen.
     */
    private class ServiceCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == MOVEMENT_DETECTION_NUMBER) {
                // Gets the result.
                ServiceResult result = (ServiceResult) message.obj;

                if (result != null) {
                    myService.didItMove(result);
                    long currentTime = System.currentTimeMillis();
                    String countdown = Integer.toString(30 -
                            (int) (currentTime - myService.service_start_time) / 1000);
                    if(Integer.parseInt(countdown) > 0) {
                        count.setText("Detector will start after " + countdown + " s!");
                    } else {
                        count.setText("Detecting!");
                    }
                    if(myService.moved && currentTime - myService.first_accel_time > 30000) {
                        tv.setText("Some one moved your phone!");
                    } else {
                        tv.setText("No one moved your phone!");
                    }
                    if (serviceBound && myService != null) {
                        Log.i(LOG_TAG, "Releasing result holder for " + result.intValue);
                        myService.releaseResult(result);
                    }
                } else {
                    Log.e(LOG_TAG, "Error: received empty message!");
                }
            }
            return true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (serviceBound) {
            if (myService != null) {
                myService.removeResultCallback(this);
            }
            Log.i("MyService", "Unbinding");
            unbindService(serviceConnection);
            serviceBound = false;
            // If we like, stops the service.
            if (true) {
                Log.i(LOG_TAG, "Stopping.");
                Intent intent = new Intent(this, MyService.class);
                stopService(intent);
                Log.i(LOG_TAG, "Stopped.");
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void bindMyService() {
        // We are ready to show images, and we should start getting the bitmaps
        // from the motion detection service.
        // Binds to the service.
        Log.i(LOG_TAG, "Starting the service");
        Intent intent = new Intent(this, MyService.class);
        Log.i("LOG_TAG", "Trying to bind");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    // Service connection code.
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            // We have bound to the service.
            MyService.MyBinder binder = (MyService.MyBinder) serviceBinder;
            myService = binder.getService();
            serviceBound = true;
            // Let's connect the callbacks.
            Log.i("MyService", "Bound succeeded, adding the callback");
            myService.addResultCallback(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
