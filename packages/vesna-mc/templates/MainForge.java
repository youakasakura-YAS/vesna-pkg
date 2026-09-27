package @@PKG@@;

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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vesna bridge entrypoint (Forge 1.20.1+).
 *
 * Events forwarded to scripts: server_started / server_stopped / player_join /
 * player_leave / player_death / player_kill / block_break / block_place /
 * player_chat / player_advancement / server_tick (every 20 ticks = 1s).
 *
 * Scripts may return {"message": ...} or {"actions": [...]}; actions are
 * executed through the ActionSink implementation below.
 */
@Mod("@@MODID@@")
public class @@MAIN@@ implements VesnaBridge.ActionSink {

    public static VesnaBridge bridge;
    private MinecraftServer server;
    private long tickCounter;

    public @@MAIN@@() {
        bridge = new VesnaBridge(new File(".").getAbsoluteFile());
        if (!bridge.available()) {
            System.out.println("[@@MODID@@] vesna runtime not found (set VESNA_HOME or config/vesna/runtime.properties)");
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("@@MODID@@")
                .then(Commands.argument("script", StringArgumentType.string())
                        .executes(ctx -> {
                            String s = StringArgumentType.getString(ctx, "script");
                            Map<String, Object> r = bridge.run(s, new LinkedHashMap<>());
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("vesna: " + VesnaJson.encode(r)), false);
                            return Command.SINGLE_SUCCESS;
                        })));
        event.getDispatcher().register(Commands.literal("@@MODID@@-reload").executes(ctx -> {
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
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter % 20 == 0) {
                Map<String, Object> pl = new LinkedHashMap<>();
                pl.put("tick", tickCounter / 20);
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
    public void log(String text) {
        System.out.println("[vesna-mc] " + (text == null ? "" : text));
    }
}
