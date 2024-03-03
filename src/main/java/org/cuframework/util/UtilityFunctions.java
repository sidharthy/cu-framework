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

package org.cuframework.util;

import java.io.StringWriter;

import java.lang.reflect.Array;

import java.net.URL;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Sidharth Yadav
 */
public final class UtilityFunctions {
    /**
     * Checks if a specific value is present in a String array.
     */
    public static boolean isItemInArray(String item, String[] items) {
        if (item == null || items == null) {
            return false;
        }
        boolean found = false;
        for (String _item: items) {
            if (item.equals(_item)) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Does a nested lookup inside the map and tries to extract value corresponding to a hierarchy of keys.
     * It uses dot(.) as the default delimiter of keys inside the keyHierarchy.
     */
    public static Object getValue(String keyHierarchy, Map<String, Object> map) {
        return getValue(keyHierarchy, "\\.", map);
    }

    /**
     * Does a nested lookup inside the map and tries to extract value corresponding to a hierarchy of keys.
     * The keyHierarchyDelimiter is optional but when specified should provide value as regular expression e.g. if dot(.) is to be used
     * as the key hierarchy delimiter then it should be specified as '\.' (quotes excluded). If null is passed as value of this parameter
     * then dot(.) would be used as the default delimiter.
     */
    public static Object getValue(String keyHierarchy, String keyHierarchyDelimiter, Map<String, Object> map) {
        Object value = null;
        if (keyHierarchy != null && map != null) {
            String[] keys = keyHierarchy.
                                split(keyHierarchyDelimiter == null? "\\.": keyHierarchyDelimiter);  //if no custom delimiter specified then
                                                                                                     //dot would be used as the default delimiter.
            Object _value = null;
            for (int i = 0; i < keys.length; i++) {
                String[] outputGroups = getRegexGroups(keys[i]);
                String key = outputGroups[0];
                boolean indicesSpecified = outputGroups.length > 1;

                _value = map.get(key/*keys[i]*/);
                if (indicesSpecified && _value != null) {
                    _value = getIndexValue(outputGroups[1], _value);
                } else if (_value == null && "[]".equals(key)) {  //deliberately not calling trim() over key as trimming of string values
                                                                  //like '  [] ', though, can functionally indicate use of .size() but
                                                                  //would degrade the readability of xml code. Thus don't want to encourage
                                                                  //the practise of loosely using white spaces while specifying key values.
                    _value = map.size();  //return the size of map
                }

                if (i == keys.length - 1) {
                    //last element
                    value = _value;
                } else if (_value instanceof Map) {
                    try {
                        map = (Map<String, Object>) _value;
                    } catch(Exception e) {
                        break;  //the key sequences have not yet exhausted and also we came across an object which is not of type map.
                                //There is no point in continuing and let's break out. null value would get returned.
                    }
                } else {
                    break;  //the key sequences have not yet exhausted and also we came across an object which is not of type map.
                            //There is no point in continuing and let's break out. null value would get returned.
                }
            }
        }
        return value;
    }

    private static Object getIndexValue(String indicesString, Object indexTarget) {
        //we will attempt to find the element at the specified indices
        //also the accessed value(s) must represent appropriate object types like list or array items

        Object _value = null;
        Matcher matcher = Pattern.compile("\\[([0-9]*)\\]+").matcher(indicesString);
        while(matcher.find()) {
            String _index = matcher.group(1);  //per the regex index value would be accessible using group(1)

            int index = _index != null && !_index.equals("")? Integer.parseInt(_index): -1;  //if _index is null or "" then we
                                                                                             //potentially encountered []
            if (indexTarget instanceof List) {
                if (index == -1)
                    _value = ((List) indexTarget).size();
                else
                    _value = ((List) indexTarget).get(index);  //if the index is out of bounds then let an exception be thrown.
            } else if (indexTarget != null && indexTarget.getClass().isArray()) {
                //using reflection here to access array element in a generic way.
                if (index == -1)
                    _value = Array.getLength(indexTarget);
                else
                    _value = Array.get(indexTarget, index);  //if the index is out of bounds then let an exception be thrown.
            } else if (indexTarget instanceof Collection) {
                if (index == -1)
                    _value = ((Collection) indexTarget).size();
                else {
                    //even though a collection can return an array object through its toArray(...) method(s) but still
                    //we will treat the situation as if the access chain broke and no longer points to an object type
                    //that can support indexed operations. This is because toArray(...) method(s) of not all collections
                    //guarantee the order of their elements and hence index based access may return different object(s)
                    //at different times and hence won't behave predictably.
                    _value = null;
                    break;
                }
            } else if (indexTarget instanceof Map) {
                if (index == -1) {
                    //let's support size operation over map using []
                    _value = ((Map) indexTarget).size();
                }
                else {
                    //access chain broke as it no longer points to an object type that can support indexed operations.
                    //null would be returned.
                    _value = null;
                    break;
                }
            } else {
                _value = null;  //access chain broke as it no longer points to an object type that can support indexed operations.
                                //null would be returned.
                break;
            }
            indexTarget = _value;
        }
        return _value;
    }

    private static String[] getRegexGroups(String input) {
        //"(^[\\w ]+|\\[(?:[ 0-9]+|)*+\\])";  //(^[\w\-\$ ]+)((\[[ 0-9]*\]| )*)$
        String extractionExpression = "(^[\\w-$*&@#:;,.|<>%!~\\/\\\\(){}+ ]+)((\\[[0-9]*\\]| )*)$";
        Matcher matcher = Pattern.compile(extractionExpression).matcher(input);
        boolean inputMatchesPattern = matcher.find();
        String[] outputGroups = null;  //new String[inputMatchesPattern? 2: 1];  //if the pattern doesn't match at all then also we need
                                                                                 //to return atleast one element i.e. the input string itself.
        if (inputMatchesPattern) {
            //now that the input matches the pattern we can safely assume that the provided input contains a key and a
            //balanced pair(s) of array indices. Also we can now safely try to extract values of group 1 and group 2.

            //if input represents the string 'mykey[10][2]' then:
            String group1Value = matcher.group(1);  //group 1 would contain the string identifying the key i.e. 'mykey' (excluding quotes)
            String group2Value = matcher.group(2);  //group 2 would set the array indices as one unified string i.e. '[10][2]' (excluding quotes).
                                                    //If however the input didn't contain any array indices i.e.
                                                    //e.g. 'mykey' then group 2 would return null or an empty string.

            boolean indicesSpecified = group2Value != null && !group2Value.equals("");
            outputGroups = new String[indicesSpecified? 2: 1];
            outputGroups[0] = group1Value;
            if (indicesSpecified)
                outputGroups[1] = group2Value;
        } else {
            outputGroups = new String[1];  //if the pattern doesn't match at all then also we need to return atleast one
                                           //element i.e. the raw input string itself.
            outputGroups[0] = input;  //return input as is.
        }
        return outputGroups;
    }

    public static String serializeChildNodesToString(Node node, Map<String, List<String>> excludeNamespaceNodes) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            NodeList nl = (NodeList) node.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                String nodeName = getLocalOrNodeName(n);
                String namespaceURI = n.getNamespaceURI();
                if (excludeNamespaceNodes == null ||
                    excludeNamespaceNodes.get(namespaceURI) == null ||
                    (excludeNamespaceNodes.get(namespaceURI) != null && !excludeNamespaceNodes.get(namespaceURI).contains(nodeName))) {
                    t.transform(new DOMSource(n), new StreamResult(sw));
                }
            }
        } catch(TransformerException te) {
            throw new RuntimeException(te);
        }
        return sw.toString();
    }

    //Attempts to first return the local name of the node (i.e. without the namespace prefix). If no local name is found then
    //it will return the value of getNodeName() of the passed node.
    public static String getLocalOrNodeName(Node n) {
        return n.getLocalName() != null? n.getLocalName(): n.getNodeName();
    }

    public static URL[] getClasspathURLs(Map<String, Object> classpath) {
        if (classpath == null || classpath.size() == 0) {
            return new URL[0];
        }
        Set<URL> urls = new HashSet<>();
        for (Entry<String, Object> _classpathE: classpath.entrySet()) {
            Object value = _classpathE.getValue();
            if (value == null) {
                continue;
            }
            String _classpath = value.toString().trim();
            String jarUrl = toJarUrl(_classpath);
            if (jarUrl == null) {
                continue;
            }
            try {
                urls.addAll(JarClasspathResolver.resolveClasspathFromJar(new URL(jarUrl)));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        return urls.toArray(new URL[0]);
    }

    public static URL[] getClasspathURLs(Object[] classpath) {
        if (classpath == null || classpath.length == 0) {
            return new URL[0];
        }
        Set<URL> urls = new HashSet<>();
        for (Object value: classpath) {
            if (value == null) {
                continue;
            }
            String _classpath = value.toString().trim();
            String jarUrl = toJarUrl(_classpath);
            if (jarUrl == null) {
                continue;
            }
            try {
                urls.addAll(JarClasspathResolver.resolveClasspathFromJar(new URL(jarUrl)));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        return urls.toArray(new URL[0]);
    }

    private static String toJarUrl(String classpath) {
        if (classpath == null) {
            return null;
        }
        String jarUrl = null;
        if (classpath.startsWith("http:") || classpath.startsWith("https:")) {
            jarUrl = "jar:" + classpath + "!/";
        }
        else {
            //we assume the location points to a file on disk
            jarUrl = "jar:file:/" + classpath + "!/";
        }
        return jarUrl;
    }
}
