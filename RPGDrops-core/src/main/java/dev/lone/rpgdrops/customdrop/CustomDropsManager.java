package dev.lone.rpgdrops.customdrop;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.lone.rpgdrops.Main;
import dev.lone.rpgdrops.Settings;
import dev.lone.rpgdrops.nms.packets.Packets;
import dev.lone.rpgdrops.utils.Scheduler;
import dev.lone.rpgdrops.utils.Utils;
import dev.lone.rpgdrops.utils.WeakList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CustomDropsManager implements Listener
{
    private static CustomDropsManager inst;

    private final Cache<Drop, Boolean> drops = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
    private final Map<Item, Drop> dropByItem = Collections.synchronizedMap(new HashMap<>());
    private final WeakList<World> worlds = WeakList.create(World.class);
    private BukkitTask physicsTask;

    public static CustomDropsManager inst()
    {
        if(inst == null)
            inst = new CustomDropsManager();
        return inst;
    }

    CustomDropsManager()
    {
        Bukkit.getPluginManager().registerEvents(this, Main.inst());

        worlds.clear();
        for (World world : Bukkit.getWorlds())
        {
            if(Settings.worldMatchRule(world))
                worlds.add(world);
        }
    }

    public void init()
    {
        ProtocolLibrary.getProtocolManager().removePacketListeners(Main.inst());

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                PacketAdapter.params()
                        .plugin(Main.inst())
                        .types(PacketType.Play.Server.SPAWN_ENTITY)
                        .listenerPriority(ListenerPriority.HIGHEST)
        )
        {
            @Override
            public void onPacketSending(PacketEvent e)
            {
                if (e.isCancelled() || e.isPlayerTemporary())
                    return;

                final PacketContainer packet = e.getPacket();
                final EntityType entityType = packet.getEntityTypeModifier().read(0);
                if (entityType == EntityType.DROPPED_ITEM)
                {
                    final Player player = e.getPlayer();
                    final Entity entity = packet.getEntityModifier(e).read(0);
                    if (entity == null) // Fake entity of another plugin
                        return;

                    if (entity instanceof Item item)
                    {
                        if(!isEnabledInWorld(item.getWorld()))
                            return;

                        Drop drop = null;
                        // Still falling or just spawned or in water.
                        // Else means it's the spawn packet sent to players on join or teleport.
                        if (!item.isOnGround() || item.isInWater())
                        {
                            boolean ignore = true;
                            // This is not reliable because the game sets ticksLived to 0
                            // when chunk is first loaded and entity didn't tick yet.
                            // Stupid bug. So I have to do another check in this if statement.
                            if(item.getTicksLived() < 5) // May be just spawned.
                            {
                                drop = getDrop(item);
                                if(drop == null)
                                    return;

                                if (!drop.justSpawned)
                                    ignore = false;
                            }

                            if(ignore)
                                return;
                        }

                        if(drop == null)
                            drop = getDrop(item);
                        if(drop == null)
                            return;

                        // Do not spawn the original item.
                        e.setCancelled(true);

                        // Spawn the fake entity.
                        drop.spawnItemDisplay(player);
                    }
                }
            }
        });

        // https://mappings.cephx.dev/1.20.4/net/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket.html
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                PacketAdapter.params()
                        .plugin(Main.inst())
                        .types(PacketType.Play.Server.ENTITY_DESTROY)
                        .listenerPriority(ListenerPriority.HIGHEST)
        )
        {
            @Override
            public void onPacketSending(PacketEvent e)
            {
                if (e.isCancelled() || e.isPlayerTemporary())
                    return;

                final PacketContainer packet = e.getPacket();
                if(Packets.ignored.contains(packet.getHandle()))
                {
                    Packets.ignored.remove(packet.getHandle());
                    return;
                }

                final Player player = e.getPlayer();
                if(!isEnabledInWorld(player.getWorld()))
                    return;

                // TODO: check if it was called by the server entity tracker. <--- no idea why tbh, old comment.
                for (int i : packet.getIntLists().read(0))
                {
                    Entity entity = ProtocolLibrary.getProtocolManager().getEntityFromID(player.getWorld(), i);
                    if (entity == null) // Fake entity of another plugin
                        return;

                    if (entity instanceof Item item)
                    {
                        final Drop drop = getDrop(item);
                        if(drop == null)
                            return;

                        drop.hideDisplay(player);
                    }
                }
            }
        });

        if(Settings.PHYSICS_PICKUP_ON_INTERACT)
        {
            // https://mappings.cephx.dev/1.20.4/net/minecraft/network/protocol/game/ServerboundInteractPacket.html
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                    PacketAdapter.params()
                            .plugin(Main.inst())
                            .types(PacketType.Play.Client.USE_ENTITY)
                            .listenerPriority(ListenerPriority.HIGHEST)
            )
            {
                @Override
                public void onPacketReceiving(PacketEvent e)
                {
                    if (e.isCancelled() || e.isPlayerTemporary())
                        return;

                    final PacketContainer packet = e.getPacket();
                    WrappedEnumEntityUseAction useAction = packet.getEnumEntityUseActions().read(0);
                    // Avoid double interaction.
                    if(useAction.getAction() == EnumWrappers.EntityUseAction.INTERACT_AT)
                        return;

                    if(!isEnabledInWorld(e.getPlayer().getWorld()))
                        return;

                    final Drop drop = Drop.getByInteraction(packet.getIntegers().read(0));
                    if (drop != null)
                    {
                        Scheduler.sync(() -> {
                            if(Utils.give(e.getPlayer(), drop.item.getItemStack()))
                            {
                                // Remove the display entity completely.
                                destroyCompletely(drop);
                                // Spawn the original item drop entity using NMS so that it plays the pickup animation.
                                if (Settings.GRAPHICS_PICKUP_ANIMATION)
                                    drop.cloneAndPickupItem(e.getPlayer());

                                drop.item.remove(); // Kill the real drop entity.
                            }
                        });
                    }
                }
            });
        }

        // TODO: test with an async non bukkit thread?
        if(physicsTask != null)
            physicsTask.cancel();
        physicsTask = Scheduler.asyncLoop((t) -> {
            for (Iterator<Map.Entry<Drop, Boolean>> iterator = drops.asMap().entrySet().iterator(); iterator.hasNext(); )
            {
                Drop drop = iterator.next().getKey();
                // Happens when another plugin removes the entity directly without killing it.
                // For example /killall command of EssentialsX.
                if(!drop.isValid())
                {
                    iterator.remove();
                    dropByItem.remove(drop.item);
                    drop.destroy();
                    continue;
                }

                boolean onGround = drop.item.isOnGround();
                if(drop.groundStatusChanged())
                {
                    if (onGround)
                        drop.spawnDisplay();
                    else
                        drop.spawnOriginal();
                }

                if(drop.renderingMode == Drop.RenderingMode.DISPLAY)
                {
                    if(drop.isItemDirty())
                    {
                        drop.markItemDirty(false);
                        drop.refreshDisplayName();
                    }
                }
            }
        }, Settings.PHYSICS_TICKRATE, Settings.PHYSICS_TICKRATE);

        // Load entities in case of a plugin reload. Might be needed only during development.
        for (World world : worlds)
        {
            loadEntities(new ArrayList<>(world.getEntitiesByClass(Item.class)));
        }
    }

    public Drop getDrop(Item item)
    {
        return dropByItem.get(item);
    }

    public void linkDisplay(Item item, Drop displayId)
    {
        dropByItem.put(item, displayId);
    }

    private void destroyCompletely(Item item)
    {
        Drop drop = dropByItem.get(item);
        if(drop == null)
            return;
        drops.invalidate(drop);
        dropByItem.remove(item);
    }

    private void destroyCompletely(Drop drop)
    {
        if(drop == null)
            return;
        drops.invalidate(drop);
        dropByItem.remove(drop.item);
        drop.destroy();
    }

    private void unhook(Drop drop)
    {
        drop.spawnOriginal();
    }

    public void unhookAll()
    {
        dropByItem.forEach((item, drop) -> {
            unhook(drop);
        });
    }

    private void loadEntities(List<Entity> entities)
    {
        for (Entity entity : entities)
        {
            if(entity.getType() == EntityType.DROPPED_ITEM && entity instanceof Item item && !Drop.canBeIgnored(item))
            {
                Drop drop = new Drop(item, false);
                drops.put(drop, true);
                linkDisplay(item, drop);
            }
        }
    }

    private boolean isEnabledInWorld(World world)
    {
        return worlds.contains(world);
    }

    private boolean isEnabledInWorld(Entity entity)
    {
        return worlds.contains(entity.getWorld());
    }

    //<editor-fold desc="Events">
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerDropItem(PlayerDropItemEvent e)
    {
        if(Settings.GRAPHICS_FACE_PLAYER_ON_DROP)
        {
            if(!isEnabledInWorld(e.getPlayer()))
                return;

            Item entity = e.getItemDrop();
            Player player = e.getPlayer();
            Location loc = entity.getLocation();

            // To change rotation Y of the model once it will be spawned to match the player rotation.
            loc.setYaw(player.getLocation().getYaw() + 180);
            entity.teleport(loc);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onItemSpawn(ItemSpawnEvent e)
    {
        Item item = e.getEntity();
        if(!isEnabledInWorld(item))
            return;
        if(Drop.canBeIgnoredFirstSpawn(item))
            return;

        Drop drop = new Drop(item);
        drops.put(drop, true);
        linkDisplay(item, drop);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerPickupItem(@SuppressWarnings("deprecation") PlayerPickupItemEvent e)
    {
        final Item item = e.getItem();
        final Drop drop = getDrop(item);
        if (drop == null)
            return;

        if(!isEnabledInWorld(item))
            return;

        if(Settings.PHYSICS_PICKUP_ON_INTERACT)
        {
            e.setCancelled(true);
            // To avoid PlayerPickupItemEvent being called over and over too much.
            // NOTE: I cannot set it to Integer.MAX_VALUE because if you remove the plugin you won't be able to
            //       pick up any old item anymore and people would complain.
            item.setPickupDelay(20 * 30); // 30 seconds delay before next PlayerPickupItemEvent, to use less CPU.
            return;
        }

        // Remove the display entity completely.
        destroyCompletely(drop);
        // Spawn the original item drop entity using NMS so that it plays the pickup animation.
        if(Settings.GRAPHICS_PICKUP_ANIMATION)
            drop.cloneAndPickupItem(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onItemMerge(ItemMergeEvent e)
    {
        if(!isEnabledInWorld(e.getEntity()))
            return;

        final Item newItem = e.getTarget();
        final Drop newDrop = getDrop(newItem);
        if (newDrop == null)
            return;

        final Item oldItem = e.getEntity();
        final Drop oldDrop = getDrop(oldItem);
        if (oldDrop == null)
            return;

        oldDrop.destroy();
        oldDrop.hideDrop();
        destroyCompletely(oldItem);

        // Stupid shit to force refresh of ItemStack.
        newDrop.markItemDirty(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent e)
    {
        if(!isEnabledInWorld(e.getPlayer()))
            return;

        // I already do that in the physics thread but to avoid the little 1 tick delay I also handle it here.
        //TODO: maybe make this configurable because some people might not care about this 1 tick delay fix.
        Location loc = e.getBlock().getLocation();
        BoundingBox aabb = BoundingBox.of(loc, 1, 1.5, 1);
        for (Entity entity : loc.getChunk().getEntities())
        {
            if(entity.getType() == EntityType.DROPPED_ITEM && aabb.contains(entity.getLocation().toVector()))
            {
                Item item = (Item) entity;
                Drop drop = new Drop(item);
                drop.spawnOriginal();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntitiesLoad(EntitiesLoadEvent e)
    {
        if(!isEnabledInWorld(e.getWorld()))
            return;
        loadEntities(e.getEntities());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntitiesUnload(EntitiesUnloadEvent e)
    {
        for (Entity entity : e.getEntities())
        {
            if(entity.getType() == EntityType.DROPPED_ITEM)
            {
                if(entity instanceof Item item)
                {
                    // Hides it if needed.
                    Drop drop = CustomDropsManager.inst().getDrop(item);
                    destroyCompletely(drop);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldLoad(WorldLoadEvent e)
    {
        if(Settings.worldMatchRule(e.getWorld()))
            worlds.add(e.getWorld());

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldUnload(WorldUnloadEvent e)
    {
        worlds.remove(e.getWorld());
    }
    //</editor-fold>
}
