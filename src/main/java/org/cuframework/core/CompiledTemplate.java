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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.xpath.XPathExpressionException;

/**
 * Representation of a compiled template.
 * @author Sidharth Yadav
 *
 */
public class CompiledTemplate {
    private String templateId = null;
    private List<CompilationUnits.ICompilationUnit> compilationUnits =
                                              new ArrayList<CompilationUnits.ICompilationUnit>();
    private boolean extensionsProcessedBefore = false;  //for optimization

    public CompiledTemplate(String templateId) {
        if (templateId == null || "".equals(templateId.trim())) {
            throw new IllegalArgumentException("Template id cannot be empty or null");
        }
        this.templateId = templateId;
    }

    public String getId() {
        return templateId;
    }

    public boolean extensionsProcessed() {
        return extensionsProcessedBefore;
    }

    CompiledTemplate doExtends(CompilationRuntimeContext compilationRuntimeContext,
                   CompiledTemplatesRegistry mctr) throws XPathExpressionException {
        if (extensionsProcessedBefore) {
            return this;
        }
        Map<CompilationUnits.ICompilationUnit, CompilationUnits.ICompilationUnit> extensibleUnits =
                                                                                                    new HashMap<>();
        boolean areAllExtensibleUnitsMarkedAsProcessed = true;
        for (CompilationUnits.ICompilationUnit cu : compilationUnits) {
            if (cu instanceof CompilationUnits.IExtensible) {
                //if (CompilationUnits.IExtensible.class.cast(cu).hasExtends()) {
                    CompilationUnits.ICompilationUnit extendedCU = CompilationUnits.IExtensible.class.cast(cu).
                                                                              extend(compilationRuntimeContext, mctr);
                    extensibleUnits.put(cu, extendedCU);
                    if (extendedCU instanceof CompilationUnits.IExtensible) {
                        areAllExtensibleUnitsMarkedAsProcessed &=
                              //CompilationUnits.IExtensible.class.cast(extendedCU).areExtensionsMarkedAsProcessed();
                              CompilationUnits.IExtensible.class.cast(cu).areExtensionsMarkedAsProcessed();
                                           //in the above statement we should check the extensions processed status on the orig cu
                                           //and not the extended cu because the extended cu is a clone of base and its 'extensions
                                           //processed' status is always marked as true. It's instance is valid only for a specific
                                           //compilationRuntimeContext and doesn't represent the finality. The orig cu however is updated
                                           //with the final extensions processing status and only if there were no <conditional extends>,
                                           //<dynamic group idOrElse in the base units> its extension processing status would be set to true.
                        /* if (CompilationUnits.IExtensible.class.cast(extendedCU).areExtensionsMarkedAsProcessed()) {
                            CompilationUnits.IExtensible.class.cast(cu).markExtensionsAsProcessed(
                                                CompilationUnits.IExtensible.class.cast(extendedCU).areExtensionsMarkedAsProcessed());
                        } */
                    }
                //}
            }
        }
        CompiledTemplate clonedMct = this;
        if (!areAllExtensibleUnitsMarkedAsProcessed) {
            clonedMct = new CompiledTemplate("cloned:" + templateId);  //not all extensions are permanently processed (owing to presence
                                                                       //of some conditional extends etc) and hence we would return a
                                                                       //different instance of compiled template and preserve the original
                                                                       //instance as is for future use.
            clonedMct.addCompilationUnits(compilationUnits);
            clonedMct.extensionsProcessedBefore = true;  //this is a specific instance of mct which is valid only for
                                                         //the current compilationRuntimeContext and thus should be
                                                         //freezed in terms of inheritance processing to prevent reuse
                                                         //by any other compilationRuntimeContext.
        }
        for (Entry<CompilationUnits.ICompilationUnit, CompilationUnits.ICompilationUnit> entry :
                                                                            extensibleUnits.entrySet()) {
            int index = clonedMct.compilationUnits.indexOf(entry.getKey());
            clonedMct.compilationUnits.add(index, entry.getValue());
            clonedMct.compilationUnits.remove(index + 1);
            if (!areAllExtensibleUnitsMarkedAsProcessed &&
                entry.getValue() instanceof CompilationUnits.IExtensible &&
                //CompilationUnits.IExtensible.class.cast(entry.getValue()).areExtensionsMarkedAsProcessed()) {
                CompilationUnits.IExtensible.class.cast(entry.getKey()).areExtensionsMarkedAsProcessed()) {
                                       //in the above statement we should check the extensions processed status on the orig cu (the key
                                       //in the map) and not the extended cu because the extended cu is a clone of base and its 'extensions
                                       //processed' status is always marked as true. It's instance is valid only for a specific
                                       //compilationRuntimeContext and doesn't represent the finality. The orig cu however is updated
                                       //with the final extensions processing status and only if there were no <conditional extends>,
                                       //<dynamic group idOrElse in the base units> its extension processing status would be set to true.
                //the core extensible unit itself is marked as processed so let's update the master copy
                //with the extended instance
                //int index = compilationUnits.indexOf(entry.getKey());
                compilationUnits.add(index, entry.getValue());
                compilationUnits.remove(index + 1);
            }
        }
        extensionsProcessedBefore = areAllExtensibleUnitsMarkedAsProcessed;
        return clonedMct;
    }

    public void addCompilationUnit(CompilationUnits.ICompilationUnit cu) {
        compilationUnits.add(cu);
    }

    public void addCompilationUnits(List<CompilationUnits.ICompilationUnit> cus) {
        compilationUnits.addAll(cus);
    }

    public void addCompilationUnits(CompilationUnits.ICompilationUnit[] cus) {
        for (CompilationUnits.ICompilationUnit cu : cus) {
            compilationUnits.add(cu);
        }
    }

    public CompilationUnits.ICompilationUnit[] getCompilationUnits() {
        return compilationUnits.toArray(new CompilationUnits.ICompilationUnit[0]);
    }

    //util method to search for a compilation unit inside the template
    public CompilationUnits.ICompilationUnit findCompilationUnit(String[] pathTrail) {
        if (pathTrail == null || pathTrail.length == 0) {
            return null;
        }
        for (CompilationUnits.ICompilationUnit cu : compilationUnits) {
            boolean matchesIdOrElse = cu.matchesIdOrElse(pathTrail[0]);
            if (matchesIdOrElse && pathTrail.length == 1) {
                return cu;
            }
            if (matchesIdOrElse) {
                String[] pathTrailLess1 = new String[pathTrail.length - 1];
                System.arraycopy(pathTrail, 1, pathTrailLess1, 0, pathTrailLess1.length);
                CompilationUnits.ICompilationUnit childCU =  findChildCU(cu, pathTrailLess1);
                if (childCU != null) {
                    return childCU;
                }
            }
        }
        return null;
    }

    private static CompilationUnits.ICompilationUnit findChildCU(CompilationUnits.ICompilationUnit parentCU,
                                                                   String[] childPathTrail) {
        if (childPathTrail == null || childPathTrail.length == 0) {
            return null;
        }
        CompilationUnits.ICompilationUnit childCU = parentCU.getChild(childPathTrail[0]);
        if (childCU != null && childPathTrail.length > 1) {
            String[] childPathTrailLess1 = new String[childPathTrail.length - 1];
            System.arraycopy(childPathTrail, 1, childPathTrailLess1, 0, childPathTrailLess1.length);
            return findChildCU(childCU, childPathTrailLess1);
        }
        return childCU;
    }

    public void dispose() {
        compilationUnits.clear();
    }
}
