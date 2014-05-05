package com.esri.gpt.catalog.provenance;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ProvenanceXML {

	private Document doc;
	private String fileIdentifier;

	private void addNode(Element p, String nodeName, String nodeValue) {
		Element el = doc.createElement(nodeName);
		el.setAttribute("value", nodeValue);
		p.appendChild(el);
	}
	
	private Element addArtifact(String fileid, String title, String uri) {
		Element el = doc.createElement("opm:artifact");
		el.setAttribute("id", fileid);
		addNode(el, "opm:label", title);
		addNode(el, "dc:uri", uri);
		return el;
	}
	
	private Element addEdge(String fileIdEffect, String fileIdCause) {
		Element el = doc.createElement("opm:wasDerivedFrom");
		addNode(el, "opm:effect", fileIdEffect);
		addNode(el, "opm:cause",  fileIdCause);
		return el;
	}

	public String addProvenanceXML(String inputXML, ProvenanceQuery pq,
			String serverUrl) {
		ProvenanceRecord record = pq.getRecord();
		ArrayList<ProvenanceRecord> ancesters = pq.getAncesters();
		ArrayList<ProvenanceRecord> children = pq.getChildren();
		ProvenanceRecord aRecord;
		
		fileIdentifier = record.getFileIdentifier();
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			docFactory.setNamespaceAware(true);
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.parse(new InputSource(new StringReader(inputXML)));

			// Get the root element to add OPM and DC namespaces
			Element rootNode = (Element) doc.getFirstChild();
			rootNode.setAttribute("xmlns:opm",
					"http://openprovenance.org/model/opmx");
			rootNode.setAttribute("xmlns:dc",
					"http://purl.org/dc/elements/1.1/");

			// append OPM graph to the end of the document
			Element graph = doc.createElement("opm:opmGraph");
			rootNode.appendChild(graph);

			// append this resource node
			graph.appendChild(addArtifact(
					record.getFileIdentifier(),
					record.getTitle(),
					serverUrl
							+ "/geoportal/catalog/search/resource/details.page?uuid="
							+ URLEncoder.encode(record.getUuid(), "UTF-8")));
			
			// append one parent resource node if it has
			if (! ancesters.isEmpty()) {
				aRecord = ancesters.get(0);
				graph.appendChild(addArtifact(
						aRecord.getFileIdentifier(),
						aRecord.getTitle(),
						serverUrl
								+ "/geoportal/catalog/search/resource/details.page?uuid="
								+ URLEncoder.encode(aRecord.getUuid(), "UTF-8")));
				graph.appendChild(addEdge(fileIdentifier, aRecord.getFileIdentifier()));
			}
			
			// append one parent resource node if it has
			if (!children.isEmpty()) {
				for (ProvenanceRecord r : children) {
					aRecord = children.get(0);
					graph.appendChild(addArtifact(
							r.getFileIdentifier(),
							r.getTitle(),
							serverUrl
									+ "/geoportal/catalog/search/resource/details.page?uuid="
									+ URLEncoder.encode(r.getUuid(), "UTF-8")));
					graph.appendChild(addEdge(r.getFileIdentifier(), fileIdentifier));
				}
			}

			// ~ gmd:fileIdentifier or dc:identifier
			// ~ <opm:opmGraph xmlns:opm="http://openprovenance.org/model/opmx"
			// ~ xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			// ~ xmlns:xsd="http://www.w3.org/2001/XMLSchema">
			// ~ <opm:artifact id="#dc:identifier1">
			// ~ <opm:label value="#dc:title"/>
			// ~ <opm:type
			// value="http://openprovenance.org/primitives#resource"/>
			// ~ <!--
			// ~ <opm:account ref="black"/>
			// ~ <opm:annotation id="annotation1">
			// ~ <opm:property uri="http://openprovenance.org/primitives#url">
			// ~ <opm:value xsi:type="xsd:string">
			// ~ #service.url
			// ~ </opm:value>
			// ~ </opm:property>
			// ~ </opm:annotation>
			// ~ -->
			// ~ </opm:artifact>
			// ~ <opm:artifact id="#dc:identifier2">
			// ~ <opm:type
			// value="http://openprovenance.org/primitives#resource"/>
			// ~ </opm:artifact>
			// ~ <opm:wasDerivedFrom>
			// ~ <opm:effect ref="#dc:identifier1"/>
			// ~ <opm:cause ref="#dc:identifier2"/>
			// ~ </opm:wasDerivedFrom>
			// ~ </opm:opmGraph>

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			transformer.transform(source,
					new javax.xml.transform.stream.StreamResult(writer));

			return writer.toString();

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SAXException sae) {
			sae.printStackTrace();
		}
		return null;
	}
}
