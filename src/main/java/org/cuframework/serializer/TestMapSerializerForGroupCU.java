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
 * @author Sidharth Yadav
 */
class TestMapSerializerForGroupCU implements ICompilationUnitSerializer {

    private static final String NON_SERIALIZABLE_KEY = "#NON_SERIALIZABLE_KEY#";

      @Override
      public Object serialize(CompilationRuntimeContext compilationRuntimeContext,
                                  CompilationUnit groupCU)
                                      throws XPathExpressionException {
          if (!(groupCU instanceof Group)) {
              throw new IllegalArgumentException("The serializer expects a CompilationUnits.Group object");
          }
          return buildMap(compilationRuntimeContext, (Group) groupCU);
      }

      private Map<String, Object> buildMap(CompilationRuntimeContext compilationRuntimeContext, Group groupToSerialize)
                                                                          throws XPathExpressionException {
          SerializationPolicy selfSerializationPolicy = groupToSerialize.getSelfSerializationPolicyRuntime();
          SerializationPolicy childSerializationPolicy = groupToSerialize.getChildSerializationPolicyRuntime();

          if (selfSerializationPolicy == SerializationPolicy.NONE) {
              return null;  //we will not attempt serialization on this group at all
          }

         boolean serializeSelfKey = selfSerializationPolicy == SerializationPolicy.ALL;

          boolean isGroupTypeList = groupToSerialize.getType() == GroupType.LIST;
          Map<String, Object> map = new HashMap<String , Object>();
          String jsonObjKey = groupToSerialize.getIdOrElse();  //fall back to name if no id is defined.
          Object value = serializeChildrenOfGroupAsJson(groupToSerialize, isGroupTypeList, childSerializationPolicy,
                                         compilationRuntimeContext);

          map.put(serializeSelfKey ? jsonObjKey : NON_SERIALIZABLE_KEY, value);

          return map;
      }

      private Object serializeChildrenOfGroupAsJson(Group groupToSerialize,
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

              boolean hasSetThisValue = false;

              hasSetThisValue = serializeKey;  //if the key is to be serialized then some output will definitely
                                               //be generated
              if (serializeValue) {
                  if (isGroupTypeList) {
                    ((ArrayList<Object>) collection).add(value);
                  } else {
                    String keyForMapEntry = serializeKey ? attributeToSet : NON_SERIALIZABLE_KEY;
                    ((Map<String, Object>) collection).put(keyForMapEntry, value);
                  }
              } else if (serializeKey) {
                if (isGroupTypeList) {
                    ((ArrayList<Object>) collection).add(attributeToSet);
                  } else {
                    ((Map<String, Object>) collection).put(attributeToSet, value);
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

                  groupAsCollectionTmp = group.build(compilationRuntimeContext, ReturnType.TEST_MAP);  //serialize the group
              } finally {
                  //reset the runtime serialization policies of the child group to its original ones to ensure there
                  //is no side effects on its future serialization that might take place in a different context (e.g.
                  //attempt to find and serialize this child group independently).
                  group.setSelfSerializationPolicyRuntime(group.getSelfSerializationPolicy());
                  group.setChildSerializationPolicyRuntime(group.getChildSerializationPolicy());
              }
              if (isGroupTypeList) {
                  ((ArrayList<Object>) collection).add(groupAsCollectionTmp);
                } else {
                  //String keyForMapEntry = serializeKey ? group.getIdOrElse() : NON_SERIALIZABLE_KEY;
                  ((Map<String, Object>) collection).putAll((Map<String, Object>)groupAsCollectionTmp);
                }
          }

          return collection;
      }
  }
