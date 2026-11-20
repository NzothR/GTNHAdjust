package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.gtnhadjust.machine.threadedproxy.MatrixProcessGraphs.Material;

/** Dark node canvas with local zoom, pan, drag preview and a sampled-pattern picker. */
final class MatrixProcessCanvas extends Widget<MatrixProcessCanvas> implements Interactable {

    private static final int LEFT = 5, TOP = 42, RIGHT = 247, BOTTOM = 181;
    private static final int TEAL = 0xff62e4d3, INK = 0xffe7eef8, MUTED = 0xffaabacf, AMBER = 0xffffce82;
    private final Supplier<NBTTagCompound> view, patterns;
    private final Consumer<String> send;
    private final Consumer<NBTTagCompound> selectionChanged;
    private final List<Hit> hits = new ArrayList<>();
    private int graph, selected, selectedEdge, source, output, dragNode, dragX, dragY, anchorX, anchorY;
    private int panX, panY, lastX, lastY, scroll, pickerScroll;
    private double zoom = 1;
    private boolean picker, panning, dragged;
    private NBTTagCompound snapshot = new NBTTagCompound();

    MatrixProcessCanvas(Supplier<NBTTagCompound> view, Supplier<NBTTagCompound> patterns, Consumer<String> send,
        Consumer<NBTTagCompound> selectionChanged) {
        this.view = view;
        this.patterns = patterns;
        this.send = send;
        this.selectionChanged = selectionChanged;
    }

    private static final class Hit {

        final int x, y, w, h;
        final Runnable action;

        Hit(int x, int y, int w, int h, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        boolean contains(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private static final class Point {

        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal("gtnhadjust.matrix." + key);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        NBTTagCompound current = view.get();
        if (current != null) snapshot = current;
        if (graph != snapshot.getInteger("graph")) {
            graph = snapshot.getInteger("graph");
            selected = selectedEdge = source = scroll = panX = panY = 0;
            picker = false;
            zoom = 1;
        }
        hits.clear();
        GuiDraw.drawRoundedRect(0, 0, 406, 186, 5, 8, 0xff121b2a);
        text(tr("title") + "  ·  " + (graph == 0 ? "—" : "#" + graph), 7, 6, 210, INK);
        text("×" + snapshot.getInteger("parallel") + " P  ·  ×" + snapshot.getInteger("oc") + " OC", 225, 6, 140, TEAL);
        GuiDraw.drawRect(LEFT, TOP, RIGHT - LEFT, BOTTOM - TOP, 0xff0a1220);
        GuiDraw.drawRect(253, TOP, 149, BOTTOM - TOP, 0xff1b283b);
        for (int x = LEFT + Math.floorMod(panX, 14); x < RIGHT; x += 14)
            for (int y = TOP + Math.floorMod(panY, 14); y < BOTTOM; y += 14) GuiDraw.drawRect(x, y, 1, 1, 0xff233247);
        Map<Integer, NBTTagCompound> nodes = indexed(snapshot.getTagList("nodes", 10));
        Map<Integer, NBTTagCompound> edges = indexed(snapshot.getTagList("edges", 10));
        for (NBTTagCompound edge : edges.values()) drawEdge(edge, nodes);
        for (NBTTagCompound node : nodes.values()) drawNode(node);
        if (source != 0 && nodes.containsKey(source)) {
            Point from = port(nodes.get(source), true, output);
            int mx = mouseX(), my = mouseY();
            if (insideCanvas(mx, my)) line(from, new Point(mx, my), TEAL);
        }
        if (nodes.isEmpty()) text(tr("empty"), LEFT + 9, TOP + 10, RIGHT - LEFT - 18, MUTED);
        drawInspector(nodes.get(selected), edges.get(selectedEdge));
        if (picker) drawPicker();
    }

    private static Map<Integer, NBTTagCompound> indexed(NBTTagList list) {
        Map<Integer, NBTTagCompound> entries = new LinkedHashMap<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            entries.put(tag.getInteger("id"), tag);
        }
        return entries;
    }

    private void drawEdge(NBTTagCompound edge, Map<Integer, NBTTagCompound> nodes) {
        NBTTagCompound from = nodes.get(edge.getInteger("source")), to = nodes.get(edge.getInteger("destination"));
        Material material = Material.load(edge);
        if (from == null || to == null || material == null) return;
        Point start = port(from, true, indexOf(from.getTagList("out", 10), material));
        Point end = port(to, false, indexOf(to.getTagList("in", 10), material));
        int id = edge.getInteger("id");
        Point middle = line(start, end, selectedEdge == id ? AMBER : 0xff438f9a);
        if (insideCanvas(middle.x, middle.y)) {
            GuiDraw.drawRect(middle.x - 2, middle.y - 2, 5, 5, AMBER);
            hits.add(new Hit(middle.x - 4, middle.y - 4, 9, 9, () -> {
                selected = 0;
                selectedEdge = id;
                scroll = 0;
                selectionChanged.accept(edge);
            }));
        }
    }

    private static int indexOf(NBTTagList ports, Material material) {
        for (int i = 0; i < ports.tagCount(); i++) {
            Material candidate = Material.load(ports.getCompoundTagAt(i));
            if (candidate != null && material.matches(candidate)) return i;
        }
        return 0;
    }

    private Point line(Point start, Point end, int color) {
        int mid = start.x + (end.x - start.x) / 2;
        clipRect(Math.min(start.x, mid), start.y, Math.abs(start.x - mid) + 1, 1, color);
        clipRect(mid, Math.min(start.y, end.y), 1, Math.abs(start.y - end.y) + 1, color);
        clipRect(Math.min(mid, end.x), end.y, Math.abs(end.x - mid) + 1, 1, color);
        return new Point(mid, start.y + (end.y - start.y) / 2);
    }

    private static void clipRect(int x, int y, int w, int h, int color) {
        int left = Math.max(LEFT, x), top = Math.max(TOP, y);
        int right = Math.min(RIGHT, x + w), bottom = Math.min(BOTTOM, y + h);
        if (left < right && top < bottom) GuiDraw.drawRect(left, top, right - left, bottom - top, color);
    }

    private int screenX(NBTTagCompound node) {
        int x = node.getInteger("id") == dragNode ? dragX : node.getInteger("x");
        return LEFT + panX + (int) (x * zoom);
    }

    private int screenY(NBTTagCompound node) {
        int y = node.getInteger("id") == dragNode ? dragY : node.getInteger("y");
        return TOP + panY + (int) (y * zoom);
    }

    private int nodeWidth() {
        return (int) (92 * zoom);
    }

    private Point port(NBTTagCompound node, boolean out, int index) {
        return new Point(screenX(node) + (out ? nodeWidth() : 0), screenY(node) + (int) ((27 + index * 11) * zoom));
    }

    private void drawNode(NBTTagCompound node) {
        int id = node.getInteger("id");
        int x = screenX(node), y = screenY(node), w = nodeWidth();
        NBTTagList in = node.getTagList("in", 10), out = node.getTagList("out", 10);
        int h = (int) ((28 + 11 * Math.max(1, Math.max(in.tagCount(), out.tagCount()))) * zoom);
        if (x + w < LEFT || x >= RIGHT || y + h < TOP || y >= BOTTOM) return;
        boolean target = id == snapshot.getInteger("target");
        clipRect(x, y, w, h, target ? 0xff254640 : 0xff26354a);
        clipRect(x, y, w, 2, id == selected || target ? TEAL : 0xff54708f);
        if (insideCanvas(x + 4, y + 6) && x + w <= RIGHT && y + 23 <= BOTTOM) {
            text(
                "#" + (node.getInteger("slot") + 1) + " " + materialName(out.getCompoundTagAt(0)),
                x + 5,
                y + 5,
                w - 9,
                INK);
            text(target ? tr("target") : state(node), x + 5, y + 15, w - 9, target ? TEAL : MUTED);
        }
        hits.add(
            new Hit(
                Math.max(x, LEFT),
                Math.max(y, TOP),
                Math.min(x + w, RIGHT) - Math.max(x, LEFT),
                Math.min(y + h, BOTTOM) - Math.max(y, TOP),
                () -> selectNode(node, true)));
        drawPorts(node, in, false);
        drawPorts(node, out, true);
    }

    private void drawPorts(NBTTagCompound node, NBTTagList ports, boolean out) {
        for (int i = 0; i < ports.tagCount(); i++) {
            Point p = port(node, out, i);
            if (!insideCanvas(p.x, p.y)) continue;
            int index = i, id = node.getInteger("id");
            clipRect(p.x - 3, p.y - 3, 6, 6, out ? TEAL : 0xff83aaff);
            if (zoom >= 0.9) {
                String name = materialName(ports.getCompoundTagAt(i));
                int x = out ? p.x - 42 : p.x + 5;
                if (x >= LEFT && x + 37 < RIGHT && p.y + 6 < BOTTOM) text(name, x, p.y - 3, 37, MUTED);
            }
            hits.add(new Hit(p.x - 5, p.y - 4, 11, 9, () -> selectPort(id, out, index)));
        }
    }

    private void selectNode(NBTTagCompound node, boolean drag) {
        selected = node.getInteger("id");
        selectedEdge = 0;
        scroll = 0;
        selectionChanged.accept(node);
        if (drag && editable()) {
            dragNode = selected;
            dragX = anchorX = node.getInteger("x");
            dragY = anchorY = node.getInteger("y");
        }
    }

    private void selectPort(int id, boolean out, int index) {
        if (!editable()) return;
        if (out) {
            source = id;
            output = index;
        } else if (source != 0) {
            send.accept("link " + graph + " " + source + " " + output + " " + id + " " + index);
            source = 0;
        }
    }

    private void drawInspector(NBTTagCompound node, NBTTagCompound edge) {
        int x = 259, y = TOP + 6;
        if (edge != null) {
            text(tr("buffer"), x, y, 135, TEAL);
            text(materialName(edge), x, y + 13, 135, INK);
            text(edge.getLong("amount") + " / " + edge.getInteger("capacity"), x, y + 26, 135, MUTED);
            text(tr("water"), x, y + 39, 135, MUTED);
            return;
        }
        if (node == null) {
            text(!editable() ? tr("readOnly") : tr("select"), x, y, 135, TEAL);
            text(snapshot.getBoolean("paused") ? tr("paused") : tr("active"), x, y + 13, 135, MUTED);
            if (snapshot.hasKey("targetRate")) {
                text(tr("targetRate") + " " + rate(snapshot.getDouble("targetRate")), x, y + 29, 135, INK);
                text(tr("feasibleRate") + " " + rate(snapshot.getDouble("feasibleRate")), x, y + 42, 135, INK);
                text(tr("actualRate") + " " + rate(snapshot.getDouble("actualRate")), x, y + 55, 135, INK);
            }
            return;
        }
        text("#" + (node.getInteger("slot") + 1) + "  " + state(node), x, y, 135, TEAL);
        text(StatCollector.translateToLocal(node.getString("map")), x, y + 11, 135, MUTED);
        text(node.getString("estimate"), x, y + 22, 135, INK);
        y += 36;
        List<Runnable> actions = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for (String kind : new String[] { "in", "out", "tools", "discarded" }) {
            NBTTagList ports = node.getTagList(kind, 10);
            for (int i = 0; i < ports.tagCount(); i++) {
                NBTTagCompound tag = ports.getCompoundTagAt(i);
                boolean out = "out".equals(kind);
                boolean tool = "tools".equals(kind);
                String count = tool ? "" : tag.getInteger("quantity") + " ";
                boolean discarded = "discarded".equals(kind);
                String chance = out || discarded ? " · " + tag.getInteger("chance") / 100.0 + "%" : "";
                double expected = node.getDouble("ops") * tag.getInteger("quantity") * tag.getInteger("chance") / 10000;
                lines.add((out ? "→ " : tool ? "◇ " : discarded ? "× " : "← ") + count + materialName(tag) + chance);
                int index = i;
                actions.add(
                    out && editable() ? () -> send.accept("target " + graph + " " + node.getInteger("id") + " " + index)
                        : null);
                if (out) {
                    lines.add("    " + rate(expected) + (editable() ? " · " + tr("setTarget") : ""));
                    actions.add(actions.get(actions.size() - 1));
                }
            }
        }
        int first = Math.min(scroll, Math.max(0, lines.size() - 1));
        for (int i = first; i < lines.size() && y + 9 < BOTTOM; i++, y += 11) {
            boolean active = actions.get(i) != null;
            text(lines.get(i), x, y, 136, active ? TEAL : INK);
            if (active) hits.add(new Hit(x, y, 136, 10, actions.get(i)));
        }
    }

    private static String rate(double number) {
        return String.format(Locale.ROOT, "%.2f/s", number);
    }

    private void drawPicker() {
        NBTTagCompound choices = patterns.get();
        if (choices == null) return;
        NBTTagList slots = choices.getTagList("slots", 10);
        hits.clear();
        GuiDraw.drawRect(LEFT, TOP, 397, BOTTOM - TOP, 0xe9121b2a);
        GuiDraw.drawRoundedRect(69, 50, 266, 124, 6, 8, 0xff25354b);
        text(tr("picker"), 79, 59, 244, TEAL);
        for (int i = pickerScroll, y = 73; i < slots.tagCount() && y < 165; i++, y += 14) {
            NBTTagCompound tag = slots.getCompoundTagAt(i);
            int slot = tag.getInteger("slot");
            GuiDraw.drawRect(78, y, 248, 13, (i & 1) == 0 ? 0xff2d4058 : 0xff26384e);
            text("#" + (slot + 1) + "  " + tag.getString("name"), 83, y + 2, 237, INK);
            hits.add(new Hit(78, y, 248, 13, () -> {
                int n = snapshot.getTagList("nodes", 10)
                    .tagCount();
                send.accept(
                    "add " + graph
                        + " "
                        + slot
                        + " "
                        + (int) ((12 + n % 2 * 110 - panX) / zoom)
                        + " "
                        + (int) ((10 + n / 2 * 53 - panY) / zoom));
                picker = false;
            }));
        }
        if (slots.tagCount() == 0) text(tr("noPatterns"), 83, 77, 237, MUTED);
    }

    private static String state(NBTTagCompound node) {
        String state = node.getString("state");
        return switch (state) {
            case "idle", "materials", "catalyst", "buffer_full", "pattern_changed", "output_wait" -> tr(state);
            default -> state;
        };
    }

    private static String materialName(NBTTagCompound tag) {
        Material material = Material.load(tag);
        return material == null ? "" : material.name();
    }

    private static void text(String label, int x, int y, int width, int color) {
        var font = Minecraft.getMinecraft().fontRenderer;
        String text = font.trimStringToWidth(label, Math.max(0, width));
        font.drawString(text, x, y, color);
    }

    private boolean editable() {
        return snapshot.getBoolean("edit");
    }

    void togglePicker() {
        if (editable() && graph != 0) {
            picker = !picker;
            pickerScroll = 0;
        }
    }

    void cycle(int delta) {
        NBTTagList graphs = snapshot.getTagList("list", 10);
        if (graphs.tagCount() < 2) return;
        for (int i = 0; i < graphs.tagCount(); i++) if (graphs.getCompoundTagAt(i)
            .getInteger("id") == graph) {
                int index = Math.floorMod(i + delta, graphs.tagCount());
                send.accept(
                    "view " + graphs.getCompoundTagAt(index)
                        .getInteger("id"));
                return;
            }
    }

    void graphEdit(String action) {
        if (graph != 0 && (editable() || "pause".equals(action))) send.accept(action + " " + graph);
    }

    void deleteSelection() {
        if (!editable()) return;
        if (selectedEdge != 0) send.accept("unlink " + graph + " " + selectedEdge);
        else if (selected != 0) send.accept("remove " + graph + " " + selected);
        selectedEdge = selected = 0;
    }

    void apply(String parallel, String oc, String power, String water) {
        if (!editable()) return;
        if (selectedEdge != 0) send.accept("water " + graph + " " + selectedEdge + " " + water.trim());
        else if (selected != 0) send.accept(
            "settings " + graph + " " + selected + " " + parallel.trim() + " " + oc.trim() + " " + power.trim());
    }

    private static boolean insideCanvas(int x, int y) {
        return x >= LEFT && x < RIGHT && y >= TOP && y < BOTTOM;
    }

    private int mouseX() {
        return getContext().getAbsMouseX() - getArea().x();
    }

    private int mouseY() {
        return getContext().getAbsMouseY() - getArea().y();
    }

    @Override
    public Result onMousePressed(int button) {
        int x = mouseX(), y = mouseY();
        if (y < TOP) return Result.IGNORE;
        lastX = x;
        lastY = y;
        dragged = false;
        if (button == 0) for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.contains(x, y)) {
                hit.action.run();
                return Result.SUCCESS;
            }
        }
        if (picker) {
            picker = false;
            return Result.SUCCESS;
        }
        if (insideCanvas(x, y) && (button == 0 || button == 2)) {
            if (button == 0) selected = selectedEdge = source = 0;
            panning = true;
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    @Override
    public void onMouseDrag(int button, long time) {
        int x = mouseX(), y = mouseY();
        if (dragNode != 0 && button == 0) {
            dragX = anchorX + (int) ((x - lastX) / zoom);
            dragY = anchorY + (int) ((y - lastY) / zoom);
            dragged = true;
        } else if (source != 0 && button == 0) {
            dragged = true;
        } else if (panning) {
            panX += x - lastX;
            panY += y - lastY;
            lastX = x;
            lastY = y;
        }
    }

    @Override
    public boolean onMouseRelease(int button) {
        if (dragNode != 0 && dragged) send.accept("move " + graph + " " + dragNode + " " + dragX + " " + dragY);
        if (button == 0 && source != 0 && dragged) for (int i = hits.size() - 1; i >= 0; i--) if (hits.get(i)
            .contains(mouseX(), mouseY())) {
                hits.get(i).action.run();
                break;
            }
        dragNode = 0;
        panning = false;
        return false;
    }

    @Override
    public boolean onMouseScroll(UpOrDown direction, int amount) {
        int delta = direction == UpOrDown.UP ? -1 : 1;
        if (picker) {
            NBTTagCompound choices = patterns.get();
            int size = choices == null ? 0
                : choices.getTagList("slots", 10)
                    .tagCount();
            pickerScroll = Math.max(0, Math.min(Math.max(0, size - 7), pickerScroll + delta));
        } else if (mouseX() >= 253) {
            scroll = Math.max(0, Math.min(100, scroll + delta));
        } else if (insideCanvas(mouseX(), mouseY())) {
            double changed = Math.max(0.65, Math.min(1.6, zoom - delta * 0.1));
            panX = (int) (mouseX() - LEFT - (mouseX() - LEFT - panX) * changed / zoom);
            panY = (int) (mouseY() - TOP - (mouseY() - TOP - panY) * changed / zoom);
            zoom = changed;
        } else return false;
        return true;
    }
}
