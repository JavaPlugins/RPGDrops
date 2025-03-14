package dev.lone.rpgdrops.nms.packets.impl;

import dev.lone.rpgdrops.nms.Implementation;
import dev.lone.rpgdrops.nms.Nms;
import dev.lone.rpgdrops.nms.packets.IPackets;
import io.netty.channel.ChannelHandlerContext;
import lonelibs.dev.lone.fastnbt.nms.Version;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

import static dev.lone.rpgdrops.Main.msg;

@Implementation.CyclicDependency(type = IPackets.class, version = Version.v1_20_R3)
public class v1_20_R3 implements IPackets<FriendlyByteBuf, Packet<?>, Entity>
{
    static Field field_connection;
    static
    {
        field_connection = Nms.getField(ServerGamePacketListenerImpl.class, Connection.class);
    }

    @Override
    public void sendRaw(Player player, FriendlyByteBuf raw)
    {
        final ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        if (!connection.isDisconnected())
        {
            ChannelHandlerContext context = null;
            if (!cachedContexts.containsKey(player))
            {
                try
                {
                    Connection conn = (Connection) field_connection.get(connection);
                    context = conn.channel.pipeline().context("encoder");
                    cachedContexts.put(player, context);
                }
                catch (IllegalAccessException e)
                {
                    msg.error("Failed to read player connection field. Some features might not work correctly!", e);
                }
            }
            else
            {
                context = cachedContexts.get(player);
            }

            if(context != null)
                context.writeAndFlush(raw);
        }
    }

    @Override
    public void send(Player player, Packet<?> packet)
    {
        final ServerPlayerConnection connection = ((CraftPlayer) player).getHandle().connection;
        if (connection != null)
            connection.send(packet);
    }

    @Override
    public void send(Player player, Packet<?>... packets)
    {
        final ServerPlayerConnection connection = ((CraftPlayer) player).getHandle().connection;
        if (connection != null)
        {
            for (Packet<?> packet : packets)
            {
                connection.send(packet);
            }
        }
    }

    @Override
    public void broadcastChunkmap(Entity handle, Packet<?> packet, boolean excludeSelf)
    {
        ((ServerChunkCache)handle.level().getChunkSource()).chunkMap.broadcast(handle, packet);
        if(!excludeSelf && handle instanceof ServerPlayer)
        {
            if(((ServerPlayer) handle).connection != null)
                ((ServerPlayer) handle).connection.send(packet);
        }
    }
}
