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

package it.eng.spagobi.tools.dataset.bo;

import it.eng.spagobi.container.ObjectUtils;
import it.eng.spagobi.services.dataset.bo.SpagoBiDataSet;
import it.eng.spagobi.tools.dataset.common.behaviour.QuerableBehaviour;
import it.eng.spagobi.tools.dataset.common.dataproxy.IDataProxy;
import it.eng.spagobi.tools.dataset.common.dataproxy.JDBCDataProxy;
import it.eng.spagobi.tools.dataset.common.datareader.JDBCStandardDataReader;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
import it.eng.spagobi.tools.dataset.common.metadata.IMetaData;
import it.eng.spagobi.tools.dataset.persist.IDataSetTableDescriptor;
import it.eng.spagobi.tools.dataset.utils.DatasetMetadataParser;
import it.eng.spagobi.tools.datasource.bo.DataSourceFactory;
import it.eng.spagobi.tools.datasource.bo.IDataSource;
import it.eng.spagobi.utilities.database.AbstractDataBase;
import it.eng.spagobi.utilities.database.temporarytable.TemporaryTableManager;
import it.eng.spagobi.utilities.engines.SpagoBIEngineRuntimeException;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;
import it.eng.spagobi.utilities.json.JSONUtils;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public abstract class AbstractJDBCDataset extends ConfigurableDataSet {

	public static String DS_TYPE = "SbiQueryDataSet";
	private static final String QUERY = "query";
	private static final String QUERY_SCRIPT = "queryScript";
	private static final String QUERY_SCRIPT_LANGUAGE = "queryScriptLanguage";
	private static final String DATA_SOURCE = "dataSource";

	private static transient Logger logger = Logger.getLogger(AbstractJDBCDataset.class);

	/**
	 * Instantiates a new empty JDBC data set.
	 */
	public AbstractJDBCDataset() {
		super();
		setDataProxy(new JDBCDataProxy());
		setDataReader(new JDBCStandardDataReader());
		addBehaviour(new QuerableBehaviour(this));
	}

	// cannibalization :D
	public AbstractJDBCDataset(JDBCDataSet jdbcDataset) {
		this(jdbcDataset.toSpagoBiDataSet());
	}

	public AbstractJDBCDataset(SpagoBiDataSet dataSetConfig) {
		super(dataSetConfig);

		setDataProxy(new JDBCDataProxy());
		setDataReader(new JDBCStandardDataReader());

		try {
			setDataSource(DataSourceFactory.getDataSource(dataSetConfig.getDataSource()));
		} catch (Exception e) {
			throw new RuntimeException("Missing right exstension", e);
		}
		try {
			String config = JSONUtils.escapeJsonString(dataSetConfig.getConfiguration());
			JSONObject jsonConf = ObjectUtils.toJSONObject(config);
			setQuery((jsonConf.get(QUERY) != null) ? jsonConf.get(QUERY).toString() : "");
			setQueryScript((jsonConf.get(QUERY_SCRIPT) != null) ? jsonConf.get(QUERY_SCRIPT).toString() : "");
			setQueryScriptLanguage((jsonConf.get(QUERY_SCRIPT_LANGUAGE) != null) ? jsonConf.get(QUERY_SCRIPT_LANGUAGE).toString() : "");
		} catch (Exception e) {
			logger.error("Error while defining dataset configuration. Error:", e);
			throw new SpagoBIRuntimeException("Error while defining dataset configuration", e);
		}

		addBehaviour(new QuerableBehaviour(this));
	}

	/**
	 * Redefined for set schema name
	 *
	 */
	@Override
	public void setUserProfileAttributes(Map userProfile) {
		this.userProfileParameters = userProfile;
		IDataSource dataSource = getDataSource();
		if (dataSource != null && dataSource.checkIsMultiSchema()) {
			String schema = null;
			try {
				schema = (String) userProfile.get(dataSource.getSchemaAttribute());
				((JDBCDataProxy) dataProxy).setSchema(schema);
				logger.debug("Set UP Schema=" + schema);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("An error occurred while reading schema name from user profile", t);
			}
		}
	}

	@Override
	public SpagoBiDataSet toSpagoBiDataSet() {
		SpagoBiDataSet toReturn;
		JDBCDataProxy dataProxy;

		toReturn = super.toSpagoBiDataSet();

		toReturn.setType(DS_TYPE);

		dataProxy = this.getDataProxy();
		toReturn.setDataSource(dataProxy.getDataSource().toSpagoBiDataSource());

		try {
			JSONObject jsonConf = new JSONObject();
			jsonConf.put(QUERY, (getQuery() == null) ? "" : getQuery());
			jsonConf.put(QUERY_SCRIPT, (getQueryScript() == null) ? "" : getQueryScript());
			jsonConf.put(QUERY_SCRIPT_LANGUAGE, (getQueryScriptLanguage() == null) ? "" : getQueryScriptLanguage());
			toReturn.setConfiguration(jsonConf.toString());
		} catch (Exception e) {
			logger.error("Error while defining dataset configuration. Error:", e);
			throw new SpagoBIRuntimeException("Error while defining dataset configuration. Error:", e);
		}

		return toReturn;
	}

	@Override
	public JDBCDataProxy getDataProxy() {
		IDataProxy dataProxy;

		dataProxy = super.getDataProxy();

		if (dataProxy == null) {
			setDataProxy(new JDBCDataProxy());
			dataProxy = getDataProxy();
		}

		if (!(dataProxy instanceof JDBCDataProxy))
			throw new RuntimeException("DataProxy cannot be of type [" + dataProxy.getClass().getName() + "] in JDBCDataSet");

		return (JDBCDataProxy) dataProxy;
	}

	@Override
	public void setDataSource(IDataSource dataSource) {
		getDataProxy().setDataSource(dataSource);
	}

	@Override
	public IDataSource getDataSource() {
		return getDataProxy().getDataSource();
	}

	@Override
	public IMetaData getMetadata() {
		IMetaData metadata = null;
		try {
			DatasetMetadataParser dsp = new DatasetMetadataParser();
			metadata = dsp.xmlToMetadata(getDsMetadata());
		} catch (Exception e) {
			logger.error("Error loading the metadata", e);
			throw new SpagoBIEngineRuntimeException("Error loading the metadata", e);
		}
		return metadata;
	}

	@Override
	public IDataStore test() {
		loadData();
		return getDataStore();
	}

	@Override
	public IDataSetTableDescriptor persist(String tableName, IDataSource dataSource) {
		IDataSource datasetDataSource = getDataSource();
		if (datasetDataSource.getLabel().equals(dataSource.getLabel())) {
			logger.debug("Specified datasource is the dataset's datasource; using CREATE TABLE AS SELECT tecnique");
			try {
				List<String> fields = this.getFieldsList();
				return TemporaryTableManager.createTable(fields, (String) query, tableName, getDataSource());
			} catch (Exception e) {
				logger.error("Error peristing the temporary table", e);
				throw new SpagoBIEngineRuntimeException("Error peristing the temporary table", e);
			}
		} else {
			logger.debug("Specified datasource is NOT the dataset's datasource");
			return super.persist(tableName, dataSource);
		}
	}

	public static String encapsulateColumnName(String columnName, IDataSource dataSource) {
		logger.debug("IN");
		String toReturn = columnName;
		if (columnName != null) {
			String aliasDelimiter = TemporaryTableManager.getAliasDelimiter(dataSource);
			logger.debug("Alias delimiter is [" + aliasDelimiter + "]");
			if (!columnName.startsWith(aliasDelimiter) || !columnName.endsWith(aliasDelimiter)) {
				toReturn = aliasDelimiter + columnName + aliasDelimiter;
			}
		}
		logger.debug("OUT: returning " + toReturn);
		return toReturn;
	}

	/**
	 * @param string
	 * @param dataSource
	 * @return
	 */
	public static String encapsulateColumnAlaias(String columnAlias, IDataSource dataSource) {
		logger.debug("IN");
		String toReturn = null;
		if (columnAlias != null) {
			String aliasDelimiter = TemporaryTableManager.getAliasDelimiter(dataSource);
			logger.debug("Alias delimiter is [" + aliasDelimiter + "]");
			toReturn = aliasDelimiter + columnAlias + aliasDelimiter;
		}
		logger.debug("OUT: returning " + toReturn);
		return toReturn;
	}

	public static String substituteStandardWithDatasourceDelimiter(String columnName, IDataSource dataSource) {
		logger.debug("IN");
		if (columnName != null) {
			logger.debug("Column name is [" + columnName + "]");
			columnName = columnName.replaceAll(AbstractDataBase.STANDARD_ALIAS_DELIMITER, TemporaryTableManager.getAliasDelimiter(dataSource));
			logger.debug("Column name after replacement is [" + columnName + "]");
		} else {
			throw new IllegalArgumentException("THe arguments columnName [" + columnName + "] and dataSource [" + dataSource + "] cannot be null");
		}
		logger.debug("OUT");
		return columnName;
	}
}
