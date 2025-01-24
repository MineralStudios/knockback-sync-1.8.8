package me.caseload.knockbacksync.world;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.caseload.knockbacksync.world.raytrace.FluidHandling;
import me.caseload.knockbacksync.world.raytrace.RayTraceResult;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.lang.reflect.Method;

public class SpigotWorld implements PlatformWorld {
    protected final World world;

    // Reflection variables
    private static Class<?> craftWorldClass;
    private static Class<?> vec3DClass;
    private static Class<?> movingObjectPositionClass;
    private static Class<?> enumDirectionClass;
    private static Method craftWorldGetHandleMethod;
    private static Method rayTraceMethod;
    private static Method vec3DAddMethod;

    static {
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_13)) {
            try {
                Object server = Bukkit.getServer().getClass().getDeclaredMethod("getServer").invoke(Bukkit.getServer());
                String nmsPackage = server.getClass().getPackage().getName();
                String bukkitPackage = Bukkit.getServer().getClass().getPackage().getName();

                // Load necessary classes dynamically for Minecraft 1.12.2
                craftWorldClass = Class.forName(bukkitPackage + ".CraftWorld");
                vec3DClass = Class.forName(nmsPackage + ".Vec3D");
                movingObjectPositionClass = Class.forName(nmsPackage + ".MovingObjectPosition");
                enumDirectionClass = Class.forName(nmsPackage + ".EnumDirection");

//                CraftWorld

                // Get method
                craftWorldGetHandleMethod = craftWorldClass.getMethod("getHandle");
                rayTraceMethod = Class.forName(nmsPackage + ".World").getMethod("rayTrace", vec3DClass, vec3DClass, boolean.class, boolean.class, boolean.class);
                vec3DAddMethod = vec3DClass.getMethod("add", double.class, double.class, double.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public SpigotWorld(World world) {
        this.world = world;
    }

    private static BlockFace getBlockFaceFrom(org.bukkit.block.BlockFace direction) {
        switch (direction) {
            case NORTH:
                return BlockFace.NORTH;
            case SOUTH:
                return BlockFace.SOUTH;
            case EAST:
                return BlockFace.EAST;
            case WEST:
                return BlockFace.WEST;
            case UP:
                return BlockFace.UP;
            case DOWN:
                return BlockFace.DOWN;
            default:
                return null;
        }
    }

    @Override
    public WrappedBlockState getBlockStateAt(int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        return SpigotConversionUtil.fromBukkitMaterialData(block.getState().getData());
    }

    @Override
    public WrappedBlockState getBlockStateAt(Vector3d loc) {
        return getBlockStateAt((int) Math.floor(loc.x), (int) Math.floor(loc.y), (int) Math.floor(loc.z));
    }

    @Override
    public RayTraceResult rayTraceBlocks(Vector3d start, Vector3d direction, double maxDistance, FluidHandling fluidHandling, boolean ignorePassableBlocks) {
        try {
            // Create start and end positions
            Object startPos = vec3DClass.getConstructor(double.class, double.class, double.class)
                    .newInstance(start.getX(), start.getY(), start.getZ());
            Object endPos = vec3DAddMethod.invoke(startPos, direction.getX(), direction.getY(), direction.getZ());

            // Call rayTrace method
            Object hitResult = rayTraceMethod.invoke(craftWorldGetHandleMethod.invoke(world), startPos, endPos, false, true, false);
            if (hitResult == null) return null;

            // Extract hit position and direction
            Vector3d hitPosition = new Vector3d(
                    (double) hitResult.getClass().getField("pos").get(hitResult),
                    (double) hitResult.getClass().getField("pos").get(hitResult).getClass().getField("y").get(hitResult.getClass().getField("pos").get(hitResult)),
                    (double) hitResult.getClass().getField("pos").get(hitResult).getClass().getField("z").get(hitResult.getClass().getField("pos").get(hitResult))
            );

            Object hitDirection = hitResult.getClass().getField("direction").get(hitResult);
            Object hitBlock = hitResult.getClass().getField("e").get(hitResult); // e = BlockHitResult

            return new RayTraceResult(
                    hitPosition,
                    getHitBlockFace(hitDirection),
                    new Vector3i((int) hitBlock.getClass().getField("x").get(hitBlock),
                            (int) hitBlock.getClass().getField("y").get(hitBlock),
                            (int) hitBlock.getClass().getField("z").get(hitBlock)),
                    hitBlock != null ? WrappedBlockState.getByString(hitBlock.getClass().getField("type").get(hitBlock).toString()) : null
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null if there was an error during ray tracing
        }
    }

    private static BlockFace getHitBlockFace(Object enumDirection) {
        try {
            // Match the EnumDirection to the corresponding BlockFace
            if (enumDirection.equals(SpigotWorld.enumDirectionClass.getField("NORTH").get(null))) {
                return BlockFace.NORTH;
            } else if (enumDirection.equals(SpigotWorld.enumDirectionClass.getField("SOUTH").get(null))) {
                return BlockFace.SOUTH;
            } else if (enumDirection.equals(SpigotWorld.enumDirectionClass.getField("EAST").get(null))) {
                return BlockFace.EAST;
            } else if (enumDirection.equals(SpigotWorld.enumDirectionClass.getField("WEST").get(null))) {
                return BlockFace.WEST;
            } else if (enumDirection.equals(SpigotWorld.enumDirectionClass.getField("UP").get(null))) {
                return BlockFace.UP;
            } else if (enumDirection.equals(SpigotWorld.enumDirectionClass.getField("DOWN").get(null))) {
                return BlockFace.DOWN;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Unexpected value: " + enumDirection);
    }
}