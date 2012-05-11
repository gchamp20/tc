/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests.experiment;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.experiment.TmfExperiment;
import org.eclipse.linuxtools.tmf.core.experiment.TmfExperimentContext;
import org.eclipse.linuxtools.tmf.core.experiment.TmfExperimentLocation;
import org.eclipse.linuxtools.tmf.core.experiment.TmfLocationArray;
import org.eclipse.linuxtools.tmf.core.tests.TmfCoreTestPlugin;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.TmfCheckpointIndexer;
import org.eclipse.linuxtools.tmf.core.trace.TmfContext;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;

/**
 * Test suite for the TmfCheckpointIndexTest class.
 */
@SuppressWarnings("nls")
public class TmfExperimentCheckpointIndexTest extends TestCase {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static final String DIRECTORY    = "testfiles";
    private static final String TEST_STREAM1 = "O-Test-10K";
    private static final String TEST_STREAM2 = "E-Test-10K";
    private static final String EXPERIMENT   = "MyExperiment";
    private static int          NB_EVENTS    = 20000;
    private static int          BLOCK_SIZE   = 1000;

    private static ITmfTrace<?>[] fTraces;
    private static TestExperiment fExperiment;

    // ------------------------------------------------------------------------
    // Helper classes
    // ------------------------------------------------------------------------

    private class TestIndexer extends TmfCheckpointIndexer<ITmfTrace<ITmfEvent>> {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public TestIndexer(TestExperiment testExperiment) {
            super((ITmfTrace) testExperiment, BLOCK_SIZE);
        }
        public List<TmfCheckpoint> getCheckpoints() {
            return getTraceIndex();
        }
    }

    private class TestExperiment extends TmfExperiment<ITmfEvent> {
        @SuppressWarnings("unchecked")
        public TestExperiment() {
            super(ITmfEvent.class, EXPERIMENT, (ITmfTrace<ITmfEvent>[]) fTraces, TmfTimestamp.ZERO, BLOCK_SIZE, false);
            setIndexer(new TestIndexer(this));
            getIndexer().buildIndex(true);
        }
        @Override
        public TestIndexer getIndexer() {
            return (TestIndexer) super.getIndexer();
        }
    }

    // ------------------------------------------------------------------------
    // Housekeeping
    // ------------------------------------------------------------------------

    public TmfExperimentCheckpointIndexTest(final String name) throws Exception {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupTrace(DIRECTORY + File.separator + TEST_STREAM1, DIRECTORY + File.separator + TEST_STREAM2);
        if (fExperiment == null) {
            fExperiment = new TestExperiment();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        fExperiment.dispose();
        fExperiment = null;
    }

    private static ITmfTrace<?>[] setupTrace(final String path1, final String path2) {
        if (fTraces == null) {
            fTraces = new ITmfTrace[2];
            try {
                URL location = FileLocator.find(TmfCoreTestPlugin.getDefault().getBundle(), new Path(path1), null);
                File test = new File(FileLocator.toFileURL(location).toURI());
                final TmfTraceStub trace1 = new TmfTraceStub(test.getPath(), 0, true);
                fTraces[0] = trace1;
                location = FileLocator.find(TmfCoreTestPlugin.getDefault().getBundle(), new Path(path2), null);
                test = new File(FileLocator.toFileURL(location).toURI());
                final TmfTraceStub trace2 = new TmfTraceStub(test.getPath(), 0, true);
                fTraces[1] = trace2;
            } catch (final TmfTraceException e) {
                e.printStackTrace();
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return fTraces;
    }

    // ------------------------------------------------------------------------
    // Verify checkpoints
    // ------------------------------------------------------------------------

    public void testTmfTraceIndexing() throws Exception {
        assertEquals("getCacheSize",   BLOCK_SIZE, fExperiment.getCacheSize());
        assertEquals("getTraceSize",   NB_EVENTS,  fExperiment.getNbEvents());
        assertEquals("getRange-start", 1,          fExperiment.getTimeRange().getStartTime().getValue());
        assertEquals("getRange-end",   NB_EVENTS,  fExperiment.getTimeRange().getEndTime().getValue());
        assertEquals("getStartTime",   1,          fExperiment.getStartTime().getValue());
        assertEquals("getEndTime",     NB_EVENTS,  fExperiment.getEndTime().getValue());

        List<TmfCheckpoint> checkpoints = fExperiment.getIndexer().getCheckpoints();
        int pageSize = fExperiment.getCacheSize();
        assertTrue("Checkpoints exist",  checkpoints != null);
        assertEquals("Checkpoints size", NB_EVENTS / BLOCK_SIZE, checkpoints.size());

        // Validate that each checkpoint points to the right event
        for (int i = 0; i < checkpoints.size(); i++) {
            TmfCheckpoint checkpoint = checkpoints.get(i);
            TmfExperimentLocation expLocation = (TmfExperimentLocation) checkpoint.getLocation();
            TmfLocationArray locations = expLocation.getLocation();
            ITmfContext[] trcContexts = new ITmfContext[2];
            trcContexts[0] = new TmfContext(locations.getLocations()[0], (i * pageSize) / 2);
            trcContexts[1] = new TmfContext(locations.getLocations()[1], (i * pageSize) / 2);
            TmfExperimentContext expContext = new TmfExperimentContext(trcContexts);
            expContext.getEvents()[0] = fTraces[0].getNext(fTraces[0].seekEvent((i * pageSize) / 2));
            expContext.getEvents()[1] = fTraces[1].getNext(fTraces[1].seekEvent((i * pageSize) / 2));
            ITmfEvent event = fExperiment.parseEvent(expContext);
            assertTrue(expContext.getRank() == i * pageSize);
            assertTrue((checkpoint.getTimestamp().compareTo(event.getTimestamp(), false) == 0));
        }
    }

}