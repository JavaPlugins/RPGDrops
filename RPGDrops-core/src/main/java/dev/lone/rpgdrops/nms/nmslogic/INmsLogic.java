package dev.lone.rpgdrops.nms.nmslogic;

import org.jetbrains.annotations.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

public interface INmsLogic
{
    void spawnCachedEntity(Location bukkitLocation, Player player, int id);
    void broadcastSpawnCachedEntity(Item bukkitItemEntity, Location bukkitLocation, int id);

    DropDisplay createAndBroadcastItemDisplay(Location bukkitLocation,
                                              Item bukkitItemEntity,
                                              @Nullable Object nameNmsComponent,
                                              Transformation bukkitTransformation,
                                              ItemDisplay.ItemDisplayTransform display,
                                              @Nullable Float shadowRadius,
                                              @Nullable Float shadowStrength,
                                              @Nullable Float viewRange,
                                              @Nullable Integer brightnessSky,
                                              @Nullable Integer brightnessBlock,
                                              @Nullable Boolean glow,
                                              @Nullable Integer glowColor,
                                              @Nullable Display.Billboard billboard, boolean needsInteractionEntity);

    String getPlaceSound(Material material);

    void uncache(Integer id);

    void destroyDisplay(Item real, int fakeId);

    Object createDropNameComponent(ItemStack bukkitItemStack, boolean showName, boolean showAmount, int nameColor, int amountColor);

    boolean iscached(Integer displayId);

    default void updateDisplayName(org.bukkit.entity.Entity bukkitNearEntity, Integer displayId, Object nameNmsComponent)
    {
        //TODO
    }

    class DropDisplay
    {
        public int displayId;
        public int interactionId;

        public final boolean useInteraction;

        public DropDisplay(boolean useInteraction)
        {
            this.useInteraction = useInteraction;
        }
    }
}