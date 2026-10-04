package com.example.vesnamc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vesna bridge entrypoint (Fabric 1.20.1+).
 *
 * Events forwarded to scripts: server_started / server_stopped / player_join /
 * player_leave / player_death / player_kill / block_break / block_place /
 * player_chat / player_advancement / server_tick (every 20 ticks = 1s).
 *
 * Scripts may return {"message": ...} or {"actions": [...]}; actions are
 * executed through the ActionSink implementation below.
 */
public class VesnaMod implements ModInitializer, VesnaBridge.ActionSink {

    public static VesnaBridge bridge;
    private MinecraftServer server;
    private long tickCounter;

    @Override
    public void onInitialize() {
        bridge = new VesnaBridge(new File(".").getAbsoluteFile());
        if (!bridge.available()) {
            System.out.println("[vesnamc] vesna runtime not found (set VESNA_HOME or config/vesna/runtime.properties)");
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("vesnamc")
                    .then(CommandManager.argument("script", StringArgumentType.string())
                            .executes(ctx -> {
                                String s = StringArgumentType.getString(ctx, "script");
                                Map<String, Object> r = bridge.run(s, new LinkedHashMap<>());
                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("vesna: " + VesnaJson.encode(r)), false);
                                return Command.SINGLE_SUCCESS;
                            })));
            dispatcher.register(CommandManager.literal("vesnamc-reload").executes(ctx -> {
                boolean ok = bridge.reload();
                ctx.getSource().sendFeedback(
                        () -> Text.literal("vesna events reloaded: " + ok), false);
                return Command.SINGLE_SUCCESS;
            }));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            bridge.startTimers(this);
            if (bridge.residentEnabled()) bridge.startResident(this);
            fireWith("server_started", new LinkedHashMap<>());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            bridge.stopResident();
            fireWith("server_stopped", new LinkedHashMap<>());
            bridge.stopTimers();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            this.server = server;
            fireWith("player_join", playerPayload(handler.getPlayer()));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            this.server = server;
            fireWith("player_leave", playerPayload(handler.getPlayer()));
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) {
                Map<String, Object> pl = playerPayload(p);
                pl.put("message", p.getDamageTracker().getDeathMessage().getString());
                fireWith("player_death", pl);
            }
            if (source.getAttacker() instanceof ServerPlayerEntity killer && entity instanceof LivingEntity) {
                Map<String, Object> pl = playerPayload(killer);
                pl.put("victim", entity.getName().getString());
                fireWith("player_kill", pl);
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity p) {
                Map<String, Object> pl = playerPayload(p);
                pl.put("block", Registry.BLOCK.getId(state.getBlock()).toString());
                pl.put("x", pos.getX());
                pl.put("y", pos.getY());
                pl.put("z", pos.getZ());
                fireWith("block_break", pl);
            }
        });

        PlayerBlockPlacementEvents.AFTER.register((world, pos, state, blockEntity, entity) -> {
            if (entity instanceof ServerPlayerEntity p) {
                Map<String, Object> pl = playerPayload(p);
                pl.put("block", Registry.BLOCK.getId(state.getBlock()).toString());
                pl.put("x", pos.getX());
                pl.put("y", pos.getY());
                pl.put("z", pos.getZ());
                fireWith("block_place", pl);
            }
        });

        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            Map<String, Object> pl = new LinkedHashMap<>();
            pl.put("message", message.getString());
            fireWith("player_chat", pl);
        });

        PlayerAdvancementEvents.ADVANCEMENT_PROGRESS_UPDATE.register((player, advancement, progress) -> {
            Map<String, Object> pl = playerPayload(player);
            pl.put("advancement", advancement.id().toString());
            fireWith("player_advancement", pl);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
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
        });
    }

    // ---------- event dispatch ----------

    private void fireWith(String event, Map<String, Object> payload) {
        if (bridge.residentActive()) bridge.sendResident(event, payload);
        else bridge.fireWithActions(this, event, payload);
    }

    private Map<String, Object> playerPayload(ServerPlayerEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("player", p.getName().getString());
        m.put("uuid", p.getUuid().toString());
        m.put("op", server != null && server.getPlayerManager().isOperator(p.getGameProfile()));
        return m;
    }

    // ---------- ActionSink ----------

    @Override
    public void message(String target, String text) {
        if (server == null) return;
        if (target == null || target.isEmpty() || target.equals("all")) {
            server.getPlayerManager().broadcast(Text.literal(text), false);
        } else if (target.startsWith("player:")) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(target.substring("player:".length()));
            if (p != null) p.sendMessage(Text.literal(text), false);
        }
    }

    @Override
    public void command(String cmd) {
        if (server != null && cmd != null && !cmd.isEmpty()) {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), cmd);
        }
    }

    @Override
    public void give(String player, String item, int count) {
        if (server == null || player == null || item == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        Item i = Registry.ITEM.get(new Identifier(item));
        if (i == null) return;
        p.getInventory().offerOrDrop(new ItemStack(i, Math.max(1, count)));
    }

    @Override
    public void kick(String player, String reason) {
        if (server == null || player == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p != null && p.networkHandler != null) {
            p.networkHandler.disconnect(Text.literal(reason == null ? "kicked by vesna" : reason));
        }
    }

    @Override
    public void effect(String player, String effect, int duration, int level) {
        if (server == null || player == null || effect == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        net.minecraft.entity.effect.StatusEffect e = Registry.STATUS_EFFECT.get(new Identifier(effect));
        if (e == null) return;
        p.addStatusEffect(new StatusEffectInstance(e, Math.max(1, duration) * 20, Math.max(0, level - 1)));
    }

    @Override
    public void teleport(String player, double x, double y, double z) {
        if (server == null || player == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        p.teleport(server.getOverworld(), x, y, z, p.getYaw(), p.getPitch());
    }

    @Override
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
    public void log(String text) {
        System.out.println("[vesna-mc] " + (text == null ? "" : text));
    }
}
