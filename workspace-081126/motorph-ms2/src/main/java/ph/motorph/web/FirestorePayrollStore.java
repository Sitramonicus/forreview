package ph.motorph.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.ArrayList;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Saved payroll runs under users/{uid}/demoPayrollRuns/{runId}. Each run stores its header,
 * the ruleset version, the calculation assumptions, and every resulting payslip line, so a
 * stored run can be re-read without recomputing or re-deriving policy.
 */
public final class FirestorePayrollStore implements PayrollStore {
    private static final String COLLECTION = "demoPayrollRuns";
    private final FirestoreRestClient client;

    public FirestorePayrollStore(FirestoreRestClient client) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        this.client = client;
    }

    @Override
    public List<PayrollRun> findAll() {
        List<PayrollRun> runs = new ArrayList<>();
        for (JsonObject document : client.listDocuments(client.userCollection(COLLECTION))) {
            runs.add(fromDocument(document));
        }
        runs.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
        return runs;
    }

    @Override
    public Optional<PayrollRun> findById(String id) {
        if (id == null) return Optional.empty();
        return findAll().stream().filter(run -> run.id().equals(id)).findFirst();
    }

    @Override
    public PayrollRun create(PayrollRun run) {
        if (!client.createDocument(client.userCollection(COLLECTION), run.id(), runFields(run))) {
            throw new IllegalStateException("Payroll run ID already exists; refresh and try again.");
        }
        return run;
    }

    private static JsonObject runFields(PayrollRun run) {
        JsonObject fields = new JsonObject();
        fields.add("payPeriod", string(run.payPeriod()));
        fields.add("periodStart", string(run.periodStart()));
        fields.add("periodEnd", string(run.periodEnd()));
        fields.add("rulesetId", string(run.rulesetId()));
        fields.add("taxBasis", string(run.taxBasis()));
        fields.add("statutoryBasis", string(run.statutoryBasis()));
        fields.add("createdAt", string(run.createdAt()));
        JsonArray payslips = new JsonArray();
        for (Payslip slip : run.payslips()) payslips.add(PayslipJson.toJson(slip));
        fields.add("payslips", array(payslips));
        return fields;
    }

    private static PayrollRun fromDocument(JsonObject document) {
        JsonObject fields = document.has("fields") && document.get("fields").isJsonObject()
            ? document.getAsJsonObject("fields") : new JsonObject();
        String id = documentId(document);
        List<Payslip> payslips = new ArrayList<>();
        JsonElement stored = fields.get("payslips");
        if (stored != null && stored.isJsonObject()) {
            JsonElement values = stored.getAsJsonObject().get("arrayValue");
            if (values != null && values.isJsonObject()) {
                JsonElement list = values.getAsJsonObject().get("values");
                if (list != null && list.isJsonArray()) {
                    for (JsonElement element : list.getAsJsonArray()) {
                        payslips.add(PayslipJson.fromJson(element.getAsJsonObject()));
                    }
                }
            }
        }
        if (payslips.isEmpty()) {
            throw new IllegalStateException("Stored payroll run " + id + " has no payslips.");
        }
        return new PayrollRun(id, value(fields, "payPeriod"), value(fields, "periodStart"),
            value(fields, "periodEnd"), value(fields, "rulesetId"), value(fields, "taxBasis"),
            value(fields, "statutoryBasis"), value(fields, "createdAt"), payslips);
    }

    private static JsonObject array(JsonArray values) {
        JsonObject wrapper = new JsonObject();
        JsonObject arrayValue = new JsonObject();
        arrayValue.add("values", values);
        wrapper.add("arrayValue", arrayValue);
        return wrapper;
    }

    private static JsonObject string(String value) {
        JsonObject field = new JsonObject();
        field.addProperty("stringValue", value == null ? "" : value);
        return field;
    }

    private static String value(JsonObject fields, String name) {
        JsonElement element = fields.get(name);
        if (element == null || !element.isJsonObject()) return "";
        JsonElement inner = element.getAsJsonObject().get("stringValue");
        return inner == null || inner.isJsonNull() ? "" : inner.getAsString();
    }

    private static String documentId(JsonObject document) {
        String name = document.has("name") ? document.get("name").getAsString() : "";
        int slash = name.lastIndexOf('/');
        if (slash < 0 || slash == name.length() - 1) throw new IllegalStateException("Payroll run document has no ID.");
        return name.substring(slash + 1);
    }

}
