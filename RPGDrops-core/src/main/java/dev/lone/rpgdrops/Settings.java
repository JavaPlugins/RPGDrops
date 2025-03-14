package dev.lone.rpgdrops;

import dev.lone.LoneLibs.EnumUtil;
import dev.lone.LoneLibs.config.ConfigFile;
import dev.lone.LoneLibs.config.CustomConfigurationSection;
import dev.lone.rpgdrops.customdrop.CachedMaterialModelInfo;
import dev.lone.rpgdrops.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.lone.rpgdrops.Main.msg;

public class Settings
{
    static ConfigFile config;

    public static int PHYSICS_TICKRATE;
    public static boolean PHYSICS_PICKUP_ON_INTERACT;

    public static boolean GRAPHICS_FACE_PLAYER_ON_DROP;
    public static boolean GRAPHICS_PICKUP_ANIMATION;
    public static boolean GRAPHICS_INCREASE_SIZE_BASED_ON_AMOUNT;
    public static boolean GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND;
    public static float GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_VOLUME;
    public static float GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_PITCH;
    public static int GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_RANGE_BLOCKS;

    public static boolean GRAPHICS_NAMEPLATE_ENABLED;
    public static boolean GRAPHICS_NAMEPLATE_SHOW_ONLY_ON_HOVER;
    public static boolean GRAPHICS_NAMEPLATE_NAME;
    public static int GRAPHICS_NAMEPLATE_NAME_COLOR;
    public static boolean GRAPHICS_NAMEPLATE_AMOUNT;
    public static int GRAPHICS_NAMEPLATE_AMOUNT_COLOR;

    public static boolean GRAPHIC_SHADOW_ENABLED;
    public static float GRAPHICS_SHADOW_RADIUS;
    public static float GRAPHICS_SHADOW_STRENGTH;
    public static float VIEW_RANGE;
    public static boolean GRAPHICS_BRIGHTNESS_ENABLED;
    @Nullable public static Integer GRAPHICS_BRIGHTNESS_SKY;
    @Nullable public static Integer GRAPHICS_BRIGHTNESS_BLOCK;
    @Nullable public static Boolean GRAPHICS_GLOW_ENABLED;
    @Nullable public static Integer GRAPHICS_GLOW_COLOR;
    @Nullable public static Display.Billboard GRAPHICS_BILLBOARD;

    public static List<String> WORLDS_WILDCARDS_RULES = new ArrayList<>();

    private static final Map<Material, CachedMaterialModelInfo> CACHED_MATERIAL_MODEL_INFO = new HashMap<>();

    public static void reload(Plugin plugin)
    {
        config = new ConfigFile(plugin, msg, true, "config.yml", true, true, false);
        ConfigFile graphicsSettings = new ConfigFile(plugin, msg, true, "graphics_settings.yml", true, false, false);

        PHYSICS_TICKRATE = config.getInt("physics.physics_tickrate", 1);
        if(PHYSICS_TICKRATE < 0)
            PHYSICS_TICKRATE = 0;
        if(PHYSICS_TICKRATE > 100)
        {
            msg.warn("'physics_tickrate: " + PHYSICS_TICKRATE + "' is too high! Setting it to 20.");
            PHYSICS_TICKRATE = 20;
        }

        PHYSICS_PICKUP_ON_INTERACT = config.getBoolean("physics.pickup_on_interact", false);
        if(PHYSICS_PICKUP_ON_INTERACT)
        {
            if(Bukkit.getPluginManager().getPlugin("ServerBooster") != null)
            {
                msg.warn("'pickup_on_interact' is not compatible with ServerBooster. Disabling the option.");
                PHYSICS_PICKUP_ON_INTERACT = false;
            }
        }

        GRAPHICS_FACE_PLAYER_ON_DROP = config.getBoolean("graphics.face_player_on_drop", true);
        GRAPHICS_PICKUP_ANIMATION = config.getBoolean("graphics.pickup_animation", true);
        GRAPHICS_INCREASE_SIZE_BASED_ON_AMOUNT = config.getBoolean("graphics.increase_size_based_on_amount", false);

        GRAPHICS_NAMEPLATE_ENABLED = config.getBoolean("graphics.nameplate.enabled", true);
        GRAPHICS_NAMEPLATE_SHOW_ONLY_ON_HOVER = config.getBoolean("graphics.nameplate.show_only_on_hover", true);
        GRAPHICS_NAMEPLATE_NAME = config.getBoolean("graphics.nameplate.name.enabled", true);
        GRAPHICS_NAMEPLATE_NAME_COLOR = Utils.hexToIntColor(config.getString("graphics.nameplate.name.color", "#ffffff"));
        GRAPHICS_NAMEPLATE_AMOUNT = config.getBoolean("graphics.nameplate.amount.enabled", true);
        GRAPHICS_NAMEPLATE_AMOUNT_COLOR = Utils.hexToIntColor(config.getString("graphics.nameplate.amount.color", "#878787"));

        GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND = config.getBoolean("graphics.play_place_sound_on_hit_ground.enabled", true);
        GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_VOLUME = config.getFloat("graphics.play_place_sound_on_hit_ground.volume", 0.2f);
        GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_PITCH = config.getFloat("graphics.play_place_sound_on_hit_ground.volume", 1f);
        GRAPHICS_PLAY_PLACE_SOUND_ON_HIT_GROUND_RANGE_BLOCKS = config.getInt("graphics.play_place_sound_on_hit_ground.range_blocks", 10);

        GRAPHIC_SHADOW_ENABLED = config.getBoolean("graphics.shadow.enabled", true);
        GRAPHICS_SHADOW_RADIUS = config.getFloat("graphics.shadow.radius", 0.3f);
        GRAPHICS_SHADOW_STRENGTH = config.getFloat("graphics.shadow.strength", 1.0f);
        VIEW_RANGE = config.getFloat("graphics.view_range", 10f);

        GRAPHICS_BRIGHTNESS_ENABLED = config.getBoolean("graphics.brightness.enabled", false);
        if(GRAPHICS_BRIGHTNESS_ENABLED)
        {
            GRAPHICS_BRIGHTNESS_SKY = config.getInt("graphics.brightness.sky", 15);
            GRAPHICS_BRIGHTNESS_BLOCK = config.getInt("graphics.brightness.block", 15);
        }
        else
        {
            GRAPHICS_BRIGHTNESS_SKY = null;
            GRAPHICS_BRIGHTNESS_BLOCK = null;
        }

        GRAPHICS_GLOW_ENABLED = config.getBoolean("graphics.glow.enabled", false);
        GRAPHICS_GLOW_COLOR = config.getInt("graphics.glow.color", 917248);
        if(config.getBoolean("graphics.billboard.enabled", false))
            GRAPHICS_BILLBOARD = config.getEnum("graphics.billboard.type", Display.Billboard.class, null);
        else
            GRAPHICS_BILLBOARD = null;

        List<String> unknownMaterials = new ArrayList<>();

        for (String key : graphicsSettings.getSectionKeys(""))
        {
            Material mat = EnumUtil.safeGet(key, Material.class, null);
            if(mat == null) // Might be a template.
            {
                // Dirty fix, because they decided to rename them in 1.20.
                if(!key.startsWith("POTTERY_"))
                    unknownMaterials.add(key);
                continue;
            }

            @Nullable CustomConfigurationSection section = graphicsSettings.getCustomSection(key);
            if(section == null) // Impossible
                continue;

            @Nullable String template = section.getString("use_template", null);
            if(template == null)
            {
                CachedMaterialModelInfo info = new CachedMaterialModelInfo(
                        section.getFloat("scale", 0),
                        section.getFloat("translation_x", 0),
                        section.getFloat("translation_y", 0),
                        section.getFloat("translation_z", 0),
                        section.getFloat("rotation_left_degrees", 0),
                        section.getFloat("rotation_left_axis_x", 0),
                        section.getFloat("rotation_left_axis_y", 0),
                        section.getFloat("rotation_left_axis_z", 0)
                );
                info.shadowRadius = section.getFloat("shadow.radius", null);
                info.shadowStrength = section.getFloat("shadow.strength", null);
                info.viewRange = section.getFloat("view_range", null);
                info.brightnessSky = section.getInt("brightness.sky", null);
                info.brightnessBlock = section.getInt("brightness.block", null);
                info.glowEnabled = section.getBoolean("glow.enabled", null);
                if(Boolean.TRUE.equals(info.glowEnabled))
                    info.glowColor = section.getInt("glow.color", null);
                info.billboardEnabled = section.getBoolean("billboard.enabled", null);
                if(Boolean.TRUE.equals(info.billboardEnabled))
                    info.billboard = section.getEnum("billboard.type", Display.Billboard.class);

                CACHED_MATERIAL_MODEL_INFO.put(mat, info);
            }
            else
            {
                if(template.equals(CachedMaterialModelInfo.TemplateType.ITEM_2D.name))
                    CACHED_MATERIAL_MODEL_INFO.put(mat, CachedMaterialModelInfo.DEFAULT_ITEM_2D);
                else
                    CACHED_MATERIAL_MODEL_INFO.put(mat, CachedMaterialModelInfo.DEFAULT_BLOCK);

                // Since it's a template I must not warn the user about missing material, because it's not a material.
                unknownMaterials.remove(key);
            }
        }

        unknownMaterials.forEach(key -> msg.warn("Unknown material: " + key));

        WORLDS_WILDCARDS_RULES = config.getStrings("worlds");
    }

    public static CachedMaterialModelInfo getMatInfo(Material material)
    {
        CachedMaterialModelInfo info = CACHED_MATERIAL_MODEL_INFO.get(material);
        if(info == null)
            return material.isBlock() ? CachedMaterialModelInfo.DEFAULT_BLOCK : CachedMaterialModelInfo.DEFAULT_ITEM_2D;
        return info;
    }

    public static boolean worldMatchRule(World world)
    {
        for (String rule : WORLDS_WILDCARDS_RULES)
        {
            if(rule.startsWith("*") || rule.endsWith("*"))
            {
                if (FilenameUtils.wildcardMatch(world.getName(), rule))
                    return true;
            }
            else
            {
                return world.getName().equals(rule);
            }
        }
        return false;
    }
}
