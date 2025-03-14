package dev.lone.rpgdrops.nms;

import dev.lone.LoneLibs.p;
import lonelibs.dev.lone.fastnbt.nms.Version;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.security.CodeSource;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static dev.lone.rpgdrops.Main.msg;

@SuppressWarnings("SameParameterValue")
public abstract class Implementation
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface CyclicDependency
    {
        Class<?> type();
        Version version();
    }

    private static final String basePackageNonObf = Implementation.class.getPackageName();

    public static <T> T get(Class<T> type)
    {
        return get(type, false);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T get(Class<T> type, boolean ignoreError)
    {
        Class<?> implClass = getImplClass(type, Version.get());
        if(implClass == null)
        {
            handleError("Failed to find nms implementation for:", type, ignoreError, null);
            return null;
        }
        else
        {
            try
            {
                return (T) implClass.getDeclaredConstructor().newInstance();
            }
            catch (Throwable e)
            {
                handleError("Failed to instantiate nms implementation for:", type, ignoreError, e);
            }
        }

        return null;
    }

    public static <T> T get(Class<T> type, Object... values)
    {
        return get(type, false, values);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T get(Class<T> type, boolean ignoreError, Object... values)
    {
        Class<?> implClass = getImplClass(type, Version.get());
        if(implClass == null)
        {
            handleError("Failed to find nms implementation for:", type, ignoreError, null);
            return null;
        }

        Class<?>[] args = new Class<?>[values.length];
        for (int i = 0; i < values.length; i++)
            args[i] = values[i].getClass();

        Constructor<Object> constructor = Nms.findConstructor(implClass, args);
        if(constructor == null)
        {
            handleError("Failed to find nms implementation constructor for:", type, ignoreError, null);
        }
        else
        {
            try
            {
                return (T) constructor.newInstance(values);
            }
            catch (Throwable e)
            {
                handleError("Failed to instantiate nms implementation for:", type, ignoreError, e);
            }
        }

        return null;
    }

    public static <T> Class<?> getImplClass(Class<T> type, Version version)
    {
        p<Class<?>> implementationClass = p.empty();
        iterateClassesInPackage(basePackageNonObf, (clazz) -> {
            if (!clazz.isAnnotationPresent(CyclicDependency.class))
                return;

            CyclicDependency annotation = clazz.getAnnotation(CyclicDependency.class);
            if (type.equals(annotation.type()) && annotation.version() == version)
                implementationClass.to(clazz);
        });

        return implementationClass.v;
    }

    public static <T> Constructor<T> constructor(Class<T> type, Class<?>... args)
    {
        return constructor(type, false, args);
    }

    public static <T> Constructor<T> constructor(Class<T> type, boolean ignoreError, Class<?>... args)
    {
        Class<?> implClass = getImplClass(type, Version.get());
        return Nms.findConstructor(implClass, args);
    }

    private static <T> void handleError(String text, Class<T> type, boolean ignoreError, @Nullable Throwable e)
    {
        msg.error(text + " "  + type);
        if(!ignoreError)
        {
//                Bukkit.shutdown(); // TODO: enable!
            if(e != null)
                throw new RuntimeException(e);
            else
                throw new RuntimeException();
        }
    }

    /**
     * Lists all classes in the current JAR which are in a particular package.
     */
    private static void iterateClassesInPackage(String basePackageNonObf, Consumer<Class<?>> run)
    {
        ClassLoader classLoader = Implementation.class.getClassLoader();
        CodeSource src = Implementation.class.getProtectionDomain().getCodeSource();
        if (src == null)
            return;
        URL jar = src.getLocation();
        ZipInputStream zip;
        try
        {
            zip = new ZipInputStream(jar.openStream());
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
            {
                String name = entry.getName();
                if (!name.endsWith(".class"))
                    continue;

                name = name
                        .replace("\\", ".")
                        .replace("/", ".")
                        .replace(".class", "");
                if(name.startsWith(basePackageNonObf + "."))
                {
                    try
                    {
                        run.accept(classLoader.loadClass(name));
                    }
                    catch (ClassNotFoundException | NoClassDefFoundError ignored)
                    {
                        // ClassNotFoundException:
                        //      Skip if not a class for some reason.
                        // NoClassDefFoundError:
                        //      Happens if impl class extends an NMS class which is not available
                        //      in the current game version.
                    }
                }
            }
        }
        catch (IOException e)
        {
            msg.error("Error getting list of classes in JAR", e);
        }
    }
}