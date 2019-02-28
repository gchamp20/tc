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

/**
 * Interface to classes that accumulate metadata about the view to feed to it a provider.
 *
 * @author Guillaume Champagne
 */
public interface ITimeGraphViewMetadataBuilder {

    /**
     * @param metadata Set of metadata keys about the entries
     */
    public void addEntriesMetadata(Set<String> metadata);
}
