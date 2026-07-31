package com.seedxray.config.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class ScreenUtil {
    private static final String ELLIPSIS = "...";

    private ScreenUtil() {
    }

    /** the block rows have long names in narrow space, so they get cut instead of overlapping */
    static FormattedCharSequence trim(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text.getVisualOrderText();
        }
        int room = Math.max(0, maxWidth - font.width(ELLIPSIS));
        return Component.literal(font.substrByWidth(text, room).getString() + ELLIPSIS).getVisualOrderText();
    }
}
