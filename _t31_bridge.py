# -*- coding: utf-8 -*-
import io

p = r'F:\Vesna-pkg\packages\vesna-mc\templates\VesnaBridge.java'
s = io.open(p, encoding='utf-8').read()

# 1) ActionSink +6
old = '''        void teleport(String player, double x, double y, double z);
        void sound(String player, String sound);
        void log(String text);
    }'''
new = '''        void teleport(String player, double x, double y, double z);
        void sound(String player, String sound);
        void title(String player, String title, String subtitle);
        void actionbar(String player, String text);
        void setBlock(int x, int y, int z, String block);
        void summon(String entity, double x, double y, double z);
        void spawnParticle(String particle, double x, double y, double z, int count);
        void scoreboard(String player, String objective, int score);
        void log(String text);
    }'''
assert s.count(old) == 1, 'sink'
s = s.replace(old, new)

# 2) 字段：throttle
old = '''    private Map<String, Object> events = new LinkedHashMap<String, Object>();
    private ScheduledExecutorService timers;'''
new = '''    private Map<String, Object> events = new LinkedHashMap<String, Object>();
    private final Map<String, Long> lastFire = new LinkedHashMap<String, Long>();
    private ScheduledExecutorService timers;'''
assert s.count(old) == 1, 'field'
s = s.replace(old, new)

# 3) fire() 节流 + tickInterval()
old = '''    /** Dispatch one event by name according to events.json; returns result or empty map. */
    public Map<String, Object> fire(String event, Map<String, Object> payload) {
        Object cfg = events.get(event);
        if (!(cfg instanceof Map)) return new LinkedHashMap<String, Object>();
        Object sc = ((Map<?, ?>) cfg).get("script");
        if (!(sc instanceof String) || ((String) sc).isEmpty()) return new LinkedHashMap<String, Object>();
        return run((String) sc, payload);
    }'''
new = '''    /** Dispatch one event by name according to events.json; returns result or empty map. */
    public Map<String, Object> fire(String event, Map<String, Object> payload) {
        Object cfg = events.get(event);
        if (!(cfg instanceof Map)) return new LinkedHashMap<String, Object>();
        Map<?, ?> m = (Map<?, ?>) cfg;
        Object sc = m.get("script");
        if (!(sc instanceof String) || ((String) sc).isEmpty()) return new LinkedHashMap<String, Object>();
        if (!throttleOk(event, m)) return new LinkedHashMap<String, Object>();
        return run((String) sc, payload);
    }

    /** Throttle high-frequency events via per-event "min_interval" (seconds). */
    private synchronized boolean throttleOk(String event, Map<?, ?> cfg) {
        Object mi = cfg.get("min_interval");
        if (!(mi instanceof Number)) return true;
        long minMs = Math.max(1, ((Number) mi).longValue()) * 1000L;
        long now = System.currentTimeMillis();
        Long last = lastFire.get(event);
        if (last != null && now - last.longValue() < minMs) return false;
        lastFire.put(event, Long.valueOf(now));
        return true;
    }

    /** server_tick interval in ticks (events.json top-level "tick_interval", default 20). */
    public int tickInterval() {
        Object v = events.get("tick_interval");
        if (v instanceof Number) return Math.max(1, ((Number) v).intValue());
        return 20;
    }'''
assert s.count(old) == 1, 'fire'
s = s.replace(old, new)

# 4) runActions +6 case
old = '''                case "sound":
                    sink.sound(player, str(m.get("sound")));
                    break;
                case "log":'''
new = '''                case "sound":
                    sink.sound(player, str(m.get("sound")));
                    break;
                case "title":
                    sink.title(player, str(m.get("title")), str(m.get("subtitle")));
                    break;
                case "actionbar":
                    sink.actionbar(player, str(m.get("text")));
                    break;
                case "set_block":
                    sink.setBlock(intOf(m.get("x"), 0), intOf(m.get("y"), 0), intOf(m.get("z"), 0), str(m.get("block")));
                    break;
                case "summon":
                    sink.summon(str(m.get("entity")), doubleOf(m.get("x")), doubleOf(m.get("y")), doubleOf(m.get("z")));
                    break;
                case "spawn_particle":
                    sink.spawnParticle(str(m.get("particle")), doubleOf(m.get("x")), doubleOf(m.get("y")), doubleOf(m.get("z")), intOf(m.get("count"), 10));
                    break;
                case "scoreboard":
                    sink.scoreboard(player, str(m.get("objective")), intOf(m.get("score"), 0));
                    break;
                case "log":'''
assert s.count(old) == 1, 'runActions'
s = s.replace(old, new)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('VesnaBridge upgraded')
