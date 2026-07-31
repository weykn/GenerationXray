package com.seedxray.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class RenderLayers {
    // 26.1 moved depth testing and blending out of the pipeline builder and into
    // these two state records.
    private static final DepthStencilState NO_DEPTH_TEST =
            new DepthStencilState(CompareOp.ALWAYS_PASS, false);
    private static final ColorTargetState TRANSLUCENT =
            new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), ColorTargetState.WRITE_ALL);

    private static final RenderPipeline.Snippet FOGLESS_LINES_SNIPPET = RenderPipeline
            .builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET,
                    RenderPipelines.GLOBALS_SNIPPET)
            .withVertexShader(Identifier.parse("seedxray:fogless_lines"))
            .withFragmentShader(Identifier.parse("seedxray:fogless_lines"))
            .withColorTargetState(TRANSLUCENT).withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
                    VertexFormat.Mode.LINES)
            .buildSnippet();

    public static final RenderPipeline ESP_LINES_PIPELINE =
            RenderPipelines.register(RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
                    .withLocation(Identifier.parse("seedxray:pipeline/esp_lines"))
                    .withDepthStencilState(NO_DEPTH_TEST).build());

    public static final RenderPipeline ESP_QUADS_PIPELINE = RenderPipelines
            .register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.parse("seedxray:pipeline/esp_quads"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withColorTargetState(TRANSLUCENT).withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(NO_DEPTH_TEST).build());

    public static final RenderType ESP_LINES =
            RenderType.create("seedxray:esp_lines",
                    RenderSetup.builder(ESP_LINES_PIPELINE)
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                            .createRenderSetup());

    public static final RenderType ESP_QUADS = RenderType.create(
            "seedxray:esp_quads", RenderSetup.builder(ESP_QUADS_PIPELINE)
                    .sortOnUpload()
                    .createRenderSetup());
}
