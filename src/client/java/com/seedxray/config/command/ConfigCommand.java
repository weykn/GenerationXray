package com.seedxray.config.command;

import com.seedxray.config.handler.ConfigHandler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** {@code /gx <setting> [value]} - the same settings the config screen shows */
public class ConfigCommand {
    private final ConfigHandler handler;

    public ConfigCommand(ConfigHandler handler) {
        this.handler = handler;
    }

    public void attachTo(LiteralArgumentBuilder<FabricClientCommandSource> parent) {
        parent.then(toggle("active", handler::displayActive, handler::setActive));
        parent.then(ClientCommands.literal("seed")
                .executes(ctx -> {
                    handler.displaySeed();
                    return 1;
                })
                .then(ClientCommands.argument("seed", LongArgumentType.longArg())
                        .executes(ctx -> {
                            handler.setSeed(LongArgumentType.getLong(ctx, "seed"));
                            return 1;
                        })));
        parent.then(toggle("filled", handler::displayRenderFilled, handler::setRenderFilled));
        parent.then(number("distance", handler::displayRenderDistance, handler::setRenderDistance));
        parent.then(number("vdistance", handler::displayVerticalViewDistance, handler::setVerticalViewDistance));
        parent.then(toggle("hud", handler::displayHudStatusIndicator, handler::setHudStatusIndicator));
        parent.then(blocks());
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> toggle(
            String name, Runnable display, Consumer<Boolean> set) {
        return ClientCommands.literal(name)
                .executes(ctx -> {
                    display.run();
                    return 1;
                })
                .then(ClientCommands.literal("true").executes(ctx -> {
                    set.accept(true);
                    return 1;
                }))
                .then(ClientCommands.literal("false").executes(ctx -> {
                    set.accept(false);
                    return 1;
                }));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> number(
            String name, Runnable display, IntConsumer set) {
        return ClientCommands.literal(name)
                .executes(ctx -> {
                    display.run();
                    return 1;
                })
                .then(ClientCommands.argument("number", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            set.accept(IntegerArgumentType.getInteger(ctx, "number"));
                            return 1;
                        }));
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> blocks() {
        return ClientCommands.literal("blocks")
                .executes(ctx -> {
                    handler.displayBlocks();
                    return 1;
                })
                .then(ClientCommands.literal("add").then(blockArgument(handler::addBlock)))
                .then(ClientCommands.literal("remove").then(blockArgument(handler::removeBlock)))
                .then(ClientCommands.literal("reset").executes(ctx -> {
                    handler.resetBlocks();
                    return 1;
                }));
    }

    private static ArgumentBuilder<FabricClientCommandSource, ?> blockArgument(Consumer<Identifier> action) {
        return ClientCommands.argument("block", IdentifierArgument.id())
                .suggests((ctx, builder) ->
                        SharedSuggestionProvider.suggestResource(BuiltInRegistries.BLOCK.keySet(), builder))
                .executes(ctx -> {
                    action.accept(ctx.getArgument("block", Identifier.class));
                    return 1;
                });
    }
}
