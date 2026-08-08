package com.casiowatch123.vchunklib.generation.virtual.world.chunk;

import com.casiowatch123.vchunklib.VChunkLib;
import com.casiowatch123.vchunklib.generation.virtual.world.VWorld;
import com.casiowatch123.vchunklib.generation.virtual.world.VWorldContext;
import com.casiowatch123.vchunklib.generation.virtual.world.VWorldService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.WorldGenTickAccess;

public class VChunkRegion extends WorldGenRegion{
    private static final Logger LOGGER = VChunkLib.LOGGER;
    private final StaticCache2D<ChunkAccess> chunks;
    private final ChunkAccess centerChunk;
    private final VWorldService worldService;
    private final VWorld world;
    private final VWorldContext worldContext;
    private final long seed;
    private final RandomSource random;
    private final DimensionType dimension;
    private final WorldGenTickAccess<Block> blockTickScheduler = new WorldGenTickAccess<>(pos -> this.getChunk(pos).getBlockTicks());
    private final WorldGenTickAccess<Fluid> fluidTickScheduler = new WorldGenTickAccess<>(pos -> this.getChunk(pos).getFluidTicks());
    private final BiomeManager biomeAccess;
    private final VChunkGenerationStep generationStep;
    @Nullable
    private Supplier<String> currentlyGeneratingStructureName;
    private final AtomicLong tickOrder = new AtomicLong();
    private static final Identifier WORLDGEN_REGION_RANDOM_ID = Identifier.withDefaultNamespace("worldgen_region_random");

    public VChunkRegion(
            VWorldService worldService, 
            StaticCache2D<ChunkAccess> chunks, 
            VChunkGenerationStep generationStep, 
            ChunkAccess centerChunk) {
        super(null, null, null, null);
        this.worldService = worldService;
        this.world = worldService.world();
        this.worldContext = worldService.worldContext();
        this.centerChunk = centerChunk;
        this.chunks = chunks;
        this.seed = worldContext.getSeed();
        this.random = worldContext.getNoiseConfig().getOrCreateRandomFactory(WORLDGEN_REGION_RANDOM_ID).at(this.centerChunk.getPos().getWorldPosition());
        this.dimension = worldContext.getDimensionType();
        this.generationStep = generationStep;
        this.biomeAccess = new BiomeManager(this, BiomeManager.obfuscateSeed(this.seed));
    }

    @Override
    public boolean isOldChunkAround(ChunkPos chunkPos, int checkRadius) {
        return false;
    }

    @Override
    public ChunkPos getCenter() {
        return this.centerChunk.getPos();
    }

    @Override
    public void setCurrentlyGenerating(@Nullable Supplier<String> structureName) {
        this.currentlyGeneratingStructureName = structureName;
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY);
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        int i = this.centerChunk.getPos().getChessboardDistance(chunkX, chunkZ);
        ChunkStatus chunkStatus = i >= this.generationStep.directDependencies().size() ? null : this.generationStep.directDependencies().get(i);
        if (chunkStatus != null) {
            ChunkAccess chunk = this.chunks.get(chunkX, chunkZ);
            if (chunk != null) {
                return chunk;
            }
        }

        throw new RuntimeException("Request outside valid region");
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        int i = this.centerChunk.getPos().getChessboardDistance(chunkX, chunkZ);
        return i < this.generationStep.directDependencies().size();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())).getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getChunk(pos).getFluidState(pos);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(double x, double y, double z, double maxDistance, Predicate<Entity> targetPredicate) {
        return null;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeAccess;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.worldContext.getGeneratorStoredBiome(biomeX, biomeY, biomeZ);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
        BlockState blockState = this.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        } else {
            return this.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL, maxUpdateDepth);
        }
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
//        ChunkAccess chunk = this.getChunk(pos);
//        BlockEntity blockEntity = chunk.getBlockEntity(pos);
//        if (blockEntity != null) {
//            return blockEntity;
//        } else {
//            CompoundTag nbtCompound = chunk.getBlockEntityNbt(pos);
//            BlockState blockState = chunk.getBlockState(pos);
//            if (nbtCompound != null) {
//                if ("DUMMY".equals(nbtCompound.getString("id"))) {
//                    if (!blockState.hasBlockEntity()) {
//                        return null;
//                    }
//
//                    blockEntity = ((EntityBlock)blockState.getBlock()).createBlockEntity(pos, blockState);
//                } else {
//                    throw new UnsupportedOperationException();
////                    blockEntity = BlockEntity.createFromNbt(pos, blockState, nbtCompound, worldContext.getRegistryManager());
//                }
//
//                if (blockEntity != null) {
//                    chunk.setBlockEntity(blockEntity);
//                    return blockEntity;
//                }
//            }
//
//            return null;
//        }
    }

    @Override
    public boolean ensureCanWrite(BlockPos pos) {
        int i = SectionPos.blockToSectionCoord(pos.getX());
        int j = SectionPos.blockToSectionCoord(pos.getZ());
        ChunkPos chunkPos = this.getCenter();
        int k = Math.abs(chunkPos.x() - i);
        int l = Math.abs(chunkPos.z() - j);
        if (k <= this.generationStep.blockStateWriteRadius() && l <= this.generationStep.blockStateWriteRadius()) {
            if (this.centerChunk.isUpgrading()) {
                LevelHeightAccessor heightLimitView = this.centerChunk.getHeightAccessorForGeneration();
                if (heightLimitView.isOutsideBuildHeight(pos.getY())) {
                    return false;
                }
            }

            return true;
        } else {
            Util.logAndPauseIfInIde(
                    "Detected setBlock in a far chunk ["
                            + i
                            + ", "
                            + j
                            + "], pos: "
                            + pos
                            + ", status: "
                            + this.generationStep.targetStatus()
                            + (this.currentlyGeneratingStructureName == null ? "" : ", currently generating: " + (String)this.currentlyGeneratingStructureName.get())
            );
            return false;
        }
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        if (!this.ensureCanWrite(pos)) {
            return false;
        } else {
            ChunkAccess chunk = this.getChunk(pos);
            BlockState blockState = chunk.setBlockState(pos, state, flags);
            if (blockState != null) {
//                this.world.onBlockStateChanged(pos, blockState, state);
            }

            if (state.hasBlockEntity()) {
                if (chunk.getPersistedStatus().getChunkType() == ChunkType.LEVELCHUNK) {
                    BlockEntity blockEntity = ((EntityBlock)state.getBlock()).newBlockEntity(pos, state);
                    if (blockEntity != null) {
                        chunk.setBlockEntity(blockEntity);
                    } else {
                        chunk.removeBlockEntity(pos);
                    }
                } else {
                    CompoundTag nbtCompound = new CompoundTag();
                    nbtCompound.putInt("x", pos.getX());
                    nbtCompound.putInt("y", pos.getY());
                    nbtCompound.putInt("z", pos.getZ());
                    nbtCompound.putString("id", "DUMMY");
                    chunk.setBlockEntityNbt(nbtCompound);
                }
            } else if (blockState != null && blockState.hasBlockEntity()) {
                chunk.removeBlockEntity(pos);
            }

            BlockPos postProcessPos = state.getPostProcessPos(this, pos);
            if (postProcessPos != null) {
                this.markBlockForPostProcessing(postProcessPos);
            }

            return true;
        }
    }

    private void markBlockForPostProcessing(BlockPos pos) {
        this.getChunk(pos).markPosForPostProcessing(pos);
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        return false;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean move) {
        return this.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.world.getWorldBorder();
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Deprecated
    @Override
    public ServerLevel getLevel() {
        LOGGER.warn(
                "Called unsupported method: {}#{}",
                this.getClass().getName(),
                "toServerWorld");
        
        return null;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.worldContext.getRegistryManager();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.worldContext.getFeatureSet();
    }

    @Override
    public LevelData getLevelData() {
        return worldContext.getWorldProperties();
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        if (!this.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            throw new RuntimeException("We are asking a region for a chunk out of bound");
        } else {
            return new DifficultyInstance(worldContext.getWorldProperties().getDifficulty(), worldContext.getWorldProperties().getGameTime(), 0L, 0f);
        }
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChunkSource getChunkSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSeed() {
        return this.seed;
    }


    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.blockTickScheduler;
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.fluidTickScheduler;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public int getHeight(Heightmap.Types heightmap, int x, int z) {
        return this.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(heightmap, x & 15, z & 15) + 1;
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 emitterPos, GameEvent.Context emitter) {
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimension;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return state.test(this.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> state) {
        return state.test(this.getFluidState(pos));
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AABB box, Predicate<? super T> predicate) {
        return Collections.emptyList();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AABB box, @Nullable Predicate<? super Entity> predicate) {
        return Collections.emptyList();
    }

    @Override
    public List<Player> players() {
        return Collections.emptyList();
    }

    @Override
    public int getMinY() {
        return this.world.getMinY();
    }

    @Override
    public int getHeight() {
        return this.world.getHeight();
    }

    @Override
    public long nextSubTickCount() {
        return this.tickOrder.getAndIncrement();
    }
    
    
    
    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 15;
    }
    
    @Override
    public int getRawBrightness(BlockPos pos, int ambientDarkness) {
        return 15;
    }
    
    public VWorldService getWorldService() {
        return this.worldService;
    }
}
