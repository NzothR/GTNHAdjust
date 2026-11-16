package com.gtnhadjust.terminal;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnhadjust.GTNHAdjust;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;

/** Measures the actual minimal construction plan without placing blocks or consulting machine tooltips. */
public final class BookmarkMachineStructure {

    /** Normalized horizontal sides and vertical extent, in blocks. */
    public static final class Size {

        private final long shortSide, longSide, height;

        Size(long shortSide, long longSide, long height) {
            this.shortSide = shortSide;
            this.longSide = longSide;
            this.height = height;
        }

        public long shortSide() {
            return shortSide;
        }

        public long longSide() {
            return longSide;
        }

        public long height() {
            return height;
        }
    }

    private static final ThreadLocal<Bounds> CURRENT = new ThreadLocal<>();
    private static final Field SPACES = field(StructureDefinition.class, "occupiedSpaces");
    private static final ClassValue<Field[]> DIMENSIONS = new ClassValue<>() {

        @Override
        protected Field[] computeValue(Class<?> type) {
            return new Field[] { field(type, "sizeA"), field(type, "sizeB"), field(type, "sizeC") };
        }
    };

    private BookmarkMachineStructure() {}

    /** Reflect only the pinned StructureLib 1.4.47 dimension metadata; cache all field lookups. */
    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    /** Use a fresh controller and a count-one construction trigger, leaving registered prototypes untouched. */
    public static Size measure(IMetaTileEntity prototype, ItemStack stack) {
        Bounds bounds = new Bounds();
        CURRENT.set(bounds);
        try {
            IMetaTileEntity probe = prototype.newMetaEntity(null);
            if (!(probe instanceof IConstructable constructable)) return null;
            ItemStack trigger = stack.copy();
            trigger.stackSize = 1;
            constructable.construct(trigger, true);
            return bounds.size();
        } catch (RuntimeException failure) {
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                "[bookmark-machines] Unknown minimum structure for {}: {}",
                stack.getDisplayName(),
                failure.toString());
            return null;
        } finally {
            CURRENT.remove();
        }
    }

    /**
     * Record only pieces actually selected by the minimum construction path, with their controller-relative offsets.
     */
    public static boolean capture(MTEEnhancedMultiBlockBase<?> machine, String piece, int x, int y, int z) {
        Bounds bounds = CURRENT.get();
        if (bounds == null) return false;
        if (!(machine.getStructureDefinition() instanceof StructureDefinition<?>definition))
            throw new IllegalArgumentException("Unsupported structure definition");
        try {
            Object space = ((Map<?, ?>) SPACES.get(definition)).get(piece);
            if (space == null) throw new IllegalArgumentException("Missing structure piece " + piece);
            Field[] dimensions = DIMENSIONS.get(space.getClass());
            bounds.add(x, y, z, dimensions[0].getInt(space), dimensions[1].getInt(space), dimensions[2].getInt(space));
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot read StructureLib dimensions", failure);
        }
        return true;
    }

    /** Union all construction pieces instead of mistaking a small extension for the whole multiblock. */
    private static final class Bounds {

        private long minX = Long.MAX_VALUE, minY = Long.MAX_VALUE, minZ = Long.MAX_VALUE;
        private long maxX = Long.MIN_VALUE, maxY = Long.MIN_VALUE, maxZ = Long.MIN_VALUE;

        void add(int x, int y, int z, int width, int height, int depth) {
            minX = Math.min(minX, -(long) x);
            minY = Math.min(minY, -(long) y);
            minZ = Math.min(minZ, -(long) z);
            maxX = Math.max(maxX, -(long) x + width);
            maxY = Math.max(maxY, -(long) y + height);
            maxZ = Math.max(maxZ, -(long) z + depth);
        }

        Size size() {
            if (minX == Long.MAX_VALUE) return null;
            long width = maxX - minX, depth = maxZ - minZ;
            return new Size(Math.min(width, depth), Math.max(width, depth), maxY - minY);
        }
    }
}
