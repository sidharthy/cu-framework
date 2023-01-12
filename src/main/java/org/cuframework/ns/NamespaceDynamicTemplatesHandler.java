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

package org.cuframework.ns;

import java.io.ByteArrayInputStream;

import javax.xml.xpath.XPathExpressionException;

import org.cuframework.TemplateCompilationException;
import org.cuframework.config.ConfigManager;
import org.cuframework.core.CompiledTemplate;
import org.cuframework.core.CompiledTemplatesRegistry;

/**
 * Registrar for handling the namespace's dynamic templates (cu's body, custom functions) by registering them
 *  as in-memory templates, thereby enabling their extension/access during the cu template's lifecycle.
 * @author Sidharth Yadav
 *
 */
public class NamespaceDynamicTemplatesHandler {

    //If there are custom ns functions defined inside the namespace definition file then this method would
    //register the ns funcitons as an in-memory template and return its id for appropriate consumption. If no
    //custom ns functions are defined then this method would return null.
    public static CompiledTemplate getCustomFunctions(String namespaceURI)
                                                               throws XPathExpressionException, TemplateCompilationException {
        String customFunctionsTemplateString = ConfigManager.getInstance().getCustomFunctions(namespaceURI, null);
        return registerAsInMemoryTemplate(customFunctionsTemplateString);
    }

    //If there is a custom cu body defined inside the namespace definition file for the passed node then this method would
    //register the cu body as an in-memory template and return its id so that it can be used inside an <extends>. If no
    //custom cu body is defined for the node then this method would return null.
    public static String getCuBodyTemplateId(String nodeName, String namespaceURI)
                                                                      throws XPathExpressionException, TemplateCompilationException {
        String cuBodyTemplateString = ConfigManager.getInstance().getCuBody(nodeName, namespaceURI, null);
        CompiledTemplate mct = registerAsInMemoryTemplate(cuBodyTemplateString);
        return mct == null? null: mct.getId() + CompiledTemplatesRegistry.TEMPLATE_PATH_SPLITTER + mct.getId();
    }

    private static CompiledTemplate registerAsInMemoryTemplate(String inMemoryTemplateString)
                                                                      throws XPathExpressionException, TemplateCompilationException {
        if (inMemoryTemplateString == null || "".equals(inMemoryTemplateString.trim())) {
            return null;
        }
        String templateUID = "inm-" + inMemoryTemplateString.hashCode();
        String _inMemoryTemplateString = getInMemoryTemplateString(templateUID, inMemoryTemplateString);
        CompiledTemplate mct = CompiledTemplatesRegistry.getInstance().
                                                getCompiledTemplate(templateUID,
                                                                    new ByteArrayInputStream(_inMemoryTemplateString.getBytes()),
                                                                    "/root/*");
        return mct;
    }

    private static String getInMemoryTemplateString(String templateUID, String inMemoryTemplateString) {
        StringBuilder template = new StringBuilder();
        template.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        template.append("<root name=\"" + templateUID + "\">");
        template.append("  <headless-group name=\"" + templateUID + "\">");  //NOTE: The in-memory template string is added inside 2 level
                                                                             //hierarchy of 'root > headless-group'. Any changes to this
                                                                             //could break things at other places. If it is to be absolutely
                                                                             //changed then also make changes inside FunctionResolver where
                                                                             //the custom functions are traced and registered.
        template.append(inMemoryTemplateString);
        template.append("  </headless-group>");
        template.append("</root>");
        return template.toString();
    }
}
