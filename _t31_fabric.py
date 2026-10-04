# -*- coding: utf-8 -*-
import io

p = r'F:\Vesna-pkg\packages\vesna-mc\templates\MainFabric.java'
s = io.open(p, encoding='utf-8').read()

# 1) imports
old = '''import net.fabricmc.fabric.api.event.player.v1.PlayerAdvancementEvents;
import net.fabricmc.fabric.api.event.player.v1.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.v1.PlayerBlockPlacementEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;'''
new = '''import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.v1.PlayerAdvancementEvents;
import net.fabricmc.fabric.api.event.player.v1.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.v1.PlayerBlockPlacementEvents;
import net.fabricmc.fabric.api.event.player.v1.PlayerDropItemEvents;
import net.fabricmc.fabric.api.event.player.v1.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.v1.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleOptions;
import net.minecraft.registry.Registry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;'''
assert s.count(old) == 1, 'imports'
s = s.replace(old, new)

# 2) 5 个新事件注册（在 Advancement 后、ServerTick 前插入）
old = '''        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 20 == 0) {
                Map<String, Object> pl = new LinkedHashMap<>();
                pl.put("tick", tickCounter / 20);
                fireWith("server_tick", pl);
            }
        });'''
new = '''        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayerEntity p) {
                Map<String, Object> pl = playerPayload(p);
                pl.put("hand", hand.name().toLowerCase());
                pl.put("block", Registry.BLOCK.getId(world.getBlockState(hitResult.getBlockPos()).getBlock()).toString());
                pl.put("x", hitResult.getBlockPos().getX());
                pl.put("y", hitResult.getBlockPos().getY());
                pl.put("z", hitResult.getBlockPos().getZ());
                fireWith("player_use_block", pl);
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity p) {
                Map<String, Object> pl = playerPayload(p);
                pl.put("hand", hand.name().toLowerCase());
                ItemStack stack = p.getStackInHand(hand);
                pl.put("item", stack.isEmpty() ? "minecraft:air" : Registry.ITEM.getId(stack.getItem()).toString());
                fireWith("player_use_item", pl);
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            fireWith("player_respawn", playerPayload(newPlayer));
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, original, blocked) -> {
            Map<String, Object> pl = new LinkedHashMap<>();
            pl.put("entity", entity.getName().getString());
            pl.put("attacker", source.getAttacker() != null ? source.getAttacker().getName().getString() : "环境");
            pl.put("amount", (double) amount);
            fireWith("entity_damage", pl);
            return false;
        });

        PlayerDropItemEvents.BEFORE_DROP.register((player, itemStack) -> {
            if (player instanceof ServerPlayerEntity p) {
                Map<String, Object> pl = playerPayload(p);
                pl.put("item", Registry.ITEM.getId(itemStack.getItem()).toString());
                pl.put("count", itemStack.getCount());
                fireWith("player_drop_item", pl);
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            int ti = bridge.tickInterval();
            if (tickCounter % ti == 0) {
                Map<String, Object> pl = new LinkedHashMap<>();
                pl.put("tick", tickCounter / ti);
                fireWith("server_tick", pl);
            }
        });'''
assert s.count(old) == 1, 'tick/events'
s = s.replace(old, new)

# 3) ActionSink +6（在 sound 后、log 前）
old = '''    @Override
    public void sound(String player, String sound) {
        if (server == null || player == null || sound == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        net.minecraft.sound.SoundEvent s = Registry.SOUND_EVENT.get(new Identifier(sound));
        if (s == null) return;
        p.getWorld().playSound(null, p.getBlockPos(), s, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void log(String text) {'''
new = '''    @Override
    public void sound(String player, String sound) {
        if (server == null || player == null || sound == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        net.minecraft.sound.SoundEvent s = Registry.SOUND_EVENT.get(new Identifier(sound));
        if (s == null) return;
        p.getWorld().playSound(null, p.getBlockPos(), s, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void title(String player, String title, String subtitle) {
        if (server == null || player == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p == null || p.networkHandler == null) return;
        if (title != null && !title.isEmpty()) {
            p.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.TITLE, Text.literal(title), 10, 40, 10));
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            p.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.SUBTITLE, Text.literal(subtitle), 10, 40, 10));
        }
    }

    @Override
    public void actionbar(String player, String text) {
        if (server == null || player == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p == null || p.networkHandler == null) return;
        p.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.literal(text)));
    }

    @Override
    public void setBlock(int x, int y, int z, String block) {
        if (server == null || block == null) return;
        net.minecraft.block.Block b = Registry.BLOCK.get(new Identifier(block));
        if (b == null) return;
        server.getOverworld().setBlockState(new BlockPos(x, y, z), b.getDefaultState(), 3);
    }

    @Override
    public void summon(String entity, double x, double y, double z) {
        if (server == null || entity == null) return;
        EntityType<?> et = Registry.ENTITY_TYPE.get(new Identifier(entity));
        if (et == null) return;
        Entity e = et.create(server.getOverworld());
        if (e != null) {
            e.setPos(x, y, z);
            server.getOverworld().spawnEntity(e);
        }
    }

    @Override
    public void spawnParticle(String particle, double x, double y, double z, int count) {
        if (server == null || particle == null) return;
        net.minecraft.particle.ParticleType<?> pt = Registry.PARTICLE_TYPE.get(new Identifier(particle));
        if (pt == null) return;
        server.getOverworld().spawnParticles((ParticleOptions) pt, x, y, z, Math.max(1, count), 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    public void scoreboard(String player, String objective, int score) {
        if (server == null || player == null || objective == null) return;
        net.minecraft.scoreboard.Scoreboard sb = server.getScoreboard();
        ScoreboardObjective obj = sb.getOrCreateObjective(objective);
        if (obj == null) return;
        sb.getOrCreateScore(player, obj).setScore(score);
    }

    @Override
    public void log(String text) {'''
assert s.count(old) == 1, 'sink'
s = s.replace(old, new)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('MainFabric upgraded')
