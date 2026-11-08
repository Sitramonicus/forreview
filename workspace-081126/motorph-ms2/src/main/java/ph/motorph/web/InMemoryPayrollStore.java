package ph.motorph.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Process-memory payroll runs; used only by the isolated loopback test fixture. */
public final class InMemoryPayrollStore implements PayrollStore {
    private final Map<String, PayrollRun> runs = new LinkedHashMap<>();

    @Override
    public synchronized List<PayrollRun> findAll() {
        List<PayrollRun> result = new ArrayList<>(runs.values());
        result.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
        return result;
    }

    @Override
    public synchronized Optional<PayrollRun> findById(String id) {
        return Optional.ofNullable(id == null ? null : runs.get(id.trim()));
    }

    @Override
    public synchronized PayrollRun create(PayrollRun run) {
        if (runs.containsKey(run.id())) throw new IllegalStateException("Payroll run ID already exists.");
        runs.put(run.id(), run);
        return run;
    }
}
