package dev.lone.rpgdrops.utils;

import org.jetbrains.annotations.NotNull;
import dev.lone.rpgdrops.Main;
import dev.lone.rpgdrops.utils.annotation.UnstableHack;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Wrapper for the Bukkit internal scheduler
 */
public class Scheduler
{
    //Class name, BukkitTask
    private static final HashMap<String, List<BukkitTask>> scheduled = new HashMap<>();
//    private static final HashMap<String, List<Thread>> scheduledThreads = new HashMap<>();

    private static final ExecutorService exe = Executors.newCachedThreadPool();

    /**
     * Returns the fully qualified name of the class which called
     * the function you call this from.
     *
     * @return the fully qualified name of the {@code Class}.
     */
    private static String getCallerClassName()
    {
        // 0 = Thread
        // 1 = Scheduler(this class)
        // 2 = Scheduler(this class)
        // 3 = The actual caller class
        return Thread.currentThread().getStackTrace()[3].getClassName(); //is it reliable?
    }

    /**
     * Unregisters all the scheduled tasks for a particular class.
     *
     * WARNING: It will unregister all the tasks for every instance!
     * Use with caution if you have multiple instances of that class.
     *
     * @param clazz The class which registered the tasks.
     */
    @UnstableHack
    public static void unregisterTasks(Class clazz)
    {
        List<BukkitTask> bukkitTasks = scheduled.get(clazz.getName());
        if (bukkitTasks != null)
        {
            ListIterator<BukkitTask> iter = bukkitTasks.listIterator();
            BukkitTask entry;
            while (iter.hasNext())
            {
                entry = iter.next();
                if (entry != null)
                    entry.cancel();
                iter.remove();
            }
        }

        //scheduledThreads.get().....
    }

    public static @NotNull BukkitScheduler get()
    {
        return Bukkit.getServer().getScheduler();
    }

    public static void async(Runnable task)
    {
        Bukkit.getScheduler().runTaskAsynchronously(Main.inst(), task);
    }

    public static void async(Runnable task, long delayTicks)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.inst(), task, delayTicks);
    }

    /**
     * I think this is bugged, fucking Spigot bug (tested on 1.17.1). https://www.spigotmc.org/threads/runtasktimerasynchronously-not-running.151085/
     */
//    @Deprecated
//    public static void asyncLoop(Consumer<BukkitTask> task, long delayTicks, long delay)
//    {
//        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), task, delayTicks, delay);
//    }

    /**
     * 101% working alternative to asyncLoop function up here.
     * @return
     */
    public static BukkitTask asyncLoop(Consumer<BukkitRunnable> task, long delayTicks, long delay)
    {
        BukkitTask bukkitTask = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                task.accept(this);
            }
        }.runTaskTimerAsynchronously(Main.inst(), delayTicks, delay);
        scheduled.computeIfAbsent(getCallerClassName(), k -> new ArrayList<>()).add(bukkitTask);
        return bukkitTask;
    }

    public static void sync(Runnable task)
    {
        Bukkit.getScheduler().runTask(Main.inst(), task);
    }

    public static void sync(Runnable task, long delayTicks)
    {
        Bukkit.getScheduler().runTaskLater(Main.inst(), task, delayTicks);
    }

    public static void delaySyncIf(Runnable task, long delayTicks, boolean b)
    {
        if(b)
            Bukkit.getScheduler().runTaskLater(Main.inst(), task, delayTicks);
        else
            task.run();
    }

    public static BukkitTask syncLoop(Consumer<BukkitRunnable> task, long delayTicks, long delay)
    {
        BukkitTask bukkitTask = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                task.accept(this);
            }
        }.runTaskTimer(Main.inst(), delayTicks, delay);
        scheduled.computeIfAbsent(getCallerClassName(), k -> new ArrayList<>()).add(bukkitTask);
        return bukkitTask;
    }

    public static void syncLoop(Runnable task, long delay)
    {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(Main.inst(), task, delay, delay);
        scheduled.computeIfAbsent(getCallerClassName(), k -> new ArrayList<>()).add(bukkitTask);
    }

    /**
     * Runs an async task without any delay and without using the bukkit internal threads utility.
     *
     * WARNING: doesn't intercept exceptions! They are silently thrown
     * @param task the task to execute.
     */
    public static void asyncNonBukkitIf(Runnable task, boolean b)
    {
        if(b)
            exe.submit(task);
        else
            task.run();
    }

    /**
     * Runs an async task without any delay and without using the bukkit internal threads utility.
     *
     * WARNING: doesn't intercept exceptions! They are silently thrown
     * @param task the task to execute.
     */
    public static void asyncNonBukkit(Runnable task)
    {
        exe.submit(task);
    }

    /**
     * Runs an async task with a delay and without using the bukkit internal threads utility.
     *
     * WARNING: doesn't intercept exceptions! They are silently thrown
     *
     * @param task the task to execute.
     * @param delayMs delay.
     */
    public static void asyncNonBukkit(Runnable task, int delayMs)
    {
        exe.submit(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {}
            task.run();
        });
    }

    public static void syncDelayedif (Runnable runnable, boolean b)
    {
        syncDelayedif (runnable, b, 0L);
    }

    public static void syncDelayedif (Runnable runnable, boolean b, long delay)
    {
        if (b)
            Scheduler.sync(runnable, delay);
        else
            runnable.run();
    }

//    public static Thread asyncThread(Runnable task)
//    {
//        Thread thread = new Thread(task);
//        thread.start();
//        scheduledThreads.computeIfAbsent(getCallerClassName(), k -> new ArrayList<>()).add(thread);
//        return thread;
//    }
}
