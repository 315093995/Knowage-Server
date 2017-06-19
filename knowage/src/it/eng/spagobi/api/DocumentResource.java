/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.
 *
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eng.spagobi.api;

import it.eng.spago.error.EMFInternalError;
import it.eng.spago.error.EMFUserError;
import it.eng.spagobi.analiticalmodel.document.AnalyticalModelDocumentManagementAPI;
import it.eng.spagobi.analiticalmodel.document.bo.BIObject;
import it.eng.spagobi.analiticalmodel.document.bo.ObjTemplate;
import it.eng.spagobi.analiticalmodel.document.dao.IBIObjectDAO;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.bo.Parameter;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.dao.IParameterDAO;
import it.eng.spagobi.behaviouralmodel.lov.bo.ModalitiesValue;
import it.eng.spagobi.behaviouralmodel.lov.dao.IModalitiesValueDAO;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.commons.utilities.JSONTemplateUtilities;
import it.eng.spagobi.commons.utilities.ObjectsAccessVerifier;
import it.eng.spagobi.commons.utilities.UserUtilities;
import it.eng.spagobi.json.Xml;
import it.eng.spagobi.services.rest.annotations.ManageAuthorization;
import it.eng.spagobi.services.serialization.JsonConverter;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;
import it.eng.spagobi.utilities.exceptions.SpagoBIServiceException;
import it.eng.spagobi.utilities.rest.RestUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author Andrea Gioia (andrea.gioia@eng.it)
 *
 */
@Path("/1.0/documents")
@ManageAuthorization
public class DocumentResource extends AbstractSpagoBIResource {
	static protected Logger logger = Logger.getLogger(DocumentResource.class);

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public Response getDocuments(@QueryParam("inputType") String inputType) {
		logger.debug("IN");
		IBIObjectDAO documentsDao = null;
		List<BIObject> allObjects = null;
		List<BIObject> objects = null;

		try {
			documentsDao = DAOFactory.getBIObjectDAO();
			allObjects = documentsDao.loadAllBIObjects();

			UserProfile profile = getUserProfile();
			objects = new ArrayList<BIObject>();

			if (inputType != null && !inputType.isEmpty()) {
				for (BIObject obj : allObjects) {
					if (obj.getBiObjectTypeCode().equals(inputType) && ObjectsAccessVerifier.canSee(obj, profile))
						objects.add(obj);
				}
			} else {
				for (BIObject obj : allObjects) {
					if (ObjectsAccessVerifier.canSee(obj, profile))
						objects.add(obj);
				}
			}
			String toBeReturned = JsonConverter.objectToJson(objects, objects.getClass());

			return Response.ok(toBeReturned).build();
		} catch (Exception e) {
			logger.error("Error while getting the list of documents", e);
			throw new SpagoBIRuntimeException("Error while getting the list of documents", e);
		} finally {
			logger.debug("OUT");
		}
	}

	@POST
	@Path("/")
	@Consumes("application/json")
	public Response insertDocument(String body) {
		BIObject document = (BIObject) JsonConverter.jsonToValidObject(body, BIObject.class);

		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());

		document.setTenant(getUserProfile().getOrganization());
		document.setCreationUser((String) getUserProfile().getUserId());

		List<Integer> functionalities = document.getFunctionalities();
		for (Integer functionality : functionalities) {
			if (!ObjectsAccessVerifier.canDev(functionality, getUserProfile())) {
				String path = "";
				try {
					path = DAOFactory.getLowFunctionalityDAO().loadLowFunctionalityByID(functionality, false).getPath();
				} catch (EMFUserError e) {
					// Do nothing, the correct SpagoBIRuntimeException will be
					// throwed anyway. Only the path will be missing
				}

				throw new SpagoBIRuntimeException("User [" + getUserProfile().getUserName() + "] has no rights to create a document inside [" + path + "]");
			}
		}

		if (documentManager.isAnExistingDocument(document))
			throw new SpagoBIRuntimeException("The document already exists");

		documentManager.saveDocument(document, null);

		try {
			String encodedLabel = URLEncoder.encode(document.getLabel(), "UTF-8");
			encodedLabel = encodedLabel.replaceAll("\\+", "%20");
			return Response.created(new URI("1.0/documents/" + encodedLabel)).build();
		} catch (Exception e) {
			logger.error("Error while creating url of the new resource", e);
			throw new SpagoBIRuntimeException("Error while creating url of the new resource", e);
		}
	}

	@GET
	@Path("/{label}")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public Response getDocumentDetails(@PathParam("label") String label) {

		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());
		BIObject document = documentManager.getDocument(label);
		if (document == null)
			throw new SpagoBIRuntimeException("Document with label [" + label + "] doesn't exist");

		try {
			if (ObjectsAccessVerifier.canSee(document, getUserProfile())) {
				String toBeReturned = JsonConverter.objectToJson(document, BIObject.class);
				return Response.ok(toBeReturned).build();
			} else
				throw new SpagoBIRuntimeException("User [" + getUserProfile().getUserName() + "] has no rights to see document with label [" + label + "]");

		} catch (SpagoBIRuntimeException e) {
			throw e;
		} catch (EMFInternalError e) {
			logger.error("Error while looking for authorizations", e);
			throw new SpagoBIRuntimeException("Error while looking for authorizations", e);
		} catch (Exception e) {
			logger.error("Error while converting document in Json", e);
			throw new SpagoBIRuntimeException("Error while converting document in Json", e);
		}

	}

	@PUT
	@Path("/{label}")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public Response updateDocument(@PathParam("label") String label, String body) {
		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());
		BIObject oldDocument = documentManager.getDocument(label);
		if (oldDocument == null)
			throw new SpagoBIRuntimeException("Document with label [" + label + "] doesn't exist");

		Integer id = oldDocument.getId();
		if (!ObjectsAccessVerifier.canDevBIObject(id, getUserProfile()))
			throw new SpagoBIRuntimeException("User [" + getUserProfile().getUserName() + "] has no rights to update document with label [" + label + "]");

		BIObject document = (BIObject) JsonConverter.jsonToValidObject(body, BIObject.class);

		document.setLabel(label);
		document.setId(id);
		documentManager.saveDocument(document, null);
		return Response.ok().build();
	}

	@DELETE
	@Path("/{label}")
	public Response deleteDocument(@PathParam("label") String label) {
		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());
		BIObject document = documentManager.getDocument(label);
		if (document == null)
			throw new SpagoBIRuntimeException("Document with label [" + label + "] doesn't exist");

		if (ObjectsAccessVerifier.canDeleteBIObject(document.getId(), getUserProfile())) {
			try {
				DAOFactory.getBIObjectDAO().eraseBIObject(document, null);
			} catch (EMFUserError e) {
				logger.error("Error while deleting the specified document", e);
				throw new SpagoBIRuntimeException("Error while deleting the specified document", e);
			}
			return Response.ok().build();
		} else
			throw new SpagoBIRuntimeException("User [" + getUserProfile().getUserName() + "] has no rights to delete document with label [" + label + "]");
	}

	@GET
	@Path("/{label}/template")
	public Response getDocumentTemplate(@PathParam("label") String label) {
		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());
		BIObject document = documentManager.getDocument(label);
		if (document == null)
			throw new SpagoBIRuntimeException("Document with label [" + label + "] doesn't exist");

		if (!ObjectsAccessVerifier.canDevBIObject(document, getUserProfile()))
			throw new SpagoBIRuntimeException("User [" + getUserProfile().getUserName() + "] has no rights to see template of document with label [" + label
					+ "]");

		ResponseBuilder rb;
		ObjTemplate template = document.getActiveTemplate();

		// The template has not been found
		if (template == null)
			throw new SpagoBIRuntimeException("Document with label [" + label + "] doesn't contain a template");
		try {
			rb = Response.ok(template.getContent());
		} catch (Exception e) {
			logger.error("Error while getting document template", e);
			throw new SpagoBIRuntimeException("Error while getting document template", e);
		}

		rb.header("Content-Disposition", "attachment; filename=" + document.getActiveTemplate().getName());
		return rb.build();
	}

	@GET
	@Path("/{label}/usertemplate")
	public Response getDocumentTemplateCheckUser(@PathParam("label") String label) {
		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());
		BIObject document = documentManager.getDocument(label);
		if (document == null)
			throw new SpagoBIRuntimeException("Document with label [" + label + "] doesn't exist");

		// check if owner of document or administrator

		if (!document.getTenant().equals(getUserProfile().getOrganization())
				|| (!UserUtilities.isAdministrator(getUserProfile()) && !document.getCreationUser().equals(getUserProfile().getUserId()))) {
			throw new SpagoBIRuntimeException("User [" + getUserProfile().getUserName() + "] has no rights to see template of document with label [" + label
					+ "]");
		}

		ResponseBuilder rb;
		ObjTemplate template = document.getActiveTemplate();

		// The template has not been found
		if (template == null)
			throw new SpagoBIRuntimeException("Document with label [" + label + "] doesn't contain a template");
		try {
			rb = Response.ok(template.getContent());
		} catch (Exception e) {
			logger.error("Error while getting document template", e);
			throw new SpagoBIRuntimeException("Error while getting document template", e);
		}

		rb.header("Content-Disposition", "attachment; filename=" + document.getActiveTemplate().getName());
		return rb.build();
	}

	// The file has to be put in a field called "file"
	@POST
	@Path("/{label}/template")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addDocumentTemplate(@PathParam("label") String label, MultipartFormDataInput input) {
		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());
		BIObject document = documentManager.getDocument(label);
		if (document == null)
			throw new SpagoBIRuntimeException("Document with label [" + label + "] doesn't exist");

		if (!ObjectsAccessVerifier.canDevBIObject(document, getUserProfile()))
			throw new SpagoBIRuntimeException("User [" + getUserProfile().getUserName() + "] has no rights to manage the template of document with label ["
					+ label + "]");

		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		List<InputPart> inputParts = uploadForm.get("file");

		if (inputParts == null)
			return Response.status(Response.Status.BAD_REQUEST).build();

		for (InputPart inputPart : inputParts) {
			try {
				MultivaluedMap<String, String> header = inputPart.getHeaders();

				// convert the uploaded file to inputstream
				InputStream inputStream = inputPart.getBody(InputStream.class, null);

				byte[] content = IOUtils.toByteArray(inputStream);

				ObjTemplate template = new ObjTemplate();
				template.setContent(content);
				template.setName(getFileName(header));

				documentManager.saveDocument(document, template);

				return Response.ok().build();
			} catch (SpagoBIRuntimeException e) {
				throw e;
			} catch (Exception e) {
				logger.error("Error while getting the template", e);
				throw new SpagoBIRuntimeException("Error while getting the template", e);
			}
		}

		throw new SpagoBIRuntimeException("Template file not found inside the request");
	}

	@GET
	@Path("/{label}/meta")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public String getDocumentMeta(@PathParam("label") String label) {
		// TODO
		return null;
	}

	@GET
	@Path("/{label}/parameters")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public String getDocumentParameters(@PathParam("label") String label) {
		logger.debug("IN");
		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());
		try {
			List<JSONObject> parameters = documentManager.getDocumentParameters(label);
			JSONArray paramsJSON = writeParameters(parameters);
			JSONObject resultsJSON = new JSONObject();
			resultsJSON.put("results", paramsJSON);
			return resultsJSON.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", t);
		} finally {
			logger.debug("OUT");
		}
	}

	@GET
	@Path("/{label}/analyticalDrivers")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public Response getDocumentAD(@PathParam("label") String label) {
		logger.debug("IN");

		IParameterDAO driversDao = null;
		List<Parameter> fullList = null;

		try {

			driversDao = DAOFactory.getParameterDAO();
			driversDao.setUserProfile(getUserProfile());
			fullList = driversDao.loadParametersByBIObjectLabel(label);
			return Response.ok(fullList).build();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", t);
		} finally {
			logger.debug("OUT");
		}
	}

	@GET
	@Path("/{label}/lovs")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public Response getDocumentLovs(@PathParam("label") String label) {
		logger.debug("IN");

		List<ModalitiesValue> modalitiesValues = null;
		IModalitiesValueDAO modalitiesValueDAO = null;

		try {

			modalitiesValueDAO = DAOFactory.getModalitiesValueDAO();
			modalitiesValueDAO.setUserProfile(getUserProfile());
			modalitiesValues = modalitiesValueDAO.loadModalitiesValueByBIObjectLabel(label);

			return Response.ok(modalitiesValues).build();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", t);
		} finally {
			logger.debug("OUT");
		}
	}

	@POST
	@Path("/{docID}/saveOlapTemplate")
	@Produces(MediaType.TEXT_XML)
	@Consumes(MediaType.APPLICATION_JSON)
	public String saveOlapTemplate(@PathParam("docID") String docId) {
		String xml = null;
		JSONObject json;

		try {
			json = RestUtilities.readBodyAsJSONObject(getServletRequest());
			xml = JSONTemplateUtilities.convertJsonToXML(json);

		} catch (JSONException e) {
			String errorMessage = e.getMessage().replace(": Couldn't read request body", "");
			throw new SpagoBIRuntimeException(errorMessage);
		} catch (IOException e) {
			String errorMessage = e.getMessage().replace(": Couldn't read request body", "");
			throw new SpagoBIRuntimeException(errorMessage);
		} catch (ParserConfigurationException e) {
			String errorMessage = e.getMessage().replace(": Error while parsing json to xml", "");
			throw new SpagoBIRuntimeException(errorMessage);
		}

		saveOlapTemplate(docId, xml, json);

		return xml;
	}

	@SuppressWarnings("unused")
	@Path("/saveGeoReportTemplate")
	@POST
	public Response saveTemplate(@Context HttpServletRequest req) throws IOException, JSONException {
		JSONObject jsonData = RestUtilities.readBodyAsJSONObject(req);
		JSONObject geoTemplate = jsonData.getJSONObject("TEMPLATE");
		String layerLabel = jsonData.getString("DOCUMENT_LABEL");

		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());

		ObjTemplate template = new ObjTemplate();
		template.setName(layerLabel + "_Template.json");
		template.setContent(geoTemplate.toString().getBytes());
		template.setDimension(Long.toString(geoTemplate.toString().getBytes().length / 1000) + " KByte");
		try {
			IBIObjectDAO biObjectDao;
			BIObject document;
			biObjectDao = DAOFactory.getBIObjectDAO();
			document = biObjectDao.loadBIObjectByLabel(layerLabel);
			documentManager.saveDocument(document, template);
		} catch (EMFUserError e) {
			logger.error("Error saving JSON Template ...", e);
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", e);
		}
		return Response.ok().build();
	}

	@POST
	@Path("/saveChartTemplate")
	@Produces(MediaType.APPLICATION_JSON)
	public String saveTemplate(@QueryParam("jsonTemplate") String jsonTemplate, @QueryParam("docLabel") String docLabel,
			@Context HttpServletResponse servletResponse) {
		String xml = null;
		try {
			JSONObject json = new JSONObject(jsonTemplate);

			xml = JSONTemplateUtilities.convertJsonToXML(json);

		} catch (Exception e) {
			logger.error("Error converting JSON Template to XML...", e);
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", e);

		}

		saveTemplate(docLabel, xml);

		return xml;
	}

	@POST
	@Path("/saveKpiTemplate")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String saveKpiTemplate(@FormParam("jsonTemplate") String jsonTemplate, @FormParam("docLabel") String docLabel,
			@Context HttpServletResponse servletResponse) {
		String xml = null;
		try {
			JSONObject json = new JSONObject(jsonTemplate);

			xml = JSONTemplateUtilities.convertJsonToXML(json);

		} catch (Exception e) {
			logger.error("Error converting JSON Template to XML...", e);
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", e);

		}

		saveTemplate(docLabel, xml);

		return xml;
	}

	public JSONArray writeParameters(List<JSONObject> params) throws Exception {
		JSONArray paramsJSON = new JSONArray();

		for (Iterator iterator = params.iterator(); iterator.hasNext();) {
			JSONObject jsonObject = (JSONObject) iterator.next();
			paramsJSON.put(jsonObject);
		}

		return paramsJSON;
	}

	// ===================================================================
	// UTILITY METHODS
	// ===================================================================

	private void saveOlapTemplate(String docLabel, String xml, JSONObject json) {

		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());

		ObjTemplate template = new ObjTemplate();
		template.setName("Template.xml");
		template.setContent(xml.getBytes());
		template.setDimension(Long.toString(xml.getBytes().length / 1000) + " KByte");

		ArrayList<String> categoriesNames = new ArrayList<String>();

		/**
		 * Handles OLAP template cross-navigation parameters.
		 *
		 * @author Nikola Simovic (nsimovic, nikola.simovic@mht.net)
		 */
		try {
			JSONObject olapJSONObject = new JSONObject();
			JSONArray jaCategories = new JSONArray();
			olapJSONObject = json;

			JSONObject crossNavFromCellSingle = new JSONObject();
			JSONArray crossNavFromCellMulti = new JSONArray();

			JSONObject crossNavFromMemberSingle = new JSONObject();
			JSONArray crossNavFromMemberMulti = new JSONArray();

			if (olapJSONObject.optJSONObject("CROSS_NAVIGATION") != null) {

				crossNavFromCellSingle = olapJSONObject.optJSONObject("CROSS_NAVIGATION").optJSONObject("PARAMETERS").optJSONObject("PARAMETER");
				crossNavFromCellMulti = olapJSONObject.optJSONObject("CROSS_NAVIGATION").optJSONObject("PARAMETERS").optJSONArray("PARAMETER");

				if (crossNavFromCellMulti == null) {
					if (crossNavFromCellSingle != null) {
						jaCategories.put(crossNavFromCellSingle);
					}
				} else if (crossNavFromCellMulti != null) {
					for (int i = 0; i < crossNavFromCellMulti.length(); i++) {
						JSONObject joT = (JSONObject) crossNavFromCellMulti.get(i);
						jaCategories.put(joT);
					}
				}
			}

			if (olapJSONObject.optJSONObject("MDXQUERY") != null) {

				crossNavFromMemberSingle = olapJSONObject.optJSONObject("MDXQUERY").optJSONObject("clickable");
				crossNavFromMemberMulti = olapJSONObject.optJSONObject("MDXQUERY").optJSONArray("clickable");

				if (crossNavFromMemberMulti == null) {
					if (crossNavFromMemberSingle != null) {
						jaCategories.put(crossNavFromMemberSingle.opt("clickParameter"));
					}
				} else if (crossNavFromMemberMulti != null) {
					for (int i = 0; i < crossNavFromMemberMulti.length(); i++) {
						JSONObject joT = (JSONObject) crossNavFromMemberMulti.get(i);
						jaCategories.put(joT.opt("clickParameter"));
					}
				}
			}

			for (int i = 0; i < jaCategories.length(); i++) {
				JSONObject joT = (JSONObject) jaCategories.get(i);
				categoriesNames.add((String) joT.opt("name"));
			}
			logger.info("Category names for the OLAP document are: " + categoriesNames);
		} catch (JSONException e) {
			logger.error("Cannot get OLAP values from JSON object", e);
			throw new SpagoBIServiceException("Cannot get OLAP values from JSON object", e);
		}

		try {
			IBIObjectDAO biObjectDao;
			BIObject document;
			biObjectDao = DAOFactory.getBIObjectDAO();
			if (docLabel instanceof String) {
				document = biObjectDao.loadBIObjectByLabel(docLabel);
			} else {
				document = biObjectDao.loadBIObjectById(new Integer(docLabel));
			}

			documentManager.saveDocument(document, template);

		} catch (EMFUserError e) {
			logger.error("Error saving JSON Template to XML...", e);
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", e);
		}
	}

	private void saveTemplate(String docLabel, String xml) {
		AnalyticalModelDocumentManagementAPI documentManager = new AnalyticalModelDocumentManagementAPI(getUserProfile());

		ObjTemplate template = new ObjTemplate();
		template.setName("Template.xml");
		template.setContent(xml.getBytes());
		template.setDimension(Long.toString(xml.getBytes().length / 1000) + " KByte");

		ArrayList<String> categoriesNames = new ArrayList<String>();

		/**
		 * 'allSpecificChartTypes': Array of all chart types that need some default (generic) output parameters to be removed from the list of final output
		 * parameters for the document of that chart type. For example, the WORDCLOUD chart type does not need a GROUPING_NAME and GROUPING_VALUE output
		 * parameters, so these two will be removed from the predefined (standard) list of output parameters (it will have only SERIE_NAME, SERIE_VALUE,
		 * CATEGORY_NAME, CATEGORY_VALUE parameters).
		 *
		 * 'specificChartType': If the type of the chart document that is saved is one of those in the following list, we will record it and manage further
		 * functions accordingly.
		 *
		 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
		 */
		String[] allSpecificChartTypes = { "PARALLEL", "WORDCLOUD", "CHORD", "GAUGE" };
		List specificChartTypes = Arrays.asList(allSpecificChartTypes);
		String specificChartType = "";

		/**
		 * Two exclusive scenarios:
		 *
		 * (1) Prepare categories that the SUNBURST chart document has in order to provide custom-made category output parameters for the cross-navigation.
		 *
		 * (2) Get the type of the chart document that is about to be saved in order to manage its output parameters if its type is one of those listed in the
		 * 'specificChartTypes'.
		 *
		 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
		 */
		try {

			JSONObject obj = new JSONObject(Xml.xml2json(xml));

			Iterator keys = obj.keys();

			JSONArray jaCategories = new JSONArray();

			while (keys.hasNext()) {

				String key = (String) keys.next();

				if (key.equals("CHART") || key.equals("VALUES")) {

					obj = (JSONObject) obj.opt(key);
					keys = obj.keys();

					// Use this while loop only if the chart type of the
					// document is SUNBURST or one of those special chart types.
					// (danristo)
					if (key.equals("CHART")) {

						// If the type of the chart document that is about to be
						// saved amongst those listed above (special cases).
						// (danristo)
						if (specificChartTypes.indexOf(obj.opt("type").toString()) >= 0) {
							// Get that specific type of the chart document that
							// is in process of saving. (danristo)
							specificChartType = (String) specificChartTypes.get(specificChartTypes.indexOf(obj.opt("type").toString()));
							break;
						} else if (!obj.opt("type").toString().equals("SUNBURST")) {
							break;
						}

					}

				}

				else if (key.equals("CATEGORY")) {

					jaCategories = obj.optJSONArray(key);

					for (int i = 0; i < jaCategories.length(); i++) {
						JSONObject joT = (JSONObject) jaCategories.get(i);
						categoriesNames.add((String) joT.opt("column"));
					}

					logger.info("Category names for the SUNBURST document are: " + categoriesNames);

				}
			}

		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TransformerFactoryConfigurationError e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TransformerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			IBIObjectDAO biObjectDao;
			BIObject document;
			biObjectDao = DAOFactory.getBIObjectDAO();
			try {
				int docId = Integer.parseInt(docLabel);
				document = biObjectDao.loadBIObjectById(docId);
				if (document == null) {
					// This is wrong. We should use only one type of identifier!!!
					logger.debug("The document identifier is an Integer, but no document is found with such identifier. Trying with with it as a String.");
					document = biObjectDao.loadBIObjectByLabel(docLabel);
				}
			} catch (NumberFormatException e) {
				logger.debug("The document identifier is not an Integer.");
				document = biObjectDao.loadBIObjectByLabel(docLabel);
			}

			Assert.assertNotNull(document, "Document identifier or label cannot be null");

			// In the case of the SUNBURST and OLAP document type, this
			// variable will be not empty. (danristo) (nsimovic)
			if (!categoriesNames.isEmpty()) {
				documentManager.saveDocument(document, template, categoriesNames);
			}
			// If the type of the chart document is amongst those listed on the
			// beginning of the method. (danristo)
			else if (!specificChartType.equals("")) {
				documentManager.saveDocument(document, template, specificChartType);
			} else {
				documentManager.saveDocument(document, template);
			}

		} catch (EMFUserError e) {
			logger.error("Error saving JSON Template to XML...", e);
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", e);
		}
	}

	private String getFileName(MultivaluedMap<String, String> multivaluedMap) {

		String[] contentDisposition = multivaluedMap.getFirst("Content-Disposition").split(";");

		for (String filename : contentDisposition) {

			if ((filename.trim().startsWith("filename"))) {
				String[] name = filename.split("=");
				String exactFileName = name[1].trim().replaceAll("\"", "");
				return exactFileName;
			}
		}
		return "";
	}
}
