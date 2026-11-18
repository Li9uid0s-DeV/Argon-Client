package net.minecraft.argon.tnt;

import net.minecraft.block.BlockTNT;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * Custom TNT block extending vanilla BlockTNT.
 * Supports ignition via flint & steel, fire charges, redstone, flaming arrows,
 * and chain explosions, spawning EntityArgonTNTPrimed with its TntType.
 */
public class BlockArgonTNT extends BlockTNT
{
    private final TntType tntType;

    public BlockArgonTNT(TntType tntType)
    {
        super();
        this.tntType = tntType;
        this.setUnlocalizedName(tntType.registryName);
        this.setHardness(0.0F);
        this.setSoundType(SoundType.PLANT);
        this.setCreativeTab(TntCreativeTab.TAB_ARGON_TNT);
    }

    public TntType getTntType()
    {
        return this.tntType;
    }

    @Override
    public void onBlockDestroyedByExplosion(World worldIn, BlockPos pos, Explosion explosionIn)
    {
        if (!worldIn.isRemote)
        {
            EntityArgonTNTPrimed primed = new EntityArgonTNTPrimed(
                worldIn,
                (double)((float)pos.getX() + 0.5F),
                (double)pos.getY(),
                (double)((float)pos.getZ() + 0.5F),
                explosionIn != null ? explosionIn.getExplosivePlacedBy() : null,
                this.tntType
            );
            int baseFuse = Math.max(16, this.tntType.fuseTicks);
            primed.setFuse((short)(worldIn.rand.nextInt(baseFuse / 4) + baseFuse / 8));
            ArgonExplosionScheduler.queueEntitySpawn(worldIn, primed);
        }
    }

    @Override
    public void explode(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase igniter)
    {
        if (!worldIn.isRemote && ((Boolean)state.getValue(EXPLODE)).booleanValue())
        {
            EntityArgonTNTPrimed primed = new EntityArgonTNTPrimed(
                worldIn,
                (double)((float)pos.getX() + 0.5F),
                (double)pos.getY(),
                (double)((float)pos.getZ() + 0.5F),
                igniter,
                this.tntType
            );
            worldIn.spawnEntity(primed);
            worldIn.playSound((EntityPlayer)null, primed.posX, primed.posY, primed.posZ, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
}
