package com.trhsy.sim.gui.block;


import com.trhsy.sim.gui.npc.GuiEmployees;
import com.trhsy.sim.loader.ConfigLoader;
import com.trhsy.sim.gui.GuiLayout;
import com.trhsy.sim.gui.GuiStyle;
import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.loader.NetWorkLoader;
import com.trhsy.sim.network.server.*;

import com.trhsy.sim.npcCode.NpcIdentity;
import com.trhsy.sim.npcCode.SimIds;
import com.trhsy.sim.npcCode.V3;
import com.trhsy.sim.npcCode.build.BuildingBlueprint;
import com.trhsy.sim.npcCode.build.TerrainType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @ClassName GuiBlockConstructorBlock
 * @Description todo 打开建筑箱gui
 * @Author TRHSY
 * @Date 2022/10/1916:23
 **/
public class GuiBlockConstructorBlock extends GuiScreen {
    /** 蓝图列表行高。 */
    private static final int LIST_ROW_H = 20;
    /** 排序字段。 */
    private static final int SORT_NAME = 0;
    private static final int SORT_COST = 1;
    private static final int SORT_AREA = 2;
    private static final int SORT_HEIGHT = 3;
    private static final int SORT_AUTHOR = 4;
    private static final int SORT_TYPE = 5;
    /** 占地筛选。 */
    private static final int SIZE_ALL = 0;
    private static final int SIZE_SMALL = 1;
    private static final int SIZE_MEDIUM = 2;
    private static final int SIZE_LARGE = 3;
    /** 分类页签对应的语言键，顺序与蓝图目录一致。 */
    private static final String[] CATEGORY_KEYS = new String[]{
            "container.sim.sim_gui_BC_Residential",
            "container.sim.sim_gui_BC_Commercial",
            "container.sim.sim_gui_BC_Industrial",
            "container.sim.sim_gui_BC_Other",
            "container.sim.sim_gui_BC_special",
            "container.sim.sim_gui_BC_Decorative"
    };
    /**
     * 有员工
     **/
    public boolean hasEmployee;
    /**
     * 雇佣的npc
     **/
    public NpcIdentity employee;
    /**
     * 建筑蓝图
     **/
    public BuildingBlueprint selectedBlueprint;
    /**
     * 蓝图集合
     **/
    public List<BuildingBlueprint> potentialBlueprints = new ArrayList<BuildingBlueprint>();
    /**
     * 可雇佣的人
     **/
    public NpcIdentity[] hireableFolkNames = new NpcIdentity[1000];
    /**
     * 雇佣地形师
     */
    public boolean hiringTerraformer = false;
    /**
     * 选定的员工
     **/
    GuiButton selectedEmployee;
    /**
     * 位置
     **/
    public BlockPos pos;
    /**
     * 建筑方向
     **/
    public int buildDirection;
    /**
     * 分页
     */
    public int currentPage = 0;
    /**
     * 上一页
     */
    public int previousPage = 1;
    /**
     * 当前分类：-1 全部，0..5 对应住宅/商业/工业/其他/特殊/装饰
     */
    private int browseCategory = -1;
    /**
     * 当前排序字段
     */
    private int sortMode = SORT_NAME;
    /**
     * 是否升序
     */
    private boolean sortAscending = true;
    /**
     * 占地筛选
     */
    private int sizeFilter = SIZE_ALL;
    /**
     * 预算筛选：0 全部，1 预算内
     */
    private int budgetFilter = 0;
    /**
     * 当前页可显示的行数
     */
    private int visibleRows = 1;
    /**
     * 搜索
     */
    private GuiTextField tfSearch;
    /**
     * 搜索文字
     **/
    private String search = "";
    /**
     * 建筑偏移量
     */
    private int buildingOffset;
    /**
     * 建筑的时间
     */
    long fingBodge = 0L;
    int dimension;
    /**
     * @return 块，方向
     * @Author fan
     * @Description //TODO 初始化
     * @Date 13:33 2022/10/31
     * @Param [p, bDir]
     **/
    public GuiBlockConstructorBlock(BlockPos p, int bDir,int dimension) {
        this.pos = p;
        this.buildDirection = bDir;
        this.hasEmployee = false;
        this.employee = null;
        this.dimension=dimension;
        this.getHireableFolkNames();
        if (ModSimClientLoader.previewConstructor == p) {
            //constructorPreviousPage
            ModSimClientLoader.constructorPreviousPage = 0;
            //savedBlueprint
            ModSimClientLoader.savedBlueprint = null;
            //要构建的
            ModSimClientLoader.previewConstructor = null;
            //预览位置
            ModSimClientLoader.previewPos1 = null;
            ModSimClientLoader.previewPos2 = null;
        }

    }

    /**
     * @return 方块，建筑方向，npc
     * @Author fan 63521155
     * @Description //TODO 初始化
     * @Date 13:37 2022/10/31
     * @Param [p, bDir, folk]
     **/
    public GuiBlockConstructorBlock(BlockPos p, int bDir, NpcIdentity folk,int dimension) {
        this.pos = p;
        this.buildDirection = bDir;
        this.employee = folk;
        this.hasEmployee = true;
        this.getHireableFolkNames();
        this.dimension=dimension;
        if(folk.job.contentEquals(new TextComponentTranslation("container.sim.Vocation16",new Object[0]).getUnformattedText())){
            this.hiringTerraformer=true;
        }
        //
        if (p.equals(ModSimClientLoader.previewConstructor)) {
            this.selectedBlueprint = ModSimClientLoader.savedBlueprint;
            if (this.selectedBlueprint != null) {
                this.previousPage = ModSimClientLoader.constructorPreviousPage;
                this.currentPage = 11;
            }

            ModSimClientLoader.constructorPreviousPage = 0;
            ModSimClientLoader.savedBlueprint = null;
            ModSimClientLoader.previewConstructor = null;
            ModSimClientLoader.previewPos1 = null;
            ModSimClientLoader.previewPos2 = null;
        }

    }

    /**
     * @return boolean
     * @Author fan
     * @Description //TODO 如果此GUI在单人游戏中显示时应暂停游戏，则返回true
     * @Date 13:44 2022/10/31
     * @Param []
     **/
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 初始化GUI
     * @Date 13:46 2022/10/31
     * @Param []
     **/
    @Override
    public void initGui() {
        super.initGui();
        //显示分页
        this.showPage();
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 显示分页
     * @Date 13:46 2022/10/31
     * @Param []
     **/
    public void showPage() {
        this.mc.setIngameNotInFocus();
        this.buttonList.clear();
        int px = left();
        int py = top();
        int pw = panelW();
        int ph = panelH();

        //完成
        this.buttonList.add(new GuiButton(0, px + pw - 60, py + 4, 52, 18, t("container.sim.sim_gui_BC_Done")));

        if (this.currentPage == 1) {
            this.showBrowsePage(px, py, pw, ph);
            return;
        }
        if (this.currentPage == 2) {
            this.showHirePage(px, py, pw, ph);
            return;
        }
        if (this.currentPage == 11) {
            this.buttonList.add(new GuiButton(1000, px + 8, py + ph - 26, 90, 20, t("container.sim.sim_gui_BC_Go_Back")));
            this.buttonList.add(new GuiButton(1001, px + pw - 210, py + ph - 26, 90, 20, t("container.sim.sim_gui_BC_Preview")));
            this.buttonList.add(new GuiButton(969, px + pw - 112, py + ph - 26, 104, 20, t("container.sim.sim_gui_BC_requirements")));
            return;
        }
        if (this.currentPage == 12) {
            this.buttonList.add(new GuiButton(1000, px + 8, py + ph - 26, 90, 20, t("container.sim.sim_gui_BC_Go_Back")));
            GuiButton build = new GuiButton(970, px + pw - 98, py + ph - 26, 90, 20, t("container.sim.sim_gui_BC_Build_it"));
            build.enabled = this.employee != null;
            this.buttonList.add(build);
            return;
        }
        if (this.currentPage == 13) {
            this.buttonList.add(new GuiButton(505, px + 8, py + ph - 26, 74, 20, t("container.sim.gui_Folk_Back")));
            int gap = 6;
            int btnW = (pw - 24 - gap) / 2;
            int startY = py + 54;
            String[] terrains = new String[]{
                    t("container.sim.Terraform2"), t("container.sim.Terraform3"), t("container.sim.Terraform4"),
                    t("container.sim.Terraform5"), t("container.sim.Terraform6"), t("container.sim.Terraform7"),
                    t("container.sim.Terraform8"), t("container.sim.Terraform9"), t("container.sim.Terraform10")
            };
            for (int i = 0; i < terrains.length; i++) {
                int col = i % 2;
                int row = i / 2;
                this.buttonList.add(new GuiButton(21 + i, px + 8 + col * (btnW + gap), startY + row * 24, btnW, 20, terrains[i]));
            }
            return;
        }

        //第 0 页：建筑箱概览
        int gap = 6;
        int btnW = (pw - 24 - gap) / 2;
        int leftX = px + 8;
        int rightX = px + 8 + btnW + gap;
        int rowY = py + 112;

        GuiButton choose = new GuiButton(1, leftX, rowY, btnW, 20, t("container.sim.sim_gui_BC_Choose_building"));
        GuiButton hireBuilder = new GuiButton(2, leftX, rowY + 26, btnW, 20, t("container.sim.Hire1"));
        GuiButton fire = new GuiButton(3, leftX, rowY + 52, btnW, 20, t("container.sim.Fire") + t("container.sim.sim_gui_BC_worker"));
        GuiButton showEmployees = new GuiButton(4, rightX, rowY, btnW, 20, t("container.sim.sim_gui_BC_Show_Employees"));
        GuiButton terraform = new GuiButton(6, rightX, rowY + 26, btnW, 20, t("container.sim.sim_gui_BC_Terraform_area"));
        GuiButton hirePlanner = new GuiButton(7, rightX, rowY + 52, btnW, 20, t("container.sim.Hire22"));

        if (!this.hasEmployee) {
            choose.enabled = false;
            terraform.enabled = this.hiringTerraformer;
            hireBuilder.enabled = true;
            fire.enabled = false;
            hirePlanner.enabled = true;
        } else {
            if (this.hiringTerraformer) {
                choose.enabled = false;
                terraform.enabled = true;
            } else {
                choose.enabled = true;
                terraform.enabled = false;
            }
            hireBuilder.enabled = false;
            fire.enabled = true;
            hirePlanner.enabled = false;
        }
        this.buttonList.add(choose);
        this.buttonList.add(hireBuilder);
        this.buttonList.add(fire);
        this.buttonList.add(showEmployees);
        this.buttonList.add(terraform);
        this.buttonList.add(hirePlanner);
    }

    /** 建筑浏览页：搜索、分类页签、排序、筛选、分页都在同一页。 */
    private void showBrowsePage(int px, int py, int pw, int ph) {
        this.refreshBlueprints();
        int listTop = this.listTop(py);
        int listBottom = py + ph - 46;
        this.visibleRows = Math.max(1, (listBottom - listTop) / LIST_ROW_H);
        int maxOffset = Math.max(0, this.potentialBlueprints.size() - this.visibleRows);
        if (this.buildingOffset < 0) {
            this.buildingOffset = 0;
        }
        if (this.buildingOffset > maxOffset) {
            this.buildingOffset = maxOffset;
        }
        //搜索框
        this.tfSearch = new GuiTextField(1, this.fontRenderer, px + 48, py + 24, pw - 56, 18);
        this.tfSearch.setText(this.search);
        this.tfSearch.setFocused(true);
        this.tfSearch.setMaxStringLength(32);
        this.tfSearch.setEnableBackgroundDrawing(true);
        this.tfSearch.setCursorPositionEnd();

        //分类页签：全部 + 6 类，两行四列，避免英文标签被截断
        int gap = 4;
        int tabW = (pw - 16 - gap * 3) / 4;
        for (int i = 0; i < 7; i++) {
            int category = i - 1;
            String label = this.categoryLabel(category);
            if (this.browseCategory == category) {
                label = "[" + label + "]";
            }
            int col = i % 4;
            int row = i / 4;
            GuiButton tab = new GuiButton(700 + i, px + 8 + col * (tabW + gap),
                    py + 46 + row * 20, tabW, 18, GuiLayout.truncate(this.fontRenderer, label, tabW - 4));
            this.buttonList.add(tab);
        }

        //排序 + 筛选
        int sortW = Math.max(120, (pw - 16) * 42 / 100);
        int filterW = (pw - 16 - sortW - gap * 2) / 2;
        this.buttonList.add(new GuiButton(710, px + 8, py + 88, sortW, 18, this.sortLabel()));
        this.buttonList.add(new GuiButton(712, px + 8 + sortW + gap, py + 88, filterW, 18, this.sizeFilterLabel()));
        this.buttonList.add(new GuiButton(713, px + 8 + sortW + gap * 2 + filterW, py + 88, filterW, 18,
                this.budgetFilterLabel()));

        //底部：返回、上一页、下一页
        this.buttonList.add(new GuiButton(505, px + 8, py + ph - 26, 60, 20, t("container.sim.gui_Folk_Back")));
        GuiButton prev = new GuiButton(501, px + 72, py + ph - 26, 56, 20, "<");
        prev.enabled = this.buildingOffset > 0;
        this.buttonList.add(prev);
        GuiButton next = new GuiButton(500, px + pw - 64, py + ph - 26, 56, 20, ">");
        next.enabled = this.buildingOffset + this.visibleRows < this.potentialBlueprints.size();
        this.buttonList.add(next);
        this.buttonList.add(new GuiButton(714, px + pw - 128, py + ph - 26, 60, 20,
                t("container.sim.sim_gui_BC_reset")));
    }

    private void showHirePage(int px, int py, int pw, int ph) {
        this.buttonList.add(new GuiButton(1000, px + 8, py + ph - 26, 90, 20, t("container.sim.sim_gui_player_to_Cancel")));
        this.buttonList.add(new GuiButton(1001, px + pw - 98, py + ph - 26, 90, 20, t("container.sim.gui_btn_name_OK")));
        int btnW = (pw - 24) / 2;
        int colGap = pw - 16 - btnW * 2;
        int startY = py + 46;
        int rowH = 22;
        int max = Math.min(ModSimClientLoader.getUnemployedFolks().size(), 12);
        for (int i = 0; i < max; i++) {
            NpcIdentity folk = ModSimClientLoader.getUnemployedFolks().get(i);
            int col = i % 2;
            int row = i / 2;
            this.buttonList.add(new GuiButton(100 + i, px + 8 + col * (btnW + colGap), startY + row * rowH, btnW, 20, folk.name));
            this.hireableFolkNames[100 + i] = folk;
        }
    }

    private static String t(String key) {
        return new TextComponentTranslation(key, new Object[0]).getUnformattedText();
    }

    private static String t(String key, Object... args) {
        return new TextComponentTranslation(key, args).getUnformattedText();
    }

    /** 当前分类的显示名；-1 返回“全部”。 */
    private String categoryLabel(int category) {
        if (category < 0 || category >= CATEGORY_KEYS.length) {
            return t("container.sim.sim_gui_BC_all");
        }
        return t(CATEGORY_KEYS[category]);
    }

    /** 根据搜索、分类、占地、预算筛选并排序。 */
    private void refreshBlueprints() {
        List<BuildingBlueprint> result = new ArrayList<BuildingBlueprint>();
        String query = this.search == null ? "" : this.search.trim().toLowerCase(Locale.ROOT);
        for (BuildingBlueprint blueprint : ModSimLoader.buildingBlueprints) {
            if (blueprint == null) {
                continue;
            }
            if (!this.matchesCategory(blueprint, this.browseCategory)) {
                continue;
            }
            if (!this.matchesSearch(blueprint, query)) {
                continue;
            }
            if (!this.matchesSize(blueprint, this.sizeFilter)) {
                continue;
            }
            if (!this.matchesBudget(blueprint, this.budgetFilter)) {
                continue;
            }
            result.add(blueprint);
        }
        Collections.sort(result, this.blueprintComparator());
        this.potentialBlueprints = result;
    }

    private boolean matchesCategory(BuildingBlueprint blueprint, int category) {
        if (category < 0 || category >= CATEGORY_KEYS.length) {
            return true;
        }
        String display = t(CATEGORY_KEYS[category]);
        String wantId = SimIds.buildingTypeId(display);
        if (wantId != null && wantId.equals(blueprint.buildingType)) {
            return true;
        }
        return display.equals(blueprint.buildingType);
    }

    private boolean matchesSearch(BuildingBlueprint blueprint, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(blueprint.name, query)
                || containsIgnoreCase(blueprint.author, query)
                || containsIgnoreCase(blueprint.desc, query)
                || containsIgnoreCase(blueprint.jobType, query)
                || containsIgnoreCase(SimIds.buildingTypeDisplay(blueprint.buildingType), query);
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesSize(BuildingBlueprint blueprint, int filter) {
        int footprint = Math.max(0, blueprint.length) * Math.max(0, blueprint.width);
        switch (filter) {
            case SIZE_SMALL:
                return footprint <= 100;
            case SIZE_MEDIUM:
                return footprint > 100 && footprint <= 400;
            case SIZE_LARGE:
                return footprint > 400;
            default:
                return true;
        }
    }

    private boolean matchesBudget(BuildingBlueprint blueprint, int filter) {
        if (filter == 0 || ModSimClientLoader.gamemode == 1) {
            return true;
        }
        return blueprintCost(blueprint) <= ModSimClientLoader.money;
    }

    private static float blueprintCost(BuildingBlueprint blueprint) {
        return (float) blueprint.blockCount * 0.02F;
    }

    private Comparator<BuildingBlueprint> blueprintComparator() {
        Comparator<BuildingBlueprint> comparator;
        switch (this.sortMode) {
            case SORT_COST:
                comparator = new Comparator<BuildingBlueprint>() {
                    @Override
                    public int compare(BuildingBlueprint a, BuildingBlueprint b) {
                        return Float.compare(blueprintCost(a), blueprintCost(b));
                    }
                };
                break;
            case SORT_AREA:
                comparator = new Comparator<BuildingBlueprint>() {
                    @Override
                    public int compare(BuildingBlueprint a, BuildingBlueprint b) {
                        return Integer.compare(a.length * a.width, b.length * b.width);
                    }
                };
                break;
            case SORT_HEIGHT:
                comparator = new Comparator<BuildingBlueprint>() {
                    @Override
                    public int compare(BuildingBlueprint a, BuildingBlueprint b) {
                        return Integer.compare(a.height, b.height);
                    }
                };
                break;
            case SORT_AUTHOR:
                comparator = new Comparator<BuildingBlueprint>() {
                    @Override
                    public int compare(BuildingBlueprint a, BuildingBlueprint b) {
                        String left = a.author == null ? "" : a.author;
                        String right = b.author == null ? "" : b.author;
                        return String.CASE_INSENSITIVE_ORDER.compare(left, right);
                    }
                };
                break;
            case SORT_TYPE:
                comparator = new Comparator<BuildingBlueprint>() {
                    @Override
                    public int compare(BuildingBlueprint a, BuildingBlueprint b) {
                        return Integer.compare(categoryOrder(a.buildingType), categoryOrder(b.buildingType));
                    }
                };
                break;
            case SORT_NAME:
            default:
                comparator = new Comparator<BuildingBlueprint>() {
                    @Override
                    public int compare(BuildingBlueprint a, BuildingBlueprint b) {
                        String left = a.name == null ? "" : a.name;
                        String right = b.name == null ? "" : b.name;
                        return String.CASE_INSENSITIVE_ORDER.compare(left, right);
                    }
                };
                break;
        }
        if (!this.sortAscending) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private static int categoryOrder(String buildingType) {
        String id = SimIds.buildingTypeId(buildingType);
        if (id == null) {
            id = buildingType;
        }
        if (SimIds.TYPE_RESIDENTIAL.equals(id)) {
            return 0;
        }
        if (SimIds.TYPE_COMMERCIAL.equals(id)) {
            return 1;
        }
        if (SimIds.TYPE_INDUSTRIAL.equals(id)) {
            return 2;
        }
        if (SimIds.TYPE_OTHER.equals(id)) {
            return 3;
        }
        if (SimIds.TYPE_SPECIAL.equals(id)) {
            return 4;
        }
        if (SimIds.TYPE_DECORATIVE.equals(id)) {
            return 5;
        }
        return CATEGORY_KEYS.length;
    }

    private String sortLabel() {
        String[] keys = new String[]{
                "container.sim.sim_gui_BC_sort_name",
                "container.sim.sim_gui_BC_sort_cost",
                "container.sim.sim_gui_BC_sort_area",
                "container.sim.sim_gui_BC_sort_height",
                "container.sim.sim_gui_BC_sort_author",
                "container.sim.sim_gui_BC_sort_type"
        };
        return t("container.sim.sim_gui_BC_sort") + ": " + t(keys[this.sortMode])
                + (this.sortAscending ? " ▲" : " ▼");
    }

    private String sizeFilterLabel() {
        return t("container.sim.sim_gui_BC_filter_size") + ": " + t(this.sizeFilterKey(this.sizeFilter));
    }

    private String budgetFilterLabel() {
        String key = this.budgetFilter == 0
                ? "container.sim.sim_gui_BC_budget_all"
                : "container.sim.sim_gui_BC_budget_affordable";
        return t("container.sim.sim_gui_BC_filter_budget") + ": " + t(key);
    }

    private String sizeFilterKey(int filter) {
        switch (filter) {
            case SIZE_SMALL:
                return "container.sim.sim_gui_BC_size_small";
            case SIZE_MEDIUM:
                return "container.sim.sim_gui_BC_size_medium";
            case SIZE_LARGE:
                return "container.sim.sim_gui_BC_size_large";
            default:
                return "container.sim.sim_gui_BC_size_all";
        }
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 激活时由按钮列表中的控件调用。（鼠标按下按钮）
     * @Date 15:48 2022/10/31
     * @Param [guibutton]
     **/
    @SubscribeEvent(
            priority = EventPriority.NORMAL
    )
    @Override
    public void actionPerformed(GuiButton guibutton) {
        try {
            //时间限制，避免连点
            if (System.currentTimeMillis() - this.fingBodge < 100L) {
                return;
            }
            this.fingBodge = System.currentTimeMillis();
            if (!guibutton.enabled) {
                return;
            }
            if (guibutton.id == 0) {
                this.closeGui();
                return;
            }
            if (this.currentPage == 0) {
                this.handleHomeButton(guibutton);
            } else if (this.currentPage == 1) {
                this.handleBrowseButton(guibutton);
            } else if (this.currentPage == 2) {
                this.handleHireButton(guibutton);
            } else if (this.currentPage == 11) {
                this.handleDetailsButton(guibutton);
            } else if (this.currentPage == 12) {
                this.handleRequirementsButton(guibutton);
            } else if (this.currentPage == 13) {
                this.handleTerrainButton(guibutton);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("建筑箱GUI actionPerformed出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    private void handleHomeButton(GuiButton guibutton) {
        if (guibutton.id == 1) {
            //选择建筑：直接进入建筑列表
            this.previousPage = 1;
            this.currentPage = 1;
            this.buildingOffset = 0;
            this.showPage();
        } else if (guibutton.id == 4) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiEmployees());
        } else if (guibutton.id == 2) {
            //雇佣建筑工
            this.hiringTerraformer = false;
            this.selectedEmployee = null;
            this.previousPage = 0;
            this.currentPage = 2;
            this.showPage();
        } else if (guibutton.id == 7) {
            //雇佣规划师
            this.hiringTerraformer = true;
            this.selectedEmployee = null;
            this.previousPage = 0;
            this.currentPage = 2;
            this.showPage();
        } else if (guibutton.id == 3) {
            //解雇
            if (this.employee != null) {
                NetWorkLoader.net.sendToServer(new PacketFireFolk(this.employee.id));
                this.closeGui();
            }
        } else if (guibutton.id == 6) {
            //规划区域
            this.previousPage = 0;
            this.currentPage = 13;
            this.showPage();
        }
    }

    private void handleBrowseButton(GuiButton guibutton) {
        if (guibutton.id == 505) {
            this.currentPage = 0;
            this.showPage();
            return;
        }
        if (guibutton.id >= 700 && guibutton.id <= 706) {
            //700=全部，701..706=六个分类
            this.browseCategory = guibutton.id - 701;
            this.buildingOffset = 0;
            this.showPage();
            return;
        }
        if (guibutton.id == 710) {
            //名称↑ → 名称↓ → 费用↑ → 费用↓ → ...
            if (this.sortAscending) {
                this.sortAscending = false;
            } else {
                this.sortAscending = true;
                this.sortMode = (this.sortMode + 1) % 6;
            }
            this.buildingOffset = 0;
            this.showPage();
            return;
        }
        if (guibutton.id == 712) {
            this.sizeFilter = (this.sizeFilter + 1) % 4;
            this.buildingOffset = 0;
            this.showPage();
            return;
        }
        if (guibutton.id == 713) {
            this.budgetFilter = 1 - this.budgetFilter;
            this.buildingOffset = 0;
            this.showPage();
            return;
        }
        if (guibutton.id == 714) {
            this.browseCategory = -1;
            this.sortMode = SORT_NAME;
            this.sortAscending = true;
            this.sizeFilter = SIZE_ALL;
            this.budgetFilter = 0;
            this.search = "";
            this.buildingOffset = 0;
            this.showPage();
            return;
        }
        if (guibutton.id == 500) {
            this.buildingOffset += this.visibleRows;
            this.showPage();
        } else if (guibutton.id == 501) {
            this.buildingOffset -= this.visibleRows;
            this.showPage();
        }
    }

    private void handleHireButton(GuiButton guibutton) {
        if (guibutton.id == 1000) {
            this.selectedEmployee = null;
            this.currentPage = 0;
            this.showPage();
            return;
        }
        if (guibutton.id == 1001 && this.selectedEmployee != null) {
            NpcIdentity hired = this.hireableFolkNames[this.selectedEmployee.id];
            if (hired == null) {
                return;
            }
            this.hasEmployee = true;
            this.employee = hired;
            if (this.hiringTerraformer) {
                NetWorkLoader.net.sendToServer(new PacketHireFolk(hired.id,
                        t("container.sim.Vocation16"),
                        new V3(this.pos.getX(), this.pos.getY() + 1, this.pos.getZ(), this.dimension),
                        this.buildDirection));
            } else {
                NetWorkLoader.net.sendToServer(new PacketHireFolk(hired.id,
                        t("container.sim.Vocation1"),
                        new V3(this.pos.getX(), this.pos.getY() + 1, this.pos.getZ(), this.dimension),
                        this.buildDirection));
            }
            this.previousPage = 0;
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
        }
    }

    private void handleDetailsButton(GuiButton guibutton) {
        if (guibutton.id == 1000) {
            this.currentPage = 1;
            this.previousPage = 1;
            this.showPage();
            return;
        }
        if (guibutton.id == 969) {
            this.previousPage = 11;
            this.currentPage = 12;
            this.showPage();
            return;
        }
        if (guibutton.id == 1001 && this.selectedBlueprint != null) {
            ModSimClientLoader.previewConstructor = this.pos;
            ModSimClientLoader.savedBlueprint = this.selectedBlueprint;
            ModSimClientLoader.previewPos1 = this.selectedBlueprint.getFirstPoint(this.pos, this.buildDirection);
            ModSimClientLoader.previewPos2 = this.selectedBlueprint.getSecondPoint(this.pos, this.buildDirection);
            ModSimClientLoader.constructorPreviousPage = 1;
            this.closeGui();
        }
    }

    private void handleRequirementsButton(GuiButton guibutton) {
        if (guibutton.id == 1000) {
            this.currentPage = 11;
            this.showPage();
            return;
        }
        if (guibutton.id == 970 && this.selectedBlueprint != null && this.employee != null) {
            NetWorkLoader.net.sendToServer(new PacketSendBlueprint(this.selectedBlueprint, this.employee.id,
                    new V3(this.pos, this.dimension).toString(), this.buildDirection));
            this.closeGui();
        }
    }

    private void handleTerrainButton(GuiButton guibutton) {
        if (guibutton.id == 505) {
            this.currentPage = 0;
            this.showPage();
            return;
        }
        int index = guibutton.id - 21;
        if (index < 0 || index > 8 || this.employee == null) {
            return;
        }
        TerrainType terrainType = new TerrainType(t("container.sim.packetTerra" + (index + 1)),
                String.valueOf(index + 1));
        NetWorkLoader.net.sendToServer(new PacketSendTerrainType(terrainType, this.employee.id,
                new V3(this.pos, this.dimension).toString()));
        this.closeGui();
    }

    private void closeGui() {
        this.mc.currentScreen = null;
        this.mc.setIngameFocus();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            this.drawDefaultBackground();
            int px = left();
            int py = top();
            int pw = panelW();
            int ph = panelH();
            GuiStyle.panel(px, py, pw, ph, t("container.sim.sim_gui_BC_Constructor"), this.fontRenderer);

            if (this.currentPage == 0) {
                GuiStyle.text(this.fontRenderer, px + 8, py + 28, GuiLayout.truncate(this.fontRenderer, t("container.sim.sim_gui_BC_building_constructor"), pw - 16), GuiStyle.TEXT_MUTED);
                this.drawSummaryCards(px, py, pw, ph);
            } else if (this.currentPage == 1) {
                this.drawBrowsePage(px, py, pw, ph, mouseX, mouseY);
            } else if (this.currentPage == 2) {
                GuiStyle.text(this.fontRenderer, px + 8, py + 28, t("container.sim.sim_gui_BC_unemployed"), GuiStyle.TEXT_MUTED);
                if (this.selectedEmployee != null) {
                    GuiStyle.text(this.fontRenderer, px + 8, py + ph - 42, t("container.sim.gui.selected") + " " + this.selectedEmployee.displayString, GuiStyle.GREEN);
                }
            } else if (this.currentPage == 11) {
                this.drawBlueprintDetails(px, py, pw, ph);
            } else if (this.currentPage == 12) {
                GuiStyle.text(this.fontRenderer, px + 8, py + 28, new TextComponentTranslation("container.sim.sim_gui_BC_requirements_for", new Object[0]).getUnformattedText(), GuiStyle.TEXT);
                if (this.selectedBlueprint != null) {
                    String requirementText = this.selectedBlueprint.getBuildingRequirementsString();
                    if (requirementText == null) {
                        requirementText = "";
                    }
                    String[] reqs = requirementText.split(";");
                    int y = py + 46;
                    int lineHeight = 12;
                    int maxLines = Math.max(1, (ph - 84) / lineHeight);
                    int drawn = 0;
                    for (String req : reqs) {
                        if (drawn >= maxLines) {
                            break;
                        }
                        for (String line : GuiLayout.wrap(this.fontRenderer, req, pw - 24)) {
                            if (drawn >= maxLines) {
                                break;
                            }
                            GuiStyle.text(this.fontRenderer, px + 12, y, line, GuiStyle.TEXT_MUTED);
                            y += lineHeight;
                            drawn++;
                        }
                    }
                }
            } else if (this.currentPage == 13) {
                GuiStyle.text(this.fontRenderer, px + 8, py + 30, new TextComponentTranslation("container.sim.Terraform11", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
                GuiStyle.text(this.fontRenderer, px + pw - 160, py + 30, new TextComponentTranslation("container.sim.Terraform12", new Object[0]).getUnformattedText() + ConfigLoader.configLumberArea, GuiStyle.TEXT_MUTED);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("建筑箱GUI drawScreen出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /** 建筑浏览页的绘制：搜索框、表头、列表、结果/页码。 */
    private void drawBrowsePage(int px, int py, int pw, int ph, int mouseX, int mouseY) {
        GuiStyle.text(this.fontRenderer, px + 8, py + 28, t("container.sim.sim_gui_BC_search"), GuiStyle.TEXT_MUTED);
        if (this.tfSearch != null) {
            this.tfSearch.drawTextBox();
        }

        int headerY = this.headerY(py);
        GuiStyle.inset(px + 8, headerY, pw - 16, 14);
        int[] columns = this.columnLayout(px, pw);
        GuiStyle.text(this.fontRenderer, columns[0], headerY + 3,
                this.headerLabel("container.sim.gui.name", SORT_NAME, columns[4]), GuiStyle.TEXT);
        GuiStyle.text(this.fontRenderer, columns[1], headerY + 3,
                this.headerLabel("container.sim.sim_gui_BC7", SORT_AREA, columns[5]), GuiStyle.TEXT);
        GuiStyle.text(this.fontRenderer, columns[2], headerY + 3,
                this.headerLabel("container.sim.sim_gui_BC6", SORT_COST, columns[6]), GuiStyle.TEXT);
        GuiStyle.text(this.fontRenderer, columns[3], headerY + 3,
                this.headerLabel("container.sim.sim_gui_BC5", SORT_AUTHOR, columns[7]), GuiStyle.TEXT);

        final int listTop = this.listTop(py);
        final int listHeight = this.visibleRows * LIST_ROW_H;
        GuiLayout.scissor(px + 8, listTop, pw - 16, listHeight, () -> {
            if (this.potentialBlueprints == null || this.potentialBlueprints.isEmpty()) {
                String message = ModSimLoader.buildingBlueprints.isEmpty()
                        ? t("container.sim.sim_gui_BC_Nothing_found")
                        : t("container.sim.sim_gui_BC_no_results");
                GuiStyle.text(this.fontRenderer, px + 12, listTop + 4,
                        GuiLayout.truncate(this.fontRenderer, message, pw - 24), GuiStyle.TEXT_MUTED);
                return;
            }
            for (int i = 0; i < this.visibleRows; i++) {
                int index = this.buildingOffset + i;
                if (index < 0 || index >= this.potentialBlueprints.size()) {
                    break;
                }
                BuildingBlueprint blueprint = this.potentialBlueprints.get(index);
                if (blueprint == null) {
                    continue;
                }
                int y = listTop + i * LIST_ROW_H;
                boolean hover = mouseX >= px + 8 && mouseX <= px + pw - 8
                        && mouseY >= y && mouseY < y + LIST_ROW_H
                        && mouseY < listTop + listHeight;
                GuiStyle.row(px + 8, y, pw - 16, LIST_ROW_H, hover || i % 2 == 1);
                if (hover) {
                    Gui.drawRect(px + 8, y, px + pw - 8, y + 1, GuiStyle.BORDER);
                    Gui.drawRect(px + 8, y + LIST_ROW_H - 1, px + pw - 8, y + LIST_ROW_H, GuiStyle.BORDER);
                    Gui.drawRect(px + 8, y, px + 9, y + LIST_ROW_H, GuiStyle.BORDER);
                    Gui.drawRect(px + pw - 9, y, px + pw - 8, y + LIST_ROW_H, GuiStyle.BORDER);
                }
                GuiStyle.text(this.fontRenderer, columns[0], y + 6,
                        GuiLayout.truncate(this.fontRenderer, blueprint.name, columns[4]), GuiStyle.TEXT);
                String dimensions = blueprint.length + " x " + blueprint.width + " x " + blueprint.height;
                GuiStyle.text(this.fontRenderer, columns[1], y + 6,
                        GuiLayout.truncate(this.fontRenderer, dimensions, columns[5]), GuiStyle.TEXT_MUTED);
                float cost = blueprintCost(blueprint);
                int costColor = (ModSimClientLoader.gamemode != 1 && cost > ModSimClientLoader.money)
                        ? GuiStyle.RED : GuiStyle.GREEN;
                GuiStyle.text(this.fontRenderer, columns[2], y + 6,
                        GuiLayout.truncate(this.fontRenderer, ModSimLoader.displayMoney(cost), columns[6]), costColor);
                GuiStyle.text(this.fontRenderer, columns[3], y + 6,
                        GuiLayout.truncate(this.fontRenderer, blueprint.author == null ? "" : blueprint.author, columns[7]),
                        GuiStyle.TEXT_MUTED);
            }
        });

        int total = this.potentialBlueprints == null ? 0 : this.potentialBlueprints.size();
        GuiStyle.text(this.fontRenderer, px + 8, py + ph - 40,
                t("container.sim.sim_gui_BC_results", total), GuiStyle.TEXT_MUTED);
        int pages = Math.max(1, (total + this.visibleRows - 1) / this.visibleRows);
        int page = total == 0 ? 1 : this.buildingOffset / this.visibleRows + 1;
        String pageText = t("container.sim.sim_gui_BC_page_info", page, pages);
        GuiStyle.textRight(this.fontRenderer, px + pw - 8, py + ph - 40, pageText, GuiStyle.TEXT_MUTED);
    }

    private int headerY(int py) {
        return py + 110;
    }

    private int listTop(int py) {
        return py + 126;
    }

    /**
     * 列起点与列宽：0..3 为 x，4..7 为宽度。
     * 用比例而不是绝对值，GUI Scale 和语言变化时不会重叠。
     */
    private int[] columnLayout(int px, int pw) {
        int gap = 6;
        int available = Math.max(140, pw - 24 - gap * 3);
        int nameW = Math.max(64, available * 42 / 100);
        int sizeW = Math.max(44, available * 20 / 100);
        int costW = Math.max(42, available * 18 / 100);
        int authorW = Math.max(36, available - nameW - sizeW - costW);
        int x0 = px + 12;
        int x1 = x0 + nameW + gap;
        int x2 = x1 + sizeW + gap;
        int x3 = x2 + costW + gap;
        return new int[]{x0, x1, x2, x3, nameW, sizeW, costW, authorW};
    }

    private String headerLabel(String key, int field, int maxWidth) {
        String label = t(key);
        if (this.sortMode == field) {
            label += this.sortAscending ? " ▲" : " ▼";
        }
        return GuiLayout.truncate(this.fontRenderer, label, maxWidth);
    }

    private void drawSummaryCards(int px, int py, int pw, int ph) {
        int gap = 4;
        int cardW = (pw - 16 - gap * 3) / 4;
        int cardsY = py + 44;
        String[] labels = new String[]{
                new TextComponentTranslation("container.sim.gui.farm.employee", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.sim_gui_BC_worker", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.gui.wage", new Object[0]).getUnformattedText(),
                new TextComponentTranslation("container.sim.gui.current_job", new Object[0]).getUnformattedText()
        };
        String[] values = new String[]{
                this.hasEmployee && this.employee != null ? this.employee.name : new TextComponentTranslation("container.sim.gui.employees.none", new Object[0]).getUnformattedText(),
                this.hiringTerraformer ? new TextComponentTranslation("container.sim.Vocation16", new Object[0]).getUnformattedText() : new TextComponentTranslation("container.sim.Vocation1", new Object[0]).getUnformattedText(),
                ModSimLoader.displayMoney(com.trhsy.sim.npcCode.economy.Economy.WAGE_PER_MINUTE) + new TextComponentTranslation("container.sim.gui.per_minute", new Object[0]).getUnformattedText(),
                this.selectedBlueprint == null ? "-" : this.selectedBlueprint.name
        };
        for (int i = 0; i < 4; i++) {
            int x = px + 8 + i * (cardW + gap);
            GuiStyle.inset(x, cardsY, cardW, 34);
            GuiStyle.text(this.fontRenderer, x + 4, cardsY + 4, labels[i], GuiStyle.TEXT_MUTED);
            GuiStyle.text(this.fontRenderer, x + 4, cardsY + 17, GuiLayout.truncate(this.fontRenderer, values[i], cardW - 8), GuiStyle.TEXT);
        }
    }

    private void drawBlueprintDetails(int px, int py, int pw, int ph) {
        if (this.selectedBlueprint == null) {
            GuiStyle.text(this.fontRenderer, px + 12, py + 32, new TextComponentTranslation("container.sim.sim_gui_BC_Nothing_found", new Object[0]).getUnformattedText(), GuiStyle.RED);
            return;
        }
        int y = py + 34;
        GuiStyle.text(this.fontRenderer, px + 12, y, new TextComponentTranslation("container.sim.sim_gui_BC_Building_details", new Object[0]).getUnformattedText(), GuiStyle.TEXT);
        y += 18;
        GuiStyle.labelValue(this.fontRenderer, px + 12, px + 84, y, new TextComponentTranslation("container.sim.sim_gui_BC3", new Object[0]).getUnformattedText(), this.selectedBlueprint.name, GuiStyle.TEXT);
        y += 16;
        GuiStyle.labelValue(this.fontRenderer, px + 12, px + 84, y, new TextComponentTranslation("container.sim.sim_gui_BC7", new Object[0]).getUnformattedText(), this.selectedBlueprint.getDimensions(), GuiStyle.TEXT);
        y += 16;
        GuiStyle.labelValue(this.fontRenderer, px + 12, px + 84, y, new TextComponentTranslation("container.sim.sim_gui_BC6", new Object[0]).getUnformattedText(), ModSimLoader.displayMoney((float) this.selectedBlueprint.blockCount * 0.02F), GuiStyle.GREEN);
        y += 16;
        GuiStyle.labelValue(this.fontRenderer, px + 12, px + 84, y, new TextComponentTranslation("container.sim.sim_gui_BC5", new Object[0]).getUnformattedText(), this.selectedBlueprint.author, GuiStyle.TEXT);
        y += 20;
        GuiStyle.text(this.fontRenderer, px + 12, y, new TextComponentTranslation("container.sim.sim_gui_BC4", new Object[0]).getUnformattedText(), GuiStyle.TEXT_MUTED);
        y += 14;
        int maxY = py + ph - 34;
        for (String line : GuiLayout.wrap(this.fontRenderer, this.selectedBlueprint.desc, pw - 24)) {
            if (y > maxY) {
                break;
            }
            GuiStyle.text(this.fontRenderer, px + 12, y, line, GuiStyle.TEXT);
            y += 11;
        }
    }

    private int panelW() {
        return Math.min(440, this.width - 16);
    }

    private int panelH() {
        return Math.min(300, this.height - 16);
    }

    private int left() {
        return GuiLayout.centerX(this.width, panelW());
    }

    private int top() {
        return GuiLayout.centerY(this.height, panelH());
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 获取可雇佣的人名
     * @Date 16:20 2022/11/1
     * @Param []
     **/
    public void getHireableFolkNames() {
        NetWorkLoader.net.sendToServer(new PacketGetHireableFolks(true));
    }

    /** 建筑列表支持滚轮逐行浏览。 */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (this.currentPage != 1) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        this.buildingOffset += wheel > 0 ? -1 : 1;
        this.showPage();
    }

    /**
     * 列表行和表头不是 GuiButton，直接处理点击：
     * 点表头排序，点整行进入详情。
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.currentPage == 1 && mouseButton == 0) {
            int px = left();
            int py = top();
            int pw = panelW();
            int[] columns = this.columnLayout(px, pw);

            int headerY = this.headerY(py);
            if (mouseY >= headerY && mouseY < headerY + 14) {
                int field = -1;
                if (mouseX >= columns[0] && mouseX < columns[0] + columns[4]) {
                    field = SORT_NAME;
                } else if (mouseX >= columns[1] && mouseX < columns[1] + columns[5]) {
                    field = SORT_AREA;
                } else if (mouseX >= columns[2] && mouseX < columns[2] + columns[6]) {
                    field = SORT_COST;
                } else if (mouseX >= columns[3] && mouseX < columns[3] + columns[7]) {
                    field = SORT_AUTHOR;
                }
                if (field >= 0) {
                    if (this.sortMode == field) {
                        this.sortAscending = !this.sortAscending;
                    } else {
                        this.sortMode = field;
                        this.sortAscending = true;
                    }
                    this.buildingOffset = 0;
                    this.showPage();
                    return;
                }
            }

            int listTop = this.listTop(py);
            int listHeight = this.visibleRows * LIST_ROW_H;
            if (mouseX >= px + 8 && mouseX <= px + pw - 8
                    && mouseY >= listTop && mouseY < listTop + listHeight) {
                int row = (mouseY - listTop) / LIST_ROW_H;
                int index = this.buildingOffset + row;
                if (index >= 0 && index < this.potentialBlueprints.size()) {
                    this.selectedBlueprint = this.potentialBlueprints.get(index);
                    this.previousPage = 1;
                    this.currentPage = 11;
                    this.showPage();
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * @return void
     * @Author fan
     * @Description //TODO 搜索框
     * @Date 16:36 2022/11/1
     * @Param [c, i]
     **/
    @Override
    public void keyTyped(char c, int i) throws IOException {
        if (i == 1) {
            this.closeGui();
            return;
        }
        if (this.currentPage == 1 && this.tfSearch != null) {
            String before = this.tfSearch.getText();
            this.tfSearch.textboxKeyTyped(c, i);
            String after = this.tfSearch.getText();
            if (!before.equals(after)) {
                this.search = after;
                this.buildingOffset = 0;
                this.showPage();
            }
            return;
        }
        super.keyTyped(c, i);
    }
}
