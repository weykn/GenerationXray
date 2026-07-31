package com.casiowatch123.vchunklib.generation.virtual.world.gen;

import com.casiowatch123.vchunklib.VChunkLib;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class VStructureAccessor extends StructureManager {
    private static final Logger LOGGER = VChunkLib.LOGGER;
    
    private final LevelAccessor world;
    private final WorldOptions options;
    public VStructureAccessor(LevelAccessor world, WorldOptions options) {
        super(null, null, null);
        
        this.world = world;
        this.options = options;
    }
    
    @Override
    public StructureManager forWorldGenRegion(WorldGenRegion region) {
        LOGGER.error(
                "Called unsupported method: {}#{}",
                this.getClass().getName(),
                "forWorldGenRegion");
        throw new UnsupportedOperationException();
    }
    
    @Override
    public List<StructureStart> startsForStructure(ChunkPos pos, Predicate<Structure> predicate) {
        Map<Structure, LongSet> map = this.world.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
        ImmutableList.Builder<StructureStart> builder = ImmutableList.builder();

        for (Map.Entry<Structure, LongSet> entry : map.entrySet()) {
            Structure structure = (Structure)entry.getKey();
            if (predicate.test(structure)) {
                this.fillStartsForStructure(structure, (LongSet)entry.getValue(), builder::add);
            }
        }

        return builder.build();
    }
    
    @Override
    public List<StructureStart> startsForStructure(SectionPos sectionPos, Structure structure) {
        LongSet longSet = this.world.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForStructure(structure);
        ImmutableList.Builder<StructureStart> builder = ImmutableList.builder();
        this.fillStartsForStructure(structure, longSet, builder::add);
        return builder.build();
    }
    
    @Override
    public void fillStartsForStructure(Structure structure, LongSet structureStartPositions, Consumer<StructureStart> consumer) {
        LongIterator var4 = structureStartPositions.iterator();

        while (var4.hasNext()) {
            long l = (Long)var4.next();
            SectionPos chunkSectionPos = SectionPos.of(ChunkPos.unpack(l), this.world.getMinSectionY());
            StructureStart structureStart = this.getStartForStructure(
                    chunkSectionPos, structure, this.world.getChunk(chunkSectionPos.x(), chunkSectionPos.z(), ChunkStatus.STRUCTURE_STARTS)
            );
            if (structureStart != null && structureStart.isValid()) {
                consumer.accept(structureStart);
            }
        }
    }

    @Override
    @Nullable
    public StructureStart getStartForStructure(SectionPos pos, Structure structure, StructureAccess holder) {
        return holder.getStartForStructure(structure);
    }

    @Override
    public void setStartForStructure(SectionPos pos, Structure structure, StructureStart structureStart, StructureAccess holder) {
        holder.setStartForStructure(structure, structureStart);
    }
    
    @Override
    public void addReferenceForStructure(SectionPos pos, Structure structure, long reference, StructureAccess holder) {
        holder.addReferenceForStructure(structure, reference);
    }
    
    @Override
    public boolean shouldGenerateStructures() {
        return this.options.generateStructures();
    }
    
    @Override
    public StructureStart getStructureAt(BlockPos pos, Structure structure) {
        for (StructureStart structureStart : this.startsForStructure(SectionPos.of(pos), structure)) {
            if (structureStart.getBoundingBox().isInside(pos)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }
    
    @Override
    public StructureStart getStructureWithPieceAt(BlockPos pos, TagKey<Structure> tag) {
        return this.getStructureWithPieceAt(pos, structure -> structure.is(tag));
    }
    
    @Override
    public StructureStart getStructureWithPieceAt(BlockPos pos, HolderSet<Structure> structures) {
        return this.getStructureWithPieceAt(pos, structures::contains);
    }
    
    @Override
    public StructureStart getStructureWithPieceAt(BlockPos pos, Predicate<Holder<Structure>> predicate) {
        Registry<Structure> registry = this.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (StructureStart structureStart : this.startsForStructure(
                ChunkPos.containing(pos), structure -> (Boolean)registry.get(registry.getId(structure)).map(predicate::test).orElse(false)
        )) {
            if (this.structureHasPieceAt(pos, structureStart)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }
    
    @Override
    public StructureStart getStructureWithPieceAt(BlockPos pos, Structure structure) {
        for (StructureStart structureStart : this.startsForStructure(SectionPos.of(pos), structure)) {
            if (this.structureHasPieceAt(pos, structureStart)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }
    
    @Override
    public boolean structureHasPieceAt(BlockPos pos, StructureStart structureStart) {
        for (StructurePiece structurePiece : structureStart.getPieces()) {
            if (structurePiece.getBoundingBox().isInside(pos)) {
                return true;
            }
        }

        return false;
    }
    
    @Override
    public boolean hasAnyStructureAt(BlockPos pos) {
        SectionPos chunkSectionPos = SectionPos.of(pos);
        return this.world.getChunk(chunkSectionPos.x(), chunkSectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
    }
    
    @Override
    public Map<Structure, LongSet> getAllStructuresAt(BlockPos pos) {
        SectionPos chunkSectionPos = SectionPos.of(pos);
        return this.world.getChunk(chunkSectionPos.x(), chunkSectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
    }
    
    @Override
    public StructureCheckResult checkStructurePresence(ChunkPos chunkPos, Structure structure, StructurePlacement placement, boolean skipReferencedStructures) {
        return StructureCheckResult.CHUNK_LOAD_NEEDED;
    }
    
    @Override
    public void addReference(StructureStart structureStart) {
        structureStart.addReference();
    }
    
    @Override
    public RegistryAccess registryAccess() {
        return this.world.registryAccess();
    }
}
