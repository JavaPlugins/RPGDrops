package dev.lone.rpgdrops.utils;

import org.jetbrains.annotations.NotNull;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

public class PersistentDataTypeUUID implements PersistentDataType<byte[], UUID>
{
    static PersistentDataType<byte[], java.util.UUID> INSTANCE = new PersistentDataTypeUUID();

    public static void write(Entity entity, NamespacedKey namespacedKey, UUID uniqueId)
    {
        entity.getPersistentDataContainer().set(namespacedKey, PersistentDataTypeUUID.INSTANCE, uniqueId);
    }

    @Nullable
    public static UUID read(Entity entity, NamespacedKey namespacedKey)
    {
        return entity.getPersistentDataContainer().get(namespacedKey, PersistentDataTypeUUID.INSTANCE);
    }

    public static boolean has(Entity entity, NamespacedKey namespacedKey)
    {
        return entity.getPersistentDataContainer().has(namespacedKey, PersistentDataTypeUUID.INSTANCE);
    }

    @Override
    public @NotNull Class<byte[]> getPrimitiveType()
    {
        return byte[].class;
    }

    @Override
    public @NotNull Class<UUID> getComplexType()
    {
        return UUID.class;
    }

    @Override
    public byte @NotNull [] toPrimitive(final UUID complex, @NotNull final PersistentDataAdapterContext context)
    {
        final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(complex.getMostSignificantBits());
        bb.putLong(complex.getLeastSignificantBits());
        return bb.array();
    }

    @Override
    public @NotNull UUID fromPrimitive(final byte @NotNull [] primitive, @NotNull final PersistentDataAdapterContext context)
    {
        final ByteBuffer bb = ByteBuffer.wrap(primitive);
        final long firstLong = bb.getLong();
        final long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }
}