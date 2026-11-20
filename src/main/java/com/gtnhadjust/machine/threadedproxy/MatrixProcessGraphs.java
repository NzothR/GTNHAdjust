package com.gtnhadjust.machine.threadedproxy;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.mixins.gtnr.ThreadedProxyOutputAccessor;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;

import gregtech.api.util.GTUtility;

/** Persistent process graphs. Each edge owns its material and its inventory. */
final class MatrixProcessGraphs {

    final Map<Integer, Graph> graphs = new LinkedHashMap<>();
    final List<RecipeThreadOutputs> exports = new ArrayList<>();
    int nextId = 1;
    int parallelMultiplier = 1;
    int overclockMultiplier = 1;
    int nextGraph;

    static final class Graph {

        final int id;
        final Map<Integer, Node> nodes = new LinkedHashMap<>();
        final Map<Integer, Edge> edges = new LinkedHashMap<>();
        final HashSet<Integer> targetChain = new HashSet<>();

        /** Classify links and nodes by whether their downstream can reach the target. */
        void updateTargetChain() {
            targetChain.clear();
            if (!nodes.containsKey(target)) return;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(target);
            while (!queue.isEmpty()) {
                int node = queue.removeFirst();
                if (targetChain.add(node))
                    for (Edge edge : edges.values()) if (edge.destination == node) queue.addLast(edge.source);
            }
            for (Edge edge : edges.values()) edge.feedsTarget = targetChain.contains(edge.destination);
        }

        int target;
        int targetPort;
        int round;
        boolean paused;
        final ArrayDeque<Production> production = new ArrayDeque<>();
        long measuredSince = -1;
        double produced;

        void record(long tick, double amount) {
            production.addLast(new Production(tick, amount));
            produced += amount;
        }

        /** Use game ticks so the rate stays comparable to recipe estimates while the server lags. */
        double actualRate(long tick) {
            if (measuredSince < 0) measuredSince = tick;
            while (!production.isEmpty() && tick - production.peekFirst().tick >= 400) {
                produced -= production.removeFirst().amount;
            }
            return produced * 20 / Math.max(1, Math.min(400, tick - measuredSince + 1));
        }

        Graph(int id) {
            this.id = id;
        }

        /** Reject a link when its destination can already reach its source. */
        boolean canLink(int source, int destination) {
            var visited = new HashSet<Integer>();
            var pending = new ArrayDeque<Integer>();
            pending.add(destination);
            while (!pending.isEmpty()) {
                int node = pending.removeFirst();
                if (node == source) return false;
                if (visited.add(node)) for (Edge edge : edges.values()) {
                    if (edge.source == node) pending.add(edge.destination);
                }
            }
            return true;
        }
    }

    private static final class Production {

        final long tick;
        final double amount;

        Production(long tick, double amount) {
            this.tick = tick;
            this.amount = amount;
        }
    }

    static final class Node {

        final int id, slot;
        final ItemStack pattern;
        int x, y, oc, parallel = 1;
        long power = -1;

        Node(int id, int slot, ItemStack pattern) {
            this.id = id;
            this.slot = slot;
            this.pattern = pattern.copy();
        }

        String key() {
            return "node:" + id;
        }

        boolean matches(ItemStack installed) {
            return installed != null && ItemStack.areItemStacksEqual(pattern, installed);
        }
    }

    static final class Material {

        final ItemStack item;
        final FluidStack fluid;

        Material(ItemStack item, FluidStack fluid) {
            this.item = item;
            this.fluid = fluid;
        }

        boolean matches(ItemStack stack) {
            return item != null && stack != null
                && item.getItem() == stack.getItem()
                && item.getItemDamage() == stack.getItemDamage()
                && ItemStack.areItemStackTagsEqual(item, stack);
        }

        boolean matches(FluidStack stack) {
            return fluid != null && stack != null && fluid.isFluidEqual(stack);
        }

        boolean matches(Material other) {
            return matches(other.item) || matches(other.fluid);
        }

        String name() {
            return item != null ? item.getDisplayName() : fluid.getLocalizedName();
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            if (item != null) GTUtility.saveItem(tag, "item", item);
            else fluid.writeToNBT(tag);
            return tag;
        }

        static Material load(NBTTagCompound tag) {
            ItemStack item = GTUtility.loadItem(tag, "item");
            FluidStack fluid = item == null ? FluidStack.loadFluidStackFromNBT(tag) : null;
            return item == null && fluid == null ? null : new Material(item, fluid);
        }
    }

    static final class Edge {

        final int id, source, destination;
        final Material material;
        int highWater;
        int capacity = 1;
        long lastDelivery;
        boolean feedsTarget;
        long amount;

        Edge(int id, int source, int destination, Material material) {
            this.id = id;
            this.source = source;
            this.destination = destination;
            this.material = material;
        }

        long space() {
            return Math.max(0, capacity - amount);
        }
    }

    private long deliverySequence;

    /** Move only actual rolled yields to the linked buffers. Unaccepted amounts stay in the task. */
    boolean route(int[] routes, RecipeThreadOutputs outputs) {
        List<Edge> candidates = new ArrayList<>();
        for (Graph graph : graphs.values()) for (int id : routes) {
            Edge edge = graph.edges.get(id);
            if (edge != null) candidates.add(edge);
        }
        var ledger = (ThreadedProxyOutputAccessor) (Object) outputs;
        boolean pending = false;
        for (int i = 0; i < ledger.gtnhadjust$getItems().length; i++) {
            ItemStack stack = ledger.gtnhadjust$getItems()[i];
            if (stack != null)
                pending |= route(candidates, new Material(stack, null), ledger.gtnhadjust$getItemAmounts(), i);
        }
        for (int i = 0; i < ledger.gtnhadjust$getFluids().length; i++) {
            FluidStack stack = ledger.gtnhadjust$getFluids()[i];
            if (stack != null)
                pending |= route(candidates, new Material(null, stack), ledger.gtnhadjust$getFluidAmounts(), i);
        }
        return !pending;
    }

    private boolean route(List<Edge> candidates, Material material, BigInteger[] amounts, int index) {
        if (amounts[index].signum() <= 0) return false;
        List<Edge> edges = candidates.stream()
            .filter(edge -> edge.material.matches(material))
            .sorted(
                Comparator.comparingDouble((Edge edge) -> (double) edge.amount / edge.capacity)
                    .thenComparingLong(edge -> edge.lastDelivery))
            .collect(java.util.stream.Collectors.toList());
        deliver(edges, amounts, index, true);
        deliver(edges, amounts, index, false);
        return !edges.isEmpty() && amounts[index].signum() > 0;
    }

    private void deliver(List<Edge> edges, BigInteger[] amounts, int index, boolean priority) {
        long room = 0;
        for (Edge edge : edges) if (edge.feedsTarget == priority) room += edge.space();
        if (room == 0 || amounts[index].signum() == 0) return;
        long move = amounts[index].min(BigInteger.valueOf(room))
            .longValue();
        long unassigned = move;
        // Share a batch in proportion to the free space; give rounding leftovers to the emptiest lines.
        for (Edge edge : edges) {
            if (edge.feedsTarget != priority) continue;
            long share = BigInteger.valueOf(move)
                .multiply(BigInteger.valueOf(edge.space()))
                .divide(BigInteger.valueOf(room))
                .longValue();
            if (share == 0) continue;
            edge.amount += share;
            edge.lastDelivery = ++deliverySequence;
            unassigned -= share;
        }
        for (Edge edge : edges) {
            if (unassigned == 0) break;
            if (edge.feedsTarget != priority || edge.space() == 0) continue;
            edge.amount++;
            edge.lastDelivery = ++deliverySequence;
            unassigned--;
        }
        amounts[index] = amounts[index].subtract(BigInteger.valueOf(move - unassigned));
    }

    /** Detach buffered material using the same persisted ledger as normal external output. */
    void drain(Edge edge) {
        if (edge.amount == 0) return;
        NBTTagCompound ledger = new NBTTagCompound();
        NBTTagList entries = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        if (edge.material.item != null) {
            GTUtility.saveItem(entry, "stack", edge.material.item);
            entry.setString("amount", Long.toString(edge.amount));
            entries.appendTag(entry);
            ledger.setTag("items", entries);
        } else {
            edge.material.fluid.writeToNBT(entry);
            entry.setString("bigAmount", Long.toString(edge.amount));
            entries.appendTag(entry);
            ledger.setTag("fluids", entries);
        }
        exports.add(RecipeThreadOutputs.load(ledger));
        edge.amount = 0;
    }

    NBTTagCompound save() {
        NBTTagCompound result = new NBTTagCompound();
        result.setInteger("next", nextId);
        result.setInteger("parallel", parallelMultiplier);
        result.setInteger("oc", overclockMultiplier);
        NBTTagList savedGraphs = new NBTTagList();
        for (Graph graph : graphs.values()) {
            NBTTagCompound saved = new NBTTagCompound();
            saved.setInteger("id", graph.id);
            saved.setInteger("target", graph.target);
            saved.setInteger("targetPort", graph.targetPort);
            saved.setBoolean("paused", graph.paused);
            NBTTagList nodes = new NBTTagList(), edges = new NBTTagList();
            for (Node node : graph.nodes.values()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("id", node.id);
                tag.setInteger("slot", node.slot);
                GTUtility.saveItem(tag, "pattern", node.pattern);
                tag.setInteger("x", node.x);
                tag.setInteger("y", node.y);
                tag.setInteger("parallel", node.parallel);
                tag.setInteger("oc", node.oc);
                tag.setLong("power", node.power);
                nodes.appendTag(tag);
            }
            for (Edge edge : graph.edges.values()) {
                NBTTagCompound tag = edge.material.save();
                tag.setInteger("id", edge.id);
                tag.setInteger("source", edge.source);
                tag.setInteger("destination", edge.destination);
                tag.setInteger("high", edge.highWater);
                tag.setInteger("capacity", edge.capacity);
                tag.setLong("amount", edge.amount);
                edges.appendTag(tag);
            }
            saved.setTag("nodes", nodes);
            saved.setTag("edges", edges);
            savedGraphs.appendTag(saved);
        }
        result.setTag("graphs", savedGraphs);
        NBTTagList output = new NBTTagList();
        for (RecipeThreadOutputs ledger : exports) output.appendTag(ledger.save());
        result.setTag("exports", output);
        return result;
    }

    void load(NBTTagCompound saved) {
        graphs.clear();
        exports.clear();
        nextId = Math.max(1, saved.getInteger("next"));
        parallelMultiplier = Math.max(1, saved.getInteger("parallel"));
        overclockMultiplier = Math.max(1, saved.getInteger("oc"));
        NBTTagList loadedGraphs = saved.getTagList("graphs", 10);
        for (int g = 0; g < loadedGraphs.tagCount(); g++) {
            NBTTagCompound tag = loadedGraphs.getCompoundTagAt(g);
            Graph graph = new Graph(tag.getInteger("id"));
            graph.target = tag.getInteger("target");
            graph.targetPort = tag.getInteger("targetPort");
            graph.paused = tag.getBoolean("paused");
            NBTTagList nodes = tag.getTagList("nodes", 10), edges = tag.getTagList("edges", 10);
            for (int n = 0; n < nodes.tagCount(); n++) {
                NBTTagCompound data = nodes.getCompoundTagAt(n);
                ItemStack pattern = GTUtility.loadItem(data, "pattern");
                if (pattern == null) continue;
                Node node = new Node(data.getInteger("id"), data.getInteger("slot"), pattern);
                node.x = data.getInteger("x");
                node.y = data.getInteger("y");
                node.parallel = data.getInteger("parallel");
                node.oc = data.getInteger("oc");
                node.power = data.getLong("power");
                graph.nodes.put(node.id, node);
            }
            for (int e = 0; e < edges.tagCount(); e++) {
                NBTTagCompound data = edges.getCompoundTagAt(e);
                Material material = Material.load(data);
                if (material == null) continue;
                Edge edge = new Edge(
                    data.getInteger("id"),
                    data.getInteger("source"),
                    data.getInteger("destination"),
                    material);
                edge.highWater = data.getInteger("high");
                edge.capacity = Math.max(1, data.getInteger("capacity"));
                edge.amount = data.getLong("amount");
                graph.edges.put(edge.id, edge);
            }
            graph.updateTargetChain();
            graphs.put(graph.id, graph);
        }
        NBTTagList output = saved.getTagList("exports", 10);
        for (int i = 0; i < output.tagCount(); i++) exports.add(RecipeThreadOutputs.load(output.getCompoundTagAt(i)));
    }
}
