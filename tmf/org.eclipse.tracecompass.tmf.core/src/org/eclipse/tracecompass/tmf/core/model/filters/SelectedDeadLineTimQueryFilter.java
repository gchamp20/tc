package org.eclipse.tracecompass.tmf.core.model.filters;

public class SelectedDeadLineTimQueryFilter extends TimeQueryFilter {

    private long fDeadlineInNs;

    public SelectedDeadLineTimQueryFilter(long start, long end, int n, long deadline) {
        super(start, end, n);
        fDeadlineInNs = deadline;
    }

    public long getDeadlineInNs() {
        return fDeadlineInNs;
    }
}
