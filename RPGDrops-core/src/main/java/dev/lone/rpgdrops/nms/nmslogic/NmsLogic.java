package dev.lone.rpgdrops.nms.nmslogic;

import dev.lone.rpgdrops.nms.Implementation;

public class NmsLogic
{
    public INmsLogic nms;
    static NmsLogic instance;

    NmsLogic()
    {
        nms = Implementation.get(INmsLogic.class);
    }

    public static INmsLogic nms()
    {
        if(!available())
            init();
        return instance.nms;
    }

    public static boolean available()
    {
        return instance != null && instance.nms != null;
    }

    public static void init()
    {
        instance = new NmsLogic();
    }
}
