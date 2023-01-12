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

import java.util.HashMap;
import java.util.Map;

import org.cuframework.core.CompilationRuntimeContext;
import org.cuframework.core.CompilationUnits.ICompilationUnit;

/**
 * Runtime context to be used for expression's evaluation.
 * @author Sidharth Yadav
 *
 */
public class ExpressionRuntimeContext {
    public static final String ADDITIONAL_CONTEXT_THIS_VALUE = "this-value";  //can be used to pass the cu value computed thus far
                                                                              //i.e. before the expression evaluation happens.
    public static final String ADDITIONAL_CONTEXT_NODE = "node";
    public static final String ADDITIONAL_CONTEXT_XPATH = "xpath";

    private transient ICompilationUnit cu = null;
    private transient CompilationRuntimeContext compilationRuntimeContext = null;
    private transient Map<String, Object> additionalContext = null;

    public static ExpressionRuntimeContext newInstance(ICompilationUnit cu,
                                                       CompilationRuntimeContext compilationRuntimeContext) {
        if (cu == null || compilationRuntimeContext == null) {
            throw new IllegalArgumentException("The expression runtime context must be provided a reference " +
                                                             "to the parent cu and the compilation runtime context");
        }
        ExpressionRuntimeContext erc = new ExpressionRuntimeContext();
        erc.cu = cu;
        erc.compilationRuntimeContext = compilationRuntimeContext;
        return erc;
    }

    public ExpressionRuntimeContext setAdditionalContext(String key, Object value) {
        if (key == null) {
            return this;
        }
        if (additionalContext == null) {
            additionalContext = new HashMap<>();
        }
        additionalContext.put(key, value);
        return this;  //returning this to support builder patter for ease of use at the caller end.
    }

    public Object getAdditionalContext(String key) {
        return key == null || additionalContext == null? null: additionalContext.get(key);
    }

    public CompilationRuntimeContext getCompilationRuntimeContext() {
        return compilationRuntimeContext;
    }

    protected ICompilationUnit getCompilationUnit() {
        return cu;
    }
}
