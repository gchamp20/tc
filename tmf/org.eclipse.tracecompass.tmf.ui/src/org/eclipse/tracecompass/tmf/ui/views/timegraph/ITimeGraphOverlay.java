/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views.timegraph;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * @author gbastien
 * @since 4.2
 */
@NonNullByDefault
public interface ITimeGraphOverlay {

    /**
     * @param startTime
     * @param endTime
     * @param resolution
     * @param monitor
     * @return
     */
    Collection<ILinkEvent> getLinks(long startTime, long endTime, long resolution, IProgressMonitor monitor);

    /**
     * @param entries
     * @param zoomStartTime
     * @param zoomEndTime
     * @param rgba
     * @param resolution
     * @param monitor
     * @return
     */
    Collection<IMarkerEvent> getMarkers(Collection<TimeGraphEntry> entries, long zoomStartTime, long zoomEndTime, RGBA rgba, long resolution, IProgressMonitor monitor);

    /**
     * @return
     */
    String getName();

}
