package mca.items;

import mca.api.wrappers.WorldWrapper;
import mca.entity.EntityVillagerMCA;
import mca.entity.VillagerFactory;
import mca.enums.EnumGender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class ItemSpawnEgg extends Item {
    private boolean isMale;

    public ItemSpawnEgg(boolean isMale) {
        this.isMale = isMale;
        this.setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        RayTraceResult rtr = this.rayTrace(world, player, true);
        if (rtr == null || rtr.typeOfHit != RayTraceResult.Type.BLOCK) {
        	return EnumActionResult.PASS;
        }
        
        double posX = rtr.hitVec.x;
        double posY = rtr.hitVec.y;
        double posZ = rtr.hitVec.z;
        
        if (!world.isRemote) {
            EntityVillagerMCA villager = VillagerFactory.newVillager(new WorldWrapper(world)).withGender(isMale ? EnumGender.MALE : EnumGender.FEMALE).build();
            villager.setPosition(posX, posY, posZ);
            villager.finalizeMobSpawn(world.getDifficultyForLocation(villager.getPos()), null, false);
            world.spawnEntity(villager);

            if (!player.capabilities.isCreativeMode) player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStack.EMPTY);
        }

        return EnumActionResult.PASS;
    }
}