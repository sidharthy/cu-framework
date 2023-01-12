// Compilation Units Framework: a very generic & powerful data driven programming framework.
// Copyright (c) 2023 Sidharth Yadav, sidharth_08@yahoo.com
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

package org.cuframework.ns;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.cuframework.config.ConfigManager;
import org.cuframework.config.ConfigManager.HierarchicalConfig;
import org.cuframework.core.CompilationUnits;
import org.cuframework.util.logging.LogManager;

/**
 * Namespace configurer of Compilation Units.
 * @author Sidharth Yadav
 *
 */
public class NamespaceConfigurer {
    //namespace config identifier keys
    private static final String CLASSPATH_GROUP_ID = "-classpath-";  //can be used to define classpath at the namespace and cu levels
    private static final String NAMESPACE_CONFIG_GROUP_ID = "-config-";
    private static final String NAMESPACE_CUS_GROUP_ID = "-cus-";
    private static final String NAMESPACE_NAME_KEY = "-name-";
    private static final String NAMESPACE_URI_KEY = "-uri-";
    private static final String NAMESPACE_FUNCTIONS_KEY = "-functions-";
    private static final String CU_ATTRIBUTES_GROUP_ID = "-attributes-";
    private static final String CU_METADATA_GROUP_ID = "-metadata-";
    private static final String CU_CLASS_KEY = "-class-";
    private static final String CU_BODY_KEY = "-body-";

    //the inputMap is supposed to contain the namespace objects
    public static void configure(Map<String, Object> inputMap) throws ClassNotFoundException {
        for (Entry<String, Object> entry: inputMap.entrySet()) {
            String cuid = entry.getKey();
            Object nsConfigMap = entry.getValue();
            if (nsConfigMap instanceof Map) {
                 HierarchicalConfig nshconfig = initNamespaceConfiguration((Map<String, Object>) nsConfigMap);
                 log("info", NamespaceConfigurer.class.getName(), "configure-namespace", cuid + " [" + nshconfig.getId() + "] :: done");
            }
        }
    }

    private static HierarchicalConfig initNamespaceConfiguration(Map<String, Object> nsInputMap)
                                                                         throws ClassNotFoundException {
        Object nsURI = nsInputMap.get(NAMESPACE_URI_KEY);
        if (nsURI == null) {
            nsURI = CompilationUnits.ROOT_CU_NAMESPACE_URI;
        }
        HierarchicalConfig nsHierarchicalConfiguration = ConfigManager.getInstance().getNamespaceConfig(nsURI.toString(), true);
        Object nsName = nsInputMap.get(NAMESPACE_NAME_KEY);
        if (nsName != null) {
            nsHierarchicalConfiguration.setName(nsName.toString());
        }
        Object nsClasspathMap = nsInputMap.get(CLASSPATH_GROUP_ID);
        if (nsClasspathMap instanceof Map) {
            nsHierarchicalConfiguration.put(HierarchicalConfig.STANDARD_KEY_CLASSPATH, nsClasspathMap);
        }
        Object nsCustomFunctions = nsInputMap.get(NAMESPACE_FUNCTIONS_KEY);
        if (nsCustomFunctions != null && !"".equals(nsCustomFunctions.toString().trim())) {
            nsHierarchicalConfiguration.put(HierarchicalConfig.STANDARD_KEY_CUSTOM_FUNCTIONS, nsCustomFunctions.toString());
        }
        Object nsConfigFlatAttributes = nsInputMap.get(NAMESPACE_CONFIG_GROUP_ID);
        if (nsConfigFlatAttributes instanceof Map) {
            initNamespaceFlatAttributes(nsHierarchicalConfiguration, (Map<String, Object>) nsConfigFlatAttributes);
        }
        Object cus = nsInputMap.get(NAMESPACE_CUS_GROUP_ID);
        if (cus instanceof Map) {
            initNamespaceCUs(nsURI.toString(), nsHierarchicalConfiguration, (Map<String, Object>) cus);
        }
        return nsHierarchicalConfiguration;
    }

    private static void initNamespaceFlatAttributes(HierarchicalConfig nsHierarchicalConfiguration,
                                                    Map<String, Object> nsFlatAttributes) {
        nsHierarchicalConfiguration.putAll(nsFlatAttributes);
    }

    private static void initNamespaceCUs(String nsURI,
                                         HierarchicalConfig nsHierarchicalConfiguration,
                                         Map<String, Object> nsCUs)
                                                                throws ClassNotFoundException {
        for (Entry<String, Object> entry: nsCUs.entrySet()) {
            String cuNodeName = entry.getKey();
            Object cuValue = entry.getValue();

            if (cuNodeName == null || cuValue == null) {
                continue;
            }

            String cuClassName = null;
            if (cuValue instanceof Map) {
                Map<String, Object> cuValueMap = (Map<String, Object>) cuValue;
                Object cuAttributes = cuValueMap.get(CU_ATTRIBUTES_GROUP_ID);
                HierarchicalConfig cuNodeConfig = null;
                if (cuAttributes instanceof Map) {
                    cuNodeConfig = new HierarchicalConfig(cuNodeName);
                    cuNodeConfig.put(HierarchicalConfig.STANDARD_CONTAINER__ATTRIBUTES, (Map) cuAttributes);
                }
                Object cuMetadata = cuValueMap.get(CU_METADATA_GROUP_ID);
                if (cuMetadata instanceof Map) {
                    cuNodeConfig = cuNodeConfig == null? new HierarchicalConfig(cuNodeName): cuNodeConfig;
                    cuNodeConfig.put(HierarchicalConfig.STANDARD_CONTAINER__METADATA, (Map) cuMetadata);
                }
                Object cuClasspathMap = cuValueMap.get(CLASSPATH_GROUP_ID);
                if (cuClasspathMap instanceof Map) {
                    cuNodeConfig = cuNodeConfig == null? new HierarchicalConfig(cuNodeName): cuNodeConfig;
                    cuNodeConfig.put(HierarchicalConfig.STANDARD_KEY_CLASSPATH, cuClasspathMap);
                }
                Object cuBody = cuValueMap.get(CU_BODY_KEY);
                if (cuBody != null && !"".equals(cuBody.toString().trim())) {
                    cuNodeConfig = cuNodeConfig == null? new HierarchicalConfig(cuNodeName): cuNodeConfig;
                    cuNodeConfig.put(HierarchicalConfig.STANDARD_KEY_CUBODY, cuBody.toString());
                }
                if (cuNodeConfig != null) {
                    nsHierarchicalConfiguration.linkDownstreamConfig(cuNodeName, cuNodeConfig);
                }

                Object _cuClassName = cuValueMap.get(CU_CLASS_KEY);
                if (_cuClassName != null) {
                    cuClassName = _cuClassName.toString();
                }
            } else if (cuValue != null){
                cuClassName = cuValue.toString();  //the value is assumed to represent the cu class name
            }

            if (cuNodeName == null || cuClassName == null) {
                continue;
            }

            //register cu
            CompilationUnits.setCompilationClassForTag(nsURI, cuNodeName, cuClassName);
        }
    }

    private static void log(String logLevel, String classContext, String methodContext, Object message) {
        Logger logger = LogManager.instance().getLogger();
        if (logger != null) {
            logger.logp(LogManager.getLogLevel(logLevel),
                        classContext,
                        methodContext,
                        message != null? message.toString(): null);
        }
    }
}
