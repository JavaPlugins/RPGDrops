package dev.lone.rpgdrops.nms.nmslogic.impl;

import com.mojang.math.Transformation;
import dev.lone.rpgdrops.Settings;
import dev.lone.rpgdrops.nms.Implementation;
import dev.lone.rpgdrops.nms.nmslogic.INmsLogic;
import dev.lone.rpgdrops.nms.packets.Packets;
import lonelibs.dev.lone.fastnbt.nms.Version;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Implementation.CyclicDependency(type = INmsLogic.class, version = Version.v1_20_R2)
public class v1_20_R2 implements INmsLogic
{
    Map<Integer, Entity> cachedEntity = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void spawnCachedEntity(Location bukkitLocation, Player player, int id)
    {
        Entity entity = cachedEntity.get(id);
        if(entity != null)
        {
            entity.setPos(bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ());
            entity.setYRot(bukkitLocation.getYaw());

            ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(entity);
            ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(
                    entity.getId(),
                    entity.getEntityData().getNonDefaultValues()
            );

            Packets.send(player, addPacket);
            Packets.send(player, dataPacket);
        }
    }

    @Override
    public void broadcastSpawnCachedEntity(Item bukkitItemEntity, Location bukkitLocation, int id)
    {
        Entity entity = cachedEntity.get(id);
        if(entity != null)
        {
            entity.setPos(bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ());
            entity.setYRot(bukkitLocation.getYaw());

            ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(entity);
            ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(
                    entity.getId(),
                    entity.getEntityData().getNonDefaultValues()
            );

            Entity handle = ((CraftEntity) bukkitItemEntity).getHandle();
            broadcastNear(handle, addPacket);
            broadcastNear(handle, dataPacket);
        }
    }

    @Override
    public DropDisplay createAndBroadcastItemDisplay(Location bukkitLocation,
                                              Item bukkitItemEntity,
                                              @Nullable Object nameNmsComponent,
                                              org.bukkit.util.Transformation bukkitTransformation,
                                              ItemDisplay.ItemDisplayTransform display,
                                              @Nullable Float shadowRadius,
                                              @Nullable Float shadowStrength,
                                              @Nullable Float viewRange,
                                              @Nullable Integer brightnessSky,
                                              @Nullable Integer brightnessBlock,
                                              @Nullable Boolean glow,
                                              @Nullable Integer glowColor,
                                              @Nullable org.bukkit.entity.Display.Billboard billboard,
                                                     boolean needsInteractionEntity)
    {
        DropDisplay dropDisplay = new DropDisplay(needsInteractionEntity);
        ServerLevel level = ((CraftWorld) bukkitLocation.getWorld()).getHandle();
        ClientboundBundlePacket packets = createDisplay(bukkitLocation,
                level,
                bukkitItemEntity.getItemStack(),
                nameNmsComponent,
                bukkitTransformation,
                display,
                shadowRadius,
                shadowStrength,
                viewRange,
                brightnessSky,
                brightnessBlock,
                glow,
                glowColor,
                billboard,
                dropDisplay);

        Entity nearEntity = ((CraftEntity) bukkitItemEntity).getHandle();
        for (Packet<ClientGamePacketListener> packet : packets.subPackets())
            broadcastNear(nearEntity, packet);

        return dropDisplay;
    }

    private ClientboundBundlePacket createDisplay(Location bukkitLocation,
                                                  ServerLevel level,
                                                  ItemStack bukkitItem,
                                                  @Nullable Object nameNmsComponent,
                                                  org.bukkit.util.Transformation bukkitTransformation,
                                                  ItemDisplay.ItemDisplayTransform display,
                                                  @Nullable Float shadowRadius,
                                                  @Nullable Float shadowStrength,
                                                  @Nullable Float viewRange,
                                                  @Nullable Integer brightnessSky,
                                                  @Nullable Integer brightnessBlock,
                                                  @Nullable Boolean glow,
                                                  @Nullable Integer glowColor,
                                                  org.bukkit.entity.Display.@Nullable Billboard billboard,
                                                  DropDisplay dropDisplay)
    {
        Display.ItemDisplay entity = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        entity.setInvisible(true); // Hide from F3+B
        entity.setPos(bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ());
        entity.setYRot(bukkitLocation.getYaw());
        entity.setItemStack(CraftItemStack.asNMSCopy(bukkitItem));
        if(nameNmsComponent != null && !Settings.GRAPHICS_NAMEPLATE_SHOW_ONLY_ON_HOVER)
        {
            entity.setCustomNameVisible(true);
            entity.setCustomName((Component) nameNmsComponent);
        }

        entity.getEntityData().set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 1);

        if(shadowRadius != null)
            entity.setShadowRadius(shadowRadius);
        if(shadowStrength != null)
            entity.setShadowStrength(shadowStrength);
        if(viewRange != null)
            entity.setViewRange(viewRange);

        entity.setItemTransform(ItemDisplayContext.BY_ID.apply(display.ordinal()));

        if (brightnessBlock != null && brightnessSky != null)
            entity.setBrightnessOverride(new Brightness(brightnessBlock, brightnessSky));

        if(glow != null)
            entity.setGlowingTag(glow);
        if(glowColor != null)
            entity.setGlowColorOverride(glowColor);

        if(billboard != null)
            entity.setBillboardConstraints(Display.BillboardConstraints.BY_ID.apply(billboard.ordinal()));

        entity.setTransformation(
                new Transformation(bukkitTransformation.getTranslation(),
                        bukkitTransformation.getLeftRotation(),
                        bukkitTransformation.getScale(),
                        bukkitTransformation.getRightRotation())
        );

        ClientboundAddEntityPacket addDisplay = new ClientboundAddEntityPacket(entity);
        @SuppressWarnings("DataFlowIssue")
        ClientboundSetEntityDataPacket dataDisplay = new ClientboundSetEntityDataPacket(
                entity.getId(),
                entity.getEntityData().getNonDefaultValues()
        );

        dropDisplay.displayId = entity.getId();
        cachedEntity.put(entity.getId(), entity);

        if(dropDisplay.useInteraction)
        {
            Interaction interaction = new Interaction(EntityType.INTERACTION, level);
            interaction.setInvisible(true); // Hide from F3+B
            interaction.setPos(bukkitLocation.getX(), bukkitLocation.getY() + bukkitTransformation.getTranslation().y, bukkitLocation.getZ());
            interaction.setWidth(bukkitTransformation.getScale().x / 1.3f);
            interaction.setHeight(bukkitTransformation.getScale().y / 1.5f);
            // If response is set to true, left-clicking an interaction entity plays a punching sound,
            // and right-clicking it makes the player's arm swing.
            interaction.setResponse(true);

            // Visible only on hover.
            if(Settings.GRAPHICS_NAMEPLATE_SHOW_ONLY_ON_HOVER)
            {
                if (nameNmsComponent != null)
                    interaction.setCustomName((Component) nameNmsComponent);
            }

            ClientboundAddEntityPacket addInteraction = new ClientboundAddEntityPacket(interaction);
            @SuppressWarnings("DataFlowIssue")
            ClientboundSetEntityDataPacket dataInteraction = new ClientboundSetEntityDataPacket(
                    interaction.getId(),
                    interaction.getEntityData().getNonDefaultValues()
            );

            dropDisplay.interactionId = interaction.getId();
            cachedEntity.put(interaction.getId(), interaction);
            return new ClientboundBundlePacket(List.of(addDisplay, dataDisplay, addInteraction, dataInteraction));
        }
        else
        {
            return new ClientboundBundlePacket(List.of(addDisplay, dataDisplay));
        }
    }

    @Override
    public String getPlaceSound(Material material)
    {
        net.minecraft.world.level.block.Block block = CraftMagicNumbers.getBlock(material);
        return block.getSoundType(block.defaultBlockState()).getPlaceSound().getLocation().toString();
    }

    @Override
    public void uncache(Integer id)
    {
        cachedEntity.remove(id);
    }

    @Override
    public void destroyDisplay(Item real, int fakeId)
    {
        net.minecraft.world.entity.Entity handle = ((CraftEntity) real).getHandle();
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(fakeId);
        Packets.ignored.add(packet);
        broadcastNear(handle, packet);
    }

    @Override
    public Object createDropNameComponent(ItemStack bukkitItemStack, boolean showName, boolean showAmount, int nameColor, int amountColor)
    {
        if(!showName && !showAmount)
            return null;

        net.minecraft.world.item.ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItemStack);
        if(showName && showAmount)
        {
            MutableComponent component = Component.empty().append(itemStack.getHoverName());
            component.setStyle(component.getStyle().withColor(nameColor));

            MutableComponent amountComp = Component.literal(" x" + itemStack.getCount());
            amountComp.setStyle(amountComp.getStyle().withColor(amountColor));
            component.append(amountComp);
            return component;
        }

        if(!showName)
        {
            MutableComponent amountComp = Component.literal("x" + itemStack.getCount());
            amountComp.setStyle(amountComp.getStyle().withColor(amountColor));
            return amountComp;
        }

        MutableComponent component = Component.empty().append(itemStack.getHoverName());
        component.setStyle(component.getStyle().withColor(nameColor));
        return component;
    }

    @Override
    public boolean iscached(Integer displayId)
    {
        return cachedEntity.containsKey(displayId);
    }

    private void broadcastNear(net.minecraft.world.entity.Entity handle, Packet<?> packet)
    {
        ((ServerChunkCache) handle.level().getChunkSource()).chunkMap.broadcast(handle, packet);
    }
}
