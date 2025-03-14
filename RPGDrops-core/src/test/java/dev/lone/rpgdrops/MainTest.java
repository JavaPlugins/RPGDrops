package dev.lone.rpgdrops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import dev.lone.rpgdrops.customdrop.CachedMaterialModelInfo;
import org.bukkit.Material;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

class MainTest
{
    private static final String SAVE_PATH = "C:\\Progetti\\Minecraft\\Spigot\\RPGDrops\\RPGDrops-core\\src\\main\\resources\\graphics_settings.yml";

    public static Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final Vector3f vecZero = new Vector3f();

    // WARNING: Do not forget to paste the vanilla Minecraft models into resources/internal/vanilla/block and resources/internal/vanilla/item.

    
    private void putEntry(Map<String, Object> data, CachedMaterialModelInfo res, String name)
    {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("scale", res.scale);
        nested.put("translation_x", res.translation.x);
        nested.put("translation_y", res.translation.y);
        nested.put("translation_z", res.translation.z);
        nested.put("rotation_left_degrees", res.rotationLeftDegrees);
        nested.put("rotation_left_axis_x", res.rotationLeftAxis.x);
        nested.put("rotation_left_axis_y", res.rotationLeftAxis.y);
        nested.put("rotation_left_axis_z", res.rotationLeftAxis.z);
        data.put(name, nested);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void calculateAll()
    {
        Map<String, Object> data = new LinkedHashMap<>();

        putEntry(data, CachedMaterialModelInfo.DEFAULT_BLOCK, "template_block");
        putEntry(data, CachedMaterialModelInfo.DEFAULT_ITEM_2D, "template_2d_item");

        for (Material type : Material.values())
        {
            if(type.isLegacy() || type.isAir() || !type.isItem())
                continue;

            CachedMaterialModelInfo.TemplateType templateType = calculate(type);
            switch (templateType)
            {
                case BLOCK -> {
                    Map<String, Object> nested = new LinkedHashMap<>();
                    nested.put("use_template", "template_block");
                    data.put(type.toString(), nested);
                }
                case ITEM_2D -> {
                    Map<String, Object> nested = new LinkedHashMap<>();
                    nested.put("use_template", "template_2d_item");
                    data.put(type.toString(), nested);
                }
            }
        }

        DumperOptions settings = new DumperOptions();
        settings.setIndent(2);
        settings.setPrettyFlow(true);
        settings.setExplicitStart(false);
        settings.setExplicitEnd(false);
        settings.setCanonical(false);
        settings.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(settings);
        FileWriter writer;
        try
        {
            writer = new FileWriter(SAVE_PATH);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        yaml.dump(data, writer);
    }

    public CachedMaterialModelInfo.TemplateType calculate(Material type)
    {
        CachedMaterialModelInfo.TemplateType templateType = CachedMaterialModelInfo.TemplateType.ITEM_2D;

        // WARNING: this also matched PISTON_HEAD, but this item can't be obtained, so it's okay.
        if(type.toString().endsWith("_HEAD") || type.toString().endsWith("_SKULL "))
        {
            templateType = CachedMaterialModelInfo.TemplateType.BLOCK;
        }
        else
        {
            JsonObject mainJson = readFromJar("internal/vanilla/item/" + type.toString().toLowerCase() + ".json");
            if (mainJson != null)
            {
                if (!mainJson.has("parent"))
                {
                    System.err.println("No parent info for " + type);
                    templateType = CachedMaterialModelInfo.TemplateType.ITEM_2D;
                }

                String parent = mainJson.get("parent").getAsString();
                boolean hasElements = mainJson.has("elements");
                // Check if any of the parents has 'elements' tag
                if (!hasElements)
                {
                    if (!(parent.startsWith("minecraft:item/generated") || parent.startsWith("item/generated/")))
                    {
                        boolean hasParent = true;
                        while (hasParent)
                        {
                            JsonObject subJson = readFromJar("internal/vanilla/" + parent.replace("minecraft:", "") + ".json");
                            if (subJson == null)
                            {
                                // For example "builtin/entity"
//                                layDown = false;
                                hasParent = false;
                            }
                            else
                            {
                                if (subJson.has("parent"))
                                {
                                    hasParent = true;
                                    parent = subJson.get("parent").getAsString();

                                    // Normal block
                                    if (parent.endsWith("block/cube_all") || parent.endsWith("block/block"))
                                    {
                                        templateType = CachedMaterialModelInfo.TemplateType.BLOCK;
                                        break;
                                    }
                                    else if (parent.endsWith("block/thin_block"))
                                    {
                                        templateType = CachedMaterialModelInfo.TemplateType.BLOCK;
                                        break;
                                    }
                                }
                                else
                                {
                                    hasParent = false;
                                }
                            }
                        }
                    }
                    else
                    {
                        // It's a normal 2D item, I can just place it as usual.
                        templateType = CachedMaterialModelInfo.TemplateType.ITEM_2D;
                    }
                }
            }
            else
            {
                System.err.println("Can't find vanilla model info for " + type);
                templateType = CachedMaterialModelInfo.TemplateType.ITEM_2D;
            }
        }
        return templateType;
    }

    @Nullable
    public static JsonObject readFromJar(String path)
    {
        InputStream inputStream = MainTest.class.getResourceAsStream(path);
        if (inputStream == null)
            return null;

        try
        {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            char[] buffer = new char[4096];
            StringBuilder sb = new StringBuilder();
            for(int len; (len = inputStreamReader.read(buffer)) > 0;)
                sb.append(buffer, 0, len);

            String jsonStr = sb.toString();
            return gson.fromJson(jsonStr, JsonObject.class);

        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }
}