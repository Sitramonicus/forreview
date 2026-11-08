package ph.motorph.web;

import java.util.List;
import java.util.Optional;

/** Storage boundary for saved payroll runs. Runs are immutable once created. */
public interface PayrollStore {
    List<PayrollRun> findAll();
    Optional<PayrollRun> findById(String id);
    PayrollRun create(PayrollRun run);
}
