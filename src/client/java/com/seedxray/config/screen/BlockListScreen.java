package com.seedxray.config.screen;

import com.seedxray.config.BlockEntry;
import com.seedxray.config.SeedXRayConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** the list of tracked blocks, one row per block: colour on the left, remove on the right */
public class BlockListScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_WIDTH = 320;
    private static final int ICON_SIZE = 16;
    private static final int SWATCH_SIZE = 12;
    private static final int COLOR_BOX_WIDTH = 58;
    private static final int REMOVE_WIDTH = 20;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;

    private final Screen parent;
    private final ConfigHolder<SeedXRayConfig> configHolder;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    private BlockList list;

    public BlockListScreen(Screen parent) {
        super(Component.translatable("seedxray.config.blocks.title"));
        this.parent = parent;
        this.configHolder = AutoConfig.getConfigHolder(SeedXRayConfig.class);
    }

    @Override
    protected void init() {
        layout.addTitleHeader(this.title, this.font);

        this.list = layout.addToContents(new BlockList(this.minecraft));

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(
                        Component.translatable("seedxray.config.blocks.add"),
                        button -> this.minecraft.gui.setScreen(new BlockPickerScreen(this)))
                .width(150)
                .build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .width(150)
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    /**
     * Coming back from the picker hands the player the same screen instance, and
     * Screen#init only ever runs once, so the rows have to be pulled in again here.
     */
    @Override
    public void added() {
        if (this.list != null) {
            this.list.rebuild();
        }
    }

    List<BlockEntry> entries() {
        return configHolder.getConfig().blocks;
    }

    void save() {
        configHolder.save();
    }

    @Override
    public void onClose() {
        save();
        this.minecraft.gui.setScreen(this.parent);
    }

    private class BlockList extends ContainerObjectSelectionList<BlockList.Entry> {
        BlockList(Minecraft minecraft) {
            super(minecraft, BlockListScreen.this.width,
                    BlockListScreen.this.layout.getContentHeight(),
                    BlockListScreen.this.layout.getHeaderHeight(),
                    ROW_HEIGHT);
            rebuild();
        }

        void rebuild() {
            clearEntries();
            for (BlockEntry entry : entries()) {
                addEntry(new Entry(entry));
            }
        }

        @Override
        public int getRowWidth() {
            return LIST_WIDTH;
        }

        private class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            private final BlockEntry data;
            private final ItemStack icon;
            private final Component name;
            private final EditBox colorBox;
            private final Button removeButton;

            Entry(BlockEntry data) {
                this.data = data;
                Block block = data.resolve();
                this.icon = new ItemStack(block);
                this.name = block == Blocks.AIR
                        ? Component.literal(data.block)
                        : block.getName();

                this.colorBox = new EditBox(BlockListScreen.this.font, COLOR_BOX_WIDTH, 16,
                        Component.translatable("seedxray.config.blocks.color"));
                this.colorBox.setMaxLength(6);
                this.colorBox.setValue(String.format("%06X", data.rgb & 0xFFFFFF));
                this.colorBox.setResponder(text -> {
                    try {
                        int rgb = Integer.parseInt(text.trim(), 16);
                        this.colorBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                        // takes effect right away, it gets written out when the screen closes
                        this.data.rgb = rgb & 0xFFFFFF;
                    } catch (NumberFormatException e) {
                        this.colorBox.setTextColor(INVALID_TEXT_COLOR);
                    }
                });

                this.removeButton = Button.builder(
                                Component.translatable("seedxray.config.blocks.remove"),
                                button -> {
                                    entries().removeIf(entry -> entry == this.data);
                                    save();
                                    // rebuilding mid-click would pull the row out from under
                                    // the button that is still being handled
                                    BlockListScreen.this.minecraft.execute(BlockList.this::rebuild);
                                })
                        .size(REMOVE_WIDTH, 16)
                        .build();
            }

            @Override
            public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int left = getContentX();
                int middle = getContentYMiddle();
                int right = getContentRight();

                ctx.item(this.icon, left, middle - ICON_SIZE / 2);

                this.removeButton.setPosition(right - REMOVE_WIDTH, middle - 8);
                this.colorBox.setPosition(right - REMOVE_WIDTH - 4 - COLOR_BOX_WIDTH, middle - 8);

                int swatchX = this.colorBox.getX() - 4 - SWATCH_SIZE;
                int swatchY = middle - SWATCH_SIZE / 2;
                ctx.fill(swatchX - 1, swatchY - 1, swatchX + SWATCH_SIZE + 1, swatchY + SWATCH_SIZE + 1, 0xFF000000);
                ctx.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE,
                        0xFF000000 | (this.data.rgb & 0xFFFFFF));

                int nameLeft = left + ICON_SIZE + 6;
                ctx.text(BlockListScreen.this.font,
                        ScreenUtil.trim(BlockListScreen.this.font, this.name, swatchX - 6 - nameLeft),
                        nameLeft, middle - BlockListScreen.this.font.lineHeight / 2,
                        0xFFFFFFFF, false);

                this.colorBox.extractRenderState(ctx, mouseX, mouseY, partialTick);
                this.removeButton.extractRenderState(ctx, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(this.colorBox, this.removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(this.colorBox, this.removeButton);
            }
        }
    }
}
