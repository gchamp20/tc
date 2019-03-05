package org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph;

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;

/**
 * Class that takes a presentation provider and wraps it to
 * implement the ITimeGraphStyleProvider interface
 *
 * FIXME: When/if the Presentation providers implements this API, this class won't be needed.
 *
 * @author Guillaume Champagne
 */
public class TimeGraphPresentationProviderWrapper implements ITimeGraphStyleProvider {

    private ITimeGraphPresentationProvider fProvider;

    /**
     * @param provider
     *          The presentation provider
     */
    public TimeGraphPresentationProviderWrapper(ITimeGraphPresentationProvider provider) {
        fProvider = provider;
    }

    /**
     * @return
     *      The original provider
     */
    public ITimeGraphPresentationProvider getPresentationProvider() {
        return fProvider;
    }

    @Override
    public StateItem[] getStateTable() {
        return fProvider.getStateTable();
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        return fProvider.getStateTableIndex(event);
    }

    @Override
    public String getStateTypeName() {
        return fProvider.getStateTypeName();
    }

}
