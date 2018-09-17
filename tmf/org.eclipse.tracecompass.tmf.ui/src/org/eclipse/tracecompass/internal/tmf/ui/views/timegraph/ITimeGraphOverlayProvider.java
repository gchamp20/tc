/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views.timegraph;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.Multimap;

/**
 * An interface to be implemented by components that provide a group of overlays
 * for a trace, for example, some type of analysis may provide overlays
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public interface ITimeGraphOverlayProvider {

    /**
     * Get the overlays provided by this provider
     *
     * @param trace
     *            The trace for which to get the overlays
     * @return A map of overlay names to overlay
     */
    Multimap<String, ITimeGraphOverlay> getOverlays(ITmfTrace trace);

}
