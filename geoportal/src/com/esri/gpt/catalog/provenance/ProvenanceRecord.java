package com.esri.gpt.catalog.provenance;

import com.esri.gpt.catalog.management.MmdRecord;

public class ProvenanceRecord extends MmdRecord {
	private String _fileIdentifier = "";
	private String _sourceUri = "";
	
	// user info
	private String _displayName = "";
	private String _email = "";
	private String _org = "";
	private String _affi = "";

	public String getFileIdentifier() {
		return _fileIdentifier;
	}

	public void setFileIdentifier(String _fileIdentifier) {
		this._fileIdentifier = _fileIdentifier;
	}

	public String getSourceUri() {
		return _sourceUri;
	}

	public void setSourceUri(String _sourceUri) {
		this._sourceUri = _sourceUri;
	}

	public String getDisplayName() {
		return _displayName;
	}

	public void setDisplayName(String _displayName) {
		this._displayName = _displayName;
	}

	public String getEmail() {
		return _email;
	}

	public void setEmail(String _email) {
		this._email = _email;
	}

	public String getOrg() {
		return _org;
	}

	public void setOrg(String _org) {
		this._org = _org;
	}

	public String getAffi() {
		return _affi;
	}

	public void setAffi(String _affi) {
		this._affi = _affi;
	}
}
