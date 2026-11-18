package com.gtnhadjust.structurelib;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.science.gtnl.common.machine.multiblock.GenerationEarthEngine;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Clips the upstream [B][C][A] layout before compiling any projector geometry. */
public final class PlanetaryEngineStructure {

    public static final String RENDER_CHANNEL = "structure_render";
    public static final String SECTION_CHANNEL = "structure_section";
    private static final String PIECE = "main";
    private static final int OFFSET_A = 321, OFFSET_B = 321, OFFSET_C = 17;
    private static final int SECTION_SHIFT = 2;

    private final String[][] shape;
    private final Map<Character, IStructureElement<GenerationEarthEngine>> palette = new HashMap<>();
    private Section lastSection;

    public PlanetaryEngineStructure(String[][] shape, StructureDefinition<GenerationEarthEngine> palette) {
        this.shape = shape;
        palette.getElements()
            .forEach((symbol, element) -> {
                // StructureLib reserves this range for shape navigation.
                if (symbol < '\uA000' && symbol != '-' && symbol != '+') this.palette.put(symbol, element);
            });
    }

    public interface Provider {

        PlanetaryEngineStructure gtnhadjust$getProjectorStructure();
    }

    /** Section numbering is X first, then Z, then four-block Y bands in world coordinates. */
    Section select(GenerationEarthEngine engine, ItemStack trigger, EntityPlayer player) {
        IGregTechTileEntity tile = engine.getBaseMetaTileEntity();
        ExtendedFacing facing = engine.getExtendedFacing();
        int x = tile.getXCoord(), y = tile.getYCoord(), z = tile.getZCoord();
        int[] first = new int[3], last = new int[3];
        facing.getWorldOffset(new int[] { -OFFSET_A, -OFFSET_B, -OFFSET_C }, first);
        facing.getWorldOffset(
            new int[] { shape[0][0].length() - 1 - OFFSET_A, shape.length - 1 - OFFSET_B,
                shape[0].length - 1 - OFFSET_C },
            last);
        int minX = (x + Math.min(first[0], last[0])) >> 4;
        int maxX = (x + Math.max(first[0], last[0])) >> 4;
        int minZ = (z + Math.min(first[2], last[2])) >> 4;
        int maxZ = (z + Math.max(first[2], last[2])) >> 4;
        int minY = Math.max(0, y + Math.min(first[1], last[1]));
        int maxY = Math.min(
            tile.getWorld()
                .getHeight() - 1,
            y + Math.max(first[1], last[1]));
        int lowBand = minY >> SECTION_SHIFT, highBand = maxY >> SECTION_SHIFT;
        int width = maxX - minX + 1, depth = maxZ - minZ + 1;
        int plane = width * depth;
        int total = plane * (highBand - lowBand + 1);
        int section;
        if (ChannelDataAccessor.hasSubChannel(trigger, SECTION_CHANNEL)) {
            section = ChannelDataAccessor.getChannelData(trigger, SECTION_CHANNEL);
            if (section < 1 || section > total) {
                PlanetaryEngineProjector.message(player, "range", total);
                return null;
            }
        } else {
            section = 1 + ((y >> SECTION_SHIFT) - lowBand) * plane + ((z >> 4) - minZ) * width + (x >> 4) - minX;
        }
        if (lastSection != null && lastSection.number == section
            && lastSection.x == x
            && lastSection.y == y
            && lastSection.z == z
            && lastSection.facing == facing) return lastSection;
        int index = section - 1;
        int chunkX = minX + index % width;
        int chunkZ = minZ + index % plane / width;
        int bottom = (lowBand + index / plane) << SECTION_SHIFT;
        lastSection = new Section(
            facing,
            x,
            y,
            z,
            chunkX,
            chunkZ,
            Math.max(minY, bottom),
            Math.min(maxY, bottom + (1 << SECTION_SHIFT) - 1),
            section,
            total);
        return lastSection;
    }

    /** A world chunk sliced vertically; native walkers still receive the full piece's containment semantics. */
    final class Section implements IStructureDefinition<GenerationEarthEngine> {

        final ExtendedFacing facing;
        final int x, y, z, chunkX, chunkZ, bottom, top, number, total;
        private final int a, b, c, sizeA, sizeB, sizeC;
        private final IStructureElement<GenerationEarthEngine>[] elements;

        Section(ExtendedFacing facing, int x, int y, int z, int chunkX, int chunkZ, int bottom, int top, int number,
            int total) {
            this.facing = facing;
            this.x = x;
            this.y = y;
            this.z = z;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.bottom = bottom;
            this.top = top;
            this.number = number;
            this.total = total;
            int[] first = new int[3], last = new int[3];
            facing.getOffsetABC(new int[] { (chunkX << 4) - x, bottom - y, (chunkZ << 4) - z }, first);
            facing.getOffsetABC(new int[] { (chunkX << 4) + 15 - x, top - y, (chunkZ << 4) + 15 - z }, last);
            a = Math.max(0, OFFSET_A + Math.min(first[0], last[0]));
            b = Math.max(0, OFFSET_B + Math.min(first[1], last[1]));
            c = Math.max(0, OFFSET_C + Math.min(first[2], last[2]));
            int endA = Math.min(shape[0][0].length(), 1 + OFFSET_A + Math.max(first[0], last[0]));
            int endB = Math.min(shape.length, 1 + OFFSET_B + Math.max(first[1], last[1]));
            int endC = Math.min(shape[0].length, 1 + OFFSET_C + Math.max(first[2], last[2]));
            sizeA = endA - a;
            sizeB = endB - b;
            sizeC = endC - c;
            String[][] clipped = new String[sizeC][sizeB];
            for (int slice = 0; slice < sizeC; slice++) {
                for (int row = 0; row < sizeB; row++) {
                    clipped[slice][row] = shape[b + row][c + slice].substring(a, endA);
                }
            }
            StructureDefinition.Builder<GenerationEarthEngine> builder = StructureDefinition.builder();
            builder.addShape(PIECE, clipped);
            palette.forEach((symbol, element) -> builder.addElement(symbol.charValue(), element));
            elements = builder.build()
                .getStructureFor(PIECE);
        }

        @Override
        public IStructureElement<GenerationEarthEngine>[] getStructureFor(String name) {
            return elements;
        }

        @Override
        public boolean isContainedInStructure(String name, int offsetA, int offsetB, int offsetC) {
            int fullA = a + offsetA, fullB = b + offsetB, fullC = c + offsetC;
            return fullA >= 0 && fullA < shape[0][0].length()
                && fullB >= 0
                && fullB < shape.length
                && fullC >= 0
                && fullC < shape[0].length
                && shape[fullB][fullC].charAt(fullA) != ' ';
        }

        void project(GenerationEarthEngine engine, ItemStack trigger, World world) {
            hints(engine, trigger, PIECE, world, facing, x, y, z, OFFSET_A - a, OFFSET_B - b, OFFSET_C - c);
        }

        int build(GenerationEarthEngine engine, ItemStack trigger, World world, int budget,
            ISurvivalBuildEnvironment environment) {
            return survivalBuild(
                engine,
                trigger,
                PIECE,
                world,
                facing,
                x,
                y,
                z,
                OFFSET_A - a,
                OFFSET_B - b,
                OFFSET_C - c,
                budget,
                environment,
                false);
        }

        int volume() {
            return sizeA * sizeB * sizeC;
        }

        /** Creative placement visits a bounded number of cells and leaves occupied blocks for the player. */
        int buildCreative(GenerationEarthEngine engine, ItemStack trigger, World world, EntityPlayer player, int start,
            int end) {
            int[] abc = new int[3], xyz = new int[3];
            int built = 0;
            for (int index = start; index < end; index++) {
                int localA = a + index % sizeA;
                int localB = b + index / sizeA % sizeB;
                int localC = c + index / (sizeA * sizeB);
                IStructureElement<GenerationEarthEngine> element = palette.get(shape[localB][localC].charAt(localA));
                if (element == null) continue;
                abc[0] = localA - OFFSET_A;
                abc[1] = localB - OFFSET_B;
                abc[2] = localC - OFFSET_C;
                facing.getWorldOffset(abc, xyz);
                int blockX = x + xyz[0], blockY = y + xyz[1], blockZ = z + xyz[2];
                if (!world.getBlock(blockX, blockY, blockZ)
                    .isReplaceable(world, blockX, blockY, blockZ) || !world.canMineBlock(player, blockX, blockY, blockZ)
                    || !player.canPlayerEdit(blockX, blockY, blockZ, 1, trigger)) continue;
                if (element.placeBlock(engine, world, blockX, blockY, blockZ, trigger)) built++;
            }
            return built;
        }
    }
}
