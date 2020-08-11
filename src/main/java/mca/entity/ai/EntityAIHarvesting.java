package mca.entity.ai;

import java.util.ArrayList;
import java.util.List;

import mca.api.objects.Pos;
import mca.core.MCA;
import mca.entity.EntityVillagerMCA;
import mca.enums.EnumChore;
import mca.util.Util;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;

public class EntityAIHarvesting extends AbstractEntityAIChore {
    private int blockWork = 0;
    private int lastCropScan = 0;

    public EntityAIHarvesting(EntityVillagerMCA villagerIn) {
        super(villagerIn);
        this.setMutexBits(1);
    }

    public boolean shouldExecute() {
        if (villager.getHealth() < villager.getMaxHealth()) {
            villager.stopChore();
        }
        return EnumChore.byId(villager.get(EntityVillagerMCA.ACTIVE_CHORE)) == EnumChore.HARVEST && (blockWork - villager.ticksExisted) < 0;
    }

    public boolean shouldContinueExecuting() {
        return !villager.getNavigator().noPath();
    }

    private Pos searchCrop(int rangeX, int rangeY) {
        List<Pos> nearbyCrops = Util.getNearbyBlocks(Util.wrapPos(villager), villager.world, BlockCrops.class, rangeX, rangeY);
        List<Pos> harvestable = new ArrayList<>();
        for (Pos pos : nearbyCrops) {
            IBlockState state = villager.world.getBlockState(pos);
            BlockCrops crop = (BlockCrops) state.getBlock();

            if (crop.isMaxAge(state)) {
                harvestable.add(pos);
            }
        }

        return Util.getNearestPoint(Util.wrapPos(villager), harvestable);
    }

    public void startExecuting() {
        if (!villager.inventory.contains(ItemHoe.class)) {
            villager.say(getAssigningPlayer(), "chore.harvesting.nohoe");
            villager.stopChore();
        }

        //search crop
        Pos target = searchCrop(16, 3);

        //no crop next to villager -> long range scan
        //limited to once a minute to reduce CPU usage
        if (target == null && villager.ticksExisted - lastCropScan > 1200) {
            //MCA.getLogger().info(villager.getName() + " scans for crops");
            lastCropScan = villager.ticksExisted;
            target = searchCrop(32, 16);
        }

        if (target == null) {
            if (villager.getWorkplace().getY() > 0 && villager.getDistanceSq(villager.getWorkplace().getBlockPos()) > 256.0D) {
                //go to their workplace (if set and more than 16 blocks away)
                //MCA.getLogger().info(villager.getName() + " goes to workplace");
                villager.moveTowardsBlock(villager.getWorkplace());
            } else {
                //failed (no crop on range), allows now other, lower priority tasks to interrupt
                //MCA.getLogger().info(villager.getName() + " idles");
                blockWork = villager.ticksExisted + 100 + villager.getRNG().nextInt(100);
            }
        } else {
            //harvest if next to it, else try to reach it
            double distanceTo = Math.sqrt(villager.getDistanceSq(target.getBlockPos()));
            if (distanceTo >= 2.0D) {
                if (!villager.getNavigator().setPath(villager.getNavigator().getPathToPos(target.getBlockPos()), 0.5D)) {
                    villager.attemptTeleport(target.getX(), target.getY(), target.getZ());
                }
            } else {
                //harvest
                IBlockState state = villager.world.getBlockState(target);
                if (state.getBlock() instanceof BlockCrops) {
                    BlockCrops crop = (BlockCrops) state.getBlock();
                    NonNullList<ItemStack> drops = NonNullList.create();
                    crop.getDrops(drops, villager.world.getVanillaWorld(), target.getBlockPos(), state, 0);
                    for (ItemStack stack : drops) {
                        villager.inventory.addItem(stack);
                    }

                    villager.swingArm(EnumHand.MAIN_HAND);
                    villager.getHeldItem(EnumHand.MAIN_HAND).damageItem(2, villager);

                    try {
                        IProperty<Integer> property = (IProperty<Integer>) crop.getBlockState().getProperty("age");
                        villager.world.setBlockState(target, state.withProperty(property, 0));
                    } catch (Exception e) { // age property may have some issues on certain mods, if it errors just set to air
                        MCA.getLogger().warn("Error resetting crop age at " + target.toString() + "! Setting to air.");
                        villager.world.setBlockToAir(target);
                    }
                }

                //wait before harvesting next crop
                ItemStack hoeStack = villager.inventory.getBestItemOfType(ItemHoe.class);
                float efficiency = hoeStack == ItemStack.EMPTY ? 0.0f : Item.ToolMaterial.valueOf(((ItemHoe) hoeStack.getItem()).getMaterialName()).getEfficiency();
                blockWork = villager.ticksExisted + (int) Math.max(2.0f, 60.0f - efficiency * 5.0f);
            }
        }
    }
}