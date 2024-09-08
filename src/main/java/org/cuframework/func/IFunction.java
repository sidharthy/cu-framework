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

import org.cuframework.el.EL.Expression;
import org.cuframework.el.ExpressionRuntimeContext;

/**
 * Functional interface of Compilation Units Framework's function library.
 * @author Sidharth Yadav
 *
 */
@FunctionalInterface
public interface IFunction { 
    Object invoke(Object[] context, ExpressionRuntimeContext expressionRuntimeContext) throws Exception;

    /**
     * Utility method to return the evaluated value of expression if the contextObject param is an instanceof EL.Expression.
     * This would be useful when the function implementation is such that the actual evaluation of the contextObject is
     * conditional e.g. an ifelse function might want to defer and doesn't even need to evaluate the else expression when the
     * if condition is satisfied.
     */
    static Object val(Object contextObject, ExpressionRuntimeContext expressionRuntimeContext) throws Exception {
        Object value = contextObject;
        if (contextObject instanceof Expression) {
            value = ((Expression) contextObject).getValue(expressionRuntimeContext);
        }
        return value;
    }

    /**
     * Utility method to return the evaluated value of expressions if the contextObject array contains instances of EL.Expression.
     * This would be useful when the function implementation is such that the actual evaluation of the contextObject(s) is to be
     * made conditional e.g. an ifelse function might want to defer and doesn't even need to evaluate the else expression when the
     * if condition is satisfied.
     */
    static Object[] vals(Object[] contextArray, ExpressionRuntimeContext expressionRuntimeContext) throws Exception {
        Object[] values = new Object[contextArray.length];
        int index = 0;
        for (Object obj: contextArray) {
            values[index++] = val(obj, expressionRuntimeContext);
        }
        return values;
    }
}
