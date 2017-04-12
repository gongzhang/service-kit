package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;

/**
 * @author Gong Zhang
 */
@FunctionalInterface
public interface EventObserver<T> {

    void notify(@NotNull EventDispatch.Event<T> event);

}
