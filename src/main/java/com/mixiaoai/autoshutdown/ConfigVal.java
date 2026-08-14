package com.mixiaoai.autoshutdown;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A configuration value whose current value is backed by the platform config spec.
 * Defaults are used until the platform module binds it to its spec value.
 */
public class ConfigVal<T>
{
    private final T defaultValue;
    private Supplier<T> getter;
    private Consumer<T> setter;

    ConfigVal(T defaultValue)
    {
        this.defaultValue = defaultValue;
        this.getter = () -> defaultValue;
        this.setter = value -> { };
    }

    public T get()
    {
        return getter.get();
    }

    public void set(T value)
    {
        setter.accept(value);
    }

    public T getDefault()
    {
        return defaultValue;
    }

    /** Wires this value to a platform config value */
    public void bind(Supplier<T> getter, Consumer<T> setter)
    {
        this.getter = getter;
        this.setter = setter;
    }
}
