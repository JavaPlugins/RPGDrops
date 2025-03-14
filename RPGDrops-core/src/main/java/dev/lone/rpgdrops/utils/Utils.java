package dev.lone.rpgdrops.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Utils
{
    /**
     * Gets nearby players within the specified radius (bounding box)
     * @param location Location
     * @param radius Radius
     * @return the collection of players near location. This will always be a non-null collection.
     */
    public static List<Player> getNearbyPlayers(Location location, int radius)
    {
        List<Player> near = new ArrayList<>();
        List<Player> all = location.getWorld().getPlayers();
        for(Player player : all)
        {
            if (player.getWorld() == location.getWorld() && player.getLocation().distance(location) <= radius)
                near.add(player);
        }
        return near;
    }

    public static boolean give(Player player, ItemStack ...itemStack)
    {
        HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(itemStack);
        return remainingItems.isEmpty();
    }

    private static java.awt.Color hex2Rgb(String colorStr)
    {
        try
        {
            return new java.awt.Color(
                    Integer.valueOf(colorStr.substring(1, 3), 16),
                    Integer.valueOf(colorStr.substring(3, 5), 16),
                    Integer.valueOf(colorStr.substring(5, 7), 16)
            );
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Integer hexToIntColor(String colorStr)
    {
        if (colorStr == null)
            return null;
        //fix java.lang.NumberFormatException: For input string: ".0"
        if (colorStr.endsWith(".0"))
            colorStr = colorStr.replace(".0", "");
        colorStr = colorStr.replace(".", "");
        //fix java.lang.StringIndexOutOfBoundsException: String index out of range: 7
        while (colorStr.length() < 7)
            colorStr = colorStr + "0";
        if (!colorStr.startsWith("#"))
            colorStr = "#" + colorStr;

        java.awt.Color javaColor = hex2Rgb(colorStr);
        if(javaColor == null)
            return null;
        return javaColor.getRGB();
    }
}
