/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;

/**
 * Functions to implement the algorithm from:
 *
 * Zhu, H., Wang, P., He, X., Li, Y., Wang, W., & Shi, B. (2010, December).
 * Efficient episode mining with minimal and non-overlapping occurrences.
 * In Data mining (ICDM), 2010 IEEE 10th international conference on (pp. 1211-1216). IEEE.
 *
 * @author Guillaume Champagne
 */
public class MANEPI {

    private static class FEPT {
        /**
         * Label of this node.
         */
        private String fLabel;

        /**
         * Set of minimal occurences
         */
        private Set<Object> fMinimalOccurences;

        /**
         * Set of minimal and non overlaping occurences
         */
        private Set<Object> fMinimalNonOverlappingOccurences;

        /**
         * Childrens of this node.
         */
        private List<Object> fChildrens;

        /**
         *  The sequence on which pattern are discovered
         */
        private List<TmfEdge> fSequence;

        /**
         * @param label
         *      Label of the node
         * @param sequence
         *      The initial sequence
         */
        public FEPT(String label, List<TmfEdge> sequence) {
            fLabel  = label;
            fMinimalOccurences = new HashSet<>();
            fMinimalNonOverlappingOccurences = new HashSet<>();

            fChildrens = new ArrayList<>();
            fSequence = sequence;
        }
    }

    /**
     * Compute the MANEPI algorithm
     *
     * @param edges
     *          The ordered list of edges on the path
     */
    public static void compute(List<TmfEdge> edges) {
        FEPT root = new FEPT("", edges);
    }
}
