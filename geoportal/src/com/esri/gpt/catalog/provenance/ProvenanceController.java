package com.esri.gpt.catalog.provenance;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGroup;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.esri.gpt.catalog.management.MmdRecord;
import com.esri.gpt.catalog.schema.SchemaException;
import com.esri.gpt.catalog.search.SearchException;
import com.esri.gpt.control.search.SearchController;
import com.esri.gpt.framework.context.RequestContext;
import com.esri.gpt.framework.jsf.BaseActionListener;
import com.esri.gpt.framework.jsf.MessageBroker;

public class ProvenanceController extends BaseActionListener {

	private static final Logger LOG = Logger.getLogger(SearchController.class
			.getCanonicalName());

	/** The search result. */
	private MmdRecord record;

	public MmdRecord getRecord() {
		return record;
	}

	public void setRecord(MmdRecord record) {
		this.record = record;
	}

	private HtmlPanelGroup publisherPanelGroup;

	public HtmlPanelGroup getPublisherPanelGroup() {
		return publisherPanelGroup;
	}

	public void setPublisherPanelGroup(HtmlPanelGroup publisherPanelGroup) {
		this.publisherPanelGroup = publisherPanelGroup;
	}

	/**
	 * Does process request parameters. It is used to process 'uuid' parameter
	 * to fetch metadata details.
	 * 
	 * @return empty string
	 */
	@SuppressWarnings("unchecked")
	public String processRequestParams() {
		try {

			// start view preparation phase
			RequestContext context = onPrepareViewStarted();
			HttpServletRequest request = getContextBroker()
					.extractHttpServletRequest();
			Map parameterMap = request.getParameterMap();

			Object url = parameterMap.get("catalog");
			String catalogUrl = null;
			if (url instanceof String[] && ((String[]) url).length > 0) {
				// catalogUrl = url.toString().trim();
				catalogUrl = ((String[]) url)[0].trim();
			}

			if (parameterMap.containsKey("uuid")) {
				Object oUuid = parameterMap.get("uuid");
				if (oUuid instanceof String[] && ((String[]) oUuid).length > 0) {
					processSearchUuid(context, ((String[]) oUuid)[0],
							catalogUrl);
				}
			}

		} catch (Throwable t) {
			handleException(t);
		} finally {
			onPrepareViewCompleted();
		}

		return "";
	}

	private void processSearchUuid(RequestContext context, String uuid,
			String catalogUrl) throws SearchException, SchemaException,
			XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, Exception {

		if (publisherPanelGroup != null) {
			publisherPanelGroup.getChildren().clear();
		} else {
			publisherPanelGroup = new HtmlPanelGroup();
		}
		if (uuid == null || "".equals(uuid)) {
			throw new SearchException(
					"UUID given for document requested is either null or empty");
		}
		String getPublisherText = "TODO"; // this.getPublisherText(uuid);
		HtmlOutputText component = new HtmlOutputText();
		component.setId("xsltBasedDetails");
		component.setValue(getPublisherText);
		component.setEscape(false);
		publisherPanelGroup.getChildren().add(component);

		// always execute the search for metadata records
		executeSearch(context, uuid);
	}

	/**
	 * Executes a search for metadata records.
	 * 
	 * @param context
	 *            the context associated with the active request
	 * @param publisher
	 *            the publisher
	 * @throws Exception
	 *             if an exception occurs
	 */
	private void executeSearch(RequestContext context, String uuid)
			throws Exception {
		MessageBroker msgBroker = extractMessageBroker();
		LOG.info("here6");
		// execute the request
		ProvenanceQuery request;
		request = new ProvenanceQuery(context);
		request.execute(uuid);
		record = request.getRecords();
	}
}
