package com.futureresourceprotocol.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

import codechicken.nei.ItemPanels;
import codechicken.nei.PanelWidget;
import codechicken.nei.api.INEIGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TerminalGui extends GuiContainer implements INEIGuiHandler {

    private static final ResourceLocation AE_TEXTURE = new ResourceLocation(
        "appliedenergistics2",
        "textures/guis/terminal.png");
    private final TerminalContainer container;
    private final InventoryPlayer inventory;
    private GuiTextField search;
    private int scroll;
    private boolean draggingScrollbar;

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
        for (ItemStack item : TerminalData.read(inventory.getStackInSlot(container.heldSlot))) {
            String key = WorldProtocol.key(item);
            if (term.isEmpty() || item.getDisplayName()
                .toLowerCase(Locale.ROOT)
                .contains(term)
                || (key != null && key.toLowerCase(Locale.ROOT)
                    .contains(term)))
                filtered.add(item);
        }
        return filtered;
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
        int total = TerminalData.read(inventory.getStackInSlot(container.heldSlot))
            .size();
        fontRendererObj.drawString(total + "/" + TerminalData.MAX, 8, 128, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        super.drawScreen(mouseX, mouseY, partial);
        ItemStack item = at(mouseX, mouseY);
        if (item != null) renderToolTip(item, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        search.mouseClicked(x, y, button);
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
            } else if (button == 0 && item != null) send(isShiftKeyDown() ? 3 : 2, item);
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
        if (release && container.marking && cell(x, y) >= 0) {
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
        if (gui == this && container.marking && stack != null && cell(x, y) >= 0 && button == 0) {
            FutureResourceProtocol.debug("NEI mark drop cell={} item={}", cell(x, y), WorldProtocol.key(stack));
            send(0, stack);
            stack.stackSize = 0;
            return true;
        }
        return false;
    }
}
