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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import java.util.HashMap;
import java.util.Map;

import org.cuframework.core.CompilationUnits.IExecutable;
import org.cuframework.core.CompilationUnits.HeadlessExecutableGroup;

/**
 * @author Sidharth Yadav
 */
public class FileIO extends HeadlessExecutableGroup implements IExecutable {
    public static final String TAG_NAME = "fileio";

    //the result or outcome of the execution should be set inside requestContext as a map and the name of the map key should be returned as the value of the function.
    @Override
    protected String doExecute(Map<String, Object> requestContext) {
        String operation = (String) requestContext.get("operation");  //possible values - open, read, write, skip, close
        if (operation == null) {
            return null;
        }
        String resultMapKeyName = "-file-io-result-";
        String resultKeyName = "result";
        Map<String, Object> resultMap = new HashMap<>();
        try {
            switch(operation.toUpperCase().trim()) {
                case "OPEN": resultMap.put(resultKeyName, openFile(requestContext)); break;
                case "READ": resultMap.put(resultKeyName, readFile(requestContext)); break;
                case "WRITE": writeFile(requestContext); break;
                case "SKIP": resultMap.put(resultKeyName, skipFile(requestContext)); break;
                case "CLOSE": closeFile(requestContext); break;
                default: throw new UnsupportedOperationException("Unsupported io operation: " + operation);
            }
            requestContext.put(resultMapKeyName, resultMap);
        } catch(Exception e) {
            //any custom processing needed?
            throw new RuntimeException(e);
        }
        return resultMapKeyName;
    }

    private Object openFile(Map<String, Object> requestContext) throws IOException {
        String path = (String) requestContext.get("path");
        String type = (String) requestContext.get("type");
        boolean textFileType = "text".equalsIgnoreCase(type);
        String ioMode = (String) requestContext.get("io-mode");
        if (ioMode == null) {
            return null;
        }
        switch(ioMode.toUpperCase().trim()) {
            case "READ": return textFileType? new BufferedReader(new InputStreamReader(new FileInputStream(path))): new FileInputStream(path);
            case "WRITE":
                         {
                             String writeMode = (String) requestContext.get("write-mode");
                             return new FileOutputStream(path, writeMode == null? true: "append".equalsIgnoreCase(writeMode));
                         }
            default: throw new UnsupportedOperationException("Unsupported io mode: " + ioMode);
        }
    }

    private Object readFile(Map<String, Object> requestContext) throws IOException {
        Object stream = requestContext.get("stream");
        if (stream == null) {
            return null;
        }
        String bytes = (String) requestContext.get("bytes");
        if (stream instanceof FileInputStream) {
            if (bytes != null) {
                int _bytes = Integer.parseInt(bytes);
                byte[] buffer = new byte[_bytes];
                int bytesRead = ((FileInputStream) stream).read(buffer, 0, buffer.length);
                byte[] filledBuffer = bytesRead == -1? null: (bytesRead == buffer.length? buffer: new byte[bytesRead]);
                if (filledBuffer != null && filledBuffer != buffer) {
                    System.arraycopy(buffer, 0, filledBuffer, 0, filledBuffer.length);
                }
                return filledBuffer;
            }
            int byteRead = ((FileInputStream) stream).read();
            return byteRead == -1? null: byteRead;
        } else if (stream instanceof BufferedReader) {
            if (bytes != null) {
                int _chars = Integer.parseInt(bytes);
                char[] buffer = new char[_chars];
                int charsRead = ((BufferedReader) stream).read(buffer, 0, buffer.length);
                char[] filledBuffer = charsRead == -1? null: (charsRead == buffer.length? buffer: new char[charsRead]);
                if (filledBuffer != null && filledBuffer != buffer) {
                    System.arraycopy(buffer, 0, filledBuffer, 0, filledBuffer.length);
                }
                return filledBuffer;
            }
            return ((BufferedReader) stream).readLine();
        }
        return null;
    }

    private void writeFile(Map<String, Object> requestContext) throws IOException {
        OutputStream stream = (OutputStream) requestContext.get("stream");
        if (stream == null) {
            return;
        }
        Object payload = requestContext.get("payload");
        if (payload instanceof byte[]) {
            stream.write((byte[]) payload);
        } else if (payload != null) {
            stream.write(payload.toString().getBytes());
        }
    }

    private Object skipFile(Map<String, Object> requestContext) throws IOException {
        Object stream = requestContext.get("stream");
        if (stream == null) {
            return null;
        }
        long skipCount = Long.parseLong(
                                requestContext.get("bytes").toString());  //let it throw NFE or NPE if the expected attribute is null or non integer.
        if (stream instanceof FileInputStream) {
            return ((FileInputStream) stream).skip(skipCount);
        } else if (stream instanceof BufferedReader) {
            return ((BufferedReader) stream).skip(skipCount);
        }
        return null;
    }

    private void closeFile(Map<String, Object> requestContext) throws IOException {
        Object stream = requestContext.get("stream");
        if (stream instanceof InputStream) {
            ((InputStream) stream).close();
        }
        else if (stream instanceof Reader) {
            ((Reader) stream).close();
        }
        else if (stream instanceof OutputStream) {
            ((OutputStream) stream).close();
        }
    }

    //overridden method to support cloning
    @Override
    protected FileIO newInstance() {
        return new FileIO();
    }
}
