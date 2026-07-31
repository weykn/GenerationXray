package com.casiowatch123.vchunklib.generation.virtual.world;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;



public class VWorld implements LevelHeightAccessor{
    private final WorldBorder border;
    private final DimensionType dimensionType;
    
    public VWorld(DimensionType dimensionType) {
        this.dimensionType = dimensionType;
        
        if (dimensionType.coordinateScale() != 1.0) {
            this.border = new WorldBorder() {
                @Override
                public double getCenterX() {
                    return super.getCenterX() / dimensionType.coordinateScale();
                }

                @Override
                public double getCenterZ() {
                    return super.getCenterZ() / dimensionType.coordinateScale();
                }
            };
        } else {
            this.border = new WorldBorder();
        }
    }
    
    public WorldBorder getWorldBorder() {
        return border;
    }
    
    @Override
    public int getHeight() {
        return dimensionType.height();
    }
    
    @Override
    public int getMinY() {
        return dimensionType.minY();
    }
}
