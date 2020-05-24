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

package org.cuframework.serializer;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.core.CompilationUnits;
import org.cuframework.core.CompilationUnits.CompilationUnit;
import org.cuframework.core.CompilationUnits.Group;
import org.cuframework.core.CompilationUnits.Group.GroupType;
import org.cuframework.core.CompilationUnits.Group.ReturnType;
import org.cuframework.core.CompilationUnits.Group.SerializationPolicy;
import org.cuframework.core.CompilationUnits.Set;

/**
 * Serializer for CompilationUnits.Group CU to return a Java Object representation (Map, List, other object type(s) as applicable).
 * @author Sidharth Yadav
 */
class ObjectSerializerForGroupCU implements ICompilationUnitSerializer {

    //private static final String NON_SERIALIZABLE_KEY = "-NON-SERIALIZABLE-KEY-";
    private static final String NULL_KEY = "-NULL-KEY-";
    private static final String KEYLESS_VALUES_GROUP = "-KEYLESS-VALUES-";
    private static final String VALUELESS_KEYS_GROUP = "-VALUELESS-KEYS-";
    private static final String DANGLING_ITEMS_KEY = "-DANGLING-ITEMS-";

    @Override
    public Object serialize(CompilationRuntimeContext compilationRuntimeContext,
                                  CompilationUnit groupCU)
                                      throws XPathExpressionException {
        if (!(groupCU instanceof Group)) {
            throw new IllegalArgumentException("The serializer expects a CompilationUnits.Group object");
        }
        return buildObject(compilationRuntimeContext, (Group) groupCU);
    }

    private Object buildObject(CompilationRuntimeContext compilationRuntimeContext, Group groupToSerialize)
                                                                          throws XPathExpressionException {
        SerializationPolicy selfSerializationPolicy = groupToSerialize.getSelfSerializationPolicyRuntime();
        SerializationPolicy childSerializationPolicy = groupToSerialize.getChildSerializationPolicyRuntime();

        if (selfSerializationPolicy == SerializationPolicy.NONE) {
            return null;  //we will not attempt serialization on this group at all
        }

        boolean serializeSelfKey = !groupToSerialize.isHeadless() &&  //key of a headless group is not to be serialized
                                                                      //irrespective of what the self serialization policy holds.
                                   (selfSerializationPolicy == SerializationPolicy.ALL ||
                                    selfSerializationPolicy == SerializationPolicy.ONLYKEY);

        boolean isGroupTypeList = groupToSerialize.getType() == GroupType.LIST;
        String jsonObjKey = groupToSerialize.getIdOrElse(compilationRuntimeContext);  //fall back to name if no id is defined.
                                                                                      //attempting to get the computed value of idOrElse

        if (selfSerializationPolicy == SerializationPolicy.ONLYKEY) {
            //we will not attempt serialization of child elements (value component) of this group
            return serializeSelfKey && jsonObjKey != null && !"".equals(jsonObjKey) ? jsonObjKey : null;
        }

        Object value = serializeChildrenOfGroupAsObject(groupToSerialize,
                                                        isGroupTypeList,
                                                        childSerializationPolicy,
                                                        compilationRuntimeContext);
        if (groupToSerialize.isHeadless()) {
            return value;
        } else {
            if (selfSerializationPolicy == SerializationPolicy.ALL) {
                Map<String, Object> result = new HashMap<>();
                result.put(jsonObjKey != null && !"".equals(jsonObjKey) ? jsonObjKey : NULL_KEY, value);
                return result;
            } else {  //self SerializationPolicy is ONLYVALUE
                return value;
            }
        }
    }

    private Object serializeChildrenOfGroupAsObject(Group groupToSerialize,
                                                    boolean isGroupTypeList,
                                                    SerializationPolicy childSerializationPolicy,
                                                    CompilationRuntimeContext compilationRuntimeContext)
                                                                                  throws XPathExpressionException {
        if (childSerializationPolicy == SerializationPolicy.NONE) {
            return null;
        }
        CompilationUnits.Set[] sets = groupToSerialize.getChildren(CompilationUnits.Set.class);
        Object collection = isGroupTypeList ? new ArrayList<Object>() : new HashMap<String , Object>();
        boolean serializeKey = childSerializationPolicy == SerializationPolicy.ALL ||
                               childSerializationPolicy == SerializationPolicy.ONLYKEY;
        boolean serializeValue = childSerializationPolicy == SerializationPolicy.ALL ||
                                 childSerializationPolicy == SerializationPolicy.ONLYVALUE;
        for (int i = 0; i < sets.length; i++) {
            Set set = sets[i];
            String attributeToSet = set.getAttributeToSet(compilationRuntimeContext);  //using the computed version to get the attribute name
            Object value = set.getValue(compilationRuntimeContext);

            if (value == null && !set.doOutputNullValue()) {
                continue;
            }

            if (serializeKey && serializeValue) {
                String keyForMapEntry = attributeToSet == null? NULL_KEY: attributeToSet;
                addToCollection(keyForMapEntry, value, collection);
            } else if (serializeValue) {  //serialize value only
                if (isGroupTypeList) {
                    addToList(value, (List<Object>) collection);
                } else {
                    //addToCollection(NON_SERIALIZABLE_KEY, value, collection);  //instead of attempting to add to a map shall we just add it to the list.
                    segregateUnpairedItems(KEYLESS_VALUES_GROUP, value, (Map<String, Object>) collection);
                }
            } else if (serializeKey && attributeToSet != null) {  //serialize key only
                if (isGroupTypeList) {
                    addToList(attributeToSet, (List<Object>) collection);
                } else {
                    //addToCollection(attributeToSet, null, collection);  //instead of attempting to add to a map shall we just add it to the list.
                    segregateUnpairedItems(VALUELESS_KEYS_GROUP, attributeToSet, (Map<String, Object>) collection);
                }
            }
        }

        Group[] groups = groupToSerialize.getChildren(Group.class);

        for (int i = 0; i < groups.length; i++) {
            Group group = groups[i];

            Object groupAsCollectionTmp = null;
            try {
                /********************************************************************************************/
                //adjust serialization policies of child group according to the policies of the parent group.
                if (childSerializationPolicy != SerializationPolicy.ALL) {
                    group.setSelfSerializationPolicyRuntime(childSerializationPolicy);
                } else {
                    //if the child serialization policy of the current group is ALL then we don't want
                    //to make any change to the self serialization policy of the child group.
                    group.setSelfSerializationPolicyRuntime(group.getSelfSerializationPolicy());
                }
                /********************************************************************************************/

                groupAsCollectionTmp = group.build(compilationRuntimeContext, ReturnType.OBJECT);  //serialize the group
            } finally {
                //reset the runtime serialization policies of the child group to its original ones to ensure there
                //is no side effects on its future serialization that might take place in a different context (e.g.
                //attempt to find and serialize this child group independently).
                group.setSelfSerializationPolicyRuntime(group.getSelfSerializationPolicy());
                group.setChildSerializationPolicyRuntime(group.getChildSerializationPolicy());
            }

            if (groupAsCollectionTmp == null) {
                continue;
            }

            if (isGroupTypeList) {
                addToList(groupAsCollectionTmp, (List<Object>) collection);
            } else {
                Map<String, Object> _collection = (Map<String, Object>) collection;
                if (groupAsCollectionTmp instanceof Map) {
                    _collection.putAll((Map<String, Object>) groupAsCollectionTmp);
                } else {
                    segregateUnpairedItems(DANGLING_ITEMS_KEY, groupAsCollectionTmp, _collection);
                }
            }
        }

        return collection;
    }

    private void segregateUnpairedItems(String itemsGroup, Object itemValue, Map<String, Object> collection) {
        Object existingUnpairedItemsGroupObj = collection.get(itemsGroup);
        List<Object> unpairedItemsList = existingUnpairedItemsGroupObj instanceof List?
                                                 (List<Object>) existingUnpairedItemsGroupObj:  //Note: if there was a group defined with the same
                                                                                                //name as the unpaired items group identifier and
                                                                                                //if it is also of type list then we would use the
                                                                                                //same list instance to add other homogenous unpaired
                                                                                                //items also as they are encountered at runtime.
                                                 null;
        if (unpairedItemsList == null) {
            unpairedItemsList = new ArrayList<>();
            if (existingUnpairedItemsGroupObj != null) {
                unpairedItemsList.add(existingUnpairedItemsGroupObj);  //if there was a group defined with same name as the
                                                                       //unpaired items group identifier then let's copy its
                                                                       //value also in the list.
            }
            addToCollection(itemsGroup, unpairedItemsList, collection);
        }
        unpairedItemsList.add(itemValue);
    }

    private void addToList(Object item, List<Object> collection) {
        collection.add(item);
    }

    private void addToCollection(String key, Object value, Object collection) {
        if (collection instanceof List) {
            Map<String, Object> wrapperMap = new HashMap<>();
            wrapperMap.put(key, value);
            ((List) collection).add(wrapperMap);
        } else if (collection instanceof Map) {
            ((Map) collection).put(key, value);
        }
    }
}
