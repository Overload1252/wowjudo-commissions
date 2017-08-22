package com.builtbroken.wowjudo.content.crafting.gui;

import com.builtbroken.wowjudo.content.crafting.TileEntityCraftingTable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/9/2017.
 */
public class InventoryCraftingMatrix3x3 extends InventoryCrafting implements IInventory
{
    public final TileEntityCraftingTable table;
    public final Container hostContainer;

    public final int[] slots;

    public InventoryCraftingMatrix3x3(Container container, TileEntityCraftingTable table, int[] slots)
    {
        super(container, 3, 3);
        this.slots = slots;
        this.hostContainer = container;
        this.table = table;
    }

    @Override
    public int getSizeInventory()
    {
        return 9;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        if (slot >= 0 && slot < 9)
        {
            int actualSlot = getSlot(slot);
            return table.getStackInSlot(actualSlot); //Map face value slot to actual slot
        }
        return null;
    }

    protected int getSlot(int slot)
    {
        return slots[slot];
    }

    public ItemStack getStackInRowAndColumn(int row, int col)
    {
        if (row >= 0 && row < 3)
        {
            int k = row + col * 3;
            return this.getStackInSlot(k);
        }
        else
        {
            return null;
        }
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot)
    {
        if (slot >= 0 && slot < 9)
        {
            return table.getStackInSlotOnClosing(getSlot(slot));
        }
        return null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int a)
    {
        if (slot >= 0 && slot < 9)
        {
            ItemStack re = table.decrStackSize(getSlot(slot), a);
            this.hostContainer.onCraftMatrixChanged(this);
            return re;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack)
    {
        if (slot >= 0 && slot < 9)
        {
            table.setInventorySlotContents(getSlot(slot), stack);
            this.hostContainer.onCraftMatrixChanged(this);
        }
    }

    @Override
    public String getInventoryName()
    {
        return "inventory.matrix";
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return false;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public void markDirty()
    {

    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p_70300_1_)
    {
        return true;
    }

    @Override
    public void openInventory()
    {

    }

    @Override
    public void closeInventory()
    {

    }

    @Override
    public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_)
    {
        return true;
    }
}
