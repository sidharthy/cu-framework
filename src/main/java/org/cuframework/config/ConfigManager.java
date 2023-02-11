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

package org.cuframework.config;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.cuframework.core.CompilationUnits;

/**
 * Class to manage the various configs like that of system, realms, namespaces etc.
 * @author Sidharth Yadav
 *
 */
public class ConfigManager {

    private static final String DEFAULT_CU_NAMESPACE_URI = CompilationUnits.ROOT_CU_NAMESPACE_URI;

    private static final String CU = "cu";  //prefix to be used with all cu tag configurations when defined as flat hierarchy (e.g. cu.group.tbt = child)
    private static final String CU_METADATA = "cumetadata";  //prefix to be used with all cu metadata configurations when defined as flat hierarchy
                                                            //(e.g. cumetadata.div.mergeable-attrs = style, css)
    private static final String JOINER = ".";  //char used as joiner to form the key of attribute to be looked up inside the various hierarchical config maps
    private static final String STAR = "*";  //wildcard char used to form the blanket key of attribute to be looked up inside the various hierarchical config maps
    private static final String MERGEABLE_ATTRIBUTES_SPLITTER = ",";

    /**
     * Defining some generic config ids.
     *
     */
    private enum ConfigID {
        SYSTEM("SYSTEM-CONFIG");
        //REALM("REALM-CONFIG"),
        //NAMESPACE("NAMESPACE-CONFIG");

        ConfigID(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        private String key;
    }

    public static class HierarchicalConfig extends HashMap<String, Object> {
        public static final String STANDARD_CONTAINER__ATTRIBUTES = "-attributes-";
        public static final String STANDARD_CONTAINER__METADATA = "-metadata-";
        public static final String STANDARD_METADATA_KEY_MERGEABLE_ATTRIBUTES = "mergeable-attrs";
        public static final String STANDARD_KEY_CLASSPATH = "-classpath-";
        public static final String STANDARD_KEY_CUBODY = "-cubody-";
        public static final String STANDARD_KEY_CUSTOM_FUNCTIONS = "-functions-";

        private String id = null;
        private String name= null;  //for information purpose
        private HashMap<String, HierarchicalConfig> linkedDownstreamConfigs = null;
        private HierarchicalConfig parentConfig = null;

        public HierarchicalConfig(String id) {
            super();
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public HierarchicalConfig getDownstreamConfig(String configId) {
            return configId == null || linkedDownstreamConfigs == null? null: linkedDownstreamConfigs.get(configId);
        }

        public synchronized void linkDownstreamConfig(String configId, HierarchicalConfig config) {
            if (configId == null) {
                return;
            }
            if (linkedDownstreamConfigs == null) {
                linkedDownstreamConfigs = new HashMap<>();
            }
            if (config != null) {
                config.parentConfig = this;
            }
            linkedDownstreamConfigs.put(configId, config);
        }

        private Map<String, Object> getContainer(String containerId) {
            Object container = get(containerId);
            return container instanceof Map? (Map) container: null;
        }

        //searches for the value of the key inside the container maps. Returns the first non null value found.
        private Object _fetchFromInsideContainers(String[] containerIds, String key) {
            if (containerIds == null || key == null) {
                return null;
            }
            Object value = null;
            for (String containerId: containerIds) {
                Object container = get(containerId);
                if (container instanceof Map) {
                    value = ((Map) container).get(key);
                }
                if (value != null) {
                    break;
                }
            }
            return value;
        }

        //looks up for the attribute value inside the standard attributes container of this config.
        //No hierarchical lookup is performed.
        public Object getAttribute(String attr) {
            return _fetchFromInsideContainers(new String[]{STANDARD_CONTAINER__ATTRIBUTES}, attr);
        }

        //looks up for the key's value inside the standard metadata container of this config.
        //No hierarchical lookup is performed.
        public Object getMetadata(String key) {  //key = metadata identifier
            return _fetchFromInsideContainers(new String[]{STANDARD_CONTAINER__METADATA}, key);
        }

        //does a hierarchical lookup of the key's value inside the specified container.
        //If the specified container is null then the key is looked up directly inside this top level map instance.
        public Object get(String[] downstreamConfigHierarchy, String keyContainerId, String key) {
            if (key == null) {
                return null;
            }

            //initialize with the top level value
            Object value = keyContainerId == null?
                                       get(key):
                                       _fetchFromInsideContainers(new String[]{keyContainerId}, key);

            if (downstreamConfigHierarchy == null || downstreamConfigHierarchy.length == 0) {
                return value;
            }
            for (int i = 0; i < downstreamConfigHierarchy.length; i++) {
                String downstreamConfigId = downstreamConfigHierarchy[i];
                HierarchicalConfig downstreamConfig = getDownstreamConfig(downstreamConfigId);
                Object downstreamValue = downstreamConfig == null? null: downstreamConfig.get(
                                                                                i == downstreamConfigHierarchy.length - 1?
                                                                                    null:
                                                                                    Arrays.copyOfRange(downstreamConfigHierarchy, i + 1, downstreamConfigHierarchy.length),
                                                                                keyContainerId,
                                                                                key);
                if (downstreamValue != null) {
                    value = downstreamValue;
                }
            }
            return value;
        }

        //the passed keys would be looked up in priority order. The first non null value found would be returned.
        //This method performs a hierarchical lookup.
        public Object get(String[] downstreamConfigHierarchy, String keyContainerId, String[] lookupKeysInPriorityOrder) {
            if (lookupKeysInPriorityOrder == null) {
                return null;
            }
            Object value = null;
            for (String key: lookupKeysInPriorityOrder) {
                value = get(downstreamConfigHierarchy, keyContainerId, key);
                if (value != null) {
                    break;
                }
            }
            return value;
        }
    }

    private static ConfigManager cm = new ConfigManager();
    private final Map<String, HierarchicalConfig> configs;

    private ConfigManager() {
        this.configs = new HashMap<>();
        this.configs.put(ConfigID.SYSTEM.getKey(), getSystemMap());  //let's make the system map
                                                                       //always available for use.
    }

    public static ConfigManager getInstance() {
        return cm;
    }

    private HierarchicalConfig getSystemMap() {
        HierarchicalConfig sysPropsMap = new HierarchicalConfig(ConfigID.SYSTEM.getKey());
        for (Entry<Object, Object> entry: System.getProperties().entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            sysPropsMap.put(key.toString(), value);
        }
        sysPropsMap.put("CURRENT_TIME", (new Date()));
        return sysPropsMap;
    }

    public Map<String, HierarchicalConfig> getConfigs() {
        return configs;
    }

    /**
     * Returns the config identified by the specified id.
     *
     * @param configId
     *            id of the config
     * @return the config identified by the specified id
     */
    public HierarchicalConfig getConfig(String configId) {
        return getConfig(configId, false);
    }

    /**
     * Returns the config identified by the specified id. If the config is not
     * present, and the create parameter is true, an empty config will be created.
     *
     * @param configId
     *            id of the config
     * @param create
     *            if true and the specified config does not exist, an empty one is
     *            created.
     * @return the config identified by the specified id
     *
     */
    public HierarchicalConfig getConfig(String configId, boolean create) {
        HierarchicalConfig config = configs.get(configId);
        if (config == null && create) {
            config = new HierarchicalConfig(configId);
            configs.put(configId, config);
        }
        return config;
    }

    /**
     * Puts the specified config into the configs map using the specified
     * config id as key. (The previous config with the same id, if any, will be
     * discarded).
     *
     * @param configId
     *            id of the config to be added
     * @param map
     *            the config to be added
     */
    public void putConfig(String configId, HierarchicalConfig config) {
        configs.put(configId, config);
    }

    public HierarchicalConfig getSystemConfig() {
        return getConfig(ConfigID.SYSTEM.getKey());
    }

    public HierarchicalConfig getRealmConfig(String realmId) {
        return getRealmConfig(realmId, false);
    }

    public HierarchicalConfig getRealmConfig(String realmId, boolean create) {
        return realmId == null? null: getConfig("realm://" + realmId, create);
    }

    public HierarchicalConfig getNamespaceConfig(String namespaceURI) {
        return getNamespaceConfig(namespaceURI, null);
    }

    public HierarchicalConfig getNamespaceConfig(String namespaceURI, boolean create) {
        return getNamespaceConfig(namespaceURI, null, create);
    }

    public HierarchicalConfig getNamespaceConfig(String namespaceURI, String fallbackNamespaceURI) {
        return getNamespaceConfig(namespaceURI, fallbackNamespaceURI, false);
    }

    public HierarchicalConfig getNamespaceConfig(String namespaceURI, String fallbackNamespaceURI, boolean create) {
        namespaceURI = namespaceURI == null? fallbackNamespaceURI: namespaceURI;
        return namespaceURI == null? null: getConfig("namespace://" + namespaceURI, create);
    }

    private void addMergeableAttributes(Set<String> mergeableAttributesSet, Object attrs) {
        String[] attrsAsStringArray = new String[0];
        if (attrs instanceof String) {
            attrsAsStringArray = attrs.toString().split(MERGEABLE_ATTRIBUTES_SPLITTER);
        } else if (attrs instanceof String[]) {
            attrsAsStringArray = (String[]) attrs;
        }
        for (String attr: attrsAsStringArray) {
            mergeableAttributesSet.add(attr);
        }
    }

    public Set<String> getNamesOfMergeableAttributes(String nodeName, String tagName, String namespaceURI, String realmId) {
        Set<String> mergeableAttributes = new HashSet<>();
        String attribute = HierarchicalConfig.STANDARD_METADATA_KEY_MERGEABLE_ATTRIBUTES;

        boolean nodeAndTagNamesAreSame = nodeName != null? nodeName.equals(tagName): false;
        String[] downstreamConfigIdsInAscPriorityOrder = nodeAndTagNamesAreSame?
                                                               new String[]{STAR, nodeName}:
                                                               new String[]{STAR, tagName, nodeName};
        Map<String, Object> attributes = new HashMap<>();
        HierarchicalConfig sysConfig = getSystemConfig();
        if (sysConfig != null) {
            for (String downstreamConfigId: downstreamConfigIdsInAscPriorityOrder) {
                //first collect attributes added directly to the config map as flattened values
                addMergeableAttributes(mergeableAttributes, sysConfig.get(CU_METADATA + JOINER + downstreamConfigId + JOINER + attribute));
            }
        }
        for (HierarchicalConfig config: new HierarchicalConfig[]{getNamespaceConfig(namespaceURI, DEFAULT_CU_NAMESPACE_URI), getRealmConfig(realmId)}) {
            if (config != null) {
                for (String downstreamConfigId: downstreamConfigIdsInAscPriorityOrder) {
                    //first collect attributes added directly to the config map as flattened values
                    addMergeableAttributes(mergeableAttributes, config.get(CU_METADATA + JOINER + downstreamConfigId + JOINER + attribute));

                    //now collect attributes of the corresponding downstream config
                    HierarchicalConfig downstreamConfig = config.getDownstreamConfig(downstreamConfigId);
                    if (downstreamConfig != null) {
                        addMergeableAttributes(mergeableAttributes, downstreamConfig.getMetadata(attribute));
                    }
                }
            }
        }
        /* config = getRealmConfig(realmId);
        if (config != null) {
            for (String downstreamConfigId: downstreamConfigIdsInAscPriorityOrder) {
                //first collect attributes added directly to the config map as flattened values
                addMergeableAttributes(mergeableAttributes, config.get(CU_METADATA + JOINER + downstreamConfigId + JOINER + attribute));
                
                //now collect attributes of the corresponding downstream config
                HierarchicalConfig downstreamConfig = config.getDownstreamConfig(downstreamConfigId);
                if (downstreamConfig != null) {
                    addMergeableAttributes(mergeableAttributes, downstreamConfig.getMetadata(attribute));
                }
            }
        } */
        return mergeableAttributes;
    }

    public Map<String, Object> getClasspath(String nodeName, String namespaceURI, String realmId) {
        final String CLASSPATH_KEY = HierarchicalConfig.STANDARD_KEY_CLASSPATH;
        String[] downstreamConfigIdsInAscPriorityOrder = new String[]{nodeName};
        Map<String, Object> classpath = new HashMap<>();
        HierarchicalConfig sysConfig = getSystemConfig();
        if (sysConfig != null) {
            //Nothing to do here as classpath at the system level would normally be set through command line
        }
        for (HierarchicalConfig config: new HierarchicalConfig[]{getNamespaceConfig(namespaceURI, DEFAULT_CU_NAMESPACE_URI), getRealmConfig(realmId)}) {
            if (config != null) {
                //first collect classpath, if any, defined at the config level (e.g. namespace Or realm level)
                if (config.get(CLASSPATH_KEY) instanceof Map) {
                    classpath.putAll((Map<String, Object>) config.get(CLASSPATH_KEY));  //NOTE: we are assuming here that the type cast to Map would not fail.
                                                                                        //If instead of a map some other object type corresponds to the key's
                                                                                        //value then a ClassCastException would be thrown.
                }

                //now collect classpath, if any, defined at downstream config levels
                for (String downstreamConfigId: downstreamConfigIdsInAscPriorityOrder) {
                    HierarchicalConfig downstreamConfig = config.getDownstreamConfig(downstreamConfigId);
                    if (downstreamConfig != null &&
                           (downstreamConfig.get(CLASSPATH_KEY) instanceof Map)) {
                        classpath.putAll((Map<String, Object>) downstreamConfig.get(CLASSPATH_KEY));  //NOTE: we are assuming here that the type cast to Map would not fail.
                                                                                                      //If instead of a map some other object type corresponds to the key's
                                                                                                      //value then a ClassCastException would be thrown.
                    }
                }
            }
        }
        return classpath;
    }

    public String getCustomFunctions(String namespaceURI, String realmId) {
        final String CUSTOM_FUNCTIONS_KEY = HierarchicalConfig.STANDARD_KEY_CUSTOM_FUNCTIONS;
        Object customFunctions = null;
        for (HierarchicalConfig config: new HierarchicalConfig[]{getNamespaceConfig(namespaceURI, DEFAULT_CU_NAMESPACE_URI), getRealmConfig(realmId)}) {
            if (config != null) {
                //custom function definitions would be looked up only at the top config levels (i.e. namespace Or realm level)
                customFunctions = config.get(CUSTOM_FUNCTIONS_KEY);
            }
            if (customFunctions != null) {
                break;
            }
        }
        return customFunctions != null? customFunctions.toString(): null;
    }

    public String getCuBody(String nodeName, String namespaceURI, String realmId) {
        final String CUBODY_KEY = HierarchicalConfig.STANDARD_KEY_CUBODY;
        String[] downstreamConfigIdsInAscPriorityOrder = new String[]{nodeName};
        Object cubody = null;
        for (HierarchicalConfig config: new HierarchicalConfig[]{getNamespaceConfig(namespaceURI, DEFAULT_CU_NAMESPACE_URI), getRealmConfig(realmId)}) {
            if (config != null) {
                //cubody definition would not be looked up at the top config level (i.e. namespace Or realm level)
                //cubody definition would be looked up only at the downstream (node) config levels
                for (String downstreamConfigId: downstreamConfigIdsInAscPriorityOrder) {
                    HierarchicalConfig downstreamConfig = config.getDownstreamConfig(downstreamConfigId);
                    if (downstreamConfig != null) {
                        cubody = downstreamConfig.get(CUBODY_KEY);
                    }
                    if (cubody != null) {
                        break;
                    }
                }
            }
            if (cubody != null) {
                break;
            }
        }
        return cubody != null? cubody.toString(): null;
    }

    public Object getMetadataValue(String nodeName, String tagName, String namespaceURI, String realmId, String key) {
        String CONTAINER_ID = HierarchicalConfig.STANDARD_CONTAINER__METADATA;

        boolean nodeAndTagNamesAreSame = nodeName != null? nodeName.equals(tagName): false;
        String[] downstreamConfigIdsInPriorityOrder = nodeAndTagNamesAreSame?
                                                              new String[]{nodeName, STAR}:
                                                              new String[]{nodeName, tagName, STAR};
        String[] flatLookupKeysInPriorityOrder = nodeAndTagNamesAreSame?
                                                      new String[]{CU_METADATA + JOINER + nodeName + JOINER + key,
                                                                   CU_METADATA + JOINER + STAR + JOINER + key}:
                                                      new String[]{CU_METADATA + JOINER + nodeName + JOINER + key,
                                                                   CU_METADATA + JOINER + tagName + JOINER + key,
                                                                   CU_METADATA + JOINER + STAR + JOINER + key};
        Object value = null;
        HierarchicalConfig config = getRealmConfig(realmId);  //first lookup for metadata value inside realm config
        for (String downstreamConfigId: downstreamConfigIdsInPriorityOrder) {
            value = getValue(config, downstreamConfigId, CONTAINER_ID, key);
            if (value != null) {
                break;
            }
        }
        if (value == null) {
            value = getValue(config, flatLookupKeysInPriorityOrder);  //lookup metadata value using the
                                                                      //flattened keys inside the realm config
        }
        if (value == null) {
            config = getNamespaceConfig(namespaceURI, DEFAULT_CU_NAMESPACE_URI);  //now lookup for metadata value inside namespace config
            for (String downstreamConfigId: downstreamConfigIdsInPriorityOrder) {
                value = getValue(config, downstreamConfigId, CONTAINER_ID, key);
                if (value != null) {
                    break;
                }
            }
            if (value == null) {
                value = getValue(config, flatLookupKeysInPriorityOrder);  //lookup metadata value using the
                                                                          //flattened keys inside the namespace config
            }
        }
        if (value == null) {
            value = getValue(getSystemConfig(), flatLookupKeysInPriorityOrder);  //now lookup for metadata value using the
                                                                                 //flattened keys inside the system config
        }
        return value;
    }

    public Object getAttributeValue(String nodeName, String tagName, String namespaceURI, String realmId, String attribute) {
        String CONTAINER_ID = HierarchicalConfig.STANDARD_CONTAINER__ATTRIBUTES;

        boolean nodeAndTagNamesAreSame = nodeName != null? nodeName.equals(tagName): false;
        String[] downstreamConfigIdsInPriorityOrder = nodeAndTagNamesAreSame?
                                                              new String[]{nodeName, STAR}:
                                                              new String[]{nodeName, tagName, STAR};
        String[] flatLookupKeysInPriorityOrder = nodeAndTagNamesAreSame?
                                                      new String[]{CU + JOINER + nodeName + JOINER + attribute,
                                                                   CU + JOINER + STAR + JOINER + attribute}:
                                                      new String[]{CU + JOINER + nodeName + JOINER + attribute,
                                                                   CU + JOINER + tagName + JOINER + attribute,
                                                                   CU + JOINER + STAR + JOINER + attribute};
        Object value = null;
        HierarchicalConfig config = getRealmConfig(realmId);  //first lookup for attribute value inside realm config
        for (String downstreamConfigId: downstreamConfigIdsInPriorityOrder) {
            value = getValue(config, downstreamConfigId, CONTAINER_ID, attribute);
            if (value != null) {
                break;
            }
        }
        if (value == null) {
            value = getValue(config, flatLookupKeysInPriorityOrder);  //lookup attribute value using the
                                                                      //flattened keys inside the realm config
        }
        if (value == null) {
            config = getNamespaceConfig(namespaceURI, DEFAULT_CU_NAMESPACE_URI);  //now lookup for attribute value inside namespace config
            for (String downstreamConfigId: downstreamConfigIdsInPriorityOrder) {
                value = getValue(config, downstreamConfigId, CONTAINER_ID, attribute);
                if (value != null) {
                    break;
                }
            }
            if (value == null) {
                value = getValue(config, flatLookupKeysInPriorityOrder);  //lookup attribute value using the
                                                                          //flattened keys inside the namespace config
            }
        }
        if (value == null) {
            value = getValue(getSystemConfig(), flatLookupKeysInPriorityOrder);  //now lookup for attribute value using the
                                                                                 //flattened keys inside the system config
        }
        return value;
    }

    private Object getValue(HierarchicalConfig config, String[] lookupKeysInPriorityOrder) {
        return config == null? null: config.get(null, null, lookupKeysInPriorityOrder);
    }

    private Object getValue(HierarchicalConfig config, String downstreamConfigId, String keyContainerId, String key) {
        return config == null? null: config.get(new String[]{downstreamConfigId}, keyContainerId, key);
    }

    public Map<String, Object> getAttributes(String nodeName, String tagName, String namespaceURI, String realmId) {
        boolean nodeAndTagNamesAreSame = nodeName != null? nodeName.equals(tagName): false;
        String[] downstreamConfigIdsInAscPriorityOrder = nodeAndTagNamesAreSame?
                                                               new String[]{STAR, nodeName}:
                                                               new String[]{STAR, tagName, nodeName};
        Map<String, Object> attributes = new HashMap<>();
        HierarchicalConfig config = getSystemConfig();
        if (config != null) {
            for (String downstreamConfigId: downstreamConfigIdsInAscPriorityOrder) {
                //first collect attributes added directly to the config map as flattened values
                attributes.putAll(collectAttributesFromFlatKeys(config, CU + JOINER + downstreamConfigId + JOINER));
            }
        }
        config = getNamespaceConfig(namespaceURI, DEFAULT_CU_NAMESPACE_URI);
        if (config != null) {
            for (String downstreamConfigId: downstreamConfigIdsInAscPriorityOrder) {
                //first collect attributes added directly to the config map as flattened values
                attributes.putAll(collectAttributesFromFlatKeys(config, CU + JOINER + downstreamConfigId + JOINER));

                //now collect attributes of the corresponding downstream config
                HierarchicalConfig downstreamConfig = config.getDownstreamConfig(downstreamConfigId);
                if (downstreamConfig != null) {
                    Map<String, Object> attributesMap = downstreamConfig.getContainer(HierarchicalConfig.STANDARD_CONTAINER__ATTRIBUTES);
                    if (attributesMap != null)
                        attributes.putAll(attributesMap);
                }
            }
        }
        config = getRealmConfig(realmId);
        if (config != null) {
            for (String downstreamConfigId: downstreamConfigIdsInAscPriorityOrder) {
                //first collect attributes added directly to the config map as flattened values
                attributes.putAll(collectAttributesFromFlatKeys(config, CU + JOINER + downstreamConfigId + JOINER));

                //now collect attributes of the corresponding downstream config
                HierarchicalConfig downstreamConfig = config.getDownstreamConfig(downstreamConfigId);
                if (downstreamConfig != null) {
                    Map<String, Object> attributesMap = downstreamConfig.getContainer(HierarchicalConfig.STANDARD_CONTAINER__ATTRIBUTES);
                    if (attributesMap != null)
                        attributes.putAll(attributesMap);
                }
            }
        }
        return attributes;
    }

    //this method basically looks up for the flat key values directly inside the config's top level map.
    private Map<String, Object> collectAttributesFromFlatKeys(HierarchicalConfig config,
                                                              String keyPrologue) {
        Map<String, Object> attributes = new HashMap<>();
        if (config == null || keyPrologue == null) {
            return attributes;
        }
        for (Entry<String, Object> entry: config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith(keyPrologue) && key.length() != keyPrologue.length()) {
                attributes.put(key.substring(keyPrologue.length()), value);
            }
        }
        return attributes;
    }

    public boolean isSystemNamespaceAware() {
        HierarchicalConfig sysConfig = getConfig(ConfigID.SYSTEM.getKey());
        Object sysPropNamespaceAware = sysConfig == null? null: sysConfig.get("cus.namespace.aware");
        return sysPropNamespaceAware == null?
                    true:  //by default the system would be namespace aware and all cu xmls would be accordingly processed.
                    Boolean.valueOf(sysPropNamespaceAware.toString());
    }
}
