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
package it.eng.spagobi.tools.datasource.bo;

import it.eng.spagobi.services.datasource.bo.SpagoBiDataSource;
import it.eng.spagobi.services.validation.Xss;
import it.eng.spagobi.tools.dataset.bo.AbstractJDBCDataset;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.bo.JDBCDatasetFactory;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Defines an <code>DataSource</code> object
 *
 */
@JsonInclude(Include.NON_NULL)
public class DataSource implements Serializable, IDataSource {

	private static transient Logger logger = Logger.getLogger(DataSource.class);

	@NotNull
	@Max(11)
	private int dsId;

	@Xss
	@Max(160)
	private String descr;

	@NotNull
	@Xss
	@Max(50)
	private String label;

	@Xss
	@Max(50)
	private String jndi;

	@Xss
	@Max(500)
	private String urlConnection;

	@Xss
	@Max(50)
	private String user;

	@Xss
	@Max(50)
	private String pwd;

	@Xss
	@Max(160)
	private String driver;

	@NotNull
	@Max(11)
	private Integer dialectId;

	private String hibDialectClass;
	private String hibDialectName;
	private Set engines = null;
	private Set objects = null;

	@Max(45)
	private String schemaAttribute = null;

	@NotNull
	private Boolean multiSchema = null;

	private Boolean readOnly;
	private Boolean writeDefault;

	public Boolean getReadOnly() {
		return readOnly;
	}

	public Boolean getWriteDefault() {
		return writeDefault;
	}

	@Override
	public String getSchemaAttribute() {
		return schemaAttribute;
	}

	@Override
	public void setSchemaAttribute(String schemaAttribute) {
		this.schemaAttribute = schemaAttribute;
	}

	@Override
	public Boolean getMultiSchema() {
		return multiSchema;
	}

	@Override
	public void setMultiSchema(Boolean multiSchema) {
		this.multiSchema = multiSchema;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#toSpagoBiDataSource()
	 */
	@Override
	public SpagoBiDataSource toSpagoBiDataSource() {
		SpagoBiDataSource sbd = new SpagoBiDataSource();
		sbd.setId(dsId);
		sbd.setDriver(driver);
		sbd.setHibDialectClass("");
		sbd.setHibDialectName("");
		sbd.setJndiName(jndi);
		sbd.setLabel(label);
		sbd.setPassword(pwd);
		sbd.setUrl(urlConnection);
		sbd.setUser(user);
		sbd.setHibDialectClass(hibDialectClass);
		sbd.setHibDialectName(hibDialectName);
		sbd.setMultiSchema(multiSchema);
		sbd.setSchemaAttribute(schemaAttribute);
		sbd.setReadOnly(readOnly);
		sbd.setWriteDefault(writeDefault);
		return sbd;
	}

	@Override
	public boolean checkIsMultiSchema() {
		return getMultiSchema() != null && getMultiSchema().booleanValue();
	}

	@Override
	public boolean checkIsJndi() {
		return getJndi() != null && getJndi().equals("") == false;
	}

	@Override
	@JsonIgnore
	public Connection getConnection() throws NamingException, SQLException, ClassNotFoundException {
		return getConnection(null);
	}

	@Override
	public Connection getConnection(String schema) throws NamingException, SQLException, ClassNotFoundException {
		Connection connection = null;

		if (checkIsJndi()) {
			connection = getJndiConnection(schema);
		} else {
			connection = getDirectConnection();
		}

		return connection;
	}

	/**
	 * Get the connection from JNDI.
	 *
	 * @return Connection to database
	 *
	 * @throws NamingException
	 *             the naming exception
	 * @throws SQLException
	 *             the SQL exception
	 */
	private Connection getJndiConnection(String schema) throws NamingException, SQLException {
		Connection connection = null;

		Context ctx;
		String jndiName;

		jndiName = (checkIsMultiSchema() && schema != null) ? getJndi() + schema : getJndi();

		ctx = new InitialContext();
		javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup(jndiName);
		connection = ds.getConnection();

		return connection;
	}

	/**
	 * Get the connection using jdbc.
	 *
	 * @return Connection to database
	 *
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws SQLException
	 *             the SQL exception
	 */
	@JsonIgnore
	private Connection getDirectConnection() throws ClassNotFoundException, SQLException {
		Connection connection = null;

		try {
			Class.forName(getDriver());
			connection = DriverManager.getConnection(getUrlConnection(), getUser(), getPwd());
		} catch (Throwable t) {
			throw new RuntimeException("Impossible to create a direct connection to database at host [" + getUrlConnection() + "] with user [" + getUser()
					+ "]. Check if the database is running and if the password is correct.");
		}
		return connection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getDsId()
	 */
	@Override
	public int getDsId() {
		return dsId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setDsId(int)
	 */
	@Override
	public void setDsId(int dsId) {
		this.dsId = dsId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getDescr()
	 */
	@Override
	public String getDescr() {
		return descr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setDescr(java.lang.String)
	 */
	@Override
	public void setDescr(String descr) {
		this.descr = descr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getLabel()
	 */
	@Override
	public String getLabel() {
		return label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setLabel(java.lang.String)
	 */
	@Override
	public void setLabel(String label) {
		this.label = label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getJndi()
	 */
	@Override
	public String getJndi() {
		return jndi;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setJndi(java.lang.String)
	 */
	@Override
	public void setJndi(String jndi) {
		this.jndi = jndi;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getUrlConnection()
	 */
	@Override
	public String getUrlConnection() {
		return urlConnection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setUrlConnection(java. lang.String)
	 */
	@Override
	public void setUrlConnection(String url_connection) {
		this.urlConnection = url_connection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getUser()
	 */
	@Override
	public String getUser() {
		return user;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setUser(java.lang.String)
	 */
	@Override
	public void setUser(String user) {
		this.user = user;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getPwd()
	 */
	@Override
	public String getPwd() {
		return pwd;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setPwd(java.lang.String)
	 */
	@Override
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getDriver()
	 */
	@Override
	public String getDriver() {
		return driver;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setDriver(java.lang.String )
	 */
	@Override
	public void setDriver(String driver) {
		this.driver = driver;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getDialectId()
	 */
	@Override
	public Integer getDialectId() {
		return dialectId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setDialectId(java.lang .Integer)
	 */
	@Override
	public void setDialectId(Integer dialectId) {
		this.dialectId = dialectId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getEngines()
	 */
	@Override
	@JsonIgnore
	public Set getEngines() {
		return engines;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setEngines(java.util.Set)
	 */
	@Override
	public void setEngines(Set engines) {
		this.engines = engines;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#getObjects()
	 */
	@Override
	@JsonIgnore
	public Set getObjects() {
		return objects;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.datasource.bo.IDataSource#setObjects(java.util.Set)
	 */
	@Override
	public void setObjects(Set objects) {
		this.objects = objects;
	}

	@Override
	public String getHibDialectClass() {
		return hibDialectClass;
	}

	@Override
	public void setHibDialectClass(String hibDialectClass) {
		this.hibDialectClass = hibDialectClass;
	}

	@Override
	public String getHibDialectName() {
		return hibDialectName;
	}

	@Override
	public void setHibDialectName(String hibDialectName) {
		this.hibDialectName = hibDialectName;
	}

	@Override
	public void setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public void setWriteDefault(Boolean writeDefault) {
		this.writeDefault = writeDefault;
	}

	@Override
	public Boolean checkIsReadOnly() {
		if (readOnly == null)
			readOnly = true;
		return readOnly;
	}

	@Override
	public Boolean checkIsWriteDefault() {
		if (writeDefault == null)
			writeDefault = false;
		return writeDefault;
	}

	@Override
	public IDataStore executeStatement(String statement, Integer start, Integer limit) {
		return executeStatement(statement, start, limit, -1);
	}

	@Override
	public IDataStore executeStatement(String statement, Integer start, Integer limit, Integer maxRowCount) {
		logger.debug("IN: Statement is [" + statement + "], start = [" + start + "], limit = [" + limit + "], maxResults = [" + maxRowCount + "]");
		IDataSet dataSet = JDBCDatasetFactory.getJDBCDataSet(this);
		dataSet.setDataSource(this);
		((AbstractJDBCDataset) dataSet).setQuery(statement); // all datasets
																// retrieved by
																// the factory
																// extend
																// AbstractJDBCDataset
		if (start == null && limit == null && maxRowCount == null) {
			dataSet.loadData();
		} else {
			dataSet.loadData(start, limit, maxRowCount);
		}
		IDataStore dataStore = dataSet.getDataStore();
		logger.debug("Data store retrieved successfully");
		logger.debug("OUT");
		return dataStore;
	}

}
