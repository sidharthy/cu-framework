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

package org.cuframework.util.cu;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.cuframework.core.CompilationUnits.IExecutable;
import org.cuframework.core.CompilationUnits.ExecutableGroup;

/**
 * @author Sidharth Yadav
 */
public class LoadProperties extends ExecutableGroup implements IExecutable {
    public static final String TAG_NAME = "load-properties";

    //the result or outcome of the execution should be set inside requestContext as a map and the name of the map key should be returned as the value of the function.
    @Override
    protected String doExecute(java.util.Map<String, Object> requestContext) {
        String propsMapKeyName = "-properties-";
        String src = (String) requestContext.get("src");
        String propsStream = (String) requestContext.get("stream");
        String propsStreamDelimiter = (String) requestContext.get("stream-delimiter");
        Properties props = new Properties();
        try {
            if (src != null) {
                props = loadPropertiesFromSrc(props, src);
            }
            if (propsStream != null) {
                 props = loadPropertiesFromStream(props,
                                                  propsStream,
                                                  propsStreamDelimiter == null? ";": propsStreamDelimiter);  //use ; as the default properties stream delimiter in case none specified.
            }
            requestContext.put(propsMapKeyName, props);
        } catch(Exception e) {
             //any custom processing needed?
             throw new RuntimeException(e);
        }
        return propsMapKeyName;
    }

    private Properties loadPropertiesFromSrc(Properties props, String src) throws IOException {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(src);
            props.load(fin);
            return props;
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch(IOException ioe) {
                    //closing of the stream failed.
                    //this exception should not disrupt the consumption of any loaded properties.
                    //can be logged and proceeded as usual.
                }
            }
        }
    }

    private Properties loadPropertiesFromStream(Properties props, String propsStream, String propsStreamDelimiter) throws IOException {
        props.load(new StringReader(propsStream.
                                      replaceAll(propsStreamDelimiter, System.getProperty("line.separator"))));  //multiple key value pairs to be separated using the delimiter.
                                                                                                                 //Also assumption is that specified delimiter would not form
                                                                                                                 // part of any of the keys or values.
        return props;
    }

    //overridden method to support cloning
    @Override
    protected LoadProperties newInstance() {
        return new LoadProperties();
    }
}
