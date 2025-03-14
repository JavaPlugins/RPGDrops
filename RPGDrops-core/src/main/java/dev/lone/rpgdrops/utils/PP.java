package dev.lone.rpgdrops.utils;

/**
 * Utility class to simulate C++ pointers.
 *
 * @param <T> type of the pointed variable.
 */
public class PP<T>
{
    /**
     * Variable pointed to by this {@link PP}.
     */
    public T v;

    /**
     * Initialize the pointer.
     *
     * @param varToPointTo variable to point to.
     */
    PP(T varToPointTo)
    {
        v = varToPointTo;
    }

    /**
     * Point to a variable.
     *
     * @param varToPointTo variable to point to
     * @param <T> type of the pointed variable
     * @return the new pointer instance.
     */
    public static <T> PP<T> point(T varToPointTo)
    {
        return new PP<>(varToPointTo);
    }

    /**
     * Creates an empty pointer.
     *
     * @param <T> future pointed variable type.
     * @return the new pointer instance.
     */
    public static <T> PP<T> empty()
    {
        return new PP<>(null);
    }

    /**
     * Check if the current {@link PP} points to a variable.
     * @return true if the current {@link PP} points to a variable.
     */
    public boolean points()
    {
        return v != null;
    }

    /**
     * Changes the pointed variable/value.
     * You can also directly set v instead.
     *
     * @param varToPointTo variable to point to.
     */
    public void to(T varToPointTo)
    {
        this.v = varToPointTo;
    }
}
