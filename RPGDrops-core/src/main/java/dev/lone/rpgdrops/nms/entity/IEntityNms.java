package dev.lone.rpgdrops.nms.entity;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public interface IEntityNms<T, E>
{
    void destroy(Player player, Entity bukkitEntity);
    void destroy(Player player, int entityId);
    void destroy(World bukkitWorld, int id);
    void destroy(Entity bukkitEntity);
    void broadcastSpawnRealEntity(Entity entity);
    void broadcastCloneAndPickupItem(Item item, Player player);
}