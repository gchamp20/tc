package org.eclipse.tracecompass.analysis.os.linux.core.realtime;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Interface for states
 *
 * @author Guillaume Champagne
 * @since 3.1
 */
public interface IState {
    /**
     * @param event
     *          The events
     * @return
     *          True if we can move towards this state.
     */
    public boolean checkIncomingCondition(ITmfEvent event);
}
