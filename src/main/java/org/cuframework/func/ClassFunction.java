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

package org.cuframework.func;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.HashMap;
import java.util.Map;

import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.util.UtilityFunctions;

/**
 * A custom java function defined in a class.
 * @author Sidharth Yadav
 *
 */
public class ClassFunction extends GenericCUAccessor implements IFunction {

    private static final String CLASSPATH_GROUP_ID = "-classpath-";  //can be used to define classpath at the fn level
    private static final String CU_CLASS_KEY = "-class-";

    private boolean cuProcessedOnce = false;
    private Object cuProcessingResult = null;
    private IFunction func = null;

    private Map<String, Object> inheritedClasspath = null;

    public ClassFunction(String templateId, String cuAccessorPath) {
        super(templateId, cuAccessorPath);
    }

    public void setInheritedClasspath(Map<String, Object> inheritedClasspath) {
        this.inheritedClasspath = inheritedClasspath;
    }

    @Override
    protected Object _cu(Object[] context,
                         CompilationRuntimeContext compilationRuntimeContext) throws Exception {
        if (!cuProcessedOnce) {
            cuProcessingResult = super._cu(context, compilationRuntimeContext);
            cuProcessedOnce = true;
        }
        return cuProcessingResult;
    }

    @Override
    protected Object _postCU(Object cuResult,
                             Object[] context,
                             CompilationRuntimeContext compilationRuntimeContext) throws Exception {
        if (cuResult == null) {
            return null;
        }

        if (func == null) {
            String fnClassName = null;
            Map<String, Object> fnCp = inheritedClasspath;
            if (cuResult instanceof Map) {
                Map<String, Object> cuResultMap = (Map<String, Object>) cuResult;
                
                Object fnClasspathMap = cuResultMap.get(CLASSPATH_GROUP_ID);
                if (fnClasspathMap instanceof Map) {
                    if (fnCp == null) {
                        fnCp = (Map<String, Object>) fnClasspathMap;
                    } else {
                        fnCp.putAll((Map<String, Object>) fnClasspathMap);
                    }
                }

                Object _fnClassName = cuResultMap.get(CU_CLASS_KEY);
                if (_fnClassName != null) {
                    fnClassName = _fnClassName.toString();
                }
            } else if (cuResult != null){
                fnClassName = cuResult.toString();  //the value is assumed to represent the fn class name
            }

            if (fnClassName != null) {
                func = Class.forName(fnClassName,
                                     true,
                                     URLClassLoader.newInstance(fnCp == null? new URL[0]: UtilityFunctions.getClasspathURLs(fnCp),
                                                                getClass().getClassLoader())
                                     ).asSubclass(IFunction.class).newInstance();
            }
        }
        return func != null? func.invoke(context, compilationRuntimeContext): null;
    }
}
