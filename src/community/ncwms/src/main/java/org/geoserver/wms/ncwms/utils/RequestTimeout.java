package org.geoserver.wms.ncwms.utils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * An utility class that can be used to set a strict timeout on requests: the subclasses can override the timeout() method to perform specific actions
 * on timeout.
 * 
 * @author Cesar Martinez Izquierdo (inspired by {@link org.geoserver.wms.map.RenderingTimeoutEnforcer})
 */
public class RequestTimeout {
    protected boolean timedOut = false;

    protected long timeout;

    protected Timer timer;

    public RequestTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Starts checking the timeout (if timeout is positive, does nothing otherwise)
     */
    public void start() {
        if (timer != null)
            throw new IllegalStateException("The timeout enforcer has already been started");

        if (timeout > 0) {
            timedOut = false;
            timer = new Timer();
            timer.schedule(new TaskFinished(), timeout);
        }
    }

    /**
     * Stops the timeout check
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    /**
     * Returns true if the renderer has been stopped mid-way due to the timeout occurring
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * Can be overridden to perform specific actions on timeout
     */
    protected void timeout() {
    }

    class TaskFinished extends TimerTask {

        @Override
        public void run() {
            RequestTimeout.this.timedOut = true;
            timeout();
        }

    }
}