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

package org.cuframework.samplecu;

import org.cuframework.core.CompilationUnits.IExecutable;
import org.cuframework.core.CompilationUnits.ExecutableGroup;

/**
 * Sample cu for a calculator implementation.
 *
 * @author Sidharth Yadav
 */
public class Calculator extends ExecutableGroup implements IExecutable {
    public static final String TAG_NAME = "calculator";

    @Override
    public String getTagName() {
        return Calculator.TAG_NAME;
    }

    //the result or outcome of the execution should be set inside requestContext as a map and
    //the name of the map key should be returned as the value of the function.
    @Override
    protected String doExecute(java.util.Map<String, Object> requestContext) {
        String resultMapName = "-calc-result-map-";
        String operation = (String) requestContext.get("operation");
        String operand1 = requestContext.get("first").toString();
        String operand2 = requestContext.get("second").toString();
        java.util.Map<String, Integer> resultMap = new java.util.HashMap<>();
        //requestContext.put(operation, calculate(operation, Integer.parseInt(operand1), Integer.parseInt(operand2)));
        resultMap.put(operation, calculate(operation, Integer.parseInt(operand1), Integer.parseInt(operand2)));
        requestContext.put(resultMapName, resultMap);
        return resultMapName;
    }

    private int calculate(String operation, int operand1, int operand2) {
        int result = -1;
        switch(operation) {
            case "add":
            case "sum": result = operand1 + operand2; break;
            case "minus":
            case "subtract": result = operand1 - operand2; break;
            case "multiply": result = operand1 * operand2; break;
            case "divide": result = operand1 / operand2; break;
            case "mod": result = operand1 % operand2; break;
        }
        return result;
    }

    //overridden method to support cloning
    @Override
    protected Calculator newInstance() {
        return new Calculator();
    }
}
