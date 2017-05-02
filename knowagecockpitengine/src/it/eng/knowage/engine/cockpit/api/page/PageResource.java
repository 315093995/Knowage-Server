/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eng.knowage.engine.cockpit.api.page;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.eng.knowage.engine.cockpit.CockpitEngine;
import it.eng.knowage.engine.cockpit.CockpitEngineInstance;
import it.eng.knowage.engine.cockpit.api.AbstractCockpitEngineResource;
import it.eng.spagobi.utilities.engines.EngineConstants;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceExceptionHandler;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;

/**
 * @authors Andrea Gioia (andrea.gioia@eng.it)
 *
 */

@Path("/1.0/pages")
public class PageResource extends AbstractCockpitEngineResource {

	static private Map<String, JSONObject> pages;
	static private Map<String, String> urls;

	static private Logger logger = Logger.getLogger(PageResource.class);

	static {
		pages = new HashMap<String, JSONObject>();
		urls = new HashMap<String, String>();

		try {
			pages.put("edit", new JSONObject("{name: 'execute', description: 'the cockpit edit page', parameters: []}"));
			urls.put("edit", "/WEB-INF/jsp/ngCockpit.jsp");
			pages.put("execute", new JSONObject("{name: 'execute', description: 'the cockpit execution page', parameters: ['template']}"));
			urls.put("execute", "/WEB-INF/jsp/ngCockpit.jsp");
			pages.put("test", new JSONObject("{name: 'test', description: 'the cockpit test page', parameters: ['template']}"));
			urls.put("execute", "/WEB-INF/jsp/test4.jsp");
		} catch (JSONException t) {
			logger.error(t);
		}
	}

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public String getDataSets() {
		try {
			JSONArray resultsJSON = new JSONArray();
			Iterator<String> it = pages.keySet().iterator();
			while (it.hasNext()) {
				String pageName = it.next();
				resultsJSON.put(pages.get(pageName));
			}

			return resultsJSON.toString();
		} catch (Exception e) {
			throw SpagoBIEngineServiceExceptionHandler.getInstance().getWrappedException("", getEngineInstance(), e);
		} finally {
			logger.debug("OUT");
		}
	}

	@GET
	@Path("/{pagename}")
	@Produces("text/html")
	public void openPage(@PathParam("pagename") String pageName, @QueryParam("extjs") @DefaultValue("4") String extjs) {
		CockpitEngineInstance engineInstance;
		String dispatchUrl = null;

		try {
			if ("execute".equals(pageName)) {
				engineInstance = CockpitEngine.createInstance(getIOManager().getTemplateAsString(), getIOManager().getEnv());
				getIOManager().getHttpSession().setAttribute(EngineConstants.ENGINE_INSTANCE, engineInstance);
				//getExecutionSession().setAttributeInSession(EngineConstants.ENGINE_INSTANCE, engineInstance);
				dispatchUrl = "/WEB-INF/jsp/ngCockpit.jsp";
			} else if ("edit".equals(pageName)) {
				JSONObject template = null;
				template = buildBaseTemplate();
				// create a new engine instance
				engineInstance = CockpitEngine.createInstance(template.toString(), // servletIOManager.getTemplateAsString(),
						getIOManager().getEnvForWidget());
				getIOManager().getHttpSession().setAttribute(EngineConstants.ENGINE_INSTANCE, engineInstance);
				//getExecutionSession().setAttributeInSession(EngineConstants.ENGINE_INSTANCE, engineInstance);
				dispatchUrl = "/WEB-INF/jsp/ngCockpit.jsp";
			} else if ("test".equals(pageName)) {
				dispatchUrl = "/WEB-INF/jsp/test4.jsp";
			} else {
				// error
				dispatchUrl = "/WEB-INF/jsp/error.jsp";
			}

			// To deploy into JBOSSEAP64 is needed a StandardWrapper, instead of RestEasy Wrapper
			HttpServletRequest request = ResteasyProviderFactory.getContextData(HttpServletRequest.class);
			HttpServletResponse response = ResteasyProviderFactory.getContextData(HttpServletResponse.class);

			/**
			 * Setting the encoding type to the response object, so the Cockpit engine when calling the rendering of the chart (chart.jsp) can display the real
			 * content of the chart template. If this is not set, specific Italian letters, such as ù and à are going to be displayed as black squared question
			 * marks - they will not be displayed as they are specified by the user.
			 *
			 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
			 */
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");

			request.getRequestDispatcher(dispatchUrl).forward(request, response);
		} catch (Exception e) {
			throw SpagoBIEngineServiceExceptionHandler.getInstance().getWrappedException("", getEngineInstance(), e);
		} finally {
			logger.debug("OUT");
		}
	}

	@GET
	@Path("/executeTest")
	@Produces(MediaType.APPLICATION_JSON)
	public String testAction(@Context HttpServletResponse response) {

		logger.debug("IN");

		try {
			JSONObject obj = new JSONObject();
			try {
				obj.put("result", "ok");
			} catch (JSONException e) {
				logger.error("Error building the success string");
				throw new SpagoBIRuntimeException("Error building the success string");
			}
			String successString = obj.toString();
			return successString;
		} finally {
			logger.debug("OUT");
		}
	}

	private JSONObject buildBaseTemplate() {
		JSONObject template;

		logger.debug("IN");
		template = new JSONObject();
		logger.debug("OUT");

		return template;
	}
}
