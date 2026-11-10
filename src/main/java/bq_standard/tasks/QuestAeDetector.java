package bq_standard.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.quest.QuestAeNetwork;
import com.gtnhadjust.quest.QuestFluidAmounts;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.sync.GuiBridge;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import baubles.api.BaublesApi;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api.utils.ItemComparison;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import cpw.mods.fml.common.Loader;
import gregtech.api.util.GTUtility;

/** Bridges BetterQuesting's package-private detector without changing its matching or party rules. */
public final class QuestAeDetector {

    private QuestAeDetector() {}

    public static final class Result {

        public final boolean updated;
        public final List<Tuple2<UUID, int[]>> progress;

        Result(TaskRetrieval.Detector detector) {
            updated = detector.updated;
            progress = detector.progress;
        }
    }

    public static Result detect(TaskRetrieval task, ParticipantInfo info) {
        TaskRetrieval.Detector detector = new TaskRetrieval.Detector(task, info.ALL_UUIDS);
        List<EntityPlayer> players = task.consume ? Collections.singletonList(info.PLAYER) : info.ACTIVE_PLAYERS;
        for (EntityPlayer player : players) {
            InventoryPlayer inventory = player.inventory;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                final int slot = i;
                detector.run(inventory.getStackInSlot(i), amount -> inventory.decrStackSize(slot, amount), info.UUID);
            }
        }
        EntityPlayerMP player = QuestAeNetwork.current();
        Access access = findAccess(player);
        if (access == null) {
            if (GTNHAdjust.debug)
                GTNHAdjust.LOG.info("Quest AE retrieval: no wireless access for {}", player.getCommandSenderName());
            QuestAeNetwork.unavailable(player);
            return new Result(detector);
        }
        try {
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                "Quest AE retrieval: player={} consume={} requirements={}",
                player.getCommandSenderName(),
                task.consume,
                task.requiredItems.size());
            if (access.host.extractAEPower(1, Actionable.MODULATE, PowerMultiplier.CONFIG) < 0.99)
                return new Result(detector);
            runNetwork(task, info, detector, access);
        } catch (RuntimeException failure) {
            GTNHAdjust.LOG.warn("Quest AE retrieval failed for {}", player.getCommandSenderName(), failure);
            player.addChatMessage(new net.minecraft.util.ChatComponentTranslation("gtnhadjust.quest.ae_error"));
        }
        return new Result(detector);
    }

    private static void runNetwork(TaskRetrieval task, ParticipantInfo info, TaskRetrieval.Detector detector,
        Access a) {
        Set<IAEItemStack> seen = new HashSet<>();
        for (BigItemStack required : task.requiredItems) {
            List<ItemStack> candidates = new ArrayList<>();
            candidates.add(required.getBaseStack());
            if (required.hasOreDict()) Collections.addAll(
                candidates,
                required.getOreIngredient()
                    .getMatchingStacks());
            for (ItemStack candidate : candidates) {
                IAEItemStack key = AEItemStack.create(candidate);
                if (key == null) continue;
                for (IAEItemStack found : new ArrayList<>(
                    a.items.getStorageList()
                        .findFuzzy(key, FuzzyMode.IGNORE_ALL))) {
                    if (found == null || found.getStackSize() <= 0 || !seen.add(found.copy())) continue;
                    IAEItemStack simulated = a.items.extractItems(
                        found.copy()
                            .setStackSize(Integer.MAX_VALUE),
                        Actionable.SIMULATE,
                        a.source);
                    if (simulated == null || simulated.getStackSize() <= 0) continue;
                    ItemStack stack = simulated.getItemStack();
                    if (stack == null) continue;
                    stack.stackSize = (int) Math.min(Integer.MAX_VALUE, simulated.getStackSize());
                    detector.run(stack, amount -> {
                        IAEItemStack extracted = a.items.extractItems(
                            found.copy()
                                .setStackSize(Math.min(amount, stack.stackSize)),
                            Actionable.MODULATE,
                            a.source);
                        ItemStack result = stack.copy();
                        result.stackSize = extracted == null ? 0
                            : (int) Math.min(Integer.MAX_VALUE, extracted.getStackSize());
                        stack.stackSize -= result.stackSize;
                        return result;
                    }, info.UUID);
                }
            }
        }
        Map<IAEFluidStack, Long> remaining = new HashMap<>();
        for (BigItemStack required : task.requiredItems) {
            ItemStack prototype = required.getBaseStack()
                .copy();
            prototype.stackSize = 1;
            FluidStack fluid = GTUtility.getFluidForFilledItem(prototype, true);
            if (fluid == null || fluid.amount <= 0) continue;
            IAEFluidStack key = AEFluidStack.create(fluid);
            if (key == null || a.fluids == null) continue;
            long available = remaining.computeIfAbsent(key, ignored -> {
                IAEFluidStack found = a.fluids.extractItems(
                    key.copy()
                        .setStackSize(Long.MAX_VALUE),
                    Actionable.SIMULATE,
                    a.source);
                return found == null ? 0L : found.getStackSize();
            });
            int units = QuestFluidAmounts.containers(available, fluid.amount);
            if (units <= 0) continue;
            int[][] before = detector.progress.stream()
                .map(
                    pair -> pair.getSecond()
                        .clone())
                .toArray(int[][]::new);
            prototype.stackSize = units;
            detector.run(prototype, amount -> {
                int wanted = Math.min(amount, prototype.stackSize);
                IAEFluidStack extracted = a.fluids.extractItems(
                    key.copy()
                        .setStackSize((long) wanted * fluid.amount),
                    Actionable.MODULATE,
                    a.source);
                long got = extracted == null ? 0 : extracted.getStackSize();
                int delivered = (int) (got / fluid.amount);
                if (got % fluid.amount != 0) {
                    IAEFluidStack refund = extracted.copy()
                        .setStackSize(got % fluid.amount);
                    IAEFluidStack notReturned = a.fluids.injectItems(refund, Actionable.MODULATE, a.source);
                    if (notReturned != null) GTNHAdjust.LOG.error(
                        "Could not return {} mB after partial quest fluid extraction",
                        notReturned.getStackSize());
                }
                ItemStack result = prototype.copy();
                result.stackSize = delivered;
                prototype.stackSize -= delivered;
                return result;
            }, info.UUID);
            long used = 0;
            for (int n = 0; n < before.length; n++) {
                long person = 0;
                int[] after = detector.progress.get(n)
                    .getSecond();
                for (int i = 0; i < after.length; i++) person += Math.max(0, after[i] - before[n][i]);
                used = Math.max(used, person);
            }
            remaining.put(key, Math.max(0, available - used * fluid.amount));
        }
    }

    /** Open an ordinary AE confirmation screen; no task progress or inventory is changed here. */
    public static void openCraft(EntityPlayerMP player, TaskRetrieval task, int index) {
        if (index < 0 || index >= task.requiredItems.size() || task.isComplete(QuestingAPI.getQuestingUUID(player)))
            return;
        Access a = findAccess(player);
        if (a == null) {
            player
                .addChatMessage(new net.minecraft.util.ChatComponentTranslation("gtnhadjust.quest.craft_unavailable"));
            return;
        }
        try {
            ISecurityGrid security = a.host.getGrid()
                .getCache(ISecurityGrid.class);
            if (security == null || !security.hasPermission(player, SecurityPermissions.CRAFT)) {
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentTranslation("gtnhadjust.quest.craft_unavailable"));
                return;
            }
            ICraftingGrid crafting = a.host.getGrid()
                .getCache(ICraftingGrid.class);
            if (crafting == null) return;
            BigItemStack required = task.requiredItems.get(index);
            int[] progress = task.getUsersProgress(QuestingAPI.getQuestingUUID(player));
            long needed = Math.max(0L, (long) required.stackSize - (task.consume ? progress[index] : 0));
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (matches(task, required, stack)) needed -= stack.stackSize;
            }
            Set<IAEItemStack> seen = new HashSet<>();
            List<ItemStack> candidates = new ArrayList<>();
            candidates.add(required.getBaseStack());
            if (required.hasOreDict()) Collections.addAll(
                candidates,
                required.getOreIngredient()
                    .getMatchingStacks());
            for (ItemStack candidate : candidates) {
                IAEItemStack key = AEItemStack.create(candidate);
                if (key == null) continue;
                for (IAEItemStack stack : a.items.getStorageList()
                    .findFuzzy(key, FuzzyMode.IGNORE_ALL)) {
                    if (stack == null || !seen.add(stack.copy()) || !matches(task, required, stack.getItemStack()))
                        continue;
                    IAEItemStack stored = a.items.extractItems(
                        stack.copy()
                            .setStackSize(Long.MAX_VALUE),
                        Actionable.SIMULATE,
                        a.source);
                    if (stored != null) needed -= stored.getStackSize();
                }
            }
            ItemStack prototype = required.getBaseStack()
                .copy();
            prototype.stackSize = 1;
            FluidStack fluid = GTUtility.getFluidForFilledItem(prototype, true);
            IAEFluidStack fluidKey = fluid == null ? null : AEFluidStack.create(fluid);
            if (fluidKey != null && fluid.amount > 0 && a.fluids != null) {
                IAEFluidStack stored = a.fluids.extractItems(
                    fluidKey.copy()
                        .setStackSize(Long.MAX_VALUE),
                    Actionable.SIMULATE,
                    a.source);
                if (stored != null) needed -= stored.getStackSize() / fluid.amount;
            }
            if (needed <= 0) {
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentTranslation("gtnhadjust.quest.craft_not_needed"));
                return;
            }
            IAEStack<?> target = null;
            for (IAEStack<?> candidate : crafting.getCraftingMultiPatterns()
                .keySet()) {
                if (candidate instanceof IAEItemStack
                    && matches(task, required, ((IAEItemStack) candidate).getItemStack())) {
                    target = candidate.copy()
                        .setStackSize(needed);
                    break;
                }
            }
            if (target == null && fluidKey != null
                && fluid.amount > 0
                && !crafting.getCraftingFor(fluidKey, null, -1, player.worldObj)
                    .isEmpty()) {
                target = fluidKey.copy()
                    .setStackSize(needed * fluid.amount);
            }
            int slot = terminalSlot(player, a.host);
            if (target == null || slot < 0) {
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentTranslation("gtnhadjust.quest.craft_no_pattern"));
                return;
            }
            java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingJob> job = crafting
                .beginCraftingJob(player.worldObj, a.host.getGrid(), a.source, target, null);
            Platform.openGUI(player, null, null, GuiBridge.GUI_CRAFTING_CONFIRM, slot);
            if (!(player.openContainer instanceof ContainerCraftConfirm)) {
                job.cancel(true);
                return;
            }
            ContainerCraftConfirm confirm = (ContainerCraftConfirm) player.openContainer;
            ItemStack terminal = slot >= Platform.baublesSlotsOffset ? BaublesApi.getBaubles(player)
                .getStackInSlot(slot - Platform.baublesSlotsOffset) : player.inventory.mainInventory[slot];
            PrimaryGui primary = new PrimaryGui(GuiBridge.GUI_ME, terminal.copy(), null, ForgeDirection.UNKNOWN);
            primary.setSlotIndex(slot);
            confirm.setPrimaryGui(primary);
            confirm.setItemToCraft(target);
            confirm.setJob(job);
        } catch (RuntimeException failure) {
            GTNHAdjust.LOG.warn("Quest AE craft request failed for {}", player.getCommandSenderName(), failure);
            player.addChatMessage(new net.minecraft.util.ChatComponentTranslation("gtnhadjust.quest.ae_error"));
        }
    }

    private static boolean matches(TaskRetrieval task, BigItemStack required, ItemStack stack) {
        return stack != null
            && (ItemComparison.StackMatch(required.getBaseStack(), stack, !task.ignoreNBT, task.partialMatch)
                || ItemComparison.OreDictionaryMatch(
                    required.getOreIngredient(),
                    required.GetTagCompound(),
                    stack,
                    !task.ignoreNBT,
                    task.partialMatch));
    }

    private static int terminalSlot(EntityPlayerMP player, WirelessTerminalGuiObject host) {
        for (int i = 0; i < player.inventory.mainInventory.length; i++)
            if (player.inventory.mainInventory[i] == host.getItemStack()) return i;
        if (Loader.isModLoaded("Baubles")) {
            IInventory inv = BaublesApi.getBaubles(player);
            if (inv != null) for (int i = 0; i < inv.getSizeInventory(); i++)
                if (inv.getStackInSlot(i) == host.getItemStack()) return Platform.baublesSlotsOffset + i;
        }
        return -1;
    }

    private static Access findAccess(EntityPlayerMP player) {
        List<ItemStack> terminals = new ArrayList<>();
        if (Loader.isModLoaded("Baubles")) {
            IInventory baubles = BaublesApi.getBaubles(player);
            if (baubles != null)
                for (int i = 0; i < baubles.getSizeInventory(); i++) terminals.add(baubles.getStackInSlot(i));
        }
        Collections.addAll(terminals, player.inventory.mainInventory);
        for (ItemStack terminal : terminals) {
            if (terminal == null) continue;
            try {
                IWirelessTermHandler handler = AEApi.instance()
                    .registries()
                    .wireless()
                    .getWirelessTerminalHandler(terminal);
                if (handler == null || !handler.hasPower(player, 1, terminal)) continue;
                WirelessTerminalGuiObject host = new WirelessTerminalGuiObject(
                    handler,
                    terminal,
                    player,
                    player.worldObj,
                    -1,
                    0,
                    0);
                if (!host.rangeCheck() || host.getGrid() == null) continue;
                ISecurityGrid security = host.getGrid()
                    .getCache(ISecurityGrid.class);
                if (security == null || !security.hasPermission(player, SecurityPermissions.EXTRACT)) continue;
                if (host.getItemInventory() == null
                    || host.extractAEPower(1, Actionable.SIMULATE, PowerMultiplier.CONFIG) < 0.99) continue;
                return new Access(host, new PlayerSource(player, host));
            } catch (RuntimeException failure) {
                if (GTNHAdjust.debug) GTNHAdjust.LOG.warn("Quest AE terminal access failed", failure);
            }
        }
        return null;
    }

    private static final class Access {

        final WirelessTerminalGuiObject host;
        final IMEMonitor<IAEItemStack> items;
        final IMEMonitor<IAEFluidStack> fluids;
        final BaseActionSource source;

        Access(WirelessTerminalGuiObject host, BaseActionSource source) {
            this.host = host;
            this.items = host.getItemInventory();
            this.fluids = host.getFluidInventory();
            this.source = source;
        }
    }
}
