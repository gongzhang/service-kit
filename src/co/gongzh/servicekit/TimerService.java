package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Gong Zhang
 */
public abstract class TimerService extends Service {

    @Nullable private Instant lastTimerAction = null;
    @Nullable private Timer timer;
    private long currentTimerPeriod = 0;

    protected TimerService(@NotNull String tag) {
        this(tag, tag);
    }

    protected TimerService(@NotNull String tag, @NotNull String serviceName) {
        super(tag, serviceName);
    }

    @Override
    protected final void onServiceStart() throws Exception {
        onTimerServiceStart();

        lastTimerAction = null;
        currentTimerPeriod = getTimerPeriod();
        timer = new Timer(getServiceName() + ".Timer", false);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timerAction();
            }
        }, getTimerStartDelay(), currentTimerPeriod);
    }

    @Override
    protected final void onServiceStop() throws Exception {
        if (timer != null) {
            timer.cancel();
            timer = null;
            lastTimerAction = null;
        }

        onTimerServiceStop();
    }

    private synchronized void timerAction() {
        if (timer == null) {
            // already stopped
            return;
        }

        Instant now = Instant.now();
        Long interval = null;
        if (lastTimerAction != null) {
            long dur = Duration.between(lastTimerAction, now).toMillis();
            interval = dur;

            if (dur * 3 / 2 < currentTimerPeriod) {
                // too early
                return;
            }
        }

        boolean handled = false;
        try {
            handled = onTimerAction(interval);
            lastTimerAction = now;
        } catch (Exception ex) {
            if (isAllowLog()) Log.e(getTag(), "Error occurred in timer action of " + getServiceName() + ".", ex);
        }
    }

    protected abstract void onTimerServiceStart() throws Exception;
    protected abstract void onTimerServiceStop() throws Exception;

    /**
     * Returns the timer period in millisecond. This method is called
     * by <code>TimerService.start()</code> method.
     * @return timer period in millisecond
     */
    protected abstract long getTimerPeriod();

    /**
     * Indicates the delay of first timer event when start timer service.
     * Returns 0 by default.
     * @return delay in millisecond
     */
    protected long getTimerStartDelay() {
        return 0;
    }

    /**
     * Notify client to do periodical task. The implementation should
     * not block current thread.
     * @param interval the interval between last valid timer action, can be {@code null}
     * @return {@code true} if this action is accepted by client, otherwise returns {@code false}
     */
    protected abstract boolean onTimerAction(@Nullable Long interval);

}
