/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.tests.view.timegraph;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.interval.IHTInterval;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.internal.tmf.ui.views.timegraph.IntervalTree;
import org.junit.Test;

/**
 * Tests for the class {@link IntervalTree}
 *
 * @author Guillaume Champagne
 */
/* This warning is suppresses because the IntervalTree is internal by design */
@SuppressWarnings("restriction")
public class IntervalTreeTest {

    /**
     * Test for queries on a single node tree.
     */
    @Test
    public void TestSingleNodeTree() {
        IntervalStub root = new IntervalStub(0, 10);
        IntervalTree<@NonNull IntervalStub> t = new IntervalTree<>(root);

        assertEquals(0, t.getStart());
        assertEquals(10, t.getUpperBound());

        List<IntervalStub> intersects = t.getIntersections(5);
        assertEquals(1, intersects.size());

        intersects = t.getIntersections(11);
        assertEquals(0, intersects.size());

        intersects = t.getIntersections(10);
        assertEquals(1, intersects.size());

        intersects = t.getIntersections(0);
        assertEquals(1, intersects.size());
    }

    /**
     * Test for queries on a tree with multiple nodes.
     *
     * Tree:
     *            -------(40, 80)--------
     *            |                     |
     *     -----(10, 20)---        ---(50, 90)----
     *     |              |        |             |
     *   (0, 10)      (10, 40)   (45, 50)    (90, 110)
     */
    @Test
    public void TestMultipleNodeTree() {


        IntervalStub root = new IntervalStub(40, 80);
        IntervalTree<@NonNull IntervalStub> t = new IntervalTree<>(root);
        t.insert(new IntervalStub(50, 90));
        t.insert(new IntervalStub(45, 50));
        t.insert(new IntervalStub(90, 110));

        t.insert(new IntervalStub(10, 20));
        t.insert(new IntervalStub(0, 10));
        t.insert(new IntervalStub(10, 40));

        List<IntervalStub> intersects = t.getIntersections(50);
        assertEquals(3, intersects.size());
        assertEquals(40, intersects.get(0).getStart());
        assertEquals(50, intersects.get(1).getStart());
        assertEquals(45, intersects.get(2).getStart());

        intersects = t.getIntersections(90);
        assertEquals(2, intersects.size());
        assertEquals(50, intersects.get(0).getStart());
        assertEquals(90, intersects.get(1).getStart());

        intersects = t.getIntersections(100);
        assertEquals(1, intersects.size());
        assertEquals(90, intersects.get(0).getStart());

        intersects = t.getIntersections(10);
        assertEquals(3, intersects.size());
        assertEquals(10, intersects.get(0).getStart());
        assertEquals(0, intersects.get(1).getStart());
        assertEquals(10, intersects.get(2).getStart());

        intersects = t.getIntersections(35);
        assertEquals(1, intersects.size(), 1);
        assertEquals(10, intersects.get(0).getStart());

        intersects = t.getIntersections(15);
        assertEquals(2, intersects.size());
        assertEquals(10, intersects.get(0).getStart());

        intersects = t.getIntersections(0);
        assertEquals(1, intersects.size());
        assertEquals(0, intersects.get(0).getStart());
    }

    /**
     * Class that implements {@link IInterval} for these tests only.
     *
     * @author Guillaume Champagne
     */
    private static class IntervalStub implements IHTInterval {

        private long fStart;
        private long fEnd;

        public IntervalStub (long start, long end) {
            fStart = start;
            fEnd = end;
        }

        @Override
        public long getStart() {
            return fStart;
        }

        @Override
        public long getEnd() {
            return fEnd;
        }

        @Override
        public int getSizeOnDisk() {
            return 0;
        }

        @Override
        public void writeSegment(@NonNull ISafeByteBufferWriter buffer) {
        }
    }

}
