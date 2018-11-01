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
import org.eclipse.tracecompass.datastore.core.interval.IHTInterval;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.model.IFilterableDataModel;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.util.IntervalTree;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.ITimeGraphOverlay;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ClusterMarkerEvent;
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
                }
                markedAspects.add(aspect);
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

        /* Now build the markers per entry
         * O(|segmentStore| * (|markedAspects| + |markableEntries|))
         */
        Map<TimeGraphEntry, List<IMarkerEvent>> markerMap = new HashMap<>();

        for (ISegment segment : segmentStore.getIntersectingElements(startTime, endTime)) {
            Multimap<String, String> resolvedAspects = HashMultimap.create();
            for (ISegmentAspect aspect : markedAspects) {
                resolvedAspects.put(aspect.getName(), String.valueOf(aspect.resolve(segment)));
            }
            for (Entry<TimeGraphEntry, Multimap<String, String>> entry : markableEntries.entrySet()) {
                if (IFilterableDataModel.commonIntersect(resolvedAspects, entry.getValue())) {
                    // Create a marker
                    MarkerEvent m = new MarkerEvent(entry.getKey(), segment.getStart(), segment.getLength(), getName(), color, (segment instanceof INamedSegment) ? ((INamedSegment) segment).getName() : "", true);

                    /* Add metadata to marker from the resolved aspects */
                    for (Entry<String, String> e: resolvedAspects.entries()) {
                        m.putMetadata(e.getKey(), e.getValue());
                    }

                    List<IMarkerEvent> markers = markerMap.getOrDefault(entry.getKey(), new ArrayList<>());
                    markers.add(m);
                    if (!markerMap.containsKey(entry.getKey())) {
                        markerMap.put(entry.getKey(), markers);
                    }
                }
            }
        }

        /*
         * Now cluster the markers.
         *
         * Total Cost: O(E * m_e * I)
         *      where m_e is number of markers on entry e and I number of intervals.
         *
         * The algorithm is as follow:
         *
         * For each entry - 0(E)
         *   For each marker m in markers: - 0(m)
         *      If center of m falls in one or more interval - 0(log|I|)
         *        Add m to closest interval - 0(1) (worst O(|I|))
         *      Else:
         *        Create interval of length L centered at center of m - 0(log|I|)
         *
         * For each interval - 0(I)
         *   If interval contains more than 1 marker:
         *     Create cluster marker
         *   Else
         *     Create regular marker
         */

        long L = Math.floorDiv(endTime - startTime, 25);

        List<IMarkerEvent>  outputMarkers = new ArrayList<>();
        IntervalTree<MarkerInterval> root;
        for (Entry<TimeGraphEntry, List<IMarkerEvent>> entry : markerMap.entrySet()) {
            List<IMarkerEvent> markers = entry.getValue();
            IMarkerEvent m = markers.get(0);
            long center = Math.floorDiv(m.getTime() + (m.getTime() + m.getDuration()), 2);
            root = new IntervalTree<>(new MarkerInterval(center - L, center + L, m));

            for (int i = 1; i < markers.size(); i++) {

                m = markers.get(i);
                center = Math.floorDiv(m.getTime() + (m.getTime() + m.getDuration()), 2);

                List<MarkerInterval> intervals = root.getIntersections(center);
                if (intervals.size() > 0) {
                    long minDist = Long.MAX_VALUE;
                    MarkerInterval position = null;
                    for (MarkerInterval interval : intervals) {
                        long dist = Math.abs(center - interval.getCenter());
                        if (dist < minDist) {
                            minDist = dist;
                            position = interval;
                        }
                    }

                    if (position != null) {
                        position.addMarker(m);
                    }
                }
                else {
                    root.insert(new MarkerInterval(center - L, center + L, m));
                }
            }

            List<MarkerInterval> intervals = root.getNodeValues();
            for (MarkerInterval interval : intervals) {
                List<IMarkerEvent> intervalMarkers = interval.getMarkers();
                if (intervalMarkers.size() == 1) {
                    outputMarkers.add(intervalMarkers.get(0));
                }
                else {
                    outputMarkers.add(new ClusterMarkerEvent(entry.getKey(), interval.getMarkerCenter(), 0, getName(), color, "", true));
                }
            }
        }

        return outputMarkers;
    }

    @Override
    public String getName() {
        return fName;
    }

    class MarkerInterval implements IHTInterval {

        private List<IMarkerEvent> fMarkers;
        private long fStart;
        private long fEnd;

        private long fRollingAverage;

        public MarkerInterval(long start, long end, IMarkerEvent m) {
            fStart = start;
            fEnd = end;
            fRollingAverage = Math.floorDiv(m.getTime() + m.getTime() + m.getDuration(), 2);
            fMarkers = new ArrayList<>();
            fMarkers.add(m);
        }

        public long getCenter() {
            return Math.floorDiv(fStart, fEnd);
        }

        @Override
        public long getStart() {
            return fStart;
        }

        @Override
        public long getEnd() {
            return fEnd;
        }

        public void addMarker(IMarkerEvent marker) {
            fMarkers.add(marker);
            long markerCenter = Math.floorDiv(marker.getTime() + marker.getTime() + marker.getDuration(), 2);
            fRollingAverage = fRollingAverage + Math.floorDiv(markerCenter - fRollingAverage, fMarkers.size());
        }

        public List<IMarkerEvent> getMarkers() {
            return fMarkers;
        }

        public long getMarkerCenter() {
            return fRollingAverage;
        }

        @Override
        /**
         * @return
         *      This class is not meant to be serialized, so we return 0.
         */
        public int getSizeOnDisk() {
            return 0;
        }

        @Override
        /**
         * This class is not meant to be serialized, so nothing is done.
         */
        public void writeSegment(ISafeByteBufferWriter buffer) {
        }
    }
}


