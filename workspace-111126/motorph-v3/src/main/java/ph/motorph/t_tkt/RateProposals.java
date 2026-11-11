package ph.motorph.t_tkt;

import java.util.ArrayList;
import java.util.List;

public final class RateProposals {
    public static final class Proposal {
        public final String id, effectiveMonth, note, proposedBy, proposedAt, rates;
        public String status = "Pending review", decidedBy = "", decidedAt = "", versionId = "", outcome = "";

        Proposal(String id, String effectiveMonth, String note, String proposedBy, String proposedAt, String rates) {
            this.id = id;
            this.effectiveMonth = effectiveMonth;
            this.note = note;
            this.proposedBy = proposedBy;
            this.proposedAt = proposedAt;
            this.rates = rates;
        }
    }

    private final List<Proposal> proposals = new ArrayList<>();
    private int next = 1;

    public synchronized Proposal add(String effectiveMonth, String note, String proposedBy, String proposedAt, String rates) {
        for (Proposal item : proposals) {
            if (item.effectiveMonth.equals(effectiveMonth) && "Pending review".equals(item.status)) {
                throw new IllegalStateException("A change for " + effectiveMonth + " is already waiting for approval (" + item.id + ").");
            }
        }
        Proposal proposal = new Proposal(String.format("RP-%04d", next++), effectiveMonth, note, proposedBy, proposedAt, rates);
        proposals.add(proposal);
        return proposal;
    }

    public synchronized Proposal find(String id) {
        for (Proposal item : proposals) if (item.id.equals(id)) return item;
        throw new java.util.NoSuchElementException("Rate change request not found.");
    }

    public synchronized void decide(Proposal proposal, String status, String by, String at, String versionId, String outcome) {
        if (!"Pending review".equals(proposal.status)) throw new IllegalStateException("This request was already " + proposal.status.toLowerCase(java.util.Locale.ROOT) + ".");
        proposal.status = status;
        proposal.decidedBy = by;
        proposal.decidedAt = at;
        proposal.versionId = versionId;
        proposal.outcome = outcome;
    }

    
    public synchronized List<Proposal> list() {
        List<Proposal> copy = new ArrayList<>(proposals);
        java.util.Collections.reverse(copy);
        return copy;
    }
}
