/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.util;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;

/**
 * Generic "Augmented interval tree" data structure.
 *
 * In summary, this is a normal non balanced binary tree sorted by the starting point of an
 * interval. The "augmented" aspect is that every nodes also contain the upper
 * bound of the end intervals in its subtree.
 *
 * @author Guillaume Champagne
 * @param <T> Type of the nodes' value
 * @since 4.2
 */
public class IntervalTree<T extends IInterval> {

    // ------------------------------------------------------------------------
    // Private fields
    // ------------------------------------------------------------------------

    private @Nullable IntervalTree<T> fLeft;
    private @Nullable IntervalTree<T> fRight;
    private final T fOriginalInterval;
    private long fUpperBound;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     * @param interval
     *      The original interval for this tree.
     */
    public IntervalTree(T interval) {
        fOriginalInterval = interval;
        fLeft = null;
        fRight = null;
        fUpperBound = interval.getEnd();
    }

    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    /**
     * Adds an interval in the tree.
     *
     * Cost: O(logN)
     *
     * @param interval
     *      The interval to add
     */
    public void insert(T interval) {
        put(new IntervalTree<>(interval));
    }

    /**
     *
     * @param point
     *          The point to find intersecting intervals for.
     * @return
     *          A List (possibly empty) of intersecting intervals.
     */
    public List<T> getIntersections(long point) {
        List<T> results = new ArrayList<>();
        find(results, point);
        return results;
    }

    /**
     * @return
     *      The upper bound contained in both subtree.
     */
    public long getUpperBound() {
        return fUpperBound;
    }

    /**
     * @return
     *      The starting point of this tree.
     */
    public long getStart() {
        return fOriginalInterval.getStart();
    }

    /**
     * @return
     *      A list of nodes values in no particular order.
     */
    public List<T> getNodeValues() {
        ImmutableList.Builder<T> builder = new ImmutableList.Builder<>();
        this.walkTree(builder);
        return builder.build();
    }

    // ------------------------------------------------------------------------
    // Protected Methods
    // ------------------------------------------------------------------------

    /**
     * @param node
     *          The node to add
     */
    protected void put(IntervalTree<T> node) {
        if (node.getUpperBound() > getUpperBound()) {
            setUpperBound(node.getUpperBound());
        }

        if (node.getStart() >= getStart()) {
            if (fRight == null) {
                fRight = node;
            } else if (fRight != null){
                fRight.put(node);
            }
        } else {
            if (fLeft == null) {
                fLeft  = node;
            } else if (fLeft != null) {
                fLeft.put(node);
            }
        }
    }

    /**
     *
     * @param results
     *          List of intersecting intervals
     * @param point
     *          The point to look for
     */
    protected void find(List<T> results, long point) {
        if (point > getUpperBound()) {
            return;
        }

        if (point > getStart()) {
            if (point <= fOriginalInterval.getEnd()) {
                results.add(fOriginalInterval);
            }

            if (fRight != null) {
                fRight.find(results, point);
            }
        } else if (point == getStart()) {
            results.add(fOriginalInterval);

            if (fLeft != null && point <= fLeft.getUpperBound()) {
                fLeft.find(results, point);
            }

            if (fRight != null) {
                fRight.find(results,  point);
            }
        }
        else if (point < getStart() && fLeft != null) {
            fLeft.find(results, point);
        }
    }

    /**
     * @param builder
     *      Builder that receives the nodes value.
     */
    protected void walkTree(ImmutableList.Builder<T> builder) {
        builder.add(fOriginalInterval);

        if (fLeft != null) {
            fLeft.walkTree(builder);
        }

        if (fRight != null) {
            fRight.walkTree(builder);
        }
    }

    /**
     * @param left
     *          The new left child.
     */
    protected void setLeftChild(IntervalTree<T> left) {
        fLeft = left;
    }

    /**
     * @param right
     *          The new right child.
     */
    protected void setRightChild(IntervalTree<T> right) {
        fRight = right;
    }

    /**
     * Sets the upper bound. Does not change
     * the original interval checked for intersections.
     *
     * @param upperBound
     *          The new maximum upper bound.
     */
    protected void setUpperBound(long upperBound) {
        fUpperBound = upperBound;
    }
}
