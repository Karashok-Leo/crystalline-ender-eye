package net.karashokleo.c_ender_eye;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Set;

public class CrystallineEnderEyeItem extends Item
{
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String DIM_KEY = "dimension";
    private static final String POS_KEY = "Position";

    public CrystallineEnderEyeItem()
    {
        super(new FabricItemSettings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand)
    {
        ItemStack stack = player.getStackInHand(hand);
        if (player.isSneaking())
        {
            NbtCompound compound = stack.getNbt() == null ? stack.getOrCreateNbt() : stack.getNbt();
            if (compound.getCompound(POS_KEY).isEmpty() || CrystallineEnderEye.config.allowRewritePositionForFixedPointWarp)
            {
                writePosition(compound, player);
                if (CrystallineEnderEye.config.enableCoolDownAfterWritePosition)
                    player.getItemCooldownManager().set(this, CrystallineEnderEye.config.coolDownAfterWritePosition);
                if (world.isClient())
                    player.sendMessage(Text.translatable("text.c_ender_eye.write_pos.success").formatted(Formatting.GREEN), CrystallineEnderEye.config.message_overlay);
            } else
            {
                if (world.isClient())
                    player.sendMessage(Text.translatable("text.c_ender_eye.write_pos.fail").formatted(Formatting.RED), CrystallineEnderEye.config.message_overlay);
            }
        } else
        {
            boolean randomWarp = false;
            boolean fixedPointWarp = false;
            NbtCompound pos = stack.getSubNbt(POS_KEY);
            if (pos == null) randomWarp = randomWarp(player);
            else
            {
                MinecraftServer server = player.getServer();
                if (server == null) return TypedActionResult.success(stack);
                fixedPointWarp = fixedPointWarp(pos, server, player);
            }
            setWarpCoolDown(randomWarp, fixedPointWarp, player, stack);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

//    @Override
//    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker)
//    {
//        if (!attacker.getWorld().isClient())
//        {
//            boolean randomWarp = false;
//            boolean fixedPointWarp = false;
//            NbtCompound pos = stack.getSubNbt(POS_KEY);
//            if (pos == null) randomWarp = randomWarp(target);
//            else
//            {
//                MinecraftServer server = attacker.getServer();
//                if (server == null) return true;
//                fixedPointWarp = fixedPointWarp(pos, server, target);
//            }
//            if (attacker instanceof PlayerEntity player)
//                setWarpCoolDown(randomWarp, fixedPointWarp, player, stack);
//        }
//        return false;
//    }

    // 使用AttackEntityCallback，取消攻击后的影响
    @SuppressWarnings("unused")
    public static ActionResult attack(PlayerEntity player, World world, Hand hand, Entity entity, HitResult hitResult)
    {
        if (!world.isClient() && (entity.isAttackable()) && (entity instanceof LivingEntity target))
        {
            ItemStack stack = player.getStackInHand(hand);
            boolean randomWarp = false;
            boolean fixedPointWarp = false;
            NbtCompound pos = stack.getSubNbt(POS_KEY);
            if (pos == null) randomWarp = randomWarp(target);
            else fixedPointWarp = fixedPointWarp(pos, world.getServer(), target);
            setWarpCoolDown(randomWarp, fixedPointWarp, player, stack);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private static void setWarpCoolDown(boolean randomWarp, boolean fixedPointWarp, PlayerEntity player, ItemStack stack)
    {
        if (randomWarp || fixedPointWarp)
        {
            if (randomWarp && CrystallineEnderEye.config.enableCoolDownAfterRandomWarp)
                player.getItemCooldownManager().set(stack.getItem(), CrystallineEnderEye.config.coolDownAfterRandomWarp);
            else if (fixedPointWarp && CrystallineEnderEye.config.enableCoolDownAfterFixedPointWarp)
                player.getItemCooldownManager().set(stack.getItem(), CrystallineEnderEye.config.coolDownAfterFixedPointWarp);
            if (!player.isCreative())
                stack.decrement(1);
        }
    }

    private static void writePosition(NbtCompound compound, PlayerEntity player)
    {
        NbtCompound position = new NbtCompound();
        position.putDouble(X_KEY, player.getX());
        position.putDouble(Y_KEY, player.getY());
        position.putDouble(Z_KEY, player.getZ());
        position.putString(DIM_KEY, player.getWorld().getRegistryKey().getValue().toString());
        compound.put(POS_KEY, position);
    }

    public static boolean fixedPointWarp(NbtCompound position, MinecraftServer server, LivingEntity entity)
    {
        if (entity.getWorld().isClient()) return false;
        if (!CrystallineEnderEye.config.enableFixedPointWarp)
        {
            if (entity instanceof PlayerEntity player)
                player.sendMessage(Text.translatable("text.c_ender_eye.fixed_point_warp.disabled").formatted(Formatting.RED), CrystallineEnderEye.config.message_overlay);
            return false;
        }
        Vec3d origin = entity.getPos();
        Vec3d target = new Vec3d(position.getDouble(X_KEY), position.getDouble(Y_KEY), position.getDouble(Z_KEY));
        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(position.getString(DIM_KEY))));
        boolean notDimensional = world == entity.getWorld();

        if (notDimensional && fixedPointWarpOutOfRange(origin, target))
        {
            if (entity instanceof PlayerEntity player)
                player.sendMessage(Text.translatable("text.c_ender_eye.fixed_point_warp.out_of_range").formatted(Formatting.RED), CrystallineEnderEye.config.message_overlay);
            return false;
        }

        if (notDimensional) return warp(entity, origin, target);
        else return dimensionalWarp(entity, world, origin, target);
    }

    public static boolean randomWarp(LivingEntity entity)
    {
        if (entity.getWorld().isClient()) return false;
        if (!CrystallineEnderEye.config.enableRandomWarp)
        {
            if (entity instanceof PlayerEntity player)
                player.sendMessage(Text.translatable("text.c_ender_eye.random_warp.disabled").formatted(Formatting.RED), CrystallineEnderEye.config.message_overlay);
            return false;
        }
        if (entity.hasVehicle())
            entity.stopRiding();
        Vec3d origin = entity.getPos();
        for (int i = 0; i < CrystallineEnderEye.config.attemptsForRandomWarp; i++)
            if (warp(entity, origin, calculateRandomWarp(entity))) return true;
        return false;
    }

    private static boolean dimensionalWarp(LivingEntity entity, ServerWorld targetWorld, Vec3d origin, Vec3d target)
    {
        if (!CrystallineEnderEye.config.allowDimensionalWarp)
        {
            if (entity instanceof PlayerEntity player)
                player.sendMessage(Text.translatable("text.c_ender_eye.dimensional_warp.disabled").formatted(Formatting.RED), CrystallineEnderEye.config.message_overlay);
            return false;
        }
        if (entity.teleport(targetWorld, target.getX(), target.getY(), target.getZ(), Set.of(), entity.getYaw(), entity.getPitch()))
        {
            World world = entity.getWorld();
            world.sendEntityStatus(entity, EntityStatuses.ADD_PORTAL_PARTICLES);
            warpSound(entity, origin, world);
            return true;
        } else return false;
    }

    private static void warpSound(LivingEntity entity, Vec3d origin, World world)
    {
        world.emitGameEvent(GameEvent.TELEPORT, origin, GameEvent.Emitter.of(entity));
        SoundEvent soundEvent = entity instanceof FoxEntity ? SoundEvents.ENTITY_FOX_TELEPORT : SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
        world.playSound(null, origin.getX(), origin.getY(), origin.getZ(), soundEvent, entity instanceof PlayerEntity ? SoundCategory.PLAYERS : SoundCategory.AMBIENT, 1.0f, 1.0f);
        entity.playSound(soundEvent, 1.0f, 1.0f);
    }

    private static boolean warp(LivingEntity entity, Vec3d origin, Vec3d target)
    {
        if (entity.teleport(target.getX(), target.getY(), target.getZ(), true))
        {
            warpSound(entity, origin, entity.getWorld());
            return true;
        } else return false;
    }

    private static boolean fixedPointWarpOutOfRange(Vec3d origin, Vec3d target)
    {
        double distance = origin.squaredDistanceTo(target);
        return distance <
                CrystallineEnderEye.config.minDistanceForFixedPointWarp * CrystallineEnderEye.config.minDistanceForFixedPointWarp
                || distance >
                CrystallineEnderEye.config.maxDistanceForFixedPointWarp * CrystallineEnderEye.config.maxDistanceForFixedPointWarp;
    }

    private static Vec3d calculateRandomWarp(LivingEntity entity)
    {
        World world = entity.getWorld();
        Random random = entity.getRandom();
        double distance = random.nextDouble() * (CrystallineEnderEye.config.maxDistanceForRandomWarp - CrystallineEnderEye.config.minDistanceForRandomWarp) + CrystallineEnderEye.config.minDistanceForRandomWarp;
        double theta = Math.PI * random.nextDouble();
        double phi = Math.PI * random.nextDouble() * 2;
        double limitedY = MathHelper.clamp(distance * Math.cos(theta), world.getBottomY(), world.getBottomY() + ((ServerWorld) world).getLogicalHeight() - 1);
        return entity.getPos().add(
                distance * Math.sin(theta) * Math.cos(phi),
                limitedY,
                distance * Math.sin(theta) * Math.sin(phi)
        );
    }
}
