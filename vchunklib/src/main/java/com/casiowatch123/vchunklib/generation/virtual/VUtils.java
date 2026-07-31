package com.casiowatch123.vchunklib.generation.virtual;

import com.casiowatch123.vchunklib.generation.virtual.structure.VStructureTemplateManager;
import com.casiowatch123.vchunklib.generation.virtual.world.VDimensionArgs;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;

public final class VUtils {
    public static VStructureTemplateManager STRUCTURE_TEMPLATE_MANAGER = new VStructureTemplateManager(BuiltInRegistries.BLOCK);
    
    private VUtils() {}
    
    //Must be used after client starting!!
    public static RegistryAccess.Frozen createVanillaRegistryManager() {
        PackRepository rpm = ServerPacksSource.createVanillaTrustedRepository();
        rpm.reload();

        rpm.setSelected(Set.of("vanilla"));

        WorldLoader.PackConfig dataPacks = new WorldLoader.PackConfig(
                rpm,
                WorldDataConfiguration.DEFAULT,
                true,
                true
        );

        WorldLoader.InitConfig serverConfig = new WorldLoader.InitConfig(
                dataPacks,
                Commands.CommandSelection.DEDICATED,
                LevelBasedPermissionSet.forLevel(PermissionLevel.ALL)
        );
        
        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            return WorldLoader.load(
                            serverConfig,
                            loadContext -> new WorldLoader.DataLoadOutput<>(null, loadContext.datapackDimensions()),
                            (rm, dp, combdRegistries, loadContext) ->
                                    combdRegistries,
                            executor,
                            executor)
                    .join()
                    .compositeAccess();
        }
    }
    
    public static VDimensionArgs createDimensionArg(ResourceKey<Level> worldKey, RegistryAccess drm) {
        if (worldKey == Level.OVERWORLD) {
            return new VDimensionArgs(
                    Level.OVERWORLD,
                    drm.lookupOrThrow(Registries.DIMENSION_TYPE)
                            .getValue(BuiltinDimensionTypes.OVERWORLD),
                    MultiNoiseBiomeSource.createFromPreset(
                            drm.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                                    .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)),
                    drm.lookupOrThrow(Registries.NOISE_SETTINGS)
                            .getOrThrow(NoiseGeneratorSettings.OVERWORLD),
                    STRUCTURE_TEMPLATE_MANAGER
            );
        } else if (worldKey == Level.NETHER) {
            return new VDimensionArgs(
                    Level.NETHER,
                    drm.lookupOrThrow(Registries.DIMENSION_TYPE)
                            .getValue(BuiltinDimensionTypes.NETHER),
                    MultiNoiseBiomeSource.createFromPreset(
                            drm.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                                    .getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER)),
                    drm.lookupOrThrow(Registries.NOISE_SETTINGS)
                            .getOrThrow(NoiseGeneratorSettings.NETHER),
                    STRUCTURE_TEMPLATE_MANAGER
            );
        } else {
            return new VDimensionArgs(
                    Level.END,
                    drm.lookupOrThrow(Registries.DIMENSION_TYPE)
                            .getValue(BuiltinDimensionTypes.END),
                    TheEndBiomeSource.create(
                            drm.lookupOrThrow(Registries.BIOME)),
                    drm.lookupOrThrow(Registries.NOISE_SETTINGS)
                            .getOrThrow(NoiseGeneratorSettings.END),
                    STRUCTURE_TEMPLATE_MANAGER
            );
        }
    }
}
