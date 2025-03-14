package dev.lone.rpgdrops.customdrop;

import dev.lone.rpgdrops.nms.nmslogic.INmsLogic;
import org.jetbrains.annotations.Nullable;
import dev.lone.rpgdrops.Settings;
import dev.lone.rpgdrops.nms.entity.EntityNms;
import dev.lone.rpgdrops.nms.nmslogic.NmsLogic;
import dev.lone.rpgdrops.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Drop
{
    private static final Map<Integer, Drop> dropByInteractionId = Collections.synchronizedMap(new HashMap<>());
    public static final Map<Integer, Integer> interactionByDropId = Collections.synchronizedMap(new HashMap<>());

    final long ms;
    final Item item;
    final boolean justSpawned;
    @Nullable Integer displayId;
    @Nullable Integer interactionId;
    RenderingMode renderingMode;
    boolean wasOnGround;
    private boolean itemDirty;

    enum RenderingMode
    {
        DROP,
        DISPLAY
    }

    public Drop(Item item, boolean justSpawned)
    {
        this.justSpawned = justSpawned;
        renderingMode = RenderingMode.DROP;
        this.ms = System.currentTimeMillis();
        this.item = item;
    }
    public Drop(Item item)
    {
        this(item, true);
    }

    @Nullable
    public static Drop getByInteraction(Integer interactionId)
    {
        return dropByInteractionId.get(interactionId);
    }

    public static boolean canBeIgnored(Item item)
    {
        return item.isInWater();
    }

    public static boolean canBeIgnoredFirstSpawn(Item item)
    {
        // I can't call isInWater() because it won't return the updated value as the item
        // was just spawned and didn't tick yet.
        return item.getLocation().getBlock().getType() == Material.WATER;
    }

    public void spawnDisplay()
    {
        if(renderingMode == RenderingMode.DISPLAY)
            return;

        renderingMode = RenderingMode.DISPLAY;
        spawnItemDisplay(null);
        playPlaceSound(item, null);
    }

    public void spawnOriginal()
    {
        if(renderingMode == RenderingMode.DROP)
            return;

        renderingMode = RenderingMode.DROP;
        // Let the game handle pretty looking physics instead of the slow ones that
        // I would implement manually syncing display location with item location.
        showOriginalItem(item);
    }

    public boolean isValid()
    {
        // WARNING: do not call the Bukkit isValid method as it's slow because it checks if the chunk is loaded
        return item != null && !item.isDead() && !canBeIgnored(item);
    }

    /**
     * If the item just reached the ground or if the block underneath was just removed.
     */
    public boolean groundStatusChanged()
    {
        boolean b = item.isOnGround() != wasOnGround;
        wasOnGround = item.isOnGround();
        return b;
    }

    public void spawnItemDisplay(@Nullable Player player)
    {
        if (displayId == null || !NmsLogic.nms().iscached(displayId))
        {
            final ItemStack itemStack = item.getItemStack();
            final Material type = itemStack.getType();
            // Happens in some cases.
            if (type == Material.AIR || itemStack.getAmount() == 0)
                return;

            final CachedMaterialModelInfo info = Settings.getMatInfo(type);

            // Rotate the model to lay it on the ground.
            final Transformation transformation = new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f());
            float scaleAddition = (float) (Math.random() * 0.05f); // Rnd to avoid z fighting of near drops.
            if(Settings.GRAPHICS_INCREASE_SIZE_BASED_ON_AMOUNT && itemStack.getAmount() > 1)
                scaleAddition += (float) itemStack.getAmount() * 0.003f;
            transformation.getScale().set(info.scale + scaleAddition);
            transformation.getTranslation().set(new Vector3f(info.translation).add(0, scaleAddition / 5f, 0));
            transformation.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(info.rotationLeftDegrees), info.rotationLeftAxis));

            hideDrop();

            INmsLogic.DropDisplay dropDisplay = NmsLogic.nms().createAndBroadcastItemDisplay(
                    item.getLocation(),
                    item,
                    createEntityName(itemStack),
                    transformation,
                    ItemDisplay.ItemDisplayTransform.FIXED,
                    info.getShadowRadius(),
                    info.getShadowStrength(),
                    info.viewRange != null ? info.viewRange : Settings.VIEW_RANGE,
                    info.brightnessSky != null ? info.brightnessSky : Settings.GRAPHICS_BRIGHTNESS_SKY,
                    info.brightnessBlock != null ? info.brightnessBlock : Settings.GRAPHICS_BRIGHTNESS_BLOCK,
                    info.glowEnabled != null ? info.glowEnabled : Settings.GRAPHICS_GLOW_ENABLED,
                    info.glowColor != null ? info.glowColor : Settings.GRAPHICS_GLOW_COLOR,
                    info.billboard != null ? info.billboard : Settings.GRAPHICS_BILLBOARD,
                    Settings.GRAPHICS_NAMEPLATE_SHOW_ONLY_ON_HOVER || Settings.PHYSICS_PICKUP_ON_INTERACT
            );

            displayId = dropDisplay.displayId;

            if(dropDisplay.useInteraction)
            {
                interactionId = dropDisplay.interactionId;
                dropByInteractionId.put(interactionId, this);
                interactionByDropId.put(displayId, interactionId);
            }

            return;
        }

        if (player != null)
        {
            hideDrop(player);
            NmsLogic.nms().spawnCachedEntity(item.getLocation(), player, displayId);
            if(interactionId != null)
                NmsLogic.nms().spawnCachedEntity(item.getLocation(), player, interactionId);
        }
        else
        {
            hideDrop();
            NmsLogic.nms().broadcastSpawnCachedEntity(item, item.getLocation(), displayId);
            if(interactionId != null)
                NmsLogic.nms().broadcastSpawnCachedEntity(item, item.getLocation(), interactionId);
        }
    }

    @Nullable
    private static Object createEntityName(ItemStack itemStack)
    {
        if (Settings.GRAPHICS_NAMEPLATE_ENABLED)
        {
            return NmsLogic.nms().createDropNameComponent(
                    itemStack,
                    Settings.GRAPHICS_NAMEPLATE_NAME,
                    Settings.GRAPHICS_NAMEPLATE_AMOUNT,
                    Settings.GRAPHICS_NAMEPLATE_NAME_COLOR,
                    Settings.GRAPHICS_NAMEPLATE_AMOUNT_COLOR
            );
        }

        return null;
    }

    public static void playPlaceSound(Item item, @Nullable Player player)
    {
        if(!Settings.GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND)
            return;

        Location loc = item.getLocation();
        Material type = item.getItemStack().getType();
        String sound;
        if (type.isBlock())
            sound = NmsLogic.nms().getPlaceSound(type);
        else
            sound = Sound.ENTITY_ITEM_FRAME_PLACE.getKey().toString();

        if (player != null)
            player.playSound(loc, sound, Settings.GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_VOLUME, Settings.GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_PITCH);
        else
        {
            for (Player pp : Utils.getNearbyPlayers(loc, Settings.GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_RANGE_BLOCKS))
            {
                pp.playSound(loc, sound, Settings.GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_VOLUME, Settings.GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_PITCH);
            }
        }
    }

    public void cloneAndPickupItem(Player player)
    {
        EntityNms.nms().broadcastCloneAndPickupItem(item, player);
    }

    public void showOriginalItem(Item item)
    {
        // Destroy the entity, in case it was the fake one.
        if(displayId != null)
            NmsLogic.nms().destroyDisplay(item, displayId);
        if(interactionId != null)
            NmsLogic.nms().destroyDisplay(item, interactionId);
        // Respawn the original entity.
        EntityNms.nms().broadcastSpawnRealEntity(item);
    }

    void hideDrop()
    {
        EntityNms.nms().destroy(item);
    }

    void hideDrop(Player player)
    {
        EntityNms.nms().destroy(player, item);
    }

    void hideDisplay(Player player)
    {
        if(displayId != null)
            EntityNms.nms().destroy(player, displayId);
        if(interactionId != null)
            EntityNms.nms().destroy(player, interactionId);
    }

    public void destroy()
    {
        if(displayId == null)
            return;

        NmsLogic.nms().uncache(displayId);
        EntityNms.nms().destroy(item.getWorld(), displayId);

        if(interactionId != null)
        {
            NmsLogic.nms().uncache(interactionId);
            EntityNms.nms().destroy(item.getWorld(), interactionId);
            dropByInteractionId.remove(interactionId);
            interactionByDropId.remove(interactionId);
        }
    }

    public void markItemDirty(boolean val)
    {
        itemDirty = val;
    }

    public boolean isItemDirty()
    {
        return itemDirty;
    }

    public void refreshDisplayName()
    {
        if(Settings.GRAPHICS_NAMEPLATE_NAME)
            NmsLogic.nms().updateDisplayName(item, displayId, createEntityName(item.getItemStack()));
    }
}
