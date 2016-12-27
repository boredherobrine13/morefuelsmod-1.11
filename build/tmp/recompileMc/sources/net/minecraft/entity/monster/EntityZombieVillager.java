package net.minecraft.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityZombieVillager extends EntityZombie
{
    private static final DataParameter<Boolean> CONVERTING = EntityDataManager.<Boolean>createKey(EntityZombieVillager.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> field_190739_c = EntityDataManager.<Integer>createKey(EntityZombieVillager.class, DataSerializers.VARINT);
    /** Ticker used to determine the time remaining for this zombie to convert into a villager when cured. */
    private int conversionTime;

    public EntityZombieVillager(World p_i47277_1_)
    {
        super(p_i47277_1_);
    }

    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(CONVERTING, Boolean.valueOf(false));
        this.dataManager.register(field_190739_c, Integer.valueOf(0));
    }

    public void func_190733_a(int p_190733_1_)
    {
        this.dataManager.set(field_190739_c, Integer.valueOf(p_190733_1_));
        net.minecraftforge.fml.common.registry.VillagerRegistry.onSetProfession(this, p_190733_1_);
    }

    public int func_190736_dl()
    {
        return Math.max(((Integer)this.dataManager.get(field_190739_c)).intValue() % 6, 0);
    }

    public static void func_190737_b(DataFixer p_190737_0_)
    {
        EntityLiving.registerFixesMob(p_190737_0_, EntityZombieVillager.class);
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("Profession", this.func_190736_dl());
        compound.setInteger("ConversionTime", this.isConverting() ? this.conversionTime : -1);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.func_190733_a(compound.getInteger("Profession"));

        if (compound.hasKey("ConversionTime", 99) && compound.getInteger("ConversionTime") > -1)
        {
            this.func_190734_b(compound.getInteger("ConversionTime"));
        }
    }

    /**
     * Called only once on an entity when first time spawned, via egg, mob spawner, natural spawning etc, but not called
     * when entity is reloaded from nbt. Mainly used for initializing attributes and inventory
     */
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata)
    {
        this.func_190733_a(this.worldObj.rand.nextInt(6));
        return super.onInitialSpawn(difficulty, livingdata);
    }

    private net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession prof;
    public void setForgeProfession(net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession prof)
    {
        this.prof = prof;
        this.func_190733_a(net.minecraftforge.fml.common.registry.VillagerRegistry.getId(prof));
    }

    public net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession getForgeProfession()
    {
        return this.prof;
    }


    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        if (!this.worldObj.isRemote && this.isConverting())
        {
            int i = this.func_190735_dq();
            this.conversionTime -= i;

            if (this.conversionTime <= 0)
            {
                this.func_190738_dp();
            }
        }

        super.onUpdate();
    }

    public boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack itemstack = player.getHeldItem(hand);

        if (itemstack.getItem() == Items.GOLDEN_APPLE && itemstack.getMetadata() == 0 && this.isPotionActive(MobEffects.WEAKNESS))
        {
            if (!player.capabilities.isCreativeMode)
            {
                itemstack.func_190918_g(1);
            }

            if (!this.worldObj.isRemote)
            {
                this.func_190734_b(this.rand.nextInt(2401) + 3600);
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Determines if an entity can be despawned, used on idle far away entities
     */
    protected boolean canDespawn()
    {
        return !this.isConverting();
    }

    /**
     * Returns whether this zombie is in the process of converting to a villager
     */
    public boolean isConverting()
    {
        return ((Boolean)this.getDataManager().get(CONVERTING)).booleanValue();
    }

    protected void func_190734_b(int p_190734_1_)
    {
        this.conversionTime = p_190734_1_;
        this.getDataManager().set(CONVERTING, Boolean.valueOf(true));
        this.removePotionEffect(MobEffects.WEAKNESS);
        this.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, p_190734_1_, Math.min(this.worldObj.getDifficulty().getDifficultyId() - 1, 0)));
        this.worldObj.setEntityState(this, (byte)16);
    }

    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id)
    {
        if (id == 16)
        {
            if (!this.isSilent())
            {
                this.worldObj.playSound(this.posX + 0.5D, this.posY + 0.5D, this.posZ + 0.5D, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, this.getSoundCategory(), 1.0F + this.rand.nextFloat(), this.rand.nextFloat() * 0.7F + 0.3F, false);
            }
        }
        else
        {
            super.handleStatusUpdate(id);
        }
    }

    protected void func_190738_dp()
    {
        EntityVillager entityvillager = new EntityVillager(this.worldObj);
        entityvillager.copyLocationAndAnglesFrom(this);
        entityvillager.setProfession(this.func_190736_dl());
        entityvillager.func_190672_a(this.worldObj.getDifficultyForLocation(new BlockPos(entityvillager)), (IEntityLivingData)null, false);
        entityvillager.setLookingForHome();

        if (this.isChild())
        {
            entityvillager.setGrowingAge(-24000);
        }

        this.worldObj.removeEntity(this);
        entityvillager.setNoAI(this.isAIDisabled());

        if (this.hasCustomName())
        {
            entityvillager.setCustomNameTag(this.getCustomNameTag());
            entityvillager.setAlwaysRenderNameTag(this.getAlwaysRenderNameTag());
        }

        this.worldObj.spawnEntityInWorld(entityvillager);
        entityvillager.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
        this.worldObj.playEvent((EntityPlayer)null, 1027, new BlockPos((int)this.posX, (int)this.posY, (int)this.posZ), 0);
    }

    protected int func_190735_dq()
    {
        int i = 1;

        if (this.rand.nextFloat() < 0.01F)
        {
            int j = 0;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int k = (int)this.posX - 4; k < (int)this.posX + 4 && j < 14; ++k)
            {
                for (int l = (int)this.posY - 4; l < (int)this.posY + 4 && j < 14; ++l)
                {
                    for (int i1 = (int)this.posZ - 4; i1 < (int)this.posZ + 4 && j < 14; ++i1)
                    {
                        Block block = this.worldObj.getBlockState(blockpos$mutableblockpos.setPos(k, l, i1)).getBlock();

                        if (block == Blocks.IRON_BARS || block == Blocks.BED)
                        {
                            if (this.rand.nextFloat() < 0.3F)
                            {
                                ++i;
                            }

                            ++j;
                        }
                    }
                }
            }
        }

        return i;
    }

    /**
     * Gets the pitch of living sounds in living entities.
     */
    protected float getSoundPitch()
    {
        return this.isChild() ? (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 2.0F : (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F;
    }

    public SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
    }

    public SoundEvent getHurtSound()
    {
        return SoundEvents.ENTITY_ZOMBIE_VILLAGER_HURT;
    }

    public SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_ZOMBIE_VILLAGER_DEATH;
    }

    public SoundEvent func_190731_di()
    {
        return SoundEvents.ENTITY_ZOMBIE_VILLAGER_STEP;
    }

    @Nullable
    protected ResourceLocation getLootTable()
    {
        return LootTableList.field_191183_as;
    }

    protected ItemStack func_190732_dj()
    {
        return ItemStack.field_190927_a;
    }
}