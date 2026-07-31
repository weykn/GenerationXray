package com.seedxray.config.handler;

import com.seedxray.SeedXRay;
import com.seedxray.config.SeedXRayConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

public abstract class AbstractConfigHandler {
    protected static final String PREFIX = "(" + SeedXRay.MOD_NAME + ") ";
    protected final ConfigHolder<SeedXRayConfig> configHolder;
    protected AbstractConfigHandler(ConfigHolder<SeedXRayConfig> configHolder) {
        this.configHolder = configHolder;
    }

    protected static void displayMessage(Component text) {
        ChatComponent chatHud = Minecraft.getInstance().gui.getChat();
        if (chatHud == null) {
            return;
        }

        chatHud.addClientSystemMessage(text);
    }
}
