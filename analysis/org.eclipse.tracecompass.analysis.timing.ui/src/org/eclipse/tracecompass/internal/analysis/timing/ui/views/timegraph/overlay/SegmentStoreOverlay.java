/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.timegraph.overlay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.model.IFilterableDataModel;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.ITimeGraphOverlay;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Geneviève Bastien
 */
public class SegmentStoreOverlay implements ITimeGraphOverlay {

    private final ISegmentStoreProvider fSsProvider;
    private final String fName;

    /**
     * @param ssProvider
     */
    public SegmentStoreOverlay(ISegmentStoreProvider ssProvider) {
        fSsProvider = ssProvider;
        if (ssProvider instanceof IAnalysisModule) {
            fName = String.valueOf(((IAnalysisModule) ssProvider).getName());
        } else {
            fName = "Segment Store provider";
        }
    }

    @Override
    public @NonNull Collection<ILinkEvent> getLinks(long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    private static class ClusterHolder
    {
        public List<ISegment> list;
        public Long center;

        public ClusterHolder() {
            list = new ArrayList<>();
            center = 0L;
        }
    }

    @Override
    public @NonNull Collection<IMarkerEvent> getMarkers(Collection<TimeGraphEntry> entries, long startTime, long endTime, RGBA color, long resolution, @NonNull IProgressMonitor monitor) {
        ISegmentStore<ISegment> segmentStore = fSsProvider.getSegmentStore();
        if (segmentStore == null) {
            // Schedule the analysis, since it was requested, so next time it
            // will return something
            if (fSsProvider instanceof IAnalysisModule) {
                ((IAnalysisModule) fSsProvider).schedule();
            }
            return Collections.emptyList();
        }
        Iterable<ISegmentAspect> segmentAspects = fSsProvider.getSegmentAspects();
        Set<ISegmentAspect> markedAspects = new HashSet<>();

        if (monitor.isCanceled()) {
            return Collections.emptyList();
        }
        // Filter the entries to get only those that match on some aspects
        Map<TimeGraphEntry, Multimap<String, String>> markableEntries = new HashMap<>();
        for (TimeGraphEntry entry : entries) {
            Multimap<String, String> metadata = entry.getMetadata();
            if (metadata.isEmpty()) {
                continue;
            }
            for (ISegmentAspect aspect : segmentAspects) {
                if (metadata.containsKey(aspect.getName())) {
                    // Add the entry to markable entries
                    markableEntries.put(entry, metadata);
                    markedAspects.add(aspect);
                }
            }
        }
        if (monitor.isCanceled() || markedAspects.isEmpty() || markableEntries.isEmpty()) {
            return Collections.emptyList();
        }

        long startInter = Long.MAX_VALUE;
        long endInter = Long.MIN_VALUE;
        for (TimeGraphEntry e : entries) {
            startInter = Math.min(startInter, e.getStartTime());
            endInter = Math.max(endInter, e.getEndTime());
        }

        // Now build the markers
        List<IMarkerEvent> markers = new ArrayList<>();
        Map<TimeGraphEntry, List<ClusterHolder>> clusters = new HashMap<>();
        Map<ISegment, ClusterHolder> crossrefMap = new HashMap<>();

        int NUMBER_CLUSTER = 35;

        long interval = Math.floorDiv((endTime - startTime), NUMBER_CLUSTER);
        for (TimeGraphEntry t : markableEntries.keySet()) {
            List<ClusterHolder> l = new ArrayList<>();
            for (int i = 0; i < NUMBER_CLUSTER; i++) {
                Long center = startTime + i * interval;
                ClusterHolder cl = new ClusterHolder();
                cl.center = center;
                l.add(cl);
            }
            clusters.put(t, l);
        }

        boolean movedSomething = true;
        boolean firstIter = true;
        synchronized(segmentStore) {
        while (movedSomething) {
            if (!firstIter) {
                movedSomething = false;
            } else {
                firstIter = false;
            }
            for (ISegment segment : segmentStore.getIntersectingElements(startTime, endTime)) {
                Multimap<String, String> resolvedAspects = HashMultimap.create();
                for (ISegmentAspect aspect : markedAspects) {
                    resolvedAspects.put(aspect.getName(), String.valueOf(aspect.resolve(segment)));
                }
                for (Entry<TimeGraphEntry, Multimap<String, String>> entry : markableEntries.entrySet()) {
                    if (IFilterableDataModel.compareMetadata(resolvedAspects, entry.getValue())) {
                        List<ClusterHolder> l = clusters.get(entry.getKey());
                        if (l != null) {
                            ClusterHolder pos = null;
                            Long diff_min = Long.MAX_VALUE;
                            for (ClusterHolder cl : l) {
                                Long center = cl.center;
                                Long diff = Math.abs(center - segment.getStart());
                                if (diff < diff_min) {
                                    pos = cl;
                                    diff_min = diff;
                                }
                            }

                            if (pos != null) {
                                if (crossrefMap.containsKey(segment)) {
                                    ClusterHolder old_pos = crossrefMap.get(segment);
                                    if (old_pos != null && old_pos != pos) {
                                        old_pos.list.remove(segment);
                                        crossrefMap.put(segment, pos);
                                        movedSomething = true;
                                        pos.list.add(segment);
                                    }
                                } else {
                                    crossrefMap.put(segment, pos);
                                    pos.list.add(segment);
                                }
                            }
                        }
                    }
                }
            }

            for (Entry<TimeGraphEntry, List<ClusterHolder>> entry : clusters.entrySet()) {
                for (ClusterHolder cl : entry.getValue()) {
                    Long meanCluster = 0L;
                    if (cl.list.size() > 0) {
                        meanCluster = 0L;
                        for (ISegment s : cl.list) {
                            meanCluster += s.getStart();
                        }
                        meanCluster = Math.floorDiv(meanCluster, cl.list.size());
                        cl.center = meanCluster;
                    }
                }
            }
        }

        for (Entry<TimeGraphEntry, List<ClusterHolder>> entry : clusters.entrySet()) {
            for (ClusterHolder cl : entry.getValue()) {
                List<ISegment> l = cl.list;
                Long center = cl.center;
                if (l.size() == 1) {
                    ISegment segment = l.get(0);
                    markers.add(new MarkerEvent(entry.getKey(), segment.getStart(), segment.getLength(), getName(), color, (segment instanceof INamedSegment) ? ((INamedSegment) segment).getName() : "", true));
                }
                else if (l.size() > 1) {
                    markers.add(new MarkerEvent(entry.getKey(), center, 1, getName(), color, "", true)); //$NON-NLS-1$
                }
            }
        }
        }

        /*for (ISegment segment : segmentStore.getIntersectingElements(startTime, endTime)) {
            Multimap<String, String> resolvedAspects = HashMultimap.create();
            for (ISegmentAspect aspect : markedAspects) {
                resolvedAspects.put(aspect.getName(), String.valueOf(aspect.resolve(segment)));
            }
            for (Entry<TimeGraphEntry, Multimap<String, String>> entry : markableEntries.entrySet()) {
                if (IFilterableDataModel.compareMetadata(resolvedAspects, entry.getValue())) {
                    // Create a marker
                    markers.add(new MarkerEvent(entry.getKey(), segment.getStart(), segment.getLength(), getName(), color, (segment instanceof INamedSegment) ? ((INamedSegment) segment).getName() : "", true));
                }
            }
        } */
        return markers;
    }

    @Override
    public String getName() {
        return fName;
    }

}
