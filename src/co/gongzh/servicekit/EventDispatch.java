package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Gong Zhang
 */
public final class EventDispatch<T> {

    public static final class Event<T> {

        @NotNull public final EventDispatch<T> dispatch;
        @Nullable public final T arg;
        public final long id;

        Event(@NotNull EventDispatch<T> dispatch, @Nullable T arg, long id) {
            this.dispatch = dispatch;
            this.arg = arg;
            this.id = id;
        }

    }

    public static final class ExceptionInfo {

        @NotNull public final Exception exception;
        @NotNull public final EventObserver<?> observer;

        ExceptionInfo(@NotNull Exception exception,
                      @NotNull EventObserver<?> observer) {
            this.exception = exception;
            this.observer = observer;
        }

    }

    public static final EventDispatch<ExceptionInfo> onException = new EventDispatch<>("EventDispatch.onException");

    private final LinkedHashMap<Integer, EventObserver<T>> observers;
    private int nextObserverId = 1;
    private long currentEventId = 1;
    @NotNull
    private final String name;

    public EventDispatch(@NotNull String name) {
        this.observers = new LinkedHashMap<>();
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public synchronized int addObserver(@NotNull EventObserver<T> observer) {
        int id = nextObserverId++;
        observers.put(id, observer);
        return id;
    }

    @Nullable
    public synchronized EventObserver<T> removeObserver(int id) {
        return observers.remove(id);
    }

    public synchronized boolean removeObserver(@NotNull EventObserver<?> observer) {
        Integer id = null;
        for (Map.Entry<Integer, EventObserver<T>> entry : observers.entrySet()) {
            if (entry.getValue() == observer) {
                id = entry.getKey();
                break;
            }
        }

        if (id != null) {
            removeObserver(id);
            return true;
        } else {
            return false;
        }
    }

    public synchronized void fire(@Nullable T arg, @Nullable Consumer<Long> eventIdHandler) {

        // capture current states
        final long eid = currentEventId++;
        Event<T> e = new Event<T>(this, arg, eid);
        List<EventObserver<T>> copied = new ArrayList<>(observers.values());


        if (eventIdHandler != null) {
            eventIdHandler.accept(eid);
        }

        for (EventObserver<T> obs : copied) {
            try {
                obs.notify(e);
            } catch (Exception ex) {
                onException.fire(new ExceptionInfo(ex, obs));
            }
        }
    }

    public final void fire(T arg) {
        fire(arg, null);
    }

    public void observeOnce(@NotNull EventObserver<T> observer) {
        final int[] obs = { 0 };
        obs[0] = addObserver(event -> {
            observer.notify(event);
            this.removeObserver(obs[0]);
        });
    }

}
