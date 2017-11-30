package com.example.mz.dialogprinter;

import android.widget.Toast;

import com.vk.sdk.api.VKRequest;

import java.util.ArrayDeque;

import static com.vk.sdk.VKUIHelper.getApplicationContext;

/**
 * Created by mz on 10/31/17.
 */

public class VkQueue extends Thread {
    private final Object runningLock = new Object();
    private boolean running = false;

    public boolean isRunning() {
        synchronized (runningLock) {
            return running;
        }
    }

    private class Request {
        VKRequest request;
        VKRequest.VKRequestListener listener;
    }

    private final Object queueLock = new Object();
    private ArrayDeque<Request> requestsQueue = new ArrayDeque<>();

    public void addRequest(VKRequest request, VKRequest.VKRequestListener listener) {
        Request requestEx = new Request();
        requestEx.request = request;
        requestEx.listener = listener;
        synchronized (queueLock) {
            requestsQueue.addLast(requestEx);
            queueLock.notifyAll();
        }
    }

    @Override
    public void run() {
        synchronized (runningLock) {
            running = true;
        }
        while (isRunning()) {
            Request request = null;
            synchronized (queueLock) {
                if (requestsQueue.isEmpty())
                    try {
                        queueLock.wait();
                    } catch (InterruptedException e) {
                        break;
                    }
            }
            request = requestsQueue.poll();
            if(request == null)
                continue;
            request.request.executeWithListener(request.listener);
            try {
                Thread.sleep(340);
            } catch (InterruptedException e) {
                break;
            }
        }
        synchronized (runningLock) {
            running = false;
        }
    }
}
