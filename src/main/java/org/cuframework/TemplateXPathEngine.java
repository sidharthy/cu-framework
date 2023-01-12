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

package org.cuframework;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cuframework.config.ConfigManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import org.xml.sax.InputSource;

/**
 * Framework XPath Engine.
 * @author Sidharth Yadav
 *
 */
public final class TemplateXPathEngine {

    private TemplateXPathEngine() {
    }

    public static Node getNode(String xpathExpr, String source)
            throws FileNotFoundException, TemplateCompilationException, XPathExpressionException {
        return getNode(xpathExpr, new FileInputStream(source));
    }

    public static NodeList getNodes(String xpathExpr, String source)
            throws FileNotFoundException, TemplateCompilationException, XPathExpressionException {
        return getNodes(xpathExpr, new FileInputStream(source));
    }

    public static Node getNode(String xpathExpr, InputStream in)
            throws TemplateCompilationException, XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        /*InputSource inputSource = new InputSource(in);
        return (Node) xpath.evaluate(xpathExpr, inputSource,
                XPathConstants.NODE);*/
        return (Node) xpath.evaluate(xpathExpr, toInputSource(in),
                XPathConstants.NODE);
    }

    public static NodeList getNodes(String xpathExpr, InputStream in)
            throws TemplateCompilationException, XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        /*InputSource inputSource = new InputSource(in);
        return (NodeList) xpath.evaluate(xpathExpr, inputSource,
                XPathConstants.NODESET);*/
        return (NodeList) xpath.evaluate(xpathExpr, toInputSource(in),
                XPathConstants.NODESET);
    }

    private static Document toInputSource(InputStream in) throws TemplateCompilationException {
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setCoalescing(true);
            dbf.setNamespaceAware(ConfigManager.getInstance().isSystemNamespaceAware());
            return dbf.newDocumentBuilder().parse(in);
        } catch(Exception e) {
            throw new TemplateCompilationException(e);
        }
    }
}
