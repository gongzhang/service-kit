package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Gong Zhang
 */
public abstract class Service {

    @NotNull private final String serviceName;
    @NotNull private final String tag;
    private boolean allowLog;

    private boolean started;

    private final Set<String> taskIdentifiers;

    protected Service(@NotNull String tag) {
        this(tag, tag);
    }

    protected Service(@NotNull String tag, @NotNull String serviceName) {
        this.tag = tag;
        this.serviceName = serviceName;
        this.started = false;
        this.taskIdentifiers = new HashSet<>();
    }

    protected String getTag() {
        return tag;
    }

    protected final String getServiceName() {
        return serviceName;
    }

    protected synchronized boolean isAllowLog() {
        return allowLog;
    }

    protected synchronized void setAllowLog(boolean allowLog) {
        this.allowLog = allowLog;
    }

    public synchronized final boolean isStarted() {
        return started;
    }

    public synchronized final boolean start() {
        if (started) {
            if (allowLog) Log.e(tag, serviceName + " is already running.");
            return false;
        }

        try {
            onServiceStart();
        } catch (Exception e) {
            if (allowLog) Log.e(tag, "Failed to start " + serviceName + ".", e);
            return false;
        }

        started = true;
        if (allowLog) Log.i(tag, serviceName + " started.");
        return true;
    }

    public synchronized final void stop() {
        if (!started) {
            if (allowLog) Log.e(tag, serviceName + " is not running.");
            return;
        }

        try {
            onServiceStop();
        } catch (Exception e) {
            if (allowLog) Log.w(tag, "Error occurred when stop " + serviceName + ".", e);
        }

        taskIdentifiers.clear();
        started = false;
        if (allowLog) Log.i(tag, serviceName + " stopped.");
    }

    protected abstract void onServiceStart() throws Exception;
    protected abstract void onServiceStop() throws Exception;

    /**
     * Executes a task asynchronously if and only if:
     * <ul>
     *     <li>the service is running</li>
     * </ul>
     * @param runnable the task
     * @return {@code true} if the task is accepted
     */
    protected final synchronized boolean async(@NotNull Runnable runnable) {
        if (started) {
            ThreadPool.execute(() -> {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    if (allowLog) Log.e(tag, "Unhandled exception occurred when executing async task.", ex);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    /**
     * Executes a task asynchronously if and only if:
     * <ul>
     *     <li>the service is running</li>
     *     <li>there is no running task with same identifier</li>
     * </ul>
     * Otherwise the task will be ignored.
     *
     * @param identifier the task identifier
     * @param taskSupplier a lambda that creates and returns the task.
     *                     If {@code null} is returned, nothing will be
     *                     executed. The {@code Runnable} passed in the
     *                     lambda is a <strong>completion handler</strong>,
     *                     which <strong>must</strong> be called after
     *                     task completion. This lambda will be invoked
     *                     synchronously when the service keeps running.
     *                     So this lambda is also an opportunity to do
     *                     preparation before the async-task is executed.
     * @return {@code true} if the task is accepted
     */
    protected final synchronized boolean asyncSingle(@NotNull String identifier, @NotNull Function<Runnable, Runnable> taskSupplier) {
        if (!taskIdentifiers.contains(identifier)) {

            Runnable complete = () -> {
                synchronized (Service.this) {
                    taskIdentifiers.remove(identifier);
                }
            };

            Runnable task = taskSupplier.apply(complete);
            if (task == null) {
                return false;
            }

            taskIdentifiers.add(identifier);

            async(task);

            return true;
        } else {
            return false;
        }
    }

}
