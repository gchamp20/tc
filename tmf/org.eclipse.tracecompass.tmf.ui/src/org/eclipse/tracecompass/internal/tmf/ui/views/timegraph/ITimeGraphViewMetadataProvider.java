/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/


package org.eclipse.tracecompass.internal.tmf.ui.views.timegraph;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface to query information about the view's metadata
 *
 * @author Guillaume Champagne
 */
public interface ITimeGraphViewMetadataProvider {

    /**
     * Get a set of all metadata key about the entries of the view.
     *
     * @return Set of metadata key
     */
    @NonNull Set<@NonNull String> getEntriesMetadata();

}
