/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.realtime;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;

/**
 * Request to mine the pattern on a thread.
 *
 * @author Guillaume Champagne
 */
public class KernelRealTimePatternRequest extends TmfEventRequest {

    private RealTimePatternProvider fProvider;
    /**
     * Default constructor.
     * @param provider
     *          The Pattern provider used to extract patterns
     */
    public KernelRealTimePatternRequest(RealTimePatternProvider provider) {
        super(TmfEvent.class,
                TmfTimeRange.ETERNITY,
                0,
                ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.BACKGROUND);

        fProvider = provider;
    }

    @Override
    public void handleData(final ITmfEvent event) {
        super.handleData(event);
        fProvider.processEvent(event);
    }

    @Override
    public synchronized void done() {
        super.done();
        fProvider.done();
    }

    @Override
    public void handleCancel() {
        super.handleCancel();
    }
}
