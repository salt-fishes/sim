package com.trhsy.sim.gui.block;

import com.trhsy.sim.gui.GuiLayout;
import com.trhsy.sim.gui.GuiStyle;
import com.trhsy.sim.gui.npc.GuiEmployees;
import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.server.*;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.NpcIdentity;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.Building;
import com.trhsy.sim.npcCode.economy.Economy;
import com.trhsy.sim.npcCode.logistics.LogisticsSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName GuiBlockControllerBlock
 * @Description 控制箱（原版像素风）
 * @Author TRHSY
 * @Date 2022/11/7 11:49
 **/
public class GuiBlockControllerBlock extends GuiScreen {
    private static final int MAX_EMPLOYEE_ROWS = 5;
    /**
     * 是否雇佣
     **/
    public boolean hasEmployee = false;
    /**
     * npc信息
     **/
    public NpcIdentity employee;
    List<NpcData> occupants;
    /**
     * 可雇佣的人名
     **/
    public NpcIdentity[] hireableFolkNames = new NpcIdentity[1000];
    /**
     * 工作名称
     **/
    List<String> jobNames = new CopyOnWriteArrayList<String>();
    /**
     * 建筑id
     **/
    public String buildingId;
    /**
     * 建筑名
     **/
    public String buildingName = "buildingName";
    /**
     * 工作
     **/
    public String jobName = "null";
    /**
     * 建筑类型
     **/
    private String buildingType;
    /**
     * 作者
     **/
    private String author;
    /**
     * 选择的NPC
     **/
    GuiButton selectedEmployee;
    /**
     * 当前页
     **/
    public int currentPage = 0;
    /**
     * 上一页
     **/
    public int previousPage = 1;
    /**
     * 位置
     **/
    public V3 pos;
    /**
     * 是否是住宅
     **/
    public boolean isResidential;
    /****/
    long fingBodge = 0L;
    /**
     * 建筑描述
     **/
    public String desc;
    public int fs_y;
    /** 物流设置页的翻页下标。 */
    private int logisticsPage = 0;
    /** 当前页渲染出来的开关项，供按钮回调定位。 */
    private final List<LogisticsEntry> logisticsEntries = new ArrayList<>();

    private static class LogisticsEntry {
        String requesterKey;
        String itemKey;
        String label;
        boolean enabled;
    }

    public GuiBlockControllerBlock(V3 p, String bId, String bName, String jName, String bType, String author, String desc) {
        try {
            this.pos = p;
            this.hasEmployee = false;
            this.occupants = null;
            this.buildingId = bId;
            ModSimLoader.log.info("控制箱ID:" + bId);
            this.buildingName = bName;
            this.jobName = jName;
            this.buildingType = bType;
            this.author = author;
            this.desc = desc;
            this.getHireableFolkNames();
            this.populateJobList();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiBlockControllerBlock出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    public GuiBlockControllerBlock(V3 p, List<NpcData> occupant, String bId, String bName, String jName, String bType, String author, String desc) {
        try {
            this.pos = p;
            this.occupants = occupant;
            this.hasEmployee = true;
            ModSimLoader.log.info("控制箱ID:" + bId);
            this.buildingId = bId;
            this.buildingName = bName;
            this.jobName = jName;
            this.buildingType = bType;
            this.author = author;
            this.desc = desc;
            this.getHireableFolkNames();
            this.populateJobList();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiBlockControllerBlock1出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    public GuiBlockControllerBlock(V3 p, List<NpcData> occupants, String bId, Boolean isResidential, String bName, String jName, String bType, String author, String desc) {
        try {
            this.pos = p;
            this.occupants = occupants;
            ModSimLoader.log.info("控制箱ID:" + bId);
            this.buildingId = bId;
            this.buildingName = bName;
            this.jobName = jName;
            this.buildingType = bType;
            this.author = author;
            this.desc = desc;
            this.isResidential = isResidential;
            this.populateJobList();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiBlockControllerBlock2出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 填充岗位列表。旧实现只写死屠夫/面包师，这里使用当前建筑岗位。
     */
    public void populateJobList() {
        this.jobNames.clear();
        if (this.isValidJob()) {
            this.jobNames.add(this.jobName);
        }
    }

    private boolean isValidJob() {
        return this.jobName != null && !this.jobName.isEmpty() && !"null".equalsIgnoreCase(this.jobName);
    }

    private boolean isCourierJob() {
        if (!this.isValidJob()) {
            return false;
        }
        String courier = new TextComponentTranslation("container.sim.Vocation10", new Object[0]).getUnformattedText();
        return this.jobName.equals(courier);
    }

    private boolean isMissingBuilding() {
        return this.buildingId == null || this.buildingId.isEmpty();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.showPage();
    }

    private int panelW() {
        return Math.min(400, this.width - 16);
    }

    private int panelH() {
        return Math.min(240, this.height - 16);
    }

    private int left() {
        return GuiLayout.centerX(this.width, panelW());
    }

    private int top() {
        return GuiLayout.centerY(this.height, panelH());
    }

    private int cardX() {
        int leftW = Math.max(150, (panelW() - 16) * 46 / 100);
        return left() + 8 + leftW + 6;
    }

    private int cardW() {
        return left() + panelW() - 8 - cardX();
    }

    private int cardY() {
        return top() + 48;
    }

    private int cardH() {
        return panelH() - 86;
    }

    private int employeeRowY(int index) {
        return cardY() + 48 + index * 20;
    }

    private int maxEmployeeRows() {
        return Math.max(1, Math.min(MAX_EMPLOYEE_ROWS, (cardH() - 54) / 20));
    }

    private boolean canHire() {
        if (!this.isValidJob()) {
            return false;
        }
        int count = this.occupants == null ? 0 : this.occupants.size();
        String soldier = new TextComponentTranslation("container.sim.Vocation7", new Object[0]).getUnformattedText();
        if (this.jobName.equals(soldier)) {
            return count < 5;
        }
        return count == 0;
    }

    public void showPage() {
        try {
            this.buttonList.clear();
            int px = left();
            int py = top();
            int pw = panelW();
            int ph = panelH();
            //完成
            this.buttonList.add(new GuiButton(0, px + pw - 60, py + 4, 52, 18, new TextComponentTranslation("container.sim.sim_gui_BC_Done", new Object[0]).getUnformattedText()));

            if (this.currentPage == 1) {
                //取消
                this.buttonList.add(new GuiButton(1010, px + 8, py + ph - 26, 90, 20, new TextComponentTranslation("container.sim.sim_gui_player_to_Cancel", new Object[0]).getUnformattedText()));
                //好的
                this.buttonList.add(new GuiButton(1002, px + pw - 98, py + ph - 26, 90, 20, new TextComponentTranslation("container.sim.gui_btn_name_OK", new Object[0]).getUnformattedText()));
                this.addHireButtons(px, py, pw, ph);
                return;
            }

            if (this.currentPage == 2) {
                this.addLogisticsButtons(px, py, pw, ph);
                return;
            }

            if (this.isMissingBuilding()) {
                return;
            }

            int gap = 4;
            int buttonCount = this.isCourierJob() ? 5 : 4;
            int btnW = (pw - 16 - gap * (buttonCount - 1)) / buttonCount;
            int btnY = py + ph - 26;
            int index = 0;
            if (this.isValidJob() && this.canHire()) {
                this.buttonList.add(new GuiButton(1, px + 8 + index * (btnW + gap), btnY, btnW, 20, new TextComponentTranslation("container.sim.Hire0", new Object[0]).getUnformattedText()));
                index++;
            }
            this.buttonList.add(new GuiButton(21, px + 8 + index * (btnW + gap), btnY, btnW, 20, new TextComponentTranslation("container.sim.sim_gui_BC_Show_Employees", new Object[0]).getUnformattedText()));
            index++;
            this.buttonList.add(new GuiButton(1000, px + 8 + index * (btnW + gap), btnY, btnW, 20, new TextComponentTranslation("container.sim.Demolish", new Object[0]).getUnformattedText()));
            index++;
            this.buttonList.add(new GuiButton(1001, px + 8 + index * (btnW + gap), btnY, btnW, 20, new TextComponentTranslation("container.sim.Rotate_Stairs", new Object[0]).getUnformattedText()));
            index++;
            if (this.isCourierJob()) {
                this.buttonList.add(new GuiButton(30, px + 8 + index * (btnW + gap), btnY, btnW, 20,
                        new TextComponentTranslation("container.sim.gui.logistics", new Object[0]).getUnformattedText()));
            }

            this.addFireButtons();
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("showPage出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /** 物流设置页：总开关 + 逐物品开关 + 翻页。 */
    private void addLogisticsButtons(int px, int py, int pw, int ph) {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, px + pw - 60, py + 4, 52, 18,
                new TextComponentTranslation("container.sim.sim_gui_BC_Done", new Object[0]).getUnformattedText()));
        this.buttonList.add(new GuiButton(1020, px + 8, py + ph - 26, 80, 20,
                new TextComponentTranslation("container.sim.gui_btn_back", new Object[0]).getUnformattedText()));
        this.buttonList.add(new GuiButton(1021, px + 92, py + ph - 26, 120, 20, this.autoDispatchLabel()));
        this.buttonList.add(new GuiButton(1022, px + pw - 108, py + ph - 26, 48, 20, "<"));
        this.buttonList.add(new GuiButton(1023, px + pw - 56, py + ph - 26, 48, 20, ">"));

        this.logisticsEntries.clear();
        LogisticsSnapshot snapshot = ModSimClientLoader.logisticsSnapshot;
        if (snapshot == null) {
            return;
        }
        List<LogisticsEntry> all = new ArrayList<>();
        for (LogisticsSnapshot.RequesterView requester : snapshot.requesters) {
            if (requester == null) {
                continue;
            }
            for (LogisticsSnapshot.ItemView item : requester.items) {
                if (item == null) {
                    continue;
                }
                LogisticsEntry entry = new LogisticsEntry();
                entry.requesterKey = requester.key;
                entry.itemKey = item.key;
                entry.enabled = item.enabled;
                String itemName = item.display == null || item.display.isEmpty()
                        ? this.itemDisplayName(item.key) : item.display;
                String requesterName = requester.label == null || requester.label.isEmpty()
                        ? "?" : requester.label;
                entry.label = requesterName + "·" + itemName;
                all.add(entry);
            }
        }
        int pageSize = 12;
        int maxPage = Math.max(0, (all.size() - 1) / pageSize);
        if (this.logisticsPage > maxPage) {
            this.logisticsPage = maxPage;
        }
        if (this.logisticsPage < 0) {
            this.logisticsPage = 0;
        }
        this.buttonList.get(3).enabled = this.logisticsPage > 0;
        this.buttonList.get(4).enabled = this.logisticsPage < maxPage;

        int start = this.logisticsPage * pageSize;
        int end = Math.min(all.size(), start + pageSize);
        int colW = Math.max(120, (pw - 24) / 2);
        int rowH = 20;
        int startY = py + 80;
        for (int i = start; i < end; i++) {
            LogisticsEntry entry = all.get(i);
            this.logisticsEntries.add(entry);
            int local = this.logisticsEntries.size() - 1;
            int col = local % 2;
            int row = local / 2;
            int x = px + 8 + col * (colW + 8);
            int y = startY + row * rowH;
            String text = (entry.enabled ? "[+] " : "[ ] ") + entry.label;
            this.buttonList.add(new GuiButton(3000 + local, x, y, colW, 18,
                    GuiLayout.truncate(this.fontRenderer, text, colW - 6)));
        }
    }

    private String autoDispatchLabel() {
        LogisticsSnapshot snapshot = ModSimClientLoader.logisticsSnapshot;
        boolean enabled = snapshot != null && snapshot.autoDispatch;
        String key = enabled ? "container.sim.gui.logistics.on" : "container.sim.gui.logistics.off";
        return new TextComponentTranslation("container.sim.gui.logistics.auto", new Object[0]).getUnformattedText()
                + ": " + new TextComponentTranslation(key, new Object[0]).getUnformattedText();
    }

    private void drawLogisticsPage(int px, int py, int pw, int ph) {
        GuiStyle.text(this.fontRenderer, px + 8, py + 28,
                new TextComponentTranslation("container.sim.gui.logistics.title", new Object[0]).getUnformattedText(),
                GuiStyle.TEXT);
        LogisticsSnapshot snapshot = ModSimClientLoader.logisticsSnapshot;
        if (snapshot == null) {
            GuiStyle.text(this.fontRenderer, px + 8, py + 44,
                    new TextComponentTranslation("container.sim.gui.logistics.loading", new Object[0]).getUnformattedText(),
                    GuiStyle.TEXT_MUTED);
            return;
        }
        String courier = new TextComponentTranslation("container.sim.gui.logistics.courier", new Object[0]).getUnformattedText()
                + " " + snapshot.courierCount;
        String pending = new TextComponentTranslation("container.sim.gui.logistics.pending", new Object[0]).getUnformattedText()
                + " " + snapshot.pendingOrders;
        String fee = new TextComponentTranslation("container.sim.gui.logistics.fee", new Object[0]).getUnformattedText()
                + " " + String.format(Locale.ROOT, "%.3f", snapshot.feePerItem) + "/"
                + new TextComponentTranslation("container.sim.gui.logistics.item", new Object[0]).getUnformattedText();
        String today = new TextComponentTranslation("container.sim.gui.logistics.today", new Object[0]).getUnformattedText()
                + " " + snapshot.todayDelivered + " "
                + new TextComponentTranslation("container.sim.gui.logistics.item", new Object[0]).getUnformattedText()
                + " / " + this.formatMoney(snapshot.todayFees);
        GuiStyle.text(this.fontRenderer, px + 8, py + 42, GuiLayout.truncate(this.fontRenderer,
                courier + "    " + pending, pw - 16), GuiStyle.TEXT);
        GuiStyle.text(this.fontRenderer, px + 8, py + 54, GuiLayout.truncate(this.fontRenderer,
                fee + "    " + today, pw - 16), GuiStyle.TEXT_MUTED);
        if (snapshot.fundsLow) {
            GuiStyle.text(this.fontRenderer, px + 8, py + 66,
                    new TextComponentTranslation("container.sim.gui.logistics.funds_low", new Object[0]).getUnformattedText(),
                    GuiStyle.RED);
        }
        if (snapshot.requesters.isEmpty() || this.logisticsEntries.isEmpty()) {
            GuiStyle.text(this.fontRenderer, px + 8, py + 74,
                    new TextComponentTranslation("container.sim.gui.logistics.empty", new Object[0]).getUnformattedText(),
                    GuiStyle.TEXT_MUTED);
        }
        String hint = new TextComponentTranslation("container.sim.gui.logistics.hint", new Object[0]).getUnformattedText();
        GuiStyle.text(this.fontRenderer, px + 8, py + ph - 40,
                GuiLayout.truncate(this.fontRenderer, hint, pw - 16), GuiStyle.TEXT_MUTED);
    }

    /** 服务端快照到达后，如果正停在物流页就重绘。 */
    public void refreshLogisticsPage() {
        if (this.currentPage == 2) {
            this.showPage();
        }
    }

    private String itemDisplayName(String key) {
        if (key == null || key.isEmpty()) {
            return "?";
        }
        int at = key.lastIndexOf('@');
        String registry = at >= 0 ? key.substring(0, at) : key;
        int meta = 0;
        if (at >= 0) {
            try {
                meta = Integer.parseInt(key.substring(at + 1));
            } catch (NumberFormatException ignored) {
                meta = 0;
            }
        }
        net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(registry);
        if (item == null) {
            return registry;
        }
        return new net.minecraft.item.ItemStack(item, 1, meta).getDisplayName();
    }

    private void addHireButtons(int px, int py, int pw, int ph) {        try {
            List<NpcIdentity> unemployed = ModSimClientLoader.getUnemployedFolks();
            int btnW = Math.min(170, (pw - 24) / 2);
            int colGap = pw - 16 - btnW * 2;
            int startY = py + 48;
            int rowH = 22;
            int max = Math.min(unemployed.size(), 14);
            for (int i = 0; i < max; i++) {
                NpcIdentity folk = unemployed.get(i);
                int col = i % 2;
                int row = i / 2;
                int x = px + 8 + col * (btnW + colGap);
                int y = startY + row * rowH;
                this.buttonList.add(new GuiButton(100 + i, x, y, btnW, 20, folk.name));
                this.hireableFolkNames[100 + i] = folk;
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("addHireButtons出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private void addFireButtons() {
        if (this.isResidential || this.occupants == null) {
            return;
        }
        for (int i = 0; i < Math.min(this.occupants.size(), this.maxEmployeeRows()); i++) {
            if (this.occupants.get(i) == null) {
                continue;
            }
            this.buttonList.add(new GuiButton(200 + i, cardX() + cardW() - 46, employeeRowY(i) - 1, 42, 18, new TextComponentTranslation("container.sim.Fire", new Object[0]).getUnformattedText()));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int px = left();
        int py = top();
        int pw = panelW();
        int ph = panelH();
        GuiStyle.panel(px, py, pw, ph, GuiLayout.truncate(this.fontRenderer, this.buildingName, pw - 80), this.fontRenderer);

        if (this.currentPage == 1) {
            this.drawHirePage(px, py, pw, ph);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        if (this.currentPage == 2) {
            this.drawLogisticsPage(px, py, pw, ph);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        if (this.isMissingBuilding()) {
            String line1 = new TextComponentTranslation("container.sim.on_this_building", new Object[0]).getUnformattedText() + " (" + this.pos + ")";
            String line2 = new TextComponentTranslation("container.sim.this_Building", new Object[0]).getUnformattedText();
            GuiStyle.text(this.fontRenderer, px + 10, py + 48, GuiLayout.truncate(this.fontRenderer, line1, pw - 20), GuiStyle.RED);
            GuiStyle.text(this.fontRenderer, px + 10, py + 64, GuiLayout.truncate(this.fontRenderer, line2, pw - 20), GuiStyle.RED);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        String status = new TextComponentTranslation("container.sim.Active_Building", new Object[0]).getUnformattedText();
        String type = this.buildingType == null ? "" : this.buildingType;
        String subtitle = type + " · " + status;
        GuiStyle.text(this.fontRenderer, px + pw / 2 - this.fontRenderer.getStringWidth(subtitle) / 2, py + 26, subtitle, GuiStyle.TEXT_MUTED);

        int leftW = Math.max(150, (pw - 16) * 46 / 100);
        int lx = px + 8;
        int ly = py + 48;
        GuiStyle.labelValue(this.fontRenderer, lx, lx + 42, ly, new TextComponentTranslation("container.sim.sim_gui_BC5", new Object[0]).getUnformattedText(), this.author == null ? "" : this.author, GuiStyle.TEXT);
        GuiStyle.labelValue(this.fontRenderer, lx, lx + 42, ly + 14, new TextComponentTranslation("container.sim.gui.coords", new Object[0]).getUnformattedText(), this.pos == null ? "" : this.pos.toString(), GuiStyle.TEXT);

        GuiStyle.text(this.fontRenderer, lx, ly + 32, new TextComponentTranslation("container.sim.sim_desc", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        List<String> descLines = GuiLayout.wrap(this.fontRenderer, this.desc == null ? "" : this.desc, leftW - 4);
        int descY = ly + 46;
        int maxDescLines = Math.max(1, (cardY() + cardH() - descY - 34) / 11);
        for (int i = 0; i < descLines.size() && i < maxDescLines; i++) {
            GuiStyle.text(this.fontRenderer, lx, descY + i * 11, GuiLayout.truncate(this.fontRenderer, descLines.get(i), leftW - 4), GuiStyle.TEXT);
        }

        int count = this.occupants == null ? 0 : this.occupants.size();
        String countLabel = this.isResidential
                ? new TextComponentTranslation("container.sim.gui.block_controller_Occupants", new Object[0]).getUnformattedText()
                : new TextComponentTranslation("container.sim.employees", new Object[0]).getUnformattedText();
        GuiStyle.text(this.fontRenderer, lx, py + ph - 48, countLabel + " " + count, GuiStyle.TEXT_MUTED);
        if (this.isCourierJob()) {
            GuiStyle.text(this.fontRenderer, lx, py + ph - 62,
                    new TextComponentTranslation("container.sim.gui.auto_logistics", new Object[0]).getUnformattedText(),
                    GuiStyle.GREEN);
        }

        this.drawJobCard(px, py, pw, ph);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawJobCard(int px, int py, int pw, int ph) {
        int rx = cardX();
        int ry = cardY();
        int rw = cardW();
        int rh = cardH();
        GuiStyle.inset(rx, ry, rw, rh);
        String jobText = this.isValidJob() ? this.jobName : new TextComponentTranslation("container.sim.gui_Folk_unemployed", new Object[0]).getUnformattedText();
        GuiStyle.text(this.fontRenderer, rx + 6, ry + 4, new TextComponentTranslation("container.sim.gui.job", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        GuiStyle.text(this.fontRenderer, rx + 6, ry + 15, GuiLayout.truncate(this.fontRenderer, jobText, rw - 70), GuiStyle.TEXT);
        GuiStyle.text(this.fontRenderer, rx + rw - 62, ry + 4, new TextComponentTranslation("container.sim.gui.wage", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        String wageText = this.isValidJob() ? this.formatMoney(Economy.WAGE_PER_MINUTE) : "-";
        GuiStyle.text(this.fontRenderer, rx + rw - 62, ry + 15, wageText, this.isValidJob() ? GuiStyle.GREEN : GuiStyle.TEXT_MUTED);
        GuiStyle.divider(rx + 6, ry + 30, rw - 12);
        String listTitle = this.isResidential
                ? new TextComponentTranslation("container.sim.gui.block_controller_Occupants", new Object[0]).getUnformattedText()
                : new TextComponentTranslation("container.sim.employees", new Object[0]).getUnformattedText();
        GuiStyle.text(this.fontRenderer, rx + 6, ry + 35, listTitle, GuiStyle.TEXT_MUTED);
        if (this.occupants == null || this.occupants.isEmpty()) {
            GuiStyle.text(this.fontRenderer, rx + 8, ry + 52, new TextComponentTranslation("container.sim.gui.employees.none", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
            return;
        }
        for (int i = 0; i < Math.min(this.occupants.size(), this.maxEmployeeRows()); i++) {
            NpcData npc = this.occupants.get(i);
            if (npc == null) {
                continue;
            }
            int rowY = employeeRowY(i);
            GuiStyle.row(rx + 5, rowY - 2, rw - 10, 20, i % 2 == 1);
            GuiStyle.text(this.fontRenderer, rx + 7, rowY + 2, GuiLayout.truncate(this.fontRenderer, npc.getName(), rw - 100), GuiStyle.TEXT);
            GuiStyle.moneyRight(this.fontRenderer, rx + rw - 52, rowY + 2, this.occupantMoney(npc));
        }
        if (this.occupants.size() > this.maxEmployeeRows()) {
            GuiStyle.text(this.fontRenderer, rx + 8, employeeRowY(this.maxEmployeeRows()) + 2, "… +" + (this.occupants.size() - this.maxEmployeeRows()), GuiStyle.TEXT_MUTED);
        }
    }

    private void drawHirePage(int px, int py, int pw, int ph) {
        String hint = new TextComponentTranslation("container.sim.sim_gui_BC_unemployed", new Object[0]).getUnformattedText();
        GuiStyle.text(this.fontRenderer, px + 8, py + 26, GuiLayout.truncate(this.fontRenderer, hint, pw - 16), GuiStyle.TEXT_MUTED);
        if (this.selectedEmployee != null) {
            String selected = new TextComponentTranslation("container.sim.gui.selected", new Object[0]).getUnformattedText() + " " + this.selectedEmployee.displayString;
            GuiStyle.text(this.fontRenderer, px + 8, py + ph - 40, GuiLayout.truncate(this.fontRenderer, selected, pw - 120), GuiStyle.GREEN);
        }
    }

    private float occupantMoney(NpcData npc) {
        NpcIdentity identity = this.findIdentity(npc.ID);
        return identity != null ? identity.money : npc.money;
    }

    private NpcIdentity findIdentity(UUID id) {
        if (id == null) {
            return null;
        }
        for (NpcIdentity identity : ModSimClientLoader.tempHireableNpcNames) {
            if (identity != null && id.equals(identity.id)) {
                return identity;
            }
        }
        return null;
    }

    private String formatMoney(float value) {
        String formatted = ModSimLoader.displayMoney(value);
        return formatted == null ? "0.00" : formatted;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    @Override
    public void actionPerformed(GuiButton guibutton) {
        try {
            ModSimLoader.log.info("点击按钮：" + guibutton.id);
            if (System.currentTimeMillis() - this.fingBodge < 100L) {
                return;
            }
            this.fingBodge = System.currentTimeMillis();
            if (!guibutton.enabled) {
                return;
            }
            if (guibutton.id == 0) {
                this.mc.currentScreen = null;
                this.mc.setIngameFocus();
                return;
            }

            if (this.currentPage == 1) {
                if (guibutton.id == 1010) {
                    this.previousPage = this.currentPage;
                    this.currentPage = 0;
                    this.selectedEmployee = null;
                    this.showPage();
                    return;
                }
                if (guibutton.id == 1002 && this.selectedEmployee != null) {
                    NpcIdentity hired = this.hireableFolkNames[this.selectedEmployee.id];
                    if (hired != null) {
                        this.employee = hired;
                        this.hasEmployee = true;
                        if (this.occupants == null) {
                            this.occupants = new CopyOnWriteArrayList<NpcData>();
                        }
                        NpcData data = ModSimLoader.getFolkDataByUID(hired.id);
                        if (data != null) {
                            this.occupants.add(data);
                        }
                        NetWorkLoader.net.sendToServer(new PacketHireFolk(hired.id, this.jobName, new V3(this.pos.x, this.pos.y + 1, this.pos.z)));
                    }
                    this.previousPage = this.currentPage;
                    this.currentPage = 0;
                    this.selectedEmployee = null;
                    this.showPage();
                    return;
                }
                if (guibutton.id > 99 && guibutton.id < 1000) {
                    if (this.selectedEmployee != null) {
                        this.selectedEmployee.enabled = true;
                    }
                    this.selectedEmployee = guibutton;
                    this.selectedEmployee.enabled = false;
                    return;
                }
            }

            if (this.currentPage == 2) {
                if (guibutton.id == 1020) {
                    this.currentPage = 0;
                    this.showPage();
                    return;
                }
                if (guibutton.id == 1021) {
                    LogisticsSnapshot snapshot = ModSimClientLoader.logisticsSnapshot;
                    if (snapshot != null && snapshot.center != null) {
                        NetWorkLoader.net.sendToServer(PacketToggleLogistics.autoDispatch(
                                snapshot.center, !snapshot.autoDispatch));
                        snapshot.autoDispatch = !snapshot.autoDispatch;
                        this.showPage();
                    }
                    return;
                }
                if (guibutton.id == 1022) {
                    this.logisticsPage = Math.max(0, this.logisticsPage - 1);
                    this.showPage();
                    return;
                }
                if (guibutton.id == 1023) {
                    this.logisticsPage++;
                    this.showPage();
                    return;
                }
                if (guibutton.id >= 3000 && guibutton.id < 3000 + this.logisticsEntries.size()) {
                    LogisticsEntry entry = this.logisticsEntries.get(guibutton.id - 3000);
                    LogisticsSnapshot snapshot = ModSimClientLoader.logisticsSnapshot;
                    if (snapshot != null && snapshot.center != null) {
                        NetWorkLoader.net.sendToServer(new PacketToggleLogistics(
                                snapshot.center, entry.requesterKey, entry.itemKey, !entry.enabled));
                        entry.enabled = !entry.enabled;
                        for (LogisticsSnapshot.RequesterView requester : snapshot.requesters) {
                            if (requester == null || !entry.requesterKey.equals(requester.key)) {
                                continue;
                            }
                            for (LogisticsSnapshot.ItemView item : requester.items) {
                                if (item != null && entry.itemKey.equals(item.key)) {
                                    item.enabled = entry.enabled;
                                }
                            }
                        }
                        this.showPage();
                    }
                    return;
                }
                return;
            }

            if (guibutton.id == 30) {
                this.currentPage = 2;
                this.logisticsPage = 0;
                this.showPage();
                return;
            }
            if (guibutton.id == 1) {
                this.previousPage = this.currentPage;
                this.currentPage = 1;
                this.selectedEmployee = null;
                this.showPage();
                return;
            }
            if (guibutton.id == 21) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiEmployees());
                return;
            }
            if (guibutton.id == 1000) {
                NetWorkLoader.net.sendToServer(new PacketDemolishBuilding(this.buildingId));
                this.mc.currentScreen = null;
                this.mc.setIngameFocus();
                return;
            }
            if (guibutton.id == 1001) {
                NetWorkLoader.net.sendToServer(new PacketrotateStairs(this.buildingId));
                this.mc.currentScreen = null;
                this.mc.setIngameFocus();
                return;
            }
            if (guibutton.id >= 200 && guibutton.id < 200 + this.maxEmployeeRows()) {
                this.fireEmployee(guibutton.id - 200);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("actionPerformed出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private void fireEmployee(int index) {
        if (this.occupants == null || index < 0 || index >= this.occupants.size()) {
            return;
        }
        NpcData npc = this.occupants.remove(index);
        if (npc != null) {
            NetWorkLoader.net.sendToServer(new PacketFireFolk(npc.ID));
            Building building = ModSimLoader.getBuildingByV3(this.pos);
            if (building != null) {
                building.occupants.remove(npc);
                building.saveBuilding();
            }
        }
        this.hasEmployee = !this.occupants.isEmpty();
        this.currentPage = 0;
        this.showPage();
    }

    public void getHireableFolkNames() {
        NetWorkLoader.net.sendToServer(new PacketGetHireableFolks(true));
    }

    @Override
    public void keyTyped(char c, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen((GuiScreen) null);
            this.mc.setIngameFocus();
        }
    }
}
