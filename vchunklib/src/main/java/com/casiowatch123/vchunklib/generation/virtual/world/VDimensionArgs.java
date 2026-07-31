package com.casiowatch123.vchunklib.generation.virtual.world;

import com.casiowatch123.vchunklib.generation.virtual.structure.VStructureTemplateManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public record VDimensionArgs(
        ResourceKey<Level> dimensionKey, 
        DimensionType dimensionType,
        BiomeSource biomeSource,
        Holder<NoiseGeneratorSettings> chunkGeneratorSettingEntry, 
        VStructureTemplateManager structureTemplateManager) {
}
