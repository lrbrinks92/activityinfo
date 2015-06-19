package cucumber.runtime.parallel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class Event {

    /**
     * Reporter or Formatter implementation
     */
    private Object listener;
    private Method method;
    private Object[] args;

    public Event(Object listener, Method method, Object[] args) {
        this.listener = listener;
        this.method = method;
        this.args = args;
    }

    public void fire() {
        try {
            method.invoke(listener, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
}
