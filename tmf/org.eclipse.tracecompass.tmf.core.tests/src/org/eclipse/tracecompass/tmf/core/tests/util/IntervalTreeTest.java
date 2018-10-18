/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.tests.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.util.IInterval;
import org.eclipse.tracecompass.tmf.core.util.IntervalTree;
import org.junit.Test;

/**
 * Tests for the class {@link IntervalTree}
 *
 * @author Guillaume Champagne
 */
@SuppressWarnings("javadoc")
public class IntervalTreeTest {

    @Test
    public void TestSingleNodeTree() {
        IntervalStub root = new IntervalStub(0, 10);
        IntervalTree<@NonNull IntervalStub> t = new IntervalTree<>(root);

        assertEquals(t.getStart(), 0);
        assertEquals(t.getUpperBound(), 10);

        List<IntervalStub> intersects = t.getIntersections(5);
        assertEquals(intersects.size(), 1);

        intersects = t.getIntersections(11);
        assertEquals(intersects.size(), 0);

        intersects = t.getIntersections(10);
        assertEquals(intersects.size(), 1);

        intersects = t.getIntersections(0);
        assertEquals(intersects.size(), 1);
    }

    @Test
    public void TestMultipleNodeTree() {

        /*
         * TREE:
         *            -------(40, 80)--------
         *            |                     |
         *     -----(10, 20)---        ---(50, 90)----
         *     |              |        |             |
         *   (0, 10)      (10, 40)   (45, 50)    (90, 110)
         */

        IntervalStub root = new IntervalStub(40, 80);
        IntervalTree<@NonNull IntervalStub> t = new IntervalTree<>(root);
        t.insert(new IntervalStub(50, 90));
        t.insert(new IntervalStub(45, 50));
        t.insert(new IntervalStub(90, 110));

        t.insert(new IntervalStub(10, 20));
        t.insert(new IntervalStub(0, 10));
        t.insert(new IntervalStub(10, 40));

        List<IntervalStub> intersects = t.getIntersections(50);
        assertEquals(intersects.size(), 3);
        assertEquals(intersects.get(0).getStart(), 40);
        assertEquals(intersects.get(1).getStart(), 50);
        assertEquals(intersects.get(2).getStart(), 45);

        intersects = t.getIntersections(90);
        assertEquals(intersects.size(), 2);
        assertEquals(intersects.get(0).getStart(), 50);
        assertEquals(intersects.get(1).getStart(), 90);

        intersects = t.getIntersections(100);
        assertEquals(intersects.size(), 1);
        assertEquals(intersects.get(0).getStart(), 90);

        intersects = t.getIntersections(10);
        assertEquals(intersects.size(), 3);
        assertEquals(intersects.get(0).getStart(), 10);
        assertEquals(intersects.get(1).getStart(), 0);
        assertEquals(intersects.get(2).getStart(), 10);

        intersects = t.getIntersections(35);
        assertEquals(intersects.size(), 1);
        assertEquals(intersects.get(0).getStart(), 10);

        intersects = t.getIntersections(15);
        assertEquals(intersects.size(), 2);
        assertEquals(intersects.get(0).getStart(), 10);

        intersects = t.getIntersections(0);
        assertEquals(intersects.size(), 1);
        assertEquals(intersects.get(0).getStart(), 0);
    }
}

class IntervalStub implements IInterval {

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
}
