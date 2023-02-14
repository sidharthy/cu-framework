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

package org.cuframework.serializer;

import javax.xml.xpath.XPathExpressionException;

import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.core.CompilationUnits;
import org.cuframework.core.CompilationUnits.CompilationUnit;
import org.cuframework.core.CompilationUnits.Group;
import org.cuframework.core.CompilationUnits.Group.GroupType;
import org.cuframework.core.CompilationUnits.Group.SerializationPolicy;
import org.cuframework.core.CompilationUnits.ICompilationUnit;
import org.cuframework.core.CompilationUnits.IEvaluable;
import org.cuframework.core.CompilationUnits.ISatisfiable;
import org.cuframework.core.CompilationUnits.Set;

/**
 * Source serializer for CompilationUnits.Group.
 * @author Sidharth Yadav
 *
 */
class SourceSerializerForGroupCU implements ICompilationUnitSerializer {

    @Override
    public Object serialize(CompilationRuntimeContext compilationRuntimeContext,
                                CompilationUnit groupCU)
                                    throws XPathExpressionException {
        if (!(groupCU instanceof Group)) {
            throw new IllegalArgumentException("The serializer expects a CompilationUnits.Group object");
        }
        //try {
            return buildSource(compilationRuntimeContext, (Group) groupCU);
        //} catch (XPathExpressionException e) {
        //    throw new CompilationUnitSerializationException(e);  //TODO throw this custom exception.
        //}
    }

    private String buildSource(CompilationRuntimeContext compilationRuntimeContext, Group groupToSerialize)
                                                                            throws XPathExpressionException {
        SerializationPolicy selfSerializationPolicy = groupToSerialize.getSelfSerializationPolicyRuntime();
        SerializationPolicy childSerializationPolicy = groupToSerialize.getChildSerializationPolicyRuntime();

        if (selfSerializationPolicy == SerializationPolicy.NONE) {
            return "";  //we will not attempt serialization on this group at all
        }

        boolean isGroupTypeList = groupToSerialize.getType() == GroupType.LIST;
        StringBuilder strBuilder = new StringBuilder();

        if (!groupToSerialize.isHeadless() &&
            selfSerializationPolicy != SerializationPolicy.ONLYVALUE) {  //don't need to serialize the group prologue if it is either
                                                                         //a headless group or if it's selfSerializationPolicy is set
                                                                         //to 'value' only.
            serializeGroupPrologue(groupToSerialize, strBuilder, compilationRuntimeContext);
        }
        serializeChildrenOfGroupAsSource(groupToSerialize, isGroupTypeList, childSerializationPolicy,
                                         strBuilder, compilationRuntimeContext);
        if (!groupToSerialize.isHeadless() &&
            selfSerializationPolicy != SerializationPolicy.ONLYVALUE) {  //don't need to serialize the group epilogue if it is either
                                                                         //a headless group or if it's selfSerializationPolicy is set
                                                                         //to 'value' only.
            serializeGroupEpilogue(groupToSerialize, strBuilder, compilationRuntimeContext);
        }

        return strBuilder.toString();
    }

    private void serializeChildrenOfGroupAsSource(Group groupToSerialize,
                                                  boolean isGroupTypeList,
                                                  SerializationPolicy childSerializationPolicy,
                                                  StringBuilder strBuilder,
                                                  CompilationRuntimeContext compilationRuntimeContext)
                                                                                  throws XPathExpressionException {
        if (childSerializationPolicy == SerializationPolicy.NONE) {
            return;
        }
        String QUOTATION_MARK = groupToSerialize.getQuotationMarks();
        CompilationUnits.ICompilationUnit[] children = groupToSerialize.getChildren(CompilationUnits.ICompilationUnit.class);
        for (int i = 0; i < children.length; i++) {
            ICompilationUnit child = children[i];
            boolean hasSetThisValue = false;
            if (child instanceof ISatisfiable && !((ISatisfiable) child).satisfies(compilationRuntimeContext)) {
                continue;  //the on condition didn't satisfy and serialization shouldn't be attempted for this cu.
            }
            if (child instanceof Set) {
                Set setToSerialize = (Set) child;
                String attributeToSet = setToSerialize.getAttributeToSet(compilationRuntimeContext);  //attempting to get the computed value of attribute
                Object value = setToSerialize.getValue(compilationRuntimeContext);
                hasSetThisValue = serializeEvaluableOfGroup(attributeToSet,
                                                            value,
                                                            setToSerialize.doOutputNullValue(),
                                                            QUOTATION_MARK,
                                                            isGroupTypeList,
                                                            childSerializationPolicy,
                                                            strBuilder,
                                                            compilationRuntimeContext);
            } else if (child instanceof Group) {
                hasSetThisValue = serializeSubgroupOfGroupAsSource((Group) child,
                                                                 childSerializationPolicy,
                                                                 strBuilder,
                                                                 compilationRuntimeContext);
            } else if (child instanceof IEvaluable) {
                IEvaluable evaluable = (IEvaluable) child;
                String key = evaluable.getIdOrElse(compilationRuntimeContext);
                Object value = evaluable.getValue(compilationRuntimeContext);
                hasSetThisValue = serializeEvaluableOfGroup(key,
                                                            value,
                                                            evaluable.doOutputNullValue(),
                                                            QUOTATION_MARK,
                                                            isGroupTypeList,
                                                            childSerializationPolicy,
                                                            strBuilder,
                                                            compilationRuntimeContext);
            }
        }
    }

    //returns true iff a value was serialized and appended to the strBuilder passed as param.
    private boolean serializeEvaluableOfGroup(String attributeToSet,
                                              Object value,
                                              boolean outputNullValue,
                                              String QUOTATION_MARK,
                                              boolean isParentGroupTypeList,
                                              SerializationPolicy childSerializationPolicyOfParentGroup,
                                              StringBuilder strBuilder,
                                              CompilationRuntimeContext compilationRuntimeContext)
                                                                              throws XPathExpressionException {
        if (value == null && !outputNullValue) {
            return false;
        }

        boolean serializeKeyOnly = childSerializationPolicyOfParentGroup == SerializationPolicy.ONLYKEY;
        if (serializeKeyOnly) {
            //strBuilder.append(QUOTATION_MARK);
            strBuilder.append(attributeToSet);
            //strBuilder.append(QUOTATION_MARK);
        } else {
            //strBuilder.append(QUOTATION_MARK);
            strBuilder.append(value);
            //strBuilder.append(QUOTATION_MARK);
        }

        return true;
    }

    //returns true iff a value was serialized and appended to the strBuilder passed as param.
    private boolean serializeSubgroupOfGroupAsSource(Group subgroupToSerialize,
                                                     SerializationPolicy childSerializationPolicyOfParentGroup,
                                                     StringBuilder strBuilder,
                                                     CompilationRuntimeContext compilationRuntimeContext)
                                                                                     throws XPathExpressionException {
        Object groupAsSourceTmp = "";
        try {
            /********************************************************************************************/
            //adjust serialization policies of child group according to the policies of the parent group.
            if (childSerializationPolicyOfParentGroup != SerializationPolicy.ALL) {
                subgroupToSerialize.setSelfSerializationPolicyRuntime(childSerializationPolicyOfParentGroup);
            } else {
                //if the child serialization policy of the current group is ALL then we don't want
                //to make any change to the self serialization policy of the child group.
                subgroupToSerialize.setSelfSerializationPolicyRuntime(subgroupToSerialize.getSelfSerializationPolicy());
            }
            /********************************************************************************************/

            groupAsSourceTmp = subgroupToSerialize.build(compilationRuntimeContext, "source");  //serialize the group
        } finally {
            //reset the runtime serialization policies of the child group to its original ones to ensure there
            //is no side effects on its future serialization that might take place in a different context (e.g.
            //attempt to find and serialize this child group independently).
            subgroupToSerialize.setSelfSerializationPolicyRuntime(subgroupToSerialize.getSelfSerializationPolicy());
            subgroupToSerialize.setChildSerializationPolicyRuntime(subgroupToSerialize.getChildSerializationPolicy());
        }

        if (groupAsSourceTmp == null &&  //this can heppen, e.g. in cases like when the selfSerializationPolicy
                                         //and childSerializationPolicy of the group were both set
                                         //to 'value' and the child evaluated to a null value.
            !subgroupToSerialize.doOutputNullValue()) {
            groupAsSourceTmp = "";  //let's assign it an empty string so effectively nothing gets written to the strBuilder.
        }

        strBuilder.append(groupAsSourceTmp);
        return !"".equals(groupAsSourceTmp);
    }

    private void serializeGroupPrologue(Group group,
                                        StringBuilder strBuilder,
                                        CompilationRuntimeContext compilationRuntimeContext)
                                                                        throws XPathExpressionException {
        String nodeName = getSerializableNodeName(group, compilationRuntimeContext);
        java.util.Set<String> attributeNames = group.getAttributeNames();
        strBuilder.append("<");
        strBuilder.append(nodeName);
        for (String attr: attributeNames) {
            if (!isGenericAttribute(attr) &&
                group.isAttributeNative(attr)) {  //we don't want to serialize the non-generic cu's native attributes
                continue;
            }
            String attrValue = group.getAttribute(attr, compilationRuntimeContext);  //using computed version of attribute
            if (attrValue != null) {
                strBuilder.append(" ");
                strBuilder.append(attr);
                strBuilder.append("=");
                strBuilder.append("\"" + attrValue.replaceAll("\"", "\\\"") + "\"");
            }
        }
        strBuilder.append(">");
    }

    private void serializeGroupEpilogue(Group group,
                                        StringBuilder strBuilder,
                                        CompilationRuntimeContext compilationRuntimeContext) 
                                                                        throws XPathExpressionException {
        String nodeName = getSerializableNodeName(group, compilationRuntimeContext);
        strBuilder.append("</");
        strBuilder.append(nodeName);
        strBuilder.append(">");
    }

    private String getSerializableNodeName(Group g, CompilationRuntimeContext compilationRuntimeContext)
                                                                                    throws XPathExpressionException {
        String serializableNodeName = g.getSerializableNodeName(compilationRuntimeContext);
        return serializableNodeName == null? g.getNodeName(): serializableNodeName;
    }

    private boolean isGenericAttribute(String attr) {
        String[] genericAttrs = {"id", "name", "type"};
        for (String genericAttr: genericAttrs) {
            if (genericAttr.equalsIgnoreCase(attr)) {
                return true;
            }
        }
        return false;
    }
}
