package com.seedxray.config.handler;

import com.seedxray.config.BlockEntry;
import com.seedxray.config.SeedXRayConfig;
import com.seedxray.config.SeedXRayConfig.ConfigConstants;
import com.seedxray.ore.DefaultBlocks;
import me.shedaniel.autoconfig.ConfigHolder;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The chat side of the config: every setting the config screen shows can also be
 * read and written from {@code /gx}.
 */
public class ConfigHandler extends AbstractConfigHandler {
    public ConfigHandler(ConfigHolder<SeedXRayConfig> configHolder) {
        super(configHolder);
    }

    private SeedXRayConfig config() {
        return configHolder.getConfig();
    }

    private void changed(String name, Component value) {
        configHolder.save();
        displayMessage(Component.literal(PREFIX + "Changed " + name + ": ").append(value));
    }

    private static void display(String name, Component value) {
        displayMessage(Component.literal(PREFIX + name + ": ").append(value));
    }

    private static Component bool(boolean value) {
        return value
                ? Component.literal("true").withStyle(ChatFormatting.GREEN)
                : Component.literal("false").withStyle(ChatFormatting.RED);
    }

    private static Component number(long value) {
        return Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GREEN);
    }

    private static void displayRange(int min, int max) {
        displayMessage(
                Component.literal(PREFIX + "wrong argument: int ")
                        .append(Component.literal("["))
                        .append(Component.literal(min + " ").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("~"))
                        .append(Component.literal(" " + max).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("]")));
    }

    public void displayActive() {
        display("active", bool(config().active));
    }

    public void setActive(boolean value) {
        if (config().active != value) {
            config().active = value;
            changed("active", bool(value));
        }
    }

    public void toggleActive() {
        setActive(!config().active);
    }

    public void displaySeed() {
        display("seed", number(config().seed));
    }

    /**
     * The tick service picks the new seed up on the next tick and regenerates the
     * region, so there is no need to rejoin the world.
     */
    public void setSeed(long value) {
        if (config().seed == value) {
            displaySeed();
            return;
        }
        config().seed = value;
        changed("seed", number(value));
    }

    public void displayRenderFilled() {
        display("render filled", bool(config().renderFilled));
    }

    public void setRenderFilled(boolean value) {
        if (config().renderFilled != value) {
            config().renderFilled = value;
            changed("render filled", bool(value));
        }
    }

    public void displayFillOpacity() {
        display("fill opacity", number(config().fillOpacity));
    }

    public void setFillOpacity(int value) {
        if (value < ConfigConstants.MIN_FILL_OPACITY || value > ConfigConstants.MAX_FILL_OPACITY) {
            displayRange(ConfigConstants.MIN_FILL_OPACITY, ConfigConstants.MAX_FILL_OPACITY);
            return;
        }
        config().fillOpacity = value;
        changed("fill opacity", number(value));
    }

    public void displayRenderDistance() {
        display("render distance", number(config().renderDistance));
    }

    public void setRenderDistance(int value) {
        if (value < ConfigConstants.MIN_RENDER_DISTANCE || value > ConfigConstants.MAX_RENDER_DISTANCE) {
            displayRange(ConfigConstants.MIN_RENDER_DISTANCE, ConfigConstants.MAX_RENDER_DISTANCE);
            return;
        }
        config().renderDistance = value;
        changed("render distance", number(value));
    }

    public void displayVerticalViewDistance() {
        display("vertical view distance", number(config().verticalViewDistance));
    }

    public void setVerticalViewDistance(int value) {
        if (value < ConfigConstants.MIN_VERTICAL_VIEW_DISTANCE
                || value > ConfigConstants.MAX_VERTICAL_VIEW_DISTANCE) {
            displayRange(ConfigConstants.MIN_VERTICAL_VIEW_DISTANCE, ConfigConstants.MAX_VERTICAL_VIEW_DISTANCE);
            return;
        }
        config().verticalViewDistance = value;
        changed("vertical view distance", number(value));
    }

    public void displayHudStatusIndicator() {
        display("hud status indicator", bool(config().hudStatusIndicator));
    }

    public void setHudStatusIndicator(boolean value) {
        if (config().hudStatusIndicator != value) {
            config().hudStatusIndicator = value;
            changed("hud status indicator", bool(value));
        }
    }

    public void displayBlocks() {
        List<BlockEntry> blocks = config().blocks;
        if (blocks.isEmpty()) {
            displayMessage(Component.literal(PREFIX + "no blocks tracked"));
            return;
        }
        MutableComponent list = Component.literal(PREFIX + "tracked blocks (" + blocks.size() + "): ");
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                list.append(Component.literal(", "));
            }
            list.append(Component.literal(blocks.get(i).block).withStyle(ChatFormatting.GREEN));
        }
        displayMessage(list);
    }

    public void addBlock(Identifier id) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == Blocks.AIR && !id.equals(BuiltInRegistries.BLOCK.getKey(Blocks.AIR))) {
            displayMessage(Component.literal(PREFIX + "unknown block: ")
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.RED)));
            return;
        }
        String key = BuiltInRegistries.BLOCK.getKey(block).toString();
        if (config().blocks.stream().anyMatch(entry -> key.equals(entry.block))) {
            displayMessage(Component.literal(PREFIX + "already tracked: ")
                    .append(Component.literal(key).withStyle(ChatFormatting.GREEN)));
            return;
        }
        config().blocks.add(new BlockEntry(key, DefaultBlocks.colorFor(block)));
        changed("blocks", Component.literal("+" + key).withStyle(ChatFormatting.GREEN));
    }

    public void removeBlock(Identifier id) {
        String key = id.toString();
        if (config().blocks.removeIf(entry -> key.equals(entry.block))) {
            changed("blocks", Component.literal("-" + key).withStyle(ChatFormatting.RED));
        } else {
            displayMessage(Component.literal(PREFIX + "not tracked: ")
                    .append(Component.literal(key).withStyle(ChatFormatting.RED)));
        }
    }

    public void resetBlocks() {
        config().blocks = DefaultBlocks.defaultEntries();
        changed("blocks", Component.literal("defaults").withStyle(ChatFormatting.GREEN));
    }
}
