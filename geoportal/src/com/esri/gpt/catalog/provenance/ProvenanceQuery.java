package com.esri.gpt.catalog.provenance;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.esri.gpt.catalog.harvest.repository.HrRecord.HarvestFrequency;
import com.esri.gpt.catalog.management.MmdEnums.PublicationMethod;
import com.esri.gpt.catalog.management.MmdRecord;
import com.esri.gpt.catalog.management.MmdRequest;
import com.esri.gpt.control.webharvest.protocol.ProtocolParseException;
import com.esri.gpt.framework.collection.StringAttributeMap;
import com.esri.gpt.framework.context.RequestContext;
import com.esri.gpt.framework.security.credentials.CredentialsDeniedException;
import com.esri.gpt.framework.security.credentials.DistinguishedNameCredential;
import com.esri.gpt.framework.security.credentials.UsernameCredential;
import com.esri.gpt.framework.security.identity.IdentityAdapter;
import com.esri.gpt.framework.security.identity.IdentityConfiguration;
import com.esri.gpt.framework.security.identity.IdentityException;
import com.esri.gpt.framework.security.identity.ldap.LdapConfiguration;
import com.esri.gpt.framework.security.identity.ldap.LdapIdentityAdapter;
import com.esri.gpt.framework.security.metadata.MetadataAcl;
import com.esri.gpt.framework.security.principal.Groups;
import com.esri.gpt.framework.security.principal.User;
import com.esri.gpt.framework.security.principal.UserAttributeMap;
import com.esri.gpt.framework.sql.ManagedConnection;
import com.esri.gpt.framework.util.DateProxy;
import com.esri.gpt.framework.util.Val;

public class ProvenanceQuery extends MmdRequest {
	
	private RequestContext context;
	private Connection con;
	
	protected ProvenanceQuery(RequestContext requestContext) {
		super(requestContext, null, null, null);
		this.context = requestContext;
	}

	// class variables
	// =============================================================
	private static final Logger LOGGER = Logger.getLogger(ProvenanceQuery.class
			.getCanonicalName());

	private String userDIT = "ou=users,ou=system";
	private Groups allGroups = null;
	private String tblImsUser;
	private ProvenanceRecord record;
	private ArrayList<ProvenanceRecord> ancesters;
	private ArrayList<ProvenanceRecord> children;

	public ArrayList<ProvenanceRecord> getAncesters() {
		return ancesters;
	}

	public void setAncesters(ArrayList<ProvenanceRecord> ancesters) {
		this.ancesters = ancesters;
	}

	/**
	 * @return the children
	 */
	public ArrayList<ProvenanceRecord> getChildren() {
		return children;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(ArrayList<ProvenanceRecord> children) {
		this.children = children;
	}

	public void execute(String uuid) throws SQLException, IdentityException,
			NamingException, ParserConfigurationException, SAXException,
			IOException {

		// === prepare GLOBAL variables ===
		tblImsUser = getRequestContext().getCatalogConfiguration()
				.getUserTableName();
		// establish the connection
		ManagedConnection mc = returnConnection();
		con = mc.getJdbcConnection();
		// === done preparation ===

		// start the SQL expression
		StringBuilder sbSql = new StringBuilder();
		sbSql.append("SELECT A.TITLE,A.DOCUUID,A.SITEUUID,A.OWNER");
		sbSql.append(",A.APPROVALSTATUS,A.PUBMETHOD,A.UPDATEDATE,A.ACL");
		sbSql.append(",A.ID,A.HOST_URL,A.FREQUENCY,A.SEND_NOTIFICATION,A.PROTOCOL");
		sbSql.append(",A.FINDABLE,A.SEARCHABLE,A.SYNCHRONIZABLE");
		sbSql.append(",A.FILEIDENTIFIER, A.SOURCEURI");
		sbSql.append(" FROM ").append(getResourceTableName()).append(" A");
		
		String ancesterSql = sbSql.toString() + " WHERE A.DOCUUID = ?";
		String childrenSql = sbSql.toString() + " WHERE A.SITEUUID = ?";

		readAncesters(ancesterSql, uuid);
		readChildren(childrenSql, uuid);
		readUserInfo(record);
	}

	private void readChildren(String childrenSql, String uuid) throws SQLException {
		ProvenanceRecord tmpRecord = null;
		PreparedStatement st = null;
		int n = 0;
		// re-initialize this array
		children = new ArrayList<ProvenanceRecord>();
		
		// prepare the statements
		st = con.prepareStatement(childrenSql);
		st.setString(1, uuid);
		
		// execute the query
		logExpression(childrenSql);
		ResultSet rs = st.executeQuery();

		while (rs.next() && n < 5) {
			tmpRecord = new ProvenanceRecord();

			try {
				readRecord(rs, tmpRecord);
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "Error reading record.", ex);
			}
			children.add(tmpRecord);
			n++;
		}
		rs.close();
		closeStatement(st);
	}

	private void readAncesters(String ancesterSql, String uuid) throws SQLException {
		// intitalize
		ProvenanceRecord tmpRecord = null;
		PreparedStatement st = null;
		int n=0;
		// re-initialize this array
		ancesters = new ArrayList<ProvenanceRecord>();
		
		while (uuid != null && uuid != "" && n < 10) {
			// prepare the statements
			st = con.prepareStatement(ancesterSql);
			st.setString(1, uuid);
			
			// execute the query
			logExpression(ancesterSql);
			ResultSet rs = st.executeQuery();

			if (rs.next()) {
				tmpRecord = new ProvenanceRecord();

				try {
					readRecord(rs, tmpRecord);
				} catch (Exception ex) {
					LOGGER.log(Level.WARNING, "Error reading record.", ex);
				}
				
				if (n==0) {
					record = tmpRecord;
				} else
					ancesters.add(tmpRecord);
				n++;
				uuid = tmpRecord.getSiteUuid();
				rs.close();
				closeStatement(st);
			} else {
				closeStatement(st);
				break;
			}
		}
	}

	private void readRecord(ResultSet rs, ProvenanceRecord record)
			throws SQLException, ParserConfigurationException, IOException,
			SAXException, ProtocolParseException {
		int n = 1;

		// set the title and uuid
		record.setTitle(rs.getString(n++));
		record.setUuid(rs.getString(n++));
		record.setSiteUuid(rs.getString(n++));
		
		// set the owner, approval status and publication method
		record.setUserId(rs.getInt(n++));
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

		boolean isProtocol = record.getProtocol() != null;

		// TODO remove as this is a temporary fix
		boolean isOther = record.getPublicationMethod().equalsIgnoreCase(
				PublicationMethod.other.name());
		if (isOther && isProtocol) {
			record.setPublicationMethod(PublicationMethod.registration.name());
		}

		record.setFindable(Val.chkBool(rs.getString(n++), false));
		record.setSearchable(Val.chkBool(rs.getString(n++), false));
		record.setSynchronizable(Val.chkBool(rs.getString(n++), false));
		
		record.setFileIdentifier(rs.getString(n++));
		record.setSourceUri(rs.getString(n++));
	}

	public ProvenanceRecord getRecord() {
		return record;
	}

	private void readUserInfo(ProvenanceRecord aRecord) throws SQLException {
		int nUserid = record.getUserId();
		String sUserDN = "", sUsername = "";
		PreparedStatement stUser = null;
		stUser = con.prepareStatement("SELECT USERNAME, DN FROM " + tblImsUser
				+ " WHERE USERID=?");
		stUser.setInt(1, nUserid);
		ResultSet rs2 = stUser.executeQuery();
		if (rs2.next()) {
			sUsername = Val.chkStr(rs2.getString(1));
			sUserDN = Val.chkStr(rs2.getString(2));
			if (sUsername.length() == 0)
				sUsername = "" + nUserid;
			try {
				User user = readUserLdapProfile(sUserDN);
				UserAttributeMap profile = user.getProfile();
				aRecord.setOwnerName(sUsername);
				aRecord.setEmail(profile.getEmailAddress());
				aRecord.setDisplayName(profile.getValue("displayName"));
				aRecord.setAffi(profile.getValue("affiliation"));
				aRecord.setOrg(profile.getValue("organization"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		rs2.close();
		closeStatement(stUser);
	}
	
	/**
	 * Reads user profile from ldap.
	 * @return user the user whose profile was read
	 * @throws IdentityException if a system error occurs preventing the action
	 * @throws NamingException if an LDAP naming exception occurs
	 * @throws SQLException if a database communication exception occurs
	 * @throws CredentialsDeniedException 
	 * @throws UnsupportedEncodingException 
	 */
	protected User readUserLdapProfile(String sUserDN) throws Exception {
		
		IdentityAdapter idAdapter = this.context.newIdentityAdapter();
		IdentityConfiguration idConfig = this.context.getIdentityConfiguration();
		User user = new User();
		
		if(idConfig != null){
			LdapConfiguration ldapConfig = idConfig.getLdapConfiguration();
			if(ldapConfig != null){
			    userDIT = ldapConfig.getUserProperties().getUserSearchDIT();
			}
		}

		if(sUserDN != null && sUserDN != "") {
			String userIdentifier = sUserDN;
			if(userIdentifier.endsWith(userDIT)){
				user.setDistinguishedName(userIdentifier);
				DistinguishedNameCredential dnCredential = new DistinguishedNameCredential();
				dnCredential.setDistinguishedName(userIdentifier);
				user.setCredentials(dnCredential);
			}else if(userIdentifier.length() > 0) {
				user.setCredentials(new UsernameCredential(userIdentifier));
			}
			((LdapIdentityAdapter)idAdapter).populateUser(context, user);
			return user;
		}else{		
			throw new Exception("error");	
		}
	}
}
