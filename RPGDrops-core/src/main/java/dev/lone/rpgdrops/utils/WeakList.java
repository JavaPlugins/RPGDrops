package dev.lone.rpgdrops.utils;

import java.util.*;

public class WeakList<T> extends ArrayList<T>
{
    public WeakList(Collection<? extends T> c)
    {
        super(c);
    }

    public static <K> WeakList<K> create(Class<K> clazz)
    {
        return new WeakList<>(Collections.newSetFromMap(new WeakHashMap<>()));
    }

    public static <K> WeakList<K> createSync(Class<K> clazz)
    {
        return new WeakList<>(Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>())));
    }
}