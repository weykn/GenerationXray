package com.casiowatch123.vchunklib.generation.virtual.structure;

import com.casiowatch123.vchunklib.VChunkLib;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplatePathFactory;

public class VStructureTemplateManager extends StructureTemplateManager {
    private static final Logger LOGGER = VChunkLib.LOGGER;
    private static final Map<Identifier, CompoundTag> NBT_COMPOUND_MAP = new ConcurrentHashMap<>();
        
    private final Map<Identifier, StructureTemplate> templates = new HashMap<>();
    
    static {
        ModContainer container = FabricLoader.getInstance().getModContainer(VChunkLib.MOD_ID).orElseThrow();

        for(var resourceRoot : container.getRootPaths()) {
            Path structureResourceRoot = resourceRoot
                    .resolve("data")
                    .resolve("vchunklib")
                    .resolve("structure");

            if (!Files.exists(structureResourceRoot) || !Files.isDirectory(structureResourceRoot)) {
                continue;
            }

            try (Stream<Path> stream = Files.walk(structureResourceRoot)) {
                stream.forEach(path -> {
                    if (!Files.isDirectory(path)) {
                        String identifierString =
                                StreamSupport.stream(structureResourceRoot.relativize(path).spliterator(), false)
                                        .map(Path::toString)
                                        .collect(Collectors.joining("/"))
                                        .replace(".nbt", "");
                        try {
                            CompoundTag nbt = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                            NBT_COMPOUND_MAP.put(Identifier.parse(identifierString), nbt);
                        } catch (IOException ignored) {
                            LOGGER.error("Structure template file({}) loading failed: {}#static",
                                    structureResourceRoot.relativize(path), 
                                    VStructureTemplateManager.class.getName());
                        }
                    }
                });
            } catch (IOException ignored) {
                LOGGER.error("Structure template files(.nbt) loading failed: {}#static", VStructureTemplateManager.class.getName());
            }
        }
    }
    
    public VStructureTemplateManager(HolderGetter<Block> blockEntryLookup) {
        super(null, null, null, null);
        
        NBT_COMPOUND_MAP.forEach((identifier, nbtCompound) -> {
            StructureTemplate structureTemplate = new StructureTemplate();
            int version = NbtUtils.getDataVersion(nbtCompound, 500);
            
            structureTemplate.load(blockEntryLookup, nbtCompound);
            templates.put(identifier, structureTemplate);
        });
    }
    
    @Override
    public StructureTemplate getOrCreate(Identifier id) {
        Optional<StructureTemplate> optional = this.get(id);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            StructureTemplate structureTemplate = new StructureTemplate();
            this.templates.put(id, structureTemplate);
            return structureTemplate;
        }
    }

    @Override
    public Optional<StructureTemplate> get(Identifier id) {
        return templates.containsKey(id) ? Optional.of(templates.get(id)) : Optional.empty();
    }

    @Override
    public Stream<Identifier> listTemplates() {
        return templates.keySet().stream();
    }

    @Override
    public boolean save(Identifier id) {
        LOGGER.warn(
                "Called unsupported method: {}#{}",
                this.getClass().getName(),
                "save");
        return false;
    }

    @Override
    public void remove(Identifier id) {
        LOGGER.warn(
                "Called unsupported method: {}#{}",
                this.getClass().getName(),
                "remove");
    }

    @Override
    public TemplatePathFactory worldTemplates() {
        LOGGER.error(
                "Called unsupported method: {}#{}",
                this.getClass().getName(),
                "worldTemplates");
        throw new UnsupportedOperationException();
    }

    @Override
    public TemplatePathFactory testTemplates() {
        LOGGER.error(
                "Called unsupported method: {}#{}",
                this.getClass().getName(),
                "testTemplates");
        throw new UnsupportedOperationException();
    }

    @Override
    public void onResourceManagerReload(ResourceManager rm) {
        LOGGER.warn(
                "Called unsupported method: {}#{}",
                this.getClass().getName(),
                "onResourceManagerReload");
    }
}
