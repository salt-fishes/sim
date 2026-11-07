package com.trhsy.sim.gui.block;

import com.trhsy.sim.loader.ModSimClientLoader;
import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.gui.block
 * @ClassName: GuiBlockMarker
 * @Description:
 * @date 2023/5/4 16:58
 */
public class GuiBlockMarker extends GuiScreen {
    public V3 location;
    //错误提示
    public String errorText = "";
    private int dimension;

    public GuiBlockMarker(V3 location, int dimension) {
        this.location = location;
        this.dimension = dimension;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {

    }

    @Override
    public void initGui() {
        try {
            this.buttonList.clear();
            //完成
            this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height - 30, new TextComponentTranslation("container.sim.sim_gui_BC_Done", new Object[0]).getUnformattedText()));
            //复制结构/建造
            GuiButton b = new GuiButton(1, this.width / 2 - 100, 100, new TextComponentTranslation("container.sim.sim_gui_Copy_structure", new Object[0]).getUnformattedText());
            //复制结构/建造
            this.buttonList.add(b);
            b.enabled = false;

            if (ModSimClientLoader.markers.size() == 3) {
                (this.buttonList.get(1)).enabled = true;
            } /*else if (BlockMarker.markers.size() == 1) {
                (this.buttonList.get(2)).enabled = true;

            }*/
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiMarker-initGui出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public void drawScreen(int i, int j, float f) {
        try {
            this.drawDefaultBackground();
            //标记棒
            this.drawCenteredString(this.fontRenderer, new TextComponentTranslation("container.sim.Markers", new Object[0]).getUnformattedText(), this.width / 2, 30, 16777215);
            //标记棒可以用来复制建筑
            this.drawCenteredString(this.fontRenderer, new TextComponentTranslation("container.sim.Markers1", new Object[0]).getUnformattedText(), this.width / 2, 40, 10551295);
            //提示：使用后,在标记新区域之前,移除旧标记。
            this.drawCenteredString(this.fontRenderer, new TextComponentTranslation("container.sim.Markers4", new Object[0]).getUnformattedText(), this.width / 2, 85, 10551295);
            this.drawCenteredString(this.fontRenderer, this.errorText, this.width / 2, this.height - 50, 16711680);

            super.drawScreen(i, j, f);
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GuiMarker-drawScreen出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    @Override
    public void actionPerformed(GuiButton guibutton) {
        try {
            if (guibutton.id == 0) {
                this.mc.currentScreen = null;
                this.mc.setIngameFocus();
            } else {
                //复制结构/建造
                if (guibutton.displayString.contentEquals(new TextComponentTranslation("container.sim.sim_gui_Copy_structure", new Object[0]).getUnformattedText())) {
                    new ThreadFacsimile();
                    //复制它
                    this.mc.currentScreen = null;
                    this.mc.setIngameFocus();
                    return;
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("GUIMARKER-actionPerformed出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    public class ThreadFacsimile extends Thread {
        public ThreadFacsimile() {
            this.start();
        }

        @Override
        public void run() {

            try {
                V3 cxyz = ModSimClientLoader.markers.get(0).loc;
                V3 Lxyz = ModSimClientLoader.markers.get(1).loc;
                V3 Bxyz = ModSimClientLoader.markers.get(2).loc;

                V3 exyz = new V3(Math.floor(GuiBlockMarker.this.mc.player.posX), Math.floor(GuiBlockMarker.this.mc.player.posY), Math.floor(GuiBlockMarker.this.mc.player.posZ));
                int ltrCountx;
                if (cxyz.x == Lxyz.x) {
                    ltrCountx = (int) (Math.abs(Lxyz.z - cxyz.z) - 1);
                } else {
                    ltrCountx = (int) (Math.abs(Lxyz.x - cxyz.x) - 1);
                }

                int ftbCountx;
                if (cxyz.x == Bxyz.x) {
                    ftbCountx = (int) (Math.abs(Bxyz.z - cxyz.z) - 1);
                } else {
                    ftbCountx = (int) (Math.abs(Bxyz.x - cxyz.x) - 1);
                }

                if (ftbCountx != 0 && ltrCountx != 0) {
                    int cx = (int) cxyz.x;
                    int cy = (int) cxyz.y;
                    int cz = (int) cxyz.z;
                    int ex = (int) exyz.x;
                    int ey = (int) exyz.y;
                    int ez = (int) exyz.z;
                    int bxx = ex;
                    int byx = ey;
                    int bzx = ez;
                    if (cz == ez) {
                        if (cx > ex) {
                            bxx = cx + 1;
                        } else {
                            bxx = cx - 1;
                        }
                    } else {
                        if (cx != ex) {
                            //请面向主标记站立,后方标记在远处。
                            GuiBlockMarker.this.errorText = new TextComponentTranslation("container.sim.Markers11", new Object[0]).getUnformattedText();
                            //无法复制建筑,技术信息cx:
                            ModSimLoader.sendChat(new TextComponentTranslation("container.sim.Markers12", new Object[0]).getUnformattedText() + cx + ", cz=" + cz + ", ex=" + ex + ", ez=" + ez);
                            return;
                        }

                        if (cz > ez) {
                            bzx = cz + 1;
                        } else {
                            bzx = cz - 1;
                        }
                    }

                    int xo = 0;
                    int zo = 0;
                    HashMap key = new HashMap();
                    key.put("minecraft:air,0", "A");
                    CopyOnWriteArrayList layerLines = new CopyOnWriteArrayList();
                    int ch = 66;

                    String keyString = "A=minecraft:air,0;";
                    int ltr;
                    int zzz;
                    for (int l = 0; l < 2000; ++l) {
                        String layerLine = "";
                        boolean allAirBlocks = true;
                        for (int ftb = 0; ftb < ftbCountx; ++ftb) {
                            for (ltr = 1; ltr <= ltrCountx; ++ltr) {
                                if (cz == ez) {
                                    if (cx > ex) {
                                        xo = ftb;
                                        zo = ltr;
                                    } else {
                                        xo = -ftb;
                                        zo = -ltr;
                                    }
                                } else if (cx == ex) {
                                    if (cz > ez) {
                                        xo = -ltr;
                                        zo = ftb;
                                    } else {
                                        xo = ltr;
                                        zo = -ftb;
                                    }
                                }

                                int xxx = bxx + xo;
                                int yyy = byx + l - 1;
                                zzz = bzx + zo;
                                BlockPos blockPos = new BlockPos(xxx, yyy, zzz);
                                IBlockState blocks = GuiBlockMarker.this.mc.getIntegratedServer().getWorld(dimension).getBlockState(blockPos);
//                                int id = Block.getiDFromBlock(blocks.getBlock());
                                Block block = blocks.getBlock();
                                String id = block.toString();
                                id = id.substring(id.indexOf("{") + 1, id.indexOf("}"));
                                int meta = block.getMetaFromState(blocks);
                                String letter = "";
                                letter = (String) key.get(id + "," + meta);
                                if (key.get(id + "," + meta) == null) {
                                    ++ch;
                                    key.put(id + "," + meta, (new Character((char) ch)).toString());
                                    keyString = keyString + (new Character((char) ch)).toString() + "=" + id + "," + meta + ";";
                                    letter = (new Character((char) ch)).toString();
                                }
                                layerLine = layerLine + letter;
                                if (!"minecraft:air".equals(id)) {
                                    allAirBlocks = false;
                                }
                            }
                        }

                        if (allAirBlocks) {
                            break;
                        }

                        layerLines.add(layerLine);
                    }

                    if (layerLines.size() == 0) {
                        String fs_15=new TextComponentTranslation("container.sim.Markers13", new Object[0]).getUnformattedText();
                        ModSimLoader.sendChat(fs_15);//错误,无法捕获所有方块,请尝试靠近标记站并重试
                        return;
                    }
                    String lang = FMLCommonHandler.instance().getCurrentLanguage();
                    File check = new File(ModSimLoader.getSimFolder()+ File.separator + "buildings_"+lang);

                    if (!check.exists()) {
                        String fs_14=new TextComponentTranslation("container.sim.Markers14",new Object[0]).getUnformattedText();
                        ModSimLoader.sendChat(ModSimLoader.getSimFolder()+ File.separator + "buildings_"+lang+"::::"+fs_14);
                        return;
                    }

                    String f = String.valueOf(System.currentTimeMillis());
                    ltr = f.length();
                    f = f.substring(ltr - 6);
                    FileWriter fstream = new FileWriter(ModSimLoader.getSimFolder() +File.separator+ "buildings_"+lang+"/other/My Build" + f + ".txt");
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(ltrCountx + "x" + ftbCountx + "x" + layerLines.size() + "\r\n");
                    out.write(keyString + "\r\n");

                    for (zzz = 0; zzz < layerLines.size(); ++zzz) {
                        out.write(layerLines.get(zzz).toString() + "\r\n");
                    }
                    out.close();
                    // P12(S2): 删掉客户端 GUI 线程的 Thread.sleep(500)，避免保存蓝图时卡渲染半秒。
                    String fs_15=new TextComponentTranslation("container.sim.Markers15",new Object[0]).getUnformattedText();
                    String fs_16=new TextComponentTranslation("container.sim.Markers16",new Object[0]).getUnformattedText();
                    ModSimLoader.sendChat(fs_15 + f + fs_16);
//                    SoundEvent soundEvent = new SoundEvent(new ResourceLocation(ModSim.MODID + ":computer"));
//                    mc.world.playSound(entityPlayer, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
                } else {
                    String fs_17=new TextComponentTranslation("container.sim.Markers17",new Object[0]).getUnformattedText();
                    ModSimLoader.sendChat(fs_17);
                }
            } catch (Exception e) {
                StackTraceElement element = e.getStackTrace()[0];
                ModSimLoader.log.error("ThreadFacsimile出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            }
        }
    }
}
