/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views.timegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.datastore.core.interval.IHTInterval;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

import com.google.common.collect.Multimap;

/**
 * Helper methods for overlays.
 *
 * @author Guillaume Champagne
 */
public class TimeGraphOverlayUtils {

    /**
     * Cluster the markers per entry.
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
     *
     * @param markerMap
     *          Multimap of ITimeGraphEntry to IMarkerEvent. Each ITimeGraphEntry may have multiple related marker
     * @param start
     *          Start time of the interval
     * @param end
     *          End time of the interval
     * @param resolution
     *          View resolution
     * @param markerName
     *          Name for the new cluster markers.
     * @param color
     *          Color for the new cluster markers
     * @return
     *        New list of markers
     */
    public static List<IMarkerEvent> clusterMarkers(Multimap<ITimeGraphEntry, IMarkerEvent> markerMap, Long start, Long end, Long resolution, String markerName, RGBA color) {
        long L = Math.floorDiv(end - start, 25);

        List<IMarkerEvent> outputMarkers = new ArrayList<>();
        IntervalTree<@NonNull MarkerInterval> root;
        for (ITimeGraphEntry entry : markerMap.keySet()) {
            Collection<IMarkerEvent> markers = markerMap.get(entry);
            Iterator<IMarkerEvent> it = markers.iterator();

            if (!it.hasNext()) {
                continue;
            }

            IMarkerEvent m = it.next();
            long center = Math.floorDiv(m.getTime() + (m.getTime() + m.getDuration()), 2);
            root = new IntervalTree<>(new MarkerInterval(center - L, center + L, m));

            while (it.hasNext()) {
                m = it.next();
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
                    outputMarkers.add(new ClusterMarkerEvent(entry, interval.getMarkerCenter(), 0, markerName, color, StringUtils.EMPTY, true));
                }
            }
        }

        return outputMarkers;
    }

    /**
     * A group of markers related to the same entry.
     */
    private static class MarkerInterval implements IHTInterval {

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
        public void writeSegment(@NonNull ISafeByteBufferWriter buffer) {
        }
    }
}
