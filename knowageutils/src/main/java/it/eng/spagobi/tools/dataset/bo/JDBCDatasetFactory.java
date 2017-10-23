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

import it.eng.spagobi.tools.datasource.bo.IDataSource;

import org.apache.log4j.Logger;

public class JDBCDatasetFactory {
	private static transient Logger logger = Logger
			.getLogger(JDBCDatasetFactory.class);

	public static IDataSet getJDBCDataSet(IDataSource dataSource) {
		IDataSet dataSet = null;

		if (dataSource == null) {

			throw new IllegalArgumentException(
					"datasource parameter cannot be null");
		}
		String dialect = dataSource.getHibDialectClass();
		if (dialect.contains("hbase")) {
			dataSet = new JDBCHBaseDataSet();
		} else if (dialect.contains("hive")) {
			dataSet = new JDBCHiveDataSet();
		}else if (dialect.contains("orient")) {
			dataSet = new JDBCOrientDbDataSet();
		} else {
			dataSet = new JDBCDataSet();
		}

		return dataSet;
	}
}
