package com.casiowatch123.vchunklib.generation.virtual.world.chunk;

import com.casiowatch123.vchunklib.generation.virtual.world.VWorldService;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public record VChunkGenerationContext(
        VWorldService worldService,
        ChunkGenerator generator,
        StructureTemplateManager structureTemplateManager
) {
}
