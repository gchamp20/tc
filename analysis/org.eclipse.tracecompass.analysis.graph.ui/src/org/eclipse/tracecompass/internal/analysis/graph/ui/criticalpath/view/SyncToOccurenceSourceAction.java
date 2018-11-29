package org.eclipse.tracecompass.internal.analysis.graph.ui.criticalpath.view;

import org.eclipse.jface.action.Action;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * Action to set the current time of the views to the selected
 * occurence.
 *
 * @author Guillaume Champagne
 */
public class SyncToOccurenceSourceAction extends Action {

    private long fTime;
    private final TmfView fView;

    /**
     * Constructor
     *
     * @param source
     *            the view that is generating the signal, but also shall
     *            broadcast it
     * @param timestamp
     *            The time to sync
     */
    public SyncToOccurenceSourceAction(TmfView source, Long timestamp) {
       fView = source;
       fTime = timestamp;
    }

    @Override
    public String getText() {
        return "Sync to occurence"; //$NON-NLS-1$
    }

    @Override
    public void run() {
        fView.broadcast(new TmfSelectionRangeUpdatedSignal(this, TmfTimestamp.fromNanos(fTime)));
        super.run();
    }
}
