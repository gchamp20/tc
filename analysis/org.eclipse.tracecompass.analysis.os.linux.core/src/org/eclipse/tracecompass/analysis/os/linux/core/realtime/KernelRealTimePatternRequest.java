/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.realtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.realtime.MANEPI.EventKey;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * Request to mine the pattern on a thread.
 *
 * @author Guillaume Champagne
 */
public class KernelRealTimePatternRequest extends TmfEventRequest {

    private Set<Integer> fSelectedThreadsID;

    private Map<Integer, List<ITmfEvent>> fThreadsEventList;

    private Map<Integer, List<Pair<List<EventKey>, List<Pair<Long, Long>>>>> fResults;

    /**
     * Default constructor.
     */
    public KernelRealTimePatternRequest() {
        super(TmfEvent.class,
                TmfTimeRange.ETERNITY,
                0,
                ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.BACKGROUND);

        fSelectedThreadsID = new HashSet<>();
        fSelectedThreadsID.add(1234);

        fThreadsEventList = new HashMap<>();
        fResults = new HashMap<>();
    }

    @Override
    public void handleData(final ITmfEvent event) {
        super.handleData(event);

        // Resolve the KernelTidAspect to get the current TID for this event.
        Object tidObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), KernelTidAspect.class, event);
        Integer tid = (tidObj != null && tidObj instanceof Integer) ? (Integer)tidObj : null;
        if (tid == null) {
            return;
        }

        // If we are not mining for this tid, exit.
        if (!fSelectedThreadsID.contains(tid)) {
            return;
        }

        // Add the event to the event list.
        List<ITmfEvent> evList = fThreadsEventList.getOrDefault(tid, new ArrayList<>());
        evList.add(event);
    }

    @Override
    public synchronized void done() {

        // Mine the patterns
        for (Integer tid : fSelectedThreadsID) {
            List<Pair<List<EventKey>, List<Pair<Long, Long>>>> pattern = MANEPI.compute(fThreadsEventList.getOrDefault(tid, new ArrayList<>()));
            fResults.put(tid, pattern);
        }

        super.done();
    }

    @Override
    public void handleCancel() {
        super.handleCancel();
    }

    /**
     * Get the result after the request completed.
     * Call after waitForCompletion()
     */
    public Map<Integer, List<Pair<List<EventKey>, List<Pair<Long, Long>>>>> getResult() {
        return fResults;
    }
}
