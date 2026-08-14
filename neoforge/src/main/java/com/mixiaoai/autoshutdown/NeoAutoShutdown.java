package com.mixiaoai.autoshutdown;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(ShutdownMod.MODID)
public class NeoAutoShutdown
{
    public NeoAutoShutdown(IEventBus modBus, ModContainer container)
    {
        container.registerConfig(ModConfig.Type.SERVER, NeoConfig.SPEC);
        NeoConfig.init();

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
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