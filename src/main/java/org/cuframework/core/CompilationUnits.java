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

package org.cuframework.core;

import org.cuframework.MapOfMaps;
import org.cuframework.serializer.CompilationUnitsSerializationFactory;
import org.cuframework.util.cu.LoadProperties;
import org.cuframework.util.UtilityFunctions;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Core Compilation Units.
 * @author Sidharth Yadav
 *
 */
public final class CompilationUnits {

    private CompilationUnits() {
    }

    private static final String CORE_CUs_CONTAINER = "core-units";
    private static final String MORE_CUs_CONTAINER = "more-units";
    private static final java.util.Map<String, java.util.Map<String, Class<? extends ICompilationUnit>>> TAG_TO_UNIT_MAPPING =
                                                             new HashMap<String, java.util.Map<String, Class<? extends ICompilationUnit>>>();
    static {
        java.util.Map<String, Class<? extends ICompilationUnit>> coreCUs = new HashMap<String, Class<? extends ICompilationUnit>>();
        coreCUs.put(Conditional.TAG_NAME, Conditional.class);
        coreCUs.put(Condition.TAG_NAME, Condition.class);
        coreCUs.put(ValueOf.TAG_NAME, ValueOf.class);
        coreCUs.put(TypeOf.TAG_NAME, TypeOf.class);
        coreCUs.put(Map.TAG_NAME, Map.class);
        coreCUs.put(InternalMap.TAG_NAME, InternalMap.class);
        coreCUs.put(Json.TAG_NAME, Json.class);
        coreCUs.put(On.TAG_NAME, On.class);
        coreCUs.put(Init.TAG_NAME, Init.class);
        coreCUs.put(Finally.TAG_NAME, Finally.class);
        coreCUs.put(Using.TAG_NAME, Using.class);
        coreCUs.put(Group.TAG_NAME, Group.class);
        coreCUs.put(HeadlessGroup.TAG_NAME, HeadlessGroup.class);
        coreCUs.put(ExecutableGroup.TAG_NAME, ExecutableGroup.class);
        coreCUs.put(HeadlessExecutableGroup.TAG_NAME, HeadlessExecutableGroup.class);
        coreCUs.put(Select.TAG_NAME, Select.class);
        coreCUs.put(Extends.TAG_NAME, Extends.class);
        coreCUs.put(Set.TAG_NAME, Set.class);
        coreCUs.put(Unset.TAG_NAME, Unset.class);
        coreCUs.put(Get.TAG_NAME, Get.class);
        coreCUs.put(Loop.TAG_NAME, Loop.class);
        coreCUs.put(Loop.Break.TAG_NAME, Loop.Break.class);
        coreCUs.put(Log.TAG_NAME, Log.class);
        coreCUs.put(Assert.TAG_NAME, Assert.class);

        java.util.Map<String, Class<? extends ICompilationUnit>> moreCUs = new HashMap<String, Class<? extends ICompilationUnit>>();
        moreCUs.put(LoadProperties.TAG_NAME, LoadProperties.class);  //adding in more cu map as it is a utility cu and its ok to allow
                                                                     //applications to replace it with their own implementations.

        TAG_TO_UNIT_MAPPING.put(CORE_CUs_CONTAINER, coreCUs);
        TAG_TO_UNIT_MAPPING.put(MORE_CUs_CONTAINER, moreCUs);
    }

    public static ICompilationUnit getCompilationUnitForTag(String tagName) {
        Class<? extends ICompilationUnit> cu = CompilationUnits.getCompilationClassForTag(tagName);
        if (cu != null) {
            try {
                return cu.newInstance();
            } catch (InstantiationException e) {
                // can be ignored as this wouldn't happen
            } catch (IllegalAccessException e) {
                // can be ignored as this wouldn't happen
            }
        }
        return null;
    }

    public static Class<? extends ICompilationUnit> getCompilationClassForTag(String tagName) {
        if (tagName == null) {
            return null;
        }
        Class<? extends ICompilationUnit> cu = TAG_TO_UNIT_MAPPING.get(CORE_CUs_CONTAINER).get(tagName);
        if (cu == null) {
            //tag name didn't correspond to the core units. Let's now check the more units.
            cu = TAG_TO_UNIT_MAPPING.get(MORE_CUs_CONTAINER).get(tagName);
        }
        return cu;
    }

    public static boolean setCompilationClassForTag(String tagName, String tagClassName) throws ClassNotFoundException, ClassCastException {
        return setCompilationClassForTag(tagName, Class.forName(tagClassName).asSubclass(ICompilationUnit.class));
    }

    public static boolean setCompilationClassForTag(String tagName, Class<? extends ICompilationUnit> tagClass) {
        boolean mapped = false;
        if (tagName != null && tagClass != null) {
            TAG_TO_UNIT_MAPPING.get(MORE_CUs_CONTAINER).put(tagName, tagClass);
            mapped = true;
        }
        return mapped;
    }

    //This method can unset only 'more' units and not the 'core' units.
    public static Class<? extends ICompilationUnit> unsetCompilationUnitForTag(String tagName) {
        Class<? extends ICompilationUnit> removedUnit = null;
        if (tagName != null) {
            removedUnit = TAG_TO_UNIT_MAPPING.get(MORE_CUs_CONTAINER).remove(tagName);
        }
        return removedUnit;
    }

    public static boolean isAssignableFrom(Class<? extends ICompilationUnit> intendedCUClass, Class<? extends ICompilationUnit> tagCUClass) {
        boolean assignable = false;
        if (intendedCUClass != null && tagCUClass != null) {
            assignable = intendedCUClass.isAssignableFrom(tagCUClass);
        }
        return assignable;
    }

    public static XPath getXPath() {
        return XPathFactory.newInstance().newXPath();
    }

    private static final XPath XPATH = getXPath();

    public interface ICompilationUnit {
        static final String ATTRIBUTE_ID = "id";

        String getAttribute(String key);
        String getId();
        String getIdOrElse();
        String getNamespaceURI();
        boolean matchesIdOrElse(String idOrElse);
        void compile(Node n) throws XPathExpressionException;
        ICompilationUnit getChild(String idOrElseOfChild);
        <T extends ICompilationUnit> T[] getChildren(Class<T> type);
    }

    public abstract static class CompilationUnit implements ICompilationUnit {
        private Properties attributes = new Properties();
        private Node nodeContext = null;

        private static enum AttributeType {
            STATIC,  //attribute value is static and is to be used as is.
            DYNAMIC_INTERNALCONTEXT_DEPENDANT,  //attribute value is dynamic and is to be resolved using the internal context map.
            DYNAMIC_CHILD_DEPENDANT  //attribute value is dynamic and is to be resolved to one of its child's value (ofcource if the child is of type IEvaluable)
        }

        //define the attributes available for all compilation units.
        //private static final String ATTRIBUTE_ID = "id";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_ID};

        private static final String IMPLICIT_ATTRIBUTE_NAMESPACE_URI = "--[namespace-uri]--";

        Node getNodeContext() {
            return this.nodeContext;
        }

        void setNodeContext(Node n) {
            this.nodeContext = n;
        }

        public String getNamespaceURI() {
            return getAttribute(IMPLICIT_ATTRIBUTE_NAMESPACE_URI);
        }

        static String getAttributeValueIffAttributeIsDefined(String attribute, Node n) throws XPathExpressionException {
            Node attribNode = (Node) CompilationUnits.XPATH.evaluate(attribute, n, XPathConstants.NODE);
            if (attribNode != null) {
                return attribNode.getNodeValue();
            }
            return null;
        }

        void setAttribute(String key, String value) {
            if (key != null && value != null) {
                attributes.setProperty(key, value);
            }
        }

        void setAttributeIffNew(String key, String value) {
            if (key != null && value != null && !attributes.containsKey(key)) {
                attributes.setProperty(key, value);
            }
        }

        public String getAttribute(String key) {
            return attributes.getProperty(key);
        }

        /******************************************************************************************************************************/
        /************************ Start - New attribute methods added to generically support computed values **************************/
        /******************************************************************************************************************************/
        /******************************************************************************************************************************/
        public String getAttribute(String key, CompilationRuntimeContext compilationRuntimeContext)
                                                                                throws XPathExpressionException {
            return computeAttributeValue(getAttribute(key), compilationRuntimeContext);
        }

        /**
            This method computes the attribute value using the following scheme:
            1. If the attribute value starts with special characters that makes it eligible for computation then an attempt
               to resolve the computed value is made, and, on success, the computed value is returned.
            2. If the attribute value either doesn't start with special characters that makes it eligible for computation or
               if the computation didn't successfully resolve to a value then the passed attribute value gets returned as is.
         */
        protected String computeAttributeValue(String attributeValue, CompilationRuntimeContext compilationRuntimeContext)
                                                                          throws XPathExpressionException {
            AttributeType attributeType = getAttributeTypeFromValue(attributeValue);
            if (CompilationUnit.isAttributeTypeDynamic(attributeType)) {
                //ids starting with '$' has special meaning and needs to be resolved to either the value
                //returned by one of the child elements or some variable inside the internal context.
                switch(attributeType) {
                    case DYNAMIC_CHILD_DEPENDANT:
                                                 {
                                                     if (attributeValue.length() > 2) {  //length of $$ = 2
                                                         String referencedChildId = attributeValue.substring(2);
                                                         ICompilationUnit childCU = getChild(referencedChildId);
                                                         if (childCU instanceof IEvaluable) {
                                                             //attributeValue = (String) ((IEvaluable) childCU).getValue(compilationRuntimeContext);
                                                             Object _value = ((IEvaluable) childCU).getValue(compilationRuntimeContext);
                                                             if (_value != null) {
                                                                 attributeValue = _value.toString();
                                                             }
                                                         }
                                                     }
                                                     break;
                                                 }
                    case DYNAMIC_INTERNALCONTEXT_DEPENDANT:
                                                 {
                                                     if (attributeValue.length() > 1) {  //length of $ = 1
                                                         Object _value = UtilityFunctions.getValue(attributeValue.substring(1),
                                                                                                  compilationRuntimeContext.getInternalContext());
                                                         if (_value != null) {
                                                             attributeValue = _value.toString();
                                                         }
                                                     }
                                                     break;
                                                 }
                }
            }
            return attributeValue;
        }

        private static AttributeType getAttributeTypeFromValue(String rawAttributeValue) {
            AttributeType attributeType = AttributeType.STATIC;
            if (rawAttributeValue == null) {
                attributeType = AttributeType.STATIC;
            } else if (rawAttributeValue.startsWith("$$")) {
                attributeType = AttributeType.DYNAMIC_CHILD_DEPENDANT;
            } else if (rawAttributeValue.startsWith("$")) {
                attributeType = AttributeType.DYNAMIC_INTERNALCONTEXT_DEPENDANT;
            }
            return attributeType;
        }

        private static boolean isAttributeTypeDynamic(AttributeType attributeType) {
            return attributeType == AttributeType.DYNAMIC_CHILD_DEPENDANT ||
                   attributeType == AttributeType.DYNAMIC_INTERNALCONTEXT_DEPENDANT;
        }

        public static boolean isAttributeValueDynamic(String rawAttributeValue) {
            return CompilationUnit.isAttributeTypeDynamic(CompilationUnit.getAttributeTypeFromValue(rawAttributeValue));
        }

        public boolean isAttributeDynamic(String key) {
            return CompilationUnit.isAttributeValueDynamic(getAttribute(key));
        }
        /******************************************************************************************************************************/
        /************************** End - New attribute methods added to generically support computed values **************************/
        /******************************************************************************************************************************/

        /**************************************************************************************/
        /***********Basically used while cloning a CU during inheritance processing************/
        Properties getAttributes() {
            return attributes;
        }

        void copyAttributes(Properties attributes) {
            this.attributes.putAll(attributes);
        }
        /**************************************************************************************/

        public String getId() {
            return getAttribute(ATTRIBUTE_ID);
        }

        /** This method can be overridden by sub classes to return value of another attribute to be used as id
         * (e.g. Group may want to use its name as its id also, Set may want to use the name of the attribute
         * as its id) in case the value returned by getId() is null.
         * 
         * @return
         */
        public String getIdOrElse() {
            return getId();
        }

        public boolean matchesIdOrElse(String idOrElse) {
            if (idOrElse == null) {
                return false;
            }
            return idOrElse.equals(getIdOrElse());
        }

        public void compile(Node n) throws XPathExpressionException {
            this.nodeContext = n;

            // compile all the base attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }

            // set the implicit namespace attributie
            if (n.getNamespaceURI() != null) {
                setAttribute(IMPLICIT_ATTRIBUTE_NAMESPACE_URI, n.getNamespaceURI());
            }

            //compile the attributes
            doCompileAttributes(n);
            //compile the children
            doCompileChildren(n);
            //doCompile(n);
        }

        /******* Start - Default implementations of compilation related methods. It should suffice for most of the cases. *******/
        /******* If any of the subclasses need absolute control over any of the compilation tasks then they can override. *******/
        /************************************************************************************************************************/
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            //compile all the children
            NodeList nl = (NodeList) CompilationUnits.XPATH
                                         .evaluate("*", n, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                String nodeName = nl.item(i).getNodeName();
                if (isChildTagRecognized(nodeName)) {
                    doCompileChild(nl.item(i));
                }
            }
        }

        protected void doCompileChild(Node n) throws XPathExpressionException {
            String nodeName = n.getNodeName();
            ICompilationUnit cu = CompilationUnits.getCompilationUnitForTag(nodeName);
            if (cu != null) {
                cu.compile(n);
                doAddCompiledUnit(nodeName, cu);
            }
        }
        /******* End - Default implementations of compilation related methods *******/
        /****************************************************************************/

        /**************************** Start - util methods ****************************/
        /******************************************************************************/
        static boolean areMatchingTypes(Class<? extends ICompilationUnit> t1, Class<? extends ICompilationUnit> t2) {
            return t1 == t2;
        }

        //Note: The method would return null if element list is null.
        static <T extends ICompilationUnit> T[] getElementsFromList(List<T> elementList, T[] typeArray) {
            if (elementList != null) {
                return elementList.toArray(typeArray);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        //Note: The method would return null if the element passed is null or if elementType is null or if
        //the class of element doesn't match the component type of the class represented by elementType.
        static <T extends ICompilationUnit, T1 extends ICompilationUnit> T[] getElementAsUnitArray(T element,
                                                                                                   Class<T1> elementType) {
            if (element != null && elementType != null &&
                    areMatchingTypes(element.getClass(), elementType)) {
                T[] returnArray = (T[]) Array.newInstance(elementType, 1);
                returnArray[0] = element;
                return returnArray;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        static <T extends ICompilationUnit> T[] getZeroLengthArrayOfType(Class<T> elementType) {
            return (T[]) Array.newInstance(elementType, 0);
        }
        /***************************** End - Util methods *****************************/
        /******************************************************************************/

        protected abstract boolean isChildTagRecognized(String tagName);
        protected abstract void doCompileAttributes(Node n) throws XPathExpressionException;
        protected abstract void doAddCompiledUnit(String cuTagName, ICompilationUnit cu);
        //protected abstract void doCompile(Node n) throws XPathExpressionException;
        public abstract ICompilationUnit getChild(String idOrElseOfChild);
        public abstract <T extends ICompilationUnit> T[] getChildren(Class<T> type);
    }

    public interface ICondition extends ICompilationUnit {
        boolean matches(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;
    }

    public interface IEvaluable extends ICompilationUnit {
        Object getValue(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;
    }

    public interface IExtensible<T extends ICompilationUnit> extends ICompilationUnit {
        List<Extends> getExtensions();
        boolean hasExtends();
        boolean hasConditionalExtends();
        boolean areExtensionsMarkedAsProcessed();
        T extend(CompilationRuntimeContext compilationRuntimeContext,
                CompiledTemplatesRegistry mctr) throws XPathExpressionException;
    }

    public interface IExecutable extends ICompilationUnit {
        Object execute(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;;
    }

    /**
     * This CU also implicitly provides the support for transforming the evaluated value before returning it.
     *
     */
    public abstract static class EvaluableCompilationUnit extends CompilationUnit implements IEvaluable {
        private static final String SKIP_EVALUATION = "EVAL.NONE";  //if the text node value equals this string then no xpath evaluations would be performed
                                                                    //over the value of this cu. Explicit skipping of evaluations would come handy in cases
                                                                    //where the base cu has defined some transformation expressions but the super unit doesn't
                                                                    //want to execute any by virtue to inheritance.

        private static final String TEXT_NODE_XPATH = "text()[1]";
        private static final String ATTRIBUTE_EVAL_IF_NULL = "evalIfNull";
        private static final String ATTRIBUTE_PGVIF = "pgvif";  //call 'postGetValue inside finally'. If this attribute is defined (irrespective of its value)
                                                                //then the postGetValue would be called inside finally. Doing so may be useful in cases where
                                                                //the cu performs some critical function and in case of exceptions may be interested in ensuring
                                                                //actions like logging of the event takes place.

        private static final String TRUE = "true";

        private boolean evalIfNull = true;    //by default if the value of this ECU evaluates to null
                                              //then we would still perform the transformation, if any
                                              //such expression is defined. This behavior could be changed
                                              //by setting the value of ATTRIBUTE_EVAL_IF_NULL to
                                              //anything other than 'true'. A typical case could be where
                                              //the transformation expression makes a reference to this
                                              //and when it won't make sense to perform transformation with
                                              //a null value (e.g. inside Select CU when no criteria has
                                              //been met).

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            //compile all the attributes
            setAttribute(
                    ATTRIBUTE_EVAL_IF_NULL,
                    getAttributeValueIffAttributeIsDefined("@" + ATTRIBUTE_EVAL_IF_NULL, n));
            setAttribute(
                    ATTRIBUTE_PGVIF,
                    getAttributeValueIffAttributeIsDefined("@" + ATTRIBUTE_PGVIF, n));
            String textNodePathAsStr = getAttributeValueIffAttributeIsDefined(TEXT_NODE_XPATH, n);
            if (textNodePathAsStr != null && !"".equals(textNodePathAsStr.trim())) {
                setAttribute(TEXT_NODE_XPATH, textNodePathAsStr.trim());
            }

            initPerformanceVariables();  //initialize some attribute variables for performance optimization
        }

        /**************** Pre-initialize some attribute variables for performance enhancement ****************/
        /*****************************************************************************************************/
        protected void initPerformanceVariables() {
            //------------------------------------------------------------------------------
            //initialize variables for some string attributes for performance optimizations
            //------------------------------------------------------------------------------
            String evalIfNullTmp = getAttribute(ATTRIBUTE_EVAL_IF_NULL);
            if (evalIfNullTmp == null || "".equals(evalIfNullTmp)) {
                evalIfNullTmp = "true";
            }
            evalIfNull = TRUE.equalsIgnoreCase(evalIfNullTmp);
            //------------------------------------------------------------------------------
            //------------------------------------------------------------------------------
        }
        /****************************************************************************************************/

        private boolean isCallPostGetValueInsideFinally() {
            return getAttribute(ATTRIBUTE_PGVIF) != null;
        }

        /************************************************************************************/
        /************* Start - pre and post processing methods of getValue(...) *************/
        /************************************************************************************/
        /************************************************************************************/
        //Provided a general method for evaluable units to indicate if the code inside get value needs to be executed or not.
        //Subclasses to override as needed.
        protected boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            return true;
        }

        //the value returned by this method would be returned by getValue(...) in case the _satisfies(...) returned false.
        //the default implementation returns 'null' but some compilation units like Group may want to return an empty string instead of 'null'.
        //Subclasses to override as needed. 
        protected Object _noValue() {
            return null;
        }

        //this method gets called just before calling the doGetValue method.
        protected void preGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            //nothing to do by default. Subclasses to override as needed.
        }

        //this method gets called after all other value processor methods have been called i.e. doGetValue, getEvaluatedValue, getDefaultValue.
        protected void postGetValue(Object value, CompilationRuntimeContext compilationRuntimeContext)
                                                                                   throws XPathExpressionException {
            //default impl just makes the value of this evaluable unit (passed as one of the method parameters) available inside the internal context map.
            //Subclasses to override as needed.

            if (value == null) {
                return;  //we can just return from here as setting null value inside map would have made no difference as the value returned by map for
                         //a missing key and for a key having null value is 'null' in both cases.
            }
            if (compilationRuntimeContext.getInternalContext() == null) {
                compilationRuntimeContext.setInternalContext(new HashMap<String, Object>());  //initialize the internal context map as we plan to make the
                                                                                              //value available inside it.
            }
            String idOrElse = getIdOrElse();  //computedAttributeValue(getIdOrElse(), compilationRuntimeContext);  //use the computed value of getIdOrElse(...)
            String thisValueIdentifier = "-" + (idOrElse == null? "unnamed": idOrElse) + "-value";  //refer comment below
                                                                            //if id or else represents a dynamically resolvable value i.e. one starting with $ then
                                                                            //also let's just use its uncomputed raw value as the intent is just to have a unique key
                                                                            //using which the value of this evaluable unit can be accessed. Also primarily the value
                                                                            //of this evaluable cu would need to be accessed in test environment and there it would
                                                                            //be easy to lookup using the raw id (which would apparently be visible in the xml definition)
                                                                            //as opposed to the computed value which would be known only at runtime. Further, let's form this
                                                                            //value accessor key by prepending a special character like '-' to the idOrElse raw value so as to
                                                                            //make it ineligible for dynamic computation even if it started with $. If not done so then the value
                                                                            //lookup might fail as the accessor's valueof key, in case it started with $, would become eligible for
                                                                            //dynamic resolution and the value lookup might fail in that case as the value here might have been
                                                                            //mapped to a different key value.
            compilationRuntimeContext.getInternalContext().put(thisValueIdentifier, value);
        }
        /************************************************************************************/
        /************** End - pre and post processing methods of getValue(...) **************/
        /************************************************************************************/

        @Override
        public final Object getValue(CompilationRuntimeContext compilationRuntimeContext)
                                                        throws XPathExpressionException {
            if (!satisfies(compilationRuntimeContext)) {
                return _noValue();
            }

            //saved the internal context of this CU for reseting later inside this method.
            java.util.Map<String, Object> savedInternalContext = compilationRuntimeContext.getInternalContext();
            if (savedInternalContext != null) {
                //instead of just holding the reference to the internal context we would like to create a copy
                //of it so that even if the internal context map gets modified by any of the methods of the
                //processor CU we would still be able to restore back to the original context state cleanly.
                java.util.Map<String, Object> savedInternalContextCpy = new HashMap<String, Object>();
                savedInternalContextCpy.putAll(savedInternalContext);
                compilationRuntimeContext.setInternalContext(savedInternalContextCpy);  //savedInternalContext = savedInternalContextCpy;  //refer comment below:
                                                                                        //Updated the reference of internal context map inside compilationRuntimeContext
                                                                                        //for this getValue request as opposed to the previous scheme of using the same reference of
                                                                                        //internal context map inside compilationRuntimeContext for this getValue request and later
                                                                                        //restoring the copy of savedInternalContext inside finally. Functionally the two approaches
                                                                                        //would have been no different except in one case. If the invoker of this getValue
                                                                                        //request had obtained a reference to the internal context map before and now wishes to add/remove objects
                                                                                        //to/from the saved internal context reference then that would not reflect inside compilationRuntimeContext
                                                                                        //as the reference to the internal context map inside compilationRuntimeContext would already
                                                                                        //have changed (inside finally) before returning from this method. A workaround then would have been
                                                                                        //to add/remove objects to/from internal context map by obtaining a fresh copy of it using the
                                                                                        //compilationRuntimeContext.getInternalContext() which would not be necessary now.
            }

            Object finalValue = null;
            boolean callPostGetValueInsideFinally = isCallPostGetValueInsideFinally();
            try {
                preGetValue(compilationRuntimeContext);  //getValue's pre processing
                Object value = doGetValue(compilationRuntimeContext);
                //get the evaluated value
                value = (value == null && !evalIfNull) ?
                                                 value :
                                                 getEvaluatedValue(compilationRuntimeContext, value);
                if (value == null) {
                    Object defaultValue = getDefaultValue();
                    if (defaultValue != null) {
                        value = defaultValue;
                    }
                }

                if (!callPostGetValueInsideFinally) {
                    postGetValue(value, compilationRuntimeContext);  //getValue's post processing
                                                                     //calling this method here means it may not get called at all in case some exception occurs in this try block.
                } else {
                    //postGetValue(...) will be called inside finally. Let's prepare the value to be passed to that method.
                    finalValue = value;  //let's set the final value as by now all processing would have been done. This variable was needed to ensure that we pass either the final
                                         //processed value or simply 'null' (in case some exception occurs while processing) as parameter to postGetValue(...) method call inside the
                                         //finally block.
                }

                return value;
            } finally {
                try {
                    //now that we have introduced a postGetValue phase let's ensure its call is wrapped inside try/finally so that the original 'finally' intent of resetting the
                    //state of internal context map is fulfilled even if some exception occurs while processing inside postGetValue(...)
                    if (callPostGetValueInsideFinally) {
                        postGetValue(finalValue, compilationRuntimeContext);  //getValue's post processing
                    }
                } finally {
                    //as we are done with getting the value of the CU and also its final evaluation (after performing any
                    //transformations) let us now reset the internal context to the state it was in when we started
                    //processing this CU
                    compilationRuntimeContext.setInternalContext(savedInternalContext);
                }
            }
        }

        //return the value after computing the value of the text() expression using the variables
        //declared inside the internal context of CompilationRuntimeContext.
        protected Object getEvaluatedValue(CompilationRuntimeContext compilationRuntimeContext,
                                                        Object thisValue) throws XPathExpressionException {
            String nodeTextExpression = getAttribute(TEXT_NODE_XPATH);
            if (nodeTextExpression == null || "".equals(nodeTextExpression.trim())
                                           || SKIP_EVALUATION.equals(nodeTextExpression.trim())) {
                return thisValue;
            }

            java.util.Map<String, Object> declaredVariables = compilationRuntimeContext.getInternalContext();

            //nodeTextExpression = nodeTextExpression.replaceAll("\\bthis\\b", "'" + thisValue + "'");  //update any
                                                                                                      //references
                                                                                                      //to 'this'
            //commented the above line and provided the below line of code. No implicit enclosing of
            //thisValue with single quotes would be done as that causes problems in case the value itself contains
            //single quotes. If the value needs to be enclosed with single quotes or double quotes then do so at
            //the source itself i.e. at the time of defining the expression inside the xml.
            nodeTextExpression = nodeTextExpression.replaceAll("\\bthis\\b", Matcher.quoteReplacement("" + thisValue));  //update any
                                                                                                                         //references
                                                                                                                         //to 'this'
            if (declaredVariables != null) {
                for (Entry<String, Object> entry : declaredVariables.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        //nodeTextExpression = nodeTextExpression.replaceAll("\\b" + key + "\\b", "'" + value + "'");

                        //commented the above line and provided the below line of code. No implicit enclosing of
                        //thisValue with single quotes would be done as that causes problems in case the value itself contains
                        //single quotes. If the value needs to be enclosed with single quotes or double quotes then do so at
                        //the source itself i.e. at the time of defining the expression inside the xml.
                        nodeTextExpression = nodeTextExpression.replaceAll("\\b" + key + "\\b", Matcher.quoteReplacement("" + value));
                    }
                }
            }
            //TODO change evaluation scheme to one using XPathVariableResolver.
            return CompilationUnits.XPATH.evaluate(
                                  nodeTextExpression, getNodeContext(), XPathConstants.STRING);
        }

        protected abstract Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                            throws XPathExpressionException;

        //by default we assume there is no default value.
        //If the subclass supports one then it should override this method.
        protected Object getDefaultValue() {
            return null;
        }
    }

    public abstract static class ExtensibleCompilationUnit<T extends ICompilationUnit>
                                                        extends EvaluableCompilationUnit implements IExtensible<T> {

        //private static final String CHILDREN_XPATH = "./extends";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Extends.TAG_NAME});

        private List<Extends> extensions = null;
        private boolean extensionsProcessed = true;
        private boolean firstTime = true;

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {

            extensions = new LinkedList<Extends>();  //since mostly we would
                                                       //be operating
                                                       //sequentially so
                                                       //LinkedList would be an
                                                       //optimal choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (cu instanceof Extends) {
                extensions.add((Extends) cu);
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName);
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (extensions != null) {
                for (Extends extension : extensions) {
                    if (idOrElseOfChild.equals(extension.getIdOrElse())) {
                        return extension;
                    }
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T1 extends ICompilationUnit> T1[] getChildren(Class<T1> type) {
            Extends[] extendsArray = areMatchingTypes(Extends.class, type) ?
                                                getElementsFromList(extensions, new Extends[0]) : null;
            return (T1[]) extendsArray;  //if extendsArray is null here then don't return a zero length array in this
                                         //case since the null check is used in super classes to continue or break the
                                         //children lookup and appropriately return the right type of children.
        }

        @Override
        public List<Extends> getExtensions() {
            return Collections.unmodifiableList(extensions);
        }

        @Override
        public boolean hasExtends() {
            return !extensions.isEmpty();
        }

        @Override
        public boolean hasConditionalExtends() {
            for (Extends extension : extensions) {
                if (extension.hasConditionals()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean areExtensionsMarkedAsProcessed() {
            return extensionsProcessed;
        }

        @SuppressWarnings("unchecked")
        public final T extend(CompilationRuntimeContext compilationRuntimeContext,
                CompiledTemplatesRegistry mctr) throws XPathExpressionException {
            if (!firstTime && extensionsProcessed) {
                return (T) this;
            } else {
                T extendedCU = doExtend(compilationRuntimeContext, mctr);
                firstTime = false;
                return extendedCU;
            }
        }

        //This method provides support for dynamic resolution of the CU identifier. Any identifier starting with
        //special characters (e.g. '$') would be dynamically computed. This method comes into picture basically
        //during inheritance processing but now this is also used to extract the group name while
        //serializing the group.
        public String getIdOrElse(CompilationRuntimeContext compilationRuntimeContext)
                                                                            throws XPathExpressionException {
            String computedIdOrElse = getIdOrElse();  //initialize the identifier with the uncomputed value
            /*
                //Let's not trim the identifier and expect that the same is provided correctly inside the
                //xml file. Trimming might bring in inconsistency between the values returned by non-parameterized
                //getIdOrElse() method and this method in case the computation wasn't required (e.g. when the
                //identifier string didn't start with $) or when the computation failed (e.g. when no child cu
                //corresponding to the referenced identifier is found). In such cases the value returned by
                //non-parameterized getIdOrElse() method should be returned as is without any trimming/modifications.
                computedIdOrElse = computedIdOrElse != null ? computedIdOrElse.trim() : computedIdOrElse;
            */
            /* if (isIdOrElseDynamic(computedIdOrElse)) {
                //ids starting with '$' has special meaning and needs to be resolved to the value
                //returned by one of the child elements.
                if (computedIdOrElse.length() > 1) {
                    String referencedChildId = computedIdOrElse.substring(1);
                    ICompilationUnit childCU = getChild(referencedChildId);
                    if (childCU instanceof IEvaluable) {
                        computedIdOrElse = (String) ((IEvaluable) childCU).getValue(compilationRuntimeContext);
                    }
                }
            }
            return computedIdOrElse; */
            return computeAttributeValue(computedIdOrElse, compilationRuntimeContext);
        }

        protected boolean isIdOrElseDynamic() {
            return isIdOrElseDynamic(getIdOrElse());
        }

        protected boolean isIdOrElseDynamic(String rawIdOrElse) {
            //return rawIdOrElse != null && rawIdOrElse.startsWith("$");  //'$' is at index 0
            return CompilationUnit.isAttributeValueDynamic(rawIdOrElse);
        }

        protected abstract T doExtend(CompilationRuntimeContext compilationRuntimeContext,
                CompiledTemplatesRegistry mctr) throws XPathExpressionException;

        /**************************************************************************************/
        /***********Basically used while cloning a CU during inheritance processing************/
        void copyExtensions(List<Extends> extensions) {
            if (this.extensions == null) {
                this.extensions = new LinkedList<Extends>();
            }
            this.extensions.addAll(extensions);
        }

        void markExtensionsAsProcessed(boolean flag) {
            extensionsProcessed = flag;
        }
        /**************************************************************************************/
    }

    public static class Conditional extends EvaluableCompilationUnit implements ICondition, IEvaluable {
        public static final String TAG_NAME = "conditional";

        private static final String ATTRIBUTE_META_EXPRESSION = "expression";
        private static final String ATTRIBUTE_VALUE = "value";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_META_EXPRESSION, ATTRIBUTE_VALUE};
        //private static final String CHILDREN_XPATH = "./condition";
        //private static final String CHILDREN_XPATH2 = "./valueof | ./get | ./select";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[] {Condition.TAG_NAME, ValueOf.TAG_NAME, Get.TAG_NAME, Select.TAG_NAME});

        private List<Condition> conditions = null;
        //private EvaluableCompilationUnit evaluable = null;
        private IEvaluable evaluable = null;

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            super.doCompileAttributes(n);

            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {

            conditions = new LinkedList<Condition>();  //since mostly we would
                                                       //be operating
                                                       //sequentially so
                                                       //LinkedList would be an
                                                       //optimal choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (cu instanceof Condition) {
                conditions.add((Condition) cu);
            //} else if ((cu instanceof EvaluableCompilationUnit) && (evaluable == null)) {
            } else if ((cu instanceof IEvaluable) && (evaluable == null)) {
                //we need just one evaluable cu and in case of many defined, let's just retain the first instance that we got.
                evaluable = (IEvaluable) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName) ||
                   CompilationUnits.isAssignableFrom(IEvaluable.class, CompilationUnits.getCompilationClassForTag(tagName));
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (conditions != null) {
                for (Condition c : conditions) {
                    if (idOrElseOfChild.equals(c.getIdOrElse())) {
                        return c;
                    }
                }
            }
            if (evaluable != null && idOrElseOfChild.equals(evaluable.getIdOrElse())) {
                return evaluable;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            Condition[] conditionsArray = areMatchingTypes(Condition.class, type) ?
                                                        getElementsFromList(conditions, new Condition[0]) : null;
            if (conditionsArray != null) {
                return (T[]) conditionsArray;
            }
            /*EvaluableCompilationUnit[] evaluableArray = getElementAsUnitArray(evaluable, type);
            return (T[]) (evaluableArray == null ? getZeroLengthArrayOfType(type) : evaluableArray);*/
            IEvaluable[] evaluableArray = getElementAsUnitArray(evaluable, type);
            return (T[]) (evaluableArray == null ? getZeroLengthArrayOfType(type) : evaluableArray);
        }

        @Override
        public boolean matches(CompilationRuntimeContext compilationRuntimeContext)
                                                                throws XPathExpressionException {
            String expr = getAttribute(ATTRIBUTE_META_EXPRESSION);
            if (expr == null) {
                return true;  //no expression specififed. Let's just return true. Alternately we could also have implicitly and'ed return values of all the
                              //conditions inside the for loop below to decide the truth or falseness of this method. I just chose the first over the other.
            }
            for (Condition condition : conditions) {
                expr = expr.replaceAll("\\b" + condition.getId() + "\\b",
                               Boolean.toString(condition.matches(compilationRuntimeContext)) + "()");
            }
            return (Boolean) CompilationUnits.XPATH.evaluate(
                                    expr, getNodeContext(), XPathConstants.BOOLEAN);
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            Object value = null;
            if (evaluable != null) {
                Object evaluableValueTmp = evaluable.getValue(compilationRuntimeContext);
                value = "".equals(evaluableValueTmp) ? null : evaluableValueTmp;
            }
            return value;
        }

        @Override
        protected Object getDefaultValue() {
            return getAttribute(ATTRIBUTE_VALUE);
        }
    }

    public static class Condition extends CompilationUnit implements ICondition {
        public static final String TAG_NAME = "condition";

        private static final String ATTRIBUTE_EXPRESSION = "expression";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_EXPRESSION};
        //private static final String CHILDREN_XPATH = "./valueof | ./get";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{ValueOf.TAG_NAME, Get.TAG_NAME});

        //private ValueOf valueOf = null;
        private IEvaluable evaluable = null;

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            /*if ((cu instanceof ValueOf) && (valueOf == null)) {
                //we need just one valueOf cu and in case of many defined, let's just retain the first instance that we got.
                valueOf = (ValueOf) cu;
            }*/
            if ((cu instanceof IEvaluable) && (evaluable == null)) {
                //we need just one evaluable cu and in case of many defined, let's just retain the first instance that we got.
                evaluable = (IEvaluable) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            //return RECOGNIZED_CHILD_TAGS.contains(tagName);
            return CompilationUnits.isAssignableFrom(IEvaluable.class, CompilationUnits.getCompilationClassForTag(tagName)); 
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (evaluable != null && idOrElseOfChild.equals(evaluable.getIdOrElse())) {
                return evaluable;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            /*ValueOf[] valueOfArray = getElementAsUnitArray(valueOf, type);
            return (T[]) (valueOfArray == null ? getZeroLengthArrayOfType(type) : valueOfArray);*/
            IEvaluable[] evaluableArray = getElementAsUnitArray(evaluable, type);
            return (T[]) (evaluableArray == null ? getZeroLengthArrayOfType(type) : evaluableArray);
        }

        @Override
        public boolean matches(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            if (evaluable == null) {
                return true; //TODO or should we return false?
            }
            String expr = getAttribute(ATTRIBUTE_EXPRESSION);
            if (expr == null) {
                return true; // no matcher expression provided. Let's just
                             // return true.
            }
            Object evaluableValueTmp = evaluable.getValue(compilationRuntimeContext);
            evaluableValueTmp = "".equals(evaluableValueTmp) ? null : evaluableValueTmp;
            return evaluableValueTmp == null ? false : Pattern.matches(expr, evaluableValueTmp.toString());
        }
    }

    public static class ValueOf extends EvaluableCompilationUnit implements IEvaluable {
        public static final String TAG_NAME = "valueof";

        private static final String ATTRIBUTE_KEY = "key";
        private static final String ATTRIBUTE_DEFAULT_VALUE = "default";
        private static final String ATTRIBUTE_EXTRACTION_EXPRESSION = "extractionExpression";
        private static final String ATTRIBUTE_MATCHER_GROUP = "matcherGroup";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_KEY, ATTRIBUTE_DEFAULT_VALUE,
                                                    ATTRIBUTE_EXTRACTION_EXPRESSION, ATTRIBUTE_MATCHER_GROUP};
        //private static final String CHILDREN_XPATH = "./map | ./internal-map";
        //private static final String CHILDREN_XPATH2 = "./on";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Map.TAG_NAME, InternalMap.TAG_NAME, Json.TAG_NAME, On.TAG_NAME});

        private List<Map> maps = null;
        private On on = null;  //'on' condition would be used only if there are no map entries defined.

        private String getKey(CompilationRuntimeContext compilationRuntimeContext) {
            String key = null;
            try {
                //attempt computation
                key = getAttribute(ATTRIBUTE_KEY, compilationRuntimeContext);  //invoking the computed version
            } catch(XPathExpressionException xpee) {
                key = getAttribute(ATTRIBUTE_KEY);  //return the uncomputed value as fallback.
            }
            return key;
        }

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            super.doCompileAttributes(n);

            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            maps = new LinkedList<Map>();  //since mostly we would be operating
                                           //sequentially so
                                           //LinkedList would be an optimal
                                           //choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (cu instanceof Map) {
                maps.add((Map) cu);
            } else if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName);
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (maps != null) {
                for (Map map : maps) {
                    if (idOrElseOfChild.equals(map.getIdOrElse())) {
                        return map;
                    }
                }
            }
            if (on != null && idOrElseOfChild.equals(on.getIdOrElse())) {
                return on;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            Map[] mapsArray = areMatchingTypes(Map.class, type) ? getElementsFromList(maps, new Map[0]) : null;
            if (mapsArray != null) {
                return (T[]) mapsArray;
            }
            On[] onArray = getElementAsUnitArray(on, type);
            return (T[]) (onArray == null ? getZeroLengthArrayOfType(type) : onArray);
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            boolean abortIfNotSatisfy = compilationRuntimeContext.isAbortIfNotSatisfy();
            //MapOfMaps mapOfMaps = compilationRuntimeContext.getExternalContext();
            Object value = null;
            boolean isMapsListEmpty = maps.isEmpty();
            boolean satisfiesAtLeastOne = isMapsListEmpty;  //if no maps are defined in which to lookup (say in case
                                                           //when we are interested in just setting a default value
                                                           //then 'satisfiesAtLeastOne' should be set to true.
            String key = getKey(compilationRuntimeContext);
            for (Map map : maps) {
                if (map.satisfiesOn(compilationRuntimeContext)) {
                    satisfiesAtLeastOne = true;
                    /****** Commented out this block because now the responsibility
                     ****** of returning the value for a key has been moved to the Map cu impl class. ***
                    java.util.Map<String, String> mapTmp = mapOfMaps == null ?
                                                                      null :
                                                                      mapOfMaps.getMap(map.getName());
                    if (mapTmp != null) {
                        value = mapTmp.get(getAttribute(ATTRIBUTE_KEY));
                        if (value != null) {
                            break;
                        }
                    }************************************************************************************/
                    value = map.getValue(key/*getAttribute(ATTRIBUTE_KEY)*/, compilationRuntimeContext);
                    if (value != null) {
                        break;
                    }
                }
            }

            //if there are no lookup maps available but if there is an 'on' condition available then we
            // need to set the satisfaction criteria to the one returned by the 'on' condition.
            satisfiesAtLeastOne = isMapsListEmpty ?
                                  (on != null ? on.satisfies(compilationRuntimeContext) : satisfiesAtLeastOne) :
                                  satisfiesAtLeastOne;

            if (abortIfNotSatisfy && !satisfiesAtLeastOne) {
                throw new RuntimeException("No available conditions satisfied.");
            }
            return getExtractedGroupValue(value);
        }

        private Object getExtractedGroupValue(Object value) {
            if (value == null) {
                return null;
            }
            String extractionExpr = getAttribute(ATTRIBUTE_EXTRACTION_EXPRESSION);
            extractionExpr = extractionExpr == null || "".equals(extractionExpr) ? null : extractionExpr;
            if (extractionExpr == null) {
                return value;  //no extraction expression specified. Return the value as is.
            }
            int matcherGroup = -1;
            if (getAttribute(ATTRIBUTE_MATCHER_GROUP) != null) {
                matcherGroup = Integer.parseInt(getAttribute(ATTRIBUTE_MATCHER_GROUP));  //let it throw number format
                                                                                         //exception if no valid int
                                                                                         //value is specified.
            }
            if (matcherGroup >= 0) {
                //compile the extraction expression and return the requested matcher group
                Matcher matcher = Pattern.compile(extractionExpr).matcher(value.toString());
                return matcher.find() ? matcher.group(matcherGroup) : null;
            } else {
                //the request is not to return any specific matcher group but to use the
                //extraction expression to split the input value into an array of tokens.
                return Pattern.compile(extractionExpr).split(value.toString());
            }
        }

        @Override
        protected Object getDefaultValue() {
            return getAttribute(ATTRIBUTE_DEFAULT_VALUE);
        }
    }

    //returns the java class type of value object returned by the enclosed evaluable child cu.
    public static class TypeOf extends EvaluableCompilationUnit implements IEvaluable {
        public static final String TAG_NAME = "typeof";

        private static final String ATTRIBUTE_DEFAULT_VALUE = "default";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_DEFAULT_VALUE};

        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[] {On.TAG_NAME});

        private IEvaluable evaluable = null;
        private On on = null;

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            super.doCompileAttributes(n);

            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if ((cu instanceof IEvaluable) && (evaluable == null)) {
                //we need just one evaluable cu and in case of many defined, let's just retain the first instance that we got.
                evaluable = (IEvaluable) cu;
            }
            if ((cu instanceof On) && (on == null)) {
                //we need just one ‘on’ cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName) ||
                   CompilationUnits.isAssignableFrom(IEvaluable.class, CompilationUnits.getCompilationClassForTag(tagName));
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (evaluable != null && idOrElseOfChild.equals(evaluable.getIdOrElse())) {
                return evaluable;
            }
            if (on != null && idOrElseOfChild.equals(on.getIdOrElse())) {
                return on;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            IEvaluable[] evaluableArray = getElementAsUnitArray(evaluable, type);
            if (evaluableArray != null) {
                return (T[]) evaluableArray;
            }
            On[] onArray = getElementAsUnitArray(on, type);
            return (T[]) (onArray == null ? getZeroLengthArrayOfType(type) : onArray);
        }

        @Override
        protected boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            return on == null || on.satisfies(compilationRuntimeContext);
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            Object value = null;
            if (evaluable != null) {
                value = evaluable.getValue(compilationRuntimeContext);
            }
            return value != null? value.getClass().getName(): value;
        }

        @Override
        protected Object getDefaultValue() {
            return getAttribute(ATTRIBUTE_DEFAULT_VALUE);
        }
    }

    public static class Map extends CompilationUnit {
        public static final String TAG_NAME = "map";

        static final String INTERNAL_CONTEXT = "internal";

        private static final String ATTRIBUTE_NAME = "name";
        static final String ATTRIBUTE_CONTEXT = "context";
        private static final String ATTRIBUTE_KEY_DELIMITER = "keyDelimiter";  //to be used to extract the hierarchy of keys
        private static final String[] ATTRIBUTES = {ATTRIBUTE_NAME, ATTRIBUTE_CONTEXT, ATTRIBUTE_KEY_DELIMITER};
        //private static final String CHILDREN_XPATH = "./on";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{On.TAG_NAME});

        private On on = null;

        String getName(CompilationRuntimeContext compilationRuntimeContext) {
            String name = null;
            try {
                //attempt computation
                name = getAttribute(ATTRIBUTE_NAME, compilationRuntimeContext);  //invoking the computed version
            } catch(XPathExpressionException xpee) {
                name = getAttribute(ATTRIBUTE_NAME);  //return the uncomputed value as fallback.
            }
            return name;
        }

        private String getKeyDelimiter() {
            return getAttribute(ATTRIBUTE_KEY_DELIMITER);
        }

        protected boolean isInternalContext() {
            return INTERNAL_CONTEXT.equalsIgnoreCase(getAttribute(ATTRIBUTE_CONTEXT));
        }

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName);
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (on != null && idOrElseOfChild.equals(on.getIdOrElse())) {
                return on;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            On[] onArray = getElementAsUnitArray(on, type);
            return (T[]) (onArray == null ? getZeroLengthArrayOfType(type) : onArray);
        }

        public boolean satisfiesOn(CompilationRuntimeContext compilationRuntimeContext)
                                                                        throws XPathExpressionException {
            return on != null ? on.satisfies(compilationRuntimeContext) : true;
        }

        public Object remove(String key, CompilationRuntimeContext compilationRuntimeContext) {
            Object value = null;
            MapOfMaps externalCtx = compilationRuntimeContext.getExternalContext();
            java.util.Map<String, Object> internalCtx = compilationRuntimeContext.getInternalContext();
            //if (key != null) {  //Let's proceed even if key is not specified as in that case we may be
                                  //interested in clearing the contents of the entire  map.
                String mapName = getName(compilationRuntimeContext);
                java.util.Map<String, Object> map = isInternalContext()?
                                                        null:  //we do not want to allow explicit removal of items from internal context map hence commented out below line and using null here
                                                        //mapName != null && internalCtx != null? (java.util.Map<String, Object>) internalCtx.get(mapName): internalCtx:
                                                                                                  //NOTE: we are assuming here that the type cast to java.util.Map above would not fail.
                                                                                                  //If instead of a map some other object type corresponds to the key's value then a
                                                                                                  //ClassCastException would be thrown.
                                                        mapName != null && externalCtx != null? externalCtx.getMap(mapName): null;  //it doesn't make sense to not specify any map name
                                                                                                                                    //when using external context.
                if (map != null) {
                    if (key != null)
                        value = map.remove(key);  //remove the specified key and return its value
                    else
                        map.clear();  //clear the entire map
                }
            //}
            return value;
        }

        public Object getValue(String key, CompilationRuntimeContext compilationRuntimeContext) {
            Object value = null;
            MapOfMaps externalCtx = compilationRuntimeContext.getExternalContext();
            java.util.Map<String, Object> internalCtx = compilationRuntimeContext.getInternalContext();
            //if (key != null) {  //Let's proceed even if key is not specified as in that case we may be
                                  //interested in returning the full map which may then, e.g., be iterated using a 'loop' cu.
                String mapName = getName(compilationRuntimeContext);
                java.util.Map<String, Object> map = isInternalContext()?
                                                        mapName != null && internalCtx != null? (java.util.Map<String, Object>) internalCtx.get(mapName): internalCtx:
                                                                                                  //NOTE: we are assuming here that the type cast to java.util.Map above would not fail.
                                                                                                  //If instead of a map some other object type corresponds to the key's value then a
                                                                                                  //ClassCastException would be thrown.
                                                        mapName != null && externalCtx != null? externalCtx.getMap(mapName): null;  //it doesn't make sense to not specify any map name
                                                                                                                                    //when using external context.
                if (map != null) {
                    if (key != null)  //Added support for returning the complete map if no key specified.
                        value = getValue(key, map);  //map.get(key);
                    else
                        value = map;  //return the complete map
                }
            //}
            return value;
        }

        //Does a nested lookup inside the map and tries to extract value corresponding to a hierarchy of keys.
        protected Object getValue(String keyHierarchy, java.util.Map<String, Object> map) {
            /*Object value = null;
            if (keyHierarchy != null && map != null) {
                String keyDelimiter = getKeyDelimiter();
                String[] keys = keyHierarchy.split(keyDelimiter == null? "\\.": keyDelimiter);  //if no custom delimiter specified then dot would be used as the default delimiter.
                Object _value = null;
                for (int i = 0; i < keys.length; i++) {
                    _value = map.get(keys[i]);
                    if (i == keys.length - 1) {
                        //last element
                        value = _value;
                    } else if (_value instanceof java.util.Map) {
                        try {
                            map = (java.util.Map<String, Object>) _value;
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
            return value;*/
            return UtilityFunctions.getValue(keyHierarchy, getKeyDelimiter(), map);
        }
    }

    public static class InternalMap extends Map {
        public static final String TAG_NAME = "internal-map";

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            super.doCompileAttributes(n);
            setAttribute(ATTRIBUTE_CONTEXT, INTERNAL_CONTEXT);  //set the map context to internal irrespective of the value defined in the source xml.
        }
    }

    public static class Json extends Map {
        public static final String TAG_NAME = "json";

        private static final String ATTRIBUTE_CONTAINER = "container";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_CONTAINER};

        private String getContainer(CompilationRuntimeContext compilationRuntimeContext) {
            String container = null;
            try {
                //attempt computation
                container = getAttribute(ATTRIBUTE_CONTAINER, compilationRuntimeContext);  //invoking the computed version
            } catch(XPathExpressionException xpee) {
                container = getAttribute(ATTRIBUTE_CONTAINER);  //return the uncomputed value as fallback.
            }
            return container;
        }

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            super.doCompileAttributes(n);

            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }
        }

        @Override
        public Object remove(String key, CompilationRuntimeContext compilationRuntimeContext) {
            Object value = null;
            MapOfMaps externalCtx = compilationRuntimeContext.getExternalContext();
            java.util.Map<String, Object> internalCtx = compilationRuntimeContext.getInternalContext();
            //if (key != null) {  //Let's proceed even if key is not specified as in that case we may be
                                  //interested in clearing the contents of the entire  map.
                String mapName = getContainer(compilationRuntimeContext);
                java.util.Map<String, Object> map = isInternalContext()?
                                                        null:  //we do not want to allow explicit removal of items from internal context map hence commented out below line and using null here
                                                        //mapName != null && internalCtx != null? (java.util.Map<String, Object>) internalCtx.get(mapName): internalCtx:
                                                                                                  //NOTE: we are assuming here that the type cast to java.util.Map above would not fail.
                                                                                                  //If instead of a map some other object type corresponds to the key's value then a
                                                                                                  //ClassCastException would be thrown.
                                                        mapName != null && externalCtx != null? externalCtx.getMap(mapName): null;  //it doesn't make sense to not specify any map name
                                                                                                                                    //when using external context.
                Object jsonObj = null;
                String mapKeyName = getName(compilationRuntimeContext);
                if (map != null && mapKeyName != null) {
                    jsonObj = map.get(mapKeyName);
                }

                java.util.Map<String, Object> jsonToMap = null;
                if (jsonObj instanceof String) {
                    javax.script.ScriptEngine scriptEngine = new javax.script.ScriptEngineManager().getEngineByName("javascript");  //using script engine of java 8
                    try {
                        jsonToMap = (java.util.Map<String, Object>) scriptEngine.eval("Java.asJSONCompatible(" +jsonObj+ ")");
                    } catch(Exception e) {
                        //ignore. null value would get returned.
                    }
                } else if (jsonObj instanceof java.util.Map) {
                    try {
                        jsonToMap = (java.util.Map<String, Object>) jsonObj;  //if it throws a class cast exception because of difference in types then let it be.
                    } catch(Exception e) {
                        //ignore. null value would get returned.
                    }
                }

                if (jsonToMap != null) {
                    if (key != null)
                        value = jsonToMap.remove(key);  //remove the specified key and return its value
                    else
                        jsonToMap.clear();  //clear the entire map
                }
            //}
            return value;
        }

        @Override
        public Object getValue(String key, CompilationRuntimeContext compilationRuntimeContext) {
            Object value = null;
            MapOfMaps externalCtx = compilationRuntimeContext.getExternalContext();
            java.util.Map<String, Object> internalCtx = compilationRuntimeContext.getInternalContext();
            //if (key != null) {  //Let's proceed even if key is not specified as in that case we may be
                                  //interested in returning the full json map which may then, e.g., be iterated using a 'loop' cu.
                String mapName = getContainer(compilationRuntimeContext);
                java.util.Map<String, Object> map = isInternalContext()?
                                                        mapName != null && internalCtx != null? (java.util.Map<String, Object>) internalCtx.get(mapName): internalCtx:
                                                                                                  //NOTE: we are assuming here that the type cast to java.util.Map above would not fail.
                                                                                                  //If instead of a map some other object type corresponds to the key's value then a
                                                                                                  //ClassCastException would be thrown.
                                                        mapName != null && externalCtx != null? externalCtx.getMap(mapName): null;  //it doesn't make sense to not specify any map name
                                                                                                                                    //when using external context.
                Object jsonObj = null;
                String mapKeyName = getName(compilationRuntimeContext);
                if (map != null && mapKeyName != null) {
                    jsonObj = map.get(mapKeyName);
                }

                java.util.Map<String, Object> jsonToMap = null;
                if (jsonObj instanceof String) {
                    javax.script.ScriptEngine scriptEngine = new javax.script.ScriptEngineManager().getEngineByName("javascript");  //using script engine of java 8
                    try {
                        jsonToMap = (java.util.Map<String, Object>) scriptEngine.eval("Java.asJSONCompatible(" +jsonObj+ ")");
                    } catch(Exception e) {
                        //ignore. null value would get returned.
                    }
                } else if (jsonObj instanceof java.util.Map) {
                    try {
                        jsonToMap = (java.util.Map<String, Object>) jsonObj;  //if it throws a class cast exception because of difference in types then let it be.
                    } catch(Exception e) {
                        //ignore. null value would get returned.
                    }
                }

                if (jsonToMap != null) {
                    if (key != null)  //Added support for returning the complete map if no key specified.
                        value = getValue(key, jsonToMap);
                    else
                        value = jsonToMap;  //return the complete map
                }
            //}
            return value;
        }
    }

    public static class On extends CompilationUnit {
        public static final String TAG_NAME = "on";

        //private static final String CHILDREN_XPATH = "./conditional | ./condition";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Conditional.TAG_NAME, Condition.TAG_NAME});

        private ICondition condition = null;

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (((cu instanceof Condition) || (cu instanceof Conditional)) && (condition == null)) {
                //we need just one 'condition' cu and in case of many defined, let's just retain the first instance that we got.
                condition = (ICondition) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName);
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            /*String idOrElseOfCondition = null;
            if (condition instanceof Condition) {
                idOrElseOfCondition = ((Condition) condition).getIdOrElse();
                if (idOrElseOfChild.equals(idOrElseOfCondition)) {
                    return (Condition) condition;
                }
            } else if (condition instanceof Conditional) {
                idOrElseOfCondition = ((Conditional) condition).getIdOrElse();
                if (idOrElseOfChild.equals(idOrElseOfCondition)) {
                    return (Conditional) condition;
                }
            }*/
            if (condition != null && idOrElseOfChild.equals(condition.getIdOrElse())) {
                return condition;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            ICondition[] conditionArray = null;
            if (condition instanceof Condition) {
                conditionArray = getElementAsUnitArray((Condition) condition, type);
            } else if (condition instanceof Conditional) {
                conditionArray = getElementAsUnitArray((Conditional) condition, type);
            }
            return (T[]) (conditionArray == null ? getZeroLengthArrayOfType(type) : conditionArray);
        }

        public boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                        throws XPathExpressionException {
            return condition != null ? condition.matches(compilationRuntimeContext) : true;
        }

        /**************************************************************************************/
        /***********Basically used while cloning a CU during inheritance processing************/
        On getClone() {
            On clonedOn = new On();
            clonedOn.copyAttributes(getAttributes());
            clonedOn.condition = condition;  //this is just a reference copy
            return clonedOn;
        }
        /**************************************************************************************/
    }

    public static class Group extends ExtensibleCompilationUnit<Group> {
        public static final String TAG_NAME = "group";

        public static enum GroupType {
            LIST ("list"),
            MAP ("map"),
            CUSTOM ("custom");

            private String type = "";
            private GroupType(String type) {
                this.type = type;
            }
            private String getTypeAsString() {
                return type;
            }
        }

        public static enum SerializationPolicy {
            ALL ("keyValue"),  //serializes both key and value
            ONLYVALUE ("value"),  //serializes only value and ignores the key
            ONLYKEY ("key"),  //serializes only key and ignores the value
            NONE ("none");  //doesn't serialize at all

            private String type = "";
            private SerializationPolicy(String type) {
                this.type = type;
            }
            private String getTypeAsString() {
                return type;
            }
        }

        private static final String ATTRIBUTE_NAME = "name";
        private static final String ATTRIBUTE_TYPE = "type";
        private static final String ATTRIBUTE_CUSTOM_BUILDER = "customBuilder";
        private static final String ATTRIBUTE_SELF_SERIALIZATION_POLICY = "selfSerializationPolicy";
        private static final String ATTRIBUTE_CHILD_SERIALIZATION_POLICY = "childSerializationPolicy";
        private static final String ATTRIBUTE_ESCAPE_QUOTES = "escapeQuotes";  //true value indicates that the single quotes needs to be escaped
                                                                               //from inside the final serialized value of the group.
        private static final String[] ATTRIBUTES = {ATTRIBUTE_NAME, ATTRIBUTE_TYPE, ATTRIBUTE_CUSTOM_BUILDER,
                                                    ATTRIBUTE_SELF_SERIALIZATION_POLICY,
                                                    ATTRIBUTE_CHILD_SERIALIZATION_POLICY,
                                                    ATTRIBUTE_ESCAPE_QUOTES};

        //private static final String CHILDREN_XPATH = "./set | ./group";
        //private static final String CHILDREN_XPATH2 = "./init";
        //private static final String CHILDREN_XPATH3 = "./on";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Set.TAG_NAME, Group.TAG_NAME, Init.TAG_NAME, Finally.TAG_NAME, On.TAG_NAME});

        private List<Set> sets = null;
        private List<Group> groups = null;
        private Init init = null;
        private Finally finallyy = null;
        private On on = null;  //'on' condition, if present, must be satisfied in order for this Group CU to execute.

        private GroupType groupType = GroupType.CUSTOM;  //defined a separate enum for 'group type' for
                                                            //performance/efficiency reasons. Otherwise we could
                                                            //have very well read the information directly by
                                                            //checking the value of ATTRIBUTE_TYPE.
        private SerializationPolicy[] serializationPolicies =
                                                    {SerializationPolicy.ALL,
                                                     SerializationPolicy.ALL};  //defined separate enum for
                                                                                 //'serialization policy' for
                                                                                 //performance/efficiency reasons.
                                                                                 //Otherwise we could have very
                                                                                 //well read the information directly
                                                                                 //by checking the value of respective
                                                                                 //serialization policy attributes.
                                                                                 //0th index holds the serialization
                                                                                 //policy of self. 1st index holds the
                                                                                 //serialization policy of children.
        private SerializationPolicy[] runtimeSerializationPolicies = serializationPolicies;  //the runtime
                                                                                              //serialization policies
                                                                                              //would be used at
                                                                                              //runtime to override the
                                                                                              //serialization policies
                                                                                              //of a child group by the
                                                                                              //parent group during
                                                                                              //serialization.
        private boolean escapeQuotes = false;  //defined a boolean variable for performance reasons.
                                               //Otherwise we could have very well read the information
                                               //directly by checking the value of the respective attribute.

        boolean isHeadless = false;  //marking a group as headless would affect its serialization scheme.
                                             //It's key would never be serialized irrespective of the value of it's self serialization policy.
                                             //Serialized representation of it's immediate children also would not be bound within bounds i.e. [] or {}.
                                             //Ideal candidates for headless groups are cu's representing loops.
                                             //The value of this variable should always be set internally and not based on attribute values.
                                             //Also it's setter method should not be provided.

        public final boolean isHeadless() {
            return isHeadless;
        }

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            super.doCompileAttributes(n);

            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }

            initPerformanceVariables();  //initialize some attribute variables for performance optimization
        }

        /**************** Pre-initialize some attribute variables for performance enhancement ****************/
        /*****************************************************************************************************/
        @Override
        protected void initPerformanceVariables() {
            super.initPerformanceVariables();

            //------------------------------------------------------------------------------
            //initialize variables for some string attributes for performance optimizations
            //------------------------------------------------------------------------------
            String typeTmp = getAttribute(ATTRIBUTE_TYPE);
            if (typeTmp == null || "".equals(typeTmp)) {
                groupType = GroupType.CUSTOM;
            } else if (GroupType.LIST.getTypeAsString().equalsIgnoreCase(typeTmp)) {
                groupType = GroupType.LIST;
            } else if (GroupType.MAP.getTypeAsString().equalsIgnoreCase(typeTmp)) {
                groupType = GroupType.MAP;
            } else {
                groupType = GroupType.CUSTOM;
            }

            setSerializationPolicy(false);  //set serialization policy for self
            setSerializationPolicy(true);  //set serialization policy for children
            runtimeSerializationPolicies = serializationPolicies;

            String escapeQuotesTmp = getAttribute(ATTRIBUTE_ESCAPE_QUOTES);
            if (escapeQuotesTmp == null || "".equals(escapeQuotesTmp)) {
                escapeQuotesTmp = "false";
            }
            escapeQuotes = "true".equalsIgnoreCase(escapeQuotesTmp);
            //------------------------------------------------------------------------------
            //------------------------------------------------------------------------------
        }
        /****************************************************************************************************/

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            sets = new LinkedList<Set>();  //since mostly we would be operating
                                           //sequentially so
                                           //LinkedList would be an optimal
                                           //choice
            groups = new LinkedList<Group>();  //since mostly we would be operating
                                           //sequentially so
                                           //LinkedList would be an optimal
                                           //choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            super.doAddCompiledUnit(tagName, cu);

            if (cu instanceof Set) {
                sets.add((Set) cu);
            } else if (cu instanceof Group) {
                groups.add((Group) cu);
            } else if ((cu instanceof Finally) && (finallyy == null)) {  //finally should be checked before init as it is a subclass of Init
                //we need just one 'finally' cu and in case of many defined, let's just retain the first instance that we got.
                finallyy = (Finally) cu;
            } else if ((cu instanceof Init) && (init == null)) {
                //we need just one 'init' cu and in case of many defined, let's just retain the first instance that we got.
                init = (Init) cu;
            } else if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return super.isChildTagRecognized(tagName) || RECOGNIZED_CHILD_TAGS.contains(tagName) ||
                                                          CompilationUnits.isAssignableFrom(Set.class, CompilationUnits.getCompilationClassForTag(tagName)) ||
                                                          //Set.class.isAssignableFrom(CompilationUnits.getCompilationClassForTag(tagName)) ||
                                                          CompilationUnits.isAssignableFrom(Group.class, CompilationUnits.getCompilationClassForTag(tagName));
                                                          //Group.class.isAssignableFrom(CompilationUnits.getCompilationClassForTag(tagName));
        }

        public GroupType getType() {
            return groupType;
        }

        private void setSerializationPolicy(boolean forChildren) {
            String serializationPolicy = getAttribute(forChildren ?
                                                        ATTRIBUTE_CHILD_SERIALIZATION_POLICY :
                                                        ATTRIBUTE_SELF_SERIALIZATION_POLICY);
            int index = forChildren ? 1 : 0;
            if (serializationPolicy == null || "".equals(serializationPolicy)) {
                serializationPolicies[index] = SerializationPolicy.ALL;
            } else if (SerializationPolicy.ONLYKEY.getTypeAsString().equalsIgnoreCase(serializationPolicy)) {
                serializationPolicies[index] = SerializationPolicy.ONLYKEY;
            }  else if (SerializationPolicy.ONLYVALUE.getTypeAsString().equalsIgnoreCase(serializationPolicy)) {
                serializationPolicies[index] = SerializationPolicy.ONLYVALUE;
            }  else if (SerializationPolicy.NONE.getTypeAsString().equalsIgnoreCase(serializationPolicy)) {
                serializationPolicies[index] = SerializationPolicy.NONE;
            } else {
                serializationPolicies[index] = SerializationPolicy.ALL;
            }
        }

        public SerializationPolicy getSelfSerializationPolicy() {
            return serializationPolicies[0];
        }

        public SerializationPolicy getChildSerializationPolicy() {
            return serializationPolicies[1];
        }

        public SerializationPolicy getSelfSerializationPolicyRuntime() {
            return runtimeSerializationPolicies[0];
        }

        public SerializationPolicy getChildSerializationPolicyRuntime() {
            return runtimeSerializationPolicies[1];
        }

        /**
         * This method is used to change the value of runtime serialization policy by the group serializers. This
         * method is not for general use and shouldn't be called arbitrarily.
         * @param serPolicy
         */
        public void setSelfSerializationPolicyRuntime(SerializationPolicy serPolicy) {
            runtimeSerializationPolicies[0] = serPolicy;
        }

        /**
         * This method is used to change the value of runtime serialization policy by the group serializers. This
         * method is not for general use and shouldn't be called arbitrarily.
         * @param serPolicy
         */
        public void setChildSerializationPolicyRuntime(SerializationPolicy serPolicy) {
            runtimeSerializationPolicies[1] = serPolicy;
        }

        @Override
        public String getIdOrElse() {
            String idOrName = getId();
            idOrName = "".equals(idOrName) ? null : idOrName;
            if (idOrName == null) {
                idOrName = getAttribute(ATTRIBUTE_NAME);
                idOrName = "".equals(idOrName) ? null : idOrName;
            }
            return idOrName;
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            ICompilationUnit cu = super.getChild(idOrElseOfChild);
            if (cu != null) {  //matching child found in base class
                return cu;
            }
            if (init != null) {
                if (idOrElseOfChild.equals(init.getIdOrElse())) {
                    return init;
                }
            }
            if (finallyy != null) {
                if (idOrElseOfChild.equals(finallyy.getIdOrElse())) {
                    return finallyy;
                }
            }
            if (on != null && idOrElseOfChild.equals(on.getIdOrElse())) {
                return on;
            }
            if (sets != null) {
                for (Set set : sets) {
                    if (idOrElseOfChild.equals(set.getIdOrElse())) {
                        return set;
                    }
                }
            }
            if (groups != null) {
                for (Group grp : groups) {
                    if (idOrElseOfChild.equals(grp.getIdOrElse())) {
                        return grp;
                    }
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            T[] superChildrenByType = super.getChildren(type);
            if (superChildrenByType != null) {
                return (T[]) superChildrenByType;
            }
            Set[] setsArray = areMatchingTypes(Set.class, type) ? getElementsFromList(sets, new Set[0]) : null;
            if (setsArray != null) {
                return (T[]) setsArray;
            }
            Group[] groupsArray = areMatchingTypes(Group.class, type) ?
                                                              getElementsFromList(groups, new Group[0]) : null;
            if (groupsArray != null) {
                return (T[]) groupsArray;
            }
            Finally[] finallyArray = getElementAsUnitArray(finallyy, type);
            if (finallyArray != null) {
                return (T[]) finallyArray;
            }
            Init[] initArray = getElementAsUnitArray(init, type);
            if (initArray != null) {
                return (T[]) initArray;
            }
            On[] onArray = getElementAsUnitArray(on, type);
            return (T[]) (onArray == null ? getZeroLengthArrayOfType(type) : onArray);
        }

        /********************************************************************************************************/
        /*****************************************Finder Methods*************************************************/
        private Group findChildGroup(String idOrElse) {
            return findChild(idOrElse, groups);
        }

        private Set findChildSet(String idOrElse) {
            return findChild(idOrElse, sets);
        }

        private <C extends ICompilationUnit, T extends List<C>> C findChild(String idOrElse, T childList) {
            if (idOrElse == null || "".equals(idOrElse)) {
                return null;
            }
            for (C child : childList) {
                String subChildIdOrElse = child.getIdOrElse();
                if (idOrElse.equals(subChildIdOrElse)) {
                    return child;
                }
            }
            return null;
        }
        /********************************************************************************************************/
        /********************************************************************************************************/

        /********************************************************************************************************/
        /************************************Inheritance Related Methods*****************************************/
        //used to create a new instance while cloning. We need a separate method for this so that the cloning of subclassing can
        //create their own instance type but still reuse the cloning logic in the super class.
        protected Group newInstance() {
            return new Group();
        }

        //deep clone
        protected Group getClone() {
            Group clonedGrp = newInstance();
            clonedGrp.setNodeContext(getNodeContext());
            clonedGrp.copyAttributes(getAttributes());
            clonedGrp.copyExtensions(getExtensions());
            clonedGrp.markExtensionsAsProcessed(areExtensionsMarkedAsProcessed());
            if (init != null) {
                clonedGrp.init = init.getClone();
            }
            if (finallyy != null) {
                clonedGrp.finallyy = finallyy.getClone();
            }
            if (on != null) {
                clonedGrp.on = on.getClone();
            }

            /*clonedGrp.groupType = groupType;
            clonedGrp.serializationPolicies = serializationPolicies;
            clonedGrp.runtimeSerializationPolicies = serializationPolicies;*/
            clonedGrp.initPerformanceVariables();  //this method must be called after all attributes have been set inside the cloned unit as
                                                   //the initilization of performance variables is based on values of some attributes.

            clonedGrp.groups = new LinkedList<Group>();
            clonedGrp.groups.addAll(groups);
            clonedGrp.sets = new LinkedList<Set>();
            clonedGrp.sets.addAll(sets);
            clonedGrp.isHeadless = isHeadless;
            return clonedGrp;
        }

        @Override
        protected Group doExtend(CompilationRuntimeContext compilationRuntimeContext,
                                                        CompiledTemplatesRegistry mctr)
                                                                throws XPathExpressionException {
            /* Should the init be called at the time of extending the unit??? I think not because if init definition is specified
               in separate xml which is inherited in the super xml then at runtime we may run into issues (exceptions) if there are
               any units which are to set the properties in a map using the 'set' cu with override set to false. Moreover it doesn't
               seem necessary to call init during inheritance processing as it won't serve any good purpose.
            if (init != null) {
                //let's execute the initializer block first (if any defined)
                init.execute(compilationRuntimeContext);
            }
            */
            Group extendedUnit = this;
            boolean canMarkExtensionsAsProcessed = !hasConditionalExtends();
            for (Extends extension : getExtensions()) {
                Extends.ExtensionOperation opType = extension.getExtensionOperation();
                Extends.ExtensionScope extScope = extension.getExtensionScope();
                Object baseUnitPath = extension.getValue(compilationRuntimeContext);
                if (baseUnitPath == null) {
                    continue;
                }
                Group baseUnit = mctr.getCompilationUnit(baseUnitPath.toString(), Group.class);
                extendedUnit = Group.doExtendFromBase(extendedUnit,
                                            baseUnit.extend(compilationRuntimeContext, mctr),
                                            opType,
                                            extScope,
                                            true,
                                            compilationRuntimeContext,
                                            mctr);
                canMarkExtensionsAsProcessed = canMarkExtensionsAsProcessed &
                                                        baseUnit.areExtensionsMarkedAsProcessed() &
                                                            extendedUnit.areExtensionsMarkedAsProcessed();
            }
            canMarkExtensionsAsProcessed = canMarkExtensionsAsProcessed &
                                                   extendedUnit.doExtendImmediateChildren(compilationRuntimeContext, mctr);  //Changed from doExtendImmediateChildren(...) to
                                                                                                                             //extendedUnit.doExtendImmediateChildren(...)
            extendedUnit.markExtensionsAsProcessed(canMarkExtensionsAsProcessed);
            if (!canMarkExtensionsAsProcessed) {
                markExtensionsAsProcessed(false);
            }
            return extendedUnit;
        }

        private boolean doExtendImmediateChildren(CompilationRuntimeContext compilationRuntimeContext,
                                                  CompiledTemplatesRegistry mctr) throws XPathExpressionException {
            boolean allGroupsCacheable = true;
            java.util.Map<Group, Group> extendedGrpMap = new HashMap<Group, Group>();
            if (groups != null) {
                for (Group group : groups) {
                    Group extendedGrp = group.extend(compilationRuntimeContext, mctr);
                    if (extendedGrp.areExtensionsMarkedAsProcessed()) {
                    	extendedGrpMap.put(group, extendedGrp);
                    }
                    allGroupsCacheable = allGroupsCacheable & extendedGrp.areExtensionsMarkedAsProcessed();
                }
            }
            for (Entry<Group, Group> entry : extendedGrpMap.entrySet()) {
                int index = groups.indexOf(entry.getKey());
                groups.add(index, entry.getValue());
                groups.remove(index + 1);
            }
            return allGroupsCacheable;
        }

        private static Group doExtendFromBase(Group superUnit,
                                              Group baseUnit,
                                              Extends.ExtensionOperation opType,
                                              Extends.ExtensionScope extScope,
                                              boolean isThroughExplicitInheritance,
                                              CompilationRuntimeContext compilationRuntimeContext,
                                              CompiledTemplatesRegistry mctr) throws XPathExpressionException {
            if (!isThroughExplicitInheritance) {
                //we need to check for id's in order to process inheritance
                String thisIdentifier = superUnit.getIdOrElse(compilationRuntimeContext);
                String thatIdentifier = baseUnit.getIdOrElse(compilationRuntimeContext);
                if (thisIdentifier == null || !thisIdentifier.equals(thatIdentifier)) {
                    return superUnit;
                }
            }

            Group thisClonedGrp = superUnit.getClone();
            if (opType == Extends.ExtensionOperation.REPLACE) {
                return thisClonedGrp;
            } else if (opType == Extends.ExtensionOperation.MERGE) {
                //merge the initializer blocks
                if (baseUnit.init != null) {
                    if (thisClonedGrp.init == null) {
                        thisClonedGrp.init = baseUnit.init.getClone();  //new Init();  //we would be interested in cloning here as that way the base attributes also would get inherited.
                    } else {
                        //thisClonedGrp.init.copySets(baseUnit.init.getSets());
                        thisClonedGrp.init.copyExecutables(baseUnit.init.getExecutables());  //we would just be interested in copying the executables without affecting the attributes in super class.
                    }
                }

                //merge the finalizer blocks
                if (baseUnit.finallyy != null) {
                    if (thisClonedGrp.finallyy == null) {
                        thisClonedGrp.finallyy = baseUnit.finallyy.getClone();  //new Finally();  //we would be interested in cloning here as that way the base attributes also would get inherited.
                    } else {
                        //thisClonedGrp.finallyy.copySets(baseUnit.finallyy.getSets());
                        thisClonedGrp.finallyy.copyExecutables(baseUnit.finallyy.getExecutables());  //we would just be interested in copying the executables
                                                                                                     //without affecting the attributes in super class.
                    }
                }

                //default inheritance strategy for the On condition would be to override.
                if (thisClonedGrp.on == null && baseUnit.on != null) {
                    //use the on condition defined inside the base unit
                    thisClonedGrp.on = baseUnit.on.getClone();
                }

                //merge the Set CUs
                for (Set thatSubSet : baseUnit.sets) {
                    Set thisSubSet = thisClonedGrp.findChildSet(thatSubSet.getIdOrElse(compilationRuntimeContext));
                    if (thisSubSet == null) {
                        //the baseUnit contains a Set CU which is not overridden and thus it needs to be added
                        thisClonedGrp.sets.add(thatSubSet.extend(compilationRuntimeContext, mctr));
                    }
                }

                //merge the Group CUs
                for (Group thatSubGroup : baseUnit.groups) {
                    //Group thisSubGroup = thisClonedGrp.findChildGroup(Group.getGroupIdOrName(thatSubGroup));
                    Group thisSubGroup = thisClonedGrp.findChildGroup(thatSubGroup.getIdOrElse(compilationRuntimeContext));
                    if (thisSubGroup != null) {
                        //groups require merging
                        Group extendedGrp = Group.doExtendFromBase(
                                                       thisSubGroup.extend(compilationRuntimeContext, mctr),
                                                       thatSubGroup.extend(compilationRuntimeContext, mctr),
                                                       opType,
                                                       extScope,
                                                       false,
                                                       compilationRuntimeContext,
                                                       mctr);
                        int indexOfGrpToBeExtended = thisClonedGrp.groups.indexOf(thisSubGroup);
                        thisClonedGrp.groups.add(indexOfGrpToBeExtended, extendedGrp);
                        thisClonedGrp.groups.remove(indexOfGrpToBeExtended + 1);  //the old group would have moved
                                                                                  //to (index + 1)th location by
                                                                                  //now
                    } else {
                        if (!thatSubGroup.isIdOrElseDynamic()) {
                            //the baseUnit contains a group which is not overridden and thus it needs to be added
                            thisClonedGrp.groups.add(thatSubGroup.extend(compilationRuntimeContext, mctr));
                        }
                    }

                    if (thatSubGroup.isIdOrElseDynamic()) {
                        //if the child group inside baseUnit has dynamic id then we can't mark the extensions of
                        //thisClonedGrp (and thisSubGroup) as processed since any future inheritance processing may
                        //result in a different group structure.
                        thisClonedGrp.markExtensionsAsProcessed(false);
                        if (thisSubGroup != null) {
                            thisSubGroup.markExtensionsAsProcessed(false);
                        }
                    }
                }

                //The following method would be useful for subclass implementations of Group to extend the additional cu's that they might have.
                //Also this method gets called only for the extension strategy 'merge' hence the logic should focus on merging the additional cu's.
                //Also the method gets called over the cloned instance so there is no need to worry about modifying the state of the original object.
                thisClonedGrp.doExtendAdditionalUnitsFromBaseUsingMerge(baseUnit, compilationRuntimeContext);

                //check if base attributes also need to be inherited.
                if (extScope == Extends.ExtensionScope.FULL) {
                    //inherit attributes from the base unit
                    Properties baseAttributes = baseUnit.getAttributes();
                    if (baseAttributes != null && baseAttributes.size() > 0) {
                        //there are attributes to be copied from base.
                        for (String key: baseAttributes.stringPropertyNames()) {
                            if (!ATTRIBUTE_ID.equals(key)) {  //attributes other than the one corresponding to id are allowed inheritance
                                thisClonedGrp.setAttributeIffNew(key, baseAttributes.getProperty(key));
                            }
                        }
                        //the attributes inside thisCloneGrp might have been updated by now.
                        //Let's give a chance to the performance variables to reinitialize.
                        thisClonedGrp.initPerformanceVariables();
                    }
                }
            }
            return thisClonedGrp;
        }

        //This method would be useful for subclass implementations of Group to extend the additional cu's that they might have.
        protected void doExtendAdditionalUnitsFromBaseUsingMerge(Group baseUnit, CompilationRuntimeContext compilationRuntimeContext) {
            //nothing to do here.
            //this method should be overridden by sub classes to perform merging of their additional cu's which are not known to this class.
        }
        /********************************************************************************************************/
        /********************************************************************************************************/

        public static enum ReturnType {
            JSON,
            TEST_MAP,
            OBJECT;
        }

        /************************ Start - initialization and finalization related methods ************************/
        /*********************************************************************************************************/
        /*********************************************************************************************************/
        //This method executes the initializer block if any. Execution of initializer block is provided in a
        //separate protected method to allow some cu's to change its sequence and frequency. E.g. 'loop' cu's
        //may want that the initializer block be executed only once and that too before any looping starts -
        //they can then override this method to not do anything and explicitly call super.doInit(...) at a
        //different place just before starting the loop iterations.
        protected void doInit(CompilationRuntimeContext compilationRuntimeContext)
                                                              throws XPathExpressionException {
            if (init != null) {
                //let's execute the initializer block (if any defined)
                init.execute(compilationRuntimeContext);
            }
        }

        //This method executes the finalizer block if any. Execution of finalizer block is provided in a
        //separate protected method to allow some cu's to change its sequence and frequency. E.g. 'loop' cu's
        //may want that the finalizer block be executed only once and that too after the loop iterations ends -
        //they can then override this method to not do anything and explicitly call super.doFinally(...) at a
        //different place after the loop iterations just finishes. 
        protected void doFinally(CompilationRuntimeContext compilationRuntimeContext) 
                                                                 throws XPathExpressionException {
            if (finallyy != null) {
                //let's execute the finalizer block (if any defined)
                finallyy.execute(compilationRuntimeContext);
            }
        }
        /************************* End - initialization and finalization related methods *************************/
        /*********************************************************************************************************/

        /************************************************************************************/
        /************* Start - pre and post processing methods of getValue(...) *************/
        /************************************************************************************/
        /************************************************************************************/
        @Override
        protected boolean satisfies(CompilationRuntimeContext compilationRuntimeContext) 
                                                                     throws XPathExpressionException {
            return on == null || on.satisfies(compilationRuntimeContext);
        }
        
        @Override 
        protected Object _noValue() {                         
            return "";  //For group it makes more iense to return a zero length string instead of null value.
                        //(this method gets called when the on condition remains unsatisfied for this cu).
        }       
            
        @Override
        protected void preGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            super.preGetValue(compilationRuntimeContext); 
            doInit(compilationRuntimeContext);  //let's attempt to execute the initializer block
        }
        
        @Override 
        protected void postGetValue(Object value, CompilationRuntimeContext compilationRuntimeContext)
                                                                                   throws XPathExpressionException {
            try {
                super.postGetValue(value, compilationRuntimeContext);
            }
            finally {
                //even if any exception occurs while executing super.postGetValue(...) the finalizer
                //block should get executed.
                doFinally(compilationRuntimeContext);  //let's attempt to execute the finalizer block
            }
        }   
        /************************************************************************************/
        /************** End - pre and post processing methods of getValue(...) **************/
        /************************************************************************************/

        public Object build(CompilationRuntimeContext compilationRuntimeContext,
                                    ReturnType returnType) throws XPathExpressionException {
            if (returnType == ReturnType.JSON) {
                //return doGetValue(compilationRuntimeContext);
                return getValue(compilationRuntimeContext);
            } else if (returnType == ReturnType.TEST_MAP) {
                /* Commented this block in favour of the satisfies(...) and _noValue(...) methods.

                if (on != null && !on.satisfies(compilationRuntimeContext)) {
                    return "";  //we need not return any value as there was an 'on' condition defined that remains
                                //unsatisfied. Also for group it makes sense to return a zero length string and not
                                //the null value.
                }

                doInit(compilationRuntimeContext);
                */

                if (!satisfies(compilationRuntimeContext)) {
                    return _noValue();
                }
                preGetValue(compilationRuntimeContext);

                Object value = null;
                try {
                    value = CompilationUnitsSerializationFactory.getGroupSerializer(
                                CompilationUnitsSerializationFactory.SerializerType.MAP).
                                                   serialize(compilationRuntimeContext, this);
                    return escapeQuotes && value instanceof String? ((String) value).replaceAll("'", "\\\\\\\\'"): value;
                } finally {
                    //doFinally(compilationRuntimeContext);
                    postGetValue(value, compilationRuntimeContext);
                }
            } else {
                //return doGetValue(compilationRuntimeContext);  //TODO return non json value. Is this really required?
                return getValue(compilationRuntimeContext);
            }
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                                throws XPathExpressionException {
            /* Commenting this block because now the EvaluableCompilationUnit's getValue(...) method
               implicitly checks for satisfaction and also invokes preGetValue(...) before calling doGetValue. The
               respective methods are overridden in this class - satisfies(...) checks for the 'on' condition and
               doInit(...) is called inside the overridden version of preGetValue(...) of this cu.

            //I decide to check the 'on' condition even before processing the init block. If however it makes
            //sense to use one or more of the initialized variables inside the 'on' condition then move this
            //'on' block below the 'init' block that follows it.
            if (on != null && !on.satisfies(compilationRuntimeContext)) {
                return "";  //we need not return any value as there was an 'on' condition defined that remains
                            //unsatisfied. Also for group it makes sense to return a zero length string and not
                            //the null value.
            }

            //let's attempt to execute the initializer block first
            doInit(compilationRuntimeContext);
            */

            //in the below serializer call we are just passing the reference to 'this' group as we assume that the
            //serialization of this group won't be done in a multi threaded environment. If however we intend to
            //support serialization in multi threaded environment then instead of passing 'this' we should pass
            //value returned by getClone() as the last parameter. This Group CU is otherwise thread safe but exposure
            //of methods to set the 'runtime serialization policy' makes it unsuitable for serialization in multi
            //threaded environment as is and thus mandates the need of using getClone() in multi threaded environment.
            /* try {  //removed try/finally block as now the doFinally(...) would implicitly get called
                      //inside the postGetValue(...) overridden by this cu.
                return CompilationUnitsSerializationFactory.getGroupSerializer(
                                CompilationUnitsSerializationFactory.SerializerType.JSON).
                                                           serialize(compilationRuntimeContext, this);
            } finally {
                doFinally(compilationRuntimeContext);
            } */
            Object value = CompilationUnitsSerializationFactory.getGroupSerializer(
                                CompilationUnitsSerializationFactory.SerializerType.JSON).
                                                           serialize(compilationRuntimeContext, this);
            return escapeQuotes && value instanceof String? ((String) value).replaceAll("'", "\\\\\\\\'"): value;
        }

        /* For group we are not interested in performing any transformations as such operations don't really make
           sense for a group. Overriding the method to return the input value as is without any transformations being
           performed
        @Override
        protected Object getEvaluatedValue(CompilationRuntimeContext compilationRuntimeContext,
                Object thisValue) throws XPathExpressionException {
            return thisValue;
        } */
    }

    //Headless group
    public static class HeadlessGroup extends Group {
        public static final String TAG_NAME = "headless-group";

        @Override
        public void doCompileAttributes(Node n) throws XPathExpressionException {
            isHeadless = true;
            super.doCompileAttributes(n);
        }

        /********************************** Cloning related methods**********************************/
        /****** Overridden to ensure cloning of a headless group also returns a headless group ******/
        @Override
        protected HeadlessGroup newInstance() {
            return new HeadlessGroup();
        }

        @Override
        //deep clone
        protected HeadlessGroup getClone() {
            HeadlessGroup clonedHG = (HeadlessGroup) super.getClone();
            clonedHG.isHeadless = isHeadless;
            return clonedHG;
        }
        /********************************************************************************************/
    }

    //Initialization handler for use basically inside a group.
    public static class Init extends CompilationUnit {
        public static final String TAG_NAME = "init";

        //private static final String CHILDREN_XPATH = "./set";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Set.TAG_NAME, Unset.TAG_NAME});

        //private List<Set> sets = null;
        private List<IExecutable> executables = null;

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            //sets = new LinkedList<Set>();  //since mostly we would be operating
                                                   //sequentially so
                                                   //LinkedList would be an optimal
                                                   //choice
            executables = new LinkedList<IExecutable>();  //since mostly we would be operating
                                                   //sequentially so
                                                   //LinkedList would be an optimal
                                                   //choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            /*if (cu instanceof Set) {
                sets.add((Set) cu);
            }*/
            if (cu instanceof IExecutable) {
                executables.add((IExecutable) cu);
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            //return RECOGNIZED_CHILD_TAGS.contains(tagName);
            //return CompilationUnits.getCompilationClassForTag(tagName) instanceof IExecutable;  //let's use the class type associated with
                                                                                                   //tag since any type of executable can get added.
            //return true;  //TODO remove blanket acceptance
            return RECOGNIZED_CHILD_TAGS.contains(tagName) ||
                   //IExecutable.class.isAssignableFrom(CompilationUnits.getCompilationClassForTag(tagName));
                   CompilationUnits.isAssignableFrom(IExecutable.class, CompilationUnits.getCompilationClassForTag(tagName));
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            /*if (sets != null) {
                for (Set set : sets) {
                    if (idOrElseOfChild.equals(set.getIdOrElse())) {
                        return set;
                    }
                }
            }*/
            if (executables != null) {
                for (IExecutable executable : executables) {
                    //CompilationUnit ecu = (CompilationUnit) executable;
                    if (idOrElseOfChild.equals(executable.getIdOrElse())) {
                        return executable;
                    }
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            /*Set[] setsArray = areMatchingTypes(Set.class, type) ? getElementsFromList(sets, new Set[0]) : null;
            return (T[]) (setsArray == null ? getZeroLengthArrayOfType(type) : setsArray);*/
            IExecutable[] executablesArray = areMatchingTypes(IExecutable.class, type) ? getElementsFromList(executables, new IExecutable[0]) : null;
            return (T[]) (executablesArray == null ? getZeroLengthArrayOfType(type) : executablesArray);
        }

        public void execute(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            /*if (sets == null) {
                return;
            }
            for (Set set : sets) {
                set.execute(compilationRuntimeContext);
            }*/
            if (executables == null) {
                return;
            }
            for (IExecutable executable : executables) {
                executable.execute(compilationRuntimeContext);
            }
        }

        /**************************************************************************************/
        /***********Basically used while cloning a CU during inheritance processing************/
        /*void copySets(List<Set> sets) {
            if (this.sets == null) {
                this.sets = new LinkedList<Set>();
            }
            this.sets.addAll(sets);
        }

        List<Set> getSets() {
            return sets;
        }

        Init getClone() {
            Init clonedInit = new Init();
            clonedInit.copyAttributes(getAttributes());
            clonedInit.sets = new LinkedList<Set>();
            clonedInit.sets.addAll(getSets());
            return clonedInit;
        }*/
        void copyExecutables(List<IExecutable> executables) {
            copyExecutables(executables, false);
        }

        void copyExecutables(List<IExecutable> executables, boolean duplicatesOk) {
            if (executables == null) {
                return;
            }
            if (this.executables == null) {
                this.executables = new LinkedList<IExecutable>();
            }
            if (duplicatesOk) {
                this.executables.addAll(executables);  //fast copy using addAll
            } else {
                for (IExecutable executable: executables) {
                    if (getChild(executable.getIdOrElse()) == null) {
                        this.executables.add(executable);
                    }
                }
            }
        }

        List<IExecutable> getExecutables() {
            return executables;
        }

        Init getClone() {
            Init clonedInit = newInstance();
            clonedInit.copyAttributes(getAttributes());
            clonedInit.executables = new LinkedList<IExecutable>();
            clonedInit.executables.addAll(getExecutables());
            return clonedInit;
        }

        //used to create a new instance while cloning. We need a separate method for this so that the cloning of subclassing can
        //create their own instance type but still reuse the cloning logic in the super class.
        protected Init newInstance() {
            return new Init();
        }
        /**************************************************************************************/
    }

    public static class Finally extends Init {
        public static final String TAG_NAME = "finally";

        @Override
        protected Finally newInstance() {
            return new Finally();
        }

        @Override
        Finally getClone() {
            return (Finally) super.getClone();
        }
    }

    public static class Select extends EvaluableCompilationUnit implements IEvaluable {
        public static final String TAG_NAME = "select";

        //private static final String CHILDREN_XPATH = "./using";
        //private static final String CHILDREN_XPATH2 = "./conditional";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Using.TAG_NAME, Conditional.TAG_NAME});

        private Using using = null;
        private List<Conditional> conditionals = null;

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            conditionals = new LinkedList<Conditional>();  //since mostly we would be operating
                                                           //sequentially so
                                                           //LinkedList would be an optimal
                                                           //choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (cu instanceof Conditional) {
                conditionals.add((Conditional) cu);
            } else if ((cu instanceof Using) && (using == null)) {
                //we need just one 'using' cu and in case of many defined, let's just retain the first instance that we got.
                using = (Using) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName);
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (using != null && idOrElseOfChild.equals(using.getIdOrElse())) {
                return using;
            }
            if (conditionals != null) {
                for (Conditional conditional : conditionals) {
                    if (idOrElseOfChild.equals(conditional.getIdOrElse())) {
                        return conditional;
                    }
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            Conditional[] conditionalArray = areMatchingTypes(Conditional.class, type) ?
                                                       getElementsFromList(conditionals, new Conditional[0]) : null;
            if (conditionalArray != null) {
                return (T[]) conditionalArray;
            }
            Using[] usingArray = getElementAsUnitArray(using, type);
            return (T[]) (usingArray == null ? getZeroLengthArrayOfType(type) : usingArray);
        }

        @Override
        protected void preGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            if (using != null) {
                java.util.Map<String, Object> originalInternalContext = using.initAndSetAllVariables(compilationRuntimeContext);
            }
            super.preGetValue(compilationRuntimeContext);
        }

        //appropriately builds and returns the value
        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                        throws XPathExpressionException {
            /* Commented this because now the 'using' gets initialized inside the overridden
               version of preGetValue(...) of this cu.

            if (using != null) {
                java.util.Map<String, Object> originalInternalContext = using.initAndSetAllVariables(compilationRuntimeContext);
            }
            */

            Object value = null;
            for (Conditional conditional : conditionals) {
                value = conditional.matches(compilationRuntimeContext) ?
                                            conditional.getValue(compilationRuntimeContext) : null;
                if (value != null) {
                    break;
                }
            }

            //Abort support during internal processing
            if (value == null && compilationRuntimeContext.isAbortIfNotSatisfy()) {
                //if the value is null then we would just assume that no condition has satisfied even though
                //it might have so happened that some condition was satisfied but the value returned by it
                //was null. This assumption should not be a problem as the intention of this Select CU is to
                //attempt to return a non null value after analyzing all available conditionals. If, however,
                //the exceptional abort needs to be done strictly iff the matches criterion of all conditionals
                //actually failed then introduce a new boolean flag and set its value to the OR of all
                //conditionals that were evaluated before breaking out of the above loop and then use the value
                //of that boolean flag to decide whether to enter this block or not.
                throw new RuntimeException("No available conditions satisfied.");
            }
            return value;
        }
    }

    public static class Extends extends Select implements IEvaluable {
        public static final String TAG_NAME = "extends";

        public static enum ExtensionOperation {
            MERGE ("merge"),
            REPLACE ("replace");

            private String type = "";
            private ExtensionOperation(String type) {
                this.type = type;
            }
            private String getTypeAsString() {
                return type;
            }
        }
        public static enum ExtensionScope {
            CHILDREN ("children"),
            FULL ("full");  //both attributes and children

            private String scope = "";
            private ExtensionScope(String scope) {
                this.scope = scope;
            }
            private String getScopeAsString() {
                return scope;
            }
        }
        private static final ExtensionOperation DEFAULT_EXTENSION_OPERATION = ExtensionOperation.MERGE;
        private static final ExtensionScope DEFAULT_EXTENSION_SCOPE = ExtensionScope.FULL;

        private static final String ATTRIBUTE_EXTEND_OP = "operation";
        private static final String ATTRIBUTE_EXTEND_SCOPE = "scope";
        private static final String ATTRIBUTE_DEFAULT_VALUE = "default";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_EXTEND_OP, ATTRIBUTE_EXTEND_SCOPE, ATTRIBUTE_DEFAULT_VALUE};

        private ExtensionOperation extOp = DEFAULT_EXTENSION_OPERATION;
        private ExtensionScope extScope = DEFAULT_EXTENSION_SCOPE;

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            super.doCompileAttributes(n);
            
            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }

            String extOpAsStr = getAttribute(ATTRIBUTE_EXTEND_OP);
            if (extOpAsStr == null || "".equals(extOpAsStr)) {
                extOp = DEFAULT_EXTENSION_OPERATION;
            } else if (ExtensionOperation.REPLACE.getTypeAsString().equalsIgnoreCase(extOpAsStr)) {
                extOp = ExtensionOperation.REPLACE;
            } else if (ExtensionOperation.MERGE.getTypeAsString().equalsIgnoreCase(extOpAsStr)) {
                extOp = ExtensionOperation.MERGE;
            } else {
                extOp = DEFAULT_EXTENSION_OPERATION;
            }

            String extScopeAsStr = getAttribute(ATTRIBUTE_EXTEND_SCOPE);
            if (ExtensionScope.CHILDREN.getScopeAsString().equalsIgnoreCase(extScopeAsStr)) {
                extScope = ExtensionScope.CHILDREN;;
            } else if (ExtensionScope.FULL.getScopeAsString().equalsIgnoreCase(extScopeAsStr)) {
                extScope = ExtensionScope.FULL;
            } else {
                extScope = DEFAULT_EXTENSION_SCOPE;
            }
        }

        boolean hasConditionals() {
            return getChildren(Conditional.class).length != 0;
        }

        public ExtensionOperation getExtensionOperation() {
            return extOp;
        }

        public ExtensionScope getExtensionScope() {
            return extScope;
        }

        @Override
        protected Object getDefaultValue() {
            return getAttribute(ATTRIBUTE_DEFAULT_VALUE);
        }
    }

    public static class Using extends CompilationUnit {
        public static final String TAG_NAME = "using";

        //private static final String CHILDREN_XPATH = "./valueof | ./get";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{ValueOf.TAG_NAME, Get.TAG_NAME});

        //private List<ValueOf> valueOfs = null;
        private List<IEvaluable> evaluables = null;

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            //no attributes to compile.
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            /* valueOfs = new LinkedList<ValueOf>();  //since mostly we would be operating
                                                   //sequentially so
                                                   //LinkedList would be an optimal
                                                   //choice */
            evaluables = new LinkedList<IEvaluable>();  //since mostly we would be operating
                                                     //sequentially so
                                                     //LinkedList would be an optimal
                                                     //choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            /* if ((cu instanceof ValueOf) || (cu instanceof Get)) {
                valueOfs.add((ValueOf) cu);
            } */
            if (cu instanceof IEvaluable) {
                evaluables.add((IEvaluable) cu);
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName) ||
                                       //IEvaluable.class.isAssignableFrom(CompilationUnits.getCompilationClassForTag(tagName));
                                       CompilationUnits.isAssignableFrom(IEvaluable.class, CompilationUnits.getCompilationClassForTag(tagName));
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            /* if (valueOfs != null) {
                for (ValueOf valueOf : valueOfs) {
                    if (idOrElseOfChild.equals(valueOf.getIdOrElse())) {
                        return valueOf;
                    }
                }
            } */
            if (evaluables != null) {
                for (IEvaluable evaluable : evaluables) {
                    if (idOrElseOfChild.equals(evaluable.getIdOrElse())) {
                        return evaluable;
                    }
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            /* ValueOf[] valueOfsArray = areMatchingTypes(ValueOf.class, type) ?
                                                    getElementsFromList(valueOfs, new ValueOf[0]) : null;
            return (T[]) (valueOfsArray == null ? getZeroLengthArrayOfType(type) : valueOfsArray); */

            IEvaluable[] evaluablesArray = areMatchingTypes(IEvaluable.class, type) ?
                                                    getElementsFromList(evaluables, new IEvaluable[0]) : null;
            return (T[]) (evaluablesArray == null ? getZeroLengthArrayOfType(type) : evaluablesArray);
        }

        public java.util.Map<String, Object> initAndGetAllVariables(
                                                    CompilationRuntimeContext compilationRuntimeContext)
                                                                                    throws XPathExpressionException {
            java.util.Map<String, Object> mapTmp = new HashMap<String, Object>();
            for (IEvaluable evaluable: evaluables/*ValueOf vof : valueOfs*/) {
                try {
                    compilationRuntimeContext.setAbortIfNotSatisfy(true);  //enable the abort flag
                    Object value = /*vof*/evaluable.getValue(compilationRuntimeContext);
                    mapTmp.put(/*vof*/evaluable.getId(), value);
                } catch (RuntimeException re) {
                    //this was just for indication that the value was not set because the 'on' condition was
                    //not satisfied even when we managed to find a value of the key inside one of the maps.
                    //no specific action needed.
                } finally {
                    compilationRuntimeContext.setAbortIfNotSatisfy(false);  //reset back the abort flag to false
                }
            }
            return mapTmp;
        }

        //updates the internal context map with the computed values of the evaluable cu's of <using> tag and also returns the original internal context map
        //to the caller function so that it can perform operations like restoring the original map back after the cu computation is over.
        public java.util.Map<String, Object> initAndSetAllVariables(
                                                    CompilationRuntimeContext compilationRuntimeContext)
                                                                                    throws XPathExpressionException {
            java.util.Map<String, Object> originalInternalContext = compilationRuntimeContext.getInternalContext();

            /*
            //initialize variables to use
            java.util.Map<String, Object> varMap = initAndGetAllVariables(compilationRuntimeContext);
            if (varMap == null && originalInternalContext != null) {
                varMap = new java.util.HashMap<String, Object>();
            }
            if (varMap != null && originalInternalContext != null) {
                varMap.putAll(originalInternalContext);  //merge the two internal contexts
            }

            //set the internal context.
            compilationRuntimeContext.setInternalContext(varMap);
            */

            //initialize variables to use
            java.util.Map<String, Object> varMap = null;
            if (originalInternalContext != null) {
                varMap = new java.util.HashMap<String, Object>();
                varMap.putAll(originalInternalContext);  //copy the current internal context
            }
            java.util.Map<String, Object> _varMap = initAndGetAllVariables(compilationRuntimeContext);
            if (varMap == null && _varMap != null) {
                varMap = _varMap;  //there was no original internal context available. Just use the returned _varMap
            } else if (varMap != null && _varMap != null) {
                varMap.putAll(_varMap);  //merge the two internal contexts
            }

            //set the internal context.
            compilationRuntimeContext.setInternalContext(varMap);

            return originalInternalContext;
        }

        /**************************************************************************************/
        /***********Basically used while cloning a CU during inheritance processing************/
        void copyEvaluables(List<IEvaluable/*ValueOf*/> evaluables) {
            copyEvaluables(evaluables, false);
        }

        void copyEvaluables(List<IEvaluable/*ValueOf*/> evaluables, boolean duplicatesOk) {
            if (evaluables == null) {
                return;
            }
            /*if (this.valueOfs == null) {
                this.valueOfs = new LinkedList<ValueOf>();
            }
            if (duplicatesOk) {
                this.valueOfs.addAll(evaluables);  //fast copy using addAll
            } else {
                for (ValueOf evaluable: evaluables) {
                    if (getChild(evaluable.getIdOrElse()) == null) {
                        this.valueOfs.add(evaluable);
                    }
                }
            }*/
            if (this.evaluables == null) {
                this.evaluables = new LinkedList<IEvaluable>();
            }
            if (duplicatesOk) {
                this.evaluables.addAll(evaluables);  //fast copy using addAll
            } else {
                for (IEvaluable evaluable: evaluables) {
                    if (getChild(evaluable.getIdOrElse()) == null) {
                        this.evaluables.add(evaluable);
                    }
                }
            }
        }

        List<IEvaluable/*ValueOf*/> getEvaluables() {
            return evaluables;  //valueOfs;
        }

        Using getClone() {
            /* Using clonedUsing = new Using();
            clonedUsing.copyAttributes(getAttributes());
            clonedUsing.valueOfs = new LinkedList<ValueOf>();
            clonedUsing.valueOfs.addAll(getEvaluables());
            return clonedUsing; */

            Using clonedUsing = new Using();
            clonedUsing.copyAttributes(getAttributes());
            clonedUsing.evaluables = new LinkedList<IEvaluable>();
            clonedUsing.evaluables.addAll(getEvaluables());
            return clonedUsing;
        }
        /**************************************************************************************/
    }

    public static class Set extends ExtensibleCompilationUnit<Set> implements IExecutable {
        public static final String TAG_NAME = "set";

        private static final String TRUE = "true";

        private static final String ATTRIBUTE_ATTRIBUTE = "attribute";
        private static final String ATTRIBUTE_IN = "in";
        private static final String ATTRIBUTE_AS = "as";
        private static final String ATTRIBUTE_BREAK_ON_FIRST_VALUE_SET = "breakOnFirstValueSet";
        private static final String ATTRIBUTE_OVERRIDE_VALUE = "override";  //true or false
        private static final String ATTRIBUTE_OUTPUT_NULL_VALUE = "outputNullValue";  //true or false
        private static final String ATTRIBUTE_CREATE_MAP_IF_MISSING = "createMapIfMissing";  //true or false
        private static final String[] ATTRIBUTES = {ATTRIBUTE_ATTRIBUTE, ATTRIBUTE_IN, ATTRIBUTE_AS,
                                                    ATTRIBUTE_BREAK_ON_FIRST_VALUE_SET,
                                                    ATTRIBUTE_OVERRIDE_VALUE, ATTRIBUTE_OUTPUT_NULL_VALUE,
                                                    ATTRIBUTE_CREATE_MAP_IF_MISSING};
        //private static final String CHILDREN_XPATH = "./valueof | ./get | ./select";
        //private static final String CHILDREN_XPATH2 = "./on";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{ValueOf.TAG_NAME, Get.TAG_NAME, Select.TAG_NAME, On.TAG_NAME});

        private List<IEvaluable/*EvaluableCompilationUnit*/> evaluables = null;
        private On on = null;  //'on' condition, if present, must be satisfied in order for this Set CU to execute.

        private boolean breakOnFirstValueSet = true;  //using a separate boolean field (and not using the
                                                      //string value of the corresponding attribute available inside
                                                      //the attributes map for optimization/performance enhancement.
        private boolean overrideValue = true;  //using a separate boolean field (and not using the
                                               //string value of the corresponding attribute available inside
                                               //the attributes map for optimization/performance enhancement.
        private boolean outputNullValue = false;  //using a separate boolean field (and not using the
                                                  //string value of the corresponding attribute available inside
                                                  //the attributes map for optimization/performance enhancement.
        private boolean createMapIfMissing = false;  //using a separate boolean field (and not using the
                                                     //string value of the corresponding attribute available inside
                                                     //the attributes map for optimization/performance enhancement.

        public String getAttributeToSet(CompilationRuntimeContext compilationRuntimeContext) {
            //return getAttribute(ATTRIBUTE_ATTRIBUTE);
            String attribute = null;
            try {
                //attempt computation
                attribute = getAttribute(ATTRIBUTE_ATTRIBUTE, compilationRuntimeContext);  //invoking the computed version
            } catch(XPathExpressionException xpee) {
                attribute = getAttribute(ATTRIBUTE_ATTRIBUTE);  //return the uncomputed value as fallback.
            }
            return attribute;
        }

        public boolean doOutputNullValue() {
            return outputNullValue;
        }

        @Override
        public String getIdOrElse() {
            String idOrName = getId();
            idOrName = "".equals(idOrName) ? null : idOrName;
            if (idOrName == null) {
                idOrName = getAttribute(ATTRIBUTE_ATTRIBUTE);
                idOrName = "".equals(idOrName) ? null : idOrName;
            }
            return idOrName;
        }

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            super.doCompileAttributes(n);

            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }

            initPerformanceVariables();  //initialize some attribute variables for performance optimization
        }

        /**************** Pre-initialize some attribute variables for performance enhancement ****************/
        /*****************************************************************************************************/
        @Override
        protected void initPerformanceVariables() {
            super.initPerformanceVariables();

            //------------------------------------------------------------------------------
            //initialize variables for some string attributes for performance optimizations
            //------------------------------------------------------------------------------
            String breakOnFirstValueSetTmp = getAttribute(ATTRIBUTE_BREAK_ON_FIRST_VALUE_SET);
            if (breakOnFirstValueSetTmp == null || "".equals(breakOnFirstValueSetTmp)) {
                breakOnFirstValueSetTmp = "true";
            }
            breakOnFirstValueSet = TRUE.equalsIgnoreCase(breakOnFirstValueSetTmp);

            String overrideValueTmp = getAttribute(ATTRIBUTE_OVERRIDE_VALUE);
            if (overrideValueTmp == null || "".equals(overrideValueTmp)) {
                overrideValueTmp = "true";
            }
            overrideValue = TRUE.equalsIgnoreCase(overrideValueTmp);

            String outputNullValueTmp = getAttribute(ATTRIBUTE_OUTPUT_NULL_VALUE);
            if (outputNullValueTmp == null || "".equals(outputNullValueTmp)) {
                outputNullValueTmp = "false";
            }
            outputNullValue = TRUE.equalsIgnoreCase(outputNullValueTmp);

            String createMapIfMissingTmp = getAttribute(ATTRIBUTE_CREATE_MAP_IF_MISSING);
            if (createMapIfMissingTmp == null || "".equals(createMapIfMissingTmp)) {
                createMapIfMissingTmp = "false";
            }
            createMapIfMissing = TRUE.equalsIgnoreCase(createMapIfMissingTmp);
            //------------------------------------------------------------------------------
            //------------------------------------------------------------------------------
        }
        /****************************************************************************************************/

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            evaluables = new LinkedList<IEvaluable/*EvaluableCompilationUnit*/>(); // since mostly we would be operating
                                                  // sequentially so
                                                  // LinkedList would be an optimal
                                                  // choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            super.doAddCompiledUnit(tagName, cu);

            //if ((cu instanceof ValueOf) || (cu instanceof Get) || (cu instanceof Select)) {
            //if (cu instanceof EvaluableCompilationUnit) {
            if (cu instanceof IEvaluable) {
                //evaluables.add((EvaluableCompilationUnit) cu);
                evaluables.add((IEvaluable) cu);
            } else if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return super.isChildTagRecognized(tagName) || RECOGNIZED_CHILD_TAGS.contains(tagName) ||
                                   //EvaluableCompilationUnit.class.isAssignableFrom(CompilationUnits.getCompilationClassForTag(tagName));
                                   //IEvaluable.class.isAssignableFrom(CompilationUnits.getCompilationClassForTag(tagName));
                                   CompilationUnits.isAssignableFrom(IEvaluable.class, CompilationUnits.getCompilationClassForTag(tagName));
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            ICompilationUnit cu = super.getChild(idOrElseOfChild);
            if (cu != null) {  //matching child found in base class
                return cu;
            }
            if (evaluables != null) {
                for (IEvaluable/*EvaluableCompilationUnit*/ evaluable : evaluables) {
                    if (idOrElseOfChild.equals(evaluable.getIdOrElse())) {
                        return evaluable;
                    }
                }
            }
            if (on != null && idOrElseOfChild.equals(on.getIdOrElse())) {
                return on;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            T[] superChildrenByType = super.getChildren(type);
            if (superChildrenByType != null) {
                return (T[]) superChildrenByType;
            }
            /*EvaluableCompilationUnit[] evaluablesArray = areMatchingTypes(EvaluableCompilationUnit.class, type) ?
                                             getElementsFromList(evaluables, new EvaluableCompilationUnit[0]) : null;*/
            IEvaluable[] evaluablesArray = areMatchingTypes(IEvaluable.class, type) ?
                                             getElementsFromList(evaluables, new IEvaluable[0]) : null;
            if (evaluablesArray != null) {
                return (T[]) evaluablesArray;
            }
            On[] onArray = getElementAsUnitArray(on, type);
            return (T[]) (onArray == null ? getZeroLengthArrayOfType(type) : onArray);
        }

        @Override
        protected Set doExtend(CompilationRuntimeContext compilationRuntimeContext,
                                                        CompiledTemplatesRegistry mctr) {
            //TODO do we really need inheritance at set level???
            return this;
        }

        @Override
        protected boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            return on == null || on.satisfies(compilationRuntimeContext);
        }

        public Object execute(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {

            /* Commented this block in favour of the satisfies(...) and _noValue(...) methods

            if (on != null && !on.satisfies(compilationRuntimeContext)) {
                return null;  //we need not attempt execution or return any value as there was an 'on' condition
                              //defined that remains unsatisfied.
            }
            */
            if (!satisfies(compilationRuntimeContext)) {
                return _noValue();
            }

            MapOfMaps mapOfMaps = compilationRuntimeContext.getExternalContext();

            Object value = getValue(compilationRuntimeContext);
            String attribute = getAttributeToSet(compilationRuntimeContext);  //getAttribute(ATTRIBUTE_ATTRIBUTE);
            String in = getAttribute(ATTRIBUTE_IN);
            java.util.Map<String, Object> mapTmp = mapOfMaps == null ?
                                                              null :
                                                              mapOfMaps.getMap(in);
            if (mapTmp != null) {
                //if (!overrideValue && mapTmp.get(attribute) != null) {
                //    throw new RuntimeException("Attribute '" +
                //                                attribute + "' in map '" + in + "' cannot be overridden.");
                //}
                //if (value != null || outputNullValue) {
                //    mapTmp.put(attribute, value/* == null ? null : value.toString()*/);  //Using the raw value instead of toString
                //}
                setValueInMap(false, in, mapTmp, attribute, value);
            } else {
                //We have opted to throw a RuntimeException in case the expected output map is missing from the
                //MapOfMaps provided 'createMapIfMissing' flag is set to false. If 'createMapIfMissing' is set
                //to true then we would dynamically create the required map and also set it inside MapOfMaps.
                if (createMapIfMissing && in != null && !"".equals(in.trim())) {
                    java.util.Map<String, Object> map = new HashMap<String, Object>();
                    //if (value != null || outputNullValue) {
                    //    map.put(attribute, value/* == null ? null : value.toString()*/);  //Using the raw value instead of toString
                    //}
                    setValueInMap(true, in, map, attribute, value);
                    mapOfMaps.putMap(in, map);
                } else {
                    throw new RuntimeException(
                                         "No output map available inside the MapOfMaps with the name: " + in);
                }
            }

            return null;  //returning nothing from 'execute'. Shall we instead return the value object here ???
        }

        private void setValueInMap(boolean newMapCreated, String nameOfMap, java.util.Map<String, Object> map, String attribute, Object value) {
            if (!newMapCreated && (!overrideValue && map.get(attribute) != null)) {
                throw new RuntimeException("Attribute '" +
                                            attribute + "' in map '" + nameOfMap + "' cannot be overridden.");
            }
            if (value != null || outputNullValue) {
                if (attribute != null) {
                    map.put(attribute, value/* == null ? null : value.toString()*/);  //Using the raw value instead of toString
                } else if (value instanceof java.util.Map) {
                    for (Entry<String, Object> entry : ((java.util.Map<String, Object>) value).entrySet()) {  //if the map isn't assignable to Map<String, Object> then an exception would be thrown here.
                        setValueInMap(newMapCreated, nameOfMap, map, entry.getKey(), entry.getValue());  //set recursively all map values
                    }
                } else {
                    //what should be done? I prefer to silently ignore this situation and won't set any value in map.
                }
            }
        }

        //appropriately builds and returns the value
        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                            throws XPathExpressionException {
            /* Commented out as the satisfaction is now implicitly checked inside getValue(...) before
               calling doGetValue(...) method.

            if (on != null && !on.satisfies(compilationRuntimeContext)) {
                return null;  //we need not return any value as there was an 'on' condition
                              //defined that remains unsatisfied.
            }
            */

            Object value = null;
            for (IEvaluable/*EvaluableCompilationUnit*/ evaluable : evaluables) {
                try {
                    compilationRuntimeContext.setAbortIfNotSatisfy(true);  //enable the abort flag
                    value = evaluable.getValue(compilationRuntimeContext);
                    if (breakOnFirstValueSet) {
                        break;
                    }

                    //we are likely interested in returning a transformed value based on the values of one
                    //or more valueof CUs. Let's pass such values using the internal context to the transformation
                    //function.
                    if (compilationRuntimeContext.getInternalContext() == null) {
                        compilationRuntimeContext.setInternalContext(new HashMap<String, Object>());
                    }
                    compilationRuntimeContext.getInternalContext().put(evaluable.getId(), value);

                } catch (RuntimeException re) {
                    //this was just for indication that the value was not set because the 'on' condition was
                    //not satisfied even when we managed to find a value of the key inside one of the maps.
                    //no specific action needed.
                } finally {
                    compilationRuntimeContext.setAbortIfNotSatisfy(false);  //reset back the abort flag to false
                }
            }
            return value;
        }
    }

    public static class Unset extends CompilationUnit implements IExecutable {
        public static final String TAG_NAME = "unset";

        private static final String ATTRIBUTE_ATTRIBUTE = "attribute";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_ATTRIBUTE};

        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Map.TAG_NAME, InternalMap.TAG_NAME, Json.TAG_NAME, On.TAG_NAME});

        private List<Map> maps = null;
        private On on = null;

        private String getAttributeToUnset(CompilationRuntimeContext compilationRuntimeContext) {
            String attribute = null;
            try {
                //attempt computation
                attribute = getAttribute(ATTRIBUTE_ATTRIBUTE, compilationRuntimeContext);  //invoking the computed version
            } catch(XPathExpressionException xpee) {
                attribute = getAttribute(ATTRIBUTE_ATTRIBUTE);  //return the uncomputed value as fallback.
            }
            return attribute;
        }

        @Override
        public String getIdOrElse() {
            String idOrName = getId();
            idOrName = "".equals(idOrName) ? null : idOrName;
            if (idOrName == null) {
                idOrName = getAttribute(ATTRIBUTE_ATTRIBUTE);
                idOrName = "".equals(idOrName) ? null : idOrName;
            }
            return idOrName;
        }

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            maps = new LinkedList<Map>();  //since mostly we would be operating
                                           //sequentially so
                                           //LinkedList would be an optimal
                                           //choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (cu instanceof Map) {
                maps.add((Map) cu);
            } else if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName);
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (maps != null) {
                for (Map map : maps) {
                    if (idOrElseOfChild.equals(map.getIdOrElse())) {
                        return map;
                    }
                }
            }
            if (on != null && idOrElseOfChild.equals(on.getIdOrElse())) {
                return on;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            Map[] mapsArray = areMatchingTypes(Map.class, type) ? getElementsFromList(maps, new Map[0]) : null;
            if (mapsArray != null) {
                return (T[]) mapsArray;
            }
            On[] onArray = getElementAsUnitArray(on, type);
            return (T[]) (onArray == null ? getZeroLengthArrayOfType(type) : onArray);
        }

        @Override
        public Object execute(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {

            if (on != null && !on.satisfies(compilationRuntimeContext)) {
                return null;  //we need not attempt execution or return any value as there was an 'on' condition
                              //defined that remains unsatisfied.
            }

            String attribute = getAttributeToUnset(compilationRuntimeContext);  //getAttribute(ATTRIBUTE_ATTRIBUTE);

            for (Map map : maps) {
                if (map.satisfiesOn(compilationRuntimeContext)) {
                    map.remove(attribute, compilationRuntimeContext);
                }
            }

            return null;  //nothing to return.
        }
    }

    public static class Get extends ValueOf {
        public static final String TAG_NAME = "get";
    }

    public static class ExecutableGroup extends Group implements IExecutable {
        public static final String TAG_NAME = "executable-group";

        //private static final String CHILDREN_XPATH = "./using";
        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Using.TAG_NAME});

        private Using using = null;

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            super.doAddCompiledUnit(tagName, cu);

            if ((cu instanceof Using) && (using == null)) {
                //we need just one 'using' cu and in case of many defined, let's just retain the first instance that we got.
                using = (Using) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return super.isChildTagRecognized(tagName) || RECOGNIZED_CHILD_TAGS.contains(tagName);
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            ICompilationUnit cu = super.getChild(idOrElseOfChild);
            if (cu != null) {  //matching child found in base class
                return cu;
            }
            if (using != null) {
                if (idOrElseOfChild.equals(using.getIdOrElse())) {
                    return using;
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            Using[] usingArray = getElementAsUnitArray(using, type);
            if (usingArray != null) {
                return (T[]) usingArray;
            }
            T[] superChildrenByType = super.getChildren(type);
            if (superChildrenByType != null) {
                return (T[]) superChildrenByType;
            }
            return (T[]) getZeroLengthArrayOfType(type);
        }

        /*********** Start - cloning methods ************/
        @Override
        protected ExecutableGroup newInstance() {
            return new ExecutableGroup();
        }

        @Override
        //deep clone
        protected ExecutableGroup getClone() {
            ExecutableGroup clonedECU = (ExecutableGroup) super.getClone();
            if (using != null) {
                clonedECU.using = using.getClone();
            }
            return clonedECU;
        }
        /*********** End - cloning methods ***********/

        /************************************* Start - method to execute using block *************************************/
        /*****************************************************************************************************************/
        /*****************************************************************************************************************/
        //This method executes the using block if any. Here execution of using block is provided in a
        //separate protected method to allow some cu's to change its sequence and frequency. E.g. 'loop' cu's
        //may want that the using block be executed only once and that too before any looping starts -
        //they can then override this method to not do anything and explicitly call super.doUsing(...) at a
        //different place just before starting the loop iterations.
        //Also execution of 'using' block may result in resetting of variables inside the internal context map
        //and this method returns the original snapshot of the internal context map that existed before execution
        //of 'using'.
        protected java.util.Map<String, Object> doUsing(CompilationRuntimeContext compilationRuntimeContext)
                                                                                        throws XPathExpressionException {
            java.util.Map<String, Object> originalInternalContext = null;
            if (using != null) {
                originalInternalContext = using.initAndSetAllVariables(compilationRuntimeContext);
            }
            return originalInternalContext;
        }
        /************************************** End - method to execute using block **************************************/
        /*****************************************************************************************************************/

        /************************************************************************************/
        /************* Start - pre and post processing methods of getValue(...) *************/
        /************************************************************************************/
        /************************************************************************************/
        @Override
        protected void preGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            //initialize the using
            java.util.Map<String, Object> originalInternalContext = doUsing(compilationRuntimeContext);
        }
        /************************************************************************************/
        /************** End - pre and post processing methods of getValue(...) **************/
        /************************************************************************************/

        //overridden to call execute by default as that makes logical sense for this executable group.
        @Override
        public Object build(CompilationRuntimeContext compilationRuntimeContext,
                                              ReturnType returnType) throws XPathExpressionException {
            return execute(compilationRuntimeContext);
        }

        @Override
        public Object execute(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            /* Commented this block in favour of the satisfies(...) method.

            On[] ons = getChildren(On.class);
            if (ons != null && ons.length > 0 && !ons[0].satisfies(compilationRuntimeContext)) {
                return null;  //we need not attempt execution or return any value as there was an 'on' condition defined
                              //that remains unsatisfied. Also within execution context it makes sense to return null.
            }
            */
            if (!satisfies(compilationRuntimeContext)) {
                return null;
            }

            return getValue(compilationRuntimeContext);
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            /* Commenting this block because now the EvaluableCompilationUnit's getValue(...) method
               implicitly checks for satisfaction and also invokes preGetValue(...) before calling doGetValue. The
               respective methods are overridden in this class - satisfies(...) checks for the 'on' condition and
               doUsing(...) is called inside the overridden version of preGetValue(...) of this cu.

            On[] ons = getChildren(On.class);
            if (ons != null && ons.length > 0 && !ons[0].satisfies(compilationRuntimeContext)) {
                return "";  //we need not return any value as there was an 'on' condition defined that remains
                            //unsatisfied. Also for group it makes sense to return a zero length string and not
                            //the null value
            }

            java.util.Map<String, Object> originalInternalContext = doUsing(compilationRuntimeContext);
            */

            String resultMapName = null;
            String EXECUTION_ENDSTATE = "_$execution-endstate";
            String EXECUTION_OUTCOME = "_$execution-outcome";
            String EXECUTION_RESULT_MAP = "_$execution-result-map";
            java.util.Map<String, Object> internalCtx = compilationRuntimeContext.getInternalContext();
            if (internalCtx == null) {
                //internalCtx is null but we would need to set couple of state variables inside the internal context.
                //Let's initialize the internal context and set the same inside compilation runtime context also.
                internalCtx = new java.util.HashMap<String, Object>();
                compilationRuntimeContext.setInternalContext(internalCtx);
            }
            try {
                resultMapName = doExecute(internalCtx);
                internalCtx.put(EXECUTION_ENDSTATE, "success");
                if (resultMapName != null) {
                    internalCtx.put(EXECUTION_RESULT_MAP, resultMapName);
                }
            } catch (Exception e) {
                internalCtx.put(EXECUTION_ENDSTATE, "failure");
                internalCtx.put(EXECUTION_OUTCOME, e);
            }
            try {
                doInit(compilationRuntimeContext);  //as we have overridden preGetValue(...) method in this class which executes just 'using' cu
                                                    //let's now explicitly call doInit(...) to ensure the init also gets called before
                                                    //doing super.doGetValue(...).
                return super.doGetValue(compilationRuntimeContext);  //build the success or the failure blocks as applicable
            } finally {
                /* No need to explicitly clear the state variables as they will automatically get discarded when the original
                   internal context map gets restored inside the getValue(...) method of EvaluableCompilationUnit.

                //now that the response map and execution state entries would have already been used to construct the response, let’s dispose them all.
                if (resultMapName != null) {
                    internalCtx.remove(resultMapName);
                }
                internalCtx.remove(EXECUTION_ENDSTATE);
                internalCtx.remove(EXECUTION_OUTCOME);
                internalCtx.remove(EXECUTION_RESULT_MAP);
                */
            }
        }

        /*********** Start - Inheritance related methods ************/
        @Override
        protected void doExtendAdditionalUnitsFromBaseUsingMerge(Group baseUnit, CompilationRuntimeContext compilationRuntimeContext) {
            super.doExtendAdditionalUnitsFromBaseUsingMerge(baseUnit, compilationRuntimeContext);

            if (baseUnit instanceof ExecutableGroup) {  //it makes sense to attempt merging only if the baseUnit is also an instance of ExecutableGroup
                ExecutableGroup _baseUnit = (ExecutableGroup) baseUnit;
                //merge the 'using' blocks
                if (_baseUnit.using != null) {
                    if (this.using == null) {
                        this.using = _baseUnit.using.getClone();  //new Using();  //we would be interested in cloning here as that way the base attributes also would get inherited.
                    } else {
                        this.using.copyEvaluables(_baseUnit.using.getEvaluables());  //we would just be interested in copying the evaluables without affecting the attributes in super class.
                    }
                }
            }
        }
        /*********** End - Inheritance related methods ***********/

        //the result or outcome of the execution should be set inside requestContext as a map and the name of the map key should be returned as the value of the function.
        protected String doExecute(java.util.Map<String, Object> requestContext) {
            return null;
        }
    }

    //Headless executable compilation unit
    public static class HeadlessExecutableGroup extends ExecutableGroup {
        public static final String TAG_NAME = "headless-executable-group";

        @Override
        public void doCompileAttributes(Node n) throws XPathExpressionException {
            isHeadless = true;
            super.doCompileAttributes(n);
        }

        /************************************ Used during cloning ***********************************/
        /****** Overridden to ensure cloning of a headless group also returns a headless group ******/
        @Override
        protected HeadlessExecutableGroup newInstance() {
            return new HeadlessExecutableGroup();
        }

        @Override
        //deep clone
        protected HeadlessExecutableGroup getClone() {
            HeadlessExecutableGroup clonedHEG = (HeadlessExecutableGroup) super.getClone();
            clonedHEG.isHeadless = isHeadless;
            return clonedHEG;
        }
        /********************************************************************************************/
    }

    //loop cu
    public static class Loop extends HeadlessExecutableGroup implements IExecutable {
        public static final String TAG_NAME = "loop";

        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Break.TAG_NAME});

        private Break breakk;

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            super.doAddCompiledUnit(tagName, cu);

            if ((cu instanceof Break) && (breakk == null)) {
                //we need just one 'break' cu and in case of many defined, let's just retain the first instance that we got.
                breakk = (Break) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return super.isChildTagRecognized(tagName) || RECOGNIZED_CHILD_TAGS.contains(tagName);
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            ICompilationUnit cu = super.getChild(idOrElseOfChild);
            if (cu != null) {  //matching child found in base class
                return cu;
            }
            if (breakk != null) {
                if (idOrElseOfChild.equals(breakk.getIdOrElse())) {
                    return breakk;
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            Break[] breakkArray = getElementAsUnitArray(breakk, type);
            if (breakkArray != null) {
                return (T[]) breakkArray;
            }
            T[] superChildrenByType = super.getChildren(type);
            if (superChildrenByType != null) {
                return (T[]) superChildrenByType;
            }
            return (T[]) getZeroLengthArrayOfType(type);
        }

        /**************************************************************************************************************************/
        /***** Start - overridden initializer, finalizer and using methods to prevent multiple invocations because of looping *****/
        /**************************************************************************************************************************/
        @Override
        protected void doInit(CompilationRuntimeContext compilationRuntimeContext)
                                                              throws XPathExpressionException {
            //overridden to not do anything or otherwise the init block would get called on every iteration.
            //call super.doInit(...) directly in this class where needed (e.g. just before starting loop iterations).
        }

        /* Commenting these methods out in favour of preGetValue(...) and postGetValue(...) methods.
        @Override
        protected void doFinally(CompilationRuntimeContext compilationRuntimeContext)
                                                                 throws XPathExpressionException {
            //overridden to not do anything or otherwise the finalizer block would get called on every iteration.
            //call super.doFinally(...) directly in this class where needed (e.g. just after ending loop iterations).
        }

        @Override
        protected java.util.Map<String, Object> doUsing(CompilationRuntimeContext compilationRuntimeContext)
                                                                                        throws XPathExpressionException {
            //overridden to not do anything or otherwise the using block would get called on every iteration.
            //call super.doUsing(...) directly in this class where needed (e.g. just before starting loop iterations).
            return null;
        }
        */

        @Override
        protected void preGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            //overridden to not do anything or otherwise the preGetValue block would get called on every iteration.
            //call super.preGetValue(...) directly in this class where needed (e.g. just before starting loop iterations).
        }

        @Override
        protected void postGetValue(Object value, CompilationRuntimeContext compilationRuntimeContext)
                                                                                   throws XPathExpressionException {
            //overridden to not do anything or otherwise the postGetValue block would get called on every iteration.
            //call super.postGetValue(...) directly in this class where needed (e.g. just after ending loop iterations).
        }
        /**************************************************************************************************************************/
        /****** End - overridden initializer, finalizer and using methods to prevent multiple invocations because of looping ******/
        /**************************************************************************************************************************/

        @Override
        public Object execute(CompilationRuntimeContext compilationRuntimeContext)
                                                               throws XPathExpressionException {
            /* Commented this block in favour of the satisfies(...) and _noValue(...) methods.

            On[] ons = getChildren(On.class);
            if (ons != null && ons.length > 0 && !ons[0].satisfies(compilationRuntimeContext)) {
                return "";  //we need not return any value as there was an 'on' condition defined that remains
                            //unsatisfied. Also for group it makes sense to return a zero length string and not
                            //the null value
            }
            */
            if (!satisfies(compilationRuntimeContext)) {
                return _noValue();
            }

            //as we have overridden the execute method per the needs of a looping unit let's also make sure that
            //we save and restore the internal context map in a  manner similar to what is done inside getValue(...)
            //method of EvaluableCompilationUnit. This is needed because the execute method here does more than just
            //calling getValue(...) method of super (in fact it calls getValue on every iteration) and may have
            //modified the internal context map in its own unique ways.
            //saved the internal context of this CU for reseting later inside this method.
            java.util.Map<String, Object> savedInternalContext = compilationRuntimeContext.getInternalContext();
            if (savedInternalContext != null) {
                //instead of just holding the reference to the internal context we would like to create a copy
                //of it so that even if the internal context map gets modified by any of the methods of the
                //processor CU we would still be able to restore back to the original context state cleanly.
                java.util.Map<String, Object> savedInternalContextCpy = new HashMap<String, Object>();
                savedInternalContextCpy.putAll(savedInternalContext);
                compilationRuntimeContext.setInternalContext(savedInternalContextCpy);  //savedInternalContext = savedInternalContextCpy;  //refer comment below:
                                                                                        //Updated the reference of internal context map inside compilationRuntimeContext
                                                                                        //for this getValue request as opposed to the previous scheme of using the same reference of
                                                                                        //internal context map inside compilationRuntimeContext for this getValue request and later
                                                                                        //restoring the copy of savedInternalContext inside finally. Functionally the two approaches
                                                                                        //would have been no different except in one case. If the invoker of this getValue
                                                                                        //request had obtained a reference to the internal context map before and now wishes to add/remove objects
                                                                                        //to/from the saved internal context reference then that would not reflect inside compilationRuntimeContext
                                                                                        //as the reference to the internal context map inside compilationRuntimeContext would already
                                                                                        //have changed (inside finally) before returning from this method. A workaround then would have been
                                                                                        //to add/remove objects to/from internal context map by obtaining a fresh copy of it using the
                                                                                        //compilationRuntimeContext.getInternalContext() which would not be necessary now.
            }

            try {
                //java.util.Map<String, Object> originalInternalContext = super.doUsing(compilationRuntimeContext);  //call the 'using' directly using super
                super.preGetValue(compilationRuntimeContext);  //calling the preGetValue directly using super which inturn will process 'using'

                java.util.Map<String, Object> internalCtx = compilationRuntimeContext.getInternalContext();
                java.util.Map<String, Object> requestContext = internalCtx;

                if (internalCtx == null) {
                    //if there is no internal context available even now then we need not proceed as there would be
                    //no looping context variables available
                    return _noValue();  //"";  //since it's a group it makes sense to return a zero length string rather than null
                }

                Object iterable = requestContext.get("iterable");
                //String iterableType = (String) requestContext.get("iterable-type");  //iterable might be string representation
                                                                                     //of object like json, properties etc.
                Object value = "";
                super.doInit(compilationRuntimeContext);  //call the initializer directly using super

                //internal context variables that should be made available inside finally block.
                //Further, any variables defined here should make sense outside the loop's iteration scope.
                final String ITERABLE_SIZE = getStateVariable("iterable-size", internalCtx);
                try {
                    if (iterable == null) {
                        value = loopTimes(internalCtx, compilationRuntimeContext);
                    } else if (iterable.getClass().isArray()) {
                        //make the array size available inside internal context
                        internalCtx.put(ITERABLE_SIZE, Array.getLength(iterable));
                        value = loopArray(iterable, internalCtx, compilationRuntimeContext);
                    } else if (iterable instanceof Iterable) {
                        //make the iterable size available inside internal context
                        if (iterable instanceof Collection) {
                            internalCtx.put(ITERABLE_SIZE, ((Collection) iterable).size());
                        }
                        value = loopIterable((Iterable) iterable, internalCtx, compilationRuntimeContext);
                    } else if (iterable instanceof java.util.Map) {
                        //make the iterable size available inside internal context
                        internalCtx.put(ITERABLE_SIZE, ((java.util.Map) iterable).size());
                        value = loopMap((java.util.Map) iterable, internalCtx, compilationRuntimeContext);
                    }
                } finally {
                    //super.doFinally(compilationRuntimeContext);  //call the finalizer directly using super
                    super.postGetValue(value, compilationRuntimeContext);
                    internalCtx.remove(ITERABLE_SIZE);
                }

                return value;
            } finally {
                //as we are done with getting the value of the CU let us now reset the internal context
                //to the state it was in when we started processing this CU
                compilationRuntimeContext.setInternalContext(savedInternalContext);
            }
        }

        private static String getStateVariable(String key, java.util.Map<String, Object> lookupMap) {
            Object variable = lookupMap.get(key);
            return variable instanceof String? (String) variable: key;  //if a custom state variable is defined and if it is of type String then return
                                                                        //it's value. Else return key as is to be used as the state variable.
        }

        private Object loopTimes(java.util.Map<String, Object> internalCtx,
                                 CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            java.util.Map<String, Object> requestContext = internalCtx;

            Object startIndexAsObject = requestContext.get("start");
            Object endIndexAsObject = requestContext.get("end");
            Object numTimesAsObject = requestContext.get("times");

            final String START_INDEX = getStateVariable("start-index", internalCtx);
            final String END_INDEX = getStateVariable("end-index", internalCtx);
            final String LOOP_INDEX = getStateVariable("index", internalCtx);
            final String NUM_TIMES = getStateVariable("num-times", internalCtx);

            int startIndex = 0;
            int endIndex = 0;
            int numTimes = 0;
            boolean endlessLoop = false;
            if (startIndexAsObject != null) {
                startIndex = Integer.parseInt(startIndexAsObject.toString());  //let it throw number format exception if the string doesn't represent a valid int value.
                internalCtx.put(START_INDEX, startIndex);
                endlessLoop = true;  //if a start index is specified then we would treat the loop as an endless one unless an end index or num times is specified.
            }

            //at any point of time either endIndex or numTimes should be considered. Both are contradictory so cannot be considered simultaneously.
            if (endIndexAsObject != null) {
                endIndex = Integer.parseInt(endIndexAsObject.toString());  //let it throw number format exception if the string doesn't represent a valid int value.
                internalCtx.put(END_INDEX, endIndex);
                endlessLoop = false;
            } else if (numTimesAsObject != null) {
                numTimes = Integer.parseInt(numTimesAsObject.toString());  //let it throw number format exception if the string doesn't represent a valid int value.
                endIndex = startIndex + numTimes;
                internalCtx.put(END_INDEX, endIndex);
                internalCtx.put(NUM_TIMES, numTimes);
                endlessLoop = false;
            }

            boolean decrement = false;
            if (!endlessLoop && endIndex < startIndex) {
                //we have to decrement instead of increment the loop counter.
                decrement = true;
            }

            Object value = "";
            boolean[] singleElemArrayIndicatingWhetherFirstTime = {true};
            try {
                for (int i = startIndex; endlessLoop? true: (decrement? i > endIndex: i < endIndex); i = decrement? i - 1: i + 1) {
                    //set the loop state variables inside internal context
                    internalCtx.put(LOOP_INDEX, i);

                    if (breakk != null && breakk.satisfies(compilationRuntimeContext)) {
                        break;
                    }

                    //evaluate the evaluables
                    value = loopBody(compilationRuntimeContext, singleElemArrayIndicatingWhetherFirstTime, value);
                }
            } finally {
                internalCtx.remove(START_INDEX);
                internalCtx.remove(END_INDEX);
                internalCtx.remove(LOOP_INDEX);
                internalCtx.remove(NUM_TIMES);
            }
            return value;
        }

        private Object loopArray(Object array,
                                 java.util.Map<String, Object> internalCtx,
                                 CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            Object value = "";
            if (array == null) {
                return value;
            }
            boolean[] singleElemArrayIndicatingWhetherFirstTime = {true};
            final String ITEM_VALUE = getStateVariable("item-value", internalCtx);
            final String ITEM_INDEX = getStateVariable("index", internalCtx);
            int arrayLength = Array.getLength(array);  //using reflection here to keep the logic of iterating array elements generic.
            try {
                for (int i = 0; i < arrayLength; i++) {
                    internalCtx.put(ITEM_VALUE, Array.get(array, i));  //using reflection here to keep the logic of iterating array elements generic.
                    internalCtx.put(ITEM_INDEX, i);

                    if (breakk != null && breakk.satisfies(compilationRuntimeContext)) {
                        break;
                    }

                    //evaluate the evaluables
                    value = loopBody(compilationRuntimeContext, singleElemArrayIndicatingWhetherFirstTime, value);
                }
            } finally {
                internalCtx.remove(ITEM_VALUE);
                internalCtx.remove(ITEM_INDEX);
            }
            return value;
        }

        private Object loopIterable(Iterable iterable,
                                    java.util.Map<String, Object> internalCtx,
                                    CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            Object value = "";
            if (iterable == null) {
                return value;
            }
            boolean[] singleElemArrayIndicatingWhetherFirstTime = {true};
            final String ITEM_VALUE = getStateVariable("item-value", internalCtx);
            final String ITEM_INDEX = getStateVariable("index", internalCtx);
            int index = 0;
            try {
                for (Object item: iterable) {  //for (int i = 0; i < iterable.size(); i++) {
                    internalCtx.put(ITEM_VALUE, item);
                    internalCtx.put(ITEM_INDEX, index++);

                    if (breakk != null && breakk.satisfies(compilationRuntimeContext)) {
                        break;
                    }

                    //evaluate the evaluables
                    value = loopBody(compilationRuntimeContext, singleElemArrayIndicatingWhetherFirstTime, value);
                }
            } finally {
                internalCtx.remove(ITEM_VALUE);
                internalCtx.remove(ITEM_INDEX);
            }
            return value;
        }

        private Object loopMap(java.util.Map map,
                               java.util.Map<String, Object> internalCtx,
                               CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            Object value = "";
            if (map == null) {
                return value;
            }
            boolean[] singleElemArrayIndicatingWhetherFirstTime = {true};
            final String ITEM_KEY = getStateVariable("item-key", internalCtx);
            final String ITEM_VALUE = getStateVariable("item-value", internalCtx);
            try {
                for (Object _entry: map.entrySet()) {  //map.forEach((key,item) -> {
                    java.util.Map.Entry entry = (java.util.Map.Entry) _entry;
                    internalCtx.put(ITEM_KEY, entry.getKey());
                    internalCtx.put(ITEM_VALUE, entry.getValue());

                    if (breakk != null && breakk.satisfies(compilationRuntimeContext)) {
                        break;
                    }

                    //evaluate the evaluables
                    value = loopBody(compilationRuntimeContext, singleElemArrayIndicatingWhetherFirstTime, value);
                }
            } finally {
                internalCtx.remove(ITEM_KEY);
                internalCtx.remove(ITEM_VALUE);
            }
            return value;
        }

        private Object loopBody(CompilationRuntimeContext compilationRuntimeContext,
                                boolean[] singleElemArrayIndicatingWhetherFirstTime,
                                Object value) throws XPathExpressionException {
            Object _value = iteration(compilationRuntimeContext);
            //Object _value = iterationValueObj;
            if (_value != null && !"".equals(_value)) {
                if (!singleElemArrayIndicatingWhetherFirstTime[0]) {
                    value += ",";
                } else {
                    singleElemArrayIndicatingWhetherFirstTime[0] = false;
                }
            }
            if (_value != null && !"".equals(_value)) {
                if (value == null) {
                    value = _value.toString();
                } else {
                    value += _value.toString();
                }
            }
            return value;
        }

        //represents an iteration of the loop. Subclasses can override.
        protected Object iteration(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            return getValue(compilationRuntimeContext);
        }

        /*********** Start - cloning methods ************/
        @Override
        protected Loop newInstance() {
            return new Loop();
        }

        @Override
        //deep clone
        protected Loop getClone() {
            Loop clonedLoop = (Loop) super.getClone();
            if (breakk != null) {
                clonedLoop.breakk = breakk.getClone();
            }
            return clonedLoop;
        }
        /*********** End - cloning methods ***********/

        public static class Break extends CompilationUnit {
            public static final String TAG_NAME = "break";

            //private static final String CHILDREN_XPATH = "./conditional | ./condition";
            private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Conditional.TAG_NAME, Condition.TAG_NAME});

            private ICondition condition = null;

            @Override
            protected void doCompileAttributes(Node n) throws XPathExpressionException {
            }

            @Override
            protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
                if (((cu instanceof Condition) || (cu instanceof Conditional)) && (condition == null)) {
                    //we need just one 'condition' cu and in case of many defined, let's just retain the first instance that we got.
                    condition = (ICondition) cu;
                }
            }

            @Override
            protected boolean isChildTagRecognized(String tagName) {
                return RECOGNIZED_CHILD_TAGS.contains(tagName);
            }

            @Override
            public ICompilationUnit getChild(String idOrElseOfChild) {
                if (idOrElseOfChild == null) {
                    return null;
                }
                if (condition != null && idOrElseOfChild.equals(condition.getIdOrElse())) {
                    return condition;
                }
                return null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
                ICondition[] conditionArray = null;
                if (condition instanceof Condition) {
                    conditionArray = getElementAsUnitArray((Condition) condition, type);
                } else if (condition instanceof Conditional) {
                    conditionArray = getElementAsUnitArray((Conditional) condition, type);
                }
                return (T[]) (conditionArray == null ? getZeroLengthArrayOfType(type) : conditionArray);
            }

            public boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                            throws XPathExpressionException {
                return condition != null ? condition.matches(compilationRuntimeContext) : true;
            }

            /**************************************************************************************/
            /***********Basically used while cloning a CU during inheritance processing************/
            Break getClone() {
                Break clonedBreak = new Break();
                clonedBreak.copyAttributes(getAttributes());
                clonedBreak.condition = condition;  //this is just a reference copy
                return clonedBreak;
            }
            /**************************************************************************************/
        }
    }

    /**********************************************************************************************/
    /*************************** Start - CUs for testing and degugging ****************************/
    /**********************************************************************************************/

    //A Logger cu
    public static class Log extends CompilationUnit implements IExecutable {
        public static final String TAG_NAME = "log";

        private static final String ATTRIBUTE_LEVEL = "level";
        private static final String ATTRIBUTE_TARGET = "target";  //recognized values would be 'file', 'console' and 'socket' (exclusive of quotes)
        private static final String[] ATTRIBUTES = {ATTRIBUTE_LEVEL, ATTRIBUTE_TARGET};

        private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Using.TAG_NAME, On.TAG_NAME});

        private On on = null;
        private Using using = null;
        private List<IEvaluable> evaluables = null;

        private String loggingContext = null;  //this will have the same significance as that of a java class in log messages. This will help identify
                                               //as to which xml file the log statement was defined.

        @Override
        protected void doCompileAttributes(Node n) throws XPathExpressionException {
            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n));
            }

            //set the logging context - we will use document element identifier for the purpose
            String _loggingContext = null;
            Document document = n.getOwnerDocument();
            if (document != null) {
                Element e = document.getDocumentElement();
                if (e != null) {
                    String rootId = e.getAttribute("id");
                    _loggingContext = rootId != null && !"".equals(rootId)? rootId: e.getAttribute("name");
                }
            }
            //loggingContext = _loggingContext == null || "".equals(_loggingContext)? "<anonymous-root>": _loggingContext;
            loggingContext = _loggingContext;
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            evaluables = new LinkedList<IEvaluable>();  //since mostly we would be operating
                                                        //sequentially so
                                                        //LinkedList would be an optimal
                                                        //choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (cu instanceof IEvaluable) {
                evaluables.add((IEvaluable) cu);
            } else if ((cu instanceof Using) && (using == null)) {
                //we need just one 'using' cu and in case of many defined, let's just retain the first instance that we got.
                using = (Using) cu;
            } else if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagName) {
            return RECOGNIZED_CHILD_TAGS.contains(tagName) ||
                   //IEvaluable.class.isAssignableFrom(CompilationUnits.getCompilationClassForTag(tagName));
                   CompilationUnits.isAssignableFrom(IEvaluable.class, CompilationUnits.getCompilationClassForTag(tagName));
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (using != null && idOrElseOfChild.equals(using.getIdOrElse())) {
                return using;
            }
            if (on != null && idOrElseOfChild.equals(on.getIdOrElse())) {
                return on;
            }
            if (evaluables != null) {
                for (IEvaluable evaluable : evaluables) {
                    if (idOrElseOfChild.equals(evaluable.getIdOrElse())) {
                        return evaluable;
                    }
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            IEvaluable[] evaluableArray = areMatchingTypes(IEvaluable.class, type) ?
                                                       getElementsFromList(evaluables, new IEvaluable[0]) : null;
            if (evaluableArray != null) {
                return (T[]) evaluableArray;
            }
            Using[] usingArray = getElementAsUnitArray(using, type);
            if (usingArray != null) {
                return (T[]) usingArray;
            }
            On[] onArray = getElementAsUnitArray(on, type);
            return (T[]) (onArray == null ? getZeroLengthArrayOfType(type) : onArray);
        }

        @Override
        public Object execute(CompilationRuntimeContext compilationRuntimeContext)
                                                              throws XPathExpressionException {
            if (on != null && !on.satisfies(compilationRuntimeContext)) {
                return null;  //we need not attempt execution or return any value as there was an 'on' condition
                              //defined that remains unsatisfied.
            }

            java.util.Map<String, Object> originalInternalContext = null;
            if (using != null) {
                 originalInternalContext = using.initAndSetAllVariables(compilationRuntimeContext);
            }

            try {
                log(compilationRuntimeContext);
            } finally {
                 if (using != null) {
                     //there was a 'using' cu defined that could have modified the state of internal context map.
                     //let's restore
                     compilationRuntimeContext.setInternalContext(originalInternalContext);
                 }
            }

            return null;  //don't need to return any value
        }

        private void log(CompilationRuntimeContext compilationRuntimeContext)
                                                            throws XPathExpressionException {
            String logLevel = getAttribute(ATTRIBUTE_LEVEL, compilationRuntimeContext);
            if (logLevel != null && "none".equalsIgnoreCase(logLevel)) {
                //extended log level 'none' provided to skip logging.
                return;
            }
            String logTarget = getAttribute(ATTRIBUTE_TARGET, compilationRuntimeContext);
            if (logTarget != null && "none".equalsIgnoreCase(logTarget)) {
                //extended log target 'none' provided to skip logging.
                return;
            }

            doLog(logLevel, logTarget, compilationRuntimeContext);
        }

        //subclasses can override
        protected void doLog(String logLevel, String loggerType, CompilationRuntimeContext compilationRuntimeContext)
                                                                               throws XPathExpressionException {
            org.cuframework.util.logging.LogManager logManager = org.cuframework.util.logging.LogManager.instance();

            java.util.Map<String, Object> internalCtx = compilationRuntimeContext.getInternalContext();
            java.util.logging.Logger logger = logManager.getLoggerByType(loggerType);
            if (logger == null)
                logger = logManager.getLogger();  //would return the default logger
            if (logger == null)  //logger still null? we should return.
                return;
            java.util.logging.Level level = logManager.getLogLevel(logLevel);

            String logIdOrElse = getIdOrElse();
            for (IEvaluable evaluable : evaluables) {
                try {
                    compilationRuntimeContext.setAbortIfNotSatisfy(true);  //enable the abort flag
                    Object value = evaluable.getValue(compilationRuntimeContext);
                    logger.logp(level, loggingContext,
                               /*("[" + (logIdOrElse != null? logIdOrElse + "#": "") + evaluable.getIdOrElse() + "]"),*/
                               (logIdOrElse + "#" + evaluable.getIdOrElse()),
                               (value != null? value.toString(): null));

                } catch (RuntimeException re) {
                    //this was just for indication that the value was not set because the 'on' condition was
                    //not satisfied even when we managed to find a value of the key inside one of the maps.
                    //no specific action needed.
                } finally {
                    compilationRuntimeContext.setAbortIfNotSatisfy(false);  //reset back the abort flag to false
                }
            }
        }
    }

    //Assertion cu
    public static class Assert extends Conditional implements IExecutable {
        public static final String TAG_NAME = "assert";

        @Override
        public Object execute(CompilationRuntimeContext compilationRuntimeContext)
                                                              throws XPathExpressionException {
            /*
            boolean satisfies = (boolean) getValue(compilationRuntimeContext);
            if (!satisfies) {
                throw new AssertionException(getIdOrElse(), "Test Failed");
            }
            return satisfies;
            */

            if (!matches(compilationRuntimeContext)) {  //let's call matches(...) here instead of getValue(...) as getEvaluatedValue(...) may perform translation
                                                        //and return a non boolean value and we are interested in checking for true or false here.
                throw new AssertionException(getIdOrElse(), "Test Failed");
            }
            return true;  //reching here means the assertion test passed.
        }

        @Override
        protected boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            return matches(compilationRuntimeContext);
        }

        @Override
        protected Object _noValue() {
            return false;
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            return true;  //matches(compilationRuntimeContext);  //returning true because reaching here via getValue(...) itself signifies
                                                                 //that the satisfaction criterion was fulfilled.
        }

        /* For 'assert' we are not interested in performing any transformations as such operations don't really make
           sense for 'assert'. Overriding the method to return the input value as is without any transformations being
           performed */
        /* @Override
        protected Object getEvaluatedValue(CompilationRuntimeContext compilationRuntimeContext,
                                                          Object thisValue) throws XPathExpressionException {
            return thisValue;
        } */

        public static class AssertionException extends RuntimeException {
            private String assertionId = null;
            private String message = null;

            public AssertionException(String assertionId, String message) {
                super(message);
                this.assertionId = assertionId;
            }

            public String getAssertionId() {
                return assertionId;
            }

            public String toString() {
                return (assertionId == null? "": assertionId + ": " + getMessage());
            }
        }
    }
}
