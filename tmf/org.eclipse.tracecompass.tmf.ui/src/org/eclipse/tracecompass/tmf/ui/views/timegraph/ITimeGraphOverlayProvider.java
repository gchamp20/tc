/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views.timegraph;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.Multimap;

/**
 * @author Geneviève Bastien
 * @since 4.2
 */
@NonNullByDefault
public interface ITimeGraphOverlayProvider {

    /**
     * @param trace
     * @return
     */
    Multimap<String, ITimeGraphOverlay> getOverlays(ITmfTrace trace);

}
