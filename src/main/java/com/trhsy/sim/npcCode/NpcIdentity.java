package com.trhsy.sim.npcCode;

import java.util.UUID;

/**
 * @author Trhsy
 * @Package: com.trhsy.sim.util.entity
 * @ClassName: NpcIdentity
 * @Description: NPC实体工具类
 * @date 2022/10/9 10:50
 */
public class NpcIdentity {
    /**id**/
    public UUID id;
    /**姓名**/
    public String name;
    /**年龄**/
    public String age;
    /**状态**/
    public String status;
    /**工作**/
    public String job;
    /**住房**/
    public String house;
    /**人际关系**/
    public String relationship;
    /**饥饿程度**/
    public String hunger;
    /**成年**/
    public String maturityAge;
    /**皮肤**/
    public String skinPath;
    /**是否死亡**/
    public boolean isDead;
    /**个人存款**/
    public float money;
    /**岗位工资（每分钟）**/
    public float wage;
    /**稳定职业 ID，例如 vocation6**/
    public String jobId;

    public NpcIdentity(UUID id, String name, String age, String status, String job, String house, String relationship, String hunger, String maturityAge, String skinPath, boolean isDead) {
        this(id, name, age, status, job, house, relationship, hunger, maturityAge, skinPath, isDead, 0.0F, 0.0F, "");
    }

    public NpcIdentity(UUID id, String name, String age, String status, String job, String house, String relationship, String hunger, String maturityAge, String skinPath, boolean isDead, float money, float wage, String jobId) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.status = status;
        this.job = job;
        this.house = house;
        this.relationship = relationship;
        this.hunger = hunger;
        this.maturityAge = maturityAge;
        this.skinPath = skinPath;
        this.isDead = isDead;
        this.money = money;
        this.wage = wage;
        this.jobId = jobId == null ? "" : jobId;
    }

    public NpcIdentity(String combinedInfo) {
        String[] parts = combinedInfo.split(",_,");
        this.id = UUID.fromString(parts[0]);
        this.name = parts[1];
        this.age = parts[2];
        this.status = parts[3];
        this.job = parts[4];
        this.house = parts[5];
        this.relationship = parts[6];
        this.hunger = parts[7];
        this.maturityAge = parts[8];
        this.skinPath = parts[9];
        this.isDead = Boolean.parseBoolean(parts[10]);
        this.money = parts.length > 11 ? parseFloat(parts[11]) : 0.0F;
        this.wage = parts.length > 12 ? parseFloat(parts[12]) : 0.0F;
        this.jobId = parts.length > 13 ? parts[13] : "";
    }

    private static float parseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (Exception ignored) {
            return 0.0F;
        }
    }

    @Override
    public String toString() {
        return this.id + ",_," + this.name + ",_," + this.age + ",_," + this.status + ",_," + this.job + ",_," + this.house + ",_," + this.relationship + ",_," + this.hunger + ",_," + this.maturityAge + ",_," + this.skinPath + ",_," + this.isDead + ",_," + this.money + ",_," + this.wage + ",_," + this.jobId;
    }
}
