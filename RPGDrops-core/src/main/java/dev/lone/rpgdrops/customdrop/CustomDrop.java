package dev.lone.rpgdrops.customdrop;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.persistence.PersistentDataType;

@Deprecated
public class CustomDrop
{
    @SuppressWarnings("deprecation")
    static final NamespacedKey PERSISTENT_KEY_SKIP_DROP = new NamespacedKey("rpgdrops", "skip");

    @Deprecated
    public static boolean canBeCustomized(Item item)
    {
        return !item.getPersistentDataContainer().getOrDefault(PERSISTENT_KEY_SKIP_DROP, PersistentDataType.BOOLEAN, false);
    }

}
