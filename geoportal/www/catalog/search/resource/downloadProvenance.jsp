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
<% // downloadProvenance.jsp - Download metadata page (JSP) %>
<%@page import="com.esri.gpt.catalog.provenance.ProvenanceQuery"%>
<%@page import="com.esri.gpt.framework.context.RequestContext"%>
<%@page import="com.esri.gpt.framework.util.LogUtil"%>
<%@page import="com.esri.gpt.framework.util.Val"%>
<%@page import="java.util.logging.Level"%>
<%
  String sUuid = Val.chkStr(request.getParameter("uuid"));
  String sXml = "";
  String sErr = "";
  
  // read the XML string associated with the UUID
  if (sUuid.length() > 0) {
    try {
      sXml = readXml(request,sUuid);
      if (sXml.length() == 0) {
        //sErr = "Download Failed.";
      }
    } catch (Exception e) {
      sErr = "Download Failed.";
      LogUtil.getLogger().log(Level.SEVERE,"Metadata download failed",e);
    }
  }
  
  // return the XML
  if ((sUuid.length() > 0) && (sXml.length() > 0) && (sErr.length() == 0)) {
      response.reset();
      response.setContentType("text/xml; charset=UTF-8");
      out.clear();
      out.print(sXml);
      out.flush();
      //out.close();
  // use javascript to alert the user if an error has occurred
  } else if (sErr.length() > 0) {
    out.println("<script type=\"text/javascript\" language=\"Javascript\">");
    out.println("alert(\""+sErr+"\");");
    out.println("</script>");
  }
%>

<%!

// read the XML string associated with the UUID
private String readXml(HttpServletRequest request, String uuid) throws Exception {
  String sXml = "";
  RequestContext context = null;
  try {
    context = RequestContext.extract(request);
    ProvenanceQuery prvDoc = new ProvenanceQuery(context);
    sXml = prvDoc.prepareForDownload(uuid);
    
    boolean bStripStyleSheets = true;
    if (bStripStyleSheets) {
      //sXml = sXml.replaceAll("<\\?xml\\-stylesheet.*\\?>|<\\!DOCTYPE.*>","");
      sXml = sXml.replaceAll("<\\?xml\\-stylesheet.+?>|<\\!DOCTYPE.+?>","");
    }
  } finally {
    if (context != null) {
      context.onExecutionPhaseCompleted();
    }
  }
  return sXml;
}

%>