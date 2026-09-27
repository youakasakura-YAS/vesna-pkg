package @@PKG@@;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Bridge between a Minecraft mod and the Vesna language runtime.
 *
 * Execution model: every event or command spawns a short-lived `vesna` process
 * (one-shot scripts). The script receives the event payload as argv[1] (a JSON
 * string) and returns its result by printing a single JSON object as the last
 * line of stdout.
 *
 * Protocol:
 *   script.ves  ->  argv[1] = JSON payload
 *   stdout      ->  last non-empty line = JSON result object
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

    private final File configDir;
    private final File runtimeDir;   // directory the vesna process is spawned from (script root)
    private final String vesnaPath;
    private Map<String, Object> events = new LinkedHashMap<String, Object>();

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
        String dirSep = File.separator;
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
