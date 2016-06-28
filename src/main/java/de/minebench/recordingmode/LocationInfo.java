package de.minebench.recordingmode;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.NumberConversions;

import java.util.LinkedHashMap;
import java.util.Map;

@SerializableAs("LocationInfo")
public class LocationInfo implements ConfigurationSerializable {
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw = 0;
    private float pitch = 0;

    private LocationInfo(String world, double x, double y, double z) {
        Preconditions.checkArgument(world != null, "world");
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LocationInfo(String world, double x, double y, double z, float yaw, float pitch) {
        this(world, x, y, z);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public LocationInfo(Location location) {
        this(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Location toBukkit() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{world=" + getWorld() + ",x=" + getX() + ",y=" + getY() + ",z=" + getZ() + ",yaw=" + getYaw() + ",pitch=" + getPitch() + "}";
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("world", world);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("pitch", pitch);
        map.put("yaw", yaw);
        return map;
    }

    public static LocationInfo deserialize(Map<String, Object> args) {
        return new LocationInfo(
                (String)args.get("world"),
                NumberConversions.toDouble(args.get("x")),
                NumberConversions.toDouble(args.get("y")),
                NumberConversions.toDouble(args.get("z")),
                NumberConversions.toFloat(args.get("yaw")),
                NumberConversions.toFloat(args.get("pitch"))
        );
    }
}
