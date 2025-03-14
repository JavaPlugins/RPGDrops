package dev.lone.rpgdrops.nms.packets;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import dev.lone.LoneLibs.data.WeakList;
import dev.lone.rpgdrops.nms.Implementation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public final class Packets
{
    public static final List<Object> ignored = WeakList.create(Object.class);

    @SuppressWarnings("rawtypes")
    public IPackets nms;
    static Packets instance;

    Packets()
    {
        nms = Implementation.get(IPackets.class);
    }

    public static IPackets<?, ?, ?> nms()
    {
        if(!available())
            init();
        return instance.nms;
    }

    public static boolean available()
    {
        return instance != null && instance.nms != null;
    }

    public static void init()
    {
        instance = new Packets();
    }

    @SuppressWarnings("unchecked")
    public static <T> void sendRaw(Player player, T raw)
    {
        instance.nms.sendRaw(player, raw);
    }

    @SuppressWarnings("unchecked")
    public static <T> void broadcast(T packet)
    {
        instance.nms.broadcast(packet);
    }

    @SuppressWarnings("unchecked")
    public static <T> void broadcastNear(Location location, T packet)
    {
        instance.nms.broadcastNear(location, packet);
    }

    @SuppressWarnings("unchecked")
    public static <T> void broadcast(T... packets)
    {
        instance.nms.broadcast(packets);
    }

    @SuppressWarnings("unchecked")
    public static <T> void broadcast(World world, T... packets)
    {
        instance.nms.broadcast(world, packets);
    }

    @SuppressWarnings("unchecked")
    public static <T> void send(Player player, T packet)
    {
        instance.nms.send(player, packet);
    }

    @SuppressWarnings("unchecked")
    public static <T> void send(Player player, T... packet)
    {
        instance.nms.send(player, packet);
    }

    @SuppressWarnings("unchecked")
    public static <E,T> void broadcastChunkmap(E entityHandle, T packetHandle, boolean excludeSelf)
    {
        instance.nms.broadcastChunkmap(entityHandle, packetHandle, excludeSelf);
    }

    /**
     * Sends a packet using ProtocolLib.
     *
     * @param receiver  the player
     * @param container the packet to send
     * @param filtered  determine if the packet should be intercepted by packet listeners.
     */
    public static void sendPlib(Player receiver, PacketContainer container, boolean filtered)
    {
        try
        {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, container, filtered);
        }
        catch (NullPointerException ignored)
        {
            //WARNING: be careful. This is sometimes be triggered when player channel is null for some reason...
            //https://github.com/PluginBugs/Issues-ItemsAdder/issues/1163
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendPlib(World world, PacketContainer container, boolean filtered)
    {
        for (Player player : world.getPlayers())
        {
            sendPlib(player, container, filtered);
        }
    }
}
