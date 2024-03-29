package com.builtbroken.wowjudo.content.wall;

import com.builtbroken.mc.api.explosive.IBlast;
import com.builtbroken.mc.api.explosive.IBlastEdit;
import com.builtbroken.mc.api.explosive.IExplosiveDamageable;
import com.builtbroken.mc.api.explosive.IExplosiveHandler;
import com.builtbroken.mc.codegen.annotations.TileWrapped;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.data.Direction;
import com.builtbroken.mc.framework.block.imp.IActivationListener;
import com.builtbroken.mc.framework.block.imp.IHardnessListener;
import com.builtbroken.mc.framework.block.imp.IToolListener;
import com.builtbroken.mc.framework.logic.TileNode;
import com.builtbroken.mc.imp.transform.vector.BlockPos;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import com.builtbroken.mc.lib.helper.MaterialDict;
import com.builtbroken.mc.lib.world.edit.BlockEdit;
import com.builtbroken.wowjudo.SurvivalMod;
import com.builtbroken.wowjudo.content.ex.HPBlockEdit;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.config.Configuration;

import java.util.UUID;

/**
 * Damageable wall system that is slowly broken down rather than instantly destroyed
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 5/12/2017.
 */
@TileWrapped(className = "TileEntityWrappedWall")
public class TileNodeWall extends TileNode implements IExplosiveDamageable, IHardnessListener, IToolListener, IActivationListener
{
    private float hp = -1;
    private WallMaterial mat_cache;
    private StructureType struct_cache;

    public TileNodeWall()
    {
        super("blast.wall", SurvivalMod.DOMAIN);
    }

    @Override
    public String getUniqueID()
    {
        return "structure." + getMaterial().materialName + "." + getStructureType().name().toLowerCase();
    }

    @Override
    public boolean requiresPerTickUpdate()
    {
        return false;
    }

    @Override
    public float getBlockHardness(EntityPlayer player)
    {
        ///setblock 46 74 258 wjsurvialmod:wjIronWall
        if (!isOwner(player))
        {
            return Short.MAX_VALUE;
        }
        return 5;
    }

    @Override
    public String getBlockHarvestTool(int metadata)
    {
        return null;
    }

    public WallMaterial getMaterial()
    {
        if (mat_cache == null)
        {
            Block block = getHost().getHostBlock();
            Material material = block.getMaterial();

            for (WallMaterial mat : WallMaterial.values())
            {
                if (mat.getMaterial() == material)
                {
                    mat_cache = mat;
                    break;
                }
            }
            if (mat_cache == null)
            {
                mat_cache = WallMaterial.STONE;
                if (Engine.runningAsDev)
                {
                    SurvivalMod.logger.error("TileNodeWall: Failed to read material for " + block);
                }
            }
        }
        return mat_cache;
    }

    public StructureType getStructureType()
    {
        if (struct_cache == null)
        {
            int meta = world().unwrap().getBlockMetadata(xi(), yi(), zi());
            if (meta > 0 && meta < StructureType.values().length)
            {
                struct_cache = StructureType.values()[meta];
            }
            else
            {
                struct_cache = StructureType.WALL;
                if (Engine.runningAsDev)
                {
                    SurvivalMod.logger.error("TileNodeWall: Failed to read structure for meta value " + meta);
                }
            }
        }
        return struct_cache;
    }

    public float getHp()
    {
        if (hp == -1)
        {
            hp = getMaxHp();
        }
        return hp;
    }

    public float getMaxHp()
    {
        return getMaterial().getHp(getStructureType());
    }

    @Override
    public void load(NBTTagCompound nbt)
    {
        super.load(nbt);
        if (nbt.hasKey("hp"))
        {
            hp = nbt.getFloat("hp");
        }
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt)
    {
        super.save(nbt);
        nbt.setFloat("hp", getHp());
        return nbt;
    }

    @Override
    public void readDescPacket(ByteBuf buf)
    {
        super.readDescPacket(buf);
        float old = getHp();
        hp = buf.readFloat();
        if (Math.abs(old - getHp()) > 0.001)
        {
            world().unwrap().markBlockRangeForRenderUpdate(xi(), yi(), zi(), xi(), yi(), zi());
        }
    }

    @Override
    public void writeDescPacket(ByteBuf buf)
    {
        super.writeDescPacket(buf);
        buf.writeFloat(getHp());
    }

    @Override
    public float getEnergyCostOfTile(IExplosiveHandler explosive, IBlast blast, Direction facing, float energy, float distance)
    {
        return Math.max(getHp(), 1) * getMaterial().energyPerType[getStructureType().ordinal()];
    }

    @Override
    public IBlastEdit getBlockEditOnBlastImpact(IExplosiveHandler explosive, IBlast blast, Direction facing, float energy, float distance)
    {
        float lostHP = Math.min(getHp(), Math.max(0, energy / getMaterial().energyPerType[getStructureType().ordinal()]));
        if (getHp() > 0 && getHp() - lostHP > 1)
        {
            if (lostHP > 0)
            {
                //Reduce HP
                return new HPBlockEdit(world().unwrap(), new BlockPos(xi(), yi(), zi()), lostHP);
            }

            //No damage
            return null;
        }
        //Kill block
        return new BlockEdit(world().unwrap(), xi(), yi(), zi()).set(Blocks.air, 0);
    }

    public void reduceHP(float hp)
    {
        this.hp -= hp;
        sendDescPacket(); //TODO find way to fire only 1 time per tick
    }

    @Override
    public boolean onPlayerActivated(EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        if (Engine.runningAsDev && player.getHeldItem() != null)
        {
            if (player.getHeldItem().getItem() == Items.stick)
            {
                if (isServer())
                {
                    setOwner(null);
                    username = "test";
                    setOwnerID(UUID.randomUUID());
                    player.addChatComponentMessage(new ChatComponentText("Removed owner of wall"));
                }
                return true;
            }
        }
        return false;
    }

    public enum WallMaterial
    {
        WOOD("wood", 5, 100, 0.02f), //weapon that does 3 damage should take 100 hits to kill a wood wall
        STONE("rock", 25, 100, 0.005f),
        IRON("iron", 62, 100, 0);

        public float hp;
        public float energyCostPerHP;

        private final String materialName;
        private Material material;

        private float[] hpPerType;
        private float[] energyPerType;
        private float[] weaponDamageScalePerType;
        private float weaponDamageScale;

        WallMaterial(String materialName, int hp, float energyCostPerHP, float weaponDamageScale)
        {
            this.materialName = materialName;
            this.hp = hp;
            this.energyCostPerHP = energyCostPerHP;
            this.weaponDamageScale = weaponDamageScale;
        }

        public static void loadConfig(Configuration configuration)
        {
            for (WallMaterial material : values())
            {
                material.hpPerType = new float[StructureType.values().length];
                material.energyPerType = new float[StructureType.values().length];
                material.weaponDamageScalePerType = new float[StructureType.values().length];
                for (StructureType type : StructureType.values())
                {
                    material.hpPerType[type.ordinal()] = configuration.getFloat(LanguageUtility.capitalizeFirst(type.name().toLowerCase()) + "_HP", material.name().toLowerCase() + "_structures", material.hp * type.multi, 1, 100000, "How many hits of damage does the structure take before being destroyed.");
                    material.energyPerType[type.ordinal()] = configuration.getFloat(LanguageUtility.capitalizeFirst(type.name().toLowerCase()) + "_energyPerHP", material.name().toLowerCase() + "_structures", material.hp * type.multi, 1, 1000000, "How much blast energy does it take to do 1 hp damage.");
                    material.weaponDamageScalePerType[type.ordinal()] = configuration.getFloat(LanguageUtility.capitalizeFirst(type.name().toLowerCase()) + "_weaponDamageScale", material.name().toLowerCase() + "_structures", material.weaponDamageScale, -1000, 1000, "How much to scale entity and item damage when attacking walls.");
                }
            }
        }

        public Material getMaterial()
        {
            if (material == null)
            {
                material = MaterialDict.get(materialName);
            }
            return material;
        }

        public float getHp(StructureType structureType)
        {
            return hpPerType[structureType.ordinal()];
        }

        public float getWeaponDamageScale(StructureType structureType)
        {
            return weaponDamageScalePerType[structureType.ordinal()];
        }
    }

    public enum StructureType
    {
        WALL(1),
        FLOOR(1.2f),
        ROOF(0.8f);

        float multi;

        StructureType(float multi)
        {
            this.multi = multi;
        }
    }
}
