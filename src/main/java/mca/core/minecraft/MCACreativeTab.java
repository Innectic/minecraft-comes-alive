package mca.core.minecraft;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

/**
 * @author Innectic
 * @since 8/10/2020
 */
public class MCACreativeTab extends CreativeTabs {

    public MCACreativeTab() {
        super("MCA");
    }

    @Override
    public ItemStack getTabIconItem() {
        return new ItemStack(ItemsMCA.ENGAGEMENT_RING);
    }
}
