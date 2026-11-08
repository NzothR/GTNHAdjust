package com.futureresourceprotocol.device;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

import codechicken.nei.ItemList;
import codechicken.nei.ItemPanels;
import codechicken.nei.ItemSorter;
import codechicken.nei.PanelWidget;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerObjectHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TerminalGui extends GuiContainer implements INEIGuiHandler, IContainerTooltipHandler {

    static {
        GuiContainerManager.addObjectHandler(new IContainerObjectHandler() {

            @Override
            public void guiTick(GuiContainer gui) {}

            @Override
            public void refresh(GuiContainer gui) {}

            @Override
            public void load(GuiContainer gui) {}

            @Override
            public ItemStack getStackUnderMouse(GuiContainer gui, int mouseX, int mouseY) {
                if (!(gui instanceof TerminalGui)) return null;
                ItemStack stack = ((TerminalGui) gui).at(mouseX, mouseY);
                return stack == null ? null : stack.copy();
            }

            @Override
            public boolean objectUnderMouse(GuiContainer gui, int mouseX, int mouseY) {
                return false;
            }

            @Override
            public boolean shouldShowTooltip(GuiContainer gui) {
                return true;
            }
        });
    }

    private static final ResourceLocation AE_TEXTURE = new ResourceLocation(
        "appliedenergistics2",
        "textures/guis/terminal.png");
    private final TerminalContainer container;
    private final InventoryPlayer inventory;
    private GuiTextField search;
    private int scroll;
    private boolean draggingScrollbar;
    private List<ItemStack> neiOrder;
    private List<ItemStack> sortedMarks;
    private List<ItemStack> worldMarks = new ArrayList<>();
    private List<ItemStack> receivingMarks;
    private String marksSession;
    private String receivingSession;
    private long receivingRevision;
    private int receivingNextPart;
    private int receivingParts;
    private int worldMarkCount;
    private boolean marksValid;
    private final Map<Item, Map<Integer, Integer>> neiRanks = new IdentityHashMap<>();
    private String debtSession;
    private boolean debtsValid;
    private Map<String, long[]> debts;

    public TerminalGui(TerminalContainer container, InventoryPlayer inventory) {
        super(container);
        this.container = container;
        this.inventory = inventory;
        xSize = 195;
        ySize = 223;
    }

    @Override
    public void initGui() {
        super.initGui();
        search = new GuiTextField(fontRendererObj, guiLeft + 81, guiTop + 5, 87, 10);
        search.setMaxStringLength(80);
        search.setEnableBackgroundDrawing(false);
        search.setTextColor(0x404040);
    }

    private List<ItemStack> visible() {
        List<ItemStack> filtered = new ArrayList<>();
        String term = search == null ? ""
            : search.getText()
                .toLowerCase(Locale.ROOT)
                .trim();
        for (ItemStack item : sortedMarks()) {
            String key = WorldProtocol.key(item);
            if (term.isEmpty() || itemName(item).toLowerCase(Locale.ROOT)
                .contains(term)
                || (key != null && key.toLowerCase(Locale.ROOT)
                    .contains(term)))
                filtered.add(item);
        }
        return filtered;
    }

    private static String itemName(ItemStack item) {
        String name = EnumChatFormatting.getTextWithoutFormattingCodes(item.getDisplayName());
        return name == null ? "" : name;
    }

    private boolean overSearch(int x, int y) {
        return search != null && x >= search.xPosition
            && x < search.xPosition + search.width
            && y >= search.yPosition
            && y < search.yPosition + search.height;
    }

    private void searchFor(ItemStack item) {
        search.setText(itemName(item));
        search.setFocused(true);
        scroll = 0;
        FutureResourceProtocol
            .debug("search filled from item key={} name={}", WorldProtocol.key(item), search.getText());
    }

    private List<ItemStack> sortedMarks() {
        ItemStack terminal = inventory.getStackInSlot(container.heldSlot);
        if (terminal == null || !terminal.hasTagCompound()
            || !TerminalContainer.sameSession(
                marksSession,
                terminal.getTagCompound()
                    .getString(TerminalContainer.SESSION_TAG)))
            return java.util.Collections.emptyList();
        List<ItemStack> currentOrder = ItemList.items;
        if (currentOrder != neiOrder) {
            neiOrder = currentOrder;
            neiRanks.clear();
            for (int i = 0; i < currentOrder.size(); i++) {
                ItemStack item = currentOrder.get(i);
                if (item != null && item.getItem() != null)
                    neiRanks.computeIfAbsent(item.getItem(), ignored -> new HashMap<>())
                        .putIfAbsent(item.getItemDamage(), i);
            }
            sortedMarks = null;
        }
        if (sortedMarks == null) {
            sortedMarks = new ArrayList<>(worldMarks);
            sortedMarks.sort((a, b) -> {
                int byRank = Integer.compare(rank(a), rank(b));
                return byRank != 0 ? byRank : ItemSorter.instance.compare(a, b);
            });
        }
        return sortedMarks;
    }

    void receiveMarks(MarkSyncMessage message) {
        if (message.part == 0) {
            receivingMarks = new ArrayList<>();
            receivingSession = message.session;
            receivingRevision = message.revision;
            receivingParts = message.parts;
            receivingNextPart = 0;
        }
        if (receivingMarks == null || !message.session.equals(receivingSession)
            || message.revision != receivingRevision
            || message.parts != receivingParts
            || message.part != receivingNextPart) return;
        receivingMarks.addAll(message.items);
        receivingNextPart++;
        if (receivingNextPart == receivingParts) {
            worldMarks = receivingMarks;
            receivingMarks = null;
            marksSession = message.session;
            marksValid = message.valid;
            worldMarkCount = message.total;
            sortedMarks = null;
        }
    }

    private int rank(ItemStack item) {
        Map<Integer, Integer> variants = neiRanks.get(item.getItem());
        return variants == null ? Integer.MAX_VALUE : variants.getOrDefault(item.getItemDamage(), Integer.MAX_VALUE);
    }

    private int cell(int x, int y) {
        int rx = x - guiLeft - 9, ry = y - guiTop - 18;
        return rx >= 0 && rx < 162 && ry >= 0 && ry < 108 ? (ry / 18) * 9 + rx / 18 : -1;
    }

    private ItemStack at(int x, int y) {
        int cell = cell(x, y);
        List<ItemStack> list = visible();
        int index = scroll * 9 + cell;
        return cell < 0 || index >= list.size() ? null : list.get(index);
    }

    void receiveDebts(DebtSyncMessage message) {
        if (container.marking) return;
        debtSession = message.session;
        debtsValid = message.valid;
        debts = message.debts;
    }

    private BigInteger debt(ItemStack item) {
        ItemStack terminal = inventory.getStackInSlot(container.heldSlot);
        if (!debtsValid || debts == null
            || terminal == null
            || !terminal.hasTagCompound()
            || !TerminalContainer.sameSession(
                debtSession,
                terminal.getTagCompound()
                    .getString(TerminalContainer.SESSION_TAG)))
            return null;
        long[] values = debts.get(WorldProtocol.key(item));
        return values == null ? null : DebtDisplay.total(values[0], values[1]);
    }

    private void send(int action, ItemStack item) {
        if (item != null && !TerminalData.safeTag(item)) {
            mc.thePlayer.addChatMessage(new ChatComponentTranslation("frp.chat.item_too_large"));
        } else if (item != null)
            FutureResourceProtocol.NETWORK.sendToServer(new TerminalMessage(action, container.windowId, item.copy()));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager()
            .bindTexture(AE_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, 195, 18);
        for (int row = 0; row < 6; row++) drawTexturedModalRect(guiLeft, guiTop + 18 + row * 18, 0, 18, 195, 18);
        drawTexturedModalRect(guiLeft, guiTop + 124, 0, 70, 195, 99);
        drawRect(guiLeft + 175, guiTop + 18, guiLeft + 186, guiTop + 126, 0xff696969);
        int maximum = Math.max(0, (visible().size() + 8) / 9 - 6);
        int thumbY = guiTop + 19 + (maximum == 0 ? 0 : scroll * 91 / maximum);
        drawRect(guiLeft + 176, thumbY, guiLeft + 185, thumbY + 15, 0xffcccccc);
        search.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format(container.marking ? "frp.gui.mark" : "frp.gui.advance"), 8, 6, 0x404040);
        List<ItemStack> list = visible();
        int maxScroll = Math.max(0, (list.size() + 8) / 9 - 6);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        RenderHelper.enableGUIStandardItemLighting();
        for (int cell = 0; cell < 54; cell++) {
            int index = scroll * 9 + cell;
            if (index >= list.size()) break;
            ItemStack item = list.get(index);
            itemRender.renderItemAndEffectIntoGUI(
                fontRendererObj,
                mc.getTextureManager(),
                item,
                9 + cell % 9 * 18,
                18 + cell / 9 * 18);
        }
        RenderHelper.disableStandardItemLighting();
        if (!container.marking) {
            GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            for (int cell = 0; cell < 54 && scroll * 9 + cell < list.size(); cell++) {
                int x = 9 + cell % 9 * 18;
                int y = 18 + cell / 9 * 18;
                BigInteger debt = debt(list.get(scroll * 9 + cell));
                String count = debt == null ? "--" : DebtDisplay.format(debt);
                float scale = Math.min(0.65F, 16F / Math.max(1, fontRendererObj.getStringWidth(count)));
                GL11.glPushMatrix();
                GL11.glTranslatef(0F, 0F, 300F);
                GL11.glScalef(scale, scale, 1F);
                fontRendererObj.drawStringWithShadow(
                    count,
                    (int) ((x + 17 - fontRendererObj.getStringWidth(count) * scale) / scale),
                    (int) ((y + 16 - 7 * scale) / scale),
                    0xffffff);
                GL11.glPopMatrix();
            }
            GL11.glPopAttrib();
        }
        ItemStack terminal = inventory.getStackInSlot(container.heldSlot);
        boolean synchronizedMarks = marksValid && terminal != null
            && terminal.hasTagCompound()
            && TerminalContainer.sameSession(
                marksSession,
                terminal.getTagCompound()
                    .getString(TerminalContainer.SESSION_TAG));
        fontRendererObj.drawString(
            (synchronizedMarks ? Integer.toString(worldMarkCount) : "--") + "/" + TerminalData.MAX,
            8,
            128,
            0x404040);
        // NEI renders its panels immediately after this foreground layer.
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    @Override
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack item, int mouseX, int mouseY,
        List<String> tooltip) {
        if (gui == this && !container.marking && item != null && at(mouseX, mouseY) != null) {
            BigInteger amount = debt(item);
            String exact = amount == null ? "--" : amount.signum() == 0 ? "0" : "-" + amount;
            tooltip.add(I18n.format("frp.tooltip.global_debt", exact));
        }
        return tooltip;
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        search.mouseClicked(x, y, button);
        if (button == 0 && overSearch(x, y) && inventory.getItemStack() != null) {
            searchFor(inventory.getItemStack());
            return;
        }
        if (button == 1 && overSearch(x, y)) {
            search.setText("");
            scroll = 0;
            return;
        }
        if (button == 0 && x >= guiLeft + 175 && x < guiLeft + 186 && y >= guiTop + 18 && y < guiTop + 126) {
            draggingScrollbar = true;
            updateScrollbar(y);
            return;
        }
        if (cell(x, y) >= 0) {
            ItemStack item = at(x, y);
            if (container.marking) {
                if (button == 1 && item != null) send(1, item);
                else if (button == 0 && inventory.getItemStack() != null) send(0, inventory.getItemStack());
            } else if (button == 0 && inventory.getItemStack() != null && !isShiftKeyDown() && !isCtrlKeyDown()) {
                if (FutureResourceProtocol.portableRepaymentEnabled)
                    send(AdvanceInteraction.REPAY_CURSOR, inventory.getItemStack());
            } else if (item != null) {
                int action = AdvanceInteraction.action(button, isShiftKeyDown(), isCtrlKeyDown());
                if (action >= 0) send(action, item);
            }
            return;
        }
        super.mouseClicked(x, y, button);
    }

    private void updateScrollbar(int y) {
        int maximum = Math.max(0, (visible().size() + 8) / 9 - 6);
        scroll = Math.max(0, Math.min(maximum, (y - guiTop - 26) * maximum / 91));
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (draggingScrollbar) updateScrollbar(y);
        else super.mouseClickMove(x, y, button, elapsed);
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        if (button == 0) draggingScrollbar = false;
        super.mouseMovedOrUp(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (search.textboxKeyTyped(character, key)) {
            scroll = 0;
            return;
        }
        super.keyTyped(character, key);
    }

    @Override
    public void handleMouseInput() {
        int button = Mouse.getEventButton();
        boolean release = button == 0 && !Mouse.getEventButtonState();
        int x = Mouse.getEventX() * width / mc.displayWidth;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        super.handleMouseInput();
        if (release && (overSearch(x, y) || container.marking && cell(x, y) >= 0)) {
            if (!dropFromPanel(ItemPanels.itemPanel, x, y)) dropFromPanel(ItemPanels.bookmarkPanel, x, y);
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll += wheel < 0 ? 1 : -1;
    }

    private boolean dropFromPanel(PanelWidget panel, int x, int y) {
        if (panel.draggedStack == null) return false;
        ItemStack stack = panel.draggedStack;
        if (!handleDragNDrop(this, x, y, stack, 0)) return false;
        panel.draggedStack = null;
        return true;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int x, int y, ItemStack stack, int button) {
        if (gui == this && stack != null && button == 0 && overSearch(x, y)) {
            searchFor(stack);
            stack.stackSize = 0;
            return true;
        }
        if (gui == this && container.marking && stack != null && cell(x, y) >= 0 && button == 0) {
            FutureResourceProtocol.debug("NEI mark drop cell={} item={}", cell(x, y), WorldProtocol.key(stack));
            send(0, stack);
            stack.stackSize = 0;
            return true;
        }
        return false;
    }
}
