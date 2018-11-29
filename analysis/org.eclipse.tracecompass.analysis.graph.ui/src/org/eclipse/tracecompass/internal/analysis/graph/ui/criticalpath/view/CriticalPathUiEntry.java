package org.eclipse.tracecompass.internal.analysis.graph.ui.criticalpath.view;

import org.eclipse.tracecompass.internal.analysis.graph.core.dataprovider.CriticalPathEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

public class CriticalPathUiEntry extends TimeGraphEntry {

    private Long fOriginalTime;

    /**
     * Constructor, build a {@link ControlFlowEntry} from it's model
     *
     * @param model
     *            the {@link ThreadEntryModel} to compose this entry
     */
    public CriticalPathUiEntry(CriticalPathEntry model) {
        super(model);
        fOriginalTime = model.getOriginalStart();
    }

    /**
     * @return
     *      zeTime
     */
    public Long getOriginalTime() {
        return fOriginalTime;
    }
}
