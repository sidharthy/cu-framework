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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Path;

import java.time.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.cuframework.core.CompilationUnits.IExecutable;
import org.cuframework.core.CompilationUnits.HeadlessExecutableGroup;

/**
 * @author Sidharth Yadav
 */
public class HttpIO extends HeadlessExecutableGroup implements IExecutable {
    public static final String TAG_NAME = "http";

    //cu input parameters
    private static final String PARAM_METHOD = "method";  //http method to invoke e.g. get, post, put, delete
    private static final String PARAM_URI = "uri";  //uri to connect to
    private static final String PARAM_HEADERS = "headers";  //generic key to represent request or response headers
    private static final String PARAM_PAYLOAD = "payload";  //request payload
    private static final String PARAM_PAYLOAD_SOURCE = "payload-source";  //request payload source e.g. string, file
    private static final String PARAM_CONNECT_TIMEOUT = "timeout";  //request connect timeout in milli secs
    private static final String PARAM_RESPONSE_HANDLER = "response-body-handler";  //response body handler e.g. string, file, inputstream, bytearray, discard
    private static final String PARAM_RESPONSE_TARGET = "response-target";  //response target can point to some file path if the response body handler was a file.
    private static final String PARAM_HTTP_CLIENT = "http-client";  //http client instance to use/reuse for subsequent requests
    private static final String PARAM_COOKIE_HANDLER = "cookie-handler";  //optionally a cookie handler/manager can be passed for use with http client
    private static final String PARAM_USE_PROXY = "use-proxy";  //indicates if a proxy is to be used. 'true' means use proxy.
    private static final String PARAM_PROXY_HOST = "proxy-host";  //proxy host to use
    private static final String PARAM_PROXY_PORT = "proxy-port";  //proxy port to use

    @Override
    public String getTagName() {
        return HttpIO.TAG_NAME;
    }

    //the result or outcome of the execution should be set inside requestContext as a map and the name of the map key should be returned as the value of the function.
    @Override
    protected String doExecute(Map<String, Object> requestContext) {
        String uri = (String) requestContext.get(PARAM_URI);
        if (uri == null) {
            return null;
        }
        String method = (String) requestContext.get(PARAM_METHOD);  //possible values - get, post, put, delete
        if (method == null) {
            //return null;
            method = "get";  //let's default method to get OR shall we just return OR throw a runtime exception?
        }
        String resultMapKeyName = "-http-io-result-";
        String idOrElse = getIdOrElse();  //using the non computed version of idOrElse
        String resultKeyName = idOrElse == null? "result": idOrElse;
        Map<String, Object> resultMap = new HashMap<>();
        try {
            switch(method.toUpperCase().trim()) {
                case "GET": resultMap.put(resultKeyName, doGet(requestContext)); break;
                case "POST": resultMap.put(resultKeyName, doPost(requestContext)); break;
                case "PUT": resultMap.put(resultKeyName, doPut(requestContext)); break;
                case "DELETE": resultMap.put(resultKeyName, doDelete(requestContext)); break;
                default: throw new UnsupportedOperationException("Unsupported http method: " + method);
            }
            requestContext.put(resultMapKeyName, resultMap);
        } catch(Exception e) {
            //any custom processing needed?
            throw new RuntimeException(e);
        }
        return resultMapKeyName;
    }

    private Object doGet(Map<String, Object> requestContext) throws IOException, InterruptedException {
        return doHttp(HttpRequest.newBuilder().GET(), requestContext);
    }

    private Object doPost(Map<String, Object> requestContext) throws IOException, InterruptedException {
        return doHttp(HttpRequest.newBuilder().POST(_requestPayload(requestContext)), requestContext); 
    }

    private Object doPut(Map<String, Object> requestContext) throws IOException, InterruptedException {
        return doHttp(HttpRequest.newBuilder().PUT(_requestPayload(requestContext)), requestContext);
    }

    private Object doDelete(Map<String, Object> requestContext) throws IOException, InterruptedException {
        return doHttp(HttpRequest.newBuilder().DELETE(), requestContext);
    }

    private Object doHttp(HttpRequest.Builder httpRequestBuilder, Map<String, Object> requestContext) throws IOException, InterruptedException {
        //set the request uri
        httpRequestBuilder.uri(URI.create(requestContext.get(PARAM_URI).toString()));

        //set the request headers
        Object headers = requestContext.get(PARAM_HEADERS);
        if (headers != null) {
            if (headers instanceof Map) {
                for (Entry<String, Object> entry : ((Map<String, Object>) headers).entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        httpRequestBuilder.header(key, value.toString());
                    }
                }
            } else if (headers instanceof String[]) {
                httpRequestBuilder.headers((String[]) headers);  //it is assumed here that the string array would contain key and value at
                                                                 //alternate indices and the keys would represent valid http headers.
                                                                 //Otherwise exception may get thrown at runtime and http call would fail.
            }
        }

        //set the connect timeout
        Object timeout = requestContext.get(PARAM_CONNECT_TIMEOUT);
        if (timeout != null) {
            try {
                httpRequestBuilder.timeout(Duration.ofMillis(Long.parseLong(timeout.toString())));
            } catch (NumberFormatException nfe) {
                //ignore. No timeout would be set.
            }
        }

        //build the request
        HttpRequest httpRequest = httpRequestBuilder.build();

        //get the client
        HttpClient httpClient = _httpClient(requestContext);

        //return the response
        return _responseMap(httpClient, httpClient.send(httpRequest, _responsePayloadHandler(requestContext)));
    }

    private HttpClient _httpClient(Map<String, Object> requestContext) {
        Object httpc = requestContext.get(PARAM_HTTP_CLIENT);
        if (httpc instanceof HttpClient) {
            return (HttpClient) httpc;
        }

        CookieHandler ch = null;
        Object _ch = requestContext.get(PARAM_COOKIE_HANDLER);
        if (_ch instanceof CookieHandler) {
            ch = (CookieHandler) _ch;
        } else {
            ch = new CookieManager();
        }
        HttpClient.Builder httpClientBuilder =  HttpClient.newBuilder() 
                                                          .version(HttpClient.Version.HTTP_1_1)
                                                          .cookieHandler(ch);

        Object useProxy = requestContext.get(PARAM_USE_PROXY);
        if (useProxy != null && "true".equalsIgnoreCase(useProxy.toString()))
        {
             Object phost = requestContext.get(PARAM_PROXY_HOST);
             Object pport = requestContext.get(PARAM_PROXY_PORT);
             ProxySelector pselector = phost != null && pport != null?
                                          ProxySelector.of(InetSocketAddress.
                                                             createUnresolved(phost.toString(),
                                                                              Integer.parseInt(pport.toString()))):  //let it throw NFE if port isn't int
                                          ProxySelector.getDefault();
             httpClientBuilder.proxy(pselector);
        }

        return httpClientBuilder.build();
    }

    //payload generator primarily for put and post requests
    private HttpRequest.BodyPublisher _requestPayload(Map<String, Object> requestContext) throws FileNotFoundException {
        Object payload = requestContext.get(PARAM_PAYLOAD);
        HttpRequest.BodyPublisher reqPayloadPublisher = null;
        if (payload == null)
            reqPayloadPublisher = HttpRequest.BodyPublishers.ofString("");
        else if (payload instanceof String) {
            Object payloadSource = requestContext.get(PARAM_PAYLOAD_SOURCE);
            reqPayloadPublisher = payloadSource != null && "file".equalsIgnoreCase(payloadSource.toString())?
                                                          HttpRequest.BodyPublishers.ofFile(Path.of((String) payload)):
                                                          HttpRequest.BodyPublishers.ofString((String) payload);
        } else if (payload instanceof byte[]) {
            reqPayloadPublisher = HttpRequest.BodyPublishers.ofByteArray((byte[]) payload);
        } else if (payload instanceof InputStream) {
            reqPayloadPublisher = HttpRequest.BodyPublishers.ofInputStream(() -> (InputStream) payload);
        } else {
            throw new IllegalArgumentException("Unsupported payload type encountered: " + payload.getClass().getName());
        }
        return reqPayloadPublisher;
    }

    private HttpResponse.BodyHandler _responsePayloadHandler(Map<String, Object> requestContext) throws FileNotFoundException, IOException {
        Object handler = requestContext.get(PARAM_RESPONSE_HANDLER);
        HttpResponse.BodyHandler responseBodyHandler = null;
        if (handler == null)
            handler = "string";  //default handler
        switch(handler.toString().toLowerCase()) {
            case "string": responseBodyHandler = HttpResponse.BodyHandlers.ofString(); break;
            case "file": {
                             Object target = requestContext.get(PARAM_RESPONSE_TARGET);
                             Path targetPath = target instanceof String? Path.of((String) target): File.createTempFile(TAG_NAME, "cu").toPath();
                             responseBodyHandler = HttpResponse.BodyHandlers.ofFile(targetPath);
                             break;
                         }
            case "bytearray": responseBodyHandler = HttpResponse.BodyHandlers.ofByteArray(); break;
            case "inputstream": responseBodyHandler = HttpResponse.BodyHandlers.ofInputStream(); break;
            case "discard":
            default: responseBodyHandler = HttpResponse.BodyHandlers.discarding();
        }
        return responseBodyHandler;
    }

    private Map<String, Object> _responseMap(HttpClient httpClient, HttpResponse httpResponse) {
        String REQUEST_MAP = "request";
        String RESPONSE_MAP = "response";
        String RESPONSE_STATUS_CODE = "status-code";
        String RESPONSE_BODY = "body";
        Map<String, Map<String, Object>> requestResponseMap = new HashMap<>();
        requestResponseMap.put(REQUEST_MAP, new HashMap<String, Object>());
        requestResponseMap.put(RESPONSE_MAP, new HashMap<String, Object>());

        //populate request related data
        requestResponseMap.get(REQUEST_MAP).put(PARAM_URI, httpResponse.request().uri().toString());
        requestResponseMap.get(REQUEST_MAP).put(PARAM_HEADERS, httpResponse.request().headers().map());

        //populate response related data
        requestResponseMap.get(RESPONSE_MAP).put(RESPONSE_STATUS_CODE, httpResponse.statusCode());
        requestResponseMap.get(RESPONSE_MAP).put(PARAM_URI, httpResponse.uri().toString());
        requestResponseMap.get(RESPONSE_MAP).put(PARAM_HEADERS, httpResponse.headers().map());
        requestResponseMap.get(RESPONSE_MAP).put(RESPONSE_BODY, httpResponse.body());

        Map<String, Object> returnMap = new HashMap<>();
        returnMap.putAll(requestResponseMap);
        returnMap.put(PARAM_HTTP_CLIENT, httpClient);
        return returnMap;
    }

    //overridden method to support cloning
    @Override
    protected HttpIO newInstance() {
        return new HttpIO();
    }
}
