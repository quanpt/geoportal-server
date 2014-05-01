package com.esri.gpt.catalog.provenance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.esri.gpt.catalog.arcims.ImsMetadataAdminDao;
import com.esri.gpt.catalog.harvest.repository.HrRecord.HarvestFrequency;
import com.esri.gpt.catalog.management.MmdCriteria;
import com.esri.gpt.catalog.management.MmdQueryRequest;
import com.esri.gpt.catalog.management.MmdQueryResult;
import com.esri.gpt.catalog.management.MmdRecord;
import com.esri.gpt.catalog.management.MmdRecords;
import com.esri.gpt.catalog.management.MmdRequest;
import com.esri.gpt.catalog.management.MmdResult;
import com.esri.gpt.catalog.management.MmdEnums.PublicationMethod;
import com.esri.gpt.control.webharvest.protocol.ProtocolParseException;
import com.esri.gpt.framework.collection.StringAttributeMap;
import com.esri.gpt.framework.context.RequestContext;
import com.esri.gpt.framework.request.DaoRequest;
import com.esri.gpt.framework.request.RequestDefinition;
import com.esri.gpt.framework.security.identity.IdentityException;
import com.esri.gpt.framework.security.metadata.MetadataAcl;
import com.esri.gpt.framework.security.principal.Groups;
import com.esri.gpt.framework.security.principal.Publisher;
import com.esri.gpt.framework.sql.ManagedConnection;
import com.esri.gpt.framework.util.DateProxy;
import com.esri.gpt.framework.util.Val;

public class ProvenanceQuery extends MmdRequest {

	protected ProvenanceQuery(RequestContext requestContext) {
		super(requestContext, null, null, null);
	}

	// class variables
	// =============================================================
	private static final Logger LOGGER = Logger.getLogger(MmdQueryRequest.class
			.getCanonicalName());

	// instance variables
	// ==========================================================
	private ImsMetadataAdminDao adminDao;
	private Groups allGroups = null;
	private boolean enableEditForAllPubMethods = false;
	private boolean isGptAdministrator;
	private String tblImsUser;
	private MmdRecord record = new MmdRecord();

	public void execute(String uuid) throws SQLException, IdentityException,
			NamingException, ParserConfigurationException, SAXException,
			IOException {

		// intitalize
		PreparedStatement st = null;
		// PreparedStatement stCount = null;
		PreparedStatement stUser = null;
		// PreparedStatement stCol = null;
		// MmdQueryCriteria criteria = new MmdQueryCriteria();
		// MmdRecords records = getQueryResult().getRecords();
		// PageCursor pageCursor = getQueryResult().getPageCursor();
		// //criteria.getDateRange().check();
		// pageCursor.setTotalRecordCount(0);
		adminDao = new ImsMetadataAdminDao(getRequestContext());
		tblImsUser = getRequestContext().getCatalogConfiguration()
				.getUserTableName();
		// Users editablePublishers =
		// Publisher.buildSelectablePublishers(getRequestContext(), false);
		// for (User u : editablePublishers.values()) {
		// if (u.getName().length() > 0) {
		// hmEditablePublishers.put(u.getName().toLowerCase(), u.getKey());
		// }
		// }
		// User tmpUser = new User();
		// tmpUser.setDistinguishedName("*");
		// getRequestContext().newIdentityAdapter().readUserGroups(tmpUser);
		// allGroups = tmpUser.getGroups();

		isGptAdministrator = Boolean.TRUE; // ---

		try {

			// establish the connection
			ManagedConnection mc = returnConnection();
			Connection con = mc.getJdbcConnection();

			// determine if the database is case sensitive
			StringAttributeMap params = getRequestContext()
					.getCatalogConfiguration().getParameters();
			String s = Val.chkStr(params.getValue("database.isCaseSensitive"));
			boolean isDbCaseSensitive = !s.equalsIgnoreCase("false");

			s = Val.chkStr(params
					.getValue("catalog.enableEditForAllPubMethods"));
			this.enableEditForAllPubMethods = s.equalsIgnoreCase("true");

			// username query
			String sqlUser = "SELECT USERNAME FROM " + tblImsUser
					+ " WHERE USERID=?";

			// start the SQL expression
			StringBuilder sbSql = new StringBuilder();
			// StringBuilder sbCount = new StringBuilder();
			StringBuilder sbFrom = new StringBuilder();
			StringBuilder sbWhere = new StringBuilder();
			sbSql.append("SELECT A.TITLE,A.DOCUUID,A.SITEUUID,A.OWNER");
			sbSql.append(",A.APPROVALSTATUS,A.PUBMETHOD,A.UPDATEDATE,A.ACL");
			sbSql.append(",A.ID,A.HOST_URL,A.FREQUENCY,A.SEND_NOTIFICATION,A.PROTOCOL");
			sbSql.append(",A.FINDABLE,A.SEARCHABLE,A.SYNCHRONIZABLE");
			// sbCount.append("SELECT COUNT(*)");

			// append from clause
			sbFrom.append(" FROM ").append(getResourceTableName()).append(" A");
			sbSql.append(sbFrom);
			// sbCount.append(sbFrom);

			// build the where clause
			// Map<String,Object> args = criteria.appendWherePhrase(
			// getRequestContext(),"A",sbWhere,getPublisher());

			// append the where clause expressions
			sbWhere.append("A.DOCUUID = ?");
			if (sbWhere.length() > 0) {
				sbSql.append(" WHERE ").append(sbWhere.toString());
				// sbCount.append(" WHERE ").append(sbWhere.toString());
			}

			// prepare the statements
			st = con.prepareStatement(sbSql.toString());
			st.setString(1, uuid);
			// stCount = con.prepareStatement(sbCount.toString());
			stUser = con.prepareStatement(sqlUser);
			// int n = 1;
			// criteria.applyArgs(st, n, args);
			// n = criteria.applyArgs(stCount, n, args);

			// query the count
			// System.err.println(sbCount.toString());
			// System.err.println(sbSql.toString());
			// LOGGER.info(sbCount.toString());
			// logExpression(sbCount.toString());
			// ResultSet rsCount = stCount.executeQuery();
			// if (rsCount.next()) {
			// pageCursor.setTotalRecordCount(rsCount.getInt(1));
			// }
			// stCount.close();
			// stCount = null;

			// query records if a count was found
			// pageCursor.checkCurrentPage();
			// if (pageCursor.getTotalRecordCount() > 0) {

			// set the start record and the number of records to retrieve
			// int nCurPage = pageCursor.getCurrentPage();
			// int nRecsPerPage = getQueryResult().getPageCursor()
			// .getRecordsPerPage();
			// int nStartRecord = ((nCurPage - 1) * nRecsPerPage) + 1;
			// int nMaxRecsToRetrieve = nCurPage * nRecsPerPage;
			// st.setMaxRows(nMaxRecsToRetrieve);

			// determine publisher names associated with editable records

			// execute the query
			logExpression(sbSql.toString());
			ResultSet rs = st.executeQuery();

			// build the record set
			int nCounter = 0;

			while (rs.next()) {
				// n = 1;
				// nCounter++;
				// if (nCounter >= nStartRecord) {
				// MmdRecord record = new MmdRecord();
				// records.add(record);

				// find the username of the owner
				int nUserid = rs.getInt(4);
				String sUsername = "";
//				stUser.clearParameters();
				stUser.setInt(1, nUserid);
				ResultSet rs2 = stUser.executeQuery();
				if (rs2.next())
					sUsername = Val.chkStr(rs2.getString(1));
				if (sUsername.length() == 0)
					sUsername = "" + nUserid;
				rs2.close();

				try {
					readRecord(rs, record, sUsername);
				} catch (Exception ex) {
					LOGGER.log(Level.WARNING, "Error reading record.", ex);
				}

				// break if we hit the max value for the cursor
				// if (records.size() >= nRecsPerPage) {
				// break;
				// }
				//
				// }
			}

			// TreeMap<String, MmdRecord> recordsMap = new TreeMap<String,
			// MmdRecord>(
			// String.CASE_INSENSITIVE_ORDER);
			// StringBuilder keys = new StringBuilder();
			//
			// for (MmdRecord r : records) {
			// if (r.getProtocol() == null)
			// continue;
			// recordsMap.put(r.getUuid(), r);
			// if (keys.length() > 0) {
			// keys.append(",");
			// }
			// keys.append("'").append(r.getUuid().toUpperCase())
			// .append("'");
			// }
			//
			// readJobStatus(con, recordsMap, keys.toString());
			// readLastHarvestDate(con, recordsMap, keys.toString());
			// }

		} finally {
			closeStatement(st);
			// closeStatement(stCount);
			closeStatement(stUser);
			// closeStatement(stCol);
		}
	}

	private void readRecord(ResultSet rs, MmdRecord record, String ownername)
			throws SQLException, ParserConfigurationException, IOException,
			SAXException, ProtocolParseException {
		int n = 1;

		// set the title and uuid
		record.setTitle(rs.getString(n++));
		record.setUuid(rs.getString(n++));
		record.setSiteUuid(rs.getString(n++));
//		if (getActionCriteria().getSelectedRecordIdSet().contains(
//				record.getUuid())) {
//			record.setIsSelected(true);
//		}

		// set the owner, approval status and publication method
		// record.setOwnerName(rs.getString(n++));
		n++;
		record.setOwnerName(ownername);
		record.setApprovalStatus(rs.getString(n++));
		record.setPublicationMethod(rs.getString(n++));

		// set the update date,
		Timestamp ts = rs.getTimestamp(n++);
		if (ts != null) {
			record.setSystemUpdateDate(ts);
			record.setFormattedUpdateDate(DateProxy.formatDate(ts));
		}

		// set the ACL
		String aclXml = rs.getString(n++);
		if (aclXml != null && aclXml.trim().length() > 0) {
			record.setMetadataAccessPolicyType("Restricted");
			MetadataAcl acl = new MetadataAcl(getRequestContext());
			record.setCurrentMetadataAccessPolicy(acl.makeGroupsfromXml(
					allGroups, aclXml));
			record.setCurrentMetadataAccessPolicyKeys(acl
					.makeGroupsKeysfromXml(allGroups, aclXml));
		} else {
			record.setMetadataAccessPolicyType("Unrestricted");
			record.setCurrentMetadataAccessPolicy("Unrestricted");
			record.setCurrentMetadataAccessPolicyKeys("Unrestricted");
		}

		// set harvesting specific data
		record.setLocalId(rs.getInt(n++));
		record.setHostUrl(rs.getString(n++));
		String frequency = Val.chkStr(rs.getString(n++));
		if (frequency.length() > 0)
			record.setHarvestFrequency(HarvestFrequency.checkValueOf(frequency));
		record.setSendNotification(Val.chkBool(rs.getString(n++), false));
		String protocol = Val.chkStr(rs.getString(n++));
		if (protocol.length() > 0)
			record.setProtocol(getApplicationConfiguration()
					.getProtocolFactories().parseProtocol(protocol));

		// set the editable status
		boolean isEditor = record.getPublicationMethod().equalsIgnoreCase(
				PublicationMethod.editor.name());
		boolean isSEditor = record.getPublicationMethod().equalsIgnoreCase(
				PublicationMethod.seditor.name());

		boolean isProtocol = record.getProtocol() != null;
		// boolean isOwner = hmEditablePublishers.containsKey(record
		// .getOwnerName().toLowerCase());
		// record.setCanEdit((this.enableEditForAllPubMethods || isEditor
		// || isSEditor || isProtocol)
		// && (isOwner || (isProtocol && isGptAdministrator)));

		// TODO remove as this is a temporary fix
		boolean isOther = record.getPublicationMethod().equalsIgnoreCase(
				PublicationMethod.other.name());
		if (isOther && isProtocol) {
			record.setPublicationMethod(PublicationMethod.registration.name());
		}

		record.setFindable(Val.chkBool(rs.getString(n++), false));
		record.setSearchable(Val.chkBool(rs.getString(n++), false));
		record.setSynchronizable(Val.chkBool(rs.getString(n++), false));
	}

	public MmdRecord getRecords() {
		return record;
	}
}
