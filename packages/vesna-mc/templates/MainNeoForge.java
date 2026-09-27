package @@PKG@@;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vesna bridge entrypoint (NeoForge 1.20.1+). Registers the /@@MODID@@ command
 * and forwards lifecycle/player events to Vesna scripts.
 */
@Mod("@@MODID@@")
public class @@MAIN@@ {

    public static VesnaBridge bridge;

    public @@MAIN@@(IEventBus bus) {
        bridge = new VesnaBridge(new File(".").getAbsoluteFile());
        if (!bridge.available()) {
            System.out.println("[@@MODID@@] vesna runtime not found (set VESNA_HOME or config/vesna/runtime.properties)");
        }
        bus.register(this);
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
        bridge.fire("server_started", new LinkedHashMap<>());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("player", p.getGameProfile().getName());
            payload.put("uuid", p.getGameProfile().getId().toString());
            Map<String, Object> r = bridge.fire("player_join", payload);
            Object msg = r.get("message");
            if (msg != null) {
                p.server.getPlayerList().broadcastSystemMessage(
                        Component.literal(String.valueOf(msg)), false);
            }
        }
    }
}
