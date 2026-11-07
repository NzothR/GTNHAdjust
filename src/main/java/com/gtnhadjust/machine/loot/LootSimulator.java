package com.gtnhadjust.machine.loot;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofCoil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnhadjust.GTNHAdjust;
import com.kuba6000.mobsinfo.api.IChanceModifier;
import com.kuba6000.mobsinfo.api.MobDrop;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.GTStructureChannels;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;
import kubatech.config.Config;
import kubatech.loaders.MobHandlerLoader;

/**
 * Simulates the EEC's normal, zero-looting drop records without spawning mobs.
 * Drop chance handling follows KubaTech's MobHandlerLoader (Copyright 2022-2024 kuba6000, LGPL-3.0-or-later).
 */
public final class LootSimulator extends MTEExtendedPowerMultiBlockBase<LootSimulator>
    implements ISurvivalConstructable {

    private static final String PIECE = "main";
    private static final GameProfile SIMULATION_PLAYER = new GameProfile(
        UUID.fromString("d6691f16-59d3-35ad-a5ef-0e8d50c7aa3a"),
        "[Loot Simulator]");
    private static final String[][] SHAPE = { { "CCC", "CCC", "CCC" }, { "C~C", "CHC", "CCC" },
        { "CCC", "CCC", "CCC" } };
    private IStructureDefinition<LootSimulator> structure;
    private HeatingCoilLevel coil = HeatingCoilLevel.None;
    private final Random random = new Random();

    public LootSimulator(int id, String name, String display) {
        super(id, name, display);
    }

    private LootSimulator(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LootSimulator(mName);
    }

    @Override
    public IStructureDefinition<LootSimulator> getStructureDefinition() {
        if (structure == null) structure = StructureDefinition.<LootSimulator>builder()
            .addShape(PIECE, transpose(SHAPE))
            .addElement(
                'C',
                buildHatchAdder(LootSimulator.class).atLeast(InputBus, OutputBus, OutputHatch, Maintenance)
                    .casingIndex(Casings.CleanStainlessSteelMachineCasing.getTextureId())
                    .hint(1)
                    .buildAndChain(ofBlock(GregTechAPI.sBlockCasings4, 1)))
            .addElement(
                'H',
                GTStructureChannels.HEATING_COIL.use(
                    ofCoil(
                        (LootSimulator machine, HeatingCoilLevel level) -> machine.coil = level,
                        machine -> machine.coil)))
            .build();
        return structure;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece(PIECE, stack, 1, 1, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        coil = HeatingCoilLevel.None;
        if (!checkPiece(PIECE, 1, 1, 0, errors)) return;
        checkHatchMin(errors, InputBus, 1);
        checkHatchMin(errors, OutputBus, 1);
        checkHatchMin(errors, Maintenance, 1);
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return 0;
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (!(tile.getWorld() instanceof WorldServer world) || coil.getTier() < 0)
            return CheckRecipeResultRegistry.NO_RECIPE;
        int multiplier = 1 << coil.getTier();
        EntityPlayer player = FakePlayerFactory.get(world, SIMULATION_PLAYER);
        List<ItemStack> outputs = new ArrayList<>();
        Map<String, Integer> counted = new HashMap<>();
        long xp = 0;
        long targets = 0;
        for (ItemStack input : getStoredInputs()) {
            if (input == null || input.stackSize <= 0) continue;
            String type = mobType(input);
            if (type == null || type.isEmpty()) continue;
            MobHandlerLoader.MobEECRecipe recipe = MobHandlerLoader.recipeMap.get(type);
            Class<?> entityClass = EntityList.stringToClassMapping.get(type);
            if (recipe == null && (entityClass == null || !EntityLivingBase.class.isAssignableFrom(entityClass)))
                continue;
            int count = LootSimulatorRules.acceptedCount(counted.getOrDefault(type, 0), input.stackSize);
            if (count == 0) continue;
            counted.merge(type, count, Integer::sum);
            targets += count;
            xp += LootSimulatorRules.experience(count, coil.getTier());
            if (recipe == null) {
                if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Loot simulator: no EEC data for {}", type);
                continue;
            }
            recipe.entityCopy.setPosition(tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
            boolean hasModifiers = false;
            for (MobDrop drop : recipe.mOutputs) {
                if (drop != null && !drop.chanceModifiers.isEmpty()) {
                    hasModifiers = true;
                    break;
                }
            }
            if (!hasModifiers) {
                for (MobDrop drop : recipe.mOutputs) {
                    if (drop == null || drop.stack == null
                        || drop.enchantable != null
                        || drop.damages != null
                        || isEquipment(drop.stack)) continue;
                    int chance = drop.chance;
                    if (drop.playerOnly && chance != 0)
                        chance = Math.max(1, (int) (chance * Config.MobHandler.playerOnlyDropsModifier));
                    int hits = LootSimulatorRules.rolls(random, count, chance);
                    addAmount(outputs, drop.stack, (long) hits * drop.stack.stackSize * multiplier);
                }
                continue;
            }
            for (int i = 0; i < count; i++) {
                List<ItemStack> kill = new ArrayList<>();
                for (MobDrop drop : recipe.mOutputs) {
                    if (drop == null || drop.stack == null
                        || drop.enchantable != null
                        || drop.damages != null
                        || isEquipment(drop.stack)) continue;
                    double probability = drop.chance / 100.0;
                    for (IChanceModifier modifier : drop.chanceModifiers)
                        probability = modifier.apply(probability, world, kill, player, recipe.entityCopy);
                    int chance = (int) (probability * 100.0);
                    if (drop.playerOnly && chance != 0)
                        chance = Math.max(1, (int) (chance * Config.MobHandler.playerOnlyDropsModifier));
                    if (chance > 0 && (chance == 10000 || random.nextInt(10000) < chance)) kill.add(drop.stack.copy());
                }
                for (ItemStack drop : kill)
                    if (drop != null && !isEquipment(drop)) addScaled(outputs, drop, multiplier);
            }
        }
        if (targets == 0) return CheckRecipeResultRegistry.NO_RECIPE;
        mOutputItems = outputs.toArray(new ItemStack[0]);
        List<FluidStack> fluids = new ArrayList<>();
        if (!mOutputHatches.isEmpty()) while (xp > 0) {
            int amount = (int) Math.min(Integer.MAX_VALUE, xp);
            FluidStack fluid = FluidRegistry.getFluidStack("xpjuice", amount);
            if (fluid == null) break;
            fluids.add(fluid);
            xp -= amount;
        }
        mOutputFluids = fluids.toArray(new FluidStack[0]);
        mMaxProgresstime = 128;
        mEfficiency = 10000;
        mEfficiencyIncrease = 0;
        lEUt = 0;
        if (GTNHAdjust.debug)
            GTNHAdjust.LOG.info("Loot simulator: {} targets, coil multiplier {}", targets, multiplier);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private static String mobType(ItemStack stack) {
        if (stack.getItem() == Items.spawn_egg) return EntityList.getStringFromID(stack.getItemDamage());
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        if (id == null || !"EnderIO".equals(id.modId)) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) return null;
        if ("itemSoulVessel".equals(id.name)) return tag.getString("id");
        if ("itemBrokenSpawner".equals(id.name) || "blockPoweredSpawner".equals(id.name))
            return tag.getString("mobType");
        return null;
    }

    private static boolean isEquipment(ItemStack stack) {
        return stack.isItemStackDamageable() || stack.getItem() instanceof ItemArmor
            || stack.getItem() instanceof ItemSword
            || stack.getItem() instanceof ItemTool;
    }

    private static void addScaled(List<ItemStack> outputs, ItemStack input, int multiplier) {
        addAmount(outputs, input, (long) input.stackSize * multiplier);
    }

    private static void addAmount(List<ItemStack> outputs, ItemStack input, long amount) {
        long remaining = amount;
        if (remaining <= 0) return;
        for (ItemStack existing : outputs) {
            if (!existing.isItemEqual(input) || !ItemStack.areItemStackTagsEqual(existing, input)) continue;
            int add = (int) Math.min(remaining, (long) Integer.MAX_VALUE - existing.stackSize);
            existing.stackSize += add;
            remaining -= add;
            if (remaining == 0) return;
        }
        while (remaining > 0) {
            ItemStack copy = input.copy();
            copy.stackSize = (int) Math.min(Integer.MAX_VALUE, remaining);
            outputs.add(copy);
            remaining -= copy.stackSize;
        }
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Casings.CleanStainlessSteelMachineCasing.getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? TexturesGtBlock.oMCAQFTActive : TexturesGtBlock.oMCAQFT)
            .build() };
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("战利品模拟室")
            .addInfo("每 6.4 秒模拟输入的生物，无需电力，不消耗输入")
            .addInfo("白铜线圈 1 倍，每升一级战利品和经验翻倍")
            .addInfo("过滤装备，不使用时运，多余产物销毁")
            .beginStructureBlock(3, 3, 3, false)
            .addController("正面中央")
            .addCasing("25", "洁净不锈钢机械方块，可替换为舱室", false)
            .addCasing("1", "中心加热线圈", true)
            .addInputBus("1+", "外壳", 1)
            .addOutputBus("1+", "外壳", 1)
            .addOutputHatch("可选", "外壳", 1)
            .addMaintenanceHatch("1+", "外壳", 1)
            .addSubChannel(GTStructureChannels.HEATING_COIL)
            .toolTipFinisher();
    }
}
