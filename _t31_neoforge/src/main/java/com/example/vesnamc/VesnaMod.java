package com.example.vesnamc;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.scores.ScoreboardObjective;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vesna bridge entrypoint (NeoForge 1.20.1+).
 *
 * Events forwarded to scripts: server_started / server_stopped / player_join /
 * player_leave / player_death / player_kill / block_break / block_place /
 * player_chat / player_advancement / server_tick (every 20 ticks = 1s).
 *
 * Scripts may return {"message": ...} or {"actions": [...]}; actions are
 * executed through the ActionSink implementation below.
 */
@Mod("vesnamc")
public class VesnaMod implements VesnaBridge.ActionSink {

    public static VesnaBridge bridge;
    private MinecraftServer server;
    private long tickCounter;

    public VesnaMod(IEventBus bus) {
        bridge = new VesnaBridge(new File(".").getAbsoluteFile());
        if (!bridge.available()) {
            System.out.println("[vesnamc] vesna runtime not found (set VESNA_HOME or config/vesna/runtime.properties)");
        }
        bus.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vesnamc")
                .then(Commands.argument("script", StringArgumentType.string())
                        .executes(ctx -> {
                            String s = StringArgumentType.getString(ctx, "script");
                            Map<String, Object> r = bridge.run(s, new LinkedHashMap<>());
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("vesna: " + VesnaJson.encode(r)), false);
                            return Command.SINGLE_SUCCESS;
                        })));
        event.getDispatcher().register(Commands.literal("vesnamc-reload").executes(ctx -> {
            boolean ok = bridge.reload();
            ctx.getSource().sendSuccess(
                    () -> Component.literal("vesna events reloaded: " + ok), false);
            return Command.SINGLE_SUCCESS;
        }));
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.server = event.getServer();
        bridge.startTimers(this);
        if (bridge.residentEnabled()) bridge.startResident(this);
        fireWith("server_started", new LinkedHashMap<>());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        bridge.stopResident();
        fireWith("server_stopped", new LinkedHashMap<>());
        bridge.stopTimers();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            this.server = p.server;
            fireWith("player_join", playerPayload(p));
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            fireWith("player_leave", playerPayload(p));
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Map<String, Object> pl = playerPayload(p);
            pl.put("message", p.getCombatTracker().getDeathMessage().getString());
            fireWith("player_death", pl);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer
                && event.getEntity() instanceof net.minecraft.world.entity.LivingEntity victim) {
            Map<String, Object> pl = playerPayload(killer);
            pl.put("victim", victim.getName().getString());
            fireWith("player_kill", pl);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p) {
            Map<String, Object> pl = playerPayload(p);
            pl.put("block", ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()).toString());
            pl.put("x", event.getPos().getX());
            pl.put("y", event.getPos().getY());
            pl.put("z", event.getPos().getZ());
            fireWith("block_break", pl);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Map<String, Object> pl = playerPayload(p);
            pl.put("block", ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()).toString());
            pl.put("x", event.getPos().getX());
            pl.put("y", event.getPos().getY());
            pl.put("z", event.getPos().getZ());
            fireWith("block_place", pl);
        }
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ServerPlayer p = event.getPlayer();
        if (p != null) {
            Map<String, Object> pl = playerPayload(p);
            pl.put("message", event.getMessage().getString());
            fireWith("player_chat", pl);
        }
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Map<String, Object> pl = playerPayload(p);
            pl.put("advancement", String.valueOf(event.getAdvancement().getId()));
            fireWith("player_advancement", pl);
        }
    }

    @SubscribeEvent
    public void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Map<String, Object> pl = playerPayload(p);
            pl.put("hand", event.getHand().name().toLowerCase());
            pl.put("block", ForgeRegistries.BLOCKS.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()).toString());
            pl.put("x", event.getPos().getX());
            pl.put("y", event.getPos().getY());
            pl.put("z", event.getPos().getZ());
            fireWith("player_use_block", pl);
        }
    }

    @SubscribeEvent
    public void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Map<String, Object> pl = playerPayload(p);
            pl.put("hand", event.getHand().name().toLowerCase());
            net.minecraft.world.item.Item i = event.getItemStack().getItem();
            pl.put("item", ForgeRegistries.ITEMS.getKey(i) == null ? "minecraft:air" : ForgeRegistries.ITEMS.getKey(i).toString());
            fireWith("player_use_item", pl);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            this.server = p.server;
            fireWith("player_respawn", playerPayload(p));
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        Map<String, Object> pl = new LinkedHashMap<>();
        pl.put("entity", event.getEntity().getName().getString());
        pl.put("attacker", event.getSource().getEntity() != null ? event.getSource().getEntity().getName().getString() : "环境");
        pl.put("amount", (double) event.getAmount());
        fireWith("entity_damage", pl);
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        ServerPlayer p = event.getPlayer();
        if (p != null) {
            Map<String, Object> pl = playerPayload(p);
            net.minecraft.world.item.Item i = event.getEntity().getItem().getItem();
            pl.put("item", ForgeRegistries.ITEMS.getKey(i) == null ? "minecraft:air" : ForgeRegistries.ITEMS.getKey(i).toString());
            pl.put("count", event.getEntity().getItem().getCount());
            fireWith("player_drop_item", pl);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            int ti = bridge.tickInterval();
            if (tickCounter % ti == 0) {
                Map<String, Object> pl = new LinkedHashMap<>();
                pl.put("tick", tickCounter / ti);
                fireWith("server_tick", pl);
            }
        }
    }

    // ---------- event dispatch ----------

    private void fireWith(String event, Map<String, Object> payload) {
        if (bridge.residentActive()) bridge.sendResident(event, payload);
        else bridge.fireWithActions(this, event, payload);
    }

    private Map<String, Object> playerPayload(ServerPlayer p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("player", p.getGameProfile().getName());
        m.put("uuid", p.getGameProfile().getId().toString());
        m.put("op", server != null && server.getPlayerList().isOp(p.getGameProfile()));
        return m;
    }

    // ---------- ActionSink ----------

    @Override
    public void message(String target, String text) {
        if (server == null) return;
        if (target == null || target.isEmpty() || target.equals("all")) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
        } else if (target.startsWith("player:")) {
            ServerPlayer p = server.getPlayerList().getPlayerByName(target.substring("player:".length()));
            if (p != null) p.sendSystemMessage(Component.literal(text));
        }
    }

    @Override
    public void command(String cmd) {
        if (server != null && cmd != null && !cmd.isEmpty()) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
        }
    }

    @Override
    public void give(String player, String item, int count) {
        if (server == null || player == null || item == null) return;
        ServerPlayer p = server.getPlayerList().getPlayerByName(player);
        if (p == null) return;
        net.minecraft.world.item.Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(item));
        if (i == null) return;
        ItemStack stack = new ItemStack(i, Math.max(1, count));
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
    }

    @Override
    public void kick(String player, String reason) {
        if (server == null || player == null) return;
        ServerPlayer p = server.getPlayerList().getPlayerByName(player);
        if (p != null && p.connection != null) {
            p.connection.disconnect(Component.literal(reason == null ? "kicked by vesna" : reason));
        }
    }

    @Override
    public void effect(String player, String effect, int duration, int level) {
        if (server == null || player == null || effect == null) return;
        ServerPlayer p = server.getPlayerList().getPlayerByName(player);
        if (p == null) return;
        net.minecraft.world.effect.MobEffect e = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effect));
        if (e == null) return;
        p.addEffect(new MobEffectInstance(e, Math.max(1, duration) * 20, Math.max(0, level - 1)));
    }

    @Override
    public void teleport(String player, double x, double y, double z) {
        if (server == null || player == null) return;
        ServerPlayer p = server.getPlayerList().getPlayerByName(player);
        if (p == null) return;
        p.teleportTo(x, y, z);
    }

    @Override
    public void sound(String player, String sound) {
        if (server == null || player == null || sound == null) return;
        ServerPlayer p = server.getPlayerList().getPlayerByName(player);
        if (p == null) return;
        net.minecraft.sounds.SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(sound));
        if (s == null) return;
        p.level().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void title(String player, String title, String subtitle) {
        if (server == null || player == null) return;
        ServerPlayer p = server.getPlayerList().getPlayerByName(player);
        if (p == null || p.connection == null) return;
        if (title != null && !title.isEmpty()) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
    }

    @Override
    public void actionbar(String player, String text) {
        if (server == null || player == null) return;
        ServerPlayer p = server.getPlayerList().getPlayerByName(player);
        if (p == null || p.connection == null) return;
        p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));
    }

    @Override
    public void setBlock(int x, int y, int z, String block) {
        if (server == null || block == null) return;
        net.minecraft.world.level.block.Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(block));
        if (b == null) return;
        server.overworld().setBlockAndUpdate(new BlockPos(x, y, z), b.defaultBlockState());
    }

    @Override
    public void summon(String entity, double x, double y, double z) {
        if (server == null || entity == null) return;
        EntityType<?> et = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entity));
        if (et == null) return;
        Entity e = et.create(server.overworld());
        if (e != null) {
            e.moveTo(x, y, z);
            server.overworld().addFreshEntity(e);
        }
    }

    @Override
    public void spawnParticle(String particle, double x, double y, double z, int count) {
        if (server == null || particle == null) return;
        net.minecraft.core.particles.ParticleType<?> pt = ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation(particle));
        if (pt == null) return;
        server.overworld().sendParticles((ParticleOptions) pt, x, y, z, Math.max(1, count), 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    public void scoreboard(String player, String objective, int score) {
        if (server == null || player == null || objective == null) return;
        net.minecraft.world.scores.Scoreboard sb = server.getScoreboard();
        ScoreboardObjective obj = sb.getOrCreateObjective(objective);
        if (obj == null) return;
        sb.getOrCreatePlayerScore(player, obj).setScore(score);
    }

    @Override
    public void log(String text) {
        System.out.println("[vesna-mc] " + (text == null ? "" : text));
    }
}
