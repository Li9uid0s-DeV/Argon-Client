package net.minecraft.argon.create;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class TrainScheduleData
{
    public static final class StopEntry
    {
        public String stationName;
        public int waitTicks;
        public double targetX;
        public double targetY;
        public double targetZ;
        public boolean hasCoordinates;

        public StopEntry(String stationName, int waitTicks, double targetX, double targetY, double targetZ, boolean hasCoordinates)
        {
            this.stationName = stationName;
            this.waitTicks = Math.max(20, waitTicks);
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.hasCoordinates = hasCoordinates;
        }
    }

    public final List<StopEntry> stops = new ArrayList<StopEntry>();
    public boolean loop = true;
    public int currentStopIndex = 0;

    public boolean isEmpty()
    {
        return this.stops.isEmpty();
    }

    public StopEntry getCurrentStop()
    {
        if (this.stops.isEmpty())
        {
            return null;
        }

        if (this.currentStopIndex < 0 || this.currentStopIndex >= this.stops.size())
        {
            this.currentStopIndex = 0;
        }

        return this.stops.get(this.currentStopIndex);
    }

    public void advanceStop()
    {
        if (this.stops.isEmpty())
        {
            return;
        }

        if (this.currentStopIndex + 1 < this.stops.size())
        {
            ++this.currentStopIndex;
        }
        else if (this.loop)
        {
            this.currentStopIndex = 0;
        }
    }

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("Loop", this.loop);
        tag.setInteger("CurIdx", this.currentStopIndex);
        NBTTagList list = new NBTTagList();

        for (int i = 0; i < this.stops.size(); ++i)
        {
            StopEntry e = this.stops.get(i);
            NBTTagCompound st = new NBTTagCompound();
            st.setString("Name", e.stationName != null ? e.stationName : "Station");
            st.setInteger("Wait", e.waitTicks);
            st.setDouble("TX", e.targetX);
            st.setDouble("TY", e.targetY);
            st.setDouble("TZ", e.targetZ);
            st.setBoolean("HasCoord", e.hasCoordinates);
            list.appendTag(st);
        }

        tag.setTag("Stops", list);
        return tag;
    }

    public static TrainScheduleData readFromNBT(NBTTagCompound tag)
    {
        TrainScheduleData data = new TrainScheduleData();

        if (tag == null)
        {
            return data;
        }

        data.loop = !tag.hasKey("Loop") || tag.getBoolean("Loop");
        data.currentStopIndex = tag.getInteger("CurIdx");
        NBTTagList list = tag.getTagList("Stops", 10);

        for (int i = 0; i < list.tagCount(); ++i)
        {
            NBTTagCompound st = list.getCompoundTagAt(i);
            data.stops.add(new StopEntry(
                st.getString("Name"),
                st.getInteger("Wait"),
                st.getDouble("TX"),
                st.getDouble("TY"),
                st.getDouble("TZ"),
                st.getBoolean("HasCoord")));
        }

        return data;
    }
}
