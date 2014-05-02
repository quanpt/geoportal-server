<%--
 See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 Esri Inc. licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
--%>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<%@ taglib prefix="gpt" uri="http://www.esri.com/tags-gpt"%>

<%
  String vmdUuid = request.getParameter("uuid");
  String sRestUrl = request.getContextPath()+"/rest/document?f=html&id="+java.net.URLEncoder.encode(vmdUuid,"UTF-8");
%>

<% // bind detail sections %>
<h:panelGrid id="Publisher" columns="2" border="0"
   cellpadding="4" cellspacing="4">
      <f:facet name="header">
         <h:outputText value="Publisher"/>
      </f:facet>
      <h:outputLabel value="Email" />
      <h:outputText value="" />
      <h:outputLabel value="Name" />
      <h:outputText value="#{ProvenanceController.query.record.ownerName}" />
      <h:outputLabel value="Organization" />
      <h:outputText value="" />
      <h:outputLabel value="Affiliation" />
      <h:outputText value="" />
</h:panelGrid>

<h:dataTable id="mmdRecords"
	value="#{ProvenanceController.query.ancesters}" var="record"
	cellspacing="2" cellpadding="10">
	<f:facet name="header">
		<h:outputText value="Repositories" />
	</f:facet>
	<h:column>
		<h:panelGrid id="Repository1" columns="2" border="0" cellpadding="4"
			cellspacing="4">
			<h:outputLabel value="Title" />
			<h:outputText value="#{record.title }" />
			<h:outputLabel value="File Identifier" />
			<h:outputText value="#{record.fileIdentifier }" />
			<h:outputLabel value="Source Uri" rendered="#{not empty record.sourceUri}"/>
			<h:outputLink value="#{record.sourceUri}" rendered="#{not empty record.sourceUri}">
				<h:outputText value="#{record.sourceUri}" />
			</h:outputLink>
			<h:outputLabel value="Host Url" rendered="#{not empty record.hostUrl}"/>
			<h:outputLink value="#{record.hostUrl}" rendered="#{not empty record.hostUrl}">
				<h:outputText value="#{record.hostUrl}" />
			</h:outputLink>
			<h:outputLabel value="UUID" />
			<h:outputLink>
				<f:param name="uuid" value="#{record.uuid}" />
				<h:outputText value="#{record.uuid}" />
			</h:outputLink>
		</h:panelGrid>
	</h:column>
</h:dataTable>

<% // button section %>
<f:verbatim>
  <iframe class="section" src="<%=sRestUrl%>" width="100%" scrolling="no" frameborder="0"></iframe>
  <span class="note"><%=sRestUrl%></span>
</f:verbatim>