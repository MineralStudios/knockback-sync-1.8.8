package me.caseload.knockbacksync.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.BoundingBox;
import com.github.retrooper.packetevents.util.Vector3d;
import me.caseload.knockbacksync.BukkitBase;
import me.caseload.knockbacksync.Platform;
import me.caseload.knockbacksync.world.FoliaWorld;
import me.caseload.knockbacksync.world.PlatformWorld;
import me.caseload.knockbacksync.world.SpigotWorld;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class BukkitPlayer implements PlatformPlayer {
    public final Player bukkitPlayer;
    private String clientBrand = "vanilla";

    // Reflection variables
    private static Class<?> craftPlayerClass;
    private static Method getHandleMethod;
    @Nullable
    private static Method getAttackStrengthScaleMethod;

    // 1.12.2 support
    static {
        try {
            // Check the current server version
            // If the version is greater than 1.14.4, use the Player method directly
            Object server = Bukkit.getServer().getClass().getDeclaredMethod("getServer").invoke(Bukkit.getServer());
            String bukkitPackage = Bukkit.getServer().getClass().getPackage().getName();

            // Step 1: Load the CraftPlayer class
            craftPlayerClass = Class.forName(bukkitPackage + ".entity.CraftPlayer");
            // Step 2: Get the getHandle method
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException("Method of Class required to support this version not found via reflection" + e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Cannot access required methods via reflection to support this version" + e);
        }
    }

    public BukkitPlayer(Player player) {
        this.bukkitPlayer = player;
    }

    @Override
    public UUID getUUID() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public double getX() {
        return bukkitPlayer.getLocation().getX();
    }

    @Override
    public double getY() {
        return bukkitPlayer.getLocation().getY();
    }

    @Override
    public double getZ() {
        return bukkitPlayer.getLocation().getZ();
    }

    @Override
    public float getPitch() {
        return bukkitPlayer.getLocation().getPitch();
    }

    @Override
    public float getYaw() {
        return bukkitPlayer.getLocation().getYaw();
    }

    @Override
    public boolean isOnGround() {
    /* Inconsistent with Entity.isOnGround()
    /  Checks to see if this player is currently standing on a block.
    /  This information may not be reliable, as it is a state provided by the client, and may therefore not be accurate.
    /  It can also easily be spoofed. We may want to cast to LivingEntity and call isOnGround() instead
    */
        return bukkitPlayer.isOnGround();
    }


    @Override
    public int getPing() {
        //if (currentVersion.isNewerThanOrEquals(ServerVersion.V_1_16_5)) {
        //return bukkitPlayer.getPing();
        //} else {
        return PacketEvents.getAPI().getPlayerManager().getPing(bukkitPlayer);
        //}
    }

    @Override
    public boolean isGliding() {
        return false /*bukkitPlayer.isGliding()*/;
    }

    @Override
    public PlatformWorld getWorld() {
        return BukkitBase.INSTANCE.getPlatform() == Platform.FOLIA ? new FoliaWorld(bukkitPlayer.getWorld()) : new SpigotWorld(bukkitPlayer.getWorld());
    }

    @Override
    public Vector3d getLocation() {
        org.bukkit.Location location = bukkitPlayer.getLocation();
        return new Vector3d(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public void sendMessage(@NotNull String s) {
        bukkitPlayer.sendMessage(s);
    }

    @Override
    public double getAttackCooldown() {
        return 0.0;
        /*if (currentVersion.isNewerThan(ServerVersion.V_1_14_4)) {
            return bukkitPlayer.getAttackCooldown();
        } else {
            try {
                // Step 1: Get the CraftPlayer instance
                // Step 2: Get the handle (NMS EntityPlayer)
                Object entityPlayer = getHandleMethod.invoke(bukkitPlayer);
                // Step 3: Invoke the getAttackStrengthScale method
                return (float) getAttackStrengthScaleMethod.invoke(entityPlayer, 0.5f);
            } catch (Exception e) {
                throw new IllegalStateException("This plugin will not work. NMS mapping for getAttackCooldown() failed!");
            }
        }*/
    }

    @Override
    public boolean isSprinting() {
        return bukkitPlayer.isSprinting();
    }

    @Override
    public int getMainHandKnockbackLevel() {
        return bukkitPlayer.getInventory().getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK);
    }

    @Override
    public @Nullable Integer getNoDamageTicks() {
        return bukkitPlayer.getNoDamageTicks();
    }

    @Override
    public void setVelocity(Vector3d adjustedVelocity) {
        bukkitPlayer.setVelocity(new Vector(adjustedVelocity.x, adjustedVelocity.y, adjustedVelocity.z));
    }

    @Override
    public Vector3d getVelocity() {
        final Vector bukkitVelocity = bukkitPlayer.getVelocity();
        return new Vector3d(bukkitVelocity.getX(), bukkitVelocity.getY(), bukkitVelocity.getZ());
    }

    @Override
    public double getJumpPower() {
        AtomicReference<Double> jumpVelocity = new AtomicReference<>(0.42);

        bukkitPlayer.getActivePotionEffects().stream().filter(effect -> effect.getType() == PotionEffectType.JUMP).findAny().ifPresent(
                jumpEffect -> {
                    int amplifier = jumpEffect.getAmplifier();
                    jumpVelocity.updateAndGet(v -> v + (amplifier + 1) * 0.1F);
                }
        );

        return jumpVelocity.get();
    }

    @Override
    public BoundingBox getBoundingBox() {
        double minX = bukkitPlayer.getLocation().getX() - 0.3;
        double minY = bukkitPlayer.getLocation().getY();
        double minZ = bukkitPlayer.getLocation().getZ() - 0.3;
        double maxX = bukkitPlayer.getLocation().getX() + 0.3;
        double maxY = bukkitPlayer.getLocation().getY() + 1.8;
        double maxZ = bukkitPlayer.getLocation().getZ() + 0.3;
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public User getUser() {
        return PacketEvents.getAPI().getPlayerManager().getUser(bukkitPlayer);
    }

    @Override
    public void setClientBrand(String brand) {
        this.clientBrand = brand;
    }

    @Override
    public String getClientBrand() {
        return this.clientBrand;
    }
}