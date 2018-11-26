package org.eclipse.tracecompass.analysis.graph.core.building;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * @author Guillaume Champagne
 *
 */
public class EventKey {
    private String evName;

    public EventKey(ITmfEvent e) {
        evName = e.getName();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EventKey)) {
            return false;
        }

        EventKey e = (EventKey)o;
        return  e.evName.equals(evName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(evName);
    }
}
