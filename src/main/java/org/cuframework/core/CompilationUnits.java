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

import org.cuframework.config.ConfigManager;
import org.cuframework.el.EL;
import org.cuframework.el.EL.Expression;
import org.cuframework.el.ExpressionRuntimeContext;
import org.cuframework.MapOfMaps;
import org.cuframework.func.IFunction;
import org.cuframework.serializer.CompilationUnitsSerializationFactory;
import org.cuframework.util.cu.FileIO;
import org.cuframework.util.cu.HttpIO;
import org.cuframework.util.cu.RdbmsIO;
import org.cuframework.util.cu.LoadProperties;
import org.cuframework.util.UtilityFunctions;
import org.cuframework.ns.NamespaceDynamicTemplatesHandler;

import java.lang.reflect.Array;
import java.text.MessageFormat;
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

    public static final String ROOT_CU_NAMESPACE_URI = "http://www.cuframework.org";

    private CompilationUnits() {
    }

    private static final java.util.Map<String, CompilationUnitsNamespace> NAMESPACES = new HashMap<>();
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
        coreCUs.put(TextBlock.TAG_NAME, TextBlock.class);
        coreCUs.put("#cdata-section", TextBlock.class);
        coreCUs.put(CuText.TAG_NAME, CuText.class);

        java.util.Map<String, Class<? extends ICompilationUnit>> moreCUs = new HashMap<String, Class<? extends ICompilationUnit>>();
        moreCUs.put(LoadProperties.TAG_NAME, LoadProperties.class);  //adding in more cu map as it is a utility cu and its ok to allow
                                                                     //applications to replace it with their own implementations.
        moreCUs.put(FileIO.TAG_NAME, FileIO.class);  //adding in more cu map as it is a utility cu and its ok to allow
                                                     //applications to replace it with their own implementations.
        moreCUs.put(RdbmsIO.TAG_NAME, RdbmsIO.class);  //adding in more cu map as it is a utility cu and its ok to allow
                                                       //applications to replace it with their own implementations.
        moreCUs.put(HttpIO.TAG_NAME, HttpIO.class);  //adding in more cu map as it is a utility cu and its ok to allow
                                                     //applications to replace it with their own implementations.

        NAMESPACES.put(ROOT_CU_NAMESPACE_URI,
                       new CompilationUnitsNamespace(ROOT_CU_NAMESPACE_URI,
                                                     coreCUs,
                                                     moreCUs,
                                                     DefaultPlatformFunctions.getCoreFunctions()));
    }

    public static ICompilationUnit getCompilationUnitForTag(String namespaceURI, String tagName) {
        CompilationUnitsNamespace cuNamespace = getCompilationUnitsNamespace(namespaceURI, false);
        return cuNamespace == null? null: cuNamespace.getCompilationUnitForTag(tagName);
    }

    public static Class<? extends ICompilationUnit> getCompilationClassForTag(String namespaceURI, String tagName) {
        CompilationUnitsNamespace cuNamespace = getCompilationUnitsNamespace(namespaceURI, false);
        return cuNamespace == null? null: cuNamespace.getCompilationClassForTag(tagName);
    }

    public static IFunction resolveFunction(String namespaceURI, String fnName) {
        CompilationUnitsNamespace cuNamespace = getCompilationUnitsNamespace(namespaceURI, false);
        return cuNamespace == null? null: cuNamespace.resolveFunction(fnName);
    }

    protected static Class<? extends ICompilationUnit> getCompilationClassForTag(String namespaceURI,
                                                                                 String tagName,
                                                                                 boolean attemptRootLookup) {
        CompilationUnitsNamespace cuNamespace = getCompilationUnitsNamespace(namespaceURI, false);
        return cuNamespace == null? null: cuNamespace.getCompilationClassForTag(tagName, attemptRootLookup);
    }

    protected static IFunction resolveFunction(String namespaceURI, String fnName, boolean attemptRootLookup) {
        CompilationUnitsNamespace cuNamespace = getCompilationUnitsNamespace(namespaceURI, false);
        return cuNamespace == null? null: cuNamespace.resolveFunction(fnName, attemptRootLookup);
    }

    public static boolean setCompilationClassForTag(String namespaceURI, String tagName, String tagClassName)
                                                                                   throws ClassNotFoundException, ClassCastException {
        //return setCompilationClassForTag(tagName, Class.forName(tagClassName).asSubclass(ICompilationUnit.class));
        return getCompilationUnitsNamespace(namespaceURI, true).setCompilationClassForTag(tagName, tagClassName);
    }

    public static boolean setCompilationClassForTag(String namespaceURI, String tagName, Class<? extends ICompilationUnit> tagClass) {
        return getCompilationUnitsNamespace(namespaceURI, true).setCompilationClassForTag(tagName, tagClass);
    }

    //This method can unset only 'more' units and not the 'core' units.
    public static Class<? extends ICompilationUnit> unsetCompilationUnitForTag(String namespaceURI, String tagName) {
        CompilationUnitsNamespace cuNamespace = getCompilationUnitsNamespace(namespaceURI, false);
        return cuNamespace == null? null: cuNamespace.unsetCompilationUnitForTag(tagName);
    }

    private static CompilationUnitsNamespace getCompilationUnitsNamespace(String namespaceURI, boolean create) {
        namespaceURI = namespaceURI == null? ROOT_CU_NAMESPACE_URI: namespaceURI;
        CompilationUnitsNamespace cuNamespace = NAMESPACES.get(namespaceURI);
        if (cuNamespace == null && create) {
            cuNamespace = new CompilationUnitsNamespace(namespaceURI);
            NAMESPACES.put(namespaceURI, cuNamespace);
        }
        return cuNamespace;
    }

    public static boolean isAssignableFrom(Class<? extends ICompilationUnit> intendedCUClass,
                                           Class<? extends ICompilationUnit> tagCUClass) {
        boolean assignable = false;
        if (intendedCUClass != null && tagCUClass != null) {
            assignable = intendedCUClass.isAssignableFrom(tagCUClass);
        }
        return assignable;
    }

    public static Object getValue(IEvaluable evaluable,
                                  CompilationRuntimeContext compilationRuntimeContext)
                                                                   throws XPathExpressionException {
        Object value = null;
        if (evaluable instanceof Group) {
            value = ((Group) evaluable).doBuild(compilationRuntimeContext);  //calling doBuild instead of build to avoid passing a serializer type
                                                                             //and just use the one that is available through the runtime context.
        } else if (evaluable != null) {
            value = evaluable.getValue(compilationRuntimeContext);
        }
        return value;
    }

    public static XPath getXPath() {
        return XPathFactory.newInstance().newXPath();
    }

    private static final XPath XPATH = getXPath();

    public interface ICompilationUnit {
        static final String ATTRIBUTE_ID = "id";

        java.util.Set<String> getAttributeNames();
        boolean isAttributeNative(String attr);
        String getAttribute(String key);
        String getAttribute(String key, CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;
        String getId();
        String getIdOrElse();
        String getIdOrElse(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;
        String getNodeName();  //should return the local name (i.e. without ns prefix) of the xml node.
                               //If local name is null then return the node name.
        String getNodeType();  //should return the type of the xml node.
        String getSerializableNodeName();  //should return the node name to be written at the time of
                                           //serialization (e.g. by the source serializer)
        String getSerializableNodeName(CompilationRuntimeContext compilationRuntimeContext)
                                               throws XPathExpressionException;  //should return the node name to be written at the time
                                                                                 //of serialization (e.g. by the source serializer)
        String getTagName();  //should return the name of the root cu class of which this
                              //cu is aliased e.g. group, select, valueof, condition etc.
        String getNamespacePrefix();  //namespace prefix of the node.
        String getNamespaceURI();
        boolean matchesIdOrElse(String idOrElse);
        void compile(Node n) throws XPathExpressionException;
        ICompilationUnit getChild(String idOrElseOfChild);
        <T extends ICompilationUnit> T[] getChildren(Class<T> type);
    }

    public abstract static class CompilationUnit implements ICompilationUnit {
        private Properties attributes = new Properties();
        private Node nodeContext = null;

        protected static enum TextBlockTreatment {
            CHILD("child"),
            EVAL_EXPRESSION("el"),
            SKIP("skip"),
            IGNORE("ignore"),
            NONE("none");

            private String treatment = "";
            private TextBlockTreatment(String treatment) {
                this.treatment = treatment;
            }
            private String getAsString() {
                return treatment;
            }
        }

        //define the attributes available for all compilation units.
        //private static final String ATTRIBUTE_ID = "id";
        private static final String ATTRIBUTE_TBT = "tbt";  //TBT = Text Block Treatment, allowed values = child, el, skip|ignore|none
        private static final String ATTRIBUTE_SERIALIZABLE_NODE_NAME = "serializableNodeName";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_ID, ATTRIBUTE_TBT, ATTRIBUTE_SERIALIZABLE_NODE_NAME};

        private static final String IMPLICIT_ATTRIBUTE_NAMESPACE_PREFIX = "--namespace-prefix--";
        private static final String IMPLICIT_ATTRIBUTE_NAMESPACE_URI = "--namespace-uri--";
        private static final String IMPLICIT_ATTRIBUTE_NODE_NAME = "--node-name--";
        private static final String IMPLICIT_ATTRIBUTE_NODE_TYPE = "--node-type--";  //this is to basically represent the xml node type (like text,
                                                                                     //cdata-section etc.). It is not to be confused with the type
                                                                                     //attribute of some cus like Group cu.

        private CompilationUnitComputationHelper cuch = null;  //should get initialized on demand

        Node getNodeContext() {
            return this.nodeContext;
        }

        void setNodeContext(Node n) {
            this.nodeContext = n;
        }

        protected CompilationUnitComputationHelper getComputationHelper() {
            if (cuch == null) {
                cuch = CompilationUnitComputationHelper.instance(this);
            }
            return cuch;
        }

        @Override
        public String getNodeName() {
            return getAttribute(IMPLICIT_ATTRIBUTE_NODE_NAME);
        }

        @Override
        public String getNodeType() {
            return getAttribute(IMPLICIT_ATTRIBUTE_NODE_TYPE);
        }

        @Override
        public String getSerializableNodeName() {
            return getAttribute(ATTRIBUTE_SERIALIZABLE_NODE_NAME);
        }

        @Override
        public String getSerializableNodeName(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            return getAttribute(ATTRIBUTE_SERIALIZABLE_NODE_NAME, compilationRuntimeContext);
        }

        @Override
        public String getNamespacePrefix() {
            return getAttribute(IMPLICIT_ATTRIBUTE_NAMESPACE_PREFIX);
        }

        @Override
        public String getNamespaceURI() {
            return getAttribute(IMPLICIT_ATTRIBUTE_NAMESPACE_URI);
        }

        @Override
        public java.util.Set<String> getAttributeNames() {
            return attributes.stringPropertyNames();
        }

        static String getAttributeValueIffAttributeIsDefined(String attribute, Node n) throws XPathExpressionException {
            Node attribNode = (Node) CompilationUnits.XPATH.evaluate(attribute, n, XPathConstants.NODE);
            if (attribNode != null) {
                return attribNode.getNodeValue();
            }
            return null;
        }

        void setAttribute(String key, String value, boolean mergeValue) {
            if (key != null && value != null) {
                if (mergeValue) {
                    String existingValue = getAttribute(key);
                    value = existingValue != null? existingValue + value: value;
                }
                attributes.setProperty(key, value);
            }
        }

        void setAttributeIffNew(String key, String value) {
            if (key != null && value != null && !attributes.containsKey(key)) {
                attributes.setProperty(key, value);
            }
        }

        @Override
        public String getAttribute(String key) {
            return attributes.getProperty(key);
        }

        /******************************************************************************************************************************/
        /************************ Start - New attribute methods added to generically support computed values **************************/
        /******************************************************************************************************************************/
        /******************************************************************************************************************************/

        @Override
        public String getAttribute(String key, CompilationRuntimeContext compilationRuntimeContext)
                                                                                throws XPathExpressionException {
            //return computeAttributeValue(getAttribute(key), compilationRuntimeContext);
            return getComputationHelper().computeAttributeValue(key, getAttribute(key), compilationRuntimeContext);
        }

        /**
            This method computes the attribute value using the following scheme:
            1. If the attribute value contains special characters that makes it eligible for computation then an attempt
               to resolve the computed value is made, and, on success, the computed value is returned.
            2. If the attribute value either doesn't contain the special characters that makes it eligible for computation or
               if the computation didn't successfully resolve to a value then the passed attribute value gets returned as is.

            Note: attributeName is used only for config lookup purpose. The actual evaluation would happen only using the attributeValue.
         */
        protected String computeAttributeValue(String attributeName,
                                               String attributeValue,
                                               CompilationRuntimeContext compilationRuntimeContext)
                                                                          throws XPathExpressionException {
            //return (String) computeAttributeValue(attributeValue, compilationRuntimeContext, true, false);
            return getComputationHelper().computeAttributeValue(attributeName, attributeValue, compilationRuntimeContext);
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
        @Override
        public String getIdOrElse() {
            return getId();
        }

        //This method provides support for dynamic resolution of the CU identifier. Any identifier containing
        //special characters (e.g. '$') would be dynamically computed.
        @Override
        public String getIdOrElse(CompilationRuntimeContext compilationRuntimeContext)
                                                                            throws XPathExpressionException {
            String computedIdOrElse = getIdOrElse();  //initialize the identifier with the uncomputed value
            /*
                //Let's not trim the identifier and expect that the same is provided correctly inside the
                //xml file. Trimming might bring in inconsistency between the values returned by non-parameterized
                //getIdOrElse() method and this method in case the computation wasn't required (e.g. when the
                //identifier string didn't contain dynamic elements) or when the computation failed (e.g. when no
                //child cu corresponding to the referenced identifier is found). In such cases the value returned by
                //non-parameterized getIdOrElse() method should be returned as is without any trimming/modifications.
                computedIdOrElse = computedIdOrElse != null ? computedIdOrElse.trim() : computedIdOrElse;
            */
            return computeAttributeValue(CompilationUnitComputationHelper.VIRTUAL_ATTRIBUTE_ID_OR_ELSE,
                                         computedIdOrElse,
                                         compilationRuntimeContext);
        }

        @Override
        public boolean matchesIdOrElse(String idOrElse) {
            if (idOrElse == null) {
                return false;
            }
            return idOrElse.equals(getIdOrElse());
        }

        @Override
        public void compile(Node n) throws XPathExpressionException {
            this.nodeContext = n;

            // compile all the attributes
            doCompileAttributes(n, doInitDefaultConfigAttributes(n));

            // set the implicit attributes
            if (n.getNamespaceURI() != null) {
                setAttribute(IMPLICIT_ATTRIBUTE_NAMESPACE_PREFIX, n.getPrefix(), false);
                setAttribute(IMPLICIT_ATTRIBUTE_NAMESPACE_URI, n.getNamespaceURI(), false);
            }
            setAttribute(IMPLICIT_ATTRIBUTE_NODE_NAME, UtilityFunctions.getLocalOrNodeName(n), false);  //prefer name without ns prefix
            setAttribute(IMPLICIT_ATTRIBUTE_NODE_TYPE, "" + n.getNodeType(), false);

            // compile the children
            doCompileChildren(n);
            postCompileChildren(n);
            //doCompile(n);
        }

        /******* Start - Default implementations of compilation related methods. It should suffice for most of the cases. *******/
        /******* If any of the subclasses need absolute control over any of the compilation tasks then they can override. *******/
        /************************************************************************************************************************/
        protected java.util.Set<String> doInitDefaultConfigAttributes(Node n) throws XPathExpressionException {
            String nodeName = UtilityFunctions.getLocalOrNodeName(n);
            String namespaceURI = n.getNamespaceURI();
            String tagName = getTagName();
            java.util.Map<String, Object> defaultConfigAttributes = ConfigManager.getInstance().getAttributes(nodeName, tagName, namespaceURI, null);
            for (Entry<String, Object> entry: defaultConfigAttributes.entrySet()) {
                String attribute = entry.getKey();
                Object attributeValue = entry.getValue();
                setAttribute(
                        attribute,
                        attributeValue == null? null: attributeValue.toString(),
                        false);
            }
            return ConfigManager.getInstance().getNamesOfMergeableAttributes(nodeName, tagName, namespaceURI, null);
        }

        protected void doCompileAttributes(Node n, java.util.Set<String> mergeableAttributes) throws XPathExpressionException {
            NodeList attrs = (NodeList) CompilationUnits.XPATH.evaluate("@*", n, XPathConstants.NODESET);
            for (int i = 0; i < attrs.getLength(); i++) {
                String attribute = attrs.item(i).getNodeName();
                String attributeValue = attrs.item(i).getNodeValue();
                setAttribute(
                        attribute,
                        attributeValue,
                        mergeableAttributes.contains(attribute));
            }
        }

        protected void doCompileChildren(Node n) throws XPathExpressionException {
            //compile all the children
            String childrenXPathExpr = treatTextBlockAsChild()? "*|text()": "*";
            NodeList nl = (NodeList) CompilationUnits.XPATH
                                         .evaluate(childrenXPathExpr, n, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                String nodeName = UtilityFunctions.getLocalOrNodeName(nl.item(i));
                String nodeNamespace = nl.item(i).getNamespaceURI();
                if (isChildTagRecognized(nodeNamespace, nodeName)) {
                    doCompileChild(i, nl.item(i));
                }
            }
        }

        protected void doCompileChild(int nodeIndex, Node n) throws XPathExpressionException {
            String nodeName = UtilityFunctions.getLocalOrNodeName(n);
            ICompilationUnit cu = CompilationUnits.getCompilationUnitForTag(n.getNamespaceURI(), nodeName);
            if (cu != null) {
                cu.compile(n);
                if (cu instanceof IEmptiable && ((IEmptiable) cu).isEmpty()) {
                    return;  //the cu is emptiable as well as empty (e.g. cu represents an empty text block) then we don't need to add it.
                }
                if (cu instanceof TextBlock) {
                    ((TextBlock) cu).setAttribute(IMPLICIT_ATTRIBUTE_NAMESPACE_PREFIX,
                                                  getNamespacePrefix(), false);  //inherit ns prefix from parent
                    ((TextBlock) cu).setAttribute(IMPLICIT_ATTRIBUTE_NAMESPACE_URI,
                                                  getNamespaceURI(), false);  //inherit namespace from parent
                    ((TextBlock) cu).setAttribute(ATTRIBUTE_ID,
                                                  (getIdOrElse() != null? getIdOrElse() + "-": "") + "#text-" + nodeIndex,
                                                  false);
                }
                doAddCompiledUnit(nodeName, cu);
            }
        }

        //Subclasses may override to support advanced cases like adding more children from other sources
        //(e.g. those inherited through cu-text defined as body of custom cu definitions in namespace).
        protected void postCompileChildren(Node n) throws XPathExpressionException {
            //nothing to do by default
        }
        /******* End - Default implementations of compilation related methods *******/
        /****************************************************************************/

        /**************************** Start - util methods ****************************/
        /******************************************************************************/
        static boolean areMatchingTypes(Class<? extends ICompilationUnit> t1, Class<? extends ICompilationUnit> t2) {
            return t1 == t2;
        }

        static boolean areAssignableTypes(Class<? extends ICompilationUnit> t1, Class<? extends ICompilationUnit> t2) {
            return CompilationUnits.isAssignableFrom(t1, t2);
        }

        //Note: The method would return null if element list is null.
        static <T extends ICompilationUnit> T[] getElementsFromList(List<T> elementList, T[] typeArray) {
            if (elementList != null) {
                return elementList.toArray(typeArray);
            }
            return null;
        }

        //Note: The method would return null if element list is null or the specified elementType is null. Else it searches inside
        //the elementList for elements of specified type and returns an array of elements of the same type.
        static <T extends ICompilationUnit> T[] getElementsFromList(List<ICompilationUnit> elementList, Class<T> elementType, T[] typeArray) {
            if (elementList != null && elementType != null) {
                return elementList.stream().filter(elem -> areAssignableTypes(elementType, elem.getClass()))
                                           .collect(java.util.stream.Collectors.toList())
                                           .toArray(typeArray);
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

        /***************************** Start - TBT methods *****************************/
        /*******************************************************************************/
        protected boolean treatTextBlockAsChild() {
            String tbt = getAttribute(ATTRIBUTE_TBT);
            return TextBlockTreatment.CHILD.getAsString().equalsIgnoreCase(tbt);
        }

        protected boolean treatTextBlockAsEvalExpr() {
            String tbt = getAttribute(ATTRIBUTE_TBT);
            return tbt == null || //by default text eval is enabled
                   TextBlockTreatment.EVAL_EXPRESSION.getAsString().equalsIgnoreCase(tbt);
        }
        /****************************** End - TBT methods ******************************/
        /*******************************************************************************/

        @Override
        public boolean isAttributeNative(String attr) {  //This method should be implemented by the subclasses to indicate if the
                                                         //attribute is also a native attribute of a particular cu. That information
                                                         //will help in performing actions like skipping of the native attributes at
                                                         //the time of specific serializations (e.g. source definition serialization
                                                         //of cu xml nodes). Generic attributes like the 'id' and the 'name' should
                                                         //usually be exempted from being flagged as native attributes.
            return UtilityFunctions.isItemInArray(attr,
                                                  new String[]{ATTRIBUTE_TBT, ATTRIBUTE_SERIALIZABLE_NODE_NAME,
                                                               IMPLICIT_ATTRIBUTE_NAMESPACE_PREFIX, IMPLICIT_ATTRIBUTE_NAMESPACE_URI,
                                                               IMPLICIT_ATTRIBUTE_NODE_NAME, IMPLICIT_ATTRIBUTE_NODE_TYPE});
        }

        protected abstract boolean isChildTagRecognized(String tagNamespaceURI, String tagName);
        protected abstract void doAddCompiledUnit(String cuTagName, ICompilationUnit cu);
        public abstract ICompilationUnit getChild(String idOrElseOfChild);
        public abstract <T extends ICompilationUnit> T[] getChildren(Class<T> type);
    }

    public interface ICondition extends ICompilationUnit {
        boolean matches(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;
    }

    public interface IEvaluable extends ICompilationUnit {
        boolean doOutputNullValue();  //if this method returns false and if the getValue method returns null then the
                                      //Set cu or the respective serializers should not add this cu's data to the output.
        Object getValue(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;
    }

    public interface IEmptiable extends ICompilationUnit {
        boolean isEmpty();
    }

    //Classes can implement this interface if they accept 'on' cu as one of their child and generically want to get the
    //satisfaction criteria considered at various steps viz during serialization, inside set/using/log cu blocks etc.
    public interface ISatisfiable extends ICompilationUnit {
        boolean satisfies(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;
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
        Object execute(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException;
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
        protected static final String ATTRIBUTE_MESSAGE_FORMAT = "messageFormat";
        protected static final String ATTRIBUTE_EXTRACTION_EXPRESSION = "extractionExpression";
        protected static final String ATTRIBUTE_MATCHER_GROUP = "matcherGroup";
        protected static final String ATTRIBUTE_EVAL_IF_NULL = "evalIfNull";
        protected static final String ATTRIBUTE_OUTPUT_NULL_VALUE = "outputNullValue";  //true or false, used while setting attributes in map or during serialization
        protected static final String ATTRIBUTE_NODE_EXPRESSION_TOKENIZER = "nodeExpressionTokenizer";
        protected static final String ATTRIBUTE_PGVIF = "pgvif";  //call 'postGetValue inside finally'. If this attribute is defined (irrespective of its value)
                                                                  //then the postGetValue would be called inside finally. Doing so may be useful in cases where
                                                                  //the cu performs some critical function and in case of exceptions may be interested in ensuring
                                                                  //actions like logging of the event takes place.
        protected static final String ATTRIBUTE_EVAL_EXTENT = "evalExtent";  //recognized values are D,V,X,DV,VD,DX,XD. This would control the evaluations inside
                                                                             //getEvaluatedValue(...) method. Not defining this attribute would enable all types
                                                                             //of evaluations i.e. dollared, variable and xpath evaluations.
        private static final String[] ATTRIBUTES = {ATTRIBUTE_MESSAGE_FORMAT, ATTRIBUTE_EXTRACTION_EXPRESSION,
                                                    ATTRIBUTE_MATCHER_GROUP, ATTRIBUTE_EVAL_IF_NULL,
                                                    ATTRIBUTE_OUTPUT_NULL_VALUE, ATTRIBUTE_NODE_EXPRESSION_TOKENIZER,
                                                    ATTRIBUTE_PGVIF, ATTRIBUTE_EVAL_EXTENT};

        protected static final String TRUE = "true";

        private boolean evalIfNull = true;    //by default if the value of this ECU evaluates to null
                                              //then we would still perform the transformation, if any
                                              //such expression is defined. This behavior could be changed
                                              //by setting the value of ATTRIBUTE_EVAL_IF_NULL to
                                              //anything other than 'true'. A typical case could be where
                                              //the transformation expression makes a reference to this
                                              //and when it won't make sense to perform transformation with
                                              //a null value (e.g. inside Select CU when no criteria has
                                              //been met).
        private boolean outputNullValue = false;  //using a separate boolean field (and not using the
                                                  //string value of the corresponding attribute available inside
                                                  //the attributes map for optimization/performance enhancement.

        //the following boolean variables are defined for performance optimizations. They would be initialized at
        //the time of compilation of attributes from the value of the attribute ATTRIBUTE_EVAL_EXTENT
        private boolean evalEnabled = true;
        private boolean xpathEvalEnabled = true;  //this would be applicable only if evalEnabled is true

        private Expression evalExpression = null;

        @Override
        public boolean doOutputNullValue() {
            return outputNullValue;
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || TEXT_NODE_XPATH.equals(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
        }

        @Override
        protected void doCompileAttributes(Node n, java.util.Set<String> mergeableAttributes) throws XPathExpressionException {
            super.doCompileAttributes(n, mergeableAttributes);
            /*
            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n),
                        mergeableAttributes.contains(attribute));
            }
            */

            boolean textEvalEnabled = treatTextBlockAsEvalExpr();
            String textNodePathAsStr = getAttributeValueIffAttributeIsDefined(TEXT_NODE_XPATH, n);
            if (textEvalEnabled && textNodePathAsStr != null && !"".equals(textNodePathAsStr.trim())) {
                setAttribute(TEXT_NODE_XPATH, textNodePathAsStr.trim(), mergeableAttributes.contains(TEXT_NODE_XPATH));
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

            String outputNullValueTmp = getAttribute(ATTRIBUTE_OUTPUT_NULL_VALUE);
            if (outputNullValueTmp == null || "".equals(outputNullValueTmp)) {
                outputNullValueTmp = "false";
            }
            outputNullValue = TRUE.equalsIgnoreCase(outputNullValueTmp);

            initEvalExtent();
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

            if (compilationRuntimeContext.getInternalContext() == value ||
                !value.equals(compilationRuntimeContext.getInternalContext())) {
                compilationRuntimeContext.getInternalContext().put(thisValueIdentifier, value);
            } else {
                //entering this block means that the final evaluated value of this cu is not referentially equal to the
                //internal context map but their object equality still holds true (i.e. .equals return true). In this
                //typical case 'value' represents a version of internal map (may be one obtained through call to
                //CompilationRuntimeContext.getImmutableInternalContext)) and we are trying to add the same onto itself.
                //As a result subsequently calls to methods like toString() over compilationRuntimeContext.getInternalContext()
                //may result in StackOverflowError. To avoid this, we will create a fresh hashmap instance, add all entries
                //of 'value' map inside it and then add this new map as value inside the internal context map so the object
                //equality check (.equals method) of the compilationRuntimeContext.getInternalContext() and this new map
                //would return false and we won't run into the subsequent StackOverflowError(s).

                java.util.Map<String, Object> cmap = new HashMap<>();
                cmap.putAll((java.util.Map<String, Object>) value);  //not expecting any class cast exception here
                compilationRuntimeContext.getInternalContext().put(thisValueIdentifier, cmap);
            }
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
                Object value = getFormattedValue(getExtractedGroupValue(doGetValue(compilationRuntimeContext)),
                                                 compilationRuntimeContext);
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

        //computed version to return message format value
        protected String getMessageFormat(CompilationRuntimeContext compilationRuntimeContext) {
            String msgFormat = null;
            try {
                //attempt computation
                msgFormat = getAttribute(ATTRIBUTE_MESSAGE_FORMAT, compilationRuntimeContext);  //invoking the computed version
            } catch(XPathExpressionException xpee) {
                msgFormat = getAttribute(ATTRIBUTE_MESSAGE_FORMAT);  //return the uncomputed value as fallback.
            }
            return msgFormat;
        }

        //if the message format property is specified then the value would be processed using the MessageFormat class.
        //subclasses to override if needed.
        protected Object getFormattedValue(Object value, CompilationRuntimeContext compilationRuntimeContext) {
            if (value == null) {
                return null;
            }
            String msgFormat = getMessageFormat(compilationRuntimeContext);  //using the computed version
            msgFormat = msgFormat == null || "".equals(msgFormat) ? null : msgFormat;
            if (msgFormat == null) {
                return value;  //no message formatter specified. Return the value as is.
            }
            return (new MessageFormat(msgFormat)).
                       format(value instanceof Object[] ? value : new Object[]{value});
        }

        //subclasses to override if needed.
        protected Object getExtractedGroupValue(Object value) {
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
                return Pattern.compile(extractionExpr).split(value.toString(), -1);  //using -1 as limit to consider trailing empty tokens also
            }
        }

        //return the value after computing the value of the text() expression using the variables
        //declared inside the internal context of CompilationRuntimeContext.
        protected Object getEvaluatedValue(CompilationRuntimeContext compilationRuntimeContext,
                                                        Object thisValue) throws XPathExpressionException {
            String nodeTextExpression = getEvalText();  //getAttribute(TEXT_NODE_XPATH);
            if (nodeTextExpression == null || "".equals(nodeTextExpression.trim())
                                           || SKIP_EVALUATION.equals(nodeTextExpression.trim())) {
                return thisValue;
            }

            Object returnValue = thisValue;
            if (_evalEnabled()) {
                Expression evalExpression = getEvalExpression();
                if (evalExpression != null) {
                    ExpressionRuntimeContext erc = ExpressionRuntimeContext.newInstance(this, compilationRuntimeContext).
                                                       setAdditionalContext(ExpressionRuntimeContext.ADDITIONAL_CONTEXT_THIS_VALUE, thisValue);
                    if (_xpathEvalEnabled()) {
                        erc.setAdditionalContext(ExpressionRuntimeContext.ADDITIONAL_CONTEXT_NODE, getNodeContext()).
                            setAdditionalContext(ExpressionRuntimeContext.ADDITIONAL_CONTEXT_XPATH, CompilationUnits.XPATH);
                    }
                    returnValue = evalExpression.getValue(erc);
                }
            }

            return returnValue;
        }

        protected Expression getEvalExpression() {
            if (evalExpression != null) {
                return evalExpression;
            }
            String evalText = getEvalText();
            if (evalText == null) {
                return null;
            }
            evalExpression = EL.parse(evalText);
            return evalExpression;
        }

        protected String getEvalText() {
            return getAttribute(TEXT_NODE_XPATH);
        }

        private String getEvalExtent() {
            return getAttribute(ATTRIBUTE_EVAL_EXTENT);
        }

        /********************************************************************************/
        /********** Controller functions for controlling the evaluation extent **********/
        /******************************* 6th Feb, 21 ************************************/
        /** Valid values are D, V, X, DV, VD, DX, XD where:

              D, DV, VD, DX, XD would enable dollered evaluation
              V, X, DV, VD, DX, XD would enable variable evaluation
              X, DX, XD would enable XPath evaluation

              Note: XPath evaluation would always result in prior variable evaluation.
         **/
        private void initEvalExtent() {
            String evalExtent = getEvalExtent();  //getAttribute(ATTRIBUTE_EVAL_EXTENT);
            if (evalExtent == null) {
                evalEnabled = true;
                xpathEvalEnabled = true;
            } else {
                boolean dollaredEvalEnabled = "D".equalsIgnoreCase(evalExtent) ||
                                              "DV".equalsIgnoreCase(evalExtent) ||
                                              "VD".equalsIgnoreCase(evalExtent) ||
                                              "DX".equalsIgnoreCase(evalExtent) ||
                                              "XD".equalsIgnoreCase(evalExtent);
                boolean variableEvalEnabled = "V".equalsIgnoreCase(evalExtent) ||
                                              "DV".equalsIgnoreCase(evalExtent) ||
                                              "VD".equalsIgnoreCase(evalExtent) ||
                                              "DX".equalsIgnoreCase(evalExtent) ||
                                              "XD".equalsIgnoreCase(evalExtent) ||
                                              "X".equalsIgnoreCase(evalExtent);
                xpathEvalEnabled = "X".equalsIgnoreCase(evalExtent) ||
                                   "DX".equalsIgnoreCase(evalExtent) ||
                                   "XD".equalsIgnoreCase(evalExtent);
                evalEnabled = dollaredEvalEnabled || variableEvalEnabled || xpathEvalEnabled;
            }
        }

        private boolean _evalEnabled() {
            return evalEnabled;
        }

        private boolean _xpathEvalEnabled() {
            return xpathEvalEnabled;
        }
        /********************************************************************************/
        /********************************************************************************/


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
        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Extends.TAG_NAME});

        private List<Extends> extensions = null;
        private boolean extensionsProcessed = false;

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
        protected void postCompileChildren(Node n) throws XPathExpressionException {
            super.postCompileChildren(n);
            try {
                String nodeName = UtilityFunctions.getLocalOrNodeName(n);
                String namespaceURI = n.getNamespaceURI();
                String cuBodyTemplateId = NamespaceDynamicTemplatesHandler.getCuBodyTemplateId(nodeName, namespaceURI);
                if (cuBodyTemplateId != null) {
                    Extends _extends = Extends.newInstance(n.getOwnerDocument().createElement(Extends.TAG_NAME), cuBodyTemplateId);
                    if (_extends != null) {
                        extensions.add(0, _extends);
                    } else {
                        //TODO log
                    }
                }
            } catch (org.cuframework.TemplateCompilationException tce) {
                throw new RuntimeException(tce);
            }
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (cu instanceof Extends) {
                extensions.add((Extends) cu);
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(Extends.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
            if (extensionsProcessed) {
                return (T) this;
            } else {
                T extendedCU = doExtend(compilationRuntimeContext, mctr);
                return extendedCU;
            }
        }

        protected boolean isIdOrElseDynamic() {
            return isIdOrElseDynamic(getIdOrElse());
        }

        protected boolean isIdOrElseDynamic(String rawIdOrElse) {
            //return CompilationUnit.isAttributeValueDynamic(rawIdOrElse);
            return getComputationHelper().
                          isAttributeDynamic(CompilationUnitComputationHelper.VIRTUAL_ATTRIBUTE_ID_OR_ELSE,
                                             rawIdOrElse);
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
        /* private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(
                                                                      new String[]{Condition.TAG_NAME, ValueOf.TAG_NAME, Get.TAG_NAME, Select.TAG_NAME}); */

        private List<Condition> conditions = null;
        //private EvaluableCompilationUnit evaluable = null;
        private IEvaluable evaluable = null;

        @Override
        public String getTagName() {
            return Conditional.TAG_NAME;
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
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
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(ICondition.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(IEvaluable.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
            return evaluable != null ? CompilationUnits.getValue(evaluable, compilationRuntimeContext) : null;
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
        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{ValueOf.TAG_NAME, Get.TAG_NAME});

        //private ValueOf valueOf = null;
        private IEvaluable evaluable = null;

        @Override
        public String getTagName() {
            return Condition.TAG_NAME;
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
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
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(IEvaluable.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
            Object evaluableValueTmp = CompilationUnits.getValue(evaluable, compilationRuntimeContext);
            //evaluableValueTmp = "".equals(evaluableValueTmp) ? null : evaluableValueTmp;  //commented out: 10th Mar 20.
                                                                                            //To check for empty string use the regex ^()$
                                                                                            //To check for any string (including empty string) use the regex ^(.*)$
            return evaluableValueTmp == null ? false : Pattern.matches(expr, evaluableValueTmp.toString());
        }
    }

    public static class ValueOf extends EvaluableCompilationUnit implements IEvaluable, ISatisfiable {
        public static final String TAG_NAME = "valueof";

        private static final String ATTRIBUTE_KEY = "key";
        private static final String ATTRIBUTE_DEFAULT_VALUE = "default";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_KEY, ATTRIBUTE_DEFAULT_VALUE};
        //private static final String CHILDREN_XPATH = "./map | ./internal-map";
        //private static final String CHILDREN_XPATH2 = "./on";
        /* private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(
                                                                        new String[]{Map.TAG_NAME, InternalMap.TAG_NAME, Json.TAG_NAME, On.TAG_NAME}); */

        private List<Map> maps = null;
        private On on = null;  //'on' condition would be used only if there are no map entries defined.

        @Override
        public String getTagName() {
            return ValueOf.TAG_NAME;
        }

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
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
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
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(Map.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(On.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
        public boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            return on == null || on.satisfies(compilationRuntimeContext);
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            //MapOfMaps mapOfMaps = compilationRuntimeContext.getExternalContext();
            Object value = null;
            String key = getKey(compilationRuntimeContext);
            for (Map map : maps) {
                if (map.satisfiesOn(compilationRuntimeContext)) {
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
            return value;
        }

        @Override
        protected Object getDefaultValue() {
            return getAttribute(ATTRIBUTE_DEFAULT_VALUE);
        }
    }

    //returns the java class type of value object returned by the enclosed evaluable child cu.
    public static class TypeOf extends EvaluableCompilationUnit implements IEvaluable, ISatisfiable {
        public static final String TAG_NAME = "typeof";

        private static final String ATTRIBUTE_DEFAULT_VALUE = "default";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_DEFAULT_VALUE};

        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{On.TAG_NAME});

        private IEvaluable evaluable = null;
        private On on = null;

        @Override
        public String getTagName() {
            return TypeOf.TAG_NAME;
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
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
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(IEvaluable.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(On.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
        public boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            return on == null || on.satisfies(compilationRuntimeContext);
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                    throws XPathExpressionException {
            Object value = null;
            if (evaluable != null) {
                value = CompilationUnits.getValue(evaluable, compilationRuntimeContext);
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
        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{On.TAG_NAME});

        private On on = null;

        @Override
        public String getTagName() {
            return Map.TAG_NAME;
        }

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
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(On.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
        public String getTagName() {
            return InternalMap.TAG_NAME;
        }

        @Override
        protected void doCompileAttributes(Node n, java.util.Set<String> mergeableAttributes) throws XPathExpressionException {
            super.doCompileAttributes(n, mergeableAttributes);
            setAttribute(ATTRIBUTE_CONTEXT, INTERNAL_CONTEXT, false);  //set the map context to internal irrespective
                                                                       //of the value defined in the source xml.
        }
    }

    public static class Json extends Map {
        public static final String TAG_NAME = "json";

        private static final String ATTRIBUTE_CONTAINER = "container";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_CONTAINER};

        @Override
        public String getTagName() {
            return Json.TAG_NAME;
        }

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
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
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
                    jsonObj = getValue(mapKeyName, map);  //map.get(mapKeyName);  //5th May, 20: Using getValue instead of direct key lookup as it supports key hierarchy traversal.
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
                    jsonObj = getValue(mapKeyName, map);  //map.get(mapKeyName);  //5th May, 20: Using getValue instead of direct key lookup as it supports key hierarchy traversal.
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
        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Conditional.TAG_NAME, Condition.TAG_NAME});

        private ICondition condition = null;

        @Override
        public String getTagName() {
            return On.TAG_NAME;
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (((cu instanceof Condition) || (cu instanceof Conditional)) && (condition == null)) {
                //we need just one 'condition' cu and in case of many defined, let's just retain the first instance that we got.
                condition = (ICondition) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(ICondition.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
            if (condition instanceof Conditional) {
                conditionArray = getElementAsUnitArray((Conditional) condition, type);
            } else if (condition instanceof Condition) {
                conditionArray = getElementAsUnitArray((Condition) condition, type);
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

    public static class TextBlock extends EvaluableCompilationUnit implements IEvaluable, IEmptiable {
        public static final String TAG_NAME = "#text";

        private String text = null;

        @Override
        public String getTagName() {
            return TextBlock.TAG_NAME;
        }

        public String getText() {
            return this.text;
        }

        @Override
        public boolean isEmpty() {
            return text == null || "".equals(text);
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return true;  //a text block cannot logically have any attributes and let's just return true from this method to skip the
                          //attribute serialization attempt by some specific Serializers (like the ones that serialize the source).
        }

        @Override
        protected void doCompileAttributes(Node n, java.util.Set<String> mergeableAttributes) throws XPathExpressionException {
            //-----------------------------------------------------------------------------------------
            String[] attributesToInheritFromParentNode = {ATTRIBUTE_MESSAGE_FORMAT, ATTRIBUTE_EXTRACTION_EXPRESSION,
                                                          ATTRIBUTE_MATCHER_GROUP, ATTRIBUTE_EVAL_IF_NULL,
                                                          ATTRIBUTE_OUTPUT_NULL_VALUE, ATTRIBUTE_NODE_EXPRESSION_TOKENIZER,
                                                          ATTRIBUTE_PGVIF, ATTRIBUTE_EVAL_EXTENT};
            for (String attribute: attributesToInheritFromParentNode) {
                setAttribute(
                            attribute,
                            getAttributeValueIffAttributeIsDefined("parent::*/@" + attribute, n),
                            mergeableAttributes.contains(attribute));  //Set specific attributes of this TextBlock cu
                                                                       //with the value of the parent attribute if one
                                                                       //is defined
            }
            //-----------------------------------------------------------------------------------------

            super.doCompileAttributes(n, mergeableAttributes);
            text = n.getNodeValue();
            if (text != null) {
                text = text.trim();
            }
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            //nothing to do
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return false;  //no children to be recognised of its own.
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ICompilationUnit> T[] getChildren(Class<T> type) {
            return (T[]) getZeroLengthArrayOfType(type);
        }

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                           throws XPathExpressionException {
            return this.text;
        }

        @Override
        protected String getEvalText() {
            return this.text;
        }
    }

    public static class CuText extends EvaluableCompilationUnit implements IEvaluable, IEmptiable, ISatisfiable {
        public static final String TAG_NAME = "cu-text";

        private static final String ATTRIBUTE_ON_TREATMENT = "on";  //valid values: ser|exec|serexec.
                                                                    //ser = serialize the On CU block
                                                                    //exec = execute the On CU block
                                                                    //serexec = serialize as well as execute
        private static final String[] ATTRIBUTES = {ATTRIBUTE_ON_TREATMENT};

        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{On.TAG_NAME});

        private String text = null;

        private On on = null;
        private boolean executeTheOnCU = false;
        private boolean serializeTheOnCU = false;

        @Override
        public String getTagName() {
            return CuText.TAG_NAME;
        }

        public String getText() {
            return this.text;
        }

        @Override
        public boolean isEmpty() {
            return text == null || "".equals(text);
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
        }

        @Override
        public boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
                                                                     throws XPathExpressionException {
            return on == null || on.satisfies(compilationRuntimeContext);
        }

        @Override
        protected java.util.Set<String> doInitDefaultConfigAttributes(Node n) throws XPathExpressionException {
            String attribute = ATTRIBUTE_EVAL_EXTENT;
            setAttribute(attribute, "N", false);  //by default let's not enable any eval. Specifying any value other than those that
                                                  //correspond to dollared, variable and xpath evaluations would mean no evaluation at all.

            return super.doInitDefaultConfigAttributes(n);  //attempt to set the default config attributes
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            super.doCompileChildren(n);
            java.util.Map<String, List<String>> excludeNamespaceNodes = null;
            if (!serializeTheOnCU) {
                excludeNamespaceNodes = new HashMap<String, List<String>>();
                excludeNamespaceNodes.put(n.getNamespaceURI(),
                                                Arrays.asList(new String[]{On.TAG_NAME}));  //the actual namespace uri of the node, as per
                                                                                            //the cu xml template, should be passed as key to
                                                                                            //ensure that the inclusion/omission of the 'on'
                                                                                            //cu during serialization is expectedly performed
                                                                                            //inside the util method. We reached here only
                                                                                            //because the root ns is inherited by all other
                                                                                            //namespaces, and, because none of them would have
                                                                                            //provided a custom cu impl matching the node/tag name
                                                                                            //but that doesn't mean the node belongs to the
                                                                                            //default/root ns inside the cu xml template.
                                                                                            //The runtime ns uri of this node, when inspected
                                                                                            //inside the util class could be different than null
                                                                                            //(or the root ns uri) and, if not passed as key of
                                                                                            //this node exclusion map, result in a buggy
                                                                                            //serialization behavior wrt excluding nodes.
            }
            text = UtilityFunctions.serializeChildNodesToString(n, excludeNamespaceNodes);  //choosing to serialize the children and not
                                                                                            //the node itself. If for some reason this node
                                                                                            //also is to be serialized then have a similar
                                                                                            //node defined and added as a child to this node
                                                                                            //so the end result is closer to desired.
            if (text != null) {
                text = text.trim();
            }
        }

        @Override
        protected void initPerformanceVariables() {
            super.initPerformanceVariables();
            String ont = getAttribute(ATTRIBUTE_ON_TREATMENT);
            executeTheOnCU = ont == null || "exec".equalsIgnoreCase(ont) || "serexec".equalsIgnoreCase(ont);  //by default On execution is enabled
            serializeTheOnCU = ont != null && ("ser".equalsIgnoreCase(ont) || "serexec".equalsIgnoreCase(ont));
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return executeTheOnCU &&
                   CompilationUnits.isAssignableFrom(On.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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

        @Override
        protected Object doGetValue(CompilationRuntimeContext compilationRuntimeContext)
                                                                           throws XPathExpressionException {
            return this.text;
        }

        @Override
        protected String getEvalText() {
            return this.text;
        }
    }

    public static class Group extends ExtensibleCompilationUnit<Group> implements ISatisfiable {
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

        private static final String DEFAULT_SERIALIZATION_QUOTATION_MARKS = "\"";  //using double quotes by default
        private static final String PARAM_GROUP_SERIALIZER_TYPE = "group-serializer-type";

        private static final String ATTRIBUTE_NAME = "name";
        private static final String ATTRIBUTE_TYPE = "type";
        private static final String ATTRIBUTE_CUSTOM_BUILDER = "customBuilder";
        private static final String ATTRIBUTE_SELF_SERIALIZATION_POLICY = "selfSerializationPolicy";
        private static final String ATTRIBUTE_CHILD_SERIALIZATION_POLICY = "childSerializationPolicy";
        private static final String ATTRIBUTE_SERIALIZATION_QUOTES = "serializationQuotes";  //double quote, single quote, no quote (empty string) or any other char
                                                                                             //combination that is to be used to wrap keys/values during serialization.
        private static final String ATTRIBUTE_ESCAPE_QUOTES = "escapeQuotes";  //true value indicates that the quotation marks needs to be escaped
                                                                               //from inside the final serialized value of the group.
        private static final String[] ATTRIBUTES = {ATTRIBUTE_NAME, ATTRIBUTE_TYPE, ATTRIBUTE_CUSTOM_BUILDER,
                                                    ATTRIBUTE_SELF_SERIALIZATION_POLICY,
                                                    ATTRIBUTE_CHILD_SERIALIZATION_POLICY,
                                                    ATTRIBUTE_SERIALIZATION_QUOTES,
                                                    ATTRIBUTE_ESCAPE_QUOTES};

        //private static final String CHILDREN_XPATH = "./set | ./group";
        //private static final String CHILDREN_XPATH2 = "./init";
        //private static final String CHILDREN_XPATH3 = "./on";
        /* private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(
                                                                       new String[]{Set.TAG_NAME, Group.TAG_NAME, Init.TAG_NAME, Finally.TAG_NAME, On.TAG_NAME}); */

        private List<ICompilationUnit> children = null;
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

        @Override
        public String getTagName() {
            return Group.TAG_NAME;
        }

        public final boolean isHeadless() {
            return isHeadless;
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
        }

        @Override
        protected void doCompileAttributes(Node n, java.util.Set<String> mergeableAttributes) throws XPathExpressionException {
            super.doCompileAttributes(n, mergeableAttributes);

            /*
            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n),
                        mergeableAttributes.contains(attribute));
            }
            */

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
            children = new LinkedList<ICompilationUnit>();  //since mostly we would be operating
                                                            //sequentially so
                                                            //LinkedList would be an optimal
                                                            //choice
            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            super.doAddCompiledUnit(tagName, cu);

            if (cu instanceof Extends) {
                return;  //returning because extends would have already been added by call to super.
            }

            if (cu instanceof Set) {
                children.add(cu);
            } else if (cu instanceof Group) {
                children.add(cu);
            } else if (cu instanceof IEvaluable) {
                children.add(cu);
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
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return super.isChildTagRecognized(tagNamespaceURI, tagName) ||
                   CompilationUnits.isAssignableFrom(Init.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(Finally.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(IEvaluable.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(On.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
            if (children != null) {
                for (ICompilationUnit child : children) {
                    if (idOrElseOfChild.equals(child.getIdOrElse())) {
                        return child;
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
            if (areMatchingTypes(ICompilationUnit.class, type)) {
                ICompilationUnit[] allChildren = getElementsFromList(children, ICompilationUnit.class, new ICompilationUnit[0]);
                return (T[]) (allChildren == null? getZeroLengthArrayOfType(type): allChildren);
            } else if (areMatchingTypes(IEvaluable.class, type)) {
                IEvaluable[] evaluableChildren = getElementsFromList(children, IEvaluable.class, new IEvaluable[0]);
                return (T[]) (evaluableChildren == null? getZeroLengthArrayOfType(type): evaluableChildren);
            } else if (areMatchingTypes(Set.class, type)) {
                Set[] sets = getElementsFromList(children, Set.class, new Set[0]);
                return (T[]) (sets == null? getZeroLengthArrayOfType(type): sets);
            } else if (areMatchingTypes(Group.class, type)) {
                Group[] groups = getElementsFromList(children, Group.class, new Group[0]);
                return (T[]) (groups == null? getZeroLengthArrayOfType(type): groups);
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
            return findChild(idOrElse, getChildren(Group.class));
        }

        private Set findChildSet(String idOrElse) {
            return findChild(idOrElse, getChildren(Set.class));
        }

        /* private <C extends ICompilationUnit, T extends List<C>> C findChild(String idOrElse, T childList) {
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
        } */

        private <C extends ICompilationUnit> C findChild(String idOrElse, C[] childList) {
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

            clonedGrp.children = new LinkedList<ICompilationUnit>();
            clonedGrp.children.addAll(children);
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
            Group extendedUnit = this.getClone();  //this;  //25th Jul, 20: Using a clone instead of just the reference to this.
            boolean canMarkExtensionsAsProcessed = !hasConditionalExtends();
            for (Extends extension : getExtensions()) {
                Extends.ExtensionOperation opType = extension.getExtensionOperation();
                Extends.ExtensionScope extScope = extension.getExtensionScope();
                Object baseUnitPath = extension.getValue(compilationRuntimeContext);
                if (baseUnitPath == null) {
                    continue;
                }
                Group baseUnit = mctr.getCompilationUnit(baseUnitPath.toString(), Group.class);
                boolean[] singleElemArrayIndicatingWhetherAnyBaseUnitHasDynamicId = {false};
                extendedUnit = Group.doExtendFromBase(extendedUnit,
                                            baseUnit.extend(compilationRuntimeContext, mctr),
                                            opType,
                                            extScope,
                                            true,
                                            compilationRuntimeContext,
                                            mctr,
                                            singleElemArrayIndicatingWhetherAnyBaseUnitHasDynamicId);
                canMarkExtensionsAsProcessed = canMarkExtensionsAsProcessed &
                                                        baseUnit.areExtensionsMarkedAsProcessed() &
                                                            /*extendedUnit.areExtensionsMarkedAsProcessed()*/
                                                            !singleElemArrayIndicatingWhetherAnyBaseUnitHasDynamicId[0];
            }
            canMarkExtensionsAsProcessed = canMarkExtensionsAsProcessed &
                                                   extendedUnit.doExtendImmediateChildren(compilationRuntimeContext,  //Changed from doExtendImmediateChildren(...) to
                                                                                                                      //extendedUnit.doExtendImmediateChildren(...)
                                                                                          mctr, this) &
                                                          extendedUnit.doExtendLifecycleBlocks(compilationRuntimeContext, mctr);  //added on 28th May, 20
            extendedUnit.markExtensionsAsProcessed(true);  //This is a cloned version and valid only for a specific compilationRuntimeContext instance.
                                                           //Let's mark it's extensions proceessed status to always true.
            /*if (!canMarkExtensionsAsProcessed) {
                markExtensionsAsProcessed(false);
            }*/
            markExtensionsAsProcessed(canMarkExtensionsAsProcessed);  //Let's mark the actual extensions processed status on
                                                                      //the orig unit from which the extendedUnit was cloned.
            return extendedUnit;
        }

        private static Group[] getCommonChildren(Group checkInsideGroup, Group checkFromGroup) {
            Group[] sourceGroups = checkInsideGroup.getChildren(Group.class);
            Group[] intersectionGroups = checkFromGroup.getChildren(Group.class);
            List<Group> commonGroups = new LinkedList<>();
            for (Group grp: sourceGroups) {
                for (Group intersectionGrp: intersectionGroups) {
                    if (grp == intersectionGrp) {
                        commonGroups.add(grp);
                        break;
                    }
                }
            }
            return commonGroups.toArray(new Group[0]);
        }

        private boolean doExtendImmediateChildren(CompilationRuntimeContext compilationRuntimeContext,
                                                  CompiledTemplatesRegistry mctr,
                                                  Group origGrp) throws XPathExpressionException {
            boolean allGroupsCacheable = true;
            java.util.Map<Group, Group> extendedGrpMap = new HashMap<Group, Group>();
            Group[] thisGroups = getCommonChildren(this, origGrp);  //this.getChildren(Group.class);
            if (thisGroups != null) {
                for (Group group : thisGroups) {
                    if (!group.areExtensionsMarkedAsProcessed()){
                        Group extendedGrp = group.extend(compilationRuntimeContext, mctr);
                        //if (extendedGrp.areExtensionsMarkedAsProcessed()) {  //Irrespective of whether the extensions can be marked as
                                                                               //processed or not we would like to update the extended
                                                                               //group's reference inside the cu's groups list or else the right
                                                                               //structure would not be available for processing. Further, since
                                                                               //we are now processing the entire extension logic on a cloned copy
                                                                               //obtained at the beginning of doExtend method we don't even risk
                                                                               //corrupting the master copy with a transient group which was valid
                                                                               //only for this extension call and could return a completely different
                                                                               //structure if another call to doExtend is made (owing to existence of
                                                                               //conditional extends or dynamic group names ($ed id))
                            extendedGrpMap.put(group, extendedGrp);
                        //}
                        //allGroupsCacheable = allGroupsCacheable & extendedGrp.areExtensionsMarkedAsProcessed();
                        allGroupsCacheable = allGroupsCacheable & group.areExtensionsMarkedAsProcessed();
                                                                           //In the above statemnt we should check the extensions processed status
                                                                           //on the orig group and not the extendedGrp because the extendedGrp is
                                                                           //a clone of base and its 'extensions processed' status is always marked
                                                                           //as true. It's instance is valid only for a specific compilationRuntimeContext
                                                                           //and doesn't represent the finality. The orig however however is updated with
                                                                           //the final extensions processing status and only if there were no
                                                                           //<conditional extends>, <dynamic group idOrElse in the base units> its extension
                                                                           //processing status would be set to true.
                    }
                }
            }
            for (Entry<Group, Group> entry : extendedGrpMap.entrySet()) {
                int index = children.indexOf(entry.getKey());
                children.add(index, entry.getValue());
                children.remove(index + 1);
            }
            return allGroupsCacheable;
        }

        /****************************************** 28th May 20 ******************************************/
        /******** Added support for extending eligible cunits present inside the lifecycle blocks ********/
        /************* using, init and finally blocks are considered as the lifecycle blocks *************/
        /*************************************************************************************************/
        protected boolean doExtendLifecycleBlocks(CompilationRuntimeContext compilationRuntimeContext,
                                                  CompiledTemplatesRegistry mctr) throws XPathExpressionException {
            boolean allUnitsCacheable = true;
            if (init != null) {
                allUnitsCacheable &= doExtendLifecycleBlockUnits((List<ICompilationUnit>) (List<? extends ICompilationUnit>) init.getExecutables(),
                                                                 compilationRuntimeContext,
                                                                 mctr);
            }
            if (finallyy != null) {
                allUnitsCacheable &= doExtendLifecycleBlockUnits((List<ICompilationUnit>) (List<? extends ICompilationUnit>) finallyy.getExecutables(),
                                                                 compilationRuntimeContext,
                                                                 mctr);
            }
            return allUnitsCacheable;
        }

        protected boolean doExtendLifecycleBlockUnits(List<ICompilationUnit> cus,
                                                      CompilationRuntimeContext compilationRuntimeContext,
                                                      CompiledTemplatesRegistry mctr) throws XPathExpressionException {
            boolean allUnitsCacheable = true;
            java.util.Map<ICompilationUnit, ICompilationUnit> extendedUnitsMap = new HashMap<>();
            if (cus != null) {
                for (ICompilationUnit cunit : cus) {
                    if (cunit instanceof IExtensible) {
                        IExtensible extendedUnit = (IExtensible) ((IExtensible) cunit).extend(compilationRuntimeContext, mctr);
                        //if (extendedUnit.areExtensionsMarkedAsProcessed()) {  //Irrespective of whether the extensions can be marked as
                                                                                //processed or not we would like to update the extended
                                                                                //group's reference inside the cu's groups list or else the right
                                                                                //structure would not be available for processing. Further, since
                                                                                //we are now processing the entire extension logic on a cloned copy
                                                                                //obtained at the beginning of doExtend method we don't even risk
                                                                                //corrupting the master copy with a transient group which was valid
                                                                                //only for this extension call and could return a completely different
                                                                                //structure if another call to doExtend is made (owing to existence of
                                                                                //conditional extends or dynamic group names ($ed id))
                            extendedUnitsMap.put(cunit, extendedUnit);
                        //}
                        //allUnitsCacheable = allUnitsCacheable & extendedUnit.areExtensionsMarkedAsProcessed();
                        allUnitsCacheable = allUnitsCacheable & ((IExtensible) cunit).areExtensionsMarkedAsProcessed();
                                                                           //In the above statemnt we should check the extensions processed status
                                                                           //on the orig cu and not the extended cu because the extended cu is a
                                                                           //clone of base and its 'extensions processed' status is always marked
                                                                           //as true. It's instance is valid only for a specific compilationRuntimeContext
                                                                           //and doesn't represent the finality. The orig however however is updated with
                                                                           //the final extensions processing status and only if there were no
                                                                           //<conditional extends>, <dynamic group idOrElse in the base units> its extension
                                                                           //processing status would be set to true.
                    }
                }
            }
            for (Entry<ICompilationUnit, ICompilationUnit> entry : extendedUnitsMap.entrySet()) {
                int index = cus.indexOf(entry.getKey());
                cus.add(index, entry.getValue());
                cus.remove(index + 1);
            }
            return allUnitsCacheable;
        }
        /*************************************************************************************************/
        /******************************** end - lifecycle blocks extension *******************************/
        /*************************************************************************************************/

        private static Group doExtendFromBase(Group superUnit,
                                              Group baseUnit,
                                              Extends.ExtensionOperation opType,
                                              Extends.ExtensionScope extScope,
                                              boolean isThroughExplicitInheritance,
                                              CompilationRuntimeContext compilationRuntimeContext,
                                              CompiledTemplatesRegistry mctr,
                                              boolean[] singleElemArrayIndicatingWhetherAnyBaseUnitHasDynamicId) throws XPathExpressionException {
            if (!isThroughExplicitInheritance) {
                //we need to check for id's in order to process inheritance
                String thisIdentifier = superUnit.getIdOrElse(compilationRuntimeContext);
                String thatIdentifier = baseUnit.getIdOrElse(compilationRuntimeContext);
                if (thisIdentifier == null || !thisIdentifier.equals(thatIdentifier)) {
                    return superUnit;
                }
            }

            Group thisClonedGrp = superUnit;  //superUnit.getClone();  //25th Jul, 20: We are now cloning inside the doExtend method itself so no need to again clone here.
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

                //merge the child CUs
                CompilationUnits.ICompilationUnit[] baseUnitCUs = baseUnit.getChildren(CompilationUnits.ICompilationUnit.class);
                for (ICompilationUnit thatCU: baseUnitCUs) {
                    if (thatCU instanceof Group) {
                        Group thatSubGroup = (Group) thatCU;
                        Group thisSubGroup = thisClonedGrp.findChildGroup(
                                                    thatSubGroup.getIdOrElse(compilationRuntimeContext));  //note that here we are trying to find a cu which matches the passed
                                                                                                           //id and is of type Group. If there is some other cu which is NOT of
                                                                                                           //type Group but has the same id then that would not be returned.
                        if (thisSubGroup != null) {
                            //groups require merging
                            Group extendedGrp = Group.doExtendFromBase(
                                                           thisSubGroup.extend(compilationRuntimeContext, mctr),
                                                           thatSubGroup.extend(compilationRuntimeContext, mctr),
                                                           opType,
                                                           extScope,
                                                           false,
                                                           compilationRuntimeContext,
                                                           mctr,
                                                           singleElemArrayIndicatingWhetherAnyBaseUnitHasDynamicId);
                            int indexOfGrpToBeExtended = thisClonedGrp.children.indexOf(thisSubGroup);
                            thisClonedGrp.children.add(indexOfGrpToBeExtended, extendedGrp);
                            thisClonedGrp.children.remove(indexOfGrpToBeExtended + 1);  //the old group would have moved
                                                                                        //to (index + 1)th location by
                                                                                        //now
                        } else {
                            if (!thatSubGroup.isIdOrElseDynamic()) {
                                //the baseUnit contains a group which is not overridden and thus it needs to be added
                                /* if (thisClonedGrp.findChild(thatEvaluable.getIdOrElse(compilationRuntimeContext),
                                                               thisClonedGrp.children.toArray(new ICompilationUnit[0])) == null)
                                                                                                 //uncomment this statement if we don't want to add
                                                                                                 //the base group as a child of this group in case
                                                                                                 //some other cu (which obviously cannot be of type
                                                                                                 //Group) has the same id as the base group.
                                */
                                thisClonedGrp.children.add(thatSubGroup.extend(compilationRuntimeContext, mctr));  //refer the related comment above
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
                            singleElemArrayIndicatingWhetherAnyBaseUnitHasDynamicId[0] = true;  //used to return value to the caller method
                        }
                    } else if (thatCU instanceof Set) {
                        Set thatSubSet = (Set) thatCU;
                        Set thisSubSet = thisClonedGrp.findChildSet(
                                                      thatSubSet.getIdOrElse(compilationRuntimeContext));  //note that here we are trying to find a cu which matches the passed
                                                                                                           //id and is of type Set. If there is some other cu which is NOT of
                                                                                                           //type Set but has the same id then that would not be returned.
                        if (thisSubSet == null) {
                            //the baseUnit contains a Set CU which is not overridden and thus it needs to be added
                            /* if (thisClonedGrp.findChild(thatEvaluable.getIdOrElse(compilationRuntimeContext),
                                                           thisClonedGrp.children.toArray(new ICompilationUnit[0])) == null)
                                                                                             //uncomment this statement if we don't want to add
                                                                                             //the base set as a child of this group in case
                                                                                             //some other cu (which obviously cannot be of type
                                                                                             //Set) has the same id as the base set.
                            */
                            thisClonedGrp.children.add(thatSubSet.extend(compilationRuntimeContext, mctr));  //refer the related comment above
                        }
                    } else {
                        ICompilationUnit thisCU = thisClonedGrp.findChild(thatCU.getIdOrElse(compilationRuntimeContext),
                                                                          thisClonedGrp.children.toArray(new ICompilationUnit[0]));
                        if (thisCU == null) {
                            //the baseUnit contains a CU which is not overridden and thus it needs to be added
                            thisClonedGrp.children.add(thatCU);
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
        public boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
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

        public String getQuotationMarks() {
            String quotationMarks = getAttribute(ATTRIBUTE_SERIALIZATION_QUOTES);
            if (quotationMarks == null) {
                String SYS_PROP_SERIALIZATION_QUOTATION_MARKS = "cu.group.serialization.quotation.marks";
                quotationMarks = System.getProperty(SYS_PROP_SERIALIZATION_QUOTATION_MARKS);
            }
            return quotationMarks != null? quotationMarks: DEFAULT_SERIALIZATION_QUOTATION_MARKS;
        }

        public final Object build(CompilationRuntimeContext compilationRuntimeContext,
                                    String returnType) throws XPathExpressionException {
            java.util.Map<String, Object> internalContext = compilationRuntimeContext.getInternalContext();
            if (internalContext == null) {
                internalContext = new HashMap<String, Object>();
                compilationRuntimeContext.setInternalContext(internalContext);
            }
            boolean internalContextContainedGroupSerializerType = internalContext.containsKey(PARAM_GROUP_SERIALIZER_TYPE);
            Object savedGroupSerializerType = internalContext.get(PARAM_GROUP_SERIALIZER_TYPE);
            try {
                internalContext.put(PARAM_GROUP_SERIALIZER_TYPE, returnType);
                return doBuild(compilationRuntimeContext);
            } finally {
                if (internalContextContainedGroupSerializerType) {
                    internalContext.put(PARAM_GROUP_SERIALIZER_TYPE, savedGroupSerializerType);
                } else {
                    internalContext.remove(PARAM_GROUP_SERIALIZER_TYPE);  //if there was no group-serializer-type key available inside the internal context
                                                                          //when we entered this method then let's remove the same from the internal context
                                                                          //map to ensure that it is the exact replica of what we started with.
                }
            }
        }

        protected Object doBuild(CompilationRuntimeContext compilationRuntimeContext)
                                                                throws XPathExpressionException {
            return getValue(compilationRuntimeContext);
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

            String serializerType = "json";  //default serializer type to be used
            java.util.Map<String, Object> internalContext = compilationRuntimeContext.getInternalContext();
            if (internalContext != null && internalContext.get(PARAM_GROUP_SERIALIZER_TYPE) instanceof String) {
                serializerType = (String) internalContext.get(PARAM_GROUP_SERIALIZER_TYPE);
            }

            Object value = CompilationUnitsSerializationFactory.getGroupSerializer(
                                CompilationUnitsSerializationFactory.SerializerType.fromString(serializerType)/*JSON*/).
                                                           serialize(compilationRuntimeContext, this);
            String QUOTATION_MARKS = getQuotationMarks();
            if (QUOTATION_MARKS != null) {
                QUOTATION_MARKS = QUOTATION_MARKS.trim();
            }
            return escapeQuotes && QUOTATION_MARKS != null && value instanceof String &&
                           ("\"".equals(QUOTATION_MARKS) || "'".equals(QUOTATION_MARKS))?
                                  ((String) value).replaceAll(QUOTATION_MARKS, "\\\\\\\\" + QUOTATION_MARKS): value;
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
        public String getTagName() {
            return HeadlessGroup.TAG_NAME;
        }

        @Override
        public void doCompileAttributes(Node n, java.util.Set<String> mergeableAttributes) throws XPathExpressionException {
            isHeadless = true;
            super.doCompileAttributes(n, mergeableAttributes);
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
        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Set.TAG_NAME, Unset.TAG_NAME});

        private List<IExecutable> executables = null;

        @Override
        public String getTagName() {
            return Init.TAG_NAME;
        }

        @Override
        protected void doCompileChildren(Node n) throws XPathExpressionException {
            executables = new LinkedList<IExecutable>();  //since mostly we would be operating
                                                   //sequentially so
                                                   //LinkedList would be an optimal
                                                   //choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            if (cu instanceof IExecutable) {
                executables.add((IExecutable) cu);
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(IExecutable.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
            }
            if (executables != null) {
                for (IExecutable executable : executables) {
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
            IExecutable[] executablesArray = areMatchingTypes(IExecutable.class, type) ? getElementsFromList(executables, new IExecutable[0]) : null;
            return (T[]) (executablesArray == null ? getZeroLengthArrayOfType(type) : executablesArray);
        }

        public void execute(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            if (executables == null) {
                return;
            }
            for (IExecutable executable : executables) {
                executable.execute(compilationRuntimeContext);
            }
        }

        /**************************************************************************************/
        /***********Basically used while cloning a CU during inheritance processing************/
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
        public String getTagName() {
            return Finally.TAG_NAME;
        }

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
        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Using.TAG_NAME, Conditional.TAG_NAME});

        private Using using = null;
        private List<Conditional> conditionals = null;

        @Override
        public String getTagName() {
            return Select.TAG_NAME;
        }

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
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(Conditional.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(Using.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
                boolean matches = conditional.matches(compilationRuntimeContext);
                if (matches) {
                    value = conditional.getValue(compilationRuntimeContext);
                    break;
                }
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

        //instantiates and returns a new instance (in null namespace) with bare minimum attributes.
        //The cu would be compiled using the passed Node instance.
        public static Extends newInstance(Node n, String attributeDefaultValue) throws XPathExpressionException {
            Extends cu = (Extends) CompilationUnits.getCompilationUnitForTag(null, Extends.TAG_NAME);  //initialize an instance with null namespace
            if (cu != null) {
                cu.compile(n);
                cu.setAttribute(ATTRIBUTE_DEFAULT_VALUE, attributeDefaultValue, false);
            }
            return cu;
        }

        @Override
        public String getTagName() {
            return Extends.TAG_NAME;
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
        }

        @Override
        protected void initPerformanceVariables() {
            super.initPerformanceVariables();

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
            return getChildren(Conditional.class).length != 0 ||
                   (getEvalExpression() != null && getEvalExpression().isDynamic());
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
        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{ValueOf.TAG_NAME, Get.TAG_NAME});

        private List<IEvaluable> evaluables = null;

        @Override
        public String getTagName() {
            return Using.TAG_NAME;
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
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(IEvaluable.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
        }

        @Override
        public ICompilationUnit getChild(String idOrElseOfChild) {
            if (idOrElseOfChild == null) {
                return null;
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
            IEvaluable[] evaluablesArray = areMatchingTypes(IEvaluable.class, type) ?
                                                    getElementsFromList(evaluables, new IEvaluable[0]) : null;
            return (T[]) (evaluablesArray == null ? getZeroLengthArrayOfType(type) : evaluablesArray);
        }

        private java.util.Map<String, Object> initAndGetAllVariables(
                                                    CompilationRuntimeContext compilationRuntimeContext)
                                                                                    throws XPathExpressionException {
            java.util.Map<String, Object> mapTmp = new HashMap<String, Object>();
            for (IEvaluable evaluable: evaluables) {
                if (evaluable instanceof ISatisfiable &&
                    !((ISatisfiable) evaluable).satisfies(compilationRuntimeContext)) {
                    continue;  //the on condition didn't satisfy and this cu should be skipped.
                }
                mapTmp.put(evaluable.getId(), CompilationUnits.getValue(evaluable, compilationRuntimeContext));
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

        List<IEvaluable> getEvaluables() {
            return evaluables;
        }

        Using getClone() {
            Using clonedUsing = new Using();
            clonedUsing.copyAttributes(getAttributes());
            clonedUsing.evaluables = new LinkedList<IEvaluable>();
            clonedUsing.evaluables.addAll(getEvaluables());
            return clonedUsing;
        }
        /**************************************************************************************/
    }

    public static class Set extends ExtensibleCompilationUnit<Set> implements IExecutable, ISatisfiable {
        public static final String TAG_NAME = "set";

        private static final String ATTRIBUTE_ATTRIBUTE = "attribute";
        private static final String ATTRIBUTE_IN = "in";
        private static final String ATTRIBUTE_BREAK_ON_FIRST_VALUE_SET = "breakOnFirstValueSet";
        private static final String ATTRIBUTE_OVERRIDE_VALUE = "override";  //true or false
        private static final String ATTRIBUTE_CREATE_MAP_IF_MISSING = "createMapIfMissing";  //true or false
        private static final String[] ATTRIBUTES = {ATTRIBUTE_ATTRIBUTE, ATTRIBUTE_IN,
                                                    ATTRIBUTE_BREAK_ON_FIRST_VALUE_SET,
                                                    ATTRIBUTE_OVERRIDE_VALUE,
                                                    ATTRIBUTE_CREATE_MAP_IF_MISSING};
        //private static final String CHILDREN_XPATH = "./valueof | ./get | ./select";
        //private static final String CHILDREN_XPATH2 = "./on";
        /* private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(
                                                                       new String[]{ValueOf.TAG_NAME, Get.TAG_NAME, Select.TAG_NAME, On.TAG_NAME}); */

        private List<IEvaluable> evaluables = null;
        private On on = null;  //'on' condition, if present, must be satisfied in order for this Set CU to execute.

        private boolean breakOnFirstValueSet = true;  //using a separate boolean field (and not using the
                                                      //string value of the corresponding attribute available inside
                                                      //the attributes map for optimization/performance enhancement.
        private boolean overrideValue = true;  //using a separate boolean field (and not using the
                                               //string value of the corresponding attribute available inside
                                               //the attributes map for optimization/performance enhancement.
        private boolean createMapIfMissing = false;  //using a separate boolean field (and not using the
                                                     //string value of the corresponding attribute available inside
                                                     //the attributes map for optimization/performance enhancement.

        @Override
        public String getTagName() {
            return Set.TAG_NAME;
        }

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
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
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
            evaluables = new LinkedList<IEvaluable>(); // since mostly we would be operating
                                                  // sequentially so
                                                  // LinkedList would be an optimal
                                                  // choice

            super.doCompileChildren(n);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            super.doAddCompiledUnit(tagName, cu);

            if (cu instanceof IEvaluable) {
                evaluables.add((IEvaluable) cu);
            } else if ((cu instanceof On) && (on == null)) {
                //we need just one 'on' cu and in case of many defined, let's just retain the first instance that we got.
                on = (On) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return super.isChildTagRecognized(tagNamespaceURI, tagName) ||
                   CompilationUnits.isAssignableFrom(IEvaluable.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(On.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
                for (IEvaluable evaluable : evaluables) {
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
            markExtensionsAsProcessed(true);  //since we are not doing anything to process inheritance for set, let's mark extensions
                                              //as processed to ensure the overall marking of extension processing status of the
                                              //containing group cus are also done correctly (set cus can exist inside init, finally blocks
                                              //and the extensions_processed flag of all extensible units is taken into consideration
                                              //before marking the same at group level).
            return this;
        }

        @Override
        public boolean satisfies(CompilationRuntimeContext compilationRuntimeContext)
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
                //if (value != null || doOutputNullValue()) {
                //    mapTmp.put(attribute, value/* == null ? null : value.toString()*/);  //Using the raw value instead of toString
                //}
                setValueInMap(false, in, mapTmp, attribute, value);
            } else {
                //We have opted to throw a RuntimeException in case the expected output map is missing from the
                //MapOfMaps provided 'createMapIfMissing' flag is set to false. If 'createMapIfMissing' is set
                //to true then we would dynamically create the required map and also set it inside MapOfMaps.
                if (createMapIfMissing && in != null && !"".equals(in.trim())) {
                    java.util.Map<String, Object> map = new HashMap<String, Object>();
                    //if (value != null || doOutputNullValue()) {
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
            if (value != null || doOutputNullValue()) {
                if (attribute != null) {
                    map.put(attribute, value/* == null ? null : value.toString()*/);  //Using the raw value instead of toString
                } else if (value instanceof java.util.Map) {
                    for (Entry<String, Object> entry : ((java.util.Map<String, Object>) value).entrySet()) {  //if the map isn't assignable to Map<String, Object>
                                                                                                              //then an exception would be thrown here.
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
            for (IEvaluable evaluable : evaluables) {
                if (evaluable instanceof ISatisfiable &&
                    !((ISatisfiable) evaluable).satisfies(compilationRuntimeContext)) {
                    continue;  //the on condition didn't satisfy and this cu should be skipped.
                }

                value = CompilationUnits.getValue(evaluable, compilationRuntimeContext);
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
            }
            return value;
        }
    }

    public static class Unset extends CompilationUnit implements IExecutable {
        public static final String TAG_NAME = "unset";

        private static final String ATTRIBUTE_ATTRIBUTE = "attribute";
        private static final String[] ATTRIBUTES = {ATTRIBUTE_ATTRIBUTE};

        /* private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(
                                                                       new String[]{Map.TAG_NAME, InternalMap.TAG_NAME, Json.TAG_NAME, On.TAG_NAME}); */

        private List<Map> maps = null;
        private On on = null;

        @Override
        public String getTagName() {
            return Unset.TAG_NAME;
        }

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
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
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
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(Map.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(On.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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

        @Override
        public String getTagName() {
            return Get.TAG_NAME;
        }
    }

    public static class ExecutableGroup extends Group implements IExecutable {
        public static final String TAG_NAME = "executable-group";

        //private static final String CHILDREN_XPATH = "./using";
        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Using.TAG_NAME});

        private Using using = null;

        @Override
        public String getTagName() {
            return ExecutableGroup.TAG_NAME;
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            super.doAddCompiledUnit(tagName, cu);

            if ((cu instanceof Using) && (using == null)) {
                //we need just one 'using' cu and in case of many defined, let's just retain the first instance that we got.
                using = (Using) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return super.isChildTagRecognized(tagNamespaceURI, tagName) ||
                   CompilationUnits.isAssignableFrom(Using.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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

        /*
        //overridden to call execute by default as that makes logical sense for this executable group.
        @Override
        public Object build(CompilationRuntimeContext compilationRuntimeContext,
                                              String returnType) throws XPathExpressionException {
            return execute(compilationRuntimeContext);
        }
        */

        //overridden to call execute by default as that makes logical sense for this executable group.
        @Override
        protected Object doBuild(CompilationRuntimeContext compilationRuntimeContext)
                                                                 throws XPathExpressionException {
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
            String EXECUTION_ENDSTATE = "_execution-endstate";
            String EXECUTION_OUTCOME = "_execution-outcome";
            String EXECUTION_RESULT_MAP = "_execution-resultmap";
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

        @Override
        protected boolean doExtendLifecycleBlocks(CompilationRuntimeContext compilationRuntimeContext,
                                                  CompiledTemplatesRegistry mctr) throws XPathExpressionException {
            boolean allUnitsCacheable = super.doExtendLifecycleBlocks(compilationRuntimeContext, mctr);
            if (using != null) {
                allUnitsCacheable &= doExtendLifecycleBlockUnits((List<ICompilationUnit>) (List<? extends ICompilationUnit>) using.getEvaluables(),
                                                                 compilationRuntimeContext,
                                                                 mctr);
            }
            return allUnitsCacheable;
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
        public String getTagName() {
            return HeadlessExecutableGroup.TAG_NAME;
        }

        @Override
        public void doCompileAttributes(Node n, java.util.Set<String> mergeableAttributes) throws XPathExpressionException {
            isHeadless = true;
            super.doCompileAttributes(n, mergeableAttributes);
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

        private static final String LOOP_INPUT_PARAM_ITERABLE = "iterable";
        private static final String LOOP_INPUT_PARAM_START = "start";
        private static final String LOOP_INPUT_PARAM_END = "end";
        private static final String LOOP_INPUT_PARAM_TIMES = "times";
        private static final String LOOP_INPUT_PARAM_ITR_COMBINATOR = "itr-combinator";
        private static final String LOOP_INPUT_PARAM_ITR_JOINER = "itr-joiner";

        private static String LOOP_STATE_VARIABLE_ITERABLE_SIZE = "iterable-size";
        private static String LOOP_STATE_VARIABLE_START_INDEX = "start-index";
        private static String LOOP_STATE_VARIABLE_END_INDEX = "end-index";
        private static String LOOP_STATE_VARIABLE_INDEX = "index";
        private static String LOOP_STATE_VARIABLE_NUM_TIMES = "num-times";
        private static String LOOP_STATE_VARIABLE_ITEM_KEY = "item-key";
        private static String LOOP_STATE_VARIABLE_ITEM_VALUE = "item-value";  //value of item for use in the current iteration
        private static String LOOP_STATE_VARIABLE_LAST_ITR_VALUE = "itr-value";  //holds the value of the last completed iteration
        private static String LOOP_STATE_VARIABLE_LOOP_VALUE_SO_FAR = "loop-value";  //holds the consolidated value of the loop calculated so far

        private static final String ATTRIBUTE_JIDSVN = "jidsvn"; //joinIdWithStateVariableNames.
                                                                 //Any value, if set, other than true would indicate false.
        private static final String ATTRIBUTE_CLIVAS = "clivas"; //clearLoopInternalVariablesAtStart.
                                                                 //Any value, if set, other than true would indicate false.
        private static final String[] ATTRIBUTES = {ATTRIBUTE_JIDSVN, ATTRIBUTE_CLIVAS};

        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Break.TAG_NAME});

        private Break breakk;

        private boolean joinIdWithStateVariableNames = true;  //by default let's join id with state variable names to avoid name conflicts
                                                              //(with state variables of nested loops Or of internalContext in general)
        private boolean clearLoopInternalVariablesAtStart = false;  //this flag, if set to true, would clear all variables from the internal
                                                                    //context map that bears the same id/name as any of the loop internal variables.
                                                                    //By default its value is set to false and means that any variables defined
                                                                    //inside the internal map would be accessible inside this loop cu as well
                                                                    //and if they happen to match any of the state variables then it can affect
                                                                    //the overall loop execution. The variables can however always be overridden
                                                                    //by this loop cu in its 'using' block to control the behavior.

        @Override
        public String getTagName() {
            return Loop.TAG_NAME;
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
        }

        @Override
        protected void initPerformanceVariables() {
            super.initPerformanceVariables();

            String joinIdWithStateVariableNamesTmp = getAttribute(ATTRIBUTE_JIDSVN);
            if (joinIdWithStateVariableNamesTmp == null || "".equals(joinIdWithStateVariableNamesTmp)) {
                joinIdWithStateVariableNamesTmp = "true";
            }
            joinIdWithStateVariableNames = TRUE.equalsIgnoreCase(joinIdWithStateVariableNamesTmp);

            String clearLoopInternalVariablesAtStartTmp = getAttribute(ATTRIBUTE_CLIVAS);
            if (clearLoopInternalVariablesAtStartTmp == null || "".equals(clearLoopInternalVariablesAtStartTmp)) {
                clearLoopInternalVariablesAtStartTmp = "false";
            }
            clearLoopInternalVariablesAtStart = TRUE.equalsIgnoreCase(clearLoopInternalVariablesAtStartTmp);
        }

        @Override
        protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
            super.doAddCompiledUnit(tagName, cu);

            if ((cu instanceof Break) && (breakk == null)) {
                //we need just one 'break' cu and in case of many defined, let's just retain the first instance that we got.
                breakk = (Break) cu;
            }
        }

        @Override
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return super.isChildTagRecognized(tagNamespaceURI, tagName) ||
                   CompilationUnits.isAssignableFrom(Break.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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

        private void _clearLoopInternalVariablesAtStart(java.util.Map<String, Object> internalCtx) {
            if (!clearLoopInternalVariablesAtStart) {  //not interested in clearing any of the variables bearing the
                                                       //same name as one or the other loop's internal variables.
                return;
            }

            //clear the variables from internal map cpy that might represent the loop input/state variables so there is no
            //side effect in this loop due to it being contained in some outer loop or because of some value defined in
            //the internal map against a key bearing the same name as one of the loop variables. If there is a case of
            //genuinely accessing such variable values then pass them in loop using some other variable names.
            String[] loopInternalVariables = {LOOP_INPUT_PARAM_ITERABLE, LOOP_INPUT_PARAM_START,
                                              LOOP_INPUT_PARAM_END, LOOP_INPUT_PARAM_TIMES,
                                              //deliberating not clearing LOOP_INPUT_PARAM_ITR_COMBINATOR
                                              //and LOOP_INPUT_PARAM_ITR_JOINER here as inheriting them from
                                              //outer loop, if defined, may actually be convenient.
                                              //In rare cases these could go unnoticed to cause side effect.
                                              getStateVariable(LOOP_STATE_VARIABLE_ITERABLE_SIZE, internalCtx),
                                              getStateVariable(LOOP_STATE_VARIABLE_START_INDEX, internalCtx),
                                              getStateVariable(LOOP_STATE_VARIABLE_END_INDEX, internalCtx),
                                              getStateVariable(LOOP_STATE_VARIABLE_INDEX, internalCtx),
                                              getStateVariable(LOOP_STATE_VARIABLE_NUM_TIMES, internalCtx),
                                              getStateVariable(LOOP_STATE_VARIABLE_ITEM_KEY, internalCtx),
                                              getStateVariable(LOOP_STATE_VARIABLE_ITEM_VALUE, internalCtx),
                                              getStateVariable(LOOP_STATE_VARIABLE_LAST_ITR_VALUE, internalCtx),
                                              getStateVariable(LOOP_STATE_VARIABLE_LOOP_VALUE_SO_FAR, internalCtx),
                                              LOOP_STATE_VARIABLE_ITERABLE_SIZE, LOOP_STATE_VARIABLE_START_INDEX,
                                              LOOP_STATE_VARIABLE_END_INDEX, LOOP_STATE_VARIABLE_INDEX,
                                              LOOP_STATE_VARIABLE_NUM_TIMES, LOOP_STATE_VARIABLE_ITEM_KEY,
                                              LOOP_STATE_VARIABLE_ITEM_VALUE, LOOP_STATE_VARIABLE_LAST_ITR_VALUE,
                                              LOOP_STATE_VARIABLE_LOOP_VALUE_SO_FAR};

            Using[] usingAsUnitArray = getChildren(Using.class);
            for (String key : loopInternalVariables) {
                if (usingAsUnitArray.length == 1 && usingAsUnitArray[0] != null) {
                    if (usingAsUnitArray[0].getChild(key) == null) {  //if the using block hasn't overridden the key's value we should
                                                                      //just remove it to clear the corresponding internal variable.
                        internalCtx.remove(key);
                    }
                } else {
                    internalCtx.remove(key);
                }
            }
        }

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

                //By now we have given the opportunity to the 'using' block to execute and any overriding of the variables that the loop
                //was interested in doing would have already been done. Now let's attempt to clear the variables from the internal context
                //that bear the same name as one or the other loop's internal variables but have not been overridden by the loop.
                _clearLoopInternalVariablesAtStart(compilationRuntimeContext.getInternalContext());

                java.util.Map<String, Object> internalCtx = compilationRuntimeContext.getInternalContext();
                java.util.Map<String, Object> requestContext = internalCtx;

                if (internalCtx == null) {
                    //if there is no internal context available even now then we need not proceed as there would be
                    //no looping context variables available
                    return _noValue();  //"";  //since it's a group it makes sense to return a zero length string rather than null
                }

                boolean isIterableProvided = requestContext.containsKey(LOOP_INPUT_PARAM_ITERABLE);
                Object iterable = requestContext.get(LOOP_INPUT_PARAM_ITERABLE);
                //String iterableType = (String) requestContext.get("iterable-type");  //iterable might be string representation
                                                                                     //of object like json, properties etc.
                Object value = null;
                super.doInit(compilationRuntimeContext);  //call the initializer directly using super

                //internal context variables that should be made available inside finally block.
                //Further, any variables defined here should make sense outside the loop's iteration scope.
                final String ITERABLE_SIZE = getStateVariable(LOOP_STATE_VARIABLE_ITERABLE_SIZE, internalCtx);
                try {
                    if (!isIterableProvided/*iterable == null*/) {
                        value = loopTimes(internalCtx, compilationRuntimeContext);
                    } else if (iterable != null && iterable.getClass().isArray()) {
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

        private String getStateVariable(String key, java.util.Map<String, Object> lookupMap) {
            Object variable = lookupMap.get(key);
            String var = variable instanceof String? (String) variable: key;  //if a custom state variable is defined and if it is of type String then use
                                                                              //it's value. Else use key as is as the state variable.
            if (joinIdWithStateVariableNames && getIdOrElse() != null) {
                var = getIdOrElse() + "_" + var;  //using underscore as joiner
            }
            return var;
        }

        private Object loopTimes(java.util.Map<String, Object> internalCtx,
                                 CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            java.util.Map<String, Object> requestContext = internalCtx;

            Object startIndexAsObject = requestContext.get(LOOP_INPUT_PARAM_START);
            Object endIndexAsObject = requestContext.get(LOOP_INPUT_PARAM_END);
            Object numTimesAsObject = requestContext.get(LOOP_INPUT_PARAM_TIMES);

            final String START_INDEX = getStateVariable(LOOP_STATE_VARIABLE_START_INDEX, internalCtx);
            final String END_INDEX = getStateVariable(LOOP_STATE_VARIABLE_END_INDEX, internalCtx);
            final String LOOP_INDEX = getStateVariable(LOOP_STATE_VARIABLE_INDEX, internalCtx);
            final String NUM_TIMES = getStateVariable(LOOP_STATE_VARIABLE_NUM_TIMES, internalCtx);
            final String LAST_ITR_VALUE = getStateVariable(LOOP_STATE_VARIABLE_LAST_ITR_VALUE, internalCtx);  //holds the value of the last completed iteration
            final String LOOP_VALUE_SO_FAR = getStateVariable(LOOP_STATE_VARIABLE_LOOP_VALUE_SO_FAR,
                                                              internalCtx);  //holds the consolidated value of the loop calculated so far

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

            Object value = null;
            boolean[] singleElemArrayIndicatingWhetherFirstTime = {true};
            long iterationDelayMillisecs = _iterationDelay(internalCtx);
            try {
                for (int i = startIndex; endlessLoop? true: (decrement? i > endIndex: i < endIndex); i = decrement? i - 1: i + 1) {
                    //set the loop state variables inside internal context
                    internalCtx.put(LOOP_INDEX, i);

                    if (breakk != null && breakk.satisfies(compilationRuntimeContext)) {
                        break;
                    }

                    _delay(iterationDelayMillisecs, i != startIndex);  //inject applicable delay between the iterations

                    //evaluate the evaluables
                    value = loopBody(compilationRuntimeContext, singleElemArrayIndicatingWhetherFirstTime, value, internalCtx, LAST_ITR_VALUE);
                    internalCtx.put(LOOP_VALUE_SO_FAR, value);
                }
            } finally {
                internalCtx.remove(START_INDEX);
                internalCtx.remove(END_INDEX);
                internalCtx.remove(LOOP_INDEX);
                internalCtx.remove(NUM_TIMES);
                internalCtx.remove(LAST_ITR_VALUE);
                internalCtx.remove(LOOP_VALUE_SO_FAR);
            }
            return value;
        }

        private Object loopArray(Object array,
                                 java.util.Map<String, Object> internalCtx,
                                 CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            Object value = null;
            if (array == null) {
                return value;
            }
            boolean[] singleElemArrayIndicatingWhetherFirstTime = {true};
            final String ITEM_VALUE = getStateVariable(LOOP_STATE_VARIABLE_ITEM_VALUE, internalCtx);
            final String ITEM_INDEX = getStateVariable(LOOP_STATE_VARIABLE_INDEX, internalCtx);
            final String LAST_ITR_VALUE = getStateVariable(LOOP_STATE_VARIABLE_LAST_ITR_VALUE, internalCtx);  //holds the value of the last completed iteration
            final String LOOP_VALUE_SO_FAR = getStateVariable(LOOP_STATE_VARIABLE_LOOP_VALUE_SO_FAR,
                                                              internalCtx);  //holds the consolidated value of the loop calculated so far
            int arrayLength = Array.getLength(array);  //using reflection here to keep the logic of iterating array elements generic.
            long iterationDelayMillisecs = _iterationDelay(internalCtx);
            try {
                for (int i = 0; i < arrayLength; i++) {
                    internalCtx.put(ITEM_VALUE, Array.get(array, i));  //using reflection here to keep the logic of iterating array elements generic.
                    internalCtx.put(ITEM_INDEX, i);

                    if (breakk != null && breakk.satisfies(compilationRuntimeContext)) {
                        break;
                    }

                    _delay(iterationDelayMillisecs, i != 0);  //inject applicable delay between the iterations

                    //evaluate the evaluables
                    value = loopBody(compilationRuntimeContext, singleElemArrayIndicatingWhetherFirstTime, value, internalCtx, LAST_ITR_VALUE);
                    internalCtx.put(LOOP_VALUE_SO_FAR, value);
                }
            } finally {
                internalCtx.remove(ITEM_VALUE);
                internalCtx.remove(ITEM_INDEX);
                internalCtx.remove(LAST_ITR_VALUE);
                internalCtx.remove(LOOP_VALUE_SO_FAR);
            }
            return value;
        }

        private Object loopIterable(Iterable iterable,
                                    java.util.Map<String, Object> internalCtx,
                                    CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            Object value = null;
            if (iterable == null) {
                return value;
            }
            boolean[] singleElemArrayIndicatingWhetherFirstTime = {true};
            final String ITEM_VALUE = getStateVariable(LOOP_STATE_VARIABLE_ITEM_VALUE, internalCtx);
            final String ITEM_INDEX = getStateVariable(LOOP_STATE_VARIABLE_INDEX, internalCtx);
            final String LAST_ITR_VALUE = getStateVariable(LOOP_STATE_VARIABLE_LAST_ITR_VALUE, internalCtx);  //holds the value of the last completed iteration
            final String LOOP_VALUE_SO_FAR = getStateVariable(LOOP_STATE_VARIABLE_LOOP_VALUE_SO_FAR,
                                                              internalCtx);  //holds the consolidated value of the loop calculated so far
            long iterationDelayMillisecs = _iterationDelay(internalCtx);
            int index = 0;
            try {
                for (Object item: iterable) {  //for (int i = 0; i < iterable.size(); i++) {
                    internalCtx.put(ITEM_VALUE, item);
                    internalCtx.put(ITEM_INDEX, index++);

                    if (breakk != null && breakk.satisfies(compilationRuntimeContext)) {
                        break;
                    }

                    _delay(iterationDelayMillisecs, index != 1);  //inject applicable delay between the iterations

                    //evaluate the evaluables
                    value = loopBody(compilationRuntimeContext, singleElemArrayIndicatingWhetherFirstTime, value, internalCtx, LAST_ITR_VALUE);
                    internalCtx.put(LOOP_VALUE_SO_FAR, value);
                }
            } finally {
                internalCtx.remove(ITEM_VALUE);
                internalCtx.remove(ITEM_INDEX);
                internalCtx.remove(LAST_ITR_VALUE);
                internalCtx.remove(LOOP_VALUE_SO_FAR);
            }
            return value;
        }

        private Object loopMap(java.util.Map map,
                               java.util.Map<String, Object> internalCtx,
                               CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            Object value = null;
            if (map == null) {
                return value;
            }
            boolean[] singleElemArrayIndicatingWhetherFirstTime = {true};
            final String ITEM_KEY = getStateVariable(LOOP_STATE_VARIABLE_ITEM_KEY, internalCtx);
            final String ITEM_VALUE = getStateVariable(LOOP_STATE_VARIABLE_ITEM_VALUE, internalCtx);
            final String ITEM_INDEX = getStateVariable(LOOP_STATE_VARIABLE_INDEX, internalCtx);
            final String LAST_ITR_VALUE = getStateVariable(LOOP_STATE_VARIABLE_LAST_ITR_VALUE, internalCtx);  //holds the value of the last completed iteration
            final String LOOP_VALUE_SO_FAR = getStateVariable(LOOP_STATE_VARIABLE_LOOP_VALUE_SO_FAR,
                                                              internalCtx);  //holds the consolidated value of the loop calculated so far
            long iterationDelayMillisecs = _iterationDelay(internalCtx);
            int index = 0;
            try {
                for (Object _entry: map.entrySet()) {  //map.forEach((key,item) -> {
                    java.util.Map.Entry entry = (java.util.Map.Entry) _entry;
                    internalCtx.put(ITEM_KEY, entry.getKey());
                    internalCtx.put(ITEM_VALUE, entry.getValue());
                    internalCtx.put(ITEM_INDEX, index++);  //this represents the iteration count/index more than the item index in the case of map

                    if (breakk != null && breakk.satisfies(compilationRuntimeContext)) {
                        break;
                    }

                    _delay(iterationDelayMillisecs, index != 1);  //inject applicable delay between the iterations

                    //evaluate the evaluables
                    value = loopBody(compilationRuntimeContext, singleElemArrayIndicatingWhetherFirstTime, value, internalCtx, LAST_ITR_VALUE);
                    internalCtx.put(LOOP_VALUE_SO_FAR, value);
                }
            } finally {
                internalCtx.remove(ITEM_KEY);
                internalCtx.remove(ITEM_VALUE);
                internalCtx.remove(ITEM_INDEX);
                internalCtx.remove(LAST_ITR_VALUE);
                internalCtx.remove(LOOP_VALUE_SO_FAR);
            }
            return value;
        }

        private Object loopBody(CompilationRuntimeContext compilationRuntimeContext,
                                boolean[] singleElemArrayIndicatingWhetherFirstTime,
                                Object value,
                                java.util.Map<String, Object> internalCtx,
                                String itrValueHolderKey) throws XPathExpressionException {
            Object _value = iteration(compilationRuntimeContext);
            internalCtx.put(itrValueHolderKey, _value);
            if (isIterationCombinatorDisabled(internalCtx)) {
                value = _value;
            } else {
                //Object _value = iterationValueObj;
                if (_value != null && !"".equals(_value)) {
                    if (!singleElemArrayIndicatingWhetherFirstTime[0]) {
                        value += joiner(compilationRuntimeContext.getInternalContext());
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
            }
            return value;
        }

        //providing this mechanism to disable combining of iteration results. This approach would be
        //eventually be replaced with option to define a custom combinator when the 'Ref' cu gets introduced.
        private boolean isIterationCombinatorDisabled(java.util.Map<String, Object> internalCtx) {
            String ITR_COMBINATOR = LOOP_INPUT_PARAM_ITR_COMBINATOR;
            boolean combinatorKeyExists = internalCtx.containsKey(ITR_COMBINATOR);
            Object combinator = internalCtx.get(ITR_COMBINATOR);
            return (combinatorKeyExists && (combinator == null || "disabled".equalsIgnoreCase(combinator.toString())));
        }

        //returns joiner to be used for iteration results
        private String joiner(java.util.Map<String, Object> internalCtx) {
            Object joiner = internalCtx.get(LOOP_INPUT_PARAM_ITR_JOINER);
            return joiner != null? joiner.toString(): ",";  //in case of missing joiner, comma will be used as the default joiner.
        }

        //represents an iteration of the loop. Subclasses can override.
        protected Object iteration(CompilationRuntimeContext compilationRuntimeContext) throws XPathExpressionException {
            return getValue(compilationRuntimeContext);
        }

        //returns -1 if there is no meaningful delay specified. Else returns the delay in millisecs
        private long _iterationDelay(java.util.Map<String, Object> internalCtx) {
            java.util.Map<String, Object> requestContext = internalCtx;
            Object iterationDelayAsObject = requestContext.get("itr-delay-millisecs");
            long delayInMillisecs = -1;
            try {
                if (iterationDelayAsObject != null) {
                    delayInMillisecs = Long.parseLong(iterationDelayAsObject.toString());
                }
            } catch(NumberFormatException nfe) {
                delayInMillisecs = -1;
            }
            return delayInMillisecs;
        }

        private void _delay(long iterationDelayMillisecs, boolean isDelayApplicable) {
            if (iterationDelayMillisecs > 0 && isDelayApplicable) {
                try {
                    Thread.sleep(iterationDelayMillisecs);
                } catch(InterruptedException ie) {
                    //instead of muting this exception shall we allow it to be thrown?
                }
            }
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
            //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Conditional.TAG_NAME, Condition.TAG_NAME});

            private ICondition condition = null;

            @Override
            public String getTagName() {
                return Break.TAG_NAME;
            }

            @Override
            protected void doAddCompiledUnit(String tagName, ICompilationUnit cu) {
                if (((cu instanceof Condition) || (cu instanceof Conditional)) && (condition == null)) {
                    //we need just one 'condition' cu and in case of many defined, let's just retain the first instance that we got.
                    condition = (ICondition) cu;
                }
            }

            @Override
            protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
                return CompilationUnits.isAssignableFrom(ICondition.class,
                                                         CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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

        //private static final List<String> RECOGNIZED_CHILD_TAGS = Arrays.asList(new String[]{Using.TAG_NAME, On.TAG_NAME});

        private On on = null;
        private Using using = null;
        private List<IEvaluable> evaluables = null;

        private String loggingContext = null;  //this will have the same significance as that of a java class in log messages. This will help identify
                                               //as to which xml file the log statement was defined.

        @Override
        public String getTagName() {
            return Log.TAG_NAME;
        }

        @Override
        public boolean isAttributeNative(String attr) {
            return super.isAttributeNative(attr) || UtilityFunctions.isItemInArray(attr, ATTRIBUTES);
        }

        @Override
        protected void doCompileAttributes(Node n, java.util.Set<String> mergeableAttributes) throws XPathExpressionException {
            super.doCompileAttributes(n, mergeableAttributes);
            /*
            // compile all the attributes
            for (String attribute : ATTRIBUTES) {
                setAttribute(
                        attribute,
                        getAttributeValueIffAttributeIsDefined("@" + attribute, n),
                        mergeableAttributes.contains(attribute));
            }
            */

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
        protected boolean isChildTagRecognized(String tagNamespaceURI, String tagName) {
            return CompilationUnits.isAssignableFrom(IEvaluable.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(Using.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName)) ||
                   CompilationUnits.isAssignableFrom(On.class,
                                                     CompilationUnits.getCompilationClassForTag(tagNamespaceURI, tagName));
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
                if (evaluable instanceof ISatisfiable &&
                    !((ISatisfiable) evaluable).satisfies(compilationRuntimeContext)) {
                    continue;  //the on condition didn't satisfy and this cu should be skipped.
                }
                Object value = CompilationUnits.getValue(evaluable, compilationRuntimeContext);
                logger.logp(level, loggingContext,
                           /*("[" + (logIdOrElse != null? logIdOrElse + "#": "") + evaluable.getIdOrElse() + "]"),*/
                           (logIdOrElse + "#" + evaluable.getIdOrElse()),
                           (value != null? value.toString(): null));
            }
        }
    }

    //Assertion cu
    public static class Assert extends Conditional implements IExecutable {
        public static final String TAG_NAME = "assert";

        @Override
        public String getTagName() {
            return Assert.TAG_NAME;
        }

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
