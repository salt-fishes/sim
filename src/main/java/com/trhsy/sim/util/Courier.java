package com.trhsy.sim.util;

import com.trhsy.sim.loader.ModSimLoader;
import com.trhsy.sim.npcCode.V3;
import net.minecraftforge.common.DimensionManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 快递点/传送点（P9 修复）。
 *
 * <ul>
 *   <li>读写用显式 UTF-8 + try-with-resources（原来用平台编码且不关流，中文点名会乱码）；</li>
 *   <li>按主键精确解析（原来 line.contains("loc") 会串行）；</li>
 *   <li>removeFarm 真正从 theCourierPoints 移除（原来调用的是
 *       buildings.remove(this)，类型不符，恒为空操作）。</li>
 * </ul>
 */
public class Courier {
    public String ID;
    public V3 loc;
    public String name;

    public Courier(String id, V3 pos, String name) {
        this.ID = id;
        this.loc = pos;
        this.name = name;
    }

    public Courier(String uuid) {
        this.loadMine(uuid);
    }

    public void loadMine(String loadID) {
        new DimensionManager();
        File mineFolder = new File(ModSimLoader.getSavesDataFolder() + File.separator + "CourierPoints");
        if (!mineFolder.exists() && !mineFolder.mkdirs()) {
            ModSimLoader.log.warn("无法创建快递点存档目录: " + mineFolder.getAbsolutePath());
            return;
        }
        File file = new File(mineFolder.getAbsolutePath() + File.separator + loadID + ".sk2");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                int m1 = line.indexOf("|");
                if (m1 <= 0) {
                    continue;
                }
                String key = line.substring(0, m1);
                String value = line.substring(m1 + 1);
                if ("loc".equals(key)) {
                    this.loc = V3.fromString(value);
                } else if ("name".equals(key)) {
                    this.name = value;
                } else if ("id".equals(key)) {
                    this.ID = value;
                }
            }
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            ModSimLoader.log.error("Courier出错了：" + e.getMessage() + "行数：" + element.getLineNumber());
        }
    }

    /**
     * 移除一个快递点：内存列表 + 存档文件。
     *
     * @param uid 存档文件名（不含 .sk2）
     */
    public void removeFarm(String uid) {
        ModSimLoader.theCourierPoints.remove(this);
        File logFile = new File(ModSimLoader.getSavesDataFolder() + File.separator
                + "CourierPoints" + File.separator + uid + ".sk2");
        if (logFile.exists() && !logFile.delete()) {
            ModSimLoader.log.warn("无法删除快递点存档: " + logFile.getAbsolutePath());
        }
    }
}
