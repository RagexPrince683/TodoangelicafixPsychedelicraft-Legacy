package ivorius.psychedelicraft.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockGrowLight extends Block
{
    public BlockGrowLight()
    {
        super(Material.glass);

        setHardness(0.3F);
        setResistance(1.5F);
        setStepSound(Block.soundTypeGlass);
        setLightLevel(1.0F);
        setLightOpacity(0);
    }
}
