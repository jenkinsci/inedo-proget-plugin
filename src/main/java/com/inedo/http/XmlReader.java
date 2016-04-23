package com.inedo.http;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A wrapper around "javax.xml" for simplifying the parsing XML strings.
 * 
 * @author Andrew Sumner
 */
public class XmlReader {
	private final Document document;	
	
	/**
	 * An xml reader.
	 * 
	 * @param xml XML string
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public XmlReader(String xml) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = domFactory.newDocumentBuilder();
	

		InputSource src = new InputSource();
		src.setCharacterStream(new StringReader(xml));
	
		document = builder.parse(src);
	}
	
	/**
	 * Search a JSON element's children for the requested element.
	 * 
	 * @param selector search path
	 * @return Node
	 */
	public Node evaluate(String selector) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		return (Node) xPath.evaluate(selector, document, XPathConstants.NODE);		
	}

	/**
	 * @return The XML document as a string.
	 */
	public String asPrettyString() {
		DOMImplementationLS domImplementation = (DOMImplementationLS) document.getImplementation();
		LSSerializer lsSerializer = domImplementation.createLSSerializer();
		return lsSerializer.writeToString(document); 
	}
}
