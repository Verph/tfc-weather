package tfcweather.common.blocks;

import net.dries007.tfc.common.blocks.soil.TFCSandBlock;

import tfcweather.interfaces.ISandColor;
import tfcweather.interfaces.RegistrySand;

public class NewSandBlock extends TFCSandBlock implements ISandColor
{
    public final RegistrySand sand;

    public NewSandBlock(int dustColorIn, Properties properties, RegistrySand sand)
    {
        super(dustColorIn, properties);
        this.sand = sand;
    }

    @Override
    public RegistrySand getSandColor()
    {
        return sand;
    }
}
