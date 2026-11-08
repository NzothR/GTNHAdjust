package com.futureresourceprotocol.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

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
        xSize = 194;
        ySize = 236;
    }

    @Override
    public void initGui() {
        super.initGui();
        search = new GuiTextField(fontRendererObj, guiLeft + 88, guiTop + 7, 98, 12);
        search.setMaxStringLength(80);
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
        int rx = x - guiLeft - 8, ry = y - guiTop - 17;
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
            mc.thePlayer.addChatMessage(new ChatComponentText("[FRP] Item NBT is too large to synchronize safely."));
        } else if (item != null)
            FutureResourceProtocol.NETWORK.sendToServer(new TerminalMessage(action, container.windowId, item.copy()));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager()
            .bindTexture(AE_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, 194, 17);
        drawTexturedModalRect(guiLeft, guiTop + 17, 0, 17, 194, 54);
        drawTexturedModalRect(guiLeft, guiTop + 71, 0, 17, 194, 54);
        drawTexturedModalRect(guiLeft, guiTop + 125, 0, 70, 194, 12);
        drawTexturedModalRect(guiLeft, guiTop + 137, 0, 70, 176, 96);
        drawRect(guiLeft + 174, guiTop + 17, guiLeft + 185, guiTop + 125, 0xff696969);
        int maximum = Math.max(0, (visible().size() + 8) / 9 - 6);
        int thumbY = guiTop + 18 + (maximum == 0 ? 0 : scroll * 91 / maximum);
        drawRect(guiLeft + 175, thumbY, guiLeft + 184, thumbY + 15, 0xffcccccc);
        search.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(container.marking ? "FRP - Mark" : "FRP - Advance", 8, 7, 0x404040);
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
                8 + cell % 9 * 18,
                17 + cell / 9 * 18);
        }
        RenderHelper.disableStandardItemLighting();
        fontRendererObj.drawString(list.size() + "/" + TerminalData.MAX, 8, 139, 0x404040);
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
        if (button == 0 && x >= guiLeft + 174 && x < guiLeft + 185 && y >= guiTop + 17 && y < guiTop + 125) {
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
        scroll = Math.max(0, Math.min(maximum, (y - guiTop - 25) * maximum / 91));
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
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll += wheel < 0 ? 1 : -1;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int x, int y, ItemStack stack, int button) {
        if (gui == this && container.marking && cell(x, y) >= 0 && button == 0) {
            send(0, stack);
            return true;
        }
        return false;
    }
}
