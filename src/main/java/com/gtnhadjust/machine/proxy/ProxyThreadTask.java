// Derived from GT-Never-Reset 0.1.34 RecipeThreadTask.
package com.gtnhadjust.machine.proxy;

import net.minecraft.nbt.NBTTagCompound;

public final class ProxyThreadTask {

    private final int thread;
    private final long eut;
    private final int duration;
    private final int parallels;
    private final ProxyThreadOutputs outputs;
    private int progress;

    public ProxyThreadTask(int thread, long eut, int duration, int parallels, ProxyThreadOutputs outputs) {
        if (thread < 0 || eut < 0 || duration < 1 || parallels < 1 || outputs == null) {
            throw new IllegalArgumentException("Invalid recipe thread task");
        }
        this.thread = thread;
        this.eut = eut;
        this.duration = duration;
        this.parallels = parallels;
        this.outputs = outputs;
    }

    public int getThread() {
        return thread;
    }

    public long getEUt() {
        return eut;
    }

    public int getDuration() {
        return duration;
    }

    public int getParallels() {
        return parallels;
    }

    public ProxyThreadOutputs getOutputs() {
        return outputs;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isFinished() {
        return progress >= duration;
    }

    public void advance() {
        advance(1);
    }

    public void advance(int ticks) {
        progress = ProxyThreadProgress.advance(progress, duration, ticks);
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("thread", thread);
        tag.setLong("eut", eut);
        tag.setInteger("duration", duration);
        tag.setInteger("progress", progress);
        tag.setInteger("parallels", parallels);
        tag.setTag("outputs", outputs.save());
        return tag;
    }

    public static ProxyThreadTask load(NBTTagCompound tag) {
        ProxyThreadTask task = new ProxyThreadTask(
            tag.getInteger("thread"),
            tag.getLong("eut"),
            tag.getInteger("duration"),
            tag.getInteger("parallels"),
            ProxyThreadOutputs.load(tag.getCompoundTag("outputs")));
        task.progress = Math.max(0, Math.min(task.duration, tag.getInteger("progress")));
        return task;
    }
}
