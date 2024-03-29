package com.builtbroken.wowjudo.stats;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.wowjudo.SurvivalMod;
import com.builtbroken.wowjudo.stats.network.PacketStatUpdate;
import com.google.common.collect.HashMultimap;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Handles stat modifications for the player
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/9/2017.
 */
public class StatEntityProperty implements IExtendedEntityProperties
{
    //NBT keys
    public static final String NBT_HP = "hp";
    public static final String NBT_SPEED = "speed";
    public static final String NBT_MEELE = "melee";
    public static final String NBT_FOOD = "food";
    public static final String NBT_ARMOR = "armor";
    public static final String NBT_AIR = "air";

    //Entity Attribute keys
    public static final String ATTR_HP = "stat." + SurvivalMod.DOMAIN + ":hp.max";
    public static final String ATTR_SPEED = "stat." + SurvivalMod.DOMAIN + ":speed";
    public static final String ATTR_ATTACK = "stat." + SurvivalMod.DOMAIN + ":damage.melee";


    //Levels
    private int hpIncrease = 0;
    private int speedIncrease = 0;
    private int meleeDamage = 0;
    private int foodAmount = 0;
    private int armorIncrease = 0;
    private int airIncrease = 0;

    //Attribute instances
    AttributeModifier healthAttribute;
    AttributeModifier speedAttribute;
    AttributeModifier attackAttribute;

    public EntityPlayer entityPlayer;
    public boolean hasChanged = true;

    private boolean headInWater = false;
    private boolean headPreviousInWater = false;

    @Override
    public void saveNBTData(NBTTagCompound compound)
    {
        NBTTagCompound save = new NBTTagCompound();

        save.setInteger(NBT_HP, getHpIncrease());
        save.setInteger(NBT_SPEED, getSpeedIncrease());
        save.setInteger(NBT_MEELE, getMeleeDamageIncrease());
        save.setInteger(NBT_FOOD, getFoodAmountIncrease());
        save.setInteger(NBT_ARMOR, getArmorIncrease());
        save.setInteger(NBT_AIR, getAirIncrease());

        compound.setTag(StatHandler.PROPERTY_ID, save);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound)
    {
        if (compound.hasKey(StatHandler.PROPERTY_ID, 10))
        {
            NBTTagCompound save = compound.getCompoundTag(StatHandler.PROPERTY_ID);

            setHpIncrease(save.getInteger(NBT_HP));
            setSpeedIncrease(save.getInteger(NBT_SPEED));
            setMeleeDamageIncrease(save.getInteger(NBT_MEELE));
            setFoodAmountIncrease(save.getInteger(NBT_FOOD));
            setArmorIncrease(save.getInteger(NBT_ARMOR));
            setAirIncrease(save.getInteger(NBT_AIR));
        }
    }

    @Override
    public void init(Entity entity, World world)
    {
        if (entity instanceof EntityPlayer)
        {
            this.entityPlayer = (EntityPlayer) entity;
        }
    }

    public void update()
    {
        if (entityPlayer != null && entityPlayer.isEntityAlive() && !entityPlayer.worldObj.isRemote)
        {
            //Check if player head in water
            headInWater = entityPlayer.isInsideOfMaterial(Material.water);

            //If head in water but not last tick
            if (StatHandler.ENABLE_AIR && headInWater && !headPreviousInWater)
            {
                int airBonus = getAirIncrease() * StatHandler.AIR_SCALE;
                int air = entityPlayer.getAir() + airBonus;

                //Increase default air supply, 300 is the default
                entityPlayer.setAir(air);
            }

            //Handle chance of more points being allocated then possible
            if (getPointsUsed() > getMaxPointUsable())
            {
                reset();
            }

            //Update attributes
            if (hasChanged)
            {
                hasChanged = false;

                //Clear
                removeAttributes();

                //Create
                createAttributes();

                //Apply
                applyAttributes();

                if (StatHandler.ENABLE_FOOD)
                {
                    //Make sure we have overridden food stats
                    if (!(entityPlayer.foodStats instanceof FoodStatOverride))
                    {
                        StatHandler.overrideFoodStats(entityPlayer);
                    }

                    //Apply updated food level
                    if (entityPlayer.foodStats instanceof FoodStatOverride)
                    {
                        ((FoodStatOverride) entityPlayer.foodStats).setMaxFoodLevel(FoodStatOverride.MAX_FOOD_DEFAULT + foodAmount * StatHandler.FOOD_SCALE);
                    }
                }

                //TODO remove extra hp if over max
                if (!entityPlayer.worldObj.isRemote && entityPlayer instanceof EntityPlayerMP)
                {
                    Engine.packetHandler.sendToPlayer(new PacketStatUpdate(entityPlayer), (EntityPlayerMP) entityPlayer);
                }
            }

            headPreviousInWater = headInWater;
        }
    }

    public boolean hasPointsLeft()
    {
        return getPointsUsed() < getMaxPointUsable();
    }

    protected void createAttributes()
    {
        //IF adding new stats > Add to remove list as well
        if (StatHandler.ENABLE_HEALTH)
        {
            healthAttribute = new AttributeModifier(ATTR_HP, getHpIncrease() * StatHandler.HEALTH_SCALE, 0);
        }
        if (StatHandler.ENABLE_SPEED)
        {
            speedAttribute = new AttributeModifier(ATTR_SPEED, getSpeedIncrease() * StatHandler.SPEED_SCALE, 0);
        }
        if (StatHandler.ENABLE_DAMAGE_REDUCTION)
        {
            attackAttribute = new AttributeModifier(ATTR_ATTACK, getMeleeDamageIncrease() * StatHandler.DAMAGE_SCALE, 0);
        }
    }

    protected void applyAttributes()
    {
        //IF adding new stats > Add to remove list as well
        HashMultimap map = HashMultimap.create();
        if (StatHandler.ENABLE_HEALTH && healthAttribute != null)
        {
            map.put(SharedMonsterAttributes.maxHealth.getAttributeUnlocalizedName(), healthAttribute);
        }
        if (StatHandler.ENABLE_SPEED && speedAttribute != null)
        {
            map.put(SharedMonsterAttributes.movementSpeed.getAttributeUnlocalizedName(), speedAttribute);
        }
        if (StatHandler.ENABLE_DAMAGE_REDUCTION && attackAttribute != null)
        {
            map.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), attackAttribute);
        }
        entityPlayer.getAttributeMap().applyAttributeModifiers(map);
    }

    /**
     * Clear unconventional to catch attributes with same name but different UUID
     */
    protected void removeAttributes()
    {
        //IF adding new stats > Make sure contains all attributes for removal or they will stack
        for (IAttribute attribute : new IAttribute[]{
                SharedMonsterAttributes.maxHealth,
                SharedMonsterAttributes.movementSpeed,
                SharedMonsterAttributes.attackDamage})
        {
            removeAttributes(entityPlayer.getEntityAttribute(attribute));
        }
    }

    protected void removeAttributes(IAttributeInstance attributeInstance)
    {
        Collection collection = attributeInstance.func_111122_c();

        if (collection != null)
        {
            ArrayList arraylist = new ArrayList(collection);
            Iterator iterator = arraylist.iterator();

            while (iterator.hasNext())
            {
                AttributeModifier attributemodifier = (AttributeModifier) iterator.next();
                if (attributemodifier.getName().contains(SurvivalMod.DOMAIN))
                {
                    attributeInstance.removeModifier(attributemodifier);
                }
            }
        }
    }

    public boolean reset()
    {
        setHpIncrease(0);
        setSpeedIncrease(0);
        setMeleeDamageIncrease(0);
        setFoodAmountIncrease(0);
        setArmorIncrease(0);
        setAirIncrease(0);
        hasChanged = true;
        return getHpIncrease() == 0
                && getSpeedIncrease() == 0
                && getMeleeDamageIncrease() == 0
                && getFoodAmountIncrease() == 0
                && getArmorIncrease() == 0
                && getAirIncrease() == 0;
    }

    public int getPointsUsed()
    {
        int points = 0;

        points += getHpIncrease();
        points += getSpeedIncrease();
        points += getMeleeDamageIncrease();
        points += getFoodAmountIncrease();
        points += getArmorIncrease();
        points += getAirIncrease();

        return points;
    }

    public int getMaxPointUsable()
    {
        return entityPlayer.experienceLevel;
    }

    public int getHpIncrease()
    {
        return hpIncrease;
    }

    public void setHpIncrease(int value)
    {
        if (value != hpIncrease && value >= 0)
        {
            this.hpIncrease = value > StatHandler.HEALTH_MAX ? StatHandler.HEALTH_MAX : value;
            hasChanged = true;
        }
    }

    public int getSpeedIncrease()
    {
        return speedIncrease;
    }

    public void setSpeedIncrease(int value)
    {
        if (value != speedIncrease && value >= 0)
        {
            this.speedIncrease = value > StatHandler.SPEED_MAX ? StatHandler.SPEED_MAX : value;
            hasChanged = true;
        }
    }

    public int getMeleeDamageIncrease()
    {
        return meleeDamage;
    }

    public void setMeleeDamageIncrease(int value)
    {
        if (value != meleeDamage && value >= 0)
        {
            this.meleeDamage = value;
            hasChanged = true;
        }
    }

    public int getFoodAmountIncrease()
    {
        return foodAmount;
    }

    public void setFoodAmountIncrease(int value)
    {
        if (value != foodAmount && value >= 0)
        {
            this.foodAmount = value;
            hasChanged = true;
        }
    }

    public int getArmorIncrease()
    {
        return armorIncrease;
    }

    public void setArmorIncrease(int value)
    {
        if (value != armorIncrease && value >= 0)
        {
            this.armorIncrease = value;
            hasChanged = true;
        }
    }

    public int getAirIncrease()
    {
        return airIncrease;
    }

    public void setAirIncrease(int value)
    {
        if (value != airIncrease && value >= 0)
        {
            this.airIncrease = value;
            hasChanged = true;
        }
    }

    public void copyData(StatEntityProperty property)
    {
        hpIncrease = property.hpIncrease;
        speedIncrease = property.speedIncrease;
        meleeDamage = property.meleeDamage;
        foodAmount = property.foodAmount;
        armorIncrease = property.armorIncrease;
        airIncrease = property.airIncrease;
        hasChanged = true;
    }
}
