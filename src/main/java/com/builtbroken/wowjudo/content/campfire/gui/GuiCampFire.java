package com.builtbroken.wowjudo.content.campfire.gui;

import com.builtbroken.mc.client.SharedAssets;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import com.builtbroken.mc.prefab.gui.GuiContainerBase;
import com.builtbroken.wowjudo.SurvivalMod;
import com.builtbroken.wowjudo.content.campfire.TileEntityCampfire;
import com.builtbroken.wowjudo.content.furnace.TileDualFurnace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/8/2017.
 */
public class GuiCampFire extends GuiContainerBase<TileEntityCampfire>
{
    private TileEntityCampfire campFire;

    public GuiCampFire(EntityPlayer player, TileEntityCampfire fire)
    {
        super(new ContainerCampFire(player, fire));
        this.campFire = fire;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(f, mouseX, mouseY);
        for (Object object : inventorySlots.inventorySlots)
        {
            if (object instanceof Slot)
            {
                drawSlot((Slot) object);
            }
        }

        this.mc.renderEngine.bindTexture(SharedAssets.GUI_COMPONENTS);

        //Render fire for fuel timer
        renderFurnaceCookFire(54, 34, campFire.fuelTimer, campFire.itemFuelTime );

        //Render arrow for crafting timer
        renderFurnaceCookArrow(84, 35, campFire.cookTimer, TileDualFurnace.MAX_COOK_TIMER);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        drawStringCentered(LanguageUtility.getLocal("tile." + SurvivalMod.PREFX + "campFire.gui.name"), 88, 6);
        drawString(LanguageUtility.getLocal("tile." + SurvivalMod.PREFX + "campFire.gui.inventory"), 8, 74);
    }
}
