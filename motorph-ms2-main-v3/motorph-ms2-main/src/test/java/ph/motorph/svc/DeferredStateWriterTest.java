package ph.motorph.svc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import ph.motorph.l_log.AuditLog;
import ph.motorph.o_oop.LocalStateFile;
import ph.motorph.o_oop.StateStore;

/** Regression checks for changes arriving during slow writes and orderly shutdown. */
public final class DeferredStateWriterTest {
    private static final class SlowStore implements StateStore {
        final Gson gson = new LocalStateFile(Paths.get("unused.json")).gson();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger writes = new AtomicInteger();
        volatile String last = "";
        public Gson gson() { return gson; }
        public String describe() { return "controlled test store"; }
        public Optional<JsonObject> load() { return Optional.empty(); }
        public void save(JsonObject stores) throws IOException {
            int number = writes.incrementAndGet();
            if (number == 1) {
                entered.countDown();
                try {
                    if (!release.await(15, TimeUnit.SECONDS)) throw new IOException("timed out waiting for test");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
            }
            last = stores.toString();
        }
    }
    private static Field field(String name) throws Exception {
        Field f = AppServer.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static Method method(String name) throws Exception {
        Method m = AppServer.class.getDeclaredMethod(name); m.setAccessible(true); return m;
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        try { run(); System.out.println("DeferredStateWriterTest: 4 checks passed"); }
        catch (Throwable e) { System.out.println("FAIL: " + e); e.printStackTrace(); System.exit(1); }
    }
    private static void run() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"), null, null);
        SlowStore store = new SlowStore();
        field("dataFile").set(app, store);
        field("deferSaves").setBoolean(app, true);
        Object lock = field("stateLock").get(app);
        AuditLog audit = (AuditLog) field("auditLog").get(app);
        Method mark = method("saveDataFile");
        method("startBackgroundWriter").invoke(app);
        try {
            synchronized (lock) { audit.record("one", "test", "POST", "/first", "", 200, null); mark.invoke(app); }
            check(store.entered.await(10, TimeUnit.SECONDS), "first save did not start");
            synchronized (lock) { audit.record("two", "test", "POST", "/second", "", 200, null); mark.invoke(app); }
            check(store.writes.get() == 1, "second save unexpectedly ran concurrently");
            store.release.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (!store.last.contains("/second") && System.nanoTime() < deadline) Thread.sleep(25);
            check(store.last.contains("/second"), "change during in-flight save was not persisted");
            synchronized (lock) { audit.record("three", "test", "POST", "/shutdown", "", 200, null); mark.invoke(app); }
            app.stop();
            check(store.last.contains("/shutdown"), "shutdown did not flush latest state");
        } finally { store.release.countDown(); app.stop(); }
    }
}
