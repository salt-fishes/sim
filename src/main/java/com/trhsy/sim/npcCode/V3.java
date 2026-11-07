package com.trhsy.sim.npcCode;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.npc
 * @ClassName: V3
 * @Description:
 * @date 2022/10/11 17:55
 */
public class V3 {
    public double x;
    public double y;
    public double z;
    public int dimension;
    /**
     * 方块
     **/
    public Block blockID;
    /**
     * 元数据
     **/
    public int meta;

    public V3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public V3(double x, double y, double z, int dim) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dim;
    }

    public V3(double x, double y, double z, Block id, int meta) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.meta = meta;
    }

    public V3(BlockPos pos) {
        this.x = (double) pos.getX();
        this.y = (double) pos.getY();
        this.z = (double) pos.getZ();
    }

    public V3(BlockPos pos, int dim) {
        this.x = (double) pos.getX();
        this.y = (double) pos.getY();
        this.z = (double) pos.getZ();
        this.dimension = dim;
    }

    @Override
    public String toString() {
        return this.x + "," + this.y + "," + this.z + "," + this.dimension;
    }

    public String toStringByMeta() {
        return this.x + "," + this.y + "," + this.z + "," + this.meta;
    }

    public BlockPos toBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public int getDistanceTo(V3 other) {
        if (other == null) {
            return 0;
        } else {
            double dist = Math.sqrt((other.x - this.x) * (other.x - this.x) + (other.y - this.y) * (other.y - this.y) + (other.z - this.z) * (other.z - this.z));
//                        ModSimLoader.log.info( "【"+other.toString()+"】与地址【" + this.toString() + "】相距"+dist);
            return (int) dist;
        }
    }

    public static V3 fromString(String v3) {
        String[] values = v3.split(",");
        if (values.length < 3) {
            throw new IllegalArgumentException("非法 V3: " + v3);
        }
        int dim = 0;
        if (values.length >= 4) {
            try {
                dim = Integer.parseInt(values[3].trim());
            } catch (NumberFormatException ignored) {
                dim = 0;
            }
        }
        return new V3(Double.parseDouble(values[0].trim()), Double.parseDouble(values[1].trim()),
                Double.parseDouble(values[2].trim()), dim);
    }

    public static V3 fromBlockPos(BlockPos pos) {
        return new V3((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());
    }

    public static V3 fromVec3d(Vec3d vec3d) {
        return new V3(vec3d.x, vec3d.y, vec3d.z);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof V3) {
            V3 v3 = (V3) o;
            if (this.toBlockPos().equals(v3.toBlockPos()) && this.dimension == v3.dimension) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        // P11(S1): equals 用“方块坐标 + 维度”，hashCode 必须用同一组字段。
        int result = this.toBlockPos().hashCode();
        result = 31 * result + this.dimension;
        return result;
    }
    /**
     * 计算当前 V3 与另一个 V3 的距离平方（避免开方运算）
     * @param other 另一个 V3 坐标
     * @return 距离平方值（若 other 为 null 则返回无穷大）
     */
    public double distanceSq(V3 other) {
        if (other == null) {
            return Double.POSITIVE_INFINITY; // 或根据需求返回 0 或其他默认值
        }
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz; // 平方和
    }
}
