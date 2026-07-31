package com.seedxray.config.screen;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** slider over a small int range, shown as "label: value" */
public class IntSliderWidget extends AbstractSliderButton {
    private final Component label;
    private final int min;
    private final int max;
    private final IntFunction<Component> formatter;
    private final IntConsumer onChange;

    public IntSliderWidget(
            int width, int height,
            Component label,
            int min, int max, int value,
            IntFunction<Component> formatter,
            IntConsumer onChange) {
        super(0, 0, width, height, CommonComponents.EMPTY, toSliderValue(value, min, max));
        this.label = label;
        this.min = min;
        this.max = max;
        this.formatter = formatter;
        this.onChange = onChange;
        updateMessage();
    }

    private static double toSliderValue(int value, int min, int max) {
        if (max <= min) {
            return 0.0;
        }
        int clamped = Math.max(min, Math.min(max, value));
        return (double) (clamped - min) / (max - min);
    }

    private int intValue() {
        return this.min + (int) Math.round(this.value * (this.max - this.min));
    }

    @Override
    protected void updateMessage() {
        // the super constructor can reach this before the fields are in place
        if (this.formatter == null) {
            return;
        }
        setMessage(CommonComponents.optionNameValue(this.label, this.formatter.apply(intValue())));
    }

    @Override
    protected void applyValue() {
        if (this.onChange != null) {
            this.onChange.accept(intValue());
        }
    }
}
