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

package org.cuframework.func;

import java.util.HashMap;
import java.util.Map;

import org.cuframework.config.ConfigManager;
import org.cuframework.core.CompilationUnits;
import org.cuframework.core.CompiledTemplate;
import org.cuframework.core.CompiledTemplatesRegistry;
import org.cuframework.ns.NamespaceDynamicTemplatesHandler;

/**
 * Default function resolver provided by the platform.
 * @author Sidharth Yadav
 *
 */
public final class FunctionResolver {
    private static final String CU_ATTRIBUTE_FN_TYPE = "fntype";  //This attribute can be used to define the type of the function defined
                                                                  //in the namespace definition. Supported values are:
                                                                  //  - 'class': It means that the cu evaluation should be assumed to
                                                                  //    represent the class name of the actual function and treated accordingly.
                                                                  //  - <any other value, including null>: It means the function should be treated as a
                                                                  //    generic cu accessor and just return the value of cu evaluation.
                                                                  //This attribute is deliberately not defined inside any of the ICompilationUnit impls
                                                                  //because it is applicable only to the cu's defined as functions and used primarily by
                                                                  //this class during function resolution.

    private static final String CORE_FUNCTIONS = "core-functions";
    private static final String MORE_FUNCTIONS = "more-functions";

    private final Map<String, Map<String, IFunction>> functionsRepository =
                                                             new HashMap<String, Map<String, IFunction>>();

    private String namespaceURI = null;
    private boolean customFunctionsLoadedOnce = false;

    public FunctionResolver(String namespaceURI,
                            Map<String, IFunction> coreFunctions,
                            Map<String, IFunction> moreFunctions) {
        this.namespaceURI = namespaceURI;
        functionsRepository.put(CORE_FUNCTIONS, coreFunctions == null? new HashMap<String, IFunction>(): coreFunctions);
        functionsRepository.put(MORE_FUNCTIONS, moreFunctions == null? new HashMap<String, IFunction>(): moreFunctions);
    }

    public IFunction resolve(String funcId) {
        if (funcId == null) {
            return null;
        }
        IFunction func = functionsRepository.get(CORE_FUNCTIONS).get(funcId);
        if (func == null) {
            //function not found in the core group. Let's now check the more group.
            if (!customFunctionsLoadedOnce) {
                customFunctionsLoadedOnce = true;  //We are going to make atleast one attempt to load custom functions.
                                                   //We will not make another attempt if this one fails.
                attemptToLoadCustomFunctions();
            }
            func = functionsRepository.get(MORE_FUNCTIONS).get(funcId);
        }
        return func;
    }

    private void attemptToLoadCustomFunctions() {
        try {
            CompiledTemplate mct = NamespaceDynamicTemplatesHandler.getCustomFunctions(namespaceURI);
            CompilationUnits.ICompilationUnit rootCU = mct == null? null: mct.findCompilationUnit(new String[]{mct.getId()});
            if (rootCU != null) {
                CompilationUnits.ICompilationUnit[] cus = rootCU.getChildren(CompilationUnits.ICompilationUnit.class);
                for (CompilationUnits.ICompilationUnit cu: cus) {
                    if (cu.getIdOrElse() == null || "".equals(cu.getIdOrElse().trim())) {
                        continue;  //cus with null|empty ids won't be registered as functions
                    }
                    if (cu instanceof CompilationUnits.IExecutable || cu instanceof CompilationUnits.IEvaluable) {
                        String fnType = cu.getAttribute(CU_ATTRIBUTE_FN_TYPE);
                        IFunction func = null;
                        if ("class".equalsIgnoreCase(fnType)) {
                            func = new ClassFunction(mct.getId(),
                                                     mct.getId() + CompiledTemplatesRegistry.TEMPLATE_PATH_SPLITTER + cu.getIdOrElse());
                            ((ClassFunction) func).setInheritedClasspath(ConfigManager.
                                                                               getInstance().
                                                                                    getClasspath(null, namespaceURI, null));
                        } else {
                            func = new GenericCUAccessor(mct.getId(),
                                                         mct.getId() + CompiledTemplatesRegistry.TEMPLATE_PATH_SPLITTER + cu.getIdOrElse());
                        }
                        functionsRepository.get(MORE_FUNCTIONS).put(cu.getIdOrElse(), func);
                    }
                }
            }
        } catch(Exception e) {
            //TODO log
        }
    }
}
