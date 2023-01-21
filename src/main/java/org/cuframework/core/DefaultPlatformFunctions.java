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

import java.io.File;

import java.lang.reflect.Array;

import java.net.URI;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.cuframework.func.IFunction;

/**
 * Default Platform Functions.
 * @author Sidharth Yadav
 *
 */
final class DefaultPlatformFunctions {

    private static interface ToNumber {
        Number toNum(Object obj);
    };
    private static ToNumber toNum = (obj) -> {
                                                 Number number = null;
                                                 if (obj instanceof Number) {
                                                     number = (Number) obj;
                                                 } else if (obj instanceof String) {
                                                     String str = (String) obj;
                                                     try {
                                                         number = Integer.parseInt(str);
                                                     } catch (NumberFormatException nfe) {
                                                         try {
                                                             number = Long.parseLong(str);
                                                         } catch (NumberFormatException nfe1) {
                                                             try {
                                                                 number = Float.parseFloat(str);
                                                             } catch (NumberFormatException nfe2) {
                                                                 try {
                                                                     number = Double.parseDouble(str);
                                                                 } catch (NumberFormatException nfe3) {
                                                                     throw nfe3;
                                                                 }
                                                             }
                                                         }
                                                     }
                                                 } else if (obj != null) {
                                                     //null obj value would be pardoned but non-null obj value that is not
                                                     //a number or String would result in abending of the operation.
                                                     throw new NumberFormatException("Invalid obj type found for conversion to number");
                                                 }
                                                 return number;
                                             };

    public static Map<String, IFunction> getCoreFunctions() {
        Map<String, IFunction> coreFunctions = new HashMap<String, IFunction>();
        coreFunctions.put("number",
                          (context, compilationRuntimeContext) -> {
                                           Object input = context.length == 1? context[0]: null;
                                           Number number = null;
                                           try {
                                               number = toNum.toNum(input);
                                           } catch (NumberFormatException nfe3) {
                                               //number parsing failed. null would be returned.
                                           }
                                           return number;
                                       });
        coreFunctions.put("int",
                          (context, compilationRuntimeContext) -> {
                                           Object input = context.length == 1? context[0]: null;
                                           Integer number = null;
                                           if (input instanceof Integer) {
                                               number = (Integer) input;
                                           } else if (input instanceof Number) {
                                               number = ((Number) input).intValue();
                                           } else if (input instanceof String) {
                                               try {
                                                   number = Integer.parseInt((String) input);
                                               } catch (NumberFormatException nfe) {
                                                   //integer parsing failed. null would be returned.
                                               }
                                           }
                                           return number;
                                       });
        coreFunctions.put("long",
                          (context, compilationRuntimeContext) -> {
                                           Object input = context.length == 1? context[0]: null;
                                           Long number = null;
                                           if (input instanceof Long) {
                                               number = (Long) input;
                                           } else if (input instanceof Number) { 
                                               number = ((Number) input).longValue();
                                           } else if (input instanceof String) {
                                               try {
                                                   number = Long.parseLong((String) input);
                                               } catch (NumberFormatException nfe) {
                                                   //long parsing failed. null would be returned.
                                               }
                                           }
                                           return number;
                                       });
        coreFunctions.put("float",
                          (context, compilationRuntimeContext) -> {
                                           Object input = context.length == 1? context[0]: null;
                                           Float number = null;
                                           if (input instanceof Float) {
                                               number = (Float) input;
                                           } else if (input instanceof Number) { 
                                               number = ((Number) input).floatValue();
                                           } else if (input instanceof String) {
                                               try {
                                                   number = Float.parseFloat((String) input);
                                               } catch (NumberFormatException nfe) {
                                                   //float parsing failed. null would be returned.
                                               }
                                           }
                                           return number;
                                       });
        coreFunctions.put("double",
                          (context, compilationRuntimeContext) -> {
                                           Object input = context.length == 1? context[0]: null;
                                           Double number = null;
                                           if (input instanceof Double) {
                                               number = (Double) input;
                                           } else if (input instanceof Number) { 
                                               number = ((Number) input).doubleValue();
                                           } else if (input instanceof String) {
                                               try {
                                                   number = Double.parseDouble((String) input);
                                               } catch (NumberFormatException nfe) {
                                                   //double parsing failed. null would be returned.
                                               }
                                           }
                                           return number;
                                       });
        coreFunctions.put("abs",
                          (context, compilationRuntimeContext) -> {
                                           Object input = context.length == 1? context[0]: null;
                                           if (input instanceof Double) {
                                               return Math.abs((Double) input);
                                           } else if (input instanceof Float) {
                                               return Math.abs((Float) input);
                                           } else if (input instanceof Integer) {
                                               return Math.abs((Integer) input);
                                           } else if (input instanceof Long) {
                                               return Math.abs((Long) input);
                                           } else if (input instanceof String) {
                                               try {
                                                   return Math.abs(Double.parseDouble((String) input));
                                               } catch (NumberFormatException nfe) {
                                                   //double parsing failed. null would be returned.
                                               }
                                           }
                                           return null;
                                       });
         coreFunctions.put("ceil",
                          (context, compilationRuntimeContext) -> {
                                           Object input = context.length == 1? context[0]: null;
                                           if (input instanceof Number) {
                                               return Math.ceil(((Number) input).doubleValue());
                                           } else if (input instanceof String) {
                                               try {
                                                   return Math.ceil(Double.parseDouble((String) input));
                                               } catch (NumberFormatException nfe) {
                                                   //double parsing failed. null would be returned.
                                               }
                                           }
                                           return null;
                                       });
        coreFunctions.put("floor",
                          (context, compilationRuntimeContext) -> {
                                           Object input = context.length == 1? context[0]: null;
                                           if (input instanceof Number) { 
                                               return Math.floor(((Number) input).doubleValue());
                                           } else if (input instanceof String) {
                                               try {
                                                   return Math.floor(Double.parseDouble((String) input));
                                               } catch (NumberFormatException nfe) {
                                                   //double parsing failed. null would be returned.
                                               }
                                           }
                                           return null;
                                       });
        coreFunctions.put("sum",  //mathematical sum of numeric values
                          (context, compilationRuntimeContext) -> {
                                           Object[] input = context.length == 1 && context[0] != null && context[0].getClass().isArray()?
                                                                 (Object[]) context[0]:
                                                                 context;
                                           Number sum = null;
                                           Number number = null;
                                           boolean nonNumericInputFound = false;
                                           for (Object obj: input) {
                                               try {
                                                   number = toNum.toNum(obj);
                                               } catch (NumberFormatException nfe) {
                                                   nonNumericInputFound = true;
                                                   break;
                                               }

                                               if (number != null) {
                                                   if (sum == null) {
                                                       sum = number;
                                                   } else {
                                                       if (sum instanceof Double || number instanceof Double) {
                                                           sum = sum.doubleValue() + number.doubleValue();
                                                       } else if (sum instanceof Float || number instanceof Float) {
                                                           sum = sum.floatValue() + number.floatValue();
                                                       } else if (sum instanceof Long || number instanceof Long) {
                                                           sum = sum.longValue() + number.longValue();
                                                       } else {
                                                           sum = sum.intValue() + number.intValue();
                                                       }
                                                   }
                                               }
                                           }
                                           return nonNumericInputFound? null: sum;
                                       });
        coreFunctions.put("multiply",  //mathematical multiplication of numeric values
                          (context, compilationRuntimeContext) -> {
                                           Object[] input = context.length == 1 && context[0] != null && context[0].getClass().isArray()?
                                                                 (Object[]) context[0]:
                                                                 context;
                                           Number multiplication = null;
                                           Number number = null;
                                           boolean nonNumericInputFound = false;
                                           for (Object obj: input) {
                                               try {
                                                   number = toNum.toNum(obj);
                                               } catch (NumberFormatException nfe) {
                                                   nonNumericInputFound = true;
                                                   break;
                                               }

                                               if (number != null) {
                                                   if (multiplication == null) {
                                                       multiplication = number;
                                                   } else {
                                                       if (multiplication instanceof Double || number instanceof Double) {
                                                           multiplication = multiplication.doubleValue() * number.doubleValue();
                                                       } else if (multiplication instanceof Float || number instanceof Float) {
                                                           multiplication = multiplication.floatValue() * number.floatValue();
                                                       } else if (multiplication instanceof Long || number instanceof Long) {
                                                           multiplication = multiplication.longValue() * number.longValue();
                                                       } else {
                                                           multiplication = multiplication.intValue() * number.intValue();
                                                       }
                                                   }
                                               }
                                           }
                                           return nonNumericInputFound? null: multiplication;
                                       });
        coreFunctions.put("subtract",  //mathematical subtraction of numeric values
                          (context, compilationRuntimeContext) -> {
                                           if (context.length == 0 || context.length > 2) {
                                               return null;
                                           }
                                           boolean invalidOperand = false;
                                           Number first = null;
                                           Number second = null;
                                           try {
                                               first = toNum.toNum(context[0]);
                                           } catch (NumberFormatException nfe) {
                                               invalidOperand = true;
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           if (context.length == 2) {
                                               try {
                                                   second = toNum.toNum(context[1]);
                                               } catch (NumberFormatException nfe) {
                                                   invalidOperand = true;
                                               }
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           Number result = null;
                                           if (first == null && second == null) {
                                               result = null;
                                           } else if (first == null) {
                                               result = second instanceof Double? second.doubleValue() * -1:
                                                        second instanceof Float? second.floatValue() * -1:
                                                        second instanceof Long? second.longValue() * -1:
                                                        second.intValue() * -1;
                                           } else if (second == null) {
                                               result = first;
                                           } else {
                                               if (first instanceof Double || second instanceof Double) {
                                                   result = first.doubleValue() - second.doubleValue();
                                               } else if (first instanceof Float || second instanceof Float) {
                                                   result = first.floatValue() - second.floatValue();
                                               } else if (first instanceof Long || second instanceof Long) {
                                                   result = first.longValue() - second.longValue();
                                               } else {
                                                   result = first.intValue() - second.intValue();
                                               }
                                           }
                                           return result;
                                       });
        coreFunctions.put("div",  //mathematical division of numeric values
                          (context, compilationRuntimeContext) -> {
                                           if (context.length == 0 || context.length > 2) {
                                               return null;
                                           }
                                           boolean invalidOperand = false;
                                           Number first = null;
                                           Number second = null;
                                           try {
                                               first = toNum.toNum(context[0]);
                                           } catch (NumberFormatException nfe) {
                                               invalidOperand = true;
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           if (context.length == 2) {
                                               try {
                                                   second = toNum.toNum(context[1]);
                                               } catch (NumberFormatException nfe) {
                                                   invalidOperand = true;
                                               }
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           Number result = null;
                                           if (first != null && second != null) {
                                               if (first instanceof Double || second instanceof Double) {
                                                   result = first.doubleValue() / second.doubleValue();
                                               } else if (first instanceof Float || second instanceof Float) {
                                                   result = first.floatValue() / second.floatValue();
                                               } else if (first instanceof Long || second instanceof Long) {
                                                   result = first.longValue() / second.longValue();
                                               } else {
                                                   result = first.intValue() / second.intValue();
                                               }
                                           }
                                           return result;
                                       });
        coreFunctions.put("mod",  //mathematical modulous of numeric values
                          (context, compilationRuntimeContext) -> {
                                           if (context.length == 0 || context.length > 2) {
                                               return null;
                                           }
                                           boolean invalidOperand = false;
                                           Number first = null;
                                           Number second = null;
                                           try {
                                               first = toNum.toNum(context[0]);
                                           } catch (NumberFormatException nfe) {
                                               invalidOperand = true;
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           if (context.length == 2) {
                                               try {
                                                   second = toNum.toNum(context[1]);
                                               } catch (NumberFormatException nfe) {
                                                   invalidOperand = true;
                                               }
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           Number result = null;
                                           if (first != null && second != null) {
                                               if (first instanceof Double || second instanceof Double) {
                                                   result = first.doubleValue() % second.doubleValue();
                                               } else if (first instanceof Float || second instanceof Float) {
                                                   result = first.floatValue() % second.floatValue();
                                               } else if (first instanceof Long || second instanceof Long) {
                                                   result = first.longValue() % second.longValue();
                                               } else {
                                                   result = first.intValue() % second.intValue();
                                               }
                                           }
                                           return result;
                                       });
        coreFunctions.put("lt",  //boolean lessthan (<) operator of numeric values
                          (context, compilationRuntimeContext) -> {
                                           if (context.length == 0 || context.length > 2) {
                                               return null;
                                           }
                                           boolean invalidOperand = false;
                                           Number first = null;
                                           Number second = null;
                                           try {
                                               first = toNum.toNum(context[0]);
                                           } catch (NumberFormatException nfe) {
                                               invalidOperand = true;
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           if (context.length == 2) {
                                               try {
                                                   second = toNum.toNum(context[1]);
                                               } catch (NumberFormatException nfe) {
                                                   invalidOperand = true;
                                               }
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           Boolean result = null;
                                           if (first != null && second != null) {
                                               if (first instanceof Double || second instanceof Double) {
                                                   result = first.doubleValue() < second.doubleValue();
                                               } else if (first instanceof Float || second instanceof Float) {
                                                   result = first.floatValue() < second.floatValue();
                                               } else if (first instanceof Long || second instanceof Long) {
                                                   result = first.longValue() < second.longValue();
                                               } else {
                                                   result = first.intValue() < second.intValue();
                                               }
                                           }
                                           return result;
                                       });
        coreFunctions.put("lte",  //boolean lessthanOrequals (<=) operator of numeric values
                          (context, compilationRuntimeContext) -> {
                                           if (context.length == 0 || context.length > 2) {
                                               return null;
                                           }
                                           boolean invalidOperand = false;
                                           Number first = null;
                                           Number second = null;
                                           try {
                                               first = toNum.toNum(context[0]);
                                           } catch (NumberFormatException nfe) {
                                               invalidOperand = true;
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           if (context.length == 2) {
                                               try {
                                                   second = toNum.toNum(context[1]);
                                               } catch (NumberFormatException nfe) {
                                                   invalidOperand = true;
                                               }
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           Boolean result = null;
                                           if (first != null && second != null) {
                                               if (first instanceof Double || second instanceof Double) {
                                                   result = first.doubleValue() <= second.doubleValue();
                                               } else if (first instanceof Float || second instanceof Float) {
                                                   result = first.floatValue() <= second.floatValue();
                                               } else if (first instanceof Long || second instanceof Long) {
                                                   result = first.longValue() <= second.longValue();
                                               } else {   
                                                   result = first.intValue() <= second.intValue();
                                               }
                                           }
                                           return result;
                                       });
        coreFunctions.put("gt",  //boolean greaterthan (>) operator of numeric values
                          (context, compilationRuntimeContext) -> {
                                           if (context.length == 0 || context.length > 2) {
                                               return null;
                                           }
                                           boolean invalidOperand = false;
                                           Number first = null;
                                           Number second = null;
                                           try {
                                               first = toNum.toNum(context[0]);
                                           } catch (NumberFormatException nfe) {
                                               invalidOperand = true;
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           if (context.length == 2) {
                                               try {
                                                   second = toNum.toNum(context[1]);
                                               } catch (NumberFormatException nfe) {
                                                   invalidOperand = true;
                                               }
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           Boolean result = null;
                                           if (first != null && second != null) {
                                               if (first instanceof Double || second instanceof Double) {
                                                   result = first.doubleValue() > second.doubleValue();
                                               } else if (first instanceof Float || second instanceof Float) {
                                                   result = first.floatValue() > second.floatValue();
                                               } else if (first instanceof Long || second instanceof Long) {
                                                   result = first.longValue() > second.longValue();
                                               } else {
                                                   result = first.intValue() > second.intValue();
                                               }
                                           }
                                           return result;
                                       });
        coreFunctions.put("gte",  //boolean greaterthanOrequals (>=) operator of numeric values
                          (context, compilationRuntimeContext) -> {
                                           if (context.length == 0 || context.length > 2) {
                                               return null;
                                           }
                                           boolean invalidOperand = false;
                                           Number first = null;
                                           Number second = null;
                                           try {
                                               first = toNum.toNum(context[0]);
                                           } catch (NumberFormatException nfe) {
                                               invalidOperand = true;
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           if (context.length == 2) {
                                               try {
                                                   second = toNum.toNum(context[1]);
                                               } catch (NumberFormatException nfe) {
                                                   invalidOperand = true;
                                               }
                                           }

                                           if (invalidOperand) {
                                               return null;
                                           }

                                           Boolean result = null;
                                           if (first != null && second != null) {
                                               if (first instanceof Double || second instanceof Double) {
                                                   result = first.doubleValue() >= second.doubleValue();
                                               } else if (first instanceof Float || second instanceof Float) {
                                                   result = first.floatValue() >= second.floatValue();
                                               } else if (first instanceof Long || second instanceof Long) {
                                                   result = first.longValue() >= second.longValue();
                                               } else {
                                                   result = first.intValue() >= second.intValue();
                                               }
                                           }
                                           return result;
                                       });
        coreFunctions.put("length",
                           (context, compilationRuntimeContext) -> {
                                           Object target = context.length > 0? context[0]: null;
                                           int size = -1;
                                           if (target instanceof List) {
                                               size = ((List) target).size();
                                           } else if (target != null && target.getClass().isArray()) {
                                               //using reflection here to access array element in a generic way.
                                               size = Array.getLength(target);
                                           } else if (target instanceof Collection) {
                                               size = ((Collection) target).size();
                                           } else if (target instanceof Map) {
                                               size = ((Map) target).size();
                                           } else if (target instanceof String) {
                                               size = ((String) target).length();
                                           }
                                           return size != -1? size: null;
                                       });
        coreFunctions.put("array",
                          (context, compilationRuntimeContext) -> {
                                           return context;  //return context array as is
                                       });
        coreFunctions.put("array-copyofrange",
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
        coreFunctions.put("array-contains",
                          (context, compilationRuntimeContext) -> {
                                           Object collection = context.length > 0? context[0]: null;
                                           if (collection == null || context.length < 2) {
                                               return null;  //should we return false?
                                           }
                                           Object value = context[1];
                                           return collection instanceof Collection? ((Collection) collection).contains(value):
                                                  collection.getClass().isArray()? Arrays.asList((Object[]) collection).contains(value):
                                                  null;
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
        coreFunctions.put("file-isdirectory",
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
        coreFunctions.put("str-replaceregex",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 3) {
                                               return str;
                                           }
                                           String regex = (String) context[1];
                                           String replacement = (String) context[2];
                                           return str.replaceAll(regex, replacement);
                                       });
        coreFunctions.put("str-startswith",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return null;  //should we return false?
                                           }
                                           String prefix = (String) context[1];
                                           return str.startsWith(prefix);
                                       });
        coreFunctions.put("str-endswith",
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
        coreFunctions.put("str-split",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return str == null? null: str.split("", 0);  //should we simply return null?
                                           }
                                           String delimiterRegex = (String) context[1];
                                           delimiterRegex = delimiterRegex == null? "": delimiterRegex;
                                           int limit = 0;
                                           try {
                                               if (context.length > 2 && context[2] != null)
                                                   limit = Integer.parseInt(context[2].toString());
                                           } catch (NumberFormatException nfe) {
                                               //ignore
                                           }
                                           return str.split(delimiterRegex, limit);
                                       });
        coreFunctions.put("str-join",
                          (context, compilationRuntimeContext) -> {
                                           String delimiter = context.length > 0? (String) context[0]: null;
                                           if (context.length <= 1) {
                                               return null;  //should we return an empty String?
                                           }
                                           delimiter = delimiter == null? "": delimiter;
                                           StringBuilder strBuilder = new StringBuilder();
                                           Object[] arrayToJoin = context.length == 2 &&
                                                                  context[1] != null &&
                                                                  context[1].getClass().isArray()?
                                                                         (Object[]) context[1]:
                                                                         context;
                                           for (int i = (arrayToJoin == context? 1: 0); i < arrayToJoin.length; i++) {
                                               strBuilder.append(arrayToJoin[i]);
                                               if (i != arrayToJoin.length - 1) {
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
        coreFunctions.put("str-tobytes",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           return str != null? str.getBytes(): null;
                                       });
        coreFunctions.put("str-frombytes",
                          (context, compilationRuntimeContext) -> {
                                           byte[] bytes = context.length > 0? (byte[]) context[0]: null;
                                           return bytes != null? new String(bytes): null;
                                       });
        coreFunctions.put("str-todate",
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
        coreFunctions.put("str-toinstant",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           return str != null? Instant.parse(str): null;
                                       });
        coreFunctions.put("str-tolocaldatetime",
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
        coreFunctions.put("substring",
                          (context, compilationRuntimeContext) -> {
                                           String str = context.length > 0? (String) context[0]: null;
                                           if (str == null || context.length < 2) {
                                               return str == null? null: str;
                                           }
                                           Object _beginIndex = context[1];
                                           Object _endIndex = context.length > 2? context[2]: null;
                                           int beginIndex = -1;
                                           int endIndex = -1;
                                           try {
                                               if (_beginIndex != null) {
                                                   beginIndex = Integer.parseInt(_beginIndex.toString());
                                               }
                                           } catch (NumberFormatException nfe) {
                                               //invalid input
                                               return null;
                                           }
                                           try {
                                               if (_endIndex != null) {
                                                   endIndex = Integer.parseInt(_endIndex.toString());
                                               }
                                           } catch (NumberFormatException nfe) {
                                               //invalid input
                                               return null;
                                           }
                                           return beginIndex == -1 && endIndex == -1?
                                                      str:
                                                      (beginIndex >= 0 && beginIndex < str.length() && endIndex == -1)?
                                                          str.substring(beginIndex):
                                                          (beginIndex == -1 && endIndex >= 0 && endIndex <= str.length())?
                                                              str.substring(0, endIndex):
                                                              (beginIndex >=0 && beginIndex < str.length() &&
                                                               endIndex >=0 && endIndex <= str.length() && beginIndex <= endIndex)?
                                                                  str.substring(beginIndex, endIndex):
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
        coreFunctions.put("date-toepoch",
                          (context, compilationRuntimeContext) -> {
                                           Date date = context.length > 0? (Date) context[0]: null;
                                           return date != null? date.getTime(): null;
                                       });
        coreFunctions.put("date-tostr",
                          (context, compilationRuntimeContext) -> {
                                           Date date = context.length > 0? (Date) context[0]: null;
                                           String format = context.length > 1? (String) context[1]: null;
                                           return date != null?
                                                    (format != null?
                                                      (new SimpleDateFormat(format)).format(date):
                                                      date.toString()
                                                    ):
                                                    null;
                                       });
        coreFunctions.put("epoch",
                          (context, compilationRuntimeContext) -> System.currentTimeMillis());
        coreFunctions.put("true",
                          (context, compilationRuntimeContext) -> true);
        coreFunctions.put("false",
                          (context, compilationRuntimeContext) -> false);
        coreFunctions.put("null",
                          (context, compilationRuntimeContext) -> null);
        coreFunctions.put("isnull",
                          (context, compilationRuntimeContext) -> context.length == 0 ||
                                                                  (context.length == 1 && context[0] == null));
        coreFunctions.put("not",
                          (context, compilationRuntimeContext) -> context.length == 0 || (context.length == 1 && context[0] == null)?
                                                                      true:
                                                                      context[0] instanceof Boolean?
                                                                          !((Boolean) context[0]):
                                                                          context[0] instanceof String &&
                                                                          ("".equals((String) context[0]) ||
                                                                           "false".equalsIgnoreCase((String) context[0]))?
                                                                              true:
                                                                              false);
        coreFunctions.put("ifelse",  //returns context[1] if condition is satisfied
                                     //else returns context[2] iff context.length == 3 else returns null
                          (context, compilationRuntimeContext) -> {
                                           if (context.length < 2) {
                                               return null;
                                           }
                                           boolean satisfiesIf = (context[0] instanceof Boolean && Boolean.valueOf((Boolean) context[0])) ||
                                                                 (context[0] != null && Boolean.valueOf(context[0].toString().toLowerCase()));
                                           return satisfiesIf? context[1]: (context.length == 3? context[2]: null);
                                       });
        coreFunctions.put("ifelsen",  //returns context[1] if condition is satisfied
                                      //else returns the first non null value from context[n] where n >= 2
                          (context, compilationRuntimeContext) -> {
                                           if (context.length < 2) {
                                               return null;
                                           }
                                           boolean satisfiesIf = (context[0] instanceof Boolean && Boolean.valueOf((Boolean) context[0])) ||
                                                                 (context[0] != null && Boolean.valueOf(context[0].toString().toLowerCase()));
                                           Object returnValue = null;
                                           if (satisfiesIf) {
                                               returnValue = context[1];
                                           } else if (context.length >= 3) {
                                               for (int i = 2; i < context.length; i++) {
                                                   if (context[i] != null) {
                                                       returnValue = context[i];
                                                       break;
                                                   }
                                               }
                                           }
                                           return returnValue;
                                       });
        coreFunctions.put("isassignablefrom",
                          (context, compilationRuntimeContext) -> context.length == 2 &&
                                                                  context[0] != null && context[1] != null &&
                                                                  context[0].getClass().isAssignableFrom(context[1].getClass()));
        coreFunctions.put("instanceof",
                          (context, compilationRuntimeContext) -> context.length == 2 &&
                                                                  context[0] != null && context[1] instanceof String &&
                                                                  Class.forName((String) context[1]).isInstance(context[0]));
        coreFunctions.put("typeof",
                          (context, compilationRuntimeContext) -> context.length == 1 && context[0] != null?
                                                                      context[0].getClass().getName():
                                                                      null);
        coreFunctions.put("tostring",
                          (context, compilationRuntimeContext) -> context.length == 1 && context[0] != null?
                                                                      context[0].toString():
                                                                      null);  //should we instead return an empty string?
        coreFunctions.put("equals",
                          (context, compilationRuntimeContext) -> {
                                           Object obj1 = context.length > 0? context[0]: null;
                                           if (obj1 == null) {
                                               return false;
                                           }
                                           Object obj2 = context.length > 1? context[1]: null;
                                           return obj1.equals(obj2);
                                       });
        coreFunctions.put("hashcode",
                          (context, compilationRuntimeContext) -> {
                                           Object obj = context.length > 0? context[0]: null;
                                           return obj != null? obj.hashCode(): null;
                                       });
        return coreFunctions;
    }

    private DefaultPlatformFunctions() {
    }
}
