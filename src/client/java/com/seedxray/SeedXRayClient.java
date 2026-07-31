package com.seedxray;

import com.casiowatch123.vchunklib.generation.virtual.VUtils;
import com.seedxray.config.SeedXRayConfig;
import com.seedxray.config.command.ConfigCommand;
import com.seedxray.config.handler.ConfigHandler;
import com.seedxray.render.HudRenderer;
import com.seedxray.render.OverlayRenderer;
import com.seedxray.world.OreChunkLoader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;

import static com.seedxray.SeedXRay.MOD_ID;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

public class SeedXRayClient implements ClientModInitializer {
    public static Logger LOGGER = SeedXRay.LOGGER;
    public static RegistryAccess DRM;
    
	@Override
	public void onInitializeClient() {
        migrateLegacyConfig();

        ConfigHolder<SeedXRayConfig> configHolder = AutoConfig.register(SeedXRayConfig.class,
                GsonConfigSerializer::new);

        LiteralArgumentBuilder<FabricClientCommandSource> rootCommand = ClientCommands.literal("gx");
        ConfigHandler configHandler = new ConfigHandler(configHolder);
        new ConfigCommand(configHandler).attachTo(rootCommand);


        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(rootCommand);
        });

        KeyMapping toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.seedxray.toggle",
                        InputConstants.UNKNOWN.getValue(), 
                        new KeyMapping.Category(Identifier.fromNamespaceAndPath("seedxray", "modname")))
        );
        
        ClientLifecycleEvents.CLIENT_STARTED.register(c -> {
            ForkJoinPool.commonPool().execute(() -> {
                DRM = VUtils.createVanillaRegistryManager();
            });
        });
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean flag = false;
            while(toggleKey.consumeClick()) {
                flag = true;
            }
            if (flag) {
                configHandler.toggleActive();
            }
        });
        
        SeedXRayTickService tickService = new SeedXRayTickService(configHolder);

        ClientTickEvents.END_CLIENT_TICK.register(tickService::updateTick);

        // geometry has to go in during the collect phase - anything submitted later
        // (END_MAIN) is never drawn, the submit nodes are already flushed by then
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
            OverlayRenderer.render(ctx,
                    tickService.getOreRenderData(),
                    configHolder.getConfig().renderFilled);
        });
        
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "loadstatus"),
                (GuiGraphicsExtractor ctx, DeltaTracker tracker) -> {
                    HudRenderer.render(ctx, tickService.getLoadStatus(), tickService.getRenderedOreCount());
                }
        );

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            configHolder.save();
            OreChunkLoader.shutdown();
        });
    }

    /**
     * The config used to be written under the old mod name. Carrying it over keeps the
     * seed and the block list of anyone who ran a build from before the rename.
     */
    private static void migrateLegacyConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path legacy = configDir.resolve("generationxray.json");
        Path current = configDir.resolve("seedxray.json");
        if (!Files.isRegularFile(legacy) || Files.exists(current)) {
            return;
        }

        try {
            Files.move(legacy, current);
            LOGGER.info("Migrated config from {} to {}", legacy.getFileName(), current.getFileName());
        } catch (IOException e) {
            LOGGER.warn("Could not migrate the old config file, starting from defaults", e);
        }
    }
}