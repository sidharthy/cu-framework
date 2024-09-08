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

import java.util.HashMap;
import java.util.Map;

import org.cuframework.MapOfMaps;
import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.core.CompilationUnits;
import org.cuframework.core.CompiledTemplatesRegistry;
import org.cuframework.el.ExpressionRuntimeContext;

/**
 * Generic CU Accessor Function.
 * @author Sidharth Yadav
 *
 */
public class GenericCUAccessor implements IFunction {

    protected static final String FUNC_CONTEXT_KEY = "-current-function-context-";
    protected String templateId = null;
    protected String[] cuAccessorPath = null;

    @Override
    public final Object invoke(Object[] context,
                               ExpressionRuntimeContext expressionRuntimeContext) throws Exception {
        return _postCU(_cu(context, expressionRuntimeContext), context, expressionRuntimeContext);
    }

    protected Object _cu(Object[] context,
                         ExpressionRuntimeContext expressionRuntimeContext) throws Exception {
        CompilationRuntimeContext compilationRuntimeContext = expressionRuntimeContext.getCompilationRuntimeContext();
        CompilationUnits.ICompilationUnit cu = CompiledTemplatesRegistry.getInstance().
                                                    processExtensions(templateId, compilationRuntimeContext).
                                                                                    //the framework would generically process extensions everytime.
                                                                                    //It is the responsibility of the template creators that they
                                                                                    //should avoid having dynamic extends in the template or else
                                                                                    //the performance would degrade.
                                                         findCompilationUnit(cuAccessorPath);
        if (cu == null) {
            return null;
        }

        context = IFunction.vals(context, expressionRuntimeContext);

        String funcContextContainerMapName = MapOfMaps.Name.CONTEXT_MAP.getKey();
        MapOfMaps mapOfMaps = compilationRuntimeContext.getExternalContext();
        boolean externalContextExisted = mapOfMaps != null;
        if (mapOfMaps == null) {
            compilationRuntimeContext.setExternalContext(new MapOfMaps());
        }
        Map<String, Object> contextMap = mapOfMaps.getMap(funcContextContainerMapName);
        if (contextMap == null) {
            contextMap = new HashMap<String, Object>();
            mapOfMaps.putMap(funcContextContainerMapName, contextMap);  //side effect of this method would be that the func context container
                                                                        //map would not be removed before returning. That however should not be
                                                                        //a matter of concern as:
                                                                        //1.) It would exist only as long as the compilationRuntimeContext exists.
                                                                        //2.) Empty context container won't cause any harm.
        }
        boolean hadFuncContext = contextMap.containsKey(FUNC_CONTEXT_KEY);
        Object savedFuncContext = contextMap.get(FUNC_CONTEXT_KEY);
        Object result = null;
        try {
            contextMap.put(FUNC_CONTEXT_KEY, context);
            if (cu instanceof CompilationUnits.Group) {
                result = ((CompilationUnits.Group) cu).build(compilationRuntimeContext, "map");  //by default let's return the map object.
                                                                                                 //The actual cu can however change this by
                                                                                                 //defining its own serializer type in template.
            } else if (cu instanceof CompilationUnits.IExecutable) {
                result = ((CompilationUnits.IExecutable) cu).execute(compilationRuntimeContext);
            } else if (cu instanceof CompilationUnits.IEvaluable) {
                result = ((CompilationUnits.IEvaluable) cu).getValue(compilationRuntimeContext);
            }
        } finally {
            if (externalContextExisted) {
                if (hadFuncContext) {
                    contextMap.put(FUNC_CONTEXT_KEY, savedFuncContext);
                } else {
                    contextMap.remove(FUNC_CONTEXT_KEY);
                }
            } else {
                compilationRuntimeContext.setExternalContext(null);  //Is this overkill? Should we just leave the empty context as is?
                                                                     //Could it cause any side effects in multi threaded env?
            }
        }
        return result;
    }

    protected Object _postCU(Object cuResult,
                             Object[] context,
                             ExpressionRuntimeContext expressionRuntimeContext) throws Exception {
        return cuResult;  //by default just return the already calculated result of cu processing
    }

    public GenericCUAccessor(String templateId, String cuAccessorPath) {
        if (templateId == null || cuAccessorPath == null ||
            "".equals(templateId.trim()) || "".equals(cuAccessorPath.trim())) {
            throw new IllegalArgumentException(getClass().getName() + ": Template id and a CU accessor path must be provided.");
        }
        this.templateId = templateId.trim();
        this.cuAccessorPath = cuAccessorPath.trim().split(CompiledTemplatesRegistry.TEMPLATE_PATH_SPLITTER);
    }
}
