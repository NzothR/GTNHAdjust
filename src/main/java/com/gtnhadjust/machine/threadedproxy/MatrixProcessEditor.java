package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.gtnhadjust.machine.threadedproxy.MatrixProcessGraphs.Edge;
import com.gtnhadjust.machine.threadedproxy.MatrixProcessGraphs.Graph;
import com.gtnhadjust.machine.threadedproxy.MatrixProcessGraphs.Material;
import com.gtnhadjust.machine.threadedproxy.MatrixProcessGraphs.Node;
import com.gtnhadjust.machine.threadedproxy.ThreadedJointProxy.Definition;
import com.gtnhadjust.mixins.gtnr.ThreadedProxyOutputAccessor;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;

import gregtech.api.util.GTRecipe;

/** Server-side graph editing, port validation and steady-state estimates. */
final class MatrixProcessEditor {

    private final ThreadedJointProxy machine;
    private final MatrixProcessGraphs model;

    MatrixProcessEditor(ThreadedJointProxy machine) {
        this.machine = machine;
        this.model = machine.processes;
    }

    static final class Port {

        final Material material;
        final int amount, chance;

        Port(Material material, int amount, int chance) {
            this.material = material;
            this.amount = amount;
            this.chance = chance;
        }

        double expected() {
            return amount * (chance / 10000.0);
        }
    }

    static List<Port> ports(Definition definition, boolean output) {
        List<Port> ports = new ArrayList<>();
        GTRecipe recipe = definition.execution;
        if (output) {
            definition.itemOutputs.stream()
                .forEach(i -> {
                    var stack = recipe.mOutputs[i];
                    if (stack != null)
                        ports.add(new Port(new Material(stack, null), stack.stackSize, recipe.getOutputChance(i)));
                });
            definition.fluidOutputs.stream()
                .forEach(i -> {
                    var stack = recipe.mFluidOutputs[i];
                    if (stack != null)
                        ports.add(new Port(new Material(null, stack), stack.amount, recipe.getFluidOutputChance(i)));
                });
        } else {
            for (var stack : definition.recipe.consumedItems)
                if (stack.stackSize > 0) ports.add(new Port(new Material(stack, null), stack.stackSize, 10000));
            for (var stack : definition.recipe.consumedFluids)
                if (stack.amount > 0 && java.util.Arrays.stream(recipe.mFluidInputs)
                    .anyMatch(required -> required != null && required.amount > 0 && required.isFluidEqual(stack)))
                    ports.add(new Port(new Material(null, stack), stack.amount, 10000));
        }
        return ports;
    }

    /** The client sends edits, never an authoritative graph or inventory. */
    void edit(String command) {
        if (command == null || command.length() > 160) return;
        String[] a = command.split(" ");
        try {
            if (a.length < 2) return;
            Graph graph = model.graphs.get(Integer.parseInt(a[1]));
            if ("pause".equals(a[0]) && graph != null) {
                graph.paused = !graph.paused;
                machine.markDirty();
                return;
            }
            if (!machine.canEditProcesses()) return;
            if ("add".equals(a[0])) {
                addNode(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]));
            } else if ("link".equals(a[0])) {
                link(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5]));
            } else if ("multipliers".equals(a[0])) {
                int parallel = positive(a[2]), oc = positive(a[3]);
                model.parallelMultiplier = parallel;
                model.overclockMultiplier = oc;
                model.graphs.values()
                    .forEach(this::updateWater);
            } else if (graph != null) {
                editGraph(graph, a);
            }
            machine.markDirty();
        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
            // Incomplete fields and malformed client packets have no operation to apply.
        }
    }

    private void editGraph(Graph graph, String[] a) {
        switch (a[0]) {
            case "deleteGraph" -> {
                graph.edges.values()
                    .forEach(model::drain);
                model.graphs.remove(graph.id);
            }
            case "unlink" -> {
                Edge edge = graph.edges.remove(Integer.parseInt(a[2]));
                if (edge != null) {
                    model.drain(edge);
                    model.split(graph);
                }
            }
            case "water" -> {
                Edge edge = graph.edges.get(Integer.parseInt(a[2]));
                if (edge != null) edge.highWater = Math.max(0, Integer.parseInt(a[3]));
            }
            case "balance" -> balance(graph);
            default -> editNode(graph, graph.nodes.get(Integer.parseInt(a[2])), a);
        }
        graph.updateTargetChain();
        updateWater(graph);
    }

    private void editNode(Graph graph, Node node, String[] a) {
        if (node == null) return;
        switch (a[0]) {
            case "move" -> {
                int x = Integer.parseInt(a[3]), y = Integer.parseInt(a[4]);
                node.x = x;
                node.y = y;
            }
            case "remove" -> {
                graph.edges.values()
                    .removeIf(edge -> {
                        if (edge.source != node.id && edge.destination != node.id) return false;
                        model.drain(edge);
                        return true;
                    });
                graph.nodes.remove(node.id);
                if (graph.target == node.id) {
                    graph.target = 0;
                    graph.production.clear();
                    graph.produced = 0;
                    graph.measuredSince = -1;
                }
                model.split(graph);
            }
            case "settings" -> {
                int parallel = positive(a[3]), oc = Math.max(0, Integer.parseInt(a[4]));
                node.parallel = parallel;
                node.oc = oc;
            }
            case "target" -> {
                Definition definition = machine.nodeDefinition(node);
                if (definition == null) return;
                boolean enabled = "1".equals(a[3]);
                if (!enabled && graph.target != node.id) return;
                int index = 0;
                if (enabled) {
                    List<Port> outputs = ports(definition, true);
                    while (index < outputs.size()) {
                        Port port = outputs.get(index);
                        if (port.chance > 0 && graph.edges.values()
                            .stream()
                            .noneMatch(edge -> edge.source == node.id && edge.material.matches(port.material))) break;
                        index++;
                    }
                    if (index == outputs.size()) return;
                }
                int target = enabled ? node.id : 0;
                if (graph.target != target || graph.targetPort != index) {
                    graph.production.clear();
                    graph.produced = 0;
                    graph.measuredSince = -1;
                }
                graph.target = target;
                graph.targetPort = index;
            }
            default -> {}
        }
    }

    private void addNode(int slot, int x, int y) {
        if (slot < 0 || slot >= machine.patterns()
            .getSlots() || machine.patternRecipe(slot) == null) return;
        if (model.graphs.values()
            .stream()
            .mapToInt(graph -> graph.nodes.size())
            .sum() >= 128) return;
        ItemStack pattern = machine.patterns()
            .getStackInSlot(slot);
        if (pattern == null) return;
        Node node = new Node(model.nextId++, slot, pattern);
        node.x = x;
        node.y = y;
        Graph graph = new Graph(node.id);
        graph.nodes.put(node.id, node);
        model.graphs.put(graph.id, graph);
    }

    private void link(int source, int output, int destination, int input) {
        Graph fromGraph = model.component(source), toGraph = model.component(destination);
        if (fromGraph == null || toGraph == null || (fromGraph == toGraph && !fromGraph.canLink(source, destination)))
            return;
        Definition from = machine.nodeDefinition(fromGraph.nodes.get(source));
        Definition to = machine.nodeDefinition(toGraph.nodes.get(destination));
        if (from == null || to == null) return;
        Port produced = ports(from, true).get(output);
        Port consumed = ports(to, false).get(input);
        if (produced.chance <= 0 || !produced.material.matches(consumed.material)) return;
        if (toGraph.edges.values()
            .stream()
            .anyMatch(edge -> edge.destination == destination && edge.material.matches(consumed.material))) return;
        Port previousTarget = targetPort(fromGraph);
        boolean consumesTarget = fromGraph.target == source && previousTarget != null
            && previousTarget.material.matches(produced.material);
        model
            .join(fromGraph, toGraph, new Edge(model.nextId++, source, destination, consumed.material), consumesTarget);
        updateWater(toGraph);
    }

    void updateWater(Graph graph) {
        for (Edge edge : graph.edges.values()) {
            Node downstream = graph.nodes.get(edge.destination);
            Definition definition = machine.nodeDefinition(downstream);
            if (definition == null) continue;
            long units = ports(definition, false).stream()
                .filter(port -> port.material.matches(edge.material))
                .mapToLong(port -> port.amount)
                .sum();
            long batch = Math.min(units, Integer.MAX_VALUE)
                * ThreadedJointProxy.scaled(downstream.parallel, model.parallelMultiplier);
            edge.capacity = edge.highWater > 0 ? (int) Math.min(Integer.MAX_VALUE, Math.max(units, edge.highWater))
                : (int) Math.max(1, Math.min(Integer.MAX_VALUE, batch + Math.min(batch, Integer.MAX_VALUE)));
        }
    }

    private static int positive(String text) {
        return Math.max(1, Integer.parseInt(text));
    }

    /** Visit the target's ancestors in reverse topological order, adding demand from all active branches. */
    private void balance(Graph graph) {
        Node target = graph.nodes.get(graph.target);
        if (target == null) return;
        var plans = new LinkedHashMap<Integer, Double>();
        plans.put(target.id, requestedOperations(target, machine.nodeDefinition(target)));
        List<Node> order = topological(graph);
        for (int i = order.size() - 1; i >= 0; i--) {
            Node node = order.get(i);
            Definition definition = machine.nodeDefinition(node);
            if (node == target || definition == null) continue;
            Map<Material, Double> demand = new LinkedHashMap<>();
            for (Edge edge : graph.edges.values()) {
                if (edge.source != node.id || !plans.containsKey(edge.destination)) continue;
                Definition downstream = machine.nodeDefinition(graph.nodes.get(edge.destination));
                double rate = plans.get(edge.destination) * inputAmount(downstream, edge);
                Material key = demand.keySet()
                    .stream()
                    .filter(material -> material.matches(edge.material))
                    .findFirst()
                    .orElse(edge.material);
                demand.merge(key, rate, Double::sum);
            }
            double needed = 0;
            for (var entry : demand.entrySet()) {
                double yield = ports(definition, true).stream()
                    .filter(port -> port.material.matches(entry.getKey()))
                    .mapToDouble(Port::expected)
                    .sum();
                if (yield > 0) needed = Math.max(needed, entry.getValue() / yield);
            }
            if (needed <= 0) continue;
            long budget = machine.getMaxInputEu();
            double bestRate = -1;
            long bestPower = Long.MAX_VALUE;
            int bestDuration = 0;
            int bestParallel = node.parallel, bestOc = node.oc;
            for (int oc = 0; oc <= 30; oc++) {
                var plan = machine.nodePlan(definition, oc, budget);
                if (plan == null) break;
                long max = plan.eut == 0 ? Integer.MAX_VALUE : budget / plan.eut;
                int baseLimit = (int) Math.min(Integer.MAX_VALUE, max / model.parallelMultiplier);
                if (baseLimit == 0) continue;
                int parallel = Math.min(
                    baseLimit,
                    Math.max(1, (int) Math.ceil(needed * plan.duration / (20.0 * model.parallelMultiplier))));
                int actual = ThreadedJointProxy.scaled(parallel, model.parallelMultiplier);
                double rate = 20.0 * actual / plan.duration;
                long power = plan.eut * actual;
                if (Math.min(rate, needed) > Math.min(bestRate, needed)
                    || (Math.min(rate, needed) == Math.min(bestRate, needed)
                        && (power < bestPower || (power == bestPower && plan.duration > bestDuration)))) {
                    bestRate = rate;
                    bestPower = power;
                    bestDuration = plan.duration;
                    bestParallel = parallel;
                    bestOc = oc;
                }
            }
            node.parallel = bestParallel;
            node.oc = bestOc;
            plans.put(node.id, needed);
        }
    }

    private static List<Node> topological(Graph graph) {
        List<Node> order = new ArrayList<>();
        List<Node> remaining = new ArrayList<>(graph.nodes.values());
        while (!remaining.isEmpty()) {
            int before = remaining.size();
            remaining.removeIf(node -> {
                if (graph.edges.values()
                    .stream()
                    .anyMatch(
                        edge -> edge.destination == node.id && order.stream()
                            .noneMatch(prior -> prior.id == edge.source)))
                    return false;
                order.add(node);
                return true;
            });
            if (remaining.size() == before) break;
        }
        return order;
    }

    private static long inputAmount(Definition definition, Edge edge) {
        return definition == null ? 0
            : ports(definition, false).stream()
                .filter(port -> port.material.matches(edge.material))
                .mapToLong(port -> port.amount)
                .sum();
    }

    private double requestedOperations(Node node, Definition definition) {
        if (definition == null) return 0;
        int oc = ThreadedJointProxy.scaled(node.oc, model.overclockMultiplier);
        var plan = ThreadedProxyAllocation.plan(
            definition.execution.mEUt,
            definition.execution.mDuration,
            gregtech.api.enums.GTValues.V[gregtech.api.enums.VoltageIndex.MAX],
            oc);
        return 20.0 * ThreadedJointProxy.scaled(node.parallel, model.parallelMultiplier) / plan.duration;
    }

    private double operations(Node node) {
        Definition definition = machine.nodeDefinition(node);
        if (definition == null) return 0;
        var plan = machine.nodePlan(definition, node.oc, machine.getMaxInputEu());
        if (plan == null) return 0;
        long parallel = ThreadedJointProxy.scaled(node.parallel, model.parallelMultiplier);
        if (plan.eut > 0) parallel = Math.min(parallel, machine.getMaxInputEu() / plan.eut);
        return 20.0 * parallel / plan.duration;
    }

    /** Count the rolled quantity once, before any part of the finished ledger is delivered. */
    void recordTarget(String key, RecipeThreadOutputs outputs, long tick) {
        for (Graph graph : model.graphs.values()) {
            Node target = graph.nodes.get(graph.target);
            if (target == null || !target.key()
                .equals(key)) continue;
            Port port = targetPort(graph);
            if (port == null) return;
            var ledger = (ThreadedProxyOutputAccessor) (Object) outputs;
            double produced = 0;
            for (int i = 0; i < ledger.gtnhadjust$getItems().length; i++)
                if (port.material.matches(ledger.gtnhadjust$getItems()[i]))
                    produced += ledger.gtnhadjust$getItemAmounts()[i].doubleValue();
            for (int i = 0; i < ledger.gtnhadjust$getFluids().length; i++)
                if (port.material.matches(ledger.gtnhadjust$getFluids()[i]))
                    produced += ledger.gtnhadjust$getFluidAmounts()[i].doubleValue();
            if (produced > 0) graph.record(tick, produced);
            return;
        }
    }

    private Port targetPort(Graph graph) {
        Node node = graph.nodes.get(graph.target);
        Definition definition = node == null ? null : machine.nodeDefinition(node);
        if (definition == null) return null;
        List<Port> ports = ports(definition, true);
        return graph.targetPort >= 0 && graph.targetPort < ports.size() ? ports.get(graph.targetPort) : null;
    }

    private static double outputPerOperation(Definition definition, Material material) {
        return definition == null ? 0
            : ports(definition, true).stream()
                .filter(port -> port.material.matches(material))
                .mapToDouble(Port::expected)
                .sum();
    }

    /** Estimate linked-material supply forwards, sharing each output in proportion to requested rates. */
    private void estimateGraph(NBTTagCompound result, Graph graph) {
        Port port = targetPort(graph);
        if (port == null) return;
        Node target = graph.nodes.get(graph.target);
        Map<Integer, Double> requested = new LinkedHashMap<>(), attainable = new LinkedHashMap<>();
        Map<Integer, Definition> definitions = new LinkedHashMap<>();
        for (Node node : graph.nodes.values()) {
            Definition definition = machine.nodeDefinition(node);
            definitions.put(node.id, definition);
            requested.put(node.id, operations(node));
        }
        for (Node node : topological(graph)) {
            double rate = requested.get(node.id);
            for (Edge edge : graph.edges.values()) {
                if (edge.destination != node.id) continue;
                long consumption = inputAmount(definitions.get(node.id), edge);
                if (consumption == 0) continue;
                double totalDemand = 0, targetDemand = 0;
                for (Edge branch : graph.edges.values()) {
                    if (branch.source != edge.source || !branch.material.matches(edge.material)) continue;
                    double demand = requested.get(branch.destination)
                        * inputAmount(definitions.get(branch.destination), branch);
                    if (branch.feedsTarget) targetDemand += demand;
                    if (branch.feedsTarget == edge.feedsTarget) totalDemand += demand;
                }
                if (totalDemand <= 0) continue;
                double supply = attainable.getOrDefault(edge.source, 0.0)
                    * outputPerOperation(definitions.get(edge.source), edge.material);
                if (!edge.feedsTarget) supply = Math.max(0, supply - targetDemand);
                rate = Math.min(rate, supply / totalDemand * requested.get(node.id));
            }
            attainable.put(node.id, rate);
        }
        double targetYield = outputPerOperation(definitions.get(target.id), port.material);
        result.setDouble("targetRate", requestedOperations(target, definitions.get(target.id)) * targetYield);
        double totalPower = 0;
        for (Node node : graph.nodes.values()) {
            Definition definition = definitions.get(node.id);
            if (definition == null) continue;
            var plan = machine.nodePlan(definition, node.oc, machine.getMaxInputEu());
            if (plan != null) totalPower += attainable.getOrDefault(node.id, 0.0) * plan.duration * plan.eut / 20.0;
        }
        double scale = totalPower > machine.getMaxInputEu() ? machine.getMaxInputEu() / totalPower : 1;
        result.setDouble("powerLoad", totalPower);
        result.setDouble("feasibleRate", attainable.getOrDefault(target.id, 0.0) * targetYield * scale);
        result.setDouble(
            "actualRate",
            graph.actualRate(
                machine.getBaseMetaTileEntity()
                    .getTimer()));
    }

    /** A bounded view for the editor. Inventories and configuration stay server-authoritative. */
    NBTTagCompound snapshot() {
        NBTTagCompound result = new NBTTagCompound();
        result.setBoolean("edit", machine.canEditProcesses());
        result.setInteger("parallel", model.parallelMultiplier);
        result.setInteger("oc", model.overclockMultiplier);
        result.setInteger("components", model.graphs.size());
        NBTTagList nodes = new NBTTagList(), edges = new NBTTagList(), components = new NBTTagList();
        for (Graph graph : model.graphs.values()) {
            NBTTagCompound info = new NBTTagCompound();
            info.setInteger("id", graph.id);
            info.setBoolean("paused", graph.paused);
            info.setInteger("size", graph.nodes.size());
            estimateGraph(info, graph);
            components.appendTag(info);
            appendComponent(graph, nodes, edges);
        }
        result.setTag("nodes", nodes);
        result.setTag("edges", edges);
        result.setTag("list", components);
        return result;
    }

    private void appendComponent(Graph graph, NBTTagList nodes, NBTTagList edges) {
        for (Node node : graph.nodes.values()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("id", node.id);
            tag.setInteger("graph", graph.id);
            tag.setBoolean("target", graph.target == node.id);
            tag.setInteger("slot", node.slot);
            tag.setInteger("x", node.x);
            tag.setInteger("y", node.y);
            tag.setInteger("parallel", node.parallel);
            tag.setInteger("oc", node.oc);
            tag.setString("state", machine.nodeState(node));
            Definition definition = machine.nodeDefinition(node);
            tag.setBoolean("valid", definition != null);
            if (definition != null) {
                tag.setTag("in", describe(ports(definition, false)));
                tag.setTag("out", describe(ports(definition, true)));
            }
            nodes.appendTag(tag);
        }
        for (Edge edge : graph.edges.values()) {
            NBTTagCompound tag = edge.material.save();
            tag.setInteger("id", edge.id);
            tag.setInteger("graph", graph.id);
            tag.setInteger("source", edge.source);
            tag.setInteger("destination", edge.destination);
            tag.setInteger("high", edge.highWater);
            tag.setInteger("capacity", edge.capacity);
            tag.setLong("amount", edge.amount);
            edges.appendTag(tag);
        }
    }

    private static NBTTagList describe(List<Port> ports) {
        NBTTagList list = new NBTTagList();
        for (Port port : ports) list.appendTag(describe(port));
        return list;
    }

    private static NBTTagCompound describe(Port port) {
        NBTTagCompound tag = port.material.save();
        tag.setInteger("quantity", port.amount);
        tag.setInteger("chance", port.chance);
        return tag;
    }
}
