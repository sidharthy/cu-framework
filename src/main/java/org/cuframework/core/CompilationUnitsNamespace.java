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

package org.cuframework.core;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.cuframework.core.CompilationUnits.ICompilationUnit;
import org.cuframework.config.ConfigManager;
import org.cuframework.func.FunctionResolver;
import org.cuframework.func.IFunction;

/**
 * Compilation Units Namespace.
 * @author Sidharth Yadav
 *
 */
public final class CompilationUnitsNamespace {

    private static final String CORE_CUs_CONTAINER = "core-units";
    private static final String MORE_CUs_CONTAINER = "more-units";
    private final Map<String, Map<String, Class<? extends ICompilationUnit>>> tagToUnitMappings =
                                                             new HashMap<String, Map<String, Class<? extends ICompilationUnit>>>();

    private String uri = null;  //namespace uri
    private String name = null;  //readable ns name
    private boolean locked = false;
    private boolean isRootNamespace = false;

    private Set<String> parentNamespaces = null;  //using set to avoid duplicates

    private FunctionResolver functionResolver = null;

    public CompilationUnitsNamespace(String uri) {
        this(uri, null, null);
    }

    public CompilationUnitsNamespace(String uri,
                                     Map<String, Class<? extends ICompilationUnit>> coreCUs) {
        this(uri, coreCUs, null);
    }

    public CompilationUnitsNamespace(String uri,
                                     Map<String, Class<? extends ICompilationUnit>> coreCUs,
                                     Map<String, Class<? extends ICompilationUnit>> moreCUs) {
        this(uri, coreCUs, moreCUs, null);
    }

    public CompilationUnitsNamespace(String uri,
                                     Map<String, Class<? extends ICompilationUnit>> coreCUs,
                                     Map<String, Class<? extends ICompilationUnit>> moreCUs,
                                     Map<String, IFunction> coreFunctions) {
        if (uri == null)
            throw new IllegalArgumentException("Namespace URI cannot be null");

        this.uri = uri;
        isRootNamespace = CompilationUnits.ROOT_CU_NAMESPACE_URI.equals(uri);

        tagToUnitMappings.put(CORE_CUs_CONTAINER, coreCUs == null? new HashMap<String, Class<? extends ICompilationUnit>>(): coreCUs);
        tagToUnitMappings.put(MORE_CUs_CONTAINER, moreCUs == null? new HashMap<String, Class<? extends ICompilationUnit>>(): moreCUs);
        functionResolver = new FunctionResolver(uri, coreFunctions, null);
    }

    public ICompilationUnit getCompilationUnitForTag(String tagName) {
        Class<? extends ICompilationUnit> cu = getCompilationClassForTag(tagName);
        if (cu != null) {
            try {
                return cu.newInstance();
            } catch (InstantiationException e) {
                // can be ignored as this wouldn't happen
            } catch (IllegalAccessException e) {
                // can be ignored as this wouldn't happen
            }
        }
        return null;
    }

    public Class<? extends ICompilationUnit> getCompilationClassForTag(String tagName) {
        return getCompilationClassForTag(tagName, true);
    }

    protected Class<? extends ICompilationUnit> getCompilationClassForTag(String tagName, boolean attemptRootLookup) {
        if (tagName == null) {
            return null;
        }
        Class<? extends ICompilationUnit> cu = tagToUnitMappings.get(CORE_CUs_CONTAINER).get(tagName);
        if (cu == null) {
            //tag name didn't correspond to the core units. Let's now check the more units.
            cu = tagToUnitMappings.get(MORE_CUs_CONTAINER).get(tagName);
        }
        if (cu == null && parentNamespaces != null) {
            for (String parentNamespace: parentNamespaces) {
                cu = CompilationUnits.getCompilationClassForTag(parentNamespace, tagName, attemptRootLookup);
                attemptRootLookup = false;  //resetting this flag to avoid unnecessary (duplicate) root lookup later in this method
                                            //if none of the parent namespaces could return the compilation class for the requested
                                            //tagName. The first iteration of this loop anyway was allowed to lookup all way through
                                            //to the root namespace. This is purely for optimized processing.
                if (cu != null) {
                    break;  //we found a compilation class. Let's break out of the loop.
                }
            }
        }
        if (cu == null && !isRootNamespace && attemptRootLookup) {  //if cu is still null and we are not already in the root
                                                                    //namespace then let's search for the cu in the root namespace.
            cu = CompilationUnits.getCompilationClassForTag(CompilationUnits.ROOT_CU_NAMESPACE_URI, tagName);
        }
        return cu;
    }

    //This method can set only 'more' units and not the 'core' units.
    public boolean setCompilationClassForTag(String tagName, String tagClassName) throws ClassNotFoundException, ClassCastException {
        return setCompilationClassForTag(tagName, 
                                         Class.forName(tagClassName, false, URLClassLoader.newInstance(getClasspath(tagName)))
                                                                                                 .asSubclass(ICompilationUnit.class));
    }

    private URL[] getClasspath(String tagName) {
        Map<String, Object> classpath = ConfigManager.getInstance().getClasspath(tagName, uri, null);
        if (classpath == null || classpath.size() == 0) {
            return new URL[0];
        }
        Set<URL> urls = new HashSet<>();
        for (Entry<String, Object> _classpathES: classpath.entrySet()) {
            Object value = _classpathES.getValue();
            if (value == null) {
                continue;
            }
            String _classpath = value.toString().trim();
            String jarUrl = null;
            if (_classpath.startsWith("http:") || _classpath.startsWith("https:")) {
                jarUrl = "jar:" + _classpath + "!/";
            }
            else {
                //we assume the location points to a file on disk
                jarUrl = "jar:file:/" + _classpath + "!/";
            }
            try {
                urls.addAll(JarClasspathResolver.resolveClasspathFromJar(new URL(jarUrl)));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        return urls.toArray(new URL[0]);
    }

    //This method can set only 'more' units and not the 'core' units.
    public boolean setCompilationClassForTag(String tagName, Class<? extends ICompilationUnit> tagClass) {
        if (locked) {
            throw new UnsupportedOperationException("Namespace [" + uri + "] is locked. Set operation is not allowed.");
        }

        boolean mapped = false;
        if (tagName != null && tagClass != null) {
            tagToUnitMappings.get(MORE_CUs_CONTAINER).put(tagName, tagClass);
            mapped = true;
        }
        return mapped;
    }

    //This method can unset only 'more' units and not the 'core' units.
    public Class<? extends ICompilationUnit> unsetCompilationUnitForTag(String tagName) {
        if (locked) {
            throw new UnsupportedOperationException("Namespace [" + uri + "] is locked. Unset operation is not allowed.");
        }

        Class<? extends ICompilationUnit> removedUnit = null;
        if (tagName != null) {
            removedUnit = tagToUnitMappings.get(MORE_CUs_CONTAINER).remove(tagName);
        }
        return removedUnit;
    }

    public IFunction resolveFunction(String fnName) {
        return resolveFunction(fnName, true);
    }

    protected IFunction resolveFunction(String fnName, boolean attemptRootLookup) {
        if (fnName == null) {
            return null;
        }
        IFunction fn = functionResolver.resolve(fnName);
        if (fn == null && parentNamespaces != null) {
            for (String parentNamespace: parentNamespaces) {
                fn = CompilationUnits.resolveFunction(parentNamespace, fnName, attemptRootLookup);
                attemptRootLookup = false;  //resetting this flag to avoid unnecessary (duplicate) root lookup later in this method
                                            //if none of the parent namespaces could return the function ref for the requested
                                            //fnName. The first iteration of this loop anyway was allowed to lookup all way through
                                            //to the root namespace. This is purely for optimized processing.
                if (fn != null) {
                    break;  //we found a function ref. Let's break out of the loop.
                }
            }
        }
        if (fn == null && !isRootNamespace && attemptRootLookup) {  //if fn is still null and we are not already in the root
                                                                    //namespace then let's search for the fn in the root namespace.
            fn = CompilationUnits.resolveFunction(CompilationUnits.ROOT_CU_NAMESPACE_URI, fnName);
        }
        return fn;
    }

    public String getURI() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocked(boolean flag) {
        this.locked = flag;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isRootNamespace() {
        return isRootNamespace;
    }

    public void addParentNamespace(String parentNamespaceURI) {
        if (isRootNamespace) {
            throw new UnsupportedOperationException("Root namespace [" + uri + "] cannot have any parent namespaces.");
        }

        if (parentNamespaceURI == null) {
            return;
        }
        if (parentNamespaces == null) {
            parentNamespaces = new HashSet<String>();
        }
        parentNamespaces.add(parentNamespaceURI);
    }

    public String[] getParentNamespaces() {
        return parentNamespaces.toArray(new String[0]);
    }

    public static boolean load(String namespaceURI) {
        return false;  //TODO
    }
}
