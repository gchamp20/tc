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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.realtime.MANEPI.EventKey;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * Finds the pattern per thread on a given trace
 *
 * @author Guillaume Champagne
 */
public class RealTimePatternProvider {

    private ITmfTrace fTrace;

    private Set<Integer> fSelectedThreadsID;

    private Map<Integer, List<ITmfEvent>> fThreadsEventList;

    private @Nullable Map<@NonNull Integer, @NonNull List<@NonNull Pair<@NonNull List<@NonNull EventKey>, @NonNull List<@NonNull Pair<@NonNull Long, @NonNull Long>>>>> fPatternsMap;

    /**
     * @param trace
     *      The trace object
     */
    public RealTimePatternProvider(ITmfTrace trace) {
        fTrace = trace;

        fSelectedThreadsID = new HashSet<>();
        fSelectedThreadsID.add(1234);

        fThreadsEventList = new HashMap<>();
    }

    /**
     * @param patternsMap
     *          The patterns in which the result are stored.
     */
    public void assignTargetMap(@Nullable Map<@NonNull Integer, @NonNull List<@NonNull Pair<@NonNull List<@NonNull EventKey>, @NonNull List<@NonNull Pair<@NonNull Long, @NonNull Long>>>>> patternsMap) {
        fPatternsMap = patternsMap;
    }

    /**
     * Process each event in the sequence.
     * @param event
     *          Event to process
     */
    public void processEvent(ITmfEvent event) {
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

    /**
     * Call when all the events have been processed.
     */
    public void done() {
        // Mine the patterns
        for (Integer tid : fSelectedThreadsID) {
            List<Pair<List<EventKey>, List<Pair<Long, Long>>>> pattern = MANEPI.compute(fThreadsEventList.getOrDefault(tid, new ArrayList<>()));
            if (fPatternsMap != null) {
                fPatternsMap.put(tid, pattern);
            }
        }
    }

    ITmfTrace getTrace() {
        return fTrace;
    }
}
