package org.cuframework;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.cuframework.MapOfMaps;
import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.core.CompilationUnits;
import org.cuframework.core.CompilationUnits.ICompilationUnit;
import org.cuframework.core.CompilationUnits.Group;
import org.cuframework.core.CompiledTemplatesRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test runner class for executing cunit tests - new generation unit tests.
 * The test definitions as well as the test data are both defined as cu in xml files.
 *
 * @author Sidharth Yadav
 */
public class TestRunner {
    private static final String RESOURCE_META_INF_DIR = "../src/test/resources/tests/";
    private static final String INSTALLED_META_INF_DIR = "./src/test/resources/tests/";

    private static final String CONTEXT_MAP = MapOfMaps.Name.CONTEXT_MAP.getKey();
    private static final String TEST_INPUT_MAP = "TEST-INPUT-MAP";
    private static final String TEST_OUTPUT_MAP = "TEST-OUTPUT-MAP";

    private static String savedPrimaryLookupDirExternalContext = "";
    private static String savedSecondaryLookupDirExternalContext = "";

    private static File[] testDefinitions = null;

    @BeforeClass
    public static void init() throws XPathExpressionException, FileNotFoundException {
        savedPrimaryLookupDirExternalContext = CompiledTemplatesRegistry.getInstance().
                                                                                getPrimaryLookupDirectory();
        savedSecondaryLookupDirExternalContext = CompiledTemplatesRegistry.getInstance().
                                                                                getSecondaryLookupDirectory();
        CompiledTemplatesRegistry.getInstance().setPrimaryLookupDirectory(INSTALLED_META_INF_DIR);
        CompiledTemplatesRegistry.getInstance().setSecondaryLookupDirectory(RESOURCE_META_INF_DIR);

        File f = new File(INSTALLED_META_INF_DIR);
        testDefinitions = f.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".xml");
            }
        });

        if (System.getProperty("cu.default.logger.type") != null)
            org.cuframework.util.logging.LogManager.instance().setDefaultLoggerType(System.getProperty("cu.default.logger.type"));

        //register sample cus
        CompilationUnits.setCompilationClassForTag(org.cuframework.samplecu.Calculator.TAG_NAME, org.cuframework.samplecu.Calculator.class);
    }

    @AfterClass
    public static void restoreLookupDirs() throws XPathExpressionException, FileNotFoundException {
        CompiledTemplatesRegistry.getInstance().
                                            setPrimaryLookupDirectory(savedPrimaryLookupDirExternalContext);
        CompiledTemplatesRegistry.getInstance().
                                            setSecondaryLookupDirectory(savedSecondaryLookupDirExternalContext);
    }

    @Test
    public void runCunitTests() throws Exception {
        //cunit tests - new generation unit tests
        //the test definitions as well as the test data are defined in xml files as cus.

        for (int i = 0; i < testDefinitions.length; i++) {
            String cuFile = testDefinitions[i].getName();

            CompilationRuntimeContext compilationRuntimeContext = getEmptyCompilationRuntimeContext();

            //process and initialize template object inside registry
            CompiledTemplatesRegistry.getInstance().processExtensions(CompiledTemplatesRegistry.getInstance().getCompiledTemplate(cuFile),
                                                                      compilationRuntimeContext);

            compilationRuntimeContext = initializeTestDataMaps(compilationRuntimeContext,
                                                               cuFile,
                                                               "-test-data-");  //cu defining the test data should have the id -test-data-
            for (Map.Entry<String, Object>  entry : compilationRuntimeContext.getExternalContext().getMap(TEST_INPUT_MAP).entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                doTest(getFreshContextMap(value,
                                          compilationRuntimeContext.getExternalContext().getMap(TEST_OUTPUT_MAP).get(key)),
                       cuFile,
                       "-test-");  //cu to test should have the id -test-
            }
        }
    }

    private Map<String, Object> getFreshContextMap(Object copyInputPairs, Object copyOutputPairs) {
        Map<String, Object> contextMap = new HashMap<String, Object>();
        if (copyInputPairs instanceof Map) {
            contextMap.putAll((Map) copyInputPairs);
        }
        if (copyOutputPairs instanceof Map) {
            contextMap.putAll((Map) copyOutputPairs);
        }
        return contextMap;
    }

    private CompilationRuntimeContext getEmptyCompilationRuntimeContext() {
        MapOfMaps mapOfMaps = new MapOfMaps();
        mapOfMaps.putMap(CONTEXT_MAP, new HashMap<String, Object>());
        mapOfMaps.putMap(TEST_INPUT_MAP, new HashMap<String, Object>());
        mapOfMaps.putMap(TEST_OUTPUT_MAP, new HashMap<String, Object>());

        CompilationRuntimeContext compilationRuntimeContext = new CompilationRuntimeContext();
        compilationRuntimeContext.setExternalContext(mapOfMaps);

        return compilationRuntimeContext;
    }

    private CompilationRuntimeContext initializeTestDataMaps(CompilationRuntimeContext compilationRuntimeContext,
                                                             String cuFile,
                                                             String cuIdOfTestDataGroup)
                                                                      throws FileNotFoundException, XPathExpressionException {
        Group compilationUnit = getCompiledCU(cuFile, cuIdOfTestDataGroup, Group.class);
        if (compilationUnit != null) {
            compilationUnit.build(compilationRuntimeContext, Group.ReturnType.JSON);
        }

        return compilationRuntimeContext;
    }

    private Object doTest(Map<String, Object> contextMap, String cuFile, String cuId) throws Exception {
        MapOfMaps mapOfMaps = new MapOfMaps();
        mapOfMaps.putMap(CONTEXT_MAP, contextMap);

        CompilationRuntimeContext compilationRuntimeContext = new CompilationRuntimeContext();
        compilationRuntimeContext.setExternalContext(mapOfMaps);

        Group compilationUnit = getCompiledCU(cuFile, cuId, Group.class);

        Object returnValue = compilationUnit.build(compilationRuntimeContext, Group.ReturnType.JSON);
        return returnValue;
    }

    private <T extends ICompilationUnit> T getCompiledCU(String cuXmlFile, String id, Class<T> cuType)
                                                        throws FileNotFoundException, XPathExpressionException {
        return CompiledTemplatesRegistry.getInstance().getCompilationUnit(cuXmlFile + "#" + id,
                                                                           cuType);
    }
}
