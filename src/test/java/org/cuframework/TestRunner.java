package org.cuframework;

import java.io.File;
import java.io.FileFilter;

import java.util.HashMap;
import java.util.Map;

import org.cuframework.core.CompilationUnits;
import org.cuframework.runner.Runner;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test runner class for executing cunit tests - new generation unit tests.
 * The test definitions as well as the test data are both defined as cu in xml files.
 *
 * @author Sidharth Yadav
 */
public class TestRunner extends Runner {
    private static final String RESOURCES_DIR = "./src/test/resources/tests/";

    private static File[] testDefinitions = null;

    @Override
    protected Map<String, String> getRunnerContext() {
        Map<String, String> runnerContext = new HashMap<>();
        runnerContext.put(DEFAULT_CU_DIR, RESOURCES_DIR);
        runnerContext.put(CONTEXT_MAP_NAME, MapOfMaps.Name.CONTEXT_MAP.getKey());
        runnerContext.put(INPUT_MAP_NAME, "TEST-INPUT-MAP");
        runnerContext.put(OUTPUT_MAP_NAME, "TEST-OUTPUT-MAP");
        runnerContext.put(CU_DEPENDENCIES_GROUP_ID, "-dependencies-");
        runnerContext.put(CU_CONTEXT_DATA_GROUP_ID, "-test-data-");
        runnerContext.put(CU_RUNNER_GROUP_ID, "-test-");
        return runnerContext;
    }

    @BeforeClass
    public static void staticInit() {
        String defaultLoggerType = System.getProperty("cu.default.logger.type");
        if (defaultLoggerType != null)
            org.cuframework.util.logging.LogManager.instance().setDefaultLoggerType(defaultLoggerType);
        String cuLookupDir = System.getProperty(SYS_PROP_CU_DIR) != null?
                                      System.getProperty(SYS_PROP_CU_DIR): RESOURCES_DIR;

        File f = new File(cuLookupDir);
        if (f.exists()) {
            testDefinitions = f.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().endsWith(".xml");
                }
            });
        } else {
            log("info", TestRunner.class.getName(), "cunit-tests-dir", "The cunit tests folder doesn't exist: '" + cuLookupDir + "'");
        }

        //register sample cus
        CompilationUnits.setCompilationClassForTag(org.cuframework.samplecu.Calculator.TAG_NAME, org.cuframework.samplecu.Calculator.class);
    }

    @Before
    public void init() {
        doInit();
    }

    @After
    public void finallyy() {
        doFinally();
    }

    @Test
    public void runCunitTests() throws Exception {
        //cunit tests - new generation unit tests
        //the test definitions as well as the test data are defined in xml files as cus.

        if (testDefinitions == null || testDefinitions.length == 0) {
            log("info", "run-cunit-tests", "No tests to run");
            return;
        }

        for (int i = 0; i < testDefinitions.length; i++) {
            String cuFile = testDefinitions[i].getName();
            run(cuFile);
        }
    }
}
