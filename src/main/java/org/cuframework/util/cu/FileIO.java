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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.Map;

import org.cuframework.core.CompilationUnits.IExecutable;
import org.cuframework.core.CompilationUnits.HeadlessExecutableGroup;

/**
 * @author Sidharth Yadav
 */
public class FileIO extends HeadlessExecutableGroup implements IExecutable {
    public static final String TAG_NAME = "fileio";

    //cu input parameters
    private static final String PARAM_OPERATION = "operation";
    private static final String PARAM_PATH = "path";  //path of file/directory to read/write. It must either be of type File or String.
    private static final String PARAM_TARGET_PATH = "target";  //target of copy and move operations.
    private static final String PARAM_TYPE = "type";  //file type e.g. text. If the operation specified is 'ls' then this will represent the file filter.
    private static final String PARAM_IO_MODE = "io-mode";  //read, write
    private static final String PARAM_WRITE_MODE = "write-mode";  //e.g. append
    private static final String PARAM_STREAM = "stream";  //holds an object of io stream
    private static final String PARAM_BYTES = "bytes";  //number of bytes to skip, read etc
    private static final String PARAM_PAYLOAD = "payload";  //payload to write

    //the result or outcome of the execution should be set inside requestContext as a map and the name of the map key should be returned as the value of the function.
    @Override
    protected String doExecute(Map<String, Object> requestContext) {
        String operation = (String) requestContext.get(PARAM_OPERATION);  //possible values - open, read, write, skip, close
        if (operation == null) {
            return null;
        }
        String resultMapKeyName = "-file-io-result-";
        String idOrElse = getIdOrElse();  //using the non computed version of idOrElse
        String resultKeyName = idOrElse == null? "result": idOrElse;
        Map<String, Object> resultMap = new HashMap<>();
        try {
            switch(operation.toUpperCase().trim()) {
                case "LS": resultMap.put(resultKeyName, listItems(requestContext)); break;
                case "CP":
                case "COPY": resultMap.put(resultKeyName, copyItem(requestContext)); break;
                case "MV":
                case "MOVE": resultMap.put(resultKeyName, moveItem(requestContext)); break;
                case "REN":
                case "RENAME": resultMap.put(resultKeyName, renameItem(requestContext)); break;
                case "RM":
                case "DEL":
                case "DELETE": resultMap.put(resultKeyName, deleteItem(requestContext)); break;
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

    //Returns null if the path to be inspected is null or represents a non existent location.
    //Returns an array of File objects, matching the type filter (if one provided), if the path represented a directory.
    //Returns the File object, matching the type filter (if one provided), if the path represented a file.
    //Else returns null.
    private Object listItems(Map<String, Object> requestContext) {
        Object path = requestContext.get(PARAM_PATH);
        if (path == null) {
            return null;
        }

        //path must represent a Path, File or a String object
        File f = path instanceof File? (File) path:
                 path instanceof Path? ((Path) path).toFile(): new File((String) path);

        if (!f.exists()) {
            return null;
        }
        String fileFilter = (String) requestContext.get(PARAM_TYPE);
        if (f.isDirectory()) {
            return f.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return fileFilter == null? true: f.getName().matches(fileFilter);
                }
            });
        } else {
            return fileFilter == null || f.getName().matches(fileFilter)? f: null;
        }
    }

    //returns null if the source or target was null. Else it returns the Path to the copied file.
    private Object copyItem(Map<String, Object> requestContext) throws IOException {
        Path[] paths = getSourceAndTargetAsPaths(requestContext);
        Path source = paths[0];
        Path target = paths[1];

        if (source == null || target == null) {
            return null;
        }

        boolean isTargetDirectory = target.toFile().isDirectory();
        return Files.copy(source, isTargetDirectory? target.resolve(source.getFileName()): target);  //TODO add recursion support
    }

    //returns null if the source or target was null. Else it returns the Path to the moved file.
    private Object moveItem(Map<String, Object> requestContext) throws IOException {
        Path[] paths = getSourceAndTargetAsPaths(requestContext);
        Path source = paths[0];
        Path target = paths[1];

        if (source == null || target == null) {
            return null;
        }

        boolean isTargetDirectory = target.toFile().isDirectory();
        return Files.move(source, isTargetDirectory? target.resolve(source.getFileName()): target);  //TODO add recursion support
    }

    //returns null if rename was not attempted else returns the Path to the renamed file.
    private Object renameItem(Map<String, Object> requestContext) throws IOException {
        Path source = getAsPath(PARAM_PATH, requestContext);
        String newName = (String) requestContext.get(PARAM_TARGET_PATH);  //object must be of type String

        if (source == null || newName == null) {
            return null;
        }

        return Files.move(source, source.resolveSibling(newName));
    }

    //returns null if source was null. Else it returns true/false as per the outcome of the delete operation.
    private Object deleteItem(Map<String, Object> requestContext) throws IOException {
        Path source = getAsPath(PARAM_PATH, requestContext);

        if (source == null) {
            return null;
        }

        return Files.deleteIfExists(source);  //TODO add recursion support
    }

    //0th index contains the path to source and 1st index contains path to target
    private Path[] getSourceAndTargetAsPaths(Map<String, Object> requestContext) {
        Path[] paths = new Path[2];
        paths[0] = getAsPath(PARAM_PATH, requestContext);
        paths[1] = getAsPath(PARAM_TARGET_PATH, requestContext);
        return paths;
    }

    private Path getAsPath(String attributeName, Map<String, Object> requestContext) {
        Object path = requestContext.get(attributeName);
        Path asPath = null;

        if (path != null) {
            //path must represent a Path, File or a String object
            asPath = path instanceof Path? (Path) path:
                     path instanceof File? ((File) path).toPath(): FileSystems.getDefault().getPath((String) path);
        }

        return asPath;
    }

    private Object openFile(Map<String, Object> requestContext) throws IOException {
        Object path = requestContext.get(PARAM_PATH);
        String type = (String) requestContext.get(PARAM_TYPE);
        boolean textFileType = "text".equalsIgnoreCase(type);
        String ioMode = (String) requestContext.get(PARAM_IO_MODE);
        if (ioMode == null) {
            return null;
        }

        //path must represent a Path, File or a String object
        File f = path instanceof File? (File) path:
                 path instanceof Path? ((Path) path).toFile(): new File((String) path);

        switch(ioMode.toUpperCase().trim()) {
            case "READ": return textFileType? new BufferedReader(new InputStreamReader(new FileInputStream(f))): new FileInputStream(f);
            case "WRITE":
                         {
                             String writeMode = (String) requestContext.get(PARAM_WRITE_MODE);
                             return new FileOutputStream(f, writeMode == null? true: "append".equalsIgnoreCase(writeMode));
                         }
            default: throw new UnsupportedOperationException("Unsupported io mode: " + ioMode);
        }
    }

    private Object readFile(Map<String, Object> requestContext) throws IOException {
        Object stream = requestContext.get(PARAM_STREAM);
        if (stream == null) {
            return null;
        }
        String bytes = (String) requestContext.get(PARAM_BYTES);
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
        OutputStream stream = (OutputStream) requestContext.get(PARAM_STREAM);
        if (stream == null) {
            return;
        }
        Object payload = requestContext.get(PARAM_PAYLOAD);
        if (payload instanceof byte[]) {
            stream.write((byte[]) payload);
        } else if (payload != null) {
            stream.write(payload.toString().getBytes());
        }
    }

    private Object skipFile(Map<String, Object> requestContext) throws IOException {
        Object stream = requestContext.get(PARAM_STREAM);
        if (stream == null) {
            return null;
        }
        long skipCount = Long.parseLong(
                                requestContext.get(PARAM_BYTES).toString());  //let it throw NFE or NPE if the expected attribute is null or non integer.
        if (stream instanceof FileInputStream) {
            return ((FileInputStream) stream).skip(skipCount);
        } else if (stream instanceof BufferedReader) {
            return ((BufferedReader) stream).skip(skipCount);
        }
        return null;
    }

    private void closeFile(Map<String, Object> requestContext) throws IOException {
        Object stream = requestContext.get(PARAM_STREAM);
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
