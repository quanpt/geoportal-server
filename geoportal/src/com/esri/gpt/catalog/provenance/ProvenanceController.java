package com.esri.gpt.catalog.provenance;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.esri.gpt.catalog.management.CollectionDao;
import com.esri.gpt.catalog.management.MmdActionCriteria;
import com.esri.gpt.catalog.management.MmdCriteria;
import com.esri.gpt.catalog.management.MmdQueryCriteria;
import com.esri.gpt.catalog.management.MmdQueryRequest;
import com.esri.gpt.catalog.management.MmdQueryResult;
import com.esri.gpt.catalog.management.MmdRecord;
import com.esri.gpt.catalog.management.MmdResult;
import com.esri.gpt.catalog.schema.MetadataDocument;
import com.esri.gpt.catalog.schema.Schema;
import com.esri.gpt.catalog.schema.SchemaException;
import com.esri.gpt.catalog.schema.UiContext;
import com.esri.gpt.catalog.search.ASearchEngine;
import com.esri.gpt.catalog.search.SearchCriteria;
import com.esri.gpt.catalog.search.SearchEngineFactory;
import com.esri.gpt.catalog.search.SearchEngineLocal;
import com.esri.gpt.catalog.search.SearchException;
import com.esri.gpt.catalog.search.SearchResult;
import com.esri.gpt.control.publication.SelectableCollections;
import com.esri.gpt.control.publication.SelectableGroups;
import com.esri.gpt.control.search.SearchController;
import com.esri.gpt.control.view.PageCursorPanel;
import com.esri.gpt.control.view.SelectablePublishers;
import com.esri.gpt.framework.context.RequestContext;
import com.esri.gpt.framework.jsf.BaseActionListener;
import com.esri.gpt.framework.jsf.FacesContextBroker;
import com.esri.gpt.framework.jsf.MessageBroker;
import com.esri.gpt.framework.security.identity.NotAuthorizedException;
import com.esri.gpt.framework.security.identity.local.SimpleIdentityAdapter;
import com.esri.gpt.framework.security.metadata.MetadataAccessPolicy;
import com.esri.gpt.framework.security.principal.Publisher;
import com.esri.gpt.framework.util.Val;

public class ProvenanceController extends BaseActionListener {

	private static final Logger LOG = Logger.getLogger(SearchController.class
			.getCanonicalName());

	// instance variables
	// ==========================================================
	private MmdCriteria _criteria;
	public MmdCriteria getCriteria() {
		return _criteria;
	}

	public void setCriteria(MmdCriteria _criteria) {
		this._criteria = _criteria;
	}

	private PageCursorPanel _pageCursorPanel;
	private MmdResult _result;

	public MmdResult getResult() {
		return _result;
	}

	public void setResult(MmdResult _result) {
		this._result = _result;
	}

	private SelectableCollections _selectableCollections;

	public SelectableCollections getSelectableCollections() {
		return _selectableCollections;
	}

	public void setSelectableCollections(
			SelectableCollections _selectableCollections) {
		this._selectableCollections = _selectableCollections;
	}

	private SelectablePublishers _selectablePublishers;
	private SelectableGroups _candidateGroups;
	private MetadataAccessPolicy _metadataAccessPolicyConfig;
	private MmdQueryCriteria _queryCriteriaForAction = new MmdQueryCriteria();
	private boolean _useCollections = false;

	/** The search result. */
	private SearchResult searchResult;

	public void setSearchResult(SearchResult searchResult) {
		this.searchResult = searchResult;
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
		
		Publisher publisher = new Publisher(context);

		// always execute the search for metadata records
		executeSearch(context, publisher);
	}

	/**
	 * Fired when the getPrepareView() property is accessed. <br/>
	 * This event is triggered from the page during the render response phase of
	 * the JSF cycle.
	 * <p>
	 * The UI components associated with the PageCursorPanel are build on the
	 * firing of this event.
	 * 
	 * @param context
	 *            the context associated with the active request
	 * @throws Exception
	 *             if an exception occurs
	 */
	@Override
	protected void onPrepareView(final RequestContext context) throws Exception {

		LOG.info("HERE");
	}

	/**
	 * Gets the query result.
	 * 
	 * @return the query result
	 */
	public MmdQueryResult getQueryResult() {
		return getResult().getQueryResult();
	}

	/**
	 * Gets the metadata text.
	 * 
	 * @param uuid
	 *            the uuid
	 * 
	 * @return the metadata text
	 * 
	 * @throws SearchException
	 *             the search exception
	 */
	public String getPublisherText(String uuid) throws SearchException {
		if (uuid == null || "".equals(uuid)) {
			throw new SearchException(
					"UUID given for document requested is either null"
							+ " or empty");
		}
		ASearchEngine dao = this.getSearchDao();
		return dao.getPublisherAsText(uuid);
	}

	/**
	 * Gets the search dao.
	 * 
	 * @return the search dao
	 * @throws SearchException
	 *             the search exception
	 */
	protected ASearchEngine getSearchDao() throws SearchException {
		ASearchEngine dao = SearchEngineFactory.createSearchEngine(
				new SearchCriteria(), this.getSearchResult(),
				this.extractRequestContext(), SearchEngineLocal.ID,
				(new FacesContextBroker()).extractMessageBroker());
		return dao;
	}

	/**
	 * Gets the search result.
	 * 
	 * @return the search result (never null)
	 */
	public SearchResult getSearchResult() {
		LOG.info("Here");
		if (searchResult == null) {
			this.setSearchResult(new SearchResult());
		}
		return this.searchResult;
	}

	/**
	 * Handles a metadata management action. <br/>
	 * This is the default entry point for a sub-class of BaseActionListener. <br/>
	 * This BaseActionListener handles the JSF processAction method and invokes
	 * the processSubAction method of the sub-class.
	 * 
	 * @param event
	 *            the associated JSF action event
	 * @param context
	 *            the context associated with the active request
	 * @throws AbortProcessingException
	 *             if processing should be aborted
	 * @throws Exception
	 *             if an exception occurs
	 */
	@Override
	protected void processSubAction(ActionEvent event, RequestContext context)
			throws AbortProcessingException, Exception {
		LOG.info("Here");
		// prepare the publisher
		Publisher publisher = new Publisher(context);

		// always execute the search for metadata records
		executeSearch(context, publisher);
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
	private void executeSearch(RequestContext context,
			Publisher publisher) throws Exception {
		MessageBroker msgBroker = extractMessageBroker();
		LOG.info("Here");
		// execute the request
		MmdQueryRequest request;
		request = new MmdQueryRequest(context, publisher, getCriteria(),
				getResult());
		request.execute();

		// set the resource messages for the results
		String sMsg;
		String sValue;
		for (MmdRecord record : request.getQueryResult().getRecords()) {

			// lookup approval status
			sValue = record.getApprovalStatus();
			sMsg = msgBroker
					.retrieveMessage("catalog.publication.manageMetadata.status."
							+ sValue);
			record.setApprovalStatusMsg(sMsg);

			// lookup publication method
			sValue = record.getPublicationMethod();
			sMsg = msgBroker
					.retrieveMessage("catalog.publication.manageMetadata.method."
							+ sValue);
			record.setPublicationMethodMsg(sMsg);
		}
	}
	
	/**
	 * Gets the query criteria.
	 * @return the query criteria
	 */
	public MmdQueryCriteria getQueryCriteria() {
	  return getCriteria().getQueryCriteria();
	}
}
