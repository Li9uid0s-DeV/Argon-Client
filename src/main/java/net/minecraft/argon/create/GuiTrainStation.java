package net.minecraft.argon.create;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

public class GuiTrainStation extends GuiScreen
{
    private final TileEntityTrainStation station;
    private GuiTextField nameField;
    private String statusText = "";

    public GuiTrainStation(TileEntityTrainStation station)
    {
        this.station = station;
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.nameField = new GuiTextField(0, this.fontRenderer, cx - 100, cy - 46, 200, 20);
        this.nameField.setMaxStringLength(32);
        this.nameField.setText(this.station.getStationName());
        this.nameField.setFocused(true);

        this.buttonList.add(new GuiButton(1, cx - 100, cy - 16, 200, 20, "Assemble Train (Up to 10 Cars)"));
        this.buttonList.add(new GuiButton(2, cx - 100, cy + 10, 200, 20, "Disassemble Nearby Train"));
        this.buttonList.add(new GuiButton(3, cx - 100, cy + 40, 200, 20, "Save & Close"));
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);

        if (this.nameField != null)
        {
            this.station.setStationName(this.nameField.getText());
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (this.nameField != null)
        {
            this.station.setStationName(this.nameField.getText());
        }

        if (button.id == 1)
        {
            this.assembleTrain();
        }
        else if (button.id == 2)
        {
            this.disassembleNearbyTrain();
        }
        else if (button.id == 3)
        {
            this.mc.displayGuiScreen((GuiScreen)null);
        }
    }

    private void assembleTrain()
    {
        World world = this.station.getWorld();

        if (world == null)
        {
            return;
        }

        BlockPos trackAnchor = this.station.findNearbyTrackPos();

        if (trackAnchor == null)
        {
            this.statusText = "\u00a7cNo Train Track found within 4 blocks!";
            return;
        }

        TileEntity trackTe = world.getTileEntity(trackAnchor);
        EnumFacing trackDir = trackTe instanceof TileEntityTrainTrack ? ((TileEntityTrainTrack)trackTe).getFacing() : EnumFacing.NORTH;

        // Scan along track forward and backward up to 64 blocks to find bogeys / train carriages (max 10 cars)
        List<BlockPos> bogeyPositions = new ArrayList<BlockPos>();
        Set<BlockPos> seen = new HashSet<BlockPos>();

        for (int step = -32; step <= 32; ++step)
        {
            for (int lateral = -1; lateral <= 1; ++lateral)
            {
                BlockPos probe = trackAnchor.offset(trackDir, step).offset(trackDir.rotateY(), lateral).up();

                if (!seen.contains(probe) && world.isBlockLoaded(probe))
                {
                    IBlockState st = world.getBlockState(probe);

                    if (st.getBlock() == CreateBlocks.TRAIN_BOGEY)
                    {
                        seen.add(probe);
                        bogeyPositions.add(probe);
                    }
                }
            }
        }

        // Fallback: if no explicit Bogey block found, try assembling glued blocks right above trackAnchor
        if (bogeyPositions.isEmpty())
        {
            BlockPos aboveTrack = trackAnchor.up();

            if (!world.isAirBlock(aboveTrack) && SuperGlueManager.isMovableContraptionBlock(world, aboveTrack, world.getBlockState(aboveTrack)))
            {
                bogeyPositions.add(aboveTrack);
            }
        }

        if (bogeyPositions.isEmpty())
        {
            this.statusText = "\u00a7cPlace a Train Bogey (and glued blocks) on the track!";
            return;
        }

        EntityArgonTrain train = new EntityArgonTrain(world);
        int carCount = 0;

        for (int i = 0; i < bogeyPositions.size() && carCount < EntityArgonTrain.MAX_CARS; ++i)
        {
            BlockPos bogeyPos = bogeyPositions.get(i);

            if (world.isAirBlock(bogeyPos))
            {
                continue;
            }

            List<BlockPos> glued = SuperGlueManager.collectGluedBlocks(world, bogeyPos, trackAnchor, 256);

            if (!glued.isEmpty())
            {
                ContraptionStructure carStruct = ContraptionStructure.captureAndRemoveFromWorld(world, bogeyPos, glued);
                double carLen = Math.max(4.0D, Math.max(carStruct.maxX - carStruct.minX, carStruct.maxZ - carStruct.minZ) + 0.6D);
                train.addCarriage(
                    carStruct,
                    (double)bogeyPos.getX() + 0.5D,
                    (double)trackAnchor.getY() + 0.1875D,
                    (double)bogeyPos.getZ() + 0.5D,
                    trackDir.getHorizontalAngle(),
                    carLen);
                ++carCount;
            }
        }

        if (carCount == 0)
        {
            this.statusText = "\u00a7cCould not capture any carriage blocks.";
            return;
        }

        world.spawnEntity(train);

        if (this.mc.player != null)
        {
            this.mc.player.sendMessage(new TextComponentString("\u00a7a[Station: " + this.station.getStationName() + "] Assembled train with " + carCount + " carriage(s)!"));
        }

        this.mc.displayGuiScreen((GuiScreen)null);
    }

    private void disassembleNearbyTrain()
    {
        World world = this.station.getWorld();

        if (world == null)
        {
            return;
        }

        AxisAlignedBB box = new AxisAlignedBB(this.station.getPos()).grow(16.0D);
        List<EntityArgonTrain> trains = world.getEntitiesWithinAABB(EntityArgonTrain.class, box);

        if (trains.isEmpty())
        {
            this.statusText = "\u00a7eNo assembled train nearby (within 16 blocks).";
            return;
        }

        for (int i = 0; i < trains.size(); ++i)
        {
            trains.get(i).disassembleToWorld();
        }

        this.statusText = "\u00a7aDisassembled " + trains.size() + " train(s).";
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.nameField != null && this.nameField.textboxKeyTyped(typedChar, keyCode))
        {
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (this.nameField != null)
        {
            this.nameField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen()
    {
        if (this.nameField != null)
        {
            this.nameField.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        int cx = this.width / 2;
        int cy = this.height / 2;
        drawRect(cx - 120, cy - 82, cx + 120, cy + 72, 0xCC1B1E24);
        drawRect(cx - 120, cy - 82, cx + 120, cy - 80, 0xFFE6B84D);

        this.drawCenteredString(this.fontRenderer, "\u00a76Create Train Station", cx, cy - 72, 0xFFFFFF);
        this.fontRenderer.drawStringWithShadow("Station Name:", cx - 100, cy - 58, 0xCCCCCC);

        if (this.nameField != null)
        {
            this.nameField.drawTextBox();
        }

        if (this.statusText.length() > 0)
        {
            this.drawCenteredString(this.fontRenderer, this.statusText, cx, cy + 63, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
