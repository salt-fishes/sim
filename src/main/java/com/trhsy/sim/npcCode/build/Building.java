package com.trhsy.sim.npcCode.build;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.NpcData;
import com.trhsy.sim.npcCode.V3;
import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc
 * @ClassName: Building
 * @Description: 建筑
 * @date 2022/10/13 17:30
 */
public class Building {
    public UUID ID = null;
    public String buildingName = "";
    public String buildingType = "NULL";
    public String jobType = "";
    public String author = "Trhsy";
    public int length = 0;
    public int width = 0;
    public int height = 0;
    public int dimension = 0;
    public float rent = 0.0F;
    public V3 controlXYZ;
    public V3 livingXYZ;
    public List<V3> structure = new CopyOnWriteArrayList<>();
    public List<NpcData> occupants = new CopyOnWriteArrayList();
    public BlockPos bed;
    public BlockPos furnace;
    public BlockPos craftingTable;
    public BlockPos buyingPos;
    public boolean markedForDeletion;
    /**描述**/
    public String desc;
    //特除的空气方块
    public List<V3> blockSpecial = new CopyOnWriteArrayList();

    /**
     * @Author fan
     * @Description //TODO
     * @Date 22:00 2022/11/7
     * @Param [bName, rent, ctrl, lv] 建筑 租金 控制箱 生活区
     * @return
     **/
    public Building(String bName, float rent, V3 ctrl, V3 lv) {
        this.buildingName = bName;
        this.controlXYZ = ctrl;
        this.livingXYZ = lv;
        this.rent = rent;
        this.ID = UUID.randomUUID();
        ModSimLoader.log.info(this.ID.toString());
        this.saveBuilding();
    }

    public Building(int l, int w, int h, float rent, V3 ctrl, V3 lv) {
        this.length = l;
        this.width = w;
        this.height = h;
        this.rent = rent;
        this.controlXYZ = ctrl;
        this.livingXYZ = lv;
        this.ID = UUID.randomUUID();
        this.saveBuilding();
    }

    public Building(World world, UUID uuid) {
        this.loadBuilding(world, uuid);
    }
    /**
     * @Author fan
     * @Description //TODO 保存建筑
     * @Date 18:59 2022/11/6
     * @Param []
     * @return void
     **/
    public void saveBuilding() {
        if (this.markedForDeletion || this.ID == null) {
            return;
        }
        new DimensionManager();
        File buildingFolder = new File(ModSimLoader.getSavesDataFolder() + File.separator + "buildings");
        if (!buildingFolder.exists()) {
            buildingFolder.mkdirs();
        }
        File target = new File(buildingFolder + File.separator + this.ID + ".sk2");
        File tmp = new File(target.getAbsolutePath() + ".tmp");
        try {
            // P13(S1): 先写临时文件，再原子替换，避免自动存档中途崩溃留下半个建筑文件。
            StringBuilder sb = new StringBuilder();
            sb.append("id|").append(this.ID.toString()).append('\n');
            sb.append("name|").append(this.buildingName).append('\n');
            sb.append("type|").append(this.buildingType).append('\n');
            sb.append("desc|").append(this.desc).append('\n');
            sb.append("dim|").append(this.length).append(',').append(this.width).append(',').append(this.height).append('\n');
            sb.append("dimension|").append(this.dimension).append('\n');
            sb.append("rent|").append(this.rent).append('\n');
            sb.append("jobtypes|").append(this.jobType).append('\n');
            if (this.controlXYZ != null) {
                sb.append("cpos|").append(this.controlXYZ.toString()).append('\n');
            }
            if (this.livingXYZ != null) {
                sb.append("lpos|").append(this.livingXYZ.toString()).append('\n');
            }
            sb.append("blockspecial|");
            for (V3 pos : this.blockSpecial) {
                sb.append(pos.toStringByMeta()).append(';');
            }
            sb.append("\nstructure|");
            for (V3 pos : this.structure) {
                sb.append(pos.toString()).append(';');
            }
            sb.append("\noccupants|");
            for (NpcData folk : this.occupants) {
                if (folk != null) {
                    sb.append(folk.ID).append(';');
                }
            }
            Files.write(tmp.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("建筑保存，出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }
    /**
     * @Author fan
     * @Description //TODO 加载建筑
     * @Date 18:58 2022/11/6
     * @Param [world, loadID]
     * @return void
     **/
    public void loadBuilding(World world, UUID loadID) {
        for (Building b:ModSimLoader.buildings){
            if(b.ID.toString().equals(loadID)){
                ModSimLoader.buildings.remove(b);
                ModSimLoader.buildings.add(this);
            }
        }

        File buildingFolder = new File(ModSimLoader.getSavesDataFolder() + File.separator + "buildings");
        if(!buildingFolder.exists()){
            buildingFolder.mkdirs();
        }

        try {
            //BufferedReader reader = new BufferedReader(new FileReader(buildingFolder.getAbsolutePath() + File.separator + loadID + ".sk2"));
            InputStream inputStream =new FileInputStream(new File(buildingFolder.getAbsolutePath() + File.separator + loadID + ".sk2"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();

            while(true) {
                while(line != null) {
                    int m1 = line.indexOf("|");
                    line.substring(0, m1);
                    String value = line.substring(m1 + 1);
                    if (line.contains("id|")) {
                        this.ID = UUID.fromString(value);
                    } else if (line.contains("name|")) {
                        this.buildingName = value;
                    } else if (line.contains("type|")) {
                        this.buildingType = value;
                    }  else if (line.contains("desc|")) {
                        this.desc = value;
                    } else if (line.contains("dim|")) {
                        this.length = Integer.valueOf(value.split(",")[0]);
                        this.width = Integer.valueOf(value.split(",")[1]);
                        this.height = Integer.valueOf(value.split(",")[2]);
                    } else if (line.contains("dimension")) {
                        this.dimension = Integer.valueOf(value);
                    } else if (line.contains("rent")) {
                        this.rent = Float.valueOf(value);
                    } else if (line.contains("jobtypes|")) {
                        this.jobType = value;
                    } else if (line.contains("cpos")) {
                        this.controlXYZ = V3.fromString(value);
                    } else if (line.contains("lpos")) {
                        this.livingXYZ = V3.fromString(value);
                    } else {
                        String[] folk;
                        //结构
                        if (line.contains("structure")) {
                            if (value.length() < 1) {
                                line = reader.readLine();
                                continue;
                            }

                            String[] fsStructure= value.split(";");
                            for (int i = 0; i < fsStructure.length; i++) {
                                String f = fsStructure[i];
                                this.structure.add(V3.fromString(f));
                            }
                            //特除方块
                        } else if(line.contains("blockspecial")){
                            if (value.length() < 1) {
                                line = reader.readLine();
                                continue;
                            }

                            String[] fsBlockSpecial = value.split(";");
                            for (int i = 0; i <fsBlockSpecial.length ; i++) {
                                String f=fsBlockSpecial[i];
                                String[]  fs_f=f.split(",");
                                double x= Double.parseDouble(fs_f[0]);
                                double y= Double.parseDouble(fs_f[1]);
                                double z= Double.parseDouble(fs_f[2]);
                                int m= Integer.parseInt(fs_f[3]);
                                V3 v=new V3(x,y,z,m);
                                this.blockSpecial.add(v);
                            }
                            //居住者
                        }else if (line.contains("occupants")) {
                            if (value.length() < 1) {
                                line = reader.readLine();
                                continue;
                            }
                            String[] fsFolk = value.split(";");
                            for (int i = 0; i < fsFolk.length; i++) {
                                String f=fsFolk[i];
                                NpcData fd = ModSimLoader.getFolkDataByUID(UUID.fromString(f));
                                if(fd!=null){
                                    this.occupants.add(fd);
                                    if(this.buildingType!=null&&this.buildingType.equals(new TextComponentTranslation("container.sim.sim_gui_BC_Residential",new Object[0]).getUnformattedText())){
                                        fd.home = this;
                                    }
                                }
                            }
                        }
                    }

                    line = reader.readLine();
                }
                reader.close();
                break;
            }
        } catch (Exception var15) {
            StackTraceElement element = var15.getStackTrace()[0];
            ModSimLoader.log.error("loadBuilding出错了：" + var15.getMessage() + "行数：" + element.getLineNumber());
        }

    }
    /**
     * @Author fan
     * @Description //TODO 建筑拆除
     * @Date 10:23 2022/11/8
     * @Param [world, removeStructure]
     * @return void
     **/
    public void demolish(World world, boolean removeStructure) {
        this.markedForDeletion = true;
        // P10(S0): 先复制住户列表再 evict。evict 会从 occupants 里 remove，
        // 直接在原列表上用下标递增会跳过一半住户。
        List<NpcData> occupantsCopy = new ArrayList<>(this.occupants);
        for (NpcData npcData : occupantsCopy) {
            if (npcData != null) {
                npcData.evict();
            }
        }
        // P10(S0): V3 必须用 equals 比较（原来 == 永远不成立，工人不会被解雇）。
        for (NpcData fd : new ArrayList<>(ModSimLoader.folks)) {
            if (fd != null && fd.job != null && fd.job.workPlace != null
                    && fd.job.workPlace.equals(this.controlXYZ)) {
                fd.fire();
            }
        }
        if (removeStructure) {
            for (V3 v3:this.structure){
                //播放拆除音效
//                world.playSound(v3.x,v3.y,v3.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1, 1,false);
                BlockPos block=v3.toBlockPos();
                // P10(S0): 未加载区块不拆除，避免隐式加载。
                if (!world.isBlockLoaded(block)) {
                    continue;
                }
                world.destroyBlock(block,false);
//                world.setBlockToAir(block);
            }
        }

        removeBuilding(this.ID);
    }
    /**
     * @Author fan
     * @Description //TODO 旋转楼梯
     * @Date 21:11 2023/3/21
     * @Param []
     * @return void
     **/
    public void rotateStairs(World world) {
        for (V3 v3:this.structure){
            BlockPos blockPos=v3.toBlockPos();
            Block id = world.getBlockState(blockPos).getBlock();
            ItemStack is = new ItemStack(world.getBlockState(blockPos).getBlock(), 1, id.getMetaFromState(world.getBlockState(blockPos)));
            if (is != null && is.getItem() != null) {
                EnumFacing facing=EnumFacing.NORTH;
                int newmeta =0;
                //楼梯
                if(Block.getBlockFromItem(is.getItem()) instanceof BlockStairs){
                    newmeta = is.getMetadata();
                    if (newmeta == 0) {
                        newmeta = 1;
                        facing=EnumFacing.SOUTH;
                    } else if (newmeta == 1) {
                        newmeta = 2;
                        facing=EnumFacing.WEST;
                    } else if (newmeta == 2) {
                        newmeta = 3;
                        facing=EnumFacing.EAST;
                    } else if (newmeta == 3) {
                        newmeta = 0;
                        facing=EnumFacing.NORTH;
                    }
                    world.setBlockState(blockPos,id.getDefaultState().withProperty(BlockWallSign.FACING, facing),3);
//                    world.setBlockMetadataWithNotify(blockLoc.x.intValue(), blockLoc.y.intValue(), blockLoc.z.intValue(), newmeta, 3);
                }
                //火把
                if(Block.getBlockFromItem(is.getItem()) instanceof BlockTorch){
                    newmeta = is.getMetadata();
                    facing= EnumFacing.NORTH;
                    if (newmeta == 1) {
                        newmeta = 2;
                        facing=EnumFacing.SOUTH;
                    } else if (newmeta == 2) {
                        newmeta = 3;
                        facing=EnumFacing.WEST;
                    } else if (newmeta == 3) {
                        newmeta = 4;
                        facing=EnumFacing.EAST;
                    } else if (newmeta == 4) {
                        newmeta = 1;
                        facing=EnumFacing.NORTH;
                    }
                    world.setBlockState(blockPos,id.getDefaultState().withProperty(BlockWallSign.FACING, facing),3);
                }
                //床
                if(Block.getBlockFromItem(is.getItem()) instanceof BlockBed){
                    newmeta = is.getMetadata();
                    ++newmeta;
                    if (newmeta == 4) {
                        newmeta = 0;
                        facing= EnumFacing.DOWN;
                    }
                    world.setBlockState(blockPos,id.getDefaultState().withProperty(BlockWallSign.FACING, facing),2);
                }
                //活塞
                if(Block.getBlockFromItem(is.getItem()) instanceof BlockPistonBase && Block.getBlockFromItem(is.getItem()) instanceof BlockPistonExtension){
                    newmeta = is.getMetadata();
                    if (newmeta == 2) {
                        newmeta = 5;
                        facing= EnumFacing.EAST;
                    } else if (newmeta == 5) {
                        newmeta = 3;
                        facing= EnumFacing.SOUTH;
                    } else if (newmeta == 3) {
                        newmeta = 4;
                        facing= EnumFacing.WEST;
                    } else if (newmeta == 4) {
                        newmeta = 2;
                        facing= EnumFacing.NORTH;
                    }
                    world.setBlockState(blockPos,id.getDefaultState().withProperty(BlockWallSign.FACING, facing),3);
                }
                //墙壁标志
                if (Block.getBlockFromItem(is.getItem()) instanceof BlockWallSign) {
                    newmeta = is.getMetadata();
                    if (newmeta == 0) {
                        newmeta = 4;
                        facing= EnumFacing.NORTH;
                    } else if (newmeta == 4) {
                        newmeta = 8;
                        facing= EnumFacing.SOUTH;
                    } else if (newmeta == 8) {
                        newmeta = 12;
                        facing= EnumFacing.WEST;
                    } else if (newmeta == 12) {
                        newmeta = 0;
                        facing= EnumFacing.DOWN;
                    }

                    world.setBlockState(blockPos,id.getDefaultState().withProperty(BlockWallSign.FACING, facing),2);
                }
                //按钮
                if (Block.getBlockFromItem(is.getItem()) instanceof BlockButton) {
                    newmeta = is.getMetadata();
                    if (newmeta == 1) {
                        newmeta = 3;
                        facing= EnumFacing.SOUTH;
                    } else if (newmeta == 3) {
                        newmeta = 2;
                        facing= EnumFacing.NORTH;
                    } else if (newmeta == 2) {
                        newmeta = 4;
                        facing= EnumFacing.WEST;
                    } else if (newmeta == 4) {
                        newmeta = 1;
                        facing= EnumFacing.UP;
                    }
                    world.setBlockState(blockPos,id.getDefaultState().withProperty(BlockWallSign.FACING, facing),3);
                }
                //栅栏门
                if (Block.getBlockFromItem(is.getItem()) instanceof BlockFenceGate) {
                    newmeta = is.getMetadata();
                    ++newmeta;
                    facing= EnumFacing.WEST;
                    if (newmeta > 3) {
                        newmeta = 0;
                        facing= EnumFacing.NORTH;
                    }
                    world.setBlockState(blockPos,id.getDefaultState().withProperty(BlockWallSign.FACING, facing),3);
                }
            }
        }
    }
    /**
     * @Author fan
     * @Description //TODO 移除建筑
     * @Date 20:54 2022/11/14
     * @Param [uid]
     * @return void
     **/
    public void removeBuilding(UUID uid){
        new DimensionManager();
        ModSimLoader.buildings.remove(this);
        File logFile = new File(ModSimLoader.getSavesDataFolder() + File.separator + "buildings" + File.separator + uid + ".sk2");
        if(logFile.delete()){
            logFile.deleteOnExit();
        }
    }

    /**
     * 获得空方块
     *
     * @param meta
     * @return
     */
    public List<V3> getSpecialBlocks(int meta) {
        List<V3> ret = new CopyOnWriteArrayList();
        try {
            for (V3 v3 : this.blockSpecial) {
                int fs_meta=v3.meta;
                if ( fs_meta== meta) {
                    ret.add(v3);
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("建筑getSpecialBlocks出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
        return ret;
    }

}
