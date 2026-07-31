package com.casiowatch123.vchunklib.generation.virtual.world;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelData;


public class VWorldContext {
    private final boolean generateStructures;
    private final long seed;
    private final RegistryAccess registryManager;
    private final WorldOptions generatorOptions;
    private final DimensionType dimensionType;
    private final NoiseBasedChunkGenerator generator;
    private final BiomeSource biomeSource;
    private final RandomState noiseConfig;
    private final FeatureFlagSet featureSet = FeatureFlags.VANILLA_SET;
    private final LevelData worldProperties;
    private final ChunkGeneratorStructureState structurePlacementCalculator;
    private final ResourceKey<Level> dimensionKey;
    
    
    public VWorldContext(RegistryAccess registryManager, VDimensionArgs dimensionArgs, long seed) {
        this.generateStructures = true;
        this.seed = seed;
        this.registryManager = registryManager;
        this.generatorOptions = new WorldOptions(seed, generateStructures, false);
        this.dimensionType = dimensionArgs.dimensionType();
        this.biomeSource = dimensionArgs.biomeSource();
        this.dimensionKey = dimensionArgs.dimensionKey();
        
        this.generator = new NoiseBasedChunkGenerator(biomeSource, dimensionArgs.chunkGeneratorSettingEntry());
        this.noiseConfig = RandomState.create(
                generator.generatorSettings().value(),
                registryManager.lookupOrThrow(Registries.NOISE),
                seed
        );
        this.worldProperties = new LevelData() {
            private final RespawnData respawnData = new RespawnData(GlobalPos.of(
                    dimensionKey, new BlockPos(0, 63, 0)),
                    0, 0);
            @Override
            public RespawnData getRespawnData() {
                return respawnData;
            }

            @Override
            public long getGameTime() {
                return 0;
            }

            @Override
            public boolean isHardcore() {
                return false;
            }

            @Override
            public Difficulty getDifficulty() {
                return Difficulty.PEACEFUL;
            }

            @Override
            public boolean isDifficultyLocked() {
                return true;
            }
        };
        this.structurePlacementCalculator =
                ChunkGeneratorStructureState.createForNormal(
                        noiseConfig,
                        seed,
                        biomeSource,
                        registryManager.lookupOrThrow(Registries.STRUCTURE_SET)
                );
    }

    public boolean shouldGenerateStructures() {
        return generateStructures;
    }
    
    public RegistryAccess getRegistryManager() {
        return registryManager;
    }

    public ChunkGeneratorStructureState getStructurePlacementCalculator() {
        return this.structurePlacementCalculator;
    }

    public WorldOptions getGeneratorOptions() {
        return generatorOptions;
    }
    
    public DimensionType getDimensionType() {
        return dimensionType;
    }
    
    public long getSeed() {
        return seed;
    }
    
    public RandomState getNoiseConfig() {
        return noiseConfig;
    }

    public Holder<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
        return this.biomeSource
                .getNoiseBiome(biomeX, biomeY, biomeZ, this.noiseConfig.sampler());
    }
    
    public FeatureFlagSet getFeatureSet() {
        return featureSet;
    }
    
    public LevelData getWorldProperties() {
        return worldProperties;
    }
    
    public ChunkGenerator getGenerator() {
        return this.generator;
    }
    
    public ResourceKey<Level> getDimensionKey() {
        return this.dimensionKey;
    }
}
