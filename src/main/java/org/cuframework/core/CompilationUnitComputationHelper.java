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

package org.cuframework.core;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.core.CompilationUnits.ICompilationUnit;
import org.cuframework.config.ConfigManager;
import org.cuframework.el.EL;
import org.cuframework.el.EL.Expression;
import org.cuframework.el.ExpressionRuntimeContext;

/**
 * Compilation Unit computation helper class - placeholder for attribute and node text expression computation methods.
 * Note: This class should be completely stateless and not hold any external state.
 *       Any state that it wants to maintain should be autonomous and built automatically by itself.
 * @author Sidharth Yadav
 *
 */
final class CompilationUnitComputationHelper {

    public static final String VIRTUAL_ATTRIBUTE_ID_OR_ELSE = "idorelse";  //defining a virtual attribute for use with the computed version of the getIdOrElse method
                                                                           //of any cu. Use it to define the related attribute's config at node, namespace and system level.

    private static final String DOT = ".";
    private static final String ATTR_TOKENIZER_KEY = "tokenizer";  //attribute suffix: used as value tokenizer during computation.
    private static final String ATTR_EXPRESSION_KEY = "expression";

    private ICompilationUnit boundCU = null;
    private Map<String, Object> autonomousCache = null;

    private CompilationUnitComputationHelper(ICompilationUnit cu) {
        if (cu == null) {
            throw new IllegalArgumentException("A containing cu instance is required by the helper class " + getClass().getName());
        }
        this.boundCU = cu;
    }

    private ICompilationUnit _boundCU() {
        return boundCU;
    }

    private Map<String, Object> _autonomousCache() {
        if (autonomousCache == null) {
            autonomousCache = new HashMap<>();
        }
        return autonomousCache;
    }

    private String getAttributeValueTokenizer(String attr) {
        ICompilationUnit _boundCU = _boundCU();
        String lookupKey = (attr + DOT + ATTR_TOKENIZER_KEY).toLowerCase();  //converting to lowercase to ignore the case anomalies in defining the
                                                                             //attribute names in the cu template xml. We are however not doing any
                                                                             //such conversion to lowercase when initializing the config values from
                                                                             //the namespace (or system properties) definition so make sure to define
                                                                             //the config keys already in lowercase in the respective definition block(s).
        Map<String, Object> autonomousCache = _autonomousCache();
        boolean cacheContainsTheKey = autonomousCache.containsKey(lookupKey);
        Object attributeValueTokenizer = autonomousCache.get(lookupKey);
        if (!cacheContainsTheKey) {
            attributeValueTokenizer = ConfigManager.getInstance().getMetadataValue(_boundCU.getNodeName(),
                                                                                   _boundCU.getTagName(),
                                                                                   _boundCU.getNamespaceURI(),
                                                                                   null,
                                                                                   lookupKey);
            autonomousCache.put(lookupKey, attributeValueTokenizer);  //let's put the value anyway inside cache (even if its null) to
                                                                      //avoid doing config manager's hierarchical lookup again. If it wasn't
                                                                      //found this time, it won't be found in subsequent lookups as well.
        }
        return attributeValueTokenizer == null? null: attributeValueTokenizer.toString();
    }

    private Expression getAttributeValueExpression(String attr, String value) {
        if (value == null) {
            return null;
        }
        String lookupKey = (attr + DOT + ATTR_EXPRESSION_KEY);
        Map<String, Object> autonomousCache = _autonomousCache();
        boolean cacheContainsTheKey = autonomousCache.containsKey(lookupKey);      
        Object attributeValueExpression = autonomousCache.get(lookupKey);
        if (!cacheContainsTheKey) { 
            attributeValueExpression = EL.parse(value);
            autonomousCache.put(lookupKey, attributeValueExpression);  //let's put the value anyway inside cache (even if its null) to 
                                                                       //avoid doing EL parsing again. If it wasn't successful this time,
                                                                       //it won't be in subsequent attempts as well.
        }
        return (Expression) attributeValueExpression;
    }

    public static CompilationUnitComputationHelper instance(ICompilationUnit cu) {
        return new CompilationUnitComputationHelper(cu);
    }

    /**
        This method computes the attribute value using the following scheme:
        1. If the attribute value contains dynamic elements that makes it eligible for computation then an attempt
           to resolve the computed value is made, and, on success, the computed value is returned.
        2. If the attribute value either doesn't contain dynamic elements that makes it eligible for computation or
           if the computation didn't successfully resolve to a value then the passed attribute value gets returned as is.
     */
    protected String computeAttributeValue(String attributeName,  //basically used for metadata lookup purposes
                                           String attributeValue,  //computation happens using this as the evaluation expression
                                           CompilationRuntimeContext compilationRuntimeContext)
                                                                      throws XPathExpressionException {
        //return (String) computeAttributeValue(attributeValue, compilationRuntimeContext, true, false);
        Object computedAttributeValue = _computeAttributeValue(attributeName, attributeValue, compilationRuntimeContext, true, false);
        return computedAttributeValue != null?
                          computedAttributeValue.toString():
                          attributeValue;   //if the computation resulted in a null value then we
                                            //would return the attribute's value expression as is.
    }

    private Object _computeAttributeValue(String attributeName,
                                          String attributeValue,
                                          CompilationRuntimeContext compilationRuntimeContext,
                                          boolean returnValueAsString,
                                          boolean acceptNullComputationValue)
                                                                      throws XPathExpressionException {
        Object computedAttributeValue = attributeValue;
        Expression attributeValueAsExpression = getAttributeValueExpression(attributeName, attributeValue);
        if (attributeValueAsExpression != null) {
            Object _value = attributeValueAsExpression.
                                  getValue(ExpressionRuntimeContext.newInstance(_boundCU(), compilationRuntimeContext));
            if (_value != null) {
                computedAttributeValue = returnValueAsString? _value.toString(): _value;
            } else if (acceptNullComputationValue) {
                computedAttributeValue = null;
            }
        }
        return computedAttributeValue;
    }

    public boolean isAttributeDynamic(String key, String rawAttributeValue) {
        Expression attributeValueAsExpression = getAttributeValueExpression(key, rawAttributeValue);
        return attributeValueAsExpression == null? false: attributeValueAsExpression.isDynamic();
    }
}
