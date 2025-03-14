package dev.lone.rpgdrops;

import dev.lone.LoneLibs.chat.Msg;
import dev.lone.rpgdrops.customdrop.CustomDropsManager;
import dev.lone.rpgdrops.nms.packets.Packets;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener
{
    //DO NOT SET AS "final" OR SPIGOT.MC won't replace it.
    public static String b = "%%__USER__%%";

    private static Main inst;
    public static Msg msg;

    public static Main inst()
    {
        return inst;
    }

    @Override
    public void onEnable()
    {
        inst = this;
        msg = new Msg("[" + this.getName()+ "] ");

        Bukkit.getPluginManager().registerEvents(this, this);
        Packets.init();
        init();
    }

    @Override
    public void onDisable()
    {
        CustomDropsManager.inst().unhookAll();
    }

    public void init()
    {
        Settings.reload(this);
        CustomDropsManager.inst().init();
    }
}
