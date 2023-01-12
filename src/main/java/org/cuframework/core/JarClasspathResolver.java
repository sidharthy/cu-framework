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

import java.lang.reflect.Constructor;

import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;

/**
 * Jar classpath resolver.
 * @author Sidharth Yadav
 *
 */
class JarClasspathResolver {
    public static Set<URL> resolveClasspathFromJar(URL classpath_jar_url) throws Exception {
        URLConnection _urlconn=classpath_jar_url.openConnection();
        if (_urlconn instanceof JarURLConnection) {
            JarFile jf=((JarURLConnection) _urlconn).getJarFile();
            return loadClasspath(classpath_jar_url, jf);
        }
        throw new IllegalArgumentException("The url doesn't point to a jar file: " + classpath_jar_url + ". Classpath resolution abended.");
    }

    private static Set<URL> loadClasspath(URL jar_url, JarFile jar_file) throws Exception {
        Manifest manifest=jar_file.getManifest();
        Attributes main_attributes=manifest.getMainAttributes();
        String[] classpathEntries=getJarClasspathEntries(jar_url, jar_file, (String) main_attributes.getValue(Attributes.Name.CLASS_PATH));

        Set<URL> _all_cp_urls = toURLs(classpathEntries);
        _all_cp_urls.add(jar_url);

        return _all_cp_urls;
    }

    private static Set<URL> toURLs(String[] entries) throws MalformedURLException {
        Set<URL> urls = new HashSet<>();
        for (int i=0; i<entries.length; i++) {
            urls.add(new URL(entries[i]));
        }
        return urls;
    }

    private static String[] getJarClasspathEntries(URL jar_url, JarFile jar_file, String manifest_classpath_value) {
        if (jar_url==null || jar_file==null || manifest_classpath_value==null)
            return new String[0];

        StringTokenizer str=new StringTokenizer(manifest_classpath_value);
        String[] _classpaths=new String[str.countTokens()];
        int i=0;
        while (str.hasMoreTokens()) {
            String token=str.nextToken();
            if (".".equals(token)) {
                String[] _shrunk_cp_array=new String[_classpaths.length-1];
                System.arraycopy(_classpaths, 0, _shrunk_cp_array, 0, _shrunk_cp_array.length);
                _classpaths=_shrunk_cp_array;
                i=i-1;
                continue;
            }
            _classpaths[i++]=jar_url.toExternalForm()+token;
        }
        return _classpaths;
    }
}
