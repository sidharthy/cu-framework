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

package org.cuframework;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Representation of a Map of maps.
 * @author Sidharth Yadav
 *
 */
public class MapOfMaps {

    /**
     * Defining some generic map names.
     *
     */
    public enum Name {
        CONTEXT_MAP("CONTEXT-MAP"),
        SYSTEM_MAP("SYSTEM-MAP");

        Name(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        private String key;
    }

    private final Map<String, Map<String, Object>> mapOfMaps;

    public MapOfMaps() {
        this(new TreeMap<String, Map<String, Object>>());
    }

    public MapOfMaps(Map<String, Map<String, Object>> map) {
        this.mapOfMaps = map;
        this.mapOfMaps.put(Name.SYSTEM_MAP.getKey(), getSystemMap());  //let's make the system map
                                                                       //always available for use.
    }

    private Map<String, Object> getSystemMap() {
        Map<String, Object> sysPropsMap = new HashMap<String, Object>();
        for (Entry<Object, Object> entry: System.getProperties().entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            sysPropsMap.put(key.toString(), value/* != null ? value.toString() : null*/);
        }
        sysPropsMap.put("CURRENT_TIME", (new Date()).toString());
        return sysPropsMap;
    }

    public Map<String, Map<String, Object>> getMapOfMaps() {
        return mapOfMaps;
    }

    /**
     * Returns the map identified by the specified name.
     *
     * @param mapName
     *            name of the map
     * @return the map identified by the specified name
     */
    public Map<String, Object> getMap(String mapName) {
        return getMap(mapName, false);
    }

    /**
     * Returns the map identified by the specified name. If the map is not
     * present, and the create parameter is true, an empty map will be created.
     *
     * @param mapName
     *            name of the map
     * @param create
     *            if true and the specified map does not exist, an empty one is
     *            created.
     * @return the map identified by the specified name
     *
     */
    public Map<String, Object> getMap(String mapName, boolean create) {
        Map<String, Object> map = mapOfMaps.get(mapName);
        if (map == null && create) {
            map = new TreeMap<String, Object>();
            mapOfMaps.put(mapName, map);
        }
        return map;
    }

    /**
     * Puts the specified map into the Map of maps using the specified
     * map name as key. (The previous map with the same name, if any, will be
     * discarded).
     *
     * @param mapName
     *            name of the map to be added
     * @param map
     *            the map to be added
     */
    public void putMap(String mapName, Map<String, Object> map) {
        mapOfMaps.put(mapName, map);
    }
}
