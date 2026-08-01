package com.seedxray.config.screen;

import com.seedxray.config.SeedXRayConfig;
import com.seedxray.config.SeedXRayConfig.ConfigConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The whole config on one screen: no tabs, one row per setting, and a button down
 * to the block list.
 */
public class SeedXRayConfigScreen extends Screen {
    static final int ROW_WIDTH = 310;
    private static final int ROW_HEIGHT = 20;
    private static final int LABEL_WIDTH = 120;
    private static final int FOOTER_BUTTON_WIDTH = 151;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;

    private final Screen parent;
    private final ConfigHolder<SeedXRayConfig> configHolder;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    /** last valid seed typed into the box, null while the box holds junk */
    private Long pendingSeed;

    private Button blocksButton;

    public SeedXRayConfigScreen(Screen parent) {
        super(Component.translatable("seedxray.config.title"));
        this.parent = parent;
        this.configHolder = AutoConfig.getConfigHolder(SeedXRayConfig.class);
    }

    @Override
    protected void init() {
        SeedXRayConfig config = configHolder.getConfig();

        layout.addTitleHeader(this.title, this.font);

        LinearLayout rows = layout.addToContents(LinearLayout.vertical().spacing(4));

        rows.addChild(yesNo(Component.translatable("seedxray.config.active"),
                config.active,
                value -> {
                    config.active = value;
                    save();
                }));

        rows.addChild(seedRow(config));

        this.blocksButton = Button.builder(
                        blocksLabel(config),
                        button -> this.minecraft.setScreen(new BlockListScreen(this)))
                .bounds(0, 0, ROW_WIDTH, ROW_HEIGHT)
                .build();
        rows.addChild(this.blocksButton);

        rows.addChild(yesNo(Component.translatable("seedxray.config.render_filled"),
                config.renderFilled,
                value -> {
                    config.renderFilled = value;
                    save();
                }));

        rows.addChild(new IntSliderWidget(ROW_WIDTH, ROW_HEIGHT,
                Component.translatable("seedxray.config.fill_opacity"),
                ConfigConstants.MIN_FILL_OPACITY, ConfigConstants.MAX_FILL_OPACITY,
                config.fillOpacity,
                value -> Component.literal(value + "%"),
                value -> {
                    config.fillOpacity = value;
                    save();
                }));

        rows.addChild(new IntSliderWidget(ROW_WIDTH, ROW_HEIGHT,
                Component.translatable("seedxray.config.render_distance"),
                ConfigConstants.MIN_RENDER_DISTANCE, ConfigConstants.MAX_RENDER_DISTANCE,
                config.renderDistance,
                value -> Component.literal(String.valueOf(value)),
                value -> {
                    config.renderDistance = value;
                    save();
                }));

        rows.addChild(new IntSliderWidget(ROW_WIDTH, ROW_HEIGHT,
                Component.translatable("seedxray.config.vertical_view_distance"),
                ConfigConstants.MIN_VERTICAL_VIEW_DISTANCE, ConfigConstants.MAX_VERTICAL_VIEW_DISTANCE,
                config.verticalViewDistance,
                value -> value == ConfigConstants.MIN_VERTICAL_VIEW_DISTANCE
                        ? Component.translatable("seedxray.config.vertical_view_distance.unlimited")
                        : Component.literal(String.valueOf(value)),
                value -> {
                    config.verticalViewDistance = value;
                    save();
                }));

        rows.addChild(yesNo(Component.translatable("seedxray.config.hud_status_indicator"),
                config.hudStatusIndicator,
                value -> {
                    config.hudStatusIndicator = value;
                    save();
                }));

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(
                        Component.translatable("seedxray.config.reset"), button -> reset())
                .width(FOOTER_BUTTON_WIDTH)
                .build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .width(FOOTER_BUTTON_WIDTH)
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    private LinearLayout seedRow(SeedXRayConfig config) {
        LinearLayout row = LinearLayout.horizontal().spacing(0);

        row.addChild(new StringWidget(LABEL_WIDTH, ROW_HEIGHT,
                Component.translatable("seedxray.config.seed"), this.font));

        EditBox seedBox = new EditBox(this.font, ROW_WIDTH - LABEL_WIDTH, ROW_HEIGHT,
                Component.translatable("seedxray.config.seed"));
        seedBox.setMaxLength(24);
        seedBox.setValue(String.valueOf(config.seed));
        seedBox.setResponder(text -> {
            try {
                // applied when the screen closes: every change regenerates the whole
                // region, and half typed seeds are not worth generating
                this.pendingSeed = Long.parseLong(text.trim());
                seedBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
            } catch (NumberFormatException e) {
                // keep the last valid seed, just tell the player this one is not one
                this.pendingSeed = null;
                seedBox.setTextColor(INVALID_TEXT_COLOR);
            }
        });
        row.addChild(seedBox);

        return row;
    }

    private CycleButton<Boolean> yesNo(Component label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        return CycleButton.booleanBuilder(
                        Component.translatable("seedxray.config.yes"),
                        Component.translatable("seedxray.config.no"),
                        initial)
                .create(0, 0, ROW_WIDTH, ROW_HEIGHT, label, (button, value) -> onChange.accept(value));
    }

    private static Component blocksLabel(SeedXRayConfig config) {
        return Component.translatable("seedxray.config.blocks", config.blocks.size());
    }

    private void save() {
        configHolder.save();
    }

    /** every setting back to its default, block list included */
    private void reset() {
        configHolder.resetToDefault();
        this.pendingSeed = null;
        save();
        // the layout is built once, so a fresh screen is what gets every widget
        // showing the defaults again
        this.minecraft.setScreen(new SeedXRayConfigScreen(this.parent));
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    /** the count on the button goes stale while the player is off editing the list */
    @Override
    public void added() {
        if (this.blocksButton != null) {
            this.blocksButton.setMessage(blocksLabel(configHolder.getConfig()));
        }
    }

    /** also runs on the way to the block list, so a typed seed survives the detour */
    @Override
    public void removed() {
        if (this.pendingSeed != null) {
            configHolder.getConfig().seed = this.pendingSeed;
            this.pendingSeed = null;
        }
        save();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
