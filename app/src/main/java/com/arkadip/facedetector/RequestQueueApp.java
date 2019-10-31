package com.arkadip.facedetector;

import android.app.Application;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class RequestQueueApp extends Application {
    public static final String TAG = RequestQueueApp.class.getSimpleName();
    private RequestQueue requestQueue;

    private static RequestQueueApp requestQueueApp;

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueueApp = this;
    }

    public static synchronized RequestQueueApp getInstance() {
        return requestQueueApp;
    }

    public RequestQueue getRequestQueue(){
        if(requestQueue == null){
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> request, String tag){
        request.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(request);
    }

    public void cancelPendingRequests(Object tag){
        if (requestQueue != null) {
            requestQueue.cancelAll(tag);
        }
    }
}
