/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views.timegraph;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.internal.tmf.ui.views.timegraph.ITimeGraphViewMetadataProvider;
import org.eclipse.tracecompass.internal.tmf.ui.views.timegraph.OverlayManager;
import org.eclipse.tracecompass.internal.tmf.ui.views.timegraph.OverlayStyleProvider;

/**
 * Manages the overlays available for the time graph
 *
 * FIXME: package-private because it doesn't need to be API, but since it
 * accesses protected methods of the time graph view, it has to be in the same
 * package
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
class OverlayManagerExtension extends OverlayManager {

    /**
     * Constructor
     *
     * @param view
     *            The view this manager is for
     * @param metadataBuilder
     *            Provider of metadata about the view
     * @param styleProvider
     *              The style provider for the overlays.
     */
    public OverlayManagerExtension(AbstractTimeGraphView view, ITimeGraphViewMetadataProvider metadataBuilder, OverlayStyleProvider styleProvider) {
        super(view, metadataBuilder, styleProvider);
    }

    @Override
    protected void refreshView(@NonNull AbstractTimeGraphView view) {
        view.refresh();
    }

}
