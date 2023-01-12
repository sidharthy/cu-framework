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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NodeList;

import org.cuframework.TemplateCompilationException;
import org.cuframework.TemplateXPathEngine;

/**
 * Registry to load and hold compiled templates.
 * @author Sidharth Yadav
 *
 */
public final class CompiledTemplatesRegistry {
    public static final String TEMPLATE_PATH_SPLITTER = "#";  //# to be used as delimiter

    private static final String RESOURCE_META_INF_DIR = "META-INF/";
    private static final String INSTALLED_META_INF_DIR = "/tmp/CU-META-INF/";

    private final Map<String, CompiledTemplate> compiledTemplatesCache =
                                                    new ConcurrentHashMap<String, CompiledTemplate>();

    private static final CompiledTemplatesRegistry CTR = new CompiledTemplatesRegistry();

    private String primaryLookupDir = INSTALLED_META_INF_DIR;
    private String secondaryLookupDir = RESOURCE_META_INF_DIR;

    private CompiledTemplatesRegistry() {
    }

    public void setPrimaryLookupDirectory(String primaryLookupDir) {
        this.primaryLookupDir = primaryLookupDir;
    }

    public String getPrimaryLookupDirectory() {
        return this.primaryLookupDir;
    }

    public void setSecondaryLookupDirectory(String secondaryLookupDir) {
        this.secondaryLookupDir = secondaryLookupDir;
    }

    public String getSecondaryLookupDirectory() {
        return this.secondaryLookupDir;
    }

    public static CompiledTemplatesRegistry getInstance() {
        return CTR;
    }

    public <T extends CompilationUnits.ICompilationUnit> T getCompilationUnit(String cuFQPath, Class<T> cuType) {
        if (cuFQPath == null || "".equals(cuFQPath.trim())) {
            return null;
        }
        String[] tokens = cuFQPath.split(TEMPLATE_PATH_SPLITTER);
        String templateFile = tokens[0];
        try {
            if (tokens.length > 1) {
                //id trail specified. Load the template and find the CU
                CompiledTemplate mct = getCompiledTemplate(templateFile);
                String[] idTrail = new String[tokens.length - 1];
                System.arraycopy(tokens, 1, idTrail, 0, idTrail.length);
                CompilationUnits.ICompilationUnit mcu = mct.findCompilationUnit(idTrail);
                if (mcu != null && cuType.isInstance(mcu)) {
                    return cuType.cast(mcu);
                }
            }
        } catch (XPathExpressionException e) {
            // TODO log
        } catch (FileNotFoundException e) {
            // TODO log
        } catch (TemplateCompilationException tce) {
            // TODO log
        }
        return null;
    }

    public <T extends CompilationUnits.ICompilationUnit> T getCompilationUnit(CompiledTemplate mct,
                                                                                String[] idTrail, Class<T> cuType) {
        if (mct == null || idTrail == null) {
            return null;
        }
        //id trail specified. Find the CU inside the template
        CompilationUnits.ICompilationUnit mcu = mct.findCompilationUnit(idTrail);
        if (mcu != null && cuType.isInstance(mcu)) {
            return cuType.cast(mcu);
        }
        return null;
    }

    public CompiledTemplate getCompiledTemplate(String templateFile)
            throws XPathExpressionException, FileNotFoundException, TemplateCompilationException {
        return getCompiledTemplate(templateFile, "/root/*");
    }

    public CompiledTemplate getCompiledTemplate(String templateFile, String selectQuery)
                                                                  throws XPathExpressionException,
                                                                         FileNotFoundException,
                                                                         TemplateCompilationException {
        return getCompiledTemplate(templateFile, selectQuery, primaryLookupDir, secondaryLookupDir);
    }

    public CompiledTemplate getCompiledTemplate(String templateFile, String selectQuery,
                                                String primaryLookupDir, String secondaryLookupDir)
                                                                 throws XPathExpressionException,
                                                                        FileNotFoundException,
                                                                        TemplateCompilationException {
        CompiledTemplate mct = compiledTemplatesCache.get(templateFile);
        if (mct == null) {
            mct = getCompiledTemplate(templateFile,
                                         getXmlObjectStream(templateFile, primaryLookupDir, secondaryLookupDir),
                                         selectQuery);
        }
        return mct;
    }

    public CompiledTemplate getCompiledTemplate(String templateUID, InputStream in, String selectQuery)
                                                              throws XPathExpressionException, TemplateCompilationException {
        CompiledTemplate mct = compiledTemplatesCache.get(templateUID);
        if (mct == null) {
            NodeList nl = TemplateXPathEngine.getNodes(selectQuery, in);
            mct = new CompiledTemplate(templateUID);
            for (int i = 0; i < nl.getLength(); i++) {
                CompilationUnits.ICompilationUnit compilationUnit =
                                            CompilationUnits.getCompilationUnitForTag(nl.item(i).getNamespaceURI(),
                                                                                      nl.item(i).getNodeName());
                if (compilationUnit != null) {
                    compilationUnit.compile(nl.item(i));
                    mct.addCompilationUnit(compilationUnit);
                }
            }
            compiledTemplatesCache.put(templateUID, mct);
        }
        return mct;
    }

    public CompiledTemplate removeCompiledTemplate(String templateUID) {
        return compiledTemplatesCache.remove(templateUID);
    }

    public CompiledTemplate processExtensions(String forTemplate,
                                        CompilationRuntimeContext compilationRuntimeContext) {
        CompiledTemplate processed = null;
        try {
            CompiledTemplate mct = getCompiledTemplate(forTemplate);
            processed = processExtensions(mct, compilationRuntimeContext);
        } catch (XPathExpressionException e) {
            // TODO log
        } catch (FileNotFoundException e) {
            // TODO log
        } catch (TemplateCompilationException tce) {
            //TODO log
        }
        return processed;
    }

    public CompiledTemplate processExtensions(CompiledTemplate mct,
                                                 CompilationRuntimeContext compilationRuntimeContext) {
        CompiledTemplate processed = mct;
        try {
            if (mct != null) {
                processed = mct.doExtends(compilationRuntimeContext, CTR);
            }
        } catch (XPathExpressionException e) {
            // TODO log
        }
        return processed;
    }

    private InputStream getXmlObjectStream(String xmlObjectName,
                                           String primaryLookupDir,
                                           String secondaryLookupDir) throws FileNotFoundException {
        InputStream inputStream;
        File file;
        String fileName = primaryLookupDir + xmlObjectName;
        file = new File(fileName);
        if (file.isFile() && file.canRead()) {
            return new FileInputStream(fileName);
        }
        fileName = secondaryLookupDir + xmlObjectName;
        inputStream = CompiledTemplatesRegistry.class.getClassLoader().getResourceAsStream(fileName);
        return inputStream;
    }
}
