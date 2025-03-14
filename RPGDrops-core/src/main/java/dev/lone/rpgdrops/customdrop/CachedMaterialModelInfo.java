package dev.lone.rpgdrops.customdrop;

import org.jetbrains.annotations.Nullable;
import dev.lone.rpgdrops.Settings;
import org.bukkit.entity.Display;
import org.joml.Vector3f;

public class CachedMaterialModelInfo
{
    //<editor-fold desc="Default data initialization">
    public static final CachedMaterialModelInfo DEFAULT_BLOCK = new CachedMaterialModelInfo();
    static
    {
        CachedMaterialModelInfo res = DEFAULT_BLOCK;
        res.scale = 0.65f;
        res.translation.x = 0;
        res.translation.y = 0.15f;
        res.translation.z = 0;
    }
    public static final CachedMaterialModelInfo DEFAULT_ITEM_2D = new CachedMaterialModelInfo();
    static
    {
        CachedMaterialModelInfo res = DEFAULT_ITEM_2D;
        res.scale = 0.65f;
        res.rotationLeftAxis.x = 1;
        res.rotationLeftDegrees = -90;
    }
    //</editor-fold>

    public enum TemplateType
    {
        BLOCK("template_block"),
        ITEM_2D("template_2d_item");

        public final String name;

        TemplateType(String name)
        {
            this.name = name;
        }
    }

    public float scale = 1.0f;
    public Vector3f translation = new Vector3f();
    public float rotationLeftDegrees = 0;
    public Vector3f rotationLeftAxis = new Vector3f();
    @Nullable
    public Float shadowRadius;
    @Nullable
    public Float shadowStrength;
    @Nullable public Float viewRange;
    @Nullable public Integer brightnessSky;
    @Nullable public Integer brightnessBlock;
    @Nullable public Boolean glowEnabled;
    @Nullable public Integer glowColor;
    @Nullable public Boolean billboardEnabled;
    @Nullable public Display.Billboard billboard;

    public CachedMaterialModelInfo(float scale,
                                   float translationX,
                                   float translationY,
                                   float translationZ,
                                   float rotationLeftDegrees,
                                   float rotationLeftAxisX,
                                   float rotationLeftAxisY,
                                   float rotationLeftAxisZ)
    {
        this.scale = scale;
        translation.set(translationX, translationY, translationZ);
        this.rotationLeftDegrees = rotationLeftDegrees;
        rotationLeftAxis.set(rotationLeftAxisX, rotationLeftAxisY, rotationLeftAxisZ);
    }

    public CachedMaterialModelInfo() {}

    @Nullable
    public Float getShadowRadius()
    {
        if(!Settings.GRAPHIC_SHADOW_ENABLED)
            return null;
        return shadowRadius != null ? shadowRadius : Settings.GRAPHICS_SHADOW_RADIUS;
    }

    @Nullable
    public Float getShadowStrength()
    {
        if(!Settings.GRAPHIC_SHADOW_ENABLED)
            return null;
        return shadowStrength != null ? shadowStrength : Settings.GRAPHICS_SHADOW_STRENGTH;
    }
}
