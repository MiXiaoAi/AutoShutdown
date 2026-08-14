package com.mixiaoai.autoshutdown;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ShutdownMod.MODID)
public class AutoShutdown
{
    public AutoShutdown()
    {
        Platform.set(new ForgePlatform());

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ForgeConfig.SPEC);
        ForgeConfig.init();

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onRegisterCommands(RegisterCommandsEvent event)
    {
        ModCommands.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event)
    {
        ShutdownMod.onServerStarting(event.getServer());
    }
}