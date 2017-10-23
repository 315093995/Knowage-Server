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
package it.eng.spagobi.commons.utilities.indexing;

import it.eng.spagobi.commons.bo.Domain;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.commons.utilities.JTidyHTMLHandler;
import it.eng.spagobi.tenant.TenantManager;
import it.eng.spagobi.tools.objmetadata.bo.ObjMetacontent;
import it.eng.spagobi.tools.objmetadata.bo.ObjMetadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.util.Version;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

public class LuceneSearcher {

	static private Logger logger = Logger.getLogger(LuceneSearcher.class);

	private static final String LONG_TEXT = "LONG_TEXT";// html
	private static final String SHORT_TEXT = "SHORT_TEXT";// simple text

	public static HashMap<String, Object> searchIndex(IndexSearcher searcher, String queryString, String index, String[] fields, String metaDataToSearch,
			boolean isSummaryEnabled) throws IOException, ParseException {
		Monitor monitor = MonitorFactory.start("it.eng.spagobi.commons.utilities.indexing.LuceneSearcher.searchIndex()");
		logger.debug("IN");
		HashMap<String, Object> objectsToReturn = new HashMap<String, Object>();

		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		BooleanQuery andQuery = new BooleanQuery();
		if (metaDataToSearch != null) {
			// search for query string on metadata name field and content
			// where metadata name = metaDataToSearch
			Query queryMetadata = new TermQuery(new Term(IndexingConstants.METADATA, metaDataToSearch));
			andQuery.add(queryMetadata, BooleanClause.Occur.MUST);
		}
		Query query = new MultiFieldQueryParser(Version.LUCENE_CURRENT, fields, analyzer).parse(cleanQueryString(queryString));
		andQuery.add(query, BooleanClause.Occur.MUST);
		Query tenantQuery = new TermQuery(new Term(IndexingConstants.TENANT, getTenant()));
		andQuery.add(tenantQuery, BooleanClause.Occur.MUST);
		logger.debug("Searching for: " + andQuery.toString());
		int hitsPerPage = 50;

		// Collect enough docs to show 5 pages
		TopScoreDocCollector collector = TopScoreDocCollector.create(5 * hitsPerPage, false);
		searcher.search(andQuery, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		// setsback to action
		objectsToReturn.put("hits", hits);

		if (hits != null && isSummaryEnabled) {
			Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(andQuery));
			logger.debug("hits size: " + hits.length);
			for (int i = 0; i < hits.length; i++) {
				ScoreDoc hit = hits[i];
				Document doc = searcher.doc(hit.doc);
				String biobjId = doc.get(IndexingConstants.BIOBJ_ID);

				String[] subobjNames = doc.getValues(IndexingConstants.SUBOBJ_NAME);
				if (subobjNames != null && subobjNames.length != 0) {
					String views = "";
					for (int k = 0; k < subobjNames.length; k++) {
						views += subobjNames[k] + " ";
					}
					objectsToReturn.put(biobjId + "-views", views);
				}
				String summary = "";
				if (highlighter != null) {
					String[] summaries;
					try {
						Integer idobj = (Integer.valueOf(biobjId));

						String contentToSearchOn = fillSummaryText(idobj);
						if (contentToSearchOn != null) {
							summaries = highlighter.getBestFragments(new StandardAnalyzer(Version.LUCENE_CURRENT), IndexingConstants.CONTENTS,
									contentToSearchOn, 3);
							if (summaries != null) {
								StringBuffer summaryBuffer = new StringBuffer();
								if (summaries.length > 0) {
									summaryBuffer.append(summaries[0]);
								}
								for (int j = 1; j < summaries.length; j++) {
									summaryBuffer.append(" ... ");
									summaryBuffer.append(summaries[j]);
								}
								summary = summaryBuffer.toString();
								// get only a portion of summary
								if (summary.length() > 101) {
									summary = summary.substring(0, 100);
									summary += "...";
								}
								objectsToReturn.put(biobjId, summary);
							}
						}
					} catch (InvalidTokenOffsetsException e) {
						logger.error(e.getMessage(), e);
					} catch (NumberFormatException e) {
						logger.error(e.getMessage(), e);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		}
		int numTotalHits = collector.getTotalHits();
		logger.info(numTotalHits + " total matching documents");

		logger.debug("OUT");
		monitor.stop();
		return objectsToReturn;

	}

	private static String fillSummaryText(Integer objId) throws Exception {
		Monitor monitor = MonitorFactory.start("it.eng.spagobi.commons.utilities.indexing.LuceneSearcher.fillSummaryText(Integer objId)");
		logger.debug("IN");
		List metadata = DAOFactory.getObjMetadataDAO().loadAllObjMetadata();
		if (metadata != null && !metadata.isEmpty()) {
			ByteArrayInputStream bais = null;
			Iterator it = metadata.iterator();
			while (it.hasNext()) {
				ObjMetadata objMetadata = (ObjMetadata) it.next();
				ObjMetacontent objMetacontent = DAOFactory.getObjMetacontentDAO().loadObjMetacontent(objMetadata.getObjMetaId(), objId, null);
				if (objMetacontent != null) {
					Integer binId = objMetacontent.getBinaryContentId();
					Integer idDomain = objMetadata.getDataType();
					Domain domain = DAOFactory.getDomainDAO().loadDomainById(idDomain);
					byte[] content = objMetacontent.getContent();
					String htmlContent = null;
					if (domain.getValueCd().equalsIgnoreCase(LONG_TEXT)) {
						bais = new ByteArrayInputStream(content);
						JTidyHTMLHandler htmlHandler = new JTidyHTMLHandler();
						htmlContent = htmlHandler.getContent(bais);
						bais.close();
						return htmlContent;
					} else {
						return new String(content, "UTF-8");
					}
				}
			}
		}
		logger.debug("OUT");
		monitor.stop();
		return null;
	}

	public static HashMap<String, Object> searchIndexFuzzy(IndexSearcher searcher, String queryString, String index, String[] fields, String metaDataToSearch,
			boolean isSummaryEnabled) throws IOException, ParseException {
		Monitor monitor = MonitorFactory.start("it.eng.spagobi.commons.utilities.indexing.LuceneSearcher.searchIndexFuzzy()");
		logger.debug("IN");
		HashMap<String, Object> objectsToReturn = new HashMap<String, Object>();
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		BooleanQuery orQuery = new BooleanQuery();
		BooleanQuery andQuery = new BooleanQuery();
		for (int i = 0; i < fields.length; i++) {
			Query query = new FuzzyQuery(new Term(fields[i], cleanQueryString(queryString)));
			query = query.rewrite(searcher.getIndexReader());
			orQuery.add(query, BooleanClause.Occur.SHOULD);
		}
		andQuery.add(orQuery, BooleanClause.Occur.MUST);
		if (metaDataToSearch != null) {
			// search for query string on metadata name field and content
			// where metadata name = metaDataToSearch
			Query queryMetadata = new TermQuery(new Term(IndexingConstants.METADATA, metaDataToSearch));
			andQuery.add(queryMetadata, BooleanClause.Occur.MUST);
		}

		Query tenantQuery = new TermQuery(new Term(IndexingConstants.TENANT, getTenant()));
		andQuery.add(tenantQuery, BooleanClause.Occur.MUST);

		logger.debug("Searching for: " + andQuery.toString());
		int hitsPerPage = 50;

		// Collect enough docs to show 5 pages
		TopScoreDocCollector collector = TopScoreDocCollector.create(5 * hitsPerPage, false);
		searcher.search(andQuery, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		objectsToReturn.put("hits", hits);

		if (hits != null && isSummaryEnabled) {
			Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(andQuery));
			for (int i = 0; i < hits.length; i++) {
				ScoreDoc hit = hits[i];
				Document doc = searcher.doc(hit.doc);
				String biobjId = doc.get(IndexingConstants.BIOBJ_ID);
				String summary = " ";
				if (highlighter != null) {
					String[] summaries;
					try {
						Integer idobj = (Integer.valueOf(biobjId));

						String contentToSearchOn = fillSummaryText(idobj);
						summaries = highlighter
								.getBestFragments(new StandardAnalyzer(Version.LUCENE_CURRENT), IndexingConstants.CONTENTS, contentToSearchOn, 3);

						StringBuffer summaryBuffer = new StringBuffer();
						if (summaries.length > 0) {
							summaryBuffer.append(summaries[0]);
						}
						for (int j = 1; j < summaries.length; j++) {
							summaryBuffer.append(" ... ");
							summaryBuffer.append(summaries[j]);
						}
						summary = summaryBuffer.toString();
						// get only a portion of summary
						if (summary.length() > 101) {
							summary = summary.substring(0, 100);
							summary += "...";
						}
						objectsToReturn.put(biobjId, summary);
					} catch (InvalidTokenOffsetsException e) {
						logger.error(e.getMessage(), e);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}

				}
			}
		}

		int numTotalHits = collector.getTotalHits();
		logger.info(numTotalHits + " total matching documents");

		logger.debug("OUT");
		monitor.stop();
		return objectsToReturn;

	}

	private static String getTenant() {
		// looks in thread
		return TenantManager.getTenant().getName();
	}

	private static String cleanQueryString(String queryString) {
		String result = queryString.replace('_', ' ');
		if (result.startsWith("*") || result.startsWith("?")) {
			result = result.substring(1, result.length());
		}
		if (result.endsWith(" *")) {
			result = result.substring(0, result.length() - 2);
		}
		return result.trim();
	}
}
