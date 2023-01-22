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

import java.util.Collections;
import java.util.Map;

import org.cuframework.MapOfMaps;

/**
 * Runtime context to be used by compilation units.
 * @author Sidharth Yadav
 *
 */
public class CompilationRuntimeContext {
    private MapOfMaps externalContext = null;
    private Map<String, Object> internalContextMap = null;
    private boolean abortIfNotSatisfy = false;  //this flag can be internally set by the compilation units to modify
                                                //the behavior of the state machine while getting the value out
                                                //from a ValueOf CU (Compilation Unit). If it so happens that any of
                                                //the controlling 'on' conditions are not satisfied inside ValueOf CU
                                                //and if this flag is set then it may opt to throw a runtime exception
                                                //to indicate that no required conditions were satisfied.

    public void setExternalContext(MapOfMaps mapOfMaps) {
        externalContext = mapOfMaps;
    }

    public MapOfMaps getExternalContext() {
        return externalContext;
    }

    public Map<String, Object> getImmutableInternalContext() {
        return internalContextMap != null? Collections.unmodifiableMap(internalContextMap): null;
    }

    protected void setInternalContext(Map<String, Object> internalContextMap) {
        this.internalContextMap = internalContextMap;
    }

    protected Map<String, Object> getInternalContext() {
        return internalContextMap;
    }

    @Deprecated
    protected void setAbortIfNotSatisfy(boolean flag) {
        this.abortIfNotSatisfy = flag;
    }

    @Deprecated
    protected boolean isAbortIfNotSatisfy() {
        return abortIfNotSatisfy;
    }
}
