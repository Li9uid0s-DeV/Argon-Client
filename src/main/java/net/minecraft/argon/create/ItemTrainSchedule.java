package net.minecraft.argon.create;

import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemTrainSchedule extends Item
{
    public ItemTrainSchedule()
    {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.TRANSPORTATION);
        this.setUnlocalizedName("createTrainSchedule");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        TrainScheduleData data = readSchedule(stack);

        if (!data.isEmpty())
        {
            return "Train Schedule (" + data.stops.size() + " stops)";
        }

        return "Train Schedule (Create)";
    }

    public static TrainScheduleData readSchedule(ItemStack stack)
    {
        if (stack.isEmpty() || !stack.hasTagCompound())
        {
            return new TrainScheduleData();
        }

        return TrainScheduleData.readFromNBT(stack.getTagCompound().getCompoundTag("ScheduleData"));
    }

    public static void writeSchedule(ItemStack stack, TrainScheduleData data)
    {
        if (stack.isEmpty() || data == null)
        {
            return;
        }

        if (!stack.hasTagCompound())
        {
            stack.setTagCompound(new NBTTagCompound());
        }

        stack.getTagCompound().setTag("ScheduleData", data.writeToNBT());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (worldIn.isRemote)
        {
            Minecraft.getMinecraft().displayGuiScreen(new GuiTrainSchedule(playerIn, handIn, stack));
        }

        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }
}
