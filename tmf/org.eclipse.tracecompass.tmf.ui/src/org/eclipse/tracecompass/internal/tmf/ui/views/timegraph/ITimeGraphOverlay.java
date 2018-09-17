/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views.timegraph;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An interface to implement by components providing an overlay
 *
 * FIXME: Could the overlays actually be data providers? The concept of marker
 * is a UI thing, but technically, anything can be an overlay to something
 * else... We may not need an interface for this. Or actually, the overlay as
 * data provider is an .core concept, this can be kept as the Eclipse UI
 * implementation...
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public interface ITimeGraphOverlay {

    /**
     * Get the links provided by this overlay
     *
     * @param startTime
     *            The start time of the query
     * @param endTime
     *            The end time of the query
     * @param resolution
     *            The resolution
     * @param monitor
     *            A progress monitor
     * @return The links provided by this overlay
     */
    Collection<ILinkEvent> getLinks(long startTime, long endTime, long resolution, IProgressMonitor monitor);

    /**
     * Get the markers for this overlay
     *
     * @param entries
     *            The list of time graph entries to overlay
     * @param zoomStartTime
     *            The start time of the request
     * @param zoomEndTime
     *            The end time of the request
     * @param rgba
     *            The RGBA color associated with this overlay
     * @param resolution
     *            The resolution of the request
     * @param monitor
     *            The progress monitor
     * @return The markers
     */
    Collection<IMarkerEvent> getMarkers(Collection<TimeGraphEntry> entries, long zoomStartTime, long zoomEndTime, RGBA rgba, long resolution, IProgressMonitor monitor);

    /**
     * Get the name of this overlay
     *
     * @return The name of the overlay
     */
    String getName();

}
