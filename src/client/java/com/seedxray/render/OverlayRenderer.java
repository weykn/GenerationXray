package com.seedxray.render;

import it.unimi.dsi.fastutil.longs.LongCollection;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class OverlayRenderer {
    private static final double MAX_VIEW_DISTANCE = 160;
    private static final float LINE_WIDTH = 1.0f;
    private static void drawSolidBox(
            PoseStack.Pose entry, VertexConsumer buffer,
            float x1, float y1, float z1,
            float x2, float y2, float z2, int rgba)
    {
        buffer.addVertex(entry, x1, y1, z1).setColor(rgba);
        buffer.addVertex(entry, x2, y1, z1).setColor(rgba);
        buffer.addVertex(entry, x2, y1, z2).setColor(rgba);
        buffer.addVertex(entry, x1, y1, z2).setColor(rgba);

        buffer.addVertex(entry, x1, y2, z1).setColor(rgba);
        buffer.addVertex(entry, x1, y2, z2).setColor(rgba);
        buffer.addVertex(entry, x2, y2, z2).setColor(rgba);
        buffer.addVertex(entry, x2, y2, z1).setColor(rgba);

        buffer.addVertex(entry, x1, y1, z1).setColor(rgba);
        buffer.addVertex(entry, x1, y2, z1).setColor(rgba);
        buffer.addVertex(entry, x2, y2, z1).setColor(rgba);
        buffer.addVertex(entry, x2, y1, z1).setColor(rgba);

        buffer.addVertex(entry, x2, y1, z1).setColor(rgba);
        buffer.addVertex(entry, x2, y2, z1).setColor(rgba);
        buffer.addVertex(entry, x2, y2, z2).setColor(rgba);
        buffer.addVertex(entry, x2, y1, z2).setColor(rgba);

        buffer.addVertex(entry, x1, y1, z2).setColor(rgba);
        buffer.addVertex(entry, x2, y1, z2).setColor(rgba);
        buffer.addVertex(entry, x2, y2, z2).setColor(rgba);
        buffer.addVertex(entry, x1, y2, z2).setColor(rgba);

        buffer.addVertex(entry, x1, y1, z1).setColor(rgba);
        buffer.addVertex(entry, x1, y1, z2).setColor(rgba);
        buffer.addVertex(entry, x1, y2, z2).setColor(rgba);
        buffer.addVertex(entry, x1, y2, z1).setColor(rgba);
    }

    private static void drawOutlinedBox(
            PoseStack.Pose entry, VertexConsumer buffer, 
            float x1, float y1, float z1, 
            float x2, float y2, float z2, int argb)
    {
        // bottom lines
        buffer.addVertex(entry, x1, y1, z1).setColor(argb).setNormal(entry, 1, 0, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y1, z1).setColor(argb).setNormal(entry, 1, 0, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y1, z1).setColor(argb).setNormal(entry, 0, 0, 1).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y1, z2).setColor(argb).setNormal(entry, 0, 0, 1).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y1, z1).setColor(argb).setNormal(entry, 0, 0, 1).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y1, z2).setColor(argb).setNormal(entry, 0, 0, 1).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y1, z2).setColor(argb).setNormal(entry, 1, 0, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y1, z2).setColor(argb).setNormal(entry, 1, 0, 0).setLineWidth(LINE_WIDTH);

        // top lines
        buffer.addVertex(entry, x1, y2, z1).setColor(argb).setNormal(entry, 1, 0, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y2, z1).setColor(argb).setNormal(entry, 1, 0, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y2, z1).setColor(argb).setNormal(entry, 0, 0, 1).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y2, z2).setColor(argb).setNormal(entry, 0, 0, 1).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y2, z1).setColor(argb).setNormal(entry, 0, 0, 1).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y2, z2).setColor(argb).setNormal(entry, 0, 0, 1).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y2, z2).setColor(argb).setNormal(entry, 1, 0, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y2, z2).setColor(argb).setNormal(entry, 1, 0, 0).setLineWidth(LINE_WIDTH);

        // side lines
        buffer.addVertex(entry, x1, y1, z1).setColor(argb).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y2, z1).setColor(argb).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y1, z1).setColor(argb).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y2, z1).setColor(argb).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y1, z2).setColor(argb).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x1, y2, z2).setColor(argb).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y1, z2).setColor(argb).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y2, z2).setColor(argb).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
    }
    
    public static void render(LevelRenderContext ctx, List<OreRenderBatch> batches, boolean fillEnabled) {
        if (batches == null || batches.isEmpty()) {
            return;
        }
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        PoseStack matrices = ctx.poseStack();

        Vec3 camPos = ctx.levelState().cameraRenderState.pos;
        Vec3 camOffset = camPos.reverse();

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        batches.forEach(batch -> {
            LongCollection collection = batch.positions();
            if (collection.isEmpty()) {
                return;
            }
            if (fillEnabled) {
                collector.submitCustomGeometry(matrices, RenderLayers.ESP_QUADS, (entry, buffer) -> {
                    for (long pos : collection) {
                        mutable.set(pos);

                        if (isOutOfViewDistance(mutable, camPos)) {
                            continue;
                        }

                        float x1 = (float) (mutable.getX() + camOffset.x);
                        float y1 = (float) (mutable.getY() + camOffset.y);
                        float z1 = (float) (mutable.getZ() + camOffset.z);
                        drawSolidBox(
                                entry, buffer,
                                x1, y1, z1,
                                x1 + 1, y1 + 1, z1 + 1,
                                batch.fillArgb());
                    }
                });
            }
            // the outline is what makes an ore readable through stone, it is never optional
            collector.submitCustomGeometry(matrices, RenderLayers.ESP_LINES, (entry, buffer) -> {
                for (long pos : collection) {
                    mutable.set(pos);

                    if (isOutOfViewDistance(mutable, camPos)) {
                        continue;
                    }

                    float x1 = (float) (mutable.getX() + camOffset.x);
                    float y1 = (float) (mutable.getY() + camOffset.y);
                    float z1 = (float) (mutable.getZ() + camOffset.z);
                    drawOutlinedBox(
                            entry, buffer,
                            x1, y1, z1,
                            x1 + 1, y1 + 1, z1 + 1,
                            batch.outlineArgb());
                }
            });
        });
    }
    
    private static boolean isOutOfViewDistance(BlockPos pos, Vec3 camPos) {
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;

        double distanceX = Math.abs(cx - camPos.x);
        double distanceZ = Math.abs(cz - camPos.z);

        return Math.max(distanceX, distanceZ) > MAX_VIEW_DISTANCE;
    }
}
