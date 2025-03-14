package dev.lone.rpgdrops.nms.packets;

import io.netty.channel.ChannelHandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.WeakHashMap;

/**
 * Very fast low-level packet sender.
 *
 * @param <T> is the "FriendlyByteBuf" type which is different in obfuscated MC code.
 */
public interface IPackets<T,P, E>
{
    WeakHashMap<Player, ChannelHandlerContext> cachedContexts = new WeakHashMap<>();

    // https://github.com/ViaVersion/ViaVersion/blob/d5a568b3fc101dc956066f12886f90cbf0a62178/common/src/main/java/com/viaversion/viaversion/connection/UserConnectionImpl.java#L132
    void sendRaw(Player player, T raw);

    default void broadcast(P packet)
    {
        for (Player player : Bukkit.getOnlinePlayers())
            send(player, packet);
    }

    default void broadcast(P... packets)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            for (P packet : packets)
                send(player, packet);
        }
    }

    default void broadcast(World world, P... packets)
    {
        for (Player player : world.getPlayers())
        {
            for (P packet : packets)
                send(player, packet);
        }
    }

    default void broadcastNear(Location location, P... packets)
    {
        @SuppressWarnings("ConstantConditions")
        int radius = location.getWorld().getViewDistance() * 16;
        for (Player player : location.getWorld().getPlayers())
        {
            if(player.getLocation().distance(location) > radius)
                continue;

            for (P packet : packets)
                send(player, packet);
        }
    }

    void send(Player player, P packet);
    void send(Player player, P... packets);
    void broadcastChunkmap(E entity, P packet, boolean excludeSelf);
}
