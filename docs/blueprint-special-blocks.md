# 蓝图放置后门、床、告示牌、楼梯、火把、画异常 · 诊断报告

日期：2026-11-10  
范围：`JobBuilder` 的蓝图施工逻辑、`BuildingBlueprint` / `Structure` 数据结构、现有 346 个中文蓝图。  
状态：**只做诊断，没有修改代码。**

## 一、结论

这不是某一个方块的小 bug，而是三层问题叠在一起：

1. **蓝图只存了 BlockState + meta，没有存 TileEntity NBT，也没有存实体。**
   - 告示牌文字、床的颜色、花盆内容、箱子内容等都在 TileEntity 里；
   - 画和物品展示框是实体，不是方块，蓝图里根本没有它们的记录。
2. **门和床走了硬编码的特殊分支，完全忽略蓝图里的朝向、铰链、颜色、半块信息。**
   - 门固定用 `rotateFacing(NORTH)` 和 `ItemDoor.placeDoor`；
   - 床固定用 `Blocks.BED` 默认状态、固定朝西；
   - 门/床的上下两半在蓝图里是两条记录，但两条记录是同一个 Block，第二条会被“同 Block 跳过”逻辑吃掉。
3. **普通方块的放置只比较 Block，不比较 BlockState。**
   - 目标位置只要已经是同一种方块，哪怕朝向、半砖、形状全错，也会被当成“已经放好”；
   - 楼梯、火把、墙上的告示牌、活板门、栅栏门都吃这个亏；
   - 通用属性旋转循环还会在找到 `facing` 之前先把未旋转的状态放下去，之后因为“同 Block”而跳过修正。

## 二、现有蓝图里这些方块的数量

扫描 `assets/sim/buildings/buildings_zh_cn` 下 346 个 `.txt`：

- `wooden_door`：87 个文件
- `bed`：83 个文件
- `wall_sign`：46 个文件
- `standing_sign`：3 个文件
- `oak_stairs`：88 个文件；`stone_stairs`：34 个文件；还有其他材质楼梯
- `torch`：146 个文件
- `painting`：**0 个文件**
- `item_frame`：**0 个文件**

所以：

- 门、床、告示牌、楼梯、火把是“蓝图里有，但放置逻辑不完整”；
- 画不是“放歪了”，而是**蓝图根本不包含画**。画如果消失，通常是建筑工替换/清空它所在的墙，画失去附着后掉落。

## 三、逐项根因

### 1. 门

代码位置：`JobBuilder` 的门分支。

```java
if (fs_st_block instanceof BlockDoor && newBlock != fs_st_block) {
    normalBlock = false;
    ItemDoor.placeDoor(world, newBP, rotateFacing(EnumFacing.NORTH), fs_st_block, false);
}
```

问题：

1. **完全不读蓝图的 door metadata**
   - `fs_structure.getMeta()` 没参与；
   - 朝向固定 `rotateFacing(NORTH)`；
   - 铰链（左/右）、开启状态、上下半块信息全部丢失。
2. **`ItemDoor.placeDoor` 会一次放上下两半**
   - 蓝图里上半门和下半门是两条记录；
   - 先处理下半门时：`placeDoor` 放好两半，后面上半条目因为 `fs_block == fs_st_block` 被外层跳过，勉强正确；
   - 先处理上半门时：`placeDoor` 会把“下半”放在上半位置，整扇门整体上移一格，还会多出一截；
   - 如果下半门放置失败（下方没有支撑、空间被占），上半条目也不会补救。
3. **`newBlock != fs_st_block` 判断的是“下方方块”**
   - 它不是在判断“这个位置是否已经有一扇正确的门”；
   - 下方是门时直接跳过，可能留下缺上半扇的门。
4. **同 Block 跳过**
   - 目标位置已有同材质门但朝向/铰链错，外层 `fs_block != fs_st_block` 为 false，永远不会修正。

结果：门可能整体偏移、缺上半、朝向错、铰链错，甚至两扇门叠在一起。

### 2. 床

代码位置：`JobBuilder` 的床分支。

```java
EnumFacing bedFacing = rotateFacing(EnumFacing.WEST);
IBlockState iblockstate1 = Blocks.BED.getDefaultState()
    .withProperty(BlockBed.OCCUPIED, false)
    .withProperty(BlockBed.FACING, bedFacing)
    .withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT);
```

问题：

1. **颜色丢失**
   - 1.12 的床颜色在 `TileEntityBed` 里；
   - `Structure` 不存 TileEntity NBT；
   - 这里直接用 `Blocks.BED.getDefaultState()`，蓝图里的 `bed,3`、`bed,11` 等颜色信息没有写入；
   - 结果通常是默认颜色，或者所有床都变成同一种颜色。
2. **朝向固定朝西**
   - `bedFacing = rotateFacing(WEST)`，不读蓝图 metadata 里的朝向。
3. **上下两半同样重复处理**
   - 蓝图里 `bed,0` 和 `bed,8` 是两条记录；
   - 第一次处理就放了 foot + head，第二条因为同 Block 被跳过；
   - 如果第一条是 head，就会把 foot 放在 head 的位置，整张床偏移。
4. **`bedPos` 记录的是第一次遇到的床位置**
   - 如果顺序颠倒，NPC 的睡觉点会记在错误的一侧。
5. **同 Block 跳过**
   - 目标位置已有床但朝向/颜色错，外层直接跳过，不修正。

### 3. 告示牌

代码位置：`Structure` + `JobBuilder` 通用放置。

问题：

1. **没有 TileEntitySign 数据**
   - `Structure` 只有 `IBlockState` 和 `int meta`；
   - 告示牌文字、颜色、发光文字都在 `TileEntitySign`；
   - 建筑工用 `setBlockState` 放下去，只会生成一个**空白告示牌**。
2. **墙上的告示牌朝向可能对，但文字一定丢**
   - `BlockWallSign` 有 `FACING` 属性，通用旋转逻辑可能把朝向转对；
   - 但 TileEntity 没有写入，文字是空的。
3. **立式告示牌朝向基本不对**
   - `BlockStandingSign` 用的是整数 `ROTATION` 属性，不是 `facing`；
   - 通用逻辑只找名为 `facing` 的属性；
   - 找不到就走到 `withRotation(CLOCKWISE_180)`，而 `withRotation` 不会改 `ROTATION` 整数；
   - 结果立式告示牌基本都是默认朝向。
4. **同 Block 跳过**
   - 目标位置已有告示牌时，不会替换成蓝图里的朝向/文字。

### 4. 楼梯

代码位置：`JobBuilder` 通用属性放置循环。

问题：

1. **只比较 Block，不比较 BlockState**
   - 目标位置已有同材质楼梯，即使 `facing`、`half`、`shape` 全错，也会被当成已放置；
   - 外层 `fs_block != fs_st_block` 直接跳过。
2. **通用循环会先把未旋转状态放下去**
   - 循环遍历 `st.getProperties()`；
   - 如果第一个属性不是 `facing`（例如先遍历到 `half` 或 `shape`），代码会执行：
     ```java
     world.setBlockState(newBP, st);
     ```
     先把**未旋转**的楼梯放下去；
   - 之后找到 `facing`，计算旋转，但设置前又判断：
     ```java
     if (!this.isSameBlock(world.getBlockState(newBP).getBlock(), fs_st_block)) {
         world.setBlockState(newBP, st);
     }
     ```
     此时世界里已经是同一种楼梯 Block，`isSameBlock` 返回 true，**旋转后的状态反而不会被放置**。
3. **`withRotation` 不会旋转楼梯的 `SHAPE`**
   - 楼梯有 `STRAIGHT / INNER_LEFT / INNER_RIGHT / OUTER_LEFT / OUTER_RIGHT`；
   - `IBlockState.withRotation` 只旋转 `EnumFacing` 属性，不会旋转 `EnumShape`；
   - 转角楼梯的朝向和形状会错位。
4. **`half=top`、`shape` 等状态没有单独校验**
   - 即使放下去，也可能是上半砖/下半砖、直梯/转角梯错配。

结果：楼梯常见“朝向反了”“转角变成直梯”“该上半砖的变成下半砖”“已有楼梯不修正”。

### 5. 火把

代码位置：`JobBuilder` 通用属性放置循环。

问题：

1. **同 Block 跳过**
   - 目标位置已有火把但朝向错，不会修正。
2. **通用循环的“先放未旋转状态”问题**
   - 火把通常只有 `facing` 一个属性，所以比楼梯轻；
   - 但如果属性遍历顺序或状态组合变化，仍可能先放默认状态，再因为同 Block 跳过旋转。
3. **支撑方块顺序问题**
   - 蓝图按层/按结构顺序放置；
   - 如果火把先于它附着的墙/地板被放，火把可能悬空，随后邻居更新时掉落；
   - 建筑工不会在放火把前检查 `canPlaceBlockAt`，也不会在支撑块放好后补放。
4. **墙上的火把和地面火把的 `facing` 不同**
   - 地面火把是 `UP`，墙上火把是水平方向；
   - 旋转逻辑对 `UP` 不处理，对水平方向处理；但没有状态比较，错了也不会修。

### 6. 画 / 物品展示框

代码位置：`Structure` / `JobBuilder` 清空与放置逻辑。

问题：

1. **蓝图里根本没有画**
   - 扫描 346 个蓝图，`painting` 和 `item_frame` 都是 0；
   - `Structure` 只存方块，画是 `EntityPainting`，物品展示框是 `EntityItemFrame`，都不在蓝图数据里；
   - 建筑工不可能“按蓝图放画”。
2. **画消失的真实原因通常是墙被替换**
   - 建筑工清空目标方块时：
     ```java
     world.setBlockState(newBP, Blocks.AIR.getDefaultState(), 3);
     ```
   - 画附着在墙上，墙被替换/清空后，画失去支撑，原版逻辑会让它掉落；
   - `Block.getDrops` 只处理方块掉落，不处理画/展示框实体；
   - 建筑工不会把画重新挂回去。
3. **`getTileEntity(newBP) == null` 的保护只保护方块 TileEntity**
   - 它不保护画/展示框这类实体；
   - 如果画所在位置正好是蓝图要放方块的位置，画会被挤掉。

## 四、为什么“有时看起来正常”

- 在完全平坦、空地上，门/床的固定朝向可能刚好和蓝图方向一致；
- 楼梯的属性遍历顺序如果刚好先遇到 `facing`，可能转对；
- 火把如果支撑块先放，可能看起来正常；
- 一旦出现以下情况，问题就暴露：
  - 建筑旋转了 90°/180°/270°；
  - 目标位置本来就有同类型方块；
  - 门/床的上下半条目顺序不是预期；
  - 蓝图里有转角楼梯、立式告示牌、带颜色的床；
  - 建筑工清空/替换了画所在的墙。

## 五、修复方向（建议，未实施）

### 短期（不动蓝图格式）

1. **放置比较改成 BlockState 比较**
   - 不再用 `fs_block != fs_st_block` / `isSameBlock(Block, Block)`；
   - 对门、床、楼梯、火把、告示牌等，比较完整 `IBlockState`，状态不对就修正。
2. **门/床改成状态感知放置**
   - 门：读 `fs_structure.getiBlockState()`，按 `HALF` 放上/下半，保留 `FACING`、`HINGE`、`OPEN`；不要无脑 `ItemDoor.placeDoor`。
   - 床：读蓝图状态，按 `PART` 放 foot/head，保留 `FACING`；写 `TileEntityBed` 颜色。
3. **告示牌先补 TileEntity**
   - 短期至少让立式告示牌按 `ROTATION` 放对；
   - 如果要文字，必须扩展蓝图存 `TileEntitySign` 的 NBT。
4. **楼梯/活板门/栅栏门用一次旋转**
   - 先对完整 `IBlockState` 做一次 `withRotation`，再放置；
   - 不要在属性循环里提前 `setBlockState`；
   - 对 `SHAPE` 做额外旋转/映射。
5. **保护画和展示框**
   - 清空/替换方块前检查该位置及相邻面是否有 `EntityPainting` / `EntityItemFrame`；
   - 有则先安全移除/掉落，或跳过该方块的替换。

### 中期（改蓝图格式）

1. `Structure` 增加 `NBTTagCompound tileEntityNbt`；
2. 蓝图格式增加 `entities` 段，记录画/展示框的类型、相对位置、朝向、NBT；
3. `BuildingBlueprint` 解析时保留这些数据，`JobBuilder` 放置后恢复 TileEntity/生成实体；
4. 旧蓝图保持兼容，缺数据时退回当前行为。

## 六、建议的验收用例

1. 同一栋带门/床/告示牌/楼梯/火把的测试房，分别按 0°/90°/180°/270° 建造，检查：
   - 门上下半完整、朝向/铰链正确；
   - 床 foot/head 正确、颜色正确、NPC 能睡；
   - 告示牌朝向正确、文字存在；
   - 楼梯 facing/half/shape 正确，转角梯不变成直梯；
   - 火把朝向正确、不悬空、不掉落。
2. 在目标位置预放同类型但错误朝向的门/床/楼梯/火把/告示牌，建造后应被修正。
3. 在墙上预放画/物品展示框，建造后不应被无提示吞掉；要么保留，要么安全掉落。
4. 检查箱子/花盆/唱片机等 TileEntity 方块，确认同类 NBT 丢失问题是否也需要一起处理。

## 七、优先级

- **S1（明显影响成品）**：门偏移/缺半、床颜色和朝向错、告示牌空白/立牌朝向错、楼梯转角错、画被墙替换掉落。
- **S2（体验）**：火把偶发掉落、已有同 Block 状态不修正。
- 不涉及存档损坏或崩溃，但会明显破坏建筑成品质量。
