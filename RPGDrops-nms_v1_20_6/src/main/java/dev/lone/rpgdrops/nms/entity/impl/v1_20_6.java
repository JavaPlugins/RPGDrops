package dev.lone.rpgdrops.nms.entity.impl;

import dev.lone.rpgdrops.nms.Implementation;
import dev.lone.rpgdrops.nms.Nms;
import dev.lone.rpgdrops.nms.entity.IEntityNms;
import dev.lone.rpgdrops.nms.packets.Packets;
import lonelibs.dev.lone.fastnbt.nms.Version;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Implementation.CyclicDependency(type = IEntityNms.class, version = Version.v1_20_6)
public class v1_20_6 implements IEntityNms<ClientboundSetPassengersPacket, net.minecraft.world.entity.Entity>
{
    @Override
    public void destroy(Player player, Entity bukkitEntity)
    {
        final ServerPlayerConnection connection = ((CraftPlayer) player).getHandle().connection;
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(bukkitEntity.getEntityId());
        Packets.ignored.add(packet);
        connection.send(packet);
    }

    @Override
    public void destroy(Player player, int entityId)
    {
        final ServerPlayerConnection connection = ((CraftPlayer) player).getHandle().connection;
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityId);
        Packets.ignored.add(packet);
        connection.send(packet);
    }

    @Override
    public void destroy(World bukkitWorld, int id)
    {
        for (Player bukkitPlayer : bukkitWorld.getPlayers())
        {
            final ServerPlayerConnection connection = ((CraftPlayer) bukkitPlayer).getHandle().connection;
            ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(id);
            Packets.ignored.add(packet);
            connection.send(packet);
        }
    }

    @Override
    public void destroy(Entity bukkitEntity)
    {
        net.minecraft.world.entity.Entity handle = ((CraftEntity) bukkitEntity).getHandle();
        ((ServerChunkCache) handle.level().getChunkSource()).chunkMap.entityMap.get(handle.getId()).seenBy.forEach(connection -> {
            ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(bukkitEntity.getEntityId());
            Packets.ignored.add(packet);
            connection.send(packet);
        });
    }

    @Override
    public void broadcastSpawnRealEntity(Entity entity)
    {
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        broadcast(handle, new ClientboundBundlePacket(
                List.of(
                        new ClientboundAddEntityPacket(handle),
                        new ClientboundSetEntityDataPacket(
                                handle.getId(),
                                handle.getEntityData().getNonDefaultValues()
                        )
                )
        ));
    }

    @Override
    public void broadcastCloneAndPickupItem(Item item, Player player)
    {
        ItemEntity handle = (ItemEntity) ((CraftEntity) item).getHandle();
        AtomicInteger atomicInteger = Nms.read(Nms.getField(net.minecraft.world.entity.Entity.class, AtomicInteger.class), null);
        int newId = atomicInteger.incrementAndGet();
        broadcast(handle, new ClientboundBundlePacket(
                List.of(
                        new ClientboundAddEntityPacket(newId, handle.getUUID(), handle.getX(), handle.getY(), handle.getZ(), handle.getXRot(), handle.getYRot(), handle.getType(), 0, handle.getDeltaMovement(), handle.getYHeadRot()),
                        new ClientboundSetEntityDataPacket(newId, handle.getEntityData().getNonDefaultValues())
                )
        ));
        broadcast(handle, new ClientboundTakeItemEntityPacket(newId, player.getEntityId(), handle.getItem().getCount()));
    }

    private void broadcast(net.minecraft.world.entity.Entity handle, Packet<?> packet)
    {
        ((ServerChunkCache) handle.level().getChunkSource()).chunkMap.broadcast(handle, packet);
    }
}
