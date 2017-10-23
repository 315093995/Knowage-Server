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
package it.eng.spagobi.analiticalmodel.document.dao;

import it.eng.spago.error.EMFErrorSeverity;
import it.eng.spago.error.EMFUserError;
import it.eng.spagobi.analiticalmodel.document.bo.Snapshot;
import it.eng.spagobi.analiticalmodel.document.metadata.SbiObjects;
import it.eng.spagobi.analiticalmodel.document.metadata.SbiSnapshots;
import it.eng.spagobi.commons.dao.AbstractHibernateDAO;
import it.eng.spagobi.commons.dao.SpagoBIDAOException;
import it.eng.spagobi.commons.metadata.SbiBinContents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class SnapshotDAOHibImpl extends AbstractHibernateDAO implements ISnapshotDAO {

	/* (non-Javadoc)
	 * @see it.eng.spagobi.analiticalmodel.document.dao.ISnapshotDAO#deleteSnapshot(java.lang.Integer)
	 */
	@Override
	public void deleteSnapshot(Integer idSnap) throws EMFUserError {
		Session aSession = null;
		Transaction tx = null;
		try {
			aSession = getSession();
			tx = aSession.beginTransaction();
			SbiSnapshots hibSnapshot = (SbiSnapshots)aSession.load(SbiSnapshots.class, idSnap);
			SbiBinContents hibBinCont = hibSnapshot.getSbiBinContents();
			aSession.delete(hibSnapshot);
			aSession.delete(hibBinCont);
			tx.commit();
		} catch (HibernateException he) {
			logException(he);
			if (tx != null)
				tx.rollback();
			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}
	}




	/* (non-Javadoc)
	 * @see it.eng.spagobi.analiticalmodel.document.dao.ISnapshotDAO#getSnapshots(java.lang.Integer)
	 */
	@Override
	public List getSnapshots(Integer idBIObj)  throws EMFUserError {
		List snaps = new ArrayList();
		Session aSession = null;
		Transaction tx = null;
		try {
			aSession = getSession();
			tx = aSession.beginTransaction();
			//String hql = "from SbiSnapshots ss where ss.sbiObject.biobjId = " + idBIObj;
			String hql = "from SbiSnapshots ss where ss.sbiObject.biobjId = ?" ;

			Query query = aSession.createQuery(hql);
			query.setInteger(0, idBIObj.intValue());

			List hibSnaps = query.list();
			Iterator iterHibSnaps = hibSnaps.iterator();
			while(iterHibSnaps.hasNext()) {
				SbiSnapshots hibSnap = (SbiSnapshots)iterHibSnaps.next();
				Snapshot snap = toSnapshot(hibSnap);
				snaps.add(snap);
			}
			tx.commit();
		} catch (HibernateException he) {
			logException(he);
			if (tx != null)
				tx.rollback();
			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}
		return snaps;
	}

	@Override
	public List getSnapshotsForSchedulationAndDocument(Integer idBIObj, String scheduler)  throws EMFUserError {
		List snaps = new ArrayList();
		Session aSession = null;
		Transaction tx = null;
		try {
			aSession = getSession();
			tx = aSession.beginTransaction();
			//String hql = "from SbiSnapshots ss where ss.sbiObject.biobjId = " + idBIObj;
			String hql = "from SbiSnapshots ss where ss.sbiObject.biobjId = ? and ss.scheduler = ?" ;

			Query query = aSession.createQuery(hql);
			query.setInteger(0, idBIObj.intValue());
			query.setString(1, scheduler);

			List hibSnaps = query.list();
			Iterator iterHibSnaps = hibSnaps.iterator();
			while(iterHibSnaps.hasNext()) {
				SbiSnapshots hibSnap = (SbiSnapshots)iterHibSnaps.next();
				Snapshot snap = toSnapshot(hibSnap);
				snaps.add(snap);
			}
			tx.commit();
		} catch (HibernateException he) {
			logException(he);
			if (tx != null)
				tx.rollback();
			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}
		return snaps;
	}


	/* (non-Javadoc)
	 * @see it.eng.spagobi.analiticalmodel.document.dao.ISnapshotDAO#saveSnapshot(byte[], java.lang.Integer, java.lang.String, java.lang.String)
	 */
	@Override
	public void saveSnapshot(byte[] content,
			Integer idBIObj,
			String name,
			String description,
			String contentType,
			long schedulationStart,
			String schedulerName,
			String schedulationName,
			int sequence) throws EMFUserError {
		Session aSession = null;
		Transaction tx = null;
		try {
			aSession = getSession();
			tx = aSession.beginTransaction();
			SbiObjects hibBIObject = (SbiObjects) aSession.load(SbiObjects.class, idBIObj);
			SbiBinContents hibBinContent = new SbiBinContents();
			hibBinContent.setContent(content);
			updateSbiCommonInfo4Insert(hibBinContent);
			Integer idBin = (Integer)aSession.save(hibBinContent);
			// recover the saved binary hibernate object
			hibBinContent = (SbiBinContents) aSession.load(SbiBinContents.class, idBin);
			// store the object note
			SbiSnapshots hibSnap = new SbiSnapshots();
			hibSnap.setCreationDate(new Date());
			hibSnap.setDescription(description);
			hibSnap.setName(name);
			hibSnap.setSbiBinContents(hibBinContent);
			hibSnap.setSbiObject(hibBIObject);
			hibSnap.setContentType(contentType);
			hibSnap.setSchedulation(schedulationName);
			hibSnap.setScheduler(schedulerName);
			hibSnap.setSchedulationStartLong(schedulationStart);
			hibSnap.setSequence(sequence);

			updateSbiCommonInfo4Insert(hibSnap);
			aSession.save(hibSnap);
			tx.commit();
		} catch (HibernateException he) {
			logException(he);
			if (tx != null)
				tx.rollback();
			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}
	}



	private Snapshot toSnapshot(SbiSnapshots hibSnap) {
		Snapshot snap = new Snapshot();
		snap.setBiobjId(hibSnap.getSbiObject().getBiobjId());
		snap.setBinId(hibSnap.getSbiBinContents().getId());
		snap.setDateCreation(hibSnap.getCreationDate());
		snap.setDescription(hibSnap.getDescription());
		snap.setId(hibSnap.getSnapId());
		snap.setName(hibSnap.getName());
		snap.setContentType(hibSnap.getContentType());
		snap.setSchedulation(hibSnap.getSchedulation());
		snap.setSequence(hibSnap.getSequence());
		snap.setSchedulationStartDate(hibSnap.getSchedulationStartDate());
		snap.setScheduler(hibSnap.getScheduler());
		snap.setSchedulationStartDate(hibSnap.getSchedulationStartDate());
		return snap;
	}




	/* (non-Javadoc)
	 * @see it.eng.spagobi.analiticalmodel.document.dao.ISnapshotDAO#loadSnapshot(java.lang.Integer)
	 */
	@Override
	public Snapshot loadSnapshot(Integer idSnap) throws EMFUserError {
		Snapshot snap = null;
		Session aSession = null;
		Transaction tx = null;
		try {
			aSession = getSession();
			tx = aSession.beginTransaction();
			SbiSnapshots hibSnap = (SbiSnapshots)aSession.load(SbiSnapshots.class, idSnap);
			snap = toSnapshot(hibSnap);
			tx.commit();
		} catch (HibernateException he) {
			logException(he);
			if (tx != null)
				tx.rollback();
			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}
		return snap;
	}



	public String loadSnapshotSchedulation(Integer idSnap) throws EMFUserError {

		Session aSession = null;

		try {
			aSession = getSession();

			SbiSnapshots hibSnap = (SbiSnapshots)aSession.load(SbiSnapshots.class, idSnap);
			return hibSnap.getScheduler();

		} catch (HibernateException he) {
			logException(he);

			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}

	}

	@Override
	public Snapshot getLastSnapshot(Integer idBIObj) throws EMFUserError {
		Session aSession = null;
		Transaction tx = null;
		Snapshot snap = null;
		try {
			aSession = getSession();
			tx = aSession.beginTransaction();
			//String hql = "from SbiSnapshots ss where ss.sbiObject.biobjId = " + idBIObj;
			String hql = "from SbiSnapshots ss where ss.sbiObject.biobjId = ? and ss.creationDate = (select max(s.creationDate) from SbiSnapshots s where s.sbiObject.biobjId = ?)" ;

			Query query = aSession.createQuery(hql);
			query.setInteger(0, idBIObj.intValue());
			query.setInteger(1, idBIObj.intValue());

			SbiSnapshots hibSnap =(SbiSnapshots)query.uniqueResult();

			if(hibSnap != null) {
				snap = toSnapshot(hibSnap);

			}
			tx.commit();
		} catch (HibernateException he) {
			logException(he);
			if (tx != null)
				tx.rollback();
			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}
		return snap;
	}


	public List<Snapshot> getLastSnapshotsBySchedulation(String schedulationName, boolean collate)  throws EMFUserError {
		 Map<String, Map<Integer,List<Snapshot>>> all =  getSnapshotsBySchedulation(schedulationName, collate);
		 List<Snapshot> last = null;
		 Date lastDate = null;

		 Iterator<String> iter1 = all.keySet().iterator();
		 while (iter1.hasNext()) {
			String aKey = iter1.next();
			Map<Integer,List<Snapshot>> aMap = all.get(aKey);
			Iterator<Integer> iter2 = aMap.keySet().iterator();
			while (iter2.hasNext()) {
				Integer aInteger = iter2.next();
				List<Snapshot> snapshots =  aMap.get(aInteger);
				if(snapshots.size()>0){
					Date creationDate = snapshots.get(0).getDateCreation();
					if(lastDate==null || creationDate.compareTo(lastDate)>0){
						lastDate = creationDate;
						last = snapshots;
					}
				}
			}
		}

		 return last;
	}

	public Map<String, Map<Integer,List<Snapshot>>> getSnapshotsBySchedulation(String schedulationName, boolean collate)  throws EMFUserError {
		Map<String, Map<Integer,List<Snapshot>>> snaps = new HashMap<String, Map<Integer,List<Snapshot>>>() ;
		List<List<Snapshot>> documentLIstLIst = new ArrayList<List<Snapshot>>();// supporting list that is the copy of the lists in snaps. it is used to fascicolate

		Session aSession = null;


		try {
			aSession = getSession();

			String hql = "from SbiSnapshots ss where ss.scheduler = ? order by ss.sbiObject.biobjId" ;

			Query query = aSession.createQuery(hql);
			query.setString(0, schedulationName);

			List hibSnaps = query.list();
			Iterator iterHibSnaps = hibSnaps.iterator();

			while(iterHibSnaps.hasNext()) {
				//the list is sorted by schedulation date and grouped by document id
				//we are interested to get oldest schedulation for each document
				//with same schedulation name
				SbiSnapshots hibSnap = (SbiSnapshots)iterHibSnaps.next();
				Snapshot snap = toSnapshot(hibSnap);

				String schedulation =  hibSnap.getSchedulation();

				Map<Integer,List<Snapshot>> snapForSchedulation = snaps.get(schedulation);
				if(snapForSchedulation==null){
					snapForSchedulation = new HashMap<>();
					snaps.put(schedulation, snapForSchedulation);
				}

				Integer schedulationTime = hibSnap.getSchedulationStartDate();
				List<Snapshot> snapForSchedulationAndTime =snapForSchedulation.get(schedulationTime);
				if(snapForSchedulationAndTime==null){
					snapForSchedulationAndTime = new ArrayList<Snapshot>();
					snapForSchedulation.put(schedulationTime, snapForSchedulationAndTime);
					documentLIstLIst.add(snapForSchedulationAndTime);
				}

				snapForSchedulationAndTime.add(snap);
			}


			if(collate){
				collateSnapshot(documentLIstLIst);
			}



		} catch (HibernateException he) {
			logException(he);
			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}
		return snaps;
	}

	/**
	 * Collate snapshots. Suppose we have doc1,doc2 with a parameter that can have 3 values (a,b,c)
	 * schedulation iterate over this 3 values. The normla result will be
	 * doc1a
	 * doc1b
	 * doc1c
	 * doc2a
	 * doc2b
	 * doc2c
	 * This method transforms the result in thsi way:
	 * doc1a
	 * doc2a
	 * doc1b
	 * doc2b
	 * doc1c
	 * doc2c
	 * @param documentLIstLIst
	 */
	private void collateSnapshot(List<List<Snapshot>> documentLIstLIst){
		for (Iterator iterator = documentLIstLIst.iterator(); iterator.hasNext();) {
			List<Snapshot> aLits = (List<Snapshot>) iterator.next();
			Map<Integer, List<Snapshot>> documetSnapMap = new TreeMap<Integer, List<Snapshot>>();
			for (Iterator iterator2 = aLits.iterator(); iterator2.hasNext();) {
				Snapshot snapshot = (Snapshot) iterator2.next();
				List<Snapshot> listOfDOc = documetSnapMap.get(snapshot.getBiobjId());
				if(listOfDOc==null){
					listOfDOc = new ArrayList<Snapshot>();
					documetSnapMap.put(snapshot.getBiobjId(), listOfDOc);
				}
				listOfDOc.add(snapshot);
			}

			List<Snapshot> sortedList = new ArrayList<Snapshot>();

			Collection<List<Snapshot>> documentsSnap = documetSnapMap.values();
			int documentListSize =-1;
			for (Iterator iterator2 = documentsSnap.iterator(); iterator2.hasNext();) {
				List<Snapshot> list = (List<Snapshot>) iterator2.next();
				if(documentListSize>=0 && list.size()!=documentListSize){
					//logger.error("Can not merge using FASCICOLA if the number of snapshot of each document is not the same");
					throw new SpagoBIDAOException("snap.size.error");
				}
				if(documentListSize<0){
					documentListSize = list.size();
				}

			}
			int index=0;
			while(index<documentListSize){
				for (Iterator iterator2 = documentsSnap.iterator(); iterator2.hasNext();) {
					List<Snapshot> list = (List<Snapshot>) iterator2.next();
					sortedList.add(list.get(index));
				}
				index++;
			}
			aLits.clear();
			aLits.addAll(sortedList);
		}
	}



}
