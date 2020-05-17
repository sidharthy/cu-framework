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
 * Json serializer for CompilationUnits.Group.
 * @author Sidharth Yadav
 *
 */
class JSONSerializerForGroupCU implements ICompilationUnitSerializer {

    @Override
    public Object serialize(CompilationRuntimeContext compilationRuntimeContext,
                                CompilationUnit groupCU)
                                    throws XPathExpressionException {
        if (!(groupCU instanceof Group)) {
            throw new IllegalArgumentException("The serializer expects a CompilationUnits.Group object");
        }
        //try {
            return buildJson(compilationRuntimeContext, (Group) groupCU);
        //} catch (XPathExpressionException e) {
        //    throw new CompilationUnitSerializationException(e);  //TODO throw this custom exception.
        //}
    }

    private String buildJson(CompilationRuntimeContext compilationRuntimeContext, Group groupToSerialize)
                                                                        throws XPathExpressionException {
        SerializationPolicy selfSerializationPolicy = groupToSerialize.getSelfSerializationPolicyRuntime();
        SerializationPolicy childSerializationPolicy = groupToSerialize.getChildSerializationPolicyRuntime();

        if (selfSerializationPolicy == SerializationPolicy.NONE) {
            return "";  //we will not attempt serialization on this group at all
        }

        boolean serializeSelfKey = !groupToSerialize.isHeadless() &&  //key of a headless group is not to be serialized
                                                                      //irrespective of what the self serialization policy holds.
                                   (selfSerializationPolicy == SerializationPolicy.ALL ||
                                   selfSerializationPolicy == SerializationPolicy.ONLYKEY);

        boolean isGroupTypeList = groupToSerialize.getType() == GroupType.LIST;
        StringBuilder strBuilder = new StringBuilder();
        String jsonObjKey = groupToSerialize.getIdOrElse(compilationRuntimeContext);  //fall back to name if no id is defined.
                                                                                      //attempting to get the computed value of idOrElse
        String QUOTATION_MARK = groupToSerialize.getQuotationMarks();
        if (serializeSelfKey && jsonObjKey != null && !"".equals(jsonObjKey)) {
            strBuilder.append(QUOTATION_MARK);
            strBuilder.append(jsonObjKey);
            strBuilder.append(QUOTATION_MARK);
        }

        if (selfSerializationPolicy == SerializationPolicy.ONLYKEY) {
            return strBuilder.toString();  //we will not attempt serialization of child elements (value component)
                                           //of this group
        }

        //if (selfSerializationPolicy == SerializationPolicy.ALL) {
        if (serializeSelfKey && selfSerializationPolicy == SerializationPolicy.ALL) {  //in case of headless group the key serialization
                                                                                       //might not have happened even if serialization policy was set to ALL.
                                                                                       //Hence including the same in the check.
            //key would have already been serialized. Let's add a colon before we attempt serialization of value.
            strBuilder.append(":");
        }

        if (!groupToSerialize.isHeadless())  //for headless group we should not to put the serialized child object within bounds
        {
            strBuilder.append(isGroupTypeList ? "[" : "{");
        }
        serializeChildrenOfGroupAsJson(groupToSerialize, isGroupTypeList, childSerializationPolicy,
                                       strBuilder, compilationRuntimeContext);
        if (!groupToSerialize.isHeadless())  //for headless group we should not to put the serialized child object within bounds
        {
            strBuilder.append(isGroupTypeList ? "]" : "}");
        }
        return strBuilder.toString();
    }

    private void serializeChildrenOfGroupAsJson(Group groupToSerialize,
                                                boolean isGroupTypeList,
                                                SerializationPolicy childSerializationPolicy,
                                                StringBuilder strBuilder,
                                                CompilationRuntimeContext compilationRuntimeContext)
                                                                                throws XPathExpressionException {
        if (childSerializationPolicy == SerializationPolicy.NONE) {
            return;
        }
        String QUOTATION_MARK = groupToSerialize.getQuotationMarks();
        CompilationUnits.Set[] sets = groupToSerialize.getChildren(CompilationUnits.Set.class);
        boolean hasSetAtleastOneValue = false;
        for (int i = 0; i < sets.length; i++) {
            Set set = sets[i];
            String attributeToSet = set.getAttributeToSet(compilationRuntimeContext);  //attempting to get the computed value of attribute
            Object value = set.getValue(compilationRuntimeContext);

            if (value == null && !set.doOutputNullValue()) {
                continue;
            }

            boolean hasSetThisValue = false;

            boolean serializeKey = childSerializationPolicy == SerializationPolicy.ALL ||
                                   childSerializationPolicy == SerializationPolicy.ONLYKEY;
            boolean serializeValue = childSerializationPolicy == SerializationPolicy.ALL ||
                                     childSerializationPolicy == SerializationPolicy.ONLYVALUE;
            hasSetThisValue = serializeKey;  //if the key is to be serialized then some output will definitely
                                             //be generated
            if (serializeKey) {
                strBuilder.append(QUOTATION_MARK);
                strBuilder.append(attributeToSet);
            }

            if (childSerializationPolicy == SerializationPolicy.ONLYKEY) {
                strBuilder.append(QUOTATION_MARK);  //let's close the quotes enclosing the key
            }

            if (childSerializationPolicy == SerializationPolicy.ALL) {
                //key would have already been serialized. Let's add necessary separators (colon or equals sign)
                //before we attempt serialization of value.
                strBuilder.append(isGroupTypeList ? "=" : QUOTATION_MARK + ":");
            }

            if (serializeValue) {
                if (value != null) {
                    strBuilder.append(isGroupTypeList ?
                                      (!serializeKey ? QUOTATION_MARK : "") :  //if no key was serialized then also we need
                                                                               //to start the value with a quote or else the
                                                                               //generated json would be malformed
                                      QUOTATION_MARK);
                    strBuilder.append(value);
                    strBuilder.append(QUOTATION_MARK);

                    hasSetThisValue = true;  //some output is definitely generated

                } else if (serializeKey) {  //only if the key was serialized it makes sense to
                                            //accommodate null value
                    strBuilder.append(isGroupTypeList ? "" : "null");
                }
            }

            if (hasSetThisValue && i != sets.length - 1) {
                strBuilder.append(",");
            }

            hasSetAtleastOneValue = hasSetAtleastOneValue || hasSetThisValue;  //let's re-evaluate
                                                                               //'hasSetAtleastOneValue' so that it
                                                                               //can be assigned a true value (if that
                                                                               //has not yet happened owing to the
                                                                               //control exercised by
                                                                               //childSerializationPolicy).
        }

        if (strBuilder.length() > 0 && ',' == strBuilder.charAt(strBuilder.length() - 1)) {
            //if there is a residual comma left after serializing all Set CUs (owing to the control exercised by
            //the childSerializationPolicy) then we need to trim it
            strBuilder.deleteCharAt(strBuilder.length() - 1);
        }

        Group[] groups = groupToSerialize.getChildren(Group.class);

        if (hasSetAtleastOneValue && groups.length != 0) {
            strBuilder.append(",");
        }

        for (int i = 0; i < groups.length; i++) {
            Group group = groups[i];

            Object groupAsJsonTmp = "";
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

                groupAsJsonTmp = group.build(compilationRuntimeContext, ReturnType.JSON);  //serialize the group
            } finally {
                //reset the runtime serialization policies of the child group to its original ones to ensure there
                //is no side effects on its future serialization that might take place in a different context (e.g.
                //attempt to find and serialize this child group independently).
                group.setSelfSerializationPolicyRuntime(group.getSelfSerializationPolicy());
                group.setChildSerializationPolicyRuntime(group.getChildSerializationPolicy());
            }
            strBuilder.append(groupAsJsonTmp);
            if (i != groups.length - 1 && !"".equals(groupAsJsonTmp)) {
                strBuilder.append(",");
            }
        }
        
        if (strBuilder.length() > 0 && ',' == strBuilder.charAt(strBuilder.length() - 1)) {
            //if there is a residual comma left after serializing all Set CUs (owing to the control exercised by
            //the childSerializationPolicy on group serialization in the above block) then we need to trim it
            strBuilder.deleteCharAt(strBuilder.length() - 1);
        }
    }
}
