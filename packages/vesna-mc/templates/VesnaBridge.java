package @@PKG@@;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Bridge between a Minecraft mod and the Vesna language runtime.
 *
 * Execution model: every event, command or timer tick spawns a short-lived
 * `vesna` process (one-shot scripts). The script receives the event payload as
 * argv[1] (a JSON string) and returns its result by printing a single JSON
 * object as the last line of stdout.
 *
 * Protocol:
 *   script.ves        ->  argv[1] = JSON payload
 *   stdout (last line)->  JSON result object
 *
 * Result object supports:
 *   { "message": "text" }                          quick broadcast
 *   { "actions": [ { "type": "...", ... } ] }      action list (see ActionSink)
 *
 * Events map to scripts via config/vesna/events.json:
 *   {
 *     "server_started": { "script": "server_started.ves" },
 *     "player_join":    { "script": "player_join.ves" },
 *     ...
 *     "timers": [ { "script": "timer_example.ves", "seconds": 30 } ]
 *   }
 *
 * Locating the runtime (first match wins):
 *   1. config/vesna/runtime.properties  -> vesna.path
 *   2. environment variable VESNA_HOME  -> <home>/bin/vesna(.exe)
 *   3. PATH lookup for `vesna` / `vesna.exe`
 */
public final class VesnaBridge {

    public static final String CONFIG_DIR = "config/vesna";
    public static final String EVENTS_FILE = "events.json";
    public static final String RUNTIME_PROPS = "runtime.properties";
    public static final long TIMEOUT_SECONDS = 30;

    private ResidentBridge resident;

    /**
     * Actions a script may request. Implemented by the platform entrypoint
     * (each loader exposes slightly different APIs).
     */
    public interface ActionSink {
        /** target: "all" or "player:<name>". */
        void message(String target, String text);
        void command(String cmd);
        void give(String player, String item, int count);
        void kick(String player, String reason);
        void effect(String player, String effect, int duration, int level);
        void teleport(String player, double x, double y, double z);
        void sound(String player, String sound);
        void log(String text);
    }

    private final File configDir;
    private final File runtimeDir;   // directory the vesna process is spawned from (script root)
    private final String vesnaPath;
    private Map<String, Object> events = new LinkedHashMap<String, Object>();
    private ScheduledExecutorService timers;

    public VesnaBridge(File gameDir) {
        this.configDir = new File(gameDir, CONFIG_DIR);
        this.runtimeDir = new File(configDir, "scripts");
        this.vesnaPath = findVesna();
        reload();
    }

    /** Find the vesna executable. Returns null when not found. */
    public String findVesna() {
        // 1) runtime.properties
        File props = new File(configDir, RUNTIME_PROPS);
        if (props.isFile()) {
            try {
                for (String line : Files.readAllLines(props.toPath(), StandardCharsets.UTF_8)) {
                    line = line.trim();
                    if (line.startsWith("vesna.path=")) {
                        String p = line.substring("vesna.path=".length()).trim();
                        if (!p.isEmpty() && new File(p).isFile()) return p;
                    }
                }
            } catch (IOException ignored) {}
        }
        // 2) VESNA_HOME
        String home = System.getenv("VESNA_HOME");
        if (home != null && !home.isEmpty()) {
            String exe = home + File.separator + "bin" + File.separator + (isWindows() ? "vesna.exe" : "vesna");
            File f = new File(exe);
            if (f.isFile()) return f.getAbsolutePath();
        }
        // 3) PATH
        String pathExe = which(isWindows() ? "vesna.exe" : "vesna");
        if (pathExe != null) return pathExe;
        return null;
    }

    public String vesnaPath() { return vesnaPath; }

    public boolean available() { return vesnaPath != null; }

    /** (Re)load config/vesna/events.json into memory. */
    public synchronized boolean reload() {
        File f = new File(configDir, EVENTS_FILE);
        if (!f.isFile()) { events = new LinkedHashMap<String, Object>(); return false; }
        try {
            String txt = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Object parsed = VesnaJson.parse(txt);
            if (parsed instanceof Map) events = VesnaJson.cast(parsed);
            else events = new LinkedHashMap<String, Object>();
            return true;
        } catch (Exception e) {
            System.err.println("[vesna-mc] failed to parse " + f + ": " + e.getMessage());
            events = new LinkedHashMap<String, Object>();
            return false;
        }
    }

    public Map<String, Object> events() { return events; }

    /** Run a script with a payload object; returns the parsed result object (empty when none). */
    public Map<String, Object> run(String scriptName, Map<String, Object> payload) {
        if (vesnaPath == null) {
            System.err.println("[vesna-mc] vesna runtime not found (set vesna.path or VESNA_HOME)");
            return new LinkedHashMap<String, Object>();
        }
        File script = new File(runtimeDir, scriptName);
        if (!script.isFile()) {
            System.err.println("[vesna-mc] script not found: " + script);
            return new LinkedHashMap<String, Object>();
        }
        String payloadJson = payload == null ? "{}" : VesnaJson.encode(payload);
        String out = exec(script.getAbsolutePath(), payloadJson);
        return parseResult(out);
    }

    /** Dispatch one event by name according to events.json; returns result or empty map. */
    public Map<String, Object> fire(String event, Map<String, Object> payload) {
        Object cfg = events.get(event);
        if (!(cfg instanceof Map)) return new LinkedHashMap<String, Object>();
        Object sc = ((Map<?, ?>) cfg).get("script");
        if (!(sc instanceof String) || ((String) sc).isEmpty()) return new LinkedHashMap<String, Object>();
        return run((String) sc, payload);
    }

    /** Run a script, then execute the actions it requested through the sink. */
    public void fireWithActions(ActionSink sink, String event, Map<String, Object> payload) {
        Map<String, Object> r = fire(event, payload);
        runActions(sink, r);
    }

    /** Execute the actions a script result requests through the platform sink. */
    public void runActions(ActionSink sink, Map<String, Object> result) {
        if (result == null) return;
        Object msg = result.get("message");
        if (msg != null) sink.message("all", String.valueOf(msg));
        Object actions = result.get("actions");
        if (!(actions instanceof List)) return;
        for (Object a : (List<?>) actions) {
            if (!(a instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) a;
            String type = String.valueOf(m.get("type"));
            String player = str(m.get("player"));
            switch (type) {
                case "message":
                    sink.message(str(m.get("target"), "all"), str(m.get("text")));
                    break;
                case "command":
                    sink.command(str(m.get("command")));
                    break;
                case "give":
                    sink.give(player, str(m.get("item")), intOf(m.get("count"), 1));
                    break;
                case "kick":
                    sink.kick(player, str(m.get("reason")));
                    break;
                case "effect":
                    sink.effect(player, str(m.get("effect")), intOf(m.get("duration"), 100), intOf(m.get("level"), 1));
                    break;
                case "tp":
                    sink.teleport(player, doubleOf(m.get("x")), doubleOf(m.get("y")), doubleOf(m.get("z")));
                    break;
                case "sound":
                    sink.sound(player, str(m.get("sound")));
                    break;
                case "log":
                    sink.log(str(m.get("text")));
                    break;
                default:
                    sink.log("[vesna-mc] unknown action: " + type);
            }
        }
    }

    /** Start periodic timers declared in events.json ("timers" list). */
    public synchronized void startTimers(ActionSink sink) {
        stopTimers();
        Object t = events.get("timers");
        if (!(t instanceof List)) return;
        timers = Executors.newScheduledThreadPool(2);
        for (Object o : (List<?>) t) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) o;
            Object sc = m.get("script");
            if (!(sc instanceof String) || ((String) sc).isEmpty()) continue;
            final String script = (String) sc;
            int secs = Math.max(1, intOf(m.get("seconds"), 10));
            timers.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    Map<String, Object> r = VesnaBridge.this.run(script, new LinkedHashMap<String, Object>());
                    runActions(sink, r);
                }
            }, secs, secs, TimeUnit.SECONDS);
        }
    }

    public synchronized void stopTimers() {
        if (timers != null) {
            timers.shutdownNow();
            timers = null;
        }
    }

    // ---------- resident process mode ----------

    /** True when a resident vesna process is running. */
    public synchronized boolean residentActive() {
        return resident != null;
    }

    /** Whether events.json enables resident mode. */
    public boolean residentEnabled() {
        return Boolean.TRUE.equals(events.get("resident"));
    }

    /** Spawn the resident process (config/vesna/scripts/resident.ves) once. */
    public synchronized void startResident(ActionSink sink) {
        if (resident != null) return;
        if (vesnaPath == null) {
            System.err.println("[vesna-mc] vesna runtime not found (set vesna.path or VESNA_HOME)");
            return;
        }
        try {
            resident = new ResidentBridge(runtimeDir, vesnaPath, sink);
        } catch (IOException e) {
            System.err.println("[vesna-mc] cannot start resident: " + e.getMessage());
        }
    }

    /** Send one event to the resident process (no-op when not running). */
    public synchronized void sendResident(String event, Map<String, Object> payload) {
        if (resident != null) resident.sendEvent(event, payload);
    }

    /** Stop the resident process. */
    public synchronized void stopResident() {
        if (resident != null) {
            resident.close();
            resident = null;
        }
    }

    // ---------- helpers ----------

    public static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    public static String str(Object v, String dflt) {
        return v == null ? dflt : String.valueOf(v);
    }

    public static int intOf(Object v, int dflt) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        }
        return dflt;
    }

    public static double doubleOf(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    /** Parse the last non-empty line of process stdout as a JSON object. */
    public static Map<String, Object> parseResult(String out) {
        if (out == null) return new LinkedHashMap<String, Object>();
        String[] lines = out.split("\r?\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String t = lines[i].trim();
            if (t.isEmpty()) continue;
            try {
                Object v = VesnaJson.parse(t);
                if (v instanceof Map) return VesnaJson.cast(v);
            } catch (Exception ignored) {}
            break;
        }
        return new LinkedHashMap<String, Object>();
    }

    /** Spawn `vesna script [payload]`, capture stdout. */
    public String exec(String scriptPath, String payloadJson) {
        if (vesnaPath == null) return "";
        List<String> cmd = new ArrayList<String>();
        cmd.add(vesnaPath);
        cmd.add(scriptPath);
        if (payloadJson != null) cmd.add(payloadJson);
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(runtimeDir);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            Thread reader = new Thread(new StreamDrain(p.getInputStream(), out));
            reader.setDaemon(true);
            reader.start();
            StringBuilder err = new StringBuilder();
            Thread errReader = new Thread(new StreamDrain(p.getErrorStream(), err));
            errReader.setDaemon(true);
            errReader.start();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                System.err.println("[vesna-mc] script timed out: " + scriptPath);
                return "";
            }
            reader.join(2000);
            errReader.join(2000);
            String e = err.toString().trim();
            if (!e.isEmpty()) System.err.println("[vesna-mc] " + e);
            return out.toString();
        } catch (IOException ex) {
            System.err.println("[vesna-mc] cannot run vesna: " + ex.getMessage());
            return "";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    private static final class StreamDrain implements Runnable {
        private final InputStream in;
        private final StringBuilder sb;
        StreamDrain(InputStream in, StringBuilder sb) { this.in = in; this.sb = sb; }
        public void run() {
            try {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) >= 0) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            } catch (IOException ignored) {}
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private static String which(String name) {
        String path = System.getenv("PATH");
        if (path == null) return null;
        String sep = File.pathSeparator;
        for (String dir : path.split(java.util.regex.Pattern.quote(sep))) {
            if (dir.isEmpty()) continue;
            File f = new File(dir, name);
            if (f.isFile()) return f.getAbsolutePath();
            if (isWindows() && !name.toLowerCase().endsWith(".exe")) {
                File g = new File(dir, name + ".exe");
                if (g.isFile()) return g.getAbsolutePath();
            }
        }
        return null;
    }
}

/**
 * Resident process bridge: a long-lived `vesna resident.ves` child process.
 *
 * Protocol (newline-delimited JSON):
 *   request : {"id": N, "event": "...", "payload": {...}}
 *   response: {"id": N, "result": {...}}
 *
 * The response thread executes the returned result through the ActionSink
 * (same action format as one-shot mode).
 */
final class ResidentBridge implements AutoCloseable {
    private final Process proc;
    private final BufferedWriter stdin;
    private final Thread reader;
    private final ActionSink sink;
    private long nextId;
    private volatile boolean closed;

    ResidentBridge(File scriptsDir, String vesnaPath, ActionSink sink) throws IOException {
        this.sink = sink;
        ProcessBuilder pb = new ProcessBuilder(vesnaPath, "resident.ves");
        pb.directory(scriptsDir);
        pb.redirectErrorStream(true);
        proc = pb.start();
        stdin = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
        reader = new Thread(new Runnable() {
            public void run() { readLoop(stdout); }
        }, "vesna-resident");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop(BufferedReader stdout) {
        try {
            String line;
            while (!closed && (line = stdout.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;
                try {
                    Object v = VesnaJson.parse(t);
                    if (v instanceof Map) {
                        Object res = ((Map<?, ?>) v).get("result");
                        if (res instanceof Map) {
                            VesnaBridge.runActions(sink, VesnaJson.cast(res));
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
    }

    synchronized void sendEvent(String event, Map<String, Object> payload) {
        if (closed) return;
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("id", ++nextId);
        req.put("event", event);
        req.put("payload", payload == null ? new LinkedHashMap<String, Object>() : payload);
        try {
            stdin.write(VesnaJson.encode(req));
            stdin.newLine();
            stdin.flush();
        } catch (IOException e) {
            System.err.println("[vesna-mc] resident write failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        closed = true;
        try { proc.destroy(); } catch (Exception ignored) {}
    }
}
