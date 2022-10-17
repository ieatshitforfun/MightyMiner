package com.jelly.MightyMiner.debug;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public class DebugHandler {
    public DebugHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }


}
