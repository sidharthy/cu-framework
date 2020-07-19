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

import java.io.File;

import java.net.URI;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
  Function resolver.
  @author Sidharth Yadav
 */
public final class FunctionResolver {
    private static final String CORE_FUNCTIONS = "core-functions";
    private static final String MORE_FUNCTIONS = "more-functions";

    private static final Map<String, Map<String, IFunction>> FUNCTIONS_REPOSITORY =
                                                             new HashMap<String, Map<String, IFunction>>();
    static {
        Map<String, IFunction> coreFunctions = new HashMap<String, IFunction>();
        coreFunctions.put("array-copy-of-range",
                          (context, compilationRuntimeContext) -> {
                                           Object[] array = context.length > 0? (Object[]) context[0]: null;
                                           if (array == null) {
                                               return null;
                                           }
                                           int fromIndex = 0;
                                           int toIndex = array.length;
                                           try {
                                               if (context.length > 1 && context[1] != null)
                                                   fromIndex = Integer.parseInt(context[1].toString());
                                           } catch (NumberFormatException nfe) {
                                               //ignore
                                           }
                                           try {
                                               if (context.length > 2 && context[2] != null)
                                                   toIndex = Integer.parseInt(context[2].toString());
                                           } catch (NumberFormatException nfe) {
                                               //ignore
                                           }
                                           return Arrays.copyOfRange(array, fromIndex, toIndex);
                                       });
        coreFunctions.put("file-new",
                          (context, compilationRuntimeContext) -> {
                                           Object path = context.length > 0? context[0]: null;
                                           return path == null? null:
                                                    (path instanceof String? new File((String) path):
                                                     path instanceof URI? new File((URI) path): null);
                                       });
        coreFunctions.put("file-name",
                          (context, compilationRuntimeContext) -> {
                                           File f = context.length > 0? (File) context[0]: null;
                                           return f != null? f.getName(): null;
                                       });
        coreFunctions.put("file-parent",
                          (context, compilationRuntimeContext) -> {
                                           File f = context.length > 0? (File) context[0]: null;
                                           return f != null? f.getParent(): null;
                                       });
        coreFunctions.put("file-path",
                          (context, compilationRuntimeContext) -> {
                                           File f = context.length > 0? (File) context[0]: null;
                                           return f != null? f.getPath(): null;
                                       });
        coreFunctions.put("file-size",
                          (context, compilationRuntimeContext) -> {
                                           File f = context.length > 0? (File) context[0]: null;
                                           return f != null? f.length(): null;
                                       });
        coreFunctions.put("file-exists",
                          (context, compilationRuntimeContext) -> {
                                           File f = context.length > 0? (File) context[0]: null;
                                           return f != null? f.exists(): null;
                                       });
        coreFunctions.put("file-is-directory",
                          (context, compilationRuntimeContext) -> {
                                           File f = context.length > 0? (File) context[0]: null;
                                           return f != null? f.isDirectory(): null;
                                       });
        coreFunctions.put("random",
                          (context, compilationRuntimeContext) -> Math.random());
        coreFunctions.put("random-int",
                          (context, compilationRuntimeContext) -> {
                                           Object origin = context.length > 1? context[0]: null;
                                           Object bound = context.length > 1? context[1]: context.length > 0? context[0]: null;
                                           boolean originIsValidNum = origin != null, boundIsValidNum = bound != null;
                                           try {
                                               if (origin != null)
                                                   Integer.parseInt(origin.toString());
                                           } catch (NumberFormatException nfe) {
                                               originIsValidNum = false;
                                           }
                                           try {
                                               if (bound != null)
                                                   Integer.parseInt(bound.toString());
                                           } catch (NumberFormatException nfe) {
                                               boundIsValidNum = false;
                                           }
                                           ThreadLocalRandom tlr = ThreadLocalRandom.current();
                                           return originIsValidNum && boundIsValidNum? tlr.nextInt(Integer.parseInt(origin.toString()),
                                                                                                   Integer.parseInt(bound.toString())):
                                                  boundIsValidNum? tlr.nextInt(Integer.parseInt(bound.toString())): tlr.nextInt();
                                       });
        coreFunctions.put("random-long",
                          (context, compilationRuntimeContext) -> {
                                           Object origin = context.length > 1? context[0]: null;
                                           Object bound = context.length > 1? context[1]: context.length > 0? context[0]: null;
                                           boolean originIsValidNum = origin != null, boundIsValidNum = bound != null;
                                           try {
                                               if (origin != null)
                                                   Long.parseLong(origin.toString());
                                           } catch (NumberFormatException nfe) {
                                               originIsValidNum = false;
                                           }
                                           try {
                                               if (bound != null)
                                                   Long.parseLong(bound.toString());
                                           } catch (NumberFormatException nfe) {
                                               boundIsValidNum = false;
                                           }
                                           ThreadLocalRandom tlr = ThreadLocalRandom.current();
                                           return originIsValidNum && boundIsValidNum? tlr.nextLong(Long.parseLong(origin.toString()),
                                                                                                    Long.parseLong(bound.toString())):
                                                  boundIsValidNum? tlr.nextLong(Long.parseLong(bound.toString())): tlr.nextLong();
                                       });
        coreFunctions.put("random-double",
                          (context, compilationRuntimeContext) -> {
                                           Object origin = context.length > 1? context[0]: null;
                                           Object bound = context.length > 1? context[1]: context.length > 0? context[0]: null;
                                           boolean originIsValidNum = origin != null, boundIsValidNum = bound != null;
                                           try {   
                                               if (origin != null)
                                                   Double.parseDouble(origin.toString());
                                           } catch (NumberFormatException nfe) {
                                               originIsValidNum = false;
                                           }   
                                           try {   
                                               if (bound != null)
                                                   Double.parseDouble(bound.toString());
                                           } catch (NumberFormatException nfe) {
                                               boundIsValidNum = false;
                                           }
                                           ThreadLocalRandom tlr = ThreadLocalRandom.current();
                                           return originIsValidNum && boundIsValidNum? tlr.nextDouble(Double.parseDouble(origin.toString()),
                                                                                                      Double.parseDouble(bound.toString())):
                                                  boundIsValidNum? tlr.nextDouble(Double.parseDouble(bound.toString())): tlr.nextDouble();
                                       });
        coreFunctions.put("uuid",
                          (context, compilationRuntimeContext) -> UUID.randomUUID());
        coreFunctions.put("str-replace", 
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 3) {
                                               return str;
                                           }
                                           String target = (String) context[1];
                                           String replacement = (String) context[2];
                                           return str.replace(target, replacement);
                                       });
        coreFunctions.put("str-replace-regex",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 3) {
                                               return str;
                                           }
                                           String regex = (String) context[1];
                                           String replacement = (String) context[2];
                                           return str.replaceAll(regex, replacement);
                                       });
        coreFunctions.put("str-starts-with",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return null;  //should we return false?
                                           }
                                           String prefix = (String) context[1];
                                           return str.startsWith(prefix);
                                       });
        coreFunctions.put("str-ends-with",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return null;  //should we return false?
                                           }
                                           String suffix = (String) context[1];
                                           return str.endsWith(suffix);
                                       });
        coreFunctions.put("str-contains",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return null;  //should we return false?
                                           }
                                           String substring = (String) context[1];
                                           return str.contains(substring);
                                       });
        coreFunctions.put("str-indexof",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return null;  //should we return -1?
                                           }
                                           String substring = (String) context[1];
                                           return str.indexOf(substring);
                                       });
        coreFunctions.put("str-matches",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return null;  //should we return false?
                                           }
                                           String regex = (String) context[1];
                                           return str.matches(regex);
                                       });
        coreFunctions.put("str-trim",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           return str != null? str.trim(): null;
                                       });
        coreFunctions.put("str-uppercase",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           return str != null? str.toUpperCase(): null;
                                       });
        coreFunctions.put("str-lowercase",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           return str != null? str.toLowerCase(): null;
                                       });
        coreFunctions.put("str-join",
                          (context, compilationRuntimeContext) -> {
                                           String delimiter = context.length > 0? (String) context[0]: null;
                                           if (context.length <= 1) {
                                               return null;  //should we return an empty String?
                                           }
                                           delimiter = delimiter == null? "": delimiter;
                                           StringBuilder strBuilder = new StringBuilder();
                                           for (int i = 1; i < context.length; i++) {
                                               strBuilder.append(context[i]);
                                               if (i != context.length - 1) {
                                                 strBuilder.append(delimiter);
                                               }
                                           }
                                           return strBuilder.toString();
                                       });
        coreFunctions.put("str-format",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return str;
                                           }
                                           String format = (String) context[1];
                                           Object[] args = Arrays.copyOfRange(context, 2, context.length);
                                           return str.format(format, args);
                                       });
        coreFunctions.put("str-to-bytes",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           return str != null? str.getBytes(): null;
                                       });
        coreFunctions.put("str-from-bytes",
                          (context, compilationRuntimeContext) -> {
                                           byte[] bytes = context.length > 0? (byte[]) context[0]: null;
                                           return bytes != null? new String(bytes): null;
                                       });
        coreFunctions.put("str-to-date",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           String format = context.length > 1? (String) context[1]: null;
                                           return str != null?
                                                    (format == null?
                                                      (new SimpleDateFormat()).parse(str):
                                                      (new SimpleDateFormat(format)).parse(str)
                                                    ):
                                                    null;
                                       });
        coreFunctions.put("str-to-instant",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           return str != null? Instant.parse(str): null;
                                       });
        coreFunctions.put("str-to-localdatetime",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           String format = context.length > 1? (String) context[1]: null;
                                           return str != null?
                                                    (format == null?
                                                      LocalDateTime.parse(str):
                                                      LocalDateTime.parse(str, DateTimeFormatter.ofPattern(format))
                                                    ):
                                                    null;
                                       });
        coreFunctions.put("date",
                          (context, compilationRuntimeContext) -> {
                                           long epoch = -1;
                                           try {
                                               if (context.length > 0 && context[0] != null) {
                                                   epoch = Long.parseLong(context[0].toString());
                                               }
                                           } catch (NumberFormatException nfe) {
                                               //ignore
                                           }
                                           return epoch >= 0? new Date(epoch): new Date();
                                       });
        coreFunctions.put("date-to-epoch",
                          (context, compilationRuntimeContext) -> {
                                           Date date = context.length > 0? (Date) context[0]: null;
                                           return date != null? date.getTime(): null;
                                       });
        coreFunctions.put("epoch",
                          (context, compilationRuntimeContext) -> System.currentTimeMillis());
        coreFunctions.put("equals",
                          (context, compilationRuntimeContext) -> {
                                           Object obj1 = context.length > 0? context[0]: null;
                                           if (obj1 == null) {
                                               return null;
                                           }
                                           Object obj2 = context.length > 1? context[1]: null;
                                           return obj1.equals(obj2);
                                       });
        coreFunctions.put("hashcode",
                          (context, compilationRuntimeContext) -> {
                                           Object obj = context.length > 0? context[0]: null;
                                           return obj != null? obj.hashCode(): null;
                                       });

        Map<String, IFunction> moreFunctions = new HashMap<String, IFunction>();

        FUNCTIONS_REPOSITORY.put(CORE_FUNCTIONS, coreFunctions);
        FUNCTIONS_REPOSITORY.put(MORE_FUNCTIONS, moreFunctions);
    }

    private FunctionResolver() {
    }

    public static IFunction resolve(Object funcIdentifier) {
        if (funcIdentifier == null) {
            return null;
        }
        String funcId = funcIdentifier.toString();
        IFunction func = FUNCTIONS_REPOSITORY.get(CORE_FUNCTIONS).get(funcId);
        if (funcId == null) {
            //function not found in the core group. Let's now check the more group.
            func = FUNCTIONS_REPOSITORY.get(MORE_FUNCTIONS).get(funcId);
        }
        return func;
    }
}
