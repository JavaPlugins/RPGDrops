package dev.lone.rpgdrops.nms;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Utility to initialize NMS wrappers and avoid Maven circular dependency problems.
 */
public class Nms
{
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> Constructor<T> findConstructor(Class<?> clazz, Class<?> ...args)
    {
        // Find public/accessible constructor
        try
        {
            return (Constructor<T>) clazz.getConstructor(args);
        }
        catch (NoSuchMethodException ignored)
        {
            // Check if there is any private constructor
            try
            {

                Constructor<T> declaredConstructor = (Constructor<T>) clazz.getDeclaredConstructor(args);
                declaredConstructor.setAccessible(true);
                return declaredConstructor;
            }
            catch (NoSuchMethodException e)
            {
                return null;
            }
        }
    }

    @Nullable
    public static Field getField(Class<?> clazz, Class<?> type)
    {
        for (Field field : clazz.getDeclaredFields())
        {
            if (field.getType() == type)
            {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    @Nullable
    public static Field getField(Class<?> clazz, Class<?> type, int index)
    {
        Field[] declared = clazz.getDeclaredFields();
        int i = 0;
        for (Field entry : declared)
        {
            if (entry.getType() == type)
            {
                if(i == index)
                {
                    entry.setAccessible(true);
                    return entry;
                }
                i++;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T read(Field field, Object obj)
    {
        try
        {
            return (T) field.get(obj);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Failed to read field " + field.getName(), e);
        }
    }
}
