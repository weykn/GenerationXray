package com.seedxray.render;

import com.seedxray.SeedXRayTickService;
import com.seedxray.config.SeedXRayConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class HudRenderer {
    public static void render(GuiGraphicsExtractor ctx, SeedXRayTickService.LoadStatus status, int oreCount) {
        if (!AutoConfig.getConfigHolder(SeedXRayConfig.class).getConfig().hudStatusIndicator) {
            return;
        }

        Component text = Component.literal(status.name() + " (" + oreCount + " ores)");

        Font renderer = Minecraft.getInstance().font;
        ctx.text(
                renderer,
                text,
                2, ctx.guiHeight() - renderer.lineHeight - 1, 0XFFFFFFFF,
                true
        );
    }
}
