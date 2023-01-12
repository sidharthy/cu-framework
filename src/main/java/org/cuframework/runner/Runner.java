// Compilation Units Framework: a very generic & powerful data driven programming framework.
// Copyright (c) 2019 Sidharth Yadav, sidharth_08@yahoo.com
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

package org.cuframework.runner;

import java.io.FileNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import org.cuframework.MapOfMaps;
import org.cuframework.TemplateCompilationException;
import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.core.CompilationUnits;
import org.cuframework.core.CompilationUnits.ICompilationUnit;
import org.cuframework.core.CompilationUnits.Group;
import org.cuframework.core.CompiledTemplate;
import org.cuframework.core.CompiledTemplatesRegistry;
import org.cuframework.ns.NamespaceConfigurer;
import org.cuframework.util.logging.LogManager;

/**
 * Generic Runner for Compilation Units.
 * @author Sidharth Yadav
 *
 */
public class Runner {
    private static final String RESOURCES_DIR = "./resources/";

    //sys props
    protected static final String SYS_PROP_CU_DIR = "cu.dir";
    protected static final String SYS_PROP_DEFAULT_CU_START_FILE = "cu.start";

    //other props
    protected static final String CU_NAMESPACES_GROUP_ID = "-namespaces-";

    //runner context identifier keys
    protected static final String DEFAULT_CU_DIR = "DEFAULT_CU_DIR";
    protected static final String CONTEXT_MAP_NAME = "CONTEXT_MAP_NAME";
    protected static final String INPUT_MAP_NAME = "INPUT_MAP_NAME";
    protected static final String OUTPUT_MAP_NAME = "OUTPUT_MAP_NAME";
    protected static final String CU_DEPENDENCIES_GROUP_ID = "CU_DEPENDENCIES_GROUP_ID";
    protected static final String CU_CONTEXT_DATA_GROUP_ID = "CU_CONTEXT_DATA_GROUP_ID";
    protected static final String CU_RUNNER_GROUP_ID = "CU_RUNNER_GROUP_ID";

    private String savedPrimaryLookupDirExternalContext = "";
    private String savedSecondaryLookupDirExternalContext = "";

    //subclasses can override as needed
    protected Map<String, String> getRunnerContext() {
        Map<String, String> runnerContext = new HashMap<>();
        runnerContext.put(DEFAULT_CU_DIR, RESOURCES_DIR);
        runnerContext.put(CONTEXT_MAP_NAME, MapOfMaps.Name.CONTEXT_MAP.getKey());
        runnerContext.put(INPUT_MAP_NAME, "INPUT-MAP");
        runnerContext.put(OUTPUT_MAP_NAME, "OUTPUT-MAP");
        runnerContext.put(CU_DEPENDENCIES_GROUP_ID, "-dependencies-");
        runnerContext.put(CU_CONTEXT_DATA_GROUP_ID, "-context-data-");
        runnerContext.put(CU_RUNNER_GROUP_ID, "-run-");
        return runnerContext;
    }

    //subclasses can override as needed
    protected void doInit() {
        String defaultLoggerType = System.getProperty("cu.default.logger.type");
        if (defaultLoggerType != null)
            LogManager.instance().setDefaultLoggerType(defaultLoggerType);

        savedPrimaryLookupDirExternalContext = CompiledTemplatesRegistry.getInstance().
                                                                                getPrimaryLookupDirectory();
        savedSecondaryLookupDirExternalContext = CompiledTemplatesRegistry.getInstance().
                                                                                getSecondaryLookupDirectory();
        String cuLookupDir = System.getProperty(SYS_PROP_CU_DIR) != null?
                                      System.getProperty(SYS_PROP_CU_DIR): getRunnerContext().get(DEFAULT_CU_DIR);
        CompiledTemplatesRegistry.getInstance().setPrimaryLookupDirectory(cuLookupDir);
        CompiledTemplatesRegistry.getInstance().setSecondaryLookupDirectory(cuLookupDir);

        log("info", "init", "CU lookup dir set to '" + cuLookupDir + "'");
    }

    //subclasses can override as needed
    protected void doFinally() {
        CompiledTemplatesRegistry.getInstance().
                                            setPrimaryLookupDirectory(savedPrimaryLookupDirExternalContext);
        CompiledTemplatesRegistry.getInstance().
                                            setSecondaryLookupDirectory(savedSecondaryLookupDirExternalContext);
    }

    //subclasses can override as needed
    protected void log(String logLevel, String methodContext, Object message) {
        log(logLevel, getClass().getName(), methodContext, message);
    }

    //made protected to limit scope to package and subclasses
    protected static void log(String logLevel, String classContext, String methodContext, Object message) {
        Logger logger = LogManager.instance().getLogger();
        if (logger != null) {
            logger.logp(LogManager.getLogLevel(logLevel),
                        classContext,
                        methodContext,
                        message != null? message.toString(): null);
        }
    }

    public static void main(String[] args) throws Exception {
        Runner runner = new Runner();
        runner.doInit();
        try {
            String cuFile = System.getProperty("cu.start");  //CU Template File - The Starting Point
            if (cuFile == null) {
                cuFile = "cu-start.xml";  //default cu starting template file
            }
            runner.log("info", "main", "CU file (starting point): '" +cuFile+ "'");
            runner.run(cuFile);
        } finally {
            runner.doFinally();
        }
    }

    //subclasses can override as needed
    protected void run(String cuFile) throws Exception {
        Map<String, String> runnerContext = getRunnerContext();
        CompilationRuntimeContext compilationRuntimeContext = getEmptyCompilationRuntimeContext();

        //first initialize the dependencies
        compilationRuntimeContext = initializeDependencies(compilationRuntimeContext,
                                                       cuFile,
                                                       runnerContext.get(CU_DEPENDENCIES_GROUP_ID));

        //initialize the context data maps
        compilationRuntimeContext = initializeContextDataMaps(compilationRuntimeContext,
                                                       cuFile,
                                                       runnerContext.get(CU_CONTEXT_DATA_GROUP_ID));

        for (Map.Entry<String, Object>  entry : compilationRuntimeContext.getExternalContext().
                                                           getMap(runnerContext.get(INPUT_MAP_NAME)).entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            doRun(getFreshContextMap(value,
                                     compilationRuntimeContext.getExternalContext().
                                                                    getMap(runnerContext.get(OUTPUT_MAP_NAME)).get(key)),
                  cuFile,
                  runnerContext.get(CU_RUNNER_GROUP_ID));
        }
    }

    //subclasses can override as needed
    protected CompilationRuntimeContext initializeDependencies(CompilationRuntimeContext compilationRuntimeContext,
                                                               String cuFile,
                                                               String cuIdOfDependenciesGroup) throws ClassNotFoundException,
                                                                                                      FileNotFoundException,
                                                                                                      XPathExpressionException {
        Group compilationUnit = getCompiledCU(cuFile, cuIdOfDependenciesGroup, Group.class);
        if (compilationUnit != null) {
            Object dependencies = compilationUnit.build(compilationRuntimeContext, "map");
            if (dependencies instanceof Map) {
                Object namespaces = ((Map) dependencies).get(CU_NAMESPACES_GROUP_ID);
                if (namespaces instanceof Map) {
                    NamespaceConfigurer.configure((Map<String, Object>) namespaces);
                } else {
                    log("debug", "init-namespaces (none)", namespaces);
                }
                log("debug", "init-dependencies (ok)", dependencies);
            } else {
                log("debug", "init-dependencies (none)", dependencies);
            }
        }

        return compilationRuntimeContext;
    }

    //subclasses can override as needed
    protected CompilationRuntimeContext initializeContextDataMaps(CompilationRuntimeContext compilationRuntimeContext,
                                                                  String cuFile,
                                                                  String cuIdOfContextDataGroup)
                                                                          throws FileNotFoundException, XPathExpressionException {
        Group compilationUnit = getCompiledCU(cuFile, cuIdOfContextDataGroup, Group.class);
        if (compilationUnit != null) {
            compilationUnit.build(compilationRuntimeContext, "json");
        }

        return compilationRuntimeContext;
    }

    //subclasses can override as needed
    protected Object doRun(Map<String, Object> contextMap, String cuFile, String cuId) throws XPathExpressionException,
                                                                                              FileNotFoundException,
                                                                                              TemplateCompilationException {
        MapOfMaps mapOfMaps = new MapOfMaps();
        mapOfMaps.putMap(getRunnerContext().get(CONTEXT_MAP_NAME), contextMap);

        CompilationRuntimeContext compilationRuntimeContext = new CompilationRuntimeContext();
        compilationRuntimeContext.setExternalContext(mapOfMaps);

        //process extensions and initialize the template object
        //I opted to process the extensions this late and not inside the run method because here the context
        //map has been updated with the run data and that also gives us the opportunity to rightly process any
        //conditional 'extends' before starting the actual cu execution.
        CompiledTemplate mct = CompiledTemplatesRegistry.getInstance().
                                      processExtensions(CompiledTemplatesRegistry.getInstance().getCompiledTemplate(cuFile),
                                                compilationRuntimeContext);
        log("info", "run", "******************** " + cuFile + " ********************");
        log("info",
            "run",
            "Mct extensions processed status - processed instance = " + mct.extensionsProcessed() + " : raw instance " +
                                                CompiledTemplatesRegistry.getInstance().getCompiledTemplate(cuFile).extensionsProcessed());

        Group compilationUnit = getCompiledCU(mct, new String[]{cuId}, Group.class);

        return compilationUnit.build(compilationRuntimeContext, "json");  //by default we will attempt to return json as the
                                                                          //output format. This however can be changed using
                                                                          //the CompilationUnits.PARAM_GROUP_SERIALIZER_TYPE
                                                                          //internal context attribute of the group inside
                                                                          //the template xml.
    }

    private CompilationRuntimeContext getEmptyCompilationRuntimeContext() {
        Map<String, String> runnerContext = getRunnerContext();

        MapOfMaps mapOfMaps = new MapOfMaps();
        mapOfMaps.putMap(runnerContext.get(CONTEXT_MAP_NAME), new HashMap<String, Object>());
        mapOfMaps.putMap(runnerContext.get(INPUT_MAP_NAME), new HashMap<String, Object>());
        mapOfMaps.putMap(runnerContext.get(OUTPUT_MAP_NAME), new HashMap<String, Object>());

        CompilationRuntimeContext compilationRuntimeContext = new CompilationRuntimeContext();
        compilationRuntimeContext.setExternalContext(mapOfMaps);

        return compilationRuntimeContext;
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

    private <T extends ICompilationUnit> T getCompiledCU(String cuXmlFile, String id, Class<T> cuType)
                                                        throws FileNotFoundException, XPathExpressionException {
        return CompiledTemplatesRegistry.getInstance().getCompilationUnit(cuXmlFile + "#" + id, cuType);
    }

    private <T extends ICompilationUnit> T getCompiledCU(CompiledTemplate mct, String[] idTrail, Class<T> cuType)
                                                           throws FileNotFoundException, XPathExpressionException {
        return CompiledTemplatesRegistry.getInstance().getCompilationUnit(mct, idTrail, cuType);
    }
}
