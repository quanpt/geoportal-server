package com.esri.gpt.catalog.provenance;

import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.esri.gpt.framework.context.RequestContext;
import com.esri.gpt.framework.jsf.BaseActionListener;
import com.esri.gpt.framework.jsf.MessageBroker;

public class ProvenanceController extends BaseActionListener {

	private static final Logger LOG = Logger.getLogger(ProvenanceController.class
			.getCanonicalName());

	/** The search result. */
	private ProvenanceQuery query;

	public ProvenanceQuery getQuery() {
		return query;
	}

	/**
	 * Does process request parameters. It is used to process 'uuid' parameter
	 * to fetch metadata provenance.
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
					executeSearch(context, ((String[]) oUuid)[0]);
				}
			}

		} catch (Throwable t) {
			handleException(t);
		} finally {
			onPrepareViewCompleted();
		}

		return "";
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
		// execute the request
		
		query = new ProvenanceQuery(context);
		query.execute(uuid);
	}
}
