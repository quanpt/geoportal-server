package com.esri.gpt.catalog.provenance;

import com.esri.gpt.catalog.management.MmdRecord;

public class ProvenanceRecord extends MmdRecord {
	private String _fileIdentifier = "";
	private String _sourceUri = "";

	public String getSourceUri() {
		return _sourceUri;
	}

	public void setSourceUri(String _sourceUri) {
		this._sourceUri = _sourceUri;
	}

	public String getFileIdentifier() {
		return _fileIdentifier;
	}

	public void setFileIdentifier(String _fileIdentifier) {
		this._fileIdentifier = _fileIdentifier;
	}
}
