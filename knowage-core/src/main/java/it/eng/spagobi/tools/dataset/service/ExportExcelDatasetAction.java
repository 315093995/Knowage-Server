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
package it.eng.spagobi.tools.dataset.service;

import it.eng.spago.error.EMFUserError;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.commons.services.AbstractSpagoBIAction;
import it.eng.spagobi.commons.utilities.SpagoBIUtilities;
import it.eng.spagobi.commons.utilities.messages.MessageBuilder;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
import it.eng.spagobi.tools.dataset.dao.IDataSetDAO;
import it.eng.spagobi.utilities.exceptions.SpagoBIServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExportExcelDatasetAction extends AbstractSpagoBIAction {

	public static final String VERSION_ID = "id";

	// logger component
	private static Logger logger = Logger.getLogger(ExportExcelDatasetAction.class);

	@Override
	public void doService() {
		logger.info("IN");

		// GET SERVER POST from sessione and set in hidden cell
		// String url =
		// "http://localhost:8080/knowage/restful-services/1.0/Mytest/test";
		// http://localhost:8080/knowage/servlet/AdapterHTTP?ACTION_NAME=EXPORT_EXCEL_DATASET_ACTION&SBI_EXECUTION_ID=-1&LIGHT_NAVIGATOR_DISABLED=TRUE&id=2
		String contextPath = it.eng.spagobi.commons.utilities.GeneralUtilities.getSpagoBiHost() + getHttpRequest().getContextPath();

		try {
			Integer id = this.getAttributeAsInteger(VERSION_ID);
			// GET DATA STORE INFO
			IDataSetDAO dao = DAOFactory.getDataSetDAO();
			dao.setUserProfile(this.getUserProfile());
			IDataSet dataSet = dao.loadDataSetById(id);

			IDataStore dataStore = null;
			try {
				dataSet.loadData();
				dataStore = dataSet.getDataStore();
			} catch (Exception e) {
				logger.error("Error loading datat for xls export");
			}

			// setup response
			String url = contextPath + "/restful-services/selfservicedataset/export/" + dataSet.getLabel();

			freezeHttpResponse();
			getHttpResponse().setHeader("Content-Disposition", "attachment" + "; filename=\"" + dataSet.getName() + ".xlsm" + "\";");
			getHttpResponse().setContentType("application/vnd.ms-excel");
			// create WB
			String resourcePath = SpagoBIUtilities.getResourcePath();
			XSSFWorkbook wb = null;
			InputStream fileInputStream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("it/eng/spagobi/tools/dataset/service/export_dataset_template.xlsm");
			try {
				wb = new XSSFWorkbook(fileInputStream);
			} catch (IOException e) {
				logger.error("Input Output Exception " + e.getMessage());
				throw new SpagoBIServiceException(this.getActionName(), "Impossible to get xlsm export template file ", e);
			}

			if (wb != null) {
				XSSFSheet sheet = wb.getSheet("datastore");
				// STYLE CELL
				CellStyle borderStyleHeader = wb.createCellStyle();
				borderStyleHeader.setBorderBottom(CellStyle.BORDER_THIN);
				borderStyleHeader.setBorderLeft(CellStyle.BORDER_THIN);
				borderStyleHeader.setBorderRight(CellStyle.BORDER_THIN);
				borderStyleHeader.setBorderTop(CellStyle.BORDER_THIN);
				borderStyleHeader.setAlignment(CellStyle.ALIGN_CENTER);
				CellStyle borderStyleRow = wb.createCellStyle();
				borderStyleRow.setBorderBottom(CellStyle.BORDER_THIN);
				borderStyleRow.setBorderLeft(CellStyle.BORDER_THIN);
				borderStyleRow.setBorderRight(CellStyle.BORDER_THIN);
				borderStyleRow.setBorderTop(CellStyle.BORDER_THIN);
				borderStyleRow.setAlignment(CellStyle.ALIGN_RIGHT);
				if (dataStore != null) {
					// CREATE HEADER SHEET
					XSSFRow header = sheet.createRow((short) 3);// quarta riga
					if (dataStore.getMetaData() != null && dataStore.getMetaData().getFieldCount() > 0) {
						for (int i = 0; i <= dataStore.getMetaData().getFieldCount() - 1; i++) {
							XSSFCell cell = header.createCell(i + 1);
							cell.setCellValue(dataStore.getMetaData().getFieldAlias(i));
							cell.setCellStyle(borderStyleHeader);
						}
					}
					// FILL CELL RECORD
					if (dataStore.getRecordsCount() > 0) {
						for (int i = 0; i <= dataStore.getRecordsCount() - 1; i++) {
							XSSFRow row = sheet.createRow(i + 4);// dalla quinta
																	// riga
							if (dataStore.getRecordAt(i) != null && dataStore.getRecordAt(i).getFields() != null
									&& dataStore.getRecordAt(i).getFields().size() > 0) {
								for (int k = 0; k <= dataStore.getRecordAt(i).getFields().size() - 1; k++) {
									XSSFCell cell = row.createCell(k + 1);
									cell.setCellValue("" + dataStore.getRecordAt(i).getFieldAt(k).getValue());
									cell.setCellStyle(borderStyleRow);
								}
							}
						}
					}
					// SET URL IN CELL C3
					XSSFRow row1 = sheet.getRow(2);
					XSSFCell urlCell = row1.getCell(2);
					urlCell.setCellValue(url);
				}else{
					MessageBuilder msgBuild = new MessageBuilder();

					XSSFRow header = sheet.createRow((short) 3);// quarta riga
					XSSFCell cell = header.createCell(1);
					cell.setCellValue(msgBuild.getMessage( "exporter.dataset.excel",getLocale()));
					cell.setCellStyle(borderStyleHeader);
				}

				OutputStream out;
				try {
					out = getHttpResponse().getOutputStream();
					wb.write(out);
					getHttpResponse().getOutputStream().flush();
					getHttpResponse().getOutputStream().close();
				} catch (IOException e) {
					logger.error("write output file stream error " + e.getMessage());
					throw new SpagoBIServiceException(this.getActionName(), "Impossible to write output file xls error", e);
				}

			}

		} catch (EMFUserError e) {
			logger.error("write output stream error " + e.getMessage());
			throw new SpagoBIServiceException(this.getActionName(), "Impossible to write back the responce to the client", e);
		}

	}
}
