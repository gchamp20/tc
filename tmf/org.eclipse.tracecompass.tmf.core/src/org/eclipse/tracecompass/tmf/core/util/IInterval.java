/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.util;

/**
 * Interval interface to query start point and end point of the
 * interval in a single dimension.
 *
 * @author Guillaume Champagne
 * @since 4.2
 */
public interface IInterval {

    /**
     * @return
     *      The starting point of this interval.
     */
    public long getStart();

    /**
     * @return
     *      The end point of this interval.
     */
    public long getEnd();

    /**
     * @return
     *      The center point of this interval rounded down.
     */
    default long getCenter() {
        return Math.floorDiv(getStart() + getEnd(), 2);
    }
}
