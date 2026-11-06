package com.gtnhadjust.mixins.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.terminal.PatternDuplicates;
import com.gtnhadjust.terminal.Reflection;
import com.gtnhadjust.terminal.TerminalNotifications;
import com.gtnhadjust.terminal.TransferMetadata;
import com.xyp.gtneverreset.common.gui.container.ContainerQuickEncodingTerminal;
import com.xyp.gtneverreset.common.packet.InterfacePatternTarget;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;

@Mixin(targets = "com.xyp.gtneverreset.common.gui.container.ContainerQuickEncodingTerminal", remap = false)
public abstract class QuickTerminalContainerMixin {

    @Shadow
    @Final
    private ContainerInterfaceTerminal interfaceDelegate;
    @Shadow
    @Final
    private SlotRestrictedInput encodedPatternSlot;

    @Shadow
    private void placeEncodedPattern(InterfacePatternTarget target) {
        throw new AssertionError();
    }

    @Unique
    private boolean gtnhadjust$outputWasEmpty;

    @Inject(method = "applyRecipeTransfer", at = @At("HEAD"), remap = false)
    private void gtnhadjust$beforeTransfer(RecipeTransferPayload payload, CallbackInfo ci) {
        gtnhadjust$outputWasEmpty = encodedPatternSlot != null && !encodedPatternSlot.getHasStack();
    }

    @Inject(method = "applyRecipeTransfer", at = @At("RETURN"), remap = false)
    private void gtnhadjust$upload(RecipeTransferPayload payload, CallbackInfo ci) {
        if (!Platform.isServer() || !payload.shouldEncode()) return;
        if (!gtnhadjust$outputWasEmpty || encodedPatternSlot == null || !encodedPatternSlot.getHasStack()) {
            gtnhadjust$report(gtnhadjust$outputWasEmpty ? "编码失败：未生成样板，请检查空白样板、配方和能源。" : "未自动上传：编码输出槽原本有样板，请先取出后重试。");
            return;
        }
        ItemStack pattern = encodedPatternSlot.getStack();
        if (!(pattern.getItem() instanceof ICraftingPatternItem)) {
            gtnhadjust$report("未自动上传：输出槽物品不是有效样板。");
            return;
        }
        ICraftingPatternDetails details = ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(
            pattern,
            ((ContainerQuickEncodingTerminal) (Object) this).getPlayerInv().player.worldObj);
        if (details == null) {
            gtnhadjust$report("未自动上传：无法解析编码后的样板，样板已保留。");
            return;
        }
        int outputCount = 0;
        for (IAEStack<?> output : details.getAEOutputs())
            if (output != null && output.getStackSize() > 0) outputCount++;
        if (outputCount != 1) {
            gtnhadjust$report("未自动上传：样板有 " + outputCount + " 个产出槽，需要恰好一个；样板已保留。");
            return;
        }
        TransferMetadata meta = (TransferMetadata) (Object) payload;
        try {
            Map<?, ?> tracked = (Map<?, ?>) Reflection.field(interfaceDelegate, "trackedById");
            List<Long> ids = new ArrayList<>();
            for (long id : meta.gtnhadjust$getTargets()) if (tracked.containsKey(id)) ids.add(id);
            String query = meta.gtnhadjust$getSearch()
                .trim()
                .toLowerCase(Locale.ROOT);
            if (query.isEmpty() && ids.isEmpty()) {
                gtnhadjust$report("未自动上传：没有接口搜索条件，请检查 GT-Never-Reset 的 NEI 自动搜索设置。");
                return;
            }
            if (!query.isEmpty()) {
                for (Map.Entry<?, ?> entry : tracked.entrySet()) {
                    String name = String.valueOf(Reflection.field(entry.getValue(), "name"))
                        .toLowerCase(Locale.ROOT);
                    long id = ((Number) entry.getKey()).longValue();
                    if (name.contains(query) && !ids.contains(id)) ids.add(id);
                }
            }
            if (ids.isEmpty()) {
                gtnhadjust$report("未自动上传：没有找到匹配接口（" + query + "），请等待接口列表同步。");
                return;
            }
            if (((ContainerQuickEncodingTerminal) (Object) this).getPlayerInv()
                .getItemStack() != null) {
                gtnhadjust$report("未自动上传：鼠标上正持有物品，请先放下；样板已保留。");
                return;
            }
            int duplicates = 0, rejected = 0, available = 0;
            for (long id : ids) {
                Object tracker = tracked.get(id);
                if (tracker == null || !Boolean.TRUE.equals(Reflection.field(tracker, "online"))
                    || !Boolean.TRUE.equals(Reflection.field(tracker, "shouldDisplay"))) continue;
                IInventory inventory = (IInventory) Reflection.field(tracker, "patterns");
                available++;
                int slots = Math
                    .min(((Number) Reflection.field(tracker, "numSlots")).intValue(), inventory.getSizeInventory());
                for (int slot = 0; slot < slots; slot++) {
                    if (inventory.getStackInSlot(slot) != null) continue;
                    if (!inventory.isItemValidForSlot(slot, pattern)) {
                        if (PatternDuplicates.isAssemblyDuplicate(inventory, slot, pattern)) duplicates++;
                        else rejected++;
                        continue;
                    }
                    placeEncodedPattern(new InterfacePatternTarget(id, slot));
                    if (!encodedPatternSlot.getHasStack()) {
                        interfaceDelegate.scheduleUpdate();
                        gtnhadjust$report("自动上传成功：接口 " + id + "，样板槽 " + (slot + 1) + "。");
                        return;
                    }
                }
            }
            if (duplicates > 0) gtnhadjust$report("未自动上传：匹配的样板总成已有完全相同的样板；样板已保留。");
            else if (rejected > 0) gtnhadjust$report("未自动上传：接口拒绝接收样板（" + rejected + " 个槽位）；样板已保留。");
            else if (available == 0) gtnhadjust$report("未自动上传：匹配的接口离线、隐藏或已失效；样板已保留。");
            else gtnhadjust$report("未自动上传：匹配接口没有空样板槽，或写入失败；样板已保留。");
        } catch (ReflectiveOperationException | RuntimeException failure) {
            GTNHAdjust.LOG.info("Quick terminal: upload failed", failure);
            gtnhadjust$report(
                "自动上传异常：" + failure.getClass()
                    .getSimpleName() + "，详情见日志；请检查样板槽。");
        }
    }

    @Unique
    private void gtnhadjust$report(String message) {
        TerminalNotifications.report(((ContainerQuickEncodingTerminal) (Object) this).getPlayerInv().player, message);
    }
}
