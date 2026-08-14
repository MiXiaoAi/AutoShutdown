package com.mixiaoai.autoshutdown;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(ShutdownMod.MODID)
public class NeoAutoShutdown
{
    public NeoAutoShutdown(IEventBus modBus, ModContainer container)
    {
        Platform.set(new NeoPlatform());

        container.registerConfig(ModConfig.Type.SERVER, NeoConfig.SPEC);
        NeoConfig.init();

        modBus.addListener(this::onConfigChanged);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onConfigChanged(ModConfigEvent event)
    {
        ShutdownMod.onConfigChanged();
    }

    private void onRegisterCommands(RegisterCommandsEvent event)
    {
        ModCommands.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event)
    {
        ShutdownMod.onServerStarting(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event)
    {
        ShutdownMod.onServerStopping();
    }
}