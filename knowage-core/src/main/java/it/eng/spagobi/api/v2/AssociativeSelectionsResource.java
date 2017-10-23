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
package it.eng.spagobi.api.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.jgrapht.graph.Pseudograph;
import org.json.JSONArray;
import org.json.JSONObject;

import it.eng.spagobi.api.common.DataSetResourceAbstractResource;
import it.eng.spagobi.commons.SingletonConfig;
import it.eng.spagobi.commons.constants.ConfigurationConstants;
import it.eng.spagobi.commons.constants.SpagoBIConstants;
import it.eng.spagobi.services.rest.annotations.UserConstraint;
import it.eng.spagobi.services.serialization.JsonConverter;
import it.eng.spagobi.tools.dataset.associativity.IAssociativityManager;
import it.eng.spagobi.tools.dataset.associativity.strategy.AssociativeStrategyFactory;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.cache.SpagoBICacheConfiguration;
import it.eng.spagobi.tools.dataset.common.association.Association;
import it.eng.spagobi.tools.dataset.common.association.Association.Field;
import it.eng.spagobi.tools.dataset.common.association.AssociationGroup;
import it.eng.spagobi.tools.dataset.common.association.AssociationGroupJSONSerializer;
import it.eng.spagobi.tools.dataset.common.datawriter.CockpitJSONDataWriter;
import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData;
import it.eng.spagobi.tools.dataset.common.metadata.IMetaData;
import it.eng.spagobi.tools.dataset.dao.IDataSetDAO;
import it.eng.spagobi.tools.dataset.graph.AssociationAnalyzer;
import it.eng.spagobi.tools.dataset.graph.LabeledEdge;
import it.eng.spagobi.tools.dataset.graph.associativity.Config;
import it.eng.spagobi.tools.dataset.graph.associativity.Selection;
import it.eng.spagobi.tools.dataset.graph.associativity.utils.AssociativeLogicResult;
import it.eng.spagobi.tools.dataset.graph.associativity.utils.AssociativeLogicUtils;
import it.eng.spagobi.tools.datasource.bo.IDataSource;
import it.eng.spagobi.utilities.StringUtils;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.exceptions.SpagoBIRestServiceException;
import it.eng.spagobi.utilities.exceptions.SpagoBIServiceParameterException;
import it.eng.spagobi.utilities.sql.SqlUtils;

@Path("/2.0/associativeSelections")
public class AssociativeSelectionsResource extends DataSetResourceAbstractResource {

	static protected Logger logger = Logger.getLogger(AssociativeSelectionsResource.class);
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.SELF_SERVICE_DATASET_MANAGEMENT })
	public String getAssociativeSelections(@QueryParam("associationGroup") String associationGroupString, @QueryParam("selections") String selectionsString,
			@QueryParam("datasets") String datasetsString, @QueryParam("nearRealtime") String nearRealtimeDatasetsString) {
		logger.debug("IN");

		try {
			IDataSetDAO dataSetDAO = getDataSetDAO();
			dataSetDAO.setUserProfile(getUserProfile());

			// parse selections
			if (selectionsString == null || selectionsString.isEmpty()) {
				throw new SpagoBIServiceParameterException(this.request.getPathInfo(), "Query parameter [selections] cannot be null or empty");
			}

			JSONObject selectionsObject = new JSONObject(selectionsString);

			// parse association group
			if (associationGroupString == null) {
				throw new SpagoBIServiceParameterException(this.request.getPathInfo(), "Query parameter [associationGroup] cannot be null");
			}

			AssociationGroupJSONSerializer serializer = new AssociationGroupJSONSerializer();

			JSONObject associationGroupObject = new JSONObject(associationGroupString);
			AssociationGroup associationGroup = serializer.deserialize(associationGroupObject);
			fixAssociationGroup(associationGroup);

			// parse documents
			Set<String> documents = new HashSet<>();
			JSONArray associations = associationGroupObject.optJSONArray("associations");
			if (associations != null) {
				for (int associationIndex = 0; associationIndex < associations.length(); associationIndex++) {
					JSONObject association = associations.getJSONObject(associationIndex);
					JSONArray fields = association.optJSONArray("fields");
					if (fields != null) {
						for (int fieldIndex = fields.length() - 1; fieldIndex >= 0; fieldIndex--) {
							JSONObject field = fields.getJSONObject(fieldIndex);
							String type = field.optString("type");
							if ("document".equalsIgnoreCase(type)) {
								String store = field.optString("store");
								documents.add(store);
							}
						}
					}
				}
			}

			JSONObject associationGroupObjectWithoutParams = new JSONObject(associationGroupString);
			JSONArray associationsWithoutParams = associationGroupObjectWithoutParams.optJSONArray("associations");
			if (associationsWithoutParams != null) {
				for (int associationIndex = associationsWithoutParams.length() - 1; associationIndex >= 0; associationIndex--) {
					JSONObject association = associationsWithoutParams.getJSONObject(associationIndex);
					JSONArray fields = association.getJSONArray("fields");
					for (int fieldIndex = fields.length() - 1; fieldIndex >= 0; fieldIndex--) {
						JSONObject field = fields.getJSONObject(fieldIndex);
						String column = field.getString("column");
						String store = field.getString("store");
						String type = field.optString("type");
						if (("document".equalsIgnoreCase(type)) || (column.startsWith("$P{") && column.endsWith("}"))
								|| dataSetDAO.loadDataSetByLabel(store).isRealtime()) {
							fields.remove(fieldIndex);
						}
					}
				}
			}
			AssociationGroup associationGroupWithoutParams = serializer.deserialize(associationGroupObjectWithoutParams);
			fixAssociationGroup(associationGroupWithoutParams);

			// parse dataset parameters
			Map<String, Map<String, String>> datasetParameters = new HashMap<>();
			if (datasetsString != null && !datasetsString.isEmpty()) {
				JSONObject datasetsObject = new JSONObject(datasetsString);
				Iterator<String> datasetsIterator = datasetsObject.keys();
				while (datasetsIterator.hasNext()) {
					String datasetLabel = datasetsIterator.next();

					Map<String, String> parameters = new HashMap<>();
					datasetParameters.put(datasetLabel, parameters);

					JSONObject datasetObject = datasetsObject.getJSONObject(datasetLabel);
					Iterator<String> datasetIterator = datasetObject.keys();
					while (datasetIterator.hasNext()) {
						String param = datasetIterator.next();
						String value = datasetObject.getString(param);
						parameters.put(param, value);
					}
				}
			}

			// parse near realtime datasets
			Set<String> nearRealtimeDatasets = new HashSet<>();
			if (nearRealtimeDatasetsString != null && !nearRealtimeDatasetsString.isEmpty()) {
				JSONArray jsonArray = new JSONArray(nearRealtimeDatasetsString);
				for (int i = 0; i < jsonArray.length(); i++) {
					nearRealtimeDatasets.add(jsonArray.getString(i));
				}
			}

			AssociationAnalyzer analyzerWithoutParams = new AssociationAnalyzer(associationGroupWithoutParams.getAssociations());
			analyzerWithoutParams.process();
			Map<String, Map<String, String>> datasetToAssociationToColumnMap = analyzerWithoutParams.getDatasetToAssociationToColumnMap();

			AssociationAnalyzer analyzer = new AssociationAnalyzer(associationGroup.getAssociations());
			analyzer.process();
			Pseudograph<String, LabeledEdge<String>> graph = analyzer.getGraph();

			IDataSource cacheDataSource = SpagoBICacheConfiguration.getInstance().getCacheDataSource();

			// get datasets from selections
			List<Selection> filters = new ArrayList<>();
			Map<String, Map<String, Set<String>>> selectionsMap = new HashMap<>();

			Iterator<String> it = selectionsObject.keys();
			while (it.hasNext()) {
				String datasetDotColumn = it.next();
				Assert.assertTrue(datasetDotColumn.indexOf(".") >= 0, "Data not compliant with format <DATASET_LABEL>.<COLUMN> [" + datasetDotColumn + "]");
				String[] tmpDatasetAndColumn = datasetDotColumn.split("\\.");
				Assert.assertTrue(tmpDatasetAndColumn.length == 2, "Impossible to get both dataset label and column");

				String datasetLabel = tmpDatasetAndColumn[0];
				String column = SqlUtils.unQuote(tmpDatasetAndColumn[1]);

				Assert.assertNotNull(datasetLabel, "A dataset label in selections is null");
				Assert.assertTrue(!datasetLabel.isEmpty(), "A dataset label in selections is empty");
				Assert.assertNotNull(column, "A column for dataset " + datasetLabel + "  in selections is null");
				Assert.assertTrue(!column.isEmpty(), "A column for dataset " + datasetLabel + " in selections is empty");

				IDataSet dataset = getDataSetDAO().loadDataSetByLabel(datasetLabel);
				boolean isNearRealtime = nearRealtimeDatasets.contains(datasetLabel);
				IDataSource dataSource = getDataSource(dataset, isNearRealtime);
				boolean isDateColumn = isDateColumn(column, dataset);

				String values = null;
				String valuesForQuery = null;
				Object object = selectionsObject.getJSONArray(datasetDotColumn).get(0);
				if (object instanceof JSONArray) {
					if (isDateColumn) {
						JSONArray jsonArray = (JSONArray) object;
						List<String> valueList = new ArrayList<>();
						List<String> valueForQueryList = new ArrayList<>();
						for (int i = 0; i < jsonArray.length(); i++) {
							String value = convertDateString(jsonArray.getString(i), CockpitJSONDataWriter.DATE_TIME_FORMAT,
									CockpitJSONDataWriter.CACHE_DATE_TIME_FORMAT);
							valueForQueryList.add(getDateForQuery(value, dataSource));
							valueList.add("'" + value + "'");
						}
						values = StringUtils.join(valueList, ",");
						valuesForQuery = StringUtils.join(valueForQueryList, ",");
					} else {
						values = ("\"" + ((JSONArray) object).join("\",\"") + "\"").replace("\"\"", "'").replace("\"", "'");
						valuesForQuery = values;
					}
				} else {
					if (isDateColumn) {
						values = convertDateString(object.toString(), CockpitJSONDataWriter.DATE_TIME_FORMAT, CockpitJSONDataWriter.CACHE_DATE_TIME_FORMAT);
						valuesForQuery = getDateForQuery(values, dataSource);
						values = "'" + values + "'";
					} else {
						values = "'" + object.toString() + "'";
						valuesForQuery = values;
					}
				}

				filters.add(new Selection(datasetLabel, getFilter(dataset, isNearRealtime, column, valuesForQuery)));

				if (!selectionsMap.containsKey(datasetLabel)) {
					selectionsMap.put(datasetLabel, new HashMap<String, Set<String>>());
				}
				Map<String, Set<String>> selection = selectionsMap.get(datasetLabel);
				if (!selection.containsKey(column)) {
					selection.put(column, new HashSet<String>());
				}
				selection.get(column).add("(" + values + ")");
			}

			logger.debug("Filter list: " + filters);

			String strategy = SingletonConfig.getInstance().getConfigValue(ConfigurationConstants.SPAGOBI_DATASET_ASSOCIATIVE_LOGIC_STRATEGY);
			Config config = AssociativeLogicUtils.buildConfig(strategy, graph, datasetToAssociationToColumnMap, filters, nearRealtimeDatasets,
					datasetParameters, documents);

			IAssociativityManager manager = AssociativeStrategyFactory.createStrategyInstance(config, getUserProfile());
			AssociativeLogicResult result = manager.process();

			Map<String, Map<String, Set<String>>> selections = AssociationAnalyzer.getSelections(associationGroup, graph, result);

			for (String d : selectionsMap.keySet()) {
				if (!selections.containsKey(d)) {
					selections.put(d, new HashMap<String, Set<String>>());
				}
				selections.get(d).putAll(selectionsMap.get(d));
			}

			String stringFeed = JsonConverter.objectToJson(selections, Map.class);
			return stringFeed;
		} catch (Exception e) {
			String errorMessage = "An error occurred while getting associative selections";
			logger.error(errorMessage, e);
			throw new SpagoBIRestServiceException(errorMessage, buildLocaleFromSession(), e);
		} finally {
			logger.debug("OUT");
		}
	}
	
	private void fixAssociationGroup(AssociationGroup associationGroup) {
		IDataSetDAO dataSetDAO = getDataSetDAO();

		Map<String, IMetaData> dataSetLabelToMedaData = new HashMap<>();
		for (Association association : associationGroup.getAssociations()) {
			if (association.getDescription().contains(".")) {
				for (Field field : association.getFields()) {
					String fieldName = field.getFieldName();
					if (fieldName.contains(":")) {
						String dataSetLabel = field.getDataSetLabel();

						IMetaData metadata = null;
						if (dataSetLabelToMedaData.containsKey(dataSetLabel)) {
							metadata = dataSetLabelToMedaData.get(dataSetLabel);
						} else {
							metadata = dataSetDAO.loadDataSetByLabel(dataSetLabel).getMetadata();
							dataSetLabelToMedaData.put(dataSetLabel, metadata);
						}

						for (int i = 0; i < metadata.getFieldCount(); i++) {
							IFieldMetaData fieldMeta = metadata.getFieldMeta(i);
							String alias = fieldMeta.getAlias();
							if (fieldMeta.getName().equals(fieldName)) {
								association.setDescription(association.getDescription().replace(dataSetLabel + "." + fieldName, dataSetLabel + "." + alias));
								field.setFieldName(alias);
								break;
							}
						}
					}
				}
			}
		}
	}
	
}
