# -*- coding: utf-8 -*-
import io

p = r'F:\Vesna-pkg\packages\vesna-mc\templates\MainForge.java'
s = io.open(p, encoding='utf-8').read()

# 1) imports
old = '''import net.minecraftforge.event.RegisterCommandsEvent;
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
import net.minecraftforge.registries.ForgeRegistries;'''
new = '''import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;'''
assert s.count(old) == 1, 'imports'
s = s.replace(old, new)

# 2) 5 个新事件（onTick 前插入）+ tick 配置
old = '''    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter % 20 == 0) {
                Map<String, Object> pl = new LinkedHashMap<>();
                pl.put("tick", tickCounter / 20);
                fireWith("server_tick", pl);
            }
        }
    }'''
new = '''    @SubscribeEvent
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
    }'''
assert s.count(old) == 1, 'tick/events'
s = s.replace(old, new)

# 3) ActionSink +6
old = '''    @Override
    public void sound(String player, String sound) {
        if (server == null || player == null || sound == null) return;
        ServerPlayer p = server.getPlayerList().getPlayerByName(player);
        if (p == null) return;
        net.minecraft.sounds.SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(sound));
        if (s == null) return;
        p.level().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void log(String text) {'''
new = '''    @Override
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
    public void log(String text) {'''
assert s.count(old) == 1, 'sink'
s = s.replace(old, new)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('MainForge upgraded')
