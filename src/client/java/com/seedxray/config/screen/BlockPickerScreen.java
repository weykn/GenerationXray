package com.seedxray.config.screen;

import com.seedxray.config.BlockEntry;
import com.seedxray.config.SeedXRayConfig;
import com.seedxray.ore.DefaultBlocks;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** every block in the registry, searchable - click one to start tracking it */
public class BlockPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_WIDTH = 320;
    private static final int ICON_SIZE = 16;
    private static final int HEADER_HEIGHT = 60;

    private final BlockListScreen parent;
    private final ConfigHolder<SeedXRayConfig> configHolder;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    private EditBox searchBox;
    private CandidateList list;
    private String query = "";

    public BlockPickerScreen(BlockListScreen parent) {
        super(Component.translatable("seedxray.config.blocks.picker.title"));
        this.parent = parent;
        this.configHolder = AutoConfig.getConfigHolder(SeedXRayConfig.class);
    }

    @Override
    protected void init() {
        // the header carries the search box on top of the title
        layout.setHeaderHeight(HEADER_HEIGHT);

        LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(6));
        header.addChild(new StringWidget(this.title, this.font));

        this.searchBox = new EditBox(this.font, LIST_WIDTH, 20,
                Component.translatable("seedxray.config.blocks.picker.search"));
        this.searchBox.setHint(Component.translatable("seedxray.config.blocks.picker.search")
                .withStyle(EditBox.SEARCH_HINT_STYLE));
        this.searchBox.setValue(this.query);
        this.searchBox.setResponder(text -> {
            this.query = text;
            if (this.list != null) {
                this.list.rebuild(text);
            }
        });
        header.addChild(this.searchBox);

        this.list = layout.addToContents(new CandidateList(this.minecraft));

        layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                .width(200)
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        setInitialFocus(this.searchBox);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    /** adds the block and stays put, so several can be picked in one visit */
    private void add(Block block) {
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        List<BlockEntry> entries = configHolder.getConfig().blocks;
        if (entries.stream().noneMatch(entry -> id.equals(entry.block))) {
            entries.add(new BlockEntry(id, DefaultBlocks.colorFor(block)));
            configHolder.save();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private class CandidateList extends ObjectSelectionList<CandidateList.Entry> {
        CandidateList(Minecraft minecraft) {
            super(minecraft, BlockPickerScreen.this.width,
                    BlockPickerScreen.this.layout.getContentHeight(),
                    BlockPickerScreen.this.layout.getHeaderHeight(),
                    ROW_HEIGHT);
            rebuild(BlockPickerScreen.this.query);
        }

        void rebuild(String query) {
            String needle = query.toLowerCase(Locale.ROOT).trim();

            Set<String> alreadyTracked = new HashSet<>();
            for (BlockEntry entry : configHolder.getConfig().blocks) {
                alreadyTracked.add(entry.block);
            }

            List<Entry> matches = new ArrayList<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
                    continue;
                }
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                if (alreadyTracked.contains(id.toString())) {
                    continue;
                }
                Component name = block.getName();
                if (!needle.isEmpty()
                        && !name.getString().toLowerCase(Locale.ROOT).contains(needle)
                        && !id.toString().contains(needle)) {
                    continue;
                }
                matches.add(new Entry(block, id, name));
            }

            replaceEntries(matches);
            setScrollAmount(0.0);
        }

        @Override
        public int getRowWidth() {
            return LIST_WIDTH;
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final Block block;
            private final Identifier id;
            private final Component name;
            private final ItemStack icon;

            Entry(Block block, Identifier id, Component name) {
                this.block = block;
                this.id = id;
                this.name = name;
                this.icon = new ItemStack(block);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int left = getContentX();
                int middle = getContentYMiddle();
                int textLeft = left + ICON_SIZE + 6;
                int textWidth = getContentRight() - textLeft;

                ctx.item(this.icon, left, middle - ICON_SIZE / 2);
                ctx.text(BlockPickerScreen.this.font,
                        ScreenUtil.trim(BlockPickerScreen.this.font, this.name, textWidth),
                        textLeft, middle - BlockPickerScreen.this.font.lineHeight - 1,
                        0xFFFFFFFF, false);
                ctx.text(BlockPickerScreen.this.font,
                        ScreenUtil.trim(BlockPickerScreen.this.font,
                                Component.literal(this.id.toString()).withStyle(ChatFormatting.GRAY),
                                textWidth),
                        textLeft, middle + 1,
                        0xFFAAAAAA, false);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                add(this.block);
                // the row is tracked now, so it drops out of the candidates - deferred
                // because the list is still walking its children for this very click
                BlockPickerScreen.this.minecraft.execute(() -> {
                    CandidateList.this.removeEntry(this);
                    CandidateList.this.setSelected(null);
                });
                return true;
            }

            @Override
            public Component getNarration() {
                return this.name;
            }
        }
    }
}
