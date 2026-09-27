package @@PKG@@;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vesna bridge entrypoint (Fabric). Implements the /@@MODID@@ command and a
 * small set of lifecycle/player events forwarded to Vesna scripts.
 */
public class @@MAIN@@ implements ModInitializer {

    public static VesnaBridge bridge;

    @Override
    public void onInitialize() {
        bridge = new VesnaBridge(new File(".").getAbsoluteFile());
        if (!bridge.available()) {
            System.out.println("[@@MODID@@] vesna runtime not found (set VESNA_HOME or config/vesna/runtime.properties)");
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("@@MODID@@")
                    .then(CommandManager.argument("script", StringArgumentType.string())
                            .executes(ctx -> {
                                String s = StringArgumentType.getString(ctx, "script");
                                Map<String, Object> r = bridge.run(s, new LinkedHashMap<>());
                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("vesna: " + VesnaJson.encode(r)), false);
                                return Command.SINGLE_SUCCESS;
                            })));
            dispatcher.register(CommandManager.literal("@@MODID@@-reload").executes(ctx -> {
                boolean ok = bridge.reload();
                ctx.getSource().sendFeedback(
                        () -> Text.literal("vesna events reloaded: " + ok), false);
                return Command.SINGLE_SUCCESS;
            }));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                bridge.fire("server_started", new LinkedHashMap<>()));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("player", p.getName().getString());
            payload.put("uuid", p.getUuid().toString());
            Map<String, Object> r = bridge.fire("player_join", payload);
            Object msg = r.get("message");
            if (msg != null) {
                server.getPlayerManager().broadcast(Text.literal(String.valueOf(msg)), false);
            }
        });
    }
}
