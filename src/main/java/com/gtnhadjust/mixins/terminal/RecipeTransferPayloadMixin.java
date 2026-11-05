package com.gtnhadjust.mixins.terminal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.terminal.TransferMetadata;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

@Mixin(value = RecipeTransferPayload.class, remap = false)
public abstract class RecipeTransferPayloadMixin implements TransferMetadata {

    @Unique
    private long[] gtnhadjust$targets = new long[0];
    @Unique
    private String gtnhadjust$search = "";

    @Override
    public void gtnhadjust$setTargets(long[] ids, String search) {
        gtnhadjust$targets = ids == null ? new long[0] : ids.clone();
        gtnhadjust$search = search == null ? "" : search;
    }

    @Override
    public long[] gtnhadjust$getTargets() {
        return gtnhadjust$targets.clone();
    }

    @Override
    public String gtnhadjust$getSearch() {
        return gtnhadjust$search;
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private static void gtnhadjust$write(ByteBuf buffer, RecipeTransferPayload payload, CallbackInfo ci) {
        TransferMetadata metadata = (TransferMetadata) (Object) payload;
        long[] ids = metadata.gtnhadjust$getTargets();
        int count = Math.min(ids.length, 1024);
        buffer.writeShort(count);
        for (int i = 0; i < count; i++) buffer.writeLong(ids[i]);
        String search = metadata.gtnhadjust$getSearch();
        ByteBufUtils.writeUTF8String(buffer, search.length() > 256 ? search.substring(0, 256) : search);
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private static void gtnhadjust$read(ByteBuf buffer, CallbackInfoReturnable<RecipeTransferPayload> cir) {
        int count = buffer.readUnsignedShort();
        if (count > 1024) throw new IllegalArgumentException("Too many terminal interface targets");
        long[] ids = new long[count];
        for (int i = 0; i < count; i++) ids[i] = buffer.readLong();
        String search = ByteBufUtils.readUTF8String(buffer);
        if (search.length() > 256) throw new IllegalArgumentException("Terminal search too long");
        ((TransferMetadata) (Object) cir.getReturnValue()).gtnhadjust$setTargets(ids, search);
    }
}
