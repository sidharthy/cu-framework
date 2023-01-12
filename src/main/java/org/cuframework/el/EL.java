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

package org.cuframework.el;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.core.CompilationUnits;
import org.cuframework.core.CompilationUnits.ICompilationUnit;
import org.cuframework.core.CompilationUnits.IEvaluable;
import org.cuframework.func.IFunction;
import org.cuframework.util.UtilityFunctions;

import org.w3c.dom.Node;

/**
 * EL class for processing cu expressions.
 * @author Sidharth Yadav
 *
 */
public class EL {
    private static final String FN_PARAMS_DELIMITER = ",";  //function params to be separated by commas

    //private static final String DOLLAR_REGEX = "(?:(?<!\\\\|\\$)(\\${3}(([\\w-]+)?:)?([\\w-]+)\\(([^\\)]*)\\);?|\\${2}(([\\w-]+)(\\.[\\w-]+)*(\\[[0-9]*\\])*);?|(?:\\$xpath|\\$)\\(([^\\)]*)\\);?|\\${1}(?=[\\w-:])((([\\w-]+)?:)?(([\\w-]+)(\\.[\\w-]+)*(\\[[0-9]*\\])*)?);?))";  //this would not match $:[]; but would match $:;
    //private static final String DOLLAR_REGEX = "(?:(?<!\\\\|\\$)(\\${3}(([\\w-]+)?:)?([\\w-]+)\\(([^\\)]*)\\);?|\\${2}(([\\w-]+)(\\.[\\w-]+)*(\\[[0-9]*\\])*);?|(?:\\$xpath|\\$)\\(([^\\)]*)\\);?|\\${1}(?=[\\w-:])((([\\w-]+)?:)?((?:([\\w-]+)(\\.[\\w-]+)*)?(\\[[0-9]*\\])*)?);?))";  //this would also match $:[]; along with $:; but could generate accessor hierarchy as an empty string.
    private static final String DOLLAR_REGEX = "(?:(?<!\\\\|\\$)(\\${3}(([\\w-]+)?:)?([\\w-]+)\\(([^\\)]*)\\);?|\\${2}(([\\w-]+)(\\.[\\w-]+)*(\\[[0-9]*\\])*);?|(?:\\$xpath|\\$)\\(([^\\)]*)\\);?|\\${1}(?=[\\w-:])((([\\w-]+)?:)?(?:((?=[\\w-]|(?:\\[\\]))([\\w-]+)?(\\.[\\w-]+)*(\\[[0-9]*\\])*)?));?))";  //this would also match $:[]; along with $:; and always null for missing accessor hierarchy.
    private static final String ATR_REGEX = "(?:(?<!\\\\)@(?=[\\w-\\|.:])(([\\w-.:]+)?(\\|(r|c|x|n|N)?)?);?)";
    private static final String DOLLAR_AND_ATR_REGEX = DOLLAR_REGEX + "|" + ATR_REGEX;

    public static Expression parse(String input) {
        return toSingularExpression(getTokens(input));
    }

    private static Expression toSingularExpression(List<Expression> exprs) {
        Expression expression = null;
        if (exprs.size() > 1) {
            GroupOfExpressions goe = new GroupOfExpressions("");
            goe.addAll(exprs);
            expression = goe;
        } else if (exprs.size() == 1){
            expression = exprs.get(0);
        }
        return expression;
    }

    private static List<Expression> getTokens(String input) {
        Pattern pattern = Pattern.compile(DOLLAR_AND_ATR_REGEX);
        Matcher m = pattern.matcher(input);
        int collectionEndIndex = 0;
        List<Expression> tokensList = new LinkedList<>();
        while (m.find()) {
            MatchResult mr = m.toMatchResult();
            int mrStart = mr.start();
            int mrEnd = mr.end();
            String mrGroup = mr.group();
            if (collectionEndIndex < mrStart) {
                PlainText pt = new PlainText(input.substring(collectionEndIndex, mrStart));
                tokensList.add(pt);
            } else if (collectionEndIndex > mrStart) {
                continue;  //probably there was some function definition, collection of whose boundaries resulted in collection of
                           //some of the tokens that the regex believed were out of the parenthesis scope (this can happen when there
                           //are nested function calls and regex fails to find proper function boundaries in that scenario). Let's just
                           //ignore the current group as it would have already been collected as part of param string of some function call.
            }
            if (mrGroup.startsWith("$$$")) {
                int[] balancedParenthesis = getBalancedParenthesisBondaries(input, mrStart, '(', ')');
                int openingParenthesisIndex = balancedParenthesis[0];
                int closingParenthesisIndex = balancedParenthesis[1];
                if (closingParenthesisIndex == -1) {
                    throw new RuntimeException("Misplaced parenthesis starting at index " + openingParenthesisIndex +
                                                                                                  " in the expression " + input);
                }
                int fnCallClosingBoundaryIndex = closingParenthesisIndex + (closingParenthesisIndex != input.length() - 1?
                                                                              (input.charAt(closingParenthesisIndex + 1) == ';'? 2: 1):
                                                                              1);
                String fnParamsString = input.substring(openingParenthesisIndex + 1, closingParenthesisIndex);
                List<Expression> unprocessedParams = getTokens(fnParamsString);
                TripleDollar td = new TripleDollar(input.substring(mrStart, fnCallClosingBoundaryIndex));
                td.setFnNamespaceGroup(mr.group(2));
                td.setFnNamespace(mr.group(3));
                td.setId(mr.group(4));
                td.setParams(unprocessedParams);
                tokensList.add(td);
                collectionEndIndex = fnCallClosingBoundaryIndex;
            } else if (mrGroup.startsWith("$$")) {
                DoubleDollar dd = new DoubleDollar(mrGroup);
                dd.setAccessorHierarchy(mr.group(6));
                dd.setId(mr.group(7));
                tokensList.add(dd);
                collectionEndIndex = mrEnd;
            } else if (mrGroup.startsWith("$(") || mrGroup.startsWith("$xpath(")) {
                int[] balancedParenthesis = getBalancedParenthesisBondaries(input, mrStart, '(', ')');
                int openingParenthesisIndex = balancedParenthesis[0];                             
                int closingParenthesisIndex = balancedParenthesis[1]; 
                if (closingParenthesisIndex == -1) {                                              
                    throw new RuntimeException("Misplaced parenthesis starting at index " + openingParenthesisIndex +
                                                                                                  " in the expression " + input);
                }
                int genericBlockClosingBoundaryIndex = closingParenthesisIndex + (closingParenthesisIndex != input.length() - 1?
                                                                              (input.charAt(closingParenthesisIndex + 1) == ';'? 2: 1):
                                                                              1);
                String associatedExpressionString = input.substring(openingParenthesisIndex + 1, closingParenthesisIndex);
                Expression associatedExpression = toSingularExpression(getTokens(associatedExpressionString));
                Expression gdORxpath = null;
                if (mrGroup.startsWith("$(")) {
                    GenericDollar gd = new GenericDollar(input.substring(mrStart, genericBlockClosingBoundaryIndex));
                    gd.setAssociatedExpression(associatedExpression);
                    gdORxpath = gd;
                } else {
                    XPathExpression xpath = new XPathExpression(input.substring(mrStart, genericBlockClosingBoundaryIndex));
                    xpath.setAssociatedExpression(associatedExpression);
                    gdORxpath = xpath;
                }
                tokensList.add(gdORxpath);
                collectionEndIndex = genericBlockClosingBoundaryIndex;
            } else if (mrGroup.startsWith("$")) {
                SingleDollar sd = new SingleDollar(mrGroup);
                sd.setContainerName(mr.group(13));
                sd.setAccessorHierarchy(mr.group(14));
                sd.setId(mr.group(15));
                tokensList.add(sd);
                collectionEndIndex = mrEnd;
            } else if (mrGroup.startsWith("@")) {
                AtTheRate atr = new AtTheRate(mrGroup);
                atr.setId(mr.group(19));
                atr.setComputationHint(mr.group(21));
                tokensList.add(atr);
                collectionEndIndex = mrEnd;
            }
        }
        if (collectionEndIndex != input.length()) {
            PlainText pt = new PlainText(input.substring(collectionEndIndex, input.length()));
            tokensList.add(pt);
        }
        return tokensList;
    }

    private static int[] getBalancedParenthesisBondaries(String s,
                                                         int startIndex,
                                                         char openingParenthesis,
                                                         char closingParenthesis) {
        int level = 0;
        int openingParenthesisIndex = -1;
        int balancedAtIndex = -1;
        for (int i = startIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == openingParenthesis) {
                level++;
                if (level == 1) {
                    openingParenthesisIndex = i;
                }
            }
            else if (c == closingParenthesis) {
                level--;
                if (level == 0) {
                    balancedAtIndex = i;
                    break;
                }
                if (level < 0) {
                    //found a closing bracket before it was ever opened
                    break;
                }
            }
        }
        return new int[]{openingParenthesisIndex, balancedAtIndex};
    }

    public static abstract class Expression {
        enum Type {
            VAR_VALUE("$"),
            EVALUABLE_VALUE("$$"),
            FUNCTION_CALL("$$$"),
            ATTRIBUTE_VALUE("@"),
            TEXT("text"),
            XPATH_VALUE("$xpath()"),
            GENERIC("$()"),  //this is the most generic but slowest of all other expression types because the
                             //actual expression to be compiled and evaluated gets known only at the runtime.
            GROUP_OF_EXPRESSIONS("goe");

            private String id = null;
            private Type(String id) {
                this.id = id;
            }
            public String getId() {
                return id;
            }
        }
        protected String id = null;
        protected String expression = null;
        protected boolean isDynamic = false;

        public Expression(String rawExpression) {
            this.expression = rawExpression;
        }

        public String getId() {
            return id;
        }

        public String getExpression() {
            return expression;
        }

        public boolean isDynamic() {
             return isDynamic;
        }

        public abstract void setId(String id);
        public abstract Type getType();
        protected abstract Object doGetValue(ExpressionRuntimeContext erc) throws XPathExpressionException;

        public final Object getValue(ExpressionRuntimeContext erc) throws XPathExpressionException {
            return getNullableValue(doGetValue(erc), erc.getCompilationRuntimeContext());
        }

        private Object getNullableValue(Object value, CompilationRuntimeContext compilationRuntimeContext) {
            if (value == null) {
                Map<String, Object> immutableInternalContext = compilationRuntimeContext.getImmutableInternalContext();
                String[] nullReplacementLookups = getNullReplacementLookups();
                if (nullReplacementLookups != null && immutableInternalContext != null) {
                    for (String lookupKey: nullReplacementLookups) {
                        if (immutableInternalContext.containsKey(lookupKey)) {
                            value = immutableInternalContext.get(lookupKey);
                            break;
                        }
                    }
                }
            }
            return value;
        }

        private String[] getNullReplacementLookups() {
            String EVAL = "eval";
            String NULL = "null";
            Type type = getType();
            if (type == null) {
                return new String[0];
            }
            boolean idApplicable = getId() != null &&
                                   (type == Type.VAR_VALUE || type == Type.EVALUABLE_VALUE ||
                                    type == Type.FUNCTION_CALL || type == Type.ATTRIBUTE_VALUE);
            return idApplicable ? new String[]{EVAL + "." + getType().getId() + "." + getId() + "." + NULL,
                                               EVAL + "." + getType().getId() + ".*." + NULL} :
                                  new String[]{EVAL + "." + getType().getId() + ".*." + NULL};
        }

        public String toString() {
            return getClass().getName() + ":" + expression;
        }
    }

    private static class PlainText extends Expression {

        public PlainText(String rawExpression) {
            super(rawExpression);
            isDynamic = false;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Type getType() {
            return Type.TEXT;
        }

        @Override
        protected Object doGetValue(ExpressionRuntimeContext erc)
                                                        throws XPathExpressionException {
            return expression;
        }
    }

    private static class SingleDollar extends Expression {
        private String containerName = null;  //name of the containing map. e.g. 'CONTEXT-MAP in $CONTEXT-MAP:var.field[]'
                                              //If null, the var would be looked for inside the internal map.
        private String accessorHierarchy = null;  //var accessor hierarchy e.g. 'var.field[] in $CONTEXT-MAP:var.field[]'

        public SingleDollar(String rawExpression) {
            super(rawExpression);
            isDynamic = true;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Type getType() {
            return Type.VAR_VALUE;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setAccessorHierarchy(String accessorHierarchy) {
            this.accessorHierarchy = accessorHierarchy;
        }

        public String getAccessorHierarchy() {
            return accessorHierarchy;
        }

        @Override
        protected Object doGetValue(ExpressionRuntimeContext erc)
                                                        throws XPathExpressionException {
            CompilationRuntimeContext compilationRuntimeContext = erc.getCompilationRuntimeContext();
            Object thisValue = erc.getAdditionalContext(ExpressionRuntimeContext.ADDITIONAL_CONTEXT_THIS_VALUE);

            Map<String, Object> varContainer = containerName == null?
                                                  compilationRuntimeContext.getImmutableInternalContext():
                                                  compilationRuntimeContext.getExternalContext() == null?
                                                       null:
                                                       compilationRuntimeContext.getExternalContext().getMap(containerName);

            Object value = null;
            if (accessorHierarchy == null) {
                value = varContainer;
            } else {
                value = UtilityFunctions.getValue(accessorHierarchy, varContainer);
                if (value == null && "this".equals(accessorHierarchy)) {  //Even if the token was $this first preference is given
                                                                          //to resolving it through context map lookups. If that
                                                                          //returns a null value it would be considered for
                                                                          //assigning the value of 'thisValue'.
                                                                          //Also for now, we will use the 'thisValue' available in
                                                                          //the additional context only if the complete accessor
                                                                          //hierarchy only contained the word 'this' without
                                                                          //additional key or array accessors (e.g this.f[4] etc)
                    value = thisValue;
                }
            }
            return value;
        }
    }

    private static class DoubleDollar extends Expression {
        private String accessorHierarchy = null;  //cuid accessor hierarchy e.g. 'cuid.field[] in $$cuid.field[]'

        public DoubleDollar(String rawExpression) {
            super(rawExpression);
            isDynamic = true;
        }

        @Override
        public void setId(String id) {
            if (id == null || "".equals(id.trim())) {
                throw new IllegalArgumentException("CU id cannot be null");
            }
            this.id = id;
        }

        @Override
        public Type getType() {
            return Type.EVALUABLE_VALUE;
        }

        public void setAccessorHierarchy(String accessorHierarchy) {
            this.accessorHierarchy = accessorHierarchy;
        }

        public String getAccessorHierarchy() {
            return accessorHierarchy;
        }

        @Override
        protected Object doGetValue(ExpressionRuntimeContext erc)
                                                        throws XPathExpressionException {
            ICompilationUnit cu = erc.getCompilationUnit();
            CompilationRuntimeContext compilationRuntimeContext = erc.getCompilationRuntimeContext();

            boolean accessorHierarchyIsPresent = id != null &&
                                                 accessorHierarchy != null &&
                                                 !id.equals(accessorHierarchy);
            ICompilationUnit childCU = cu.getChild(id);
            Object _value = null;
            if (childCU instanceof IEvaluable) {
                _value = ((IEvaluable) childCU).getValue(compilationRuntimeContext);
                if (_value != null && accessorHierarchyIsPresent) {
                    Map<String, Object> containerMap = new HashMap<>();
                    containerMap.put(id, _value);
                    _value = UtilityFunctions.getValue(accessorHierarchy, containerMap);
                }
            }
            return _value;
        }
    }

    private static class TripleDollar extends Expression {
        private String fnNamespaceGroupWithColon = null;
        private String namespace = null;
        private List<Expression> paramsAsExpressions = new LinkedList<>();

        public TripleDollar(String rawExpression) {
            super(rawExpression);
            isDynamic = true;
        }

        @Override
        public void setId(String id) {
            if (id == null || "".equals(id.trim())) {
                throw new IllegalArgumentException("Function id cannot be null");
            }
            this.id = id;
        }

        @Override
        public Type getType() {
            return Type.FUNCTION_CALL;
        }

        public void setFnNamespaceGroup(String fnNamespaceGroupWithColon) {
            this.fnNamespaceGroupWithColon = fnNamespaceGroupWithColon;
        }

        public String getFnNamespaceGroup() {
            return fnNamespaceGroupWithColon;
        }

        private boolean isRootNamespaceIndicated() {
            return ":".equals(fnNamespaceGroupWithColon);
        }

        public void setFnNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getFnNamespace() {
            return namespace;
        }

        public void setParams(List<Expression> unprocessedParams) {
            if (unprocessedParams == null) {
                return;
            }
            StringBuilder strBuilder = new StringBuilder();
            List<Expression> hashcodedExpressions = new LinkedList<>();
            for (Expression expr: unprocessedParams) {
                if (expr.getType() == Expression.Type.FUNCTION_CALL) {
                    strBuilder.append(expr.getExpression().hashCode());
                    hashcodedExpressions.add(expr);
                } else {
                    strBuilder.append(expr.getExpression());
                }
            }
            paramsAsExpressions.clear();
            String[] finalParams = strBuilder.toString().split(FN_PARAMS_DELIMITER);
            for (String param: finalParams) {
                for (Expression exprToRestore: hashcodedExpressions) {
                    param = param.replace("" + exprToRestore.getExpression().hashCode(), exprToRestore.getExpression());
                }
                List<Expression> exprs = getTokens(param.trim());  //opting to trim the param value here to ensure that white spaces
                                                                   //before/after the function params separator (comma(,)) doesn't produce
                                                                   //different results (e.g. $$$equals($1,$2) And $$$equals($1, $2) should
                                                                   //be treated the same). If starting/trailing whitespaces are needed to
                                                                   //be part of string then achieve the same by defining a corresponding
                                                                   //variable in internal or external contexts and using the same in the
                                                                   //funtion call e.g. $$$equals($1,$2) And $$$equals($1,$WHITESPACE;$2)
                if (exprs.size() > 0) {
                    paramsAsExpressions.add(toSingularExpression(exprs));
                }
            }
        }

        public List<Expression> getParams() {
            return paramsAsExpressions;
        }

        @Override
        protected Object doGetValue(ExpressionRuntimeContext erc)
                                                        throws XPathExpressionException {
            ICompilationUnit cu = erc.getCompilationUnit();

            String funcId = id;
            if (funcId == null) {
                return null;
            }
            Object[] funcParams = new Object[paramsAsExpressions.size()];
            int index = 0;
            for (Expression expression: paramsAsExpressions) {
                funcParams[index++] = expression.getValue(erc);
            }
            String ns = (namespace == null || "".equals(namespace)) && !isRootNamespaceIndicated()? cu.getNamespaceURI(): namespace;
            IFunction func = CompilationUnits.resolveFunction(ns, funcId);
            try {
                return func == null? null: func.invoke(funcParams, erc.getCompilationRuntimeContext());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class GenericDollar extends Expression {
        private Expression associatedSingularExpression = null;

        public GenericDollar(String rawExpression) {
            super(rawExpression);
            isDynamic = true;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Type getType() {
            return Type.GENERIC;
        }

        public void setAssociatedExpression(Expression associatedSingularExpression) {
            this.associatedSingularExpression = associatedSingularExpression;
        }

        public Expression getAssociatedExpression() {
            return associatedSingularExpression;
        }

        @Override
        protected Object doGetValue(ExpressionRuntimeContext erc)
                                                        throws XPathExpressionException {
            if (associatedSingularExpression == null) {
                return null;
            }
            Object evaluatedAssociatedSingularExpression = associatedSingularExpression.getValue(erc);
            Object value = evaluatedAssociatedSingularExpression;
            if (evaluatedAssociatedSingularExpression instanceof String) {
                String newInputExpression = evaluatedAssociatedSingularExpression.toString();
                Expression expression = toSingularExpression(getTokens(newInputExpression));
                if (expression != null) {
                    value = expression.getValue(erc);
                } else {
                    value = null;
                }
            }
            return value;
        }
    }

    private static class XPathExpression extends Expression {
        private Expression associatedSingularExpression = null;

        public XPathExpression(String rawExpression) {
            super(rawExpression);
            isDynamic = true;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Type getType() {
            return Type.XPATH_VALUE;
        }

        public void setAssociatedExpression(Expression associatedSingularExpression) {
            this.associatedSingularExpression = associatedSingularExpression;
        }

        public Expression getAssociatedExpression() {
            return associatedSingularExpression;
        }

        @Override
        protected Object doGetValue(ExpressionRuntimeContext erc)
                                                        throws XPathExpressionException {
            if (associatedSingularExpression == null) {
                return null;
            }
            Object evaluatedAssociatedSingularExpression = associatedSingularExpression.getValue(erc);
            Object value = evaluatedAssociatedSingularExpression;
            if (evaluatedAssociatedSingularExpression instanceof String) {
                String xpathExpression = evaluatedAssociatedSingularExpression.toString();
                Object node = erc.getAdditionalContext(ExpressionRuntimeContext.ADDITIONAL_CONTEXT_NODE);
                Object xpath = erc.getAdditionalContext(ExpressionRuntimeContext.ADDITIONAL_CONTEXT_XPATH);
                if (node instanceof Node && xpath instanceof XPath) {
                    value = ((XPath) xpath).evaluate(xpathExpression, (Node) node, XPathConstants.STRING);
                }
            }
            return value;
        }
    }

    private static class AtTheRate extends Expression {
        private String computationHint = null;  //computation hint: r = return raw attribute value, c = return computed value,
                                                //                  x = compute the value and return tokens delimited using attribute tokenizer,
                                                //                  n = is native attribute, N = is non-native attribute
                                                //If null, r would be used as the default treatment when applicable.

        public AtTheRate(String rawExpression) {
            super(rawExpression);
            isDynamic = true;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Type getType() {
            return Type.ATTRIBUTE_VALUE;
        }

        public void setComputationHint(String computationHint) {
            this.computationHint = computationHint;
        }

        public String getComputationHint() {
            return computationHint;
        }

        @Override
        protected Object doGetValue(ExpressionRuntimeContext erc)
                                                        throws XPathExpressionException {
            ICompilationUnit cu = erc.getCompilationUnit();
            CompilationRuntimeContext compilationRuntimeContext = erc.getCompilationRuntimeContext();
            

            Object value = null;
            String attributeName = getId();
            if (attributeName != null) {
                String cHint = computationHint == null? "r": computationHint;
                switch(cHint) {
                    case "c": value = cu.getAttribute(attributeName, compilationRuntimeContext); break;
                    case "x": {
                                  value = cu.getAttribute(attributeName, compilationRuntimeContext);
                                  if (value != null) {
                                      //TODO split using attribute's tokenizer
                                  }
                                  break;
                              }
                    case "n": value = cu.isAttributeNative(attributeName); break;
                    case "N": value = !cu.isAttributeNative(attributeName); break;
                    case "r":
                    default: value = cu.getAttribute(attributeName);
                }
            } else {
                value = cu.getAttributeNames();
            }
            return value;
        }
    }

    //getValue(...) method of this class will return the following:
    //  1. If no child expressions exist then null is returned
    //  2. If just one child expression exist then its getValue(...) is returned
    //  3. If multiple child expressions exist then toString() values of all the child
    //     expressions concatenated together is returned.
    private static class GroupOfExpressions extends Expression {
        private List<Expression> expressions = new LinkedList<>();

        public GroupOfExpressions(String rawExpression) {  //for a group rawExpression can mostly be set to ""
            super(rawExpression);
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Type getType() {
            return Type.GROUP_OF_EXPRESSIONS;
        }

        //returns true if added
        public boolean add(Expression expression) {
            if (expression == null) {
                return false;
            }
            this.expressions.add(expression);
            isDynamic = isDynamic | expression.isDynamic();
            return true;
        }

        public void addAll(List<Expression> expressions) {
            if (expressions == null) {
                return;
            }
            for (Expression expression: expressions) {
                add(expression);
            }
        }

        public void setAll(List<Expression> expressions) {
            isDynamic = false;
            if (expressions == null) {
                this.expressions.clear();
                return;
            }
            this.expressions.clear();
            for (Expression expression: expressions) {
                add(expression);
            }
        }

        public List<Expression> getExpressions() {
            return expressions;
        }

        @Override
        protected Object doGetValue(ExpressionRuntimeContext erc)
                                                        throws XPathExpressionException {
            if (expressions.size() == 0) {
                return null;
            } else if (expressions.size() == 1) {
                return expressions.get(0).getValue(erc);
            }

            StringBuilder strBuilder = new StringBuilder();
            for (Expression expression: expressions) {
                Object value = expression.getValue(erc);
                strBuilder.append(value);
            }
            return strBuilder.toString();
        }
    }
}
