/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.timegraph.overlay;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.ITimeGraphOverlay;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.ITimeGraphOverlayProvider;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 *
 *
 * @author Geneviève Bastien
 */
public class SegmentStoreOverlayProvider implements ITimeGraphOverlayProvider {

    @Override
    public Multimap<String, ITimeGraphOverlay> getOverlays(@NonNull ITmfTrace trace) {
        Iterable<ISegmentStoreProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, ISegmentStoreProvider.class);
        Multimap<String, ITimeGraphOverlay> overlays = HashMultimap.create();
        for (ISegmentStoreProvider ssProvider : modules) {
            overlays.put(String.valueOf(((IAnalysisModule) ssProvider).getName()), new SegmentStoreOverlay(ssProvider));
        }
        return overlays;
    }

}
