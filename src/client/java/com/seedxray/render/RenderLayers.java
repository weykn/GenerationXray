package com.seedxray.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public class RenderLayers {
    // 26.1 moved depth testing and blending out of the pipeline builder and into
    // these two state records.
    private static final DepthStencilState NO_DEPTH_TEST =
            new DepthStencilState(CompareOp.ALWAYS_PASS, false);
    private static final ColorTargetState TRANSLUCENT =
            new ColorTargetState(BlendFunction.TRANSLUCENT);

    // 26.2 dropped the prebuilt matrices-projection snippet; the matrix uniforms are
    // now pulled in as a bind group layout on top of the globals snippet.
    private static final RenderPipeline.Snippet FOGLESS_LINES_SNIPPET = RenderPipeline
            .builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader(Identifier.parse("seedxray:fogless_lines"))
            .withFragmentShader(Identifier.parse("seedxray:fogless_lines"))
            .withColorTargetState(TRANSLUCENT).withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
            .withPrimitiveTopology(PrimitiveTopology.LINES)
            .buildSnippet();

    public static final RenderPipeline ESP_LINES_PIPELINE =
            RenderPipelines.register(RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
                    .withLocation(Identifier.parse("seedxray:pipeline/esp_lines"))
                    .withDepthStencilState(NO_DEPTH_TEST).build());

    public static final RenderPipeline ESP_QUADS_PIPELINE = RenderPipelines
            .register(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withLocation(Identifier.parse("seedxray:pipeline/esp_quads"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withColorTargetState(TRANSLUCENT).withCull(false)
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
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
