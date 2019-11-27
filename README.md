# Compilation Units Framework (cu-framework)
_A very generic and powerful data driven coding and testing framework. A whole new paradigm enabling true data driven programming. [Try Live Fiddle](http://www.bracket.co.in/cufiddle)._

_<<<<<<Upcoming: Support for cu packages/namespaces.>>>>>>_
<br><br>

**Compilation Units and their categorization**: Compilation units (aka cus) are the building blocks of this cu-framework. These represent processing blocks that carry out specific tasks in a generic manner which when intertwined with each other can easily build complex programming logic. The compilation units have associated xml tags and the entire processing logic can be defined using a simple xml structure. The xml can be stored inside a database, an external file, on cloud, or even constructed on the fly which means the processing logic can now accompany the data and even picked/switched/built at runtime. The cu framework also supports inheritance where in the logic residing inside one or more base xml files can directly be imported and auto-merged to build a new one. The framework also provides a revolutionary data driven testing framework.

Categorization:
The following compilation units can be categorized as first class units that logically can have an independent existance.

* [ValueOf](#valueof)
* [Get](#valueof)
* [TypeOf](#typeof)
* [Set](#set)
* [Unset](#unset)
* [Select](#select)
* [Group](#group)
* [Headless Group](#headless-group)
* [Executable Group](#executable-group)
* [Headless Executable Group](#headless-executable-group)
* [Condition](#condition)
* [Conditional](#conditional)
* [Loop](#loop)
* [Log](#log)
* [Assert](#assert)
* [Load Properties](#load-properties)

The following compilation units are meaningful only when used inside their parent cus. Their independent existance is of not much use (unless they are directly referenced and processed by the consuming application).

* [Using](#using)
* [Init](#init)
* [Finally](#finally)
* [Extends](#extends)
* [Loop.Break](#break)
* [On](#on)
* [Map](#map)
* [Internal Map](#map)
* [Json](#json)


### Samples
1. <a id="valueof"></a>**valueof | get**

    _Java code_
    ```Java
    java.util.Map<String, java.util.Map<String, Object>> mapOfMaps = new java.util.HashMap<>();
    mapOfMaps.put("CONTEXT-MAP", new java.util.HashMap<String, Object>());  //code to add key-value pair of 'abc' inside CONTEXT-MAP is deliberately skipped
    Object id1 = mapOfMaps.get("CONTEXT-MAP").get("abc");
    ```
    _cu equivalent_
    ```
    <valueof id="id1" key="abc">
      <map name="CONTEXT-MAP"/>
    </valueof>
    ```
    _Note: 'get' is an alias tag of 'valueof' and can simply replace the later in the above cu snippet._

2. <a id="typeof"></a>**typeof**

    _Java code_
    ```Java
    java.util.Map<String, java.util.Map<String, Object>> mapOfMaps = new java.util.HashMap<>();
    mapOfMaps.put("CONTEXT-MAP", new java.util.HashMap<String, Object>());  //code to add key-value pair of 'abc' inside CONTEXT-MAP is deliberately skipped
    Object id1 = mapOfMaps.get("CONTEXT-MAP").get("abc");
    return id1.getClass().getName();  //if id1 represented a String value object then java.lang.String would be returned.
    ```
    _cu equivalent_
    ```
    <typeof>
      <valueof id="id1" key="abc">
        <map name="CONTEXT-MAP"/>
      </valueof>
    </typeof>
    <!-- the above would return 'java.lang.String' assuming 'id1' evaluated to a String value object.
    ```

3. <a id="set"></a>**set**

    _Scenario 1 Java code_
    ```Java
    Object id1 = "test-value";
    ```
    _cu equivalent_
    ```
    <set attribute="id1">
      <valueof default="test-value"/>
    </set>
    ```
    
    _Scenario 2 Java code_
    ```Java
    java.util.Map<String, java.util.Map<String, Object>> mapOfMaps = new java.util.HashMap<>();
    mapOfMaps.put("CONTEXT-MAP", new java.util.HashMap<String, Object>());
    mapOfMaps.get("CONTEXT-MAP").put("id1", "test-value");
    ```
    _cu equivalent_
    ```
    <set attribute="id1" in="CONTEXT-MAP">
      <valueof default="test-value"/>
    </set>
    ```

4. <a id="unset"></a>**unset**

    _Scenarion 1 Java Code_
    ```Java
    //initialization code
    java.util.Map<String, java.util.Map<String, Object>> mapOfMaps = new java.util.HashMap<>();
    mapOfMaps.put("CONTEXT-MAP", new java.util.HashMap<String, Object>());
    mapOfMaps.get("CONTEXT-MAP").put("id1", "test-value");
    mapOfMaps.get("CONTEXT-MAP").put("id2", "test-value2");
    
    //demonstration code
    mapOfMaps.get("CONTEXT-MAP").remove("id1");
    //context-map now doesn't contain the key 'id1'
    ```
    _cu equivalent_
    ```
    <unset attribute="id1">
      <map name="CONTEXT-MAP"/>
    </unset>
    ```
    
    _Scenarion 2 Java Code_
    ```Java
    //initialization code
    java.util.Map<String, java.util.Map<String, Object>> mapOfMaps = new java.util.HashMap<>();
    mapOfMaps.put("CONTEXT-MAP", new java.util.HashMap<String, Object>());
    mapOfMaps.get("CONTEXT-MAP").put("id1", "test-value");
    mapOfMaps.get("CONTEXT-MAP").put("id2", "test-value2");
    
    //demonstration code
    mapOfMaps.get("CONTEXT-MAP").clear();
    //context-map is now empty
    ```
    _cu equivalent_
    ```
    <unset>
      <map name="CONTEXT-MAP"/>
    </unset>
    ```

5. <a id="select"></a>**select**

    _Java Code_
    ```Java
    Object testInputObj = <assume some value assigned to it>;
    Object returnValue = null;
    if ("123".equals(testInputObj)) {
        returnValue = "out-123";
    } else if ("abc".equals(testInputObj)) {
        returnValue = "out-abc";
    } else if ("xyz".equals(testInputObj)) {
        returnValue = "out-xyz";
    } else {
        returnValue = "out-default-value";
    }
    return returnValue;
    ```
    _cu equivalent_
    ```
    <select>
      <using>
        <valueof id="testInputObj" default="xyz"/> . <!-- instead of statically assigning a value of 'xyz' the valueof tag can also lookup and use value of a key inside a map. Refer [Valueof](#valueof) for example.-->
      </using>
      <conditional expression="1" value="out-123">
        <condition id="1" expression="123">
          <valueof>testInputObj</valueof>
        </condition>
      </conditional>
      <conditional expression="1" value="out-abc">
        <condition id="1" expression="abc">
          <valueof>testInputObj</valueof>
        </condition>
      </conditional>
      <conditional expression="1" value="out-xyz">
        <condition id="1" expression="xyz">
          <valueof>testInputObj</valueof>
        </condition>
      </conditional>
      <conditional value="out-default-value"/>
    </select>
    ```

6. <a id="group"></a>**group**

    _Java Code_
    ```Java
    Object attr1 = "attribute1 value";
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("'wrapper':{");
    strBuilder.append("'attr1':'");
    strBuilder.append(attr1);
    strBuilder.append("'");
    strBuilder.append("}");
    return strBuilder.toString();  //this would return the json 'wrapper':{'attr1':'attribute1 value'}
    ```
    _cu equivalent_
    ```
    <group name="wrapper">
      <set attribute="attr1">
        <valueof default="attribute1 value"/>
      </set>
    </group>
    ```
    _Note: group can have any number of sub groups and set units inside it. Also nesting upto nth level is supported._

7. <a id="headless-group"></a>**headless-group**

    _Java Code_
    ```Java
    Object attr1 = "attribute1 value";
    StringBuilder strBuilder = new StringBuilder();
    //strBuilder.append("'wrapper':{");  //note that this is commented
    strBuilder.append("'attr1':'");
    strBuilder.append(attr1);
    strBuilder.append("'");
    //strBuilder.append("}");  //this is also commented
    return strBuilder.toString();  //this would return the json representation 'attr1':'attribute1 value'
    ```
    _cu equivalent_
    ```
    <headless-group name="wrapper">
      <set attribute="attr1">
        <valueof default="attribute1 value"/>
      </set>
    </headless-group>
    ```
    _Note: headless-group is also a type of group._

8. <a id="executable-group"></a>**executable-group**

    executable-group is also a type of group - the only difference being how it gets processed at runtime. Conceptually a group is evaluable but an executable group is evaluable as well as executable. In Java terms what this means is that the group implements an IEvaluable interface but an executable group implements both IEvaluable and IExecutable interfaces. For detailed documentation refer the link _TBD_

    _Java Code_
    ```
    Same as that provided for group earlier.
    ```
    _cu equivalent_
    ```
    Same as that provided for group earlier with the difference that in place of 'group' the tag to use would be 'executable-group' (quotes excluded).
    ```

9. <a id="headless-executable-group"></a>**headless-executable-group**

    headless-executable-group is also a type of group - the only difference being how it gets processed at runtime. Conceptually a group is evaluable but an executable group is evaluable as well as executable. In Java terms what this means is that the group implements an IEvaluable interface but an executable group implements both IEvaluable and IExecutable interfaces. For detailed documentation refer the link _TBD_

    _Java Code_
    ```
    Same as that provided for headless-group earlier.
    ```
    _cu equivalent_
    ```
    Same as that provided for headless-group earlier with the difference that in place of 'headless-group' the tag to use would be 'headless-executable-group' (quotes excluded).
    ```

10. <a id="condition"></a>**condition**

    A condition cu would evaluate to either true or false. The evaluation would be done by matching a value against a regular expression. If no expression is specified then the condition would evaluate to true. A condition cu can be used inside an 'on' or 'conditional' cu.

    _Java Code_
    ```Java
    Object valueToCheck = "abc";
    boolean isConditionSatisfied = "xyz".equals(valueToCheck);  //this obviously would evaluate to false
    return isConditionSatisfied;
    ```
    _cu equivalent_
    ```
    <!-- the below condition would evaluate to false -->
    <condition expression="xyz">
      <valueof id="valueToCheck" default="abc"/>
    </condition>
    ```

11. <a id="conditional"></a>**conditional**

    A conditional cu would evaluate to either true or false and can also return a value if the condition is satisfied. Also it can logically collate the boolean outcome of one or more condition(s) (using and, or, not) to arrive at a final true or false value. If no collating expression is specified then the conditional would evaluate to true. A conditional cu can be used inside an 'on', 'select', 'extends' or 'break' cu.

    _Java Code_
    ```Java
    Object valueToCheck = "abc";
    boolean condition1 = valueToCheck.indexOf("a") != -1;
    boolean condition2 = valueToCheck.indexOf("b") != -1;
    boolean condition2 = valueToCheck.indexOf("c") != -1;
    boolean isConditionalSatisfied = condition1 && condition2 && condition3;  //this would evaluate to true
    return isConditionalSatisfied? "Oh Yeah!!!": null;
    ```
    _cu equivalent_
    ```
    <!-- the below conditional would evaluate to true and return Oh Yeah!!! -->
    <conditional expression="c1 and c2 and c3" value="Oh Yeah!!!">
      <condition id="c1" expression="[a]">
        <valueof evalIfNull="true">'abc'</valueof>
      </condition>
      <condition id="c2" expression="[b]">
        <valueof evalIfNull="true">'abc'</valueof>
      </condition>
      <condition id="c3" expression="[c]">
        <valueof evalIfNull="true">'abc'</valueof>
      </condition>
    </condition>
    ```

12. <a id="loop"></a>**loop**

    A loop cu can be used to implement unbounded loops (endless loops breaking on specific condition(s)), bounded loops (fixed number of times) and iterables (arrays, collections, maps).

    _Java Code (Unbounded loop)_
    ```Java
    int index = 0;
    StringBuilder strBuilder = new StringBuilder();
    while (true) {
        if (index == 100) {
            break;
        }
        if (index != 0) {
            strBuilder.append(",");
        }
        strBuilder.append("'");
        strBuilder.append(index);
        strBuilder.append("'");
        strBuilder.append(":");
        strBuilder.append("'");
        strBuilder.append("item-" + index);
        strBuilder.append("'");
        index++;
    }
    return strBuilder.toString();
    ```
    _cu equivalent_
    ```
    <loop name="endless-loop">
      <using>
        <valueof id="start" default="0"/>
      </using>
      <break>
        <condition expression="true">
          <valueof>index = 100</valueof>
        </condition>
      </break>
      <set attribute="$index">
        <valueof>concat('item-', index)</valueof>
      </set>
    </loop>
    ```
    
    _Java Code (Bounded loop)_
    ```Java
    StringBuilder strBuilder = new StringBuilder();
    for (int i = 0; i < 100; i++) {  //loop 100 times
        if (i != 0) {
            strBuilder.append(",");
        }
        strBuilder.append("'");
        strBuilder.append(index);
        strBuilder.append("'");
        strBuilder.append(":");
        strBuilder.append("'");
        strBuilder.append("item-" + index);
        strBuilder.append("'");
    }
    return strBuilder.toString();
    ```
    _cu equivalent_
    ```
    <loop name="fixed-times-loop">
      <using>
        <valueof id="start" default="0"/>
        <valueof id="times" default="100"/>
      </using>
      <set attribute="$index">
        <valueof>concat('item-', index)</valueof>
      </set>
    </loop>
    ```
    
    _Java Code (Iterable loop for list)_
    ```Java
    StringBuilder strBuilder = new StringBuilder();
    java.util.List<String> listItems = new java.util.ArrayList<>();
    listItems.add("l1");
    listItems.add("l2");
    listItems.add("l3");
    int index = 0;
    for (String listItem : listItems) {
        if (index != 0) {
            strBuilder.append(",");
        }
        strBuilder.append("'");
        strBuilder.append(index);
        strBuilder.append("'");
        strBuilder.append(":");
        strBuilder.append("'");
        strBuilder.append(listItem);
        strBuilder.append("'");
        index++;
    }
    return strBuilder.toString();
    ```
    _cu equivalent_
    ```
    <headless-group name="wrapper">
      <!- initializer block to initialize list items for the purpose of this demo -->
      <init>
        <set attribute="list-items-as-json" in="TMP-MAP" createMapIfMissing="true">  <!-- tmp allocation -->
          <valueof default="{'data':['l1', 'l2', 'l3']}"/>
        </set>

        <!-- let's make the list items available inside CONTEXT-MAP against the key 'list-items' -->
        <set attribute="list-items" in="CONTEXT-MAP">
          <valueof key="data">
            <json container="TMP-MAP" name="list-items-as-json"/>
          </valueof>
        </set>
      </init>
      
      <!-- this is the core loop definition -->
      <loop name="list-items-loop">
        <using>
          <valueof id="iterable" key="list-items">
            <map name="CONTEXT-MAP"/>
          </valueof>
        </using>
        <set attribute="$index">
          <!-- items values are accessible inside the loop using 'item-value' variable. As the loop iterates
               it would hold values l1, l2, l3 as initialized in the list earlier. -->
          <valueof key="item-value">
            <internal-map/>
          </valueof>
        </set>
      </loop>
      
      <!-- this is the finally block where we want to carry out some cleanups -->
      <finally>
        <!-- clear the objects that were initialized inside init block -->
        <unset attribute="list-items-as-json">
          <map name="TMP_MAP"/>
        </unset>
      </finally>
    </headless-group>
    ```
    
    _Java Code (Iterable loop for map)_
    ```Java
    StringBuilder strBuilder = new StringBuilder();
    java.util.Map<String, Object> map = new java.util.HashMap<>();
    map.put("k1", "v1");
    map.put("k2", "v2");
    map.put("k3", "v3");
    int index = 0;
    for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
        if (index != 0) {
            strBuilder.append(",");
        }
        String key = entry.getKey();
        Object value = entry.getValue();
        strBuilder.append("'");
        strBuilder.append(key);
        strBuilder.append("'");
        strBuilder.append(":");
        strBuilder.append("'");
        strBuilder.append(value);
        strBuilder.append("'");
        index++;
    }
    return strBuilder.toString();
    ```
    _cu equivalent_
    ```
    <headless-group name="wrapper">
      <!- initializer block to initialize map for the purpose of this demo -->
      <init>
        <load-properties id="map-input">
          <using>
            <valueof id="stream" default='k1=v1;k2=v2;k3=v3'/>
            <valueof id="stream-delimiter" default=";"/>
          </using>
          <init>
            <set attribute="map-obj" in="CONTEXT-MAP" createMapIfMissing="true">
              <valueof key="$_$execution-result-map">
                <internal-map/>
              </valueof>
            </set>
          </init>
        </load-properties>
      </init>
      
      <!-- this is the core loop definition -->
      <loop name="map-items-iterator">
        <using>
          <valueof id="iterable" key="map-obj">
            <map name="CONTEXT-MAP"/>
          </valueof>
        </using>
        <!-- For the current iteration, map key is accessbile using 'item-key' and its value is accessible using 'item-value'.
             As the loop iterates it would hold pairs like (k1, v1), (k2, v2), (k3, v3). -->
        <set attribute="$item-key">
          <valueof key="item-value">
            <internal-map/>
          </valueof>
        </set>
      </loop>
      
      <!-- this is the finally block where we want to carry out some cleanups -->
      <finally>
        <!-- clear the objects that were initialized inside init block -->
        <unset attribute="map-obj">
          <map name="CONTEXT-MAP"/>
        </unset>
      </finally>
    </headless-group>
    ```
    
13. <a id="log"></a>**log**

    A log cu can be used to log messages to console, file or socket.

    _Java Code_
    ```Java
    //Using a logging framework like java.util.logging, log4j etc. to log the messages. Skipping the code.
    ```
    _cu equivalent_
    ```
    <log id="logging-test" target="console">  <!-- supported target types are console, file and socket -->
      <valueof default="test-message"/>
    </log>
    ```
    
14. <a id="assert"></a>**assert**

    An assert cu can be used to assert values inside cunit tests. Cunit testing framework is a revolutionary new way of writing pure data driven tests.
    
    _Java Code_
    ```Java
    //Using a framework like junit for doing the assertions. Skipping the code.
    ```
    _cu equivalent_
    ```
    <assert id="string-equals-ignore-case-abc" expression="condition1 or condition2">
      <condition id="condition1" expression="abc">
        <valueof key="string-to-check">  <!-- assume the strign value to assert is available as the key 'string-to-check' inside the map named TEST-DATA-MAP -->
          <map name="TEST-DATA-MAP"/>
        </valueof>
      </condition>
      <condition id="condition2" expression="ABC">
        <valueof key="string-to-check">  <!-- assume the strign value to assert is available as the key 'string-to-check' inside the map named TEST-DATA-MAP -->
          <map name="TEST-DATA-MAP"/>
        </valueof>
      </condition>
    </assert>
    ```
    
15. <a id="load-properties"></a>**load-properties**

    Since the Compilation Units Framework extensively use properties map, the 'load-properties' cu is a convenience cu that loads and initializes properties map for consumption by other compilation units (group, condition, conditional, set etc.). It can load properties from a file and also from an in-memory stream.

    _Java Code_
    ```Java
    java.util.Properties props = new java.util.Properties();
    props.setProperty("p1", "v1");
    props.setProperty("p2", "v2");
    props.setProperty("p3", "v3");
    
    //use the loaded properties
    ```
    _cu equivalent_
    ```
    <load-properties id="props-from-stream">
      <using>
        <valueof id="stream" default='p1=v1;p2=v2;p3=v3'/>
        <valueof id="stream-delimiter" default=";"/>
      </using>
      <!-- define the cus here that will consume the properties -->
    </load-properties>
    ```
    
    _Java Code (load properties from file)_
    ```Java
    java.util.Properties props = new java.util.Properties();
    props.load(new java.io.FileInputStream("./propsFilePath"));
    
    //use the loaded properties
    ```
    _cu equivalent_
    ```
    <load-properties id="props-from-file">
      <using>
        <valueof id="src" default="./propsFilePath"/>
      </using>
      <!-- define the cus here that will consume the properties -->
    </load-properties>
    ```
    
16. <a id="using"></a>**using**

    A 'using' cu initializes variables for use within the scope of its parent cus. The initialized variables are disposed off when the control comes out of the scope of the parent cu. The parent cus that can contain a 'using' cu are 'select', 'executable-group', 'headless-executable-group', 'loop' and 'log'. Further, any custom tags defined in future are also free to use 'using' in their implementation. As for the cus that can be added as child of the 'using' cu - any tag which is of type _org.cuframework.core.CompilationUnits.IEvaluable_ can be added as its child. Some examples of tags which are of type IEvaluable and can be added as child of 'using' cu are 'valueof', 'get', 'select', 'group', 'headless-group', 'executable-group', 'headless-executable-group' and 'conditional'. 'valueof' is the most frequently used tag inside using.
    
    _examples_
    ```
    <loop id="demo-loop">
      <using>
        <!-- we are going to define 4 variables below which would be accessible within the 
             scope of the parent cu i.e. 'demo-loop' -->
        <valueof id="start" default="0"/>
        <valueof id="end" default="10"/>
        <valueof id="custom-var1" default="value1"/>
        <valueof id="custom-var2" default="value2"/>
      </using>
      <!-- define the loop body cus -->
    </loop>
    ```
    
17. <a id="init"></a>**init**

    An 'init' cu can be used to perform initialization tasks for its parent cus i.e. group, headless-group, executable-group and headless-executable-group before the body gets executed. It can contain tags (cus) which are of type _org.cuframework.core.CompilationUnits.IExecutable_. Some examples of tags which are of type IExecutable and can be added as child of 'init' cu are 'executable-group', 'headless-executable-group', 'loop', 'log', 'assert' and 'load-properties'.
    
    _examples_
    ```
    <group id="init-demo-group">
      <init>
        <log target="console">
          <valueof id="enter-message" default="Entered init block."/>
        </log>
        <loop>
          <using>
            <!-- we are going to define 4 variables below which would be accessible within the 
                 scope of the parent cu i.e. 'demo-loop' -->
            <valueof id="start" default="0"/>
            <valueof id="end" default="10"/>
            <valueof id="custom-var1" default="value1"/>
            <valueof id="custom-var2" default="value2"/>
          </using>
          <!-- define the loop body -->
        </loop>
        <log target="console">
          <valueof id="exit-message" default="Exiting init block."/>
        </log>
      </init>
      <!-- group body goes here -->
    </group>
    ```
    
18. <a id="finally"></a>**finally**

    A 'finally' cu can be used to perform finalization tasks for its parent cus i.e. group, headless-group, executable-group and headless-executable-group after the body has executed. It can contain tags (cus) which are of type _org.cuframework.core.CompilationUnits.IExecutable_. Some examples of tags which are of type IExecutable and can be added as child of 'finally' cu are 'executable-group', 'headless-executable-group', 'loop', 'log', 'assert' and 'load-properties'.
    
    _examples_
    ```
    <group id="finally-demo-group">
      <!-- group body goes here -->
      <finally>
        <log target="console">
          <valueof id="enter-message" default="Body execution is complete. Entered finally block."/>
        </log>
        <assert id="this-is-always-true" expression="true()"/>
        <log target="console">
          <valueof id="exit-message" default="Exiting finally block."/>
        </log>
      </finally>
    </group>
    ```
    
19. <a id="extends"></a>**extends**

    The 'extends' cu is the hallmark of inheritance support inside the Compulation Units Framework. Any extensible CU can use it to inherit functionality from a base cu of the same type. The framework supports static inheritance as well as conditional inheritance. Also inheritance from multiple sources is supported. Merge as well as Replace strategies are supported. Examples of extensible cus available made available by the framework are group cus (i.e. group, headless-group, extensible-group, headless-extensible-group). New extensible units can also be created by implementing _org.cuframework.core.CompilationUnits.IExtensible_ interface or by extending the _org.cuframework.core.CompilationUnits.ExtensibleCompilationUnit_ class.
    
    _cu inheritance example_
    ```
    //--- assume the following group definition is saved inside an xml file called baseUnits.xml ---
    <group id="base-demo-group">
      <group id="base-subgroup">
        <set attribute="base-attr">
          <valueof default="base-attr-value"/>
        </set>
      </group>
      <set attribute="base-attr1">
        <valueof default="base-attr1-value"/>
      </set>
      <set attribute="to-be-overridden">
        <valueof default="to-be-overridden-base"/>
      </set>
    </group>
    
    //--- assume the following group definition is saved in a file other than baseUnits.xml ---
    <group id="super-demo-group">
      <extends default="./baseUnits.xml#base-demo-group"/>  <!-- this would inherit the group and set cus from base file -->
      <group id="new-group-in-super">
        <set attribute="super-attr">
          <valueof default="super-attr-value"/>
        </set>
      </group>
      <set attribute="to-be-overridden">
        <valueof default="to-be-overridden-super"/>
      </set>
    </group>
    
    //--- the super-demo-group defined above would finally look like as follows ---
    <group id="super-demo-group">
      <group id="base-subgroup">
        <set attribute="base-attr">
          <valueof default="base-attr-value"/>
        </set>
      </group>
      <set attribute="base-attr1">
        <valueof default="base-attr1-value"/>
      </set>
      <group id="new-group-in-super">
        <set attribute="super-attr">
          <valueof default="super-attr-value"/>
        </set>
      </group>
      <set attribute="to-be-overridden">  <!-- Note: The super group cu has overridden its base group counterpart -->
        <valueof default="to-be-overridden-super"/>
      </set>
    </group>
    ```
    
20. <a id="break"></a>**break**

    The 'break' cu is used to break out of the loop iteration based on specific condition(s). It can have 'condition' and 'conditional' cus as it's children. It can be added as a child only of the 'loop' cu. Added elsewhere would simply be ignored.
    
    _Java Code_
    ```Java
    for (int i = 0; i < 100; i++) {
        if (i == 10) {
            break;
        }
    }
    ```
    _cu equivalent_
    ```
    <loop id="break-demo">
      <using>
        <valueof id="start" default="0"/>  <!-- loop starts with index value 0 -->
        <valueof id="times" default="100"/>  <!-- loop iterates for 100 times -->
      </using>
      <break>  <!-- break condition -->
        <condition expression="true">
          <valueof>index = 10</value>  <!-- check if index value equals 10 -->
        </condition>
      </break>
    </loop>
    ```
    
21. <a id="on"></a>**on**

    The 'on' cu is used to control execution of a cu basis satisfaction of specific condition(s). If 'on' evaluates to true then the processing of the parent cu would take place, else skipped. 'on' can be added as a child of 'valueof', 'get', 'set', 'unset', 'group', 'headless-group', executable-group', 'headless-executale-group', 'map', 'internal-map', 'json', 'log' and 'load-properties' cus and control their execution. 'on' can contain 'condition' and 'conditional' cus as it's children.
    
    _Java Code_
    ```Java
    //assume dataType is of type String and is assigned a value from among "int", "boolean" or "char"
    Object result = null;
    if ("boolean".equals(dataType)) {
        result = "Found boolean dataType";
    }
    return result != null? "'message':'" +result+ "'": "";
    ```
    _cu equivalent_
    ```
    <headless-group id="result">
      <set attribute="message">
        <on>  <!-- only when this on condition is satisfied the attribute 'message' would be set inside the group -->
          <condition expression="boolean">
            <valueof>dataType</valueof>
          </condition>
        </on>
        <valueof default="Found boolean dataType"/>
      </set>
    </headless-group>
    
    //execution of the above headless-group would return the json {'message': 'Found boolean dataType'} if the variable dataType holds the string 'boolean'. Else execution would just result in an empty string.
    ```
    
22. <a id="map"></a>**map | internal-map**

    The 'map' cu is used to perform key-value lookups inside a specified map. Also it supports removal of existing key-value pairs from inside the specified map. It can be contained inside the 'valueof' and 'get' cus as their child. It can have 'on' cu as its own child. The 'internal-map' cu is also a type of map which holds the internal execution state of the cu being processed (including its scoped variables).
    
    _Java Code_
    ```Java
    //demo initialization code
    java.util.Map<String, Object> map = new java.util.HashMap<>();
    map.put("k1", "v1");
    
    //get the value of k1 from the map
    Object val = map.get("k1");
    
    //remove k1 from map
    map.remove("k1");
    ```
    _cu equivalent_
    ```
    <!-- key lookup example -->
    <valueof id="val" key="k1">
      <map name="CONTEXT-MAP"/>  <!-- here it is assumed that CONTEXT-MAP holds the runtime context required to process the cus -->
    </valueof>
    
    <!-- key removal example -->
    <unset attribute="k1">
      <map name="CONTEXT-MAP"/>
    </unset>
    
    <!-- internal-map example -->
    <valueof key="some-runtime-property">
      <internal-map/>  <!-- Note: internal-map tag is sufficient to access the internal map -->
    </value>
    ```
    
23. <a id="json"></a>**json**

    The 'json' cu is used to return attribute values from inside a json structure. If no attribute/key is specified it would convert the json into a Map representation and returns the entire map. Nested attributes can be accessed by using a field delimiter like dot(.) when specifying the attribute key. For example 'a.b.c' would return 'c-value' from inside the following json structure "{a: {b: {c: 'c-value'}}}". 'json' cu can be contained inside the 'valueof' and 'get' cus as their child. It can have 'on' cu as its own child.
    
    _example_
    ```
    <!-- json lookup example -->
    <valueof key="json-attribute">
      <json container="CONTEXT-MAP" name="json-string-obj"/>  <!-- here it is assumed that CONTEXT-MAP holds the runtime context required to process the cus. Also it is assumed that json-string-obj contains a json string e.g. {'json-attribute':'json-attribute-value'} -->
    </valueof>
    
    <!-- the above valueof cu would return the string value 'json-attribute-value' -->
    ```

### Compilation Unit(s) in detail _(WIP)_

| Compilation Unit Tag | Tag Type(s) | Key Attributes | Child CUs | Key Parent Container CUs |
| -------------------- | ----------- | -------------- | --------- | ------------------------ |
| _**valueof**_ or _**get**_ | _evaluable_ | <li> **id**<br><li> **key** - key to be looked up inside the map(s).<br><li> **default** - default value to be returned in case the evaluated value is null.<br><li> **extractionExpression** - a regex that is run on the string representation of the final value object to perform actions like group extraction or splitting of the cu value object into an array of tokens.<br><li> **matcherGroup** - an int value that is used to extract value corresponding to the matching group as defined in the _extractionExpression_. Negative int values would not result in any group extraction. Non integer values would raise a runtime error.<br><li> **evalIfNull** - true or false.<br><li> Transformation expression corresponding to xpath expression text()[0]. | <li> **map** (‘n’ number of entries allowed)<br><li> **internal-map** (‘n’ number of entries allowed)<br><li> **json** (‘n’ number of entries allowed)<br><li> **on** (only 1 entry allowed. Any excess entries would be ignored without raising an error) | <li> **set**<br><li> **condition**<br><li> **conditional**<br><li> **using**<br><li> **log** |
| _**set**_ | _evaluable_ | <li> **id**<br><li> **attribute** - name of the attribute to set inside a group or inside an output map.<br><li> **in** - Name of the output map in which to set the attribute.<br><li> **breakOnFirstValueSet** - true or false. It tells the CU to terminate execution as soon as a non null is returned by any of child evaluable tags.<br><li> **override** - true or false. By default the value set by one child evaluable tag can be overridden by subsequent tags when processed serially. However if override is set to false then a RuntimeException would be raised if an attempt is made by any subsequent evaluable tags to override the value of an attribute which has already been set previously by another tag.<br><li> Transformation expression corresponding to xpath expression text()[0]. | <li> **valueof**<br><li> **get**<br><li> **any tag of type _evaluable_** (‘n’ number of entries allowed)<br><li> **on** (only 1 entry allowed. Any excess entries would be ignored without raising an error) | **group** |
| _**select**_ | _evaluable_ | <li> **id**<br><li> **evalIfNull** - true or false.<br><li> Transformation expression corresponding to xpath expression text()[0]. | <li> **using** (only 1 entry allowed. Any excess entries would be ignored without raising an error)<br><li> **any tag of type _evaluable_** (only 1 entry allowed. Any excess entries would be ignored without raising an error)<br><li> **conditional** (‘n’ number of entries allowed) | <li> **set**<br><li> **condition**<br><li> **conditional**<br><li> **using**<br><li> **log** |
| _**group**_ and _**headless-group**_ | _evaluable_ | <li> **id**<br><li> **name** - name of group.<br><li> **type** - list or map<br><li> **selfSerializationPolicy** - none, key, value or keyValue<br><li> **childSerializationPolicy** - none, key, value or keyValue | <li> **set** (‘n’ number of entries allowed)<br><li> **group** (‘n’ number of entries allowed)<br><li> **init** (only 1 entry allowed. Any excess entries would be ignored without raising an error)<br><li> **finally** (only 1 entry allowed. Any excess entries would be ignored without raising an error)<br><li> **on** (only 1 entry allowed. Any excess entries would be ignored without raising an error) | <li>**group**<br><li> **headless-group**<br><li> **executable-group**<br><li> **headless-executable-group** |
| _**executable-group**_ and _**headless-executable-group**_ | _evaluable_, _executable_ | _\<same as that of group\>_ | <li> _\<all child cus of group\>_<br><li> **using** (only 1 entry allowed. Any excess entries would be ignored without raising an error) | <li>**group**<br><li> **headless-group**<br><li> **executable-group**<br><li> **headless-executable-group** |
