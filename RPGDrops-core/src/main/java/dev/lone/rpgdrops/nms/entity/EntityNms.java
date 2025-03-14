package dev.lone.rpgdrops.nms.entity;

import dev.lone.rpgdrops.nms.Implementation;

@SuppressWarnings("rawtypes")
public class EntityNms
{
    public IEntityNms nms;
    static EntityNms instance;

    EntityNms()
    {
        nms = Implementation.get(IEntityNms.class);
    }

    public static IEntityNms nms()
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
        instance = new EntityNms();
    }
}
