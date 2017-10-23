angular.module("cockpitModule").service("cockpitModule_datasetServices",function(sbiModule_translate,sbiModule_restServices,cockpitModule_template, $filter, $q, $mdPanel,cockpitModule_widgetSelection,cockpitModule_properties,cockpitModule_utilstServices, $rootScope,sbiModule_messaging){
	var ds=this;

	this.datasetList=[];
	this.datasetMapById={};
	this.datasetMapByLabel={};

	this.infoColumns = [];

	this.isDatasetFromTemplateLoaded = false;

	this.loadDatasetsFromTemplate=function(){
		var def=$q.defer();
		if(!ds.isDatasetFromTemplateLoaded){
			var dsIds = [];
			angular.forEach(cockpitModule_template.configuration.datasets, function(item){
				this.push(item.dsId);
			}, dsIds);

			sbiModule_restServices.restToRootProject();
			sbiModule_restServices.promiseGet("2.0/datasets", "", "asPagedList=true&seeTechnical=TRUE&ids=" + dsIds.join())
			.then(function(response){
				for(var i in response.data.item){
					var dataset = response.data.item[i];
					ds.datasetList.push(dataset);
					ds.datasetMapById[dataset.id.dsId] = dataset;
					ds.datasetMapByLabel[dataset.label] = dataset;
				}

				sbiModule_restServices.restToRootProject();
				sbiModule_restServices.promiseGet("1.0/datasets","?ids=" + dsIds.join())
				.then(function(response){
					angular.forEach(ds.datasetList, function(dsv2){
						for(var i=0; i<response.data.root.length; i++){
							var dsv1=response.data.root[i];
							if(dsv2.id.dsId == dsv1.id){
								dsv2.isRealtime = dsv1.isRealtime;
								for(var i2 in dsv2.parameters){
									for(var i1 in dsv1.pars){
										if(dsv2.parameters[i2].name == dsv1.pars[i1].name){
											dsv2.parameters[i2].multiValue = dsv1.pars[i1].multiValue;
											break;
										}
									}
								}
								break;
							}
						}
					});

					ds.initNearRealTimeValues(ds.datasetList);
					ds.checkForDSChange();
					cockpitModule_widgetSelection.getAssociations(cockpitModule_properties.HAVE_SELECTIONS_OR_FILTERS,undefined,def);

					ds.isDatasetFromTemplateLoaded = true;
					def.resolve();
				},function(response){
					sbiModule_restServices.errorHandler(response.data,"");
					def.reject();
				});

			},function(response){
				sbiModule_restServices.errorHandler(response.data,"");
				def.reject();
			});
		}else{
			def.resolve();
		}
		return def.promise;
	};

	this.isDatasetListLoaded = false;

	this.loadDatasetList=function(){
		var def=$q.defer();
		if(!ds.isDatasetListLoaded){
			sbiModule_restServices.restToRootProject();
			sbiModule_restServices.promiseGet("2.0/datasets", "", "asPagedList=true&seeTechnical=TRUE")
			.then(function(response){
				var allDatasets = response.data.item;

				var newDatasets = [];
				angular.forEach(allDatasets,function(item){
					var found = false;
					var itemId = item.id;
					for(var i=0; i<ds.datasetList.length; i++){
						var currId = ds.datasetList[i].id;
						if(itemId.dsId == currId.dsId
								&& itemId.versionNum == currId.versionNum
								&& itemId.organization == currId.organization){
							found = true;
							break;
						}
					}
					if(!found){
						this.push(item);
					}
				}, newDatasets);

				ds.initNearRealTimeValues(newDatasets);

				for(var i in newDatasets){
					var dataset = newDatasets[i];
					ds.datasetList.push(dataset);
					ds.datasetMapById[dataset.id.dsId] = dataset;
					ds.datasetMapByLabel[dataset.label] = dataset;
				}

				sbiModule_restServices.restToRootProject();
				sbiModule_restServices.promiseGet("1.0/datasets","")
				.then(function(response){
					angular.forEach(ds.datasetList, function(dsv2){
						if(dsv2.isRealtime == undefined){
							for(var i=0; i<response.data.root.length; i++){
								var dsv1=response.data.root[i];
								if(dsv2.id.dsId == dsv1.id){
									dsv2.isRealtime = dsv1.isRealtime;
									for(var i2 in dsv2.parameters){
										for(var i1 in dsv1.pars){
											if(dsv2.parameters[i2].name == dsv1.pars[i1].name){
												dsv2.parameters[i2].multiValue = dsv1.pars[i1].multiValue;
												break;
											}
										}
									}
									break;
								}
							}
						}
					});

					ds.isDatasetListLoaded = true;
					def.resolve();
				},function(response){
					sbiModule_restServices.errorHandler(response.data,"");
					def.reject();
				});
			},function(response){
				sbiModule_restServices.errorHandler(response.data,"");
				def.reject();
			});
		}else{
			def.resolve();
		}
		return def.promise;
	};

	this.initNearRealTimeValues=function(datasets){
		for(var i=0; i < datasets.length; i++){
			var dataset = datasets[i];
			if(dataset.useCache == undefined){
				dataset.useCache = true;
			}
			if(dataset.frequency == undefined){
				dataset.frequency = 0;
			}
		}
	}

	this.checkForDSChange=function(){
		var changed=[];
		var removedDatasetParams=[];

		angular.forEach(cockpitModule_template.configuration.datasets,function(item){
			var actualDs=ds.getDatasetById(item.dsId);
			var addedParams=[];
			var removedParams=[];

			//check if label changed
			if(!angular.equals(actualDs.label,item.dsLabel)){
				var oldlab=angular.copy(item.dsLabel);
				//update the label of dataset
				this.push(sbiModule_translate.load("sbi.generic.label")+": "+item.dsLabel+" -> "+actualDs.label)
				item.dsLabel=actualDs.label;

				//update the dataset label in the associations
				for(var i=0;i<cockpitModule_template.configuration.associations.length;i++){
					var ass=cockpitModule_template.configuration.associations[i];
					if(ass.description.search("="+oldlab+"\.")!=-1){
						ass.description=ass.description.replace("="+oldlab+"\.", "="+item.dsLabel+".");
						for(var f=0;f<ass.fields.length;f++){
							if(angular.equals(ass.fields[f].store,oldlab)){
								ass.fields[f].store=item.dsLabel;
								break;
							}
						}
					}
				}

				//update the dataset in the filters
				if(cockpitModule_template.configuration.filters.hasOwnProperty(oldlab)){
					cockpitModule_template.configuration.filters[item.dsLabel]=cockpitModule_template.configuration.filters[oldlab];
					delete cockpitModule_template.configuration.filters[oldlab];
				}

				//update the dataset label in the aggregations
				for(var i=0;i<cockpitModule_template.configuration.aggregations.length;i++){
					var aggr=cockpitModule_template.configuration.aggregations[i];
					//check if this aggregations have this ds
					var ind=aggr.datasets.indexOf(oldlab);
					if(ind!=-1){
						//alter the label in dstasets variable
						aggr.datasets[ind]=item.dsLabel;
						//alter the label in selections
						var alteration={add:{},remove:[]}
						angular.forEach(aggr.selection,function(selVal,selInd){
							if(selInd.startsWith(oldlab)){
								this.add[item.dsLabel+"."+selInd.split(".")[1]]=selVal;
								this.remove.push(selInd);
							}
						},alteration)

						angular.forEach(alteration.add,function(val,ind){
							this[ind]=val;
						},aggr.selection)

						angular.forEach(alteration.remove,function(val){
							delete this[val];
						},aggr.selection)
					}
				}
			}

			//check if name changed
			if(!angular.equals(actualDs.name,item.name)){
				//update the name of dataset
				this.push(sbiModule_translate.load("sbi.generic.name")+": "+item.name+" -> "+actualDs.name)
				item.name=actualDs.name;
			}

			//check if parameters changed
			removedDatasetParams[item.dsLabel] = [];
			if(actualDs.parameters!=undefined && item.parameters!=undefined){

				//check added params
				for(var i=0; i<actualDs.parameters.length; i++){
					var paramName = actualDs.parameters[i].name;
					if(!item.parameters.hasOwnProperty(paramName)){
						addedParams.push(paramName);
						this.push(sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.addedParameter")
								.replace("{0}", "<b>" + item.dsLabel + ".$P{" + paramName + "}</b>"));
					}
				}

				//check removed params
				for (var paramName in item.parameters) {
				    if (item.parameters.hasOwnProperty(paramName)) {
				    	var removed = true;
				    	for(var i=0; i<actualDs.parameters.length; i++){
							if(actualDs.parameters[i].name == paramName){
								removed = false;
								break;
							}
						}
				    	if(removed){
				    		removedParams.push(paramName);
				    		removedDatasetParams[item.dsLabel].push(paramName);
				    		this.push(sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.removedParameter")
				    				.replace("{0}", "<b>" + item.dsLabel + ".$P{" + paramName + "}</b>"));
				    	}
				    }
				}
			}

			//fix template parameters
			for(var i=0; i<addedParams.length; i++){
				var addedParam = addedParams[i];
				item.parameters[addedParam] = null;
			}
			for(var i=0; i<removedParams.length; i++){
				var removedParam = removedParams[i];
				delete item.parameters[removedParam];
			}
		},changed)

		var modifiedAssociations = 0;
		angular.forEach(cockpitModule_template.configuration.associations,function(item){
			//fix fields & description
			var modifiedAssociation = 0;

			for(var i=item.fields.length-1; i>=0; i--){
				var field = item.fields[i];
				var paramName = (field.column.startsWith("$P{") && field.column.endsWith("}")) ? field.column.substring(3, field.column.length - 1) : field.column;
				if(field.type == "dataset" && removedDatasetParams[field.store].indexOf(paramName) > -1){
					item.description = item.description.replace(field.store + "." + field.column, "");
					if(item.description.startsWith("=")){
						item.description = item.description.substring(1);
					}else if(item.description.endsWith("=")){
						item.description = item.description.substring(0, str.length - 1);
					}else{
						item.description = item.description.replace("==", "=");
					}

					item.fields.splice(i, 1);

					modifiedAssociation = 1;
				}
			}

			modifiedAssociations += modifiedAssociation;
		},changed)

		//remove degenerated associations
		var removedAssociations = 0;
		for(var i=cockpitModule_template.configuration.associations.length-1; i>=0; i--){
			var association=cockpitModule_template.configuration.associations[i];
			if(association.fields.length < 2){
				cockpitModule_template.configuration.associations.splice(i, 1);
				removedAssociations++;
				modifiedAssociations--;
			}
		}

		if(modifiedAssociations > 0){
			changed.push(sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.modifiedAssociations")
					.replace("{0}", "" + modifiedAssociations));
		}

		if(removedAssociations > 0){
			changed.push(sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.removedAssociations")
					.replace("{0}", "" + removedAssociations));
		}

		angular.forEach(cockpitModule_template.sheets,function(sheet){
			angular.forEach(sheet.widgets,function(widget){
				if(widget.dataset && widget.dataset.dsId){
					var actualDs=ds.getDatasetById(widget.dataset.dsId);
					angular.forEach(widget.content.columnSelectedOfDataset,function(widgetColumn){
						var isWidgetColumnMatching = false;
						for(var i = 0; i < actualDs.metadata.fieldsMeta.length; i++){
							if(actualDs.metadata.fieldsMeta[i].name == widgetColumn.name || widgetColumn.formula){
								isWidgetColumnMatching = true;
								break;
							}
						}
						if(!isWidgetColumnMatching){
							this.push(sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.unabletoloadcolumnforwidget")
									.replace("{0}", "<b>" + actualDs.name + "." + widgetColumn.alias + "</b>")
									.replace("{1}", "<b>" + widget.content.name + "</b>"));
						}
					}, changed);
				}
			});
		});

		if(changed.length>0){
			changed.push(sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.checkconfigandsave"));
			sbiModule_messaging.showErrorMessage(changed.join("<br>"), sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.title"));
		}
	}

	this.getDatasetList=function(){
		return angular.copy(ds.datasetList);
	}

	//return a COPY of dataset with specific id or null
	this.getDatasetById=function(dsId){
		return angular.copy(ds.datasetMapById[dsId]);
	}

	//return a COPY of dataset with specific label or null
	this.getDatasetByLabel=function(dsLabel){
		return angular.copy(ds.datasetMapByLabel[dsLabel]);
	}


	//return a COPY of avaiable dataset with specific id or null
	this.getAvaiableDatasetById=function(dsId){
		var dsAvList=ds.getAvaiableDatasets();
		for(var i=0;i<dsAvList.length;i++){
			if(angular.equals(dsAvList[i].id.dsId,dsId)){
				var tmpDS={};
				angular.copy(dsAvList[i],tmpDS);
				return tmpDS;
			}
		}
	}

	//return a COPY of avaiable dataset with specific label or null
	this.getAvaiableDatasetByLabel=function(dsLabel){
		var dsAvList=ds.getAvaiableDatasets();
		for(var i=0;i<dsAvList.length;i++){
			if(angular.equals(dsAvList[i].label,dsLabel)){
				var tmpDS={};
				angular.copy(dsAvList[i],tmpDS);
				return tmpDS;
			}
		}
	}

	this.getLabelDatasetsUsed = function(){
		var string = "";
		for(var i=0;i<cockpitModule_template.sheets.length;i++){
			var sheet = cockpitModule_template.sheets[i];
			for(var j=0;j<sheet.widgets.length;j++){
				var widget = sheet.widgets[j];
				if(widget.dataset !=undefined){
					var ds = this.getDatasetById(widget.dataset.dsId)
					//array.push(ds.label);
					string = string + ds.label;
					if(j<sheet.widgets.length-1){
						string = string + ","
					}
				}
			}
		}

		return string;
	}

	this.getDatasetsUsed = function(){
		var array = [];
		var result = {};
		for(var i=0;i<cockpitModule_template.sheets.length;i++){
			var sheet = cockpitModule_template.sheets[i];
			for(var j=0;j<sheet.widgets.length;j++){
				var widget = sheet.widgets[j];
				if(widget.dataset !=undefined){
					array.push(widget.dataset.dsId);
				}
			}
		}

		return array;

	}



	//return a list of avaiable dataset with all parameters
	this.getAvaiableDatasets=function(){
		var fad=[];
		for(var i=0;i<cockpitModule_template.configuration.datasets.length;i++){
			var dataset = cockpitModule_template.configuration.datasets[i];
			var dsIl=ds.getDatasetById(dataset.dsId);
			if(dsIl!=undefined){
				dsIl.useCache = (dataset.useCache == undefined && !dsIl.isRealtime) ? true : dataset.useCache;
				dsIl.frequency = (dataset.frequency == undefined) ? 0 : dataset.frequency;
				if(dataset.parameters!=undefined){
					angular.forEach(dsIl.parameters,function(item){
						item.value=dataset.parameters[item.name];
					})
				}
				fad.push(dsIl);
			}else{
				console.error("ds with id "+dataset.dsId +" not found;")
			}
		}

		return fad;
	}

	//get in input a full list of avaiable dataset and save only the attributes needed in template
	this.setAvaiableDataset=function(adl){
		angular.copy([],cockpitModule_template.configuration.datasets);
		var tmpList=[];
		for(var i=0;i<adl.length;i++){
			ds.addAvaiableDataset(adl[i])
		}
	}

	this.addAvaiableDataset=function(avDataset){
		var tmpDS={};
		tmpDS.dsId=avDataset.id.dsId;
		tmpDS.name=avDataset.name;
		tmpDS.dsLabel=avDataset.label;
		tmpDS.useCache = (avDataset.useCache == undefined) ? true : avDataset.useCache;
		tmpDS.frequency = (avDataset.frequency == undefined) ? 0 : avDataset.frequency;
		tmpDS.parameters={};
		if(avDataset.parameters!=undefined){
			for(var p=0;p<avDataset.parameters.length;p++){
				tmpDS.parameters[avDataset.parameters[p].name]=avDataset.parameters[p].value;
			}
		}

		cockpitModule_template.configuration.datasets.push(tmpDS);
	}

	this.getDatasetParameters=function(dsId){
		var param={};
		for(var i=0;i<cockpitModule_template.configuration.datasets.length;i++){
			if(angular.equals(cockpitModule_template.configuration.datasets[i].dsId,dsId)){
				angular.forEach(cockpitModule_template.configuration.datasets[i].parameters,function(item,key){
						this[key]=cockpitModule_utilstServices.getParameterValue(item);
				},param)
			}
		}

		var datasetLabel=ds.getDatasetById(dsId).label;
		var selections=cockpitModule_widgetSelection.getCurrentSelections(datasetLabel);
		if(selections!=undefined && selections.hasOwnProperty(datasetLabel)){
			for(var parName in selections[datasetLabel]){
				if(parName.startsWith("$P{") && parName.endsWith("}")){
					var parValue=selections[datasetLabel][parName];
					if(parValue!=undefined){
						var finalP=[];
						angular.forEach(parValue,function(item){
							this.push(item.substring(2,item.length-2))
						},finalP)
						param[parName.substring(3,parName.length-1)]=finalP.join(",");
					}
				}
			}
		}
		return param;
	}

	//TODO missing maxRows
	this.loadDatasetRecordsById = function(dsId, page, itemPerPage,columnOrdering, reverseOrdering, ngModel){
		//after retry LabelDataset by Id call service for data
		var dataset = this.getAvaiableDatasetById(dsId);
		var deferred = $q.defer();
		var params="";

		var aggregation = cockpitModule_widgetSelection.getAggregation(ngModel,dataset,columnOrdering, reverseOrdering);

		// apply sorting column & order
		if(ngModel.settings && ngModel.settings.sortingColumn && ngModel.settings.sortingColumn!=""){
			var isSortingAlreadyDefined = false;

			// check if a sorting order is alredy defined on categories
			for(var i=0; i<aggregation.categories.length; i++){
				var category = aggregation.categories[i];
				if(category.orderType.trim() != ""){
					isSortingAlreadyDefined = true;
					break;
				}
			}

			// check if a sorting order is alredy defined on measures
			if(!isSortingAlreadyDefined){
				for(var i=0; i<aggregation.measures.length; i++){
					var measure = aggregation.measures[i];
					if(measure.orderType.trim() != ""){
						isSortingAlreadyDefined = true;
						break;
					}
				}
			}

			if(!isSortingAlreadyDefined){
				var isSortingApplied = false;

				// apply sorting order on categories
				for(var i=0; i<aggregation.categories.length; i++){
					var category = aggregation.categories[i];
					if(category.columnName == ngModel.settings.sortingColumn && category.orderType == ""){
						category.orderType = ngModel.settings.sortingOrder;
						isSortingApplied = true;
						break;
					}
				}

				// apply sorting order on measures
				if(!isSortingApplied){
					for(var i=0; i<aggregation.measures.length; i++){
						var measure = aggregation.measures[i];
						if(measure.columnName == ngModel.settings.sortingColumn && measure.orderType == ""){
							measure.orderType = ngModel.settings.sortingOrder;
							isSortingApplied = true;
							break;
						}
					}
				}

				// add a new category if necessary
				if(!isSortingApplied){
					var newCategory = {
							alias : ngModel.settings.sortingColumn,
							columnName : ngModel.settings.sortingColumn,
							id : ngModel.settings.sortingColumn,
							orderType : ngModel.settings.sortingOrder
					}
					aggregation.categories.push(newCategory);
				}
			}
		}

		var aggr = encodeURIComponent(JSON.stringify(aggregation))
		.replace(/'/g,"%27")
		.replace(/"/g,"%22");

		var parameters = ds.getDatasetParameters(dsId);
		var parameterErrors = [];
		for (var parameter in parameters) {
			if (parameters.hasOwnProperty(parameter)){
				for(var i in dataset.parameters){
					if(dataset.parameters[i].name==parameter){
						var valueCount = parameters[parameter].split(",").length;
						if(!dataset.parameters[i].multiValue && valueCount > 1){
							var parameterError = sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.unabletoapplyvaluestosinglevalueparameter")
									.replace("{0}", "<b>" + valueCount + "</b>")
									.replace("{1}", "<b>" + dataset.name + ".$P{" + parameter + "}</b>")
							parameterErrors.push(parameterError);
						}
						break;
					}
				}
			}
		}
		if(parameterErrors.length > 0){
			var title = sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.widget")
					.replace("{0}", "<b>" + ngModel.content.name + "</b>");
			sbiModule_restServices.errorHandler(parameterErrors.join("<br>"), title);
			deferred.reject('Error');
		}

		var parametersString = JSON.stringify(parameters);
		for (var parameter in parameters) {
			if (parameters.hasOwnProperty(parameter) && (parameters[parameter] == null || parameters[parameter] == undefined)) {
				parametersString = parametersString.replace("}" , ", \"" + parameter + "\":null}");
			}
		}
		var par = encodeURIComponent(parametersString)
		.replace(/'/g,"%27")
		.replace(/"/g,"%22");



		// if cross navigation referes to a non present column (and widget is a table ) avoid sending aggregation because all columns are needed
		if(ngModel.type === 'table'){
			// if cross navigation is defined
			if (ngModel.cross != null && ngModel.cross.cross != null && ngModel.cross.cross.column){
				var crossCol = ngModel.cross.cross.column;
				var found = false;
				// if column passed is not among visible ones
				if(aggregation.categories != null){
					for(var i=0;i<aggregation.categories.length && !found;i++){
						var id = aggregation.categories[i].id;
						if(id === crossCol){
							found = true;
						}
					}
					// get all data
					if(found==false){
						aggr = '';
					}
				}
			}
		}


		params =  "?aggregations=" +aggr+"&parameters="+par;

		if(page!=undefined && page>-1 && itemPerPage!=undefined && itemPerPage>-1){
			params = params + "&offset=" + (page * itemPerPage) + "&size=" + itemPerPage;
		}else{
			params = params + "&offset=-1&size=-1";
		}

		if(ngModel.settings && ngModel.settings.summary.enabled){
			var summaryRow = encodeURIComponent(JSON.stringify(ds.getSummaryRow(ngModel)))
				.replace(/'/g,"%27")
				.replace(/"/g,"%22");
			params =  params+"&summaryRow=" +summaryRow;
		}

		if(dataset.useCache==false){
			params+="&nearRealtime=true";
		}

		var dataToSend=angular.copy(cockpitModule_widgetSelection.getCurrentSelections(dataset.label));
		if(Object.keys(dataToSend).length==0){
			dataToSend=cockpitModule_widgetSelection.getCurrentFilters(dataset.label);
		}

		var limitRows;
		if(ngModel.limitRows){
			limitRows = ngModel.limitRows;
		}else if(ngModel.content && ngModel.content.limitRows){
			limitRows = ngModel.content.limitRows;
		}
		if(limitRows != undefined && limitRows.enable && limitRows.rows > 0){
			params += "&limit=" + limitRows.rows;
		}

		var filters;
		if(ngModel.filters){
			filters = ngModel.filters;
		}else if(ngModel.content && ngModel.content.filters){
			filters = ngModel.content.filters;
		}
		if(filters){
			for(var i=0;i<filters.length;i++){
				var filterElement=filters[i];
				var colName=filterElement.colName;
				var type=filterElement.type;

				var filterOperator;
				if(filterElement.filterOperator != undefined){
					filterOperator=filterElement.filterOperator;
				}
				else {
					if(filterElement.filterVals && filterElement.filterVals.length>0){
						filterOperator = "=";
					}
					else{
						filterOperator = "";
					}

				}

				// if type is undefined get it from metadata
				if(type == undefined){
					var found = false;
					for(var j=0; j<dataset.metadata.fieldsMeta.length && !found; j++){
						var metaElement=dataset.metadata.fieldsMeta[j];
						if(metaElement.name == colName){
							filterElement.type = metaElement.type;
							found = true;
						}
					}
				}


				var filterVals=filterElement.filterVals;
				if(filterOperator != ""  //filterVals.length>0
						){
					var values=[];

					// if filterOperator is IN and filterVals has , must split
					if(filterOperator == "IN" && filterVals[0] && filterVals[0].includes(",") ){
						filterVals = filterVals[0].split(",");
					}

					angular.forEach(filterVals, function(item){
						this.push("('" + item + "')");
					}, values);

					if(!dataToSend[dataset.label]){
						dataToSend[dataset.label] = {};
					}
					if(!dataToSend[dataset.label][colName]){
						dataToSend[dataset.label][colName] =
							{
								filterOperator: filterOperator,
								filterVals: values
							}
					}else{
//						dataToSend[dataset.label][colName] = dataToSend[dataset.label][colName].filter(
//								function(elem) { // intersect arrays
//									return values.indexOf(elem) !== -1;
//						}).filter(function (elem, i, c) { // extra step to remove duplicates
//					        return c.indexOf(elem) === i;
//					    });

						values = dataToSend[dataset.label][colName].filter(
								function(elem) { // intersect arrays
									return values.indexOf(elem) !== -1;
						}).filter(function (elem, i, c) { // extra step to remove duplicates
					        return c.indexOf(elem) === i;
					    });

						dataToSend[dataset.label][colName] =
							{
								filterOperator: filterOperator,
								filterVals: values
							}






					}
				}


//				var filterVals=filterElement.filterVals;
//				if(filterVals.length>0){
//					var values=[];
//					angular.forEach(filterVals, function(item){
//						this.push("('" + item + "')");
//					}, values);
//
//					if(!dataToSend[dataset.label]){
//						dataToSend[dataset.label] = {};
//					}
//					if(!dataToSend[dataset.label][colName]){
//						dataToSend[dataset.label][colName] = values;
//					}else{
//						dataToSend[dataset.label][colName] = dataToSend[dataset.label][colName].filter(function(elem) { // intersect arrays
//						    return values.indexOf(elem) !== -1;
//						}).filter(function (elem, i, c) { // extra step to remove duplicates
//					        return c.indexOf(elem) === i;
//					    });
//					}
//				}
			}
		}

		if(ngModel.search
				&& ngModel.search.text && ngModel.search.text!=""
				&& ngModel.search.columns && ngModel.search.columns.length>0){
			var columns = ngModel.search.columns.join(",");
			var searchData = {};
			searchData[columns] = ngModel.search.text;
			var likeSelections = {};
			likeSelections[dataset.label] = searchData;
			params += "&likeSelections=" + encodeURIComponent(JSON.stringify(likeSelections)).replace(/'/g,"%27").replace(/"/g,"%22");
		}

		var dataToSendWithoutParams = {};
		if(dataset.useCache == true || ngModel.updateble == true){
			angular.copy(dataToSend,dataToSendWithoutParams);
			angular.forEach(dataToSendWithoutParams, function(item){
				var paramsToDelete = [];
				for (var property in item) {
				    if (item.hasOwnProperty(property) && property.startsWith("$P{") && property.endsWith("}")) {
				    	paramsToDelete.push(property);
				    }
				}
				angular.forEach(paramsToDelete, function(prop){
					delete item[prop];
				});
			});
		}

		params += "&widgetName=" + encodeURIComponent(ngModel.content.name);

		sbiModule_restServices.restToRootProject();
		sbiModule_restServices.promisePost("2.0/datasets",encodeURIComponent(dataset.label)+"/data"+params,dataToSendWithoutParams)
		.then(function(response){
			if(cockpitModule_properties.DS_IN_CACHE.indexOf(dataset.label)==-1){
				cockpitModule_properties.DS_IN_CACHE.push(dataset.label);
			}
			deferred.resolve(response.data);
		},function(response){
			var regex = /(.*)2.0\/datasets\/(.*)\/data(.*)widgetName=(.*)/g;
			var array = regex.exec(response.config.url);
			var datasetLabel = array[2];
			var widgetName = decodeURIComponent(array[4]);
			var title = sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.widget")
					.replace("{0}", "<b>" + widgetName + "</b>");
			var text = sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.unabletoloaddatafromdataset")
					.replace("{0}", "<b>" + datasetLabel + "</b>")
			text += "<br>";
			text += sbiModule_translate.load("sbi.cockpit.load.datasetsInformation.checkdatasetandwidgetconfig");
			sbiModule_restServices.errorHandler(text, title);
			deferred.reject('Error');
		})

		return deferred.promise;

	}

	this.getSummaryRow = function(ngModel){
		var measures = [];
		var columns = ngModel.content.columnSelectedOfDataset;

		if(columns){
			//create aggregation
			for(var i=0;i<columns.length;i++){
				var col = columns[i];

				if(col.fieldType!="ATTRIBUTE"){
					var obj = {};
					obj["id"] = col.name;
					obj["alias"] = ngModel.type == "table" ? col.aliasToShow : col.alias;
					obj["funct"] = col.funcSummary == undefined? "" : col.funcSummary;
					obj["columnName"] = ngModel.type == "table" ? col.aliasToShow : col.alias;

					measures.push(obj);
				}
			}
		}
		var result = {};
		result["measures"] = measures;
		result["dataset"] = ngModel.dataset.dsId;

		return result;

	}

	this.addDataset=function(attachToElementWithId,container,multiple,autoAdd){
		var deferred = $q.defer();
		var eleToAtt=document.body;
		if(attachToElementWithId!=undefined){
			eleToAtt=angular.element(document.getElementById(attachToElementWithId))
		}

		var config = {
				attachTo: eleToAtt,
				locals :{currentAvaiableDataset:container,multiple:multiple,deferred:deferred},
				controller: function($scope,mdPanelRef,sbiModule_translate,cockpitModule_datasetServices,currentAvaiableDataset,multiple,deferred,$mdDialog){

					$scope.tmpCurrentAvaiableDataset;
					if(multiple){
						tmpCurrentAvaiableDataset=[];
					}else{
						tmpCurrentAvaiableDataset={};
					}

					$scope.multiple=multiple;

					$scope.cockpitDatasetColumn=[{label:"Label",name:"label"},{label:"Name",name:"name" } ];

					$scope.isDatasetListLoaded = false;

					cockpitModule_datasetServices.loadDatasetList().then(function(response){
						var datasetList = cockpitModule_datasetServices.getDatasetList();
						//remove available dataset
						for(var i=0;i<currentAvaiableDataset.length;i++){
							for(var j=0; j<datasetList.length; j++){
								if(angular.equals(currentAvaiableDataset[i].id.dsId, datasetList[j].id.dsId)){
									datasetList.splice(j,1);
									break;
								}
							}
						}
						$scope.datasetList = datasetList;
						$scope.isDatasetListLoaded = true;
					},function(response){
						sbiModule_restServices.errorHandler(response.data,"");
						def.reject();
					});

					$scope.closeDialog=function(){
						mdPanelRef.close();
						$scope.$destroy();
						deferred.reject();
					}

					$scope.saveDataset=function(){
						if(multiple){
							for(var i=0;i<$scope.tmpCurrentAvaiableDataset.length;i++){
								if(autoAdd){
									ds.addAvaiableDataset($scope.tmpCurrentAvaiableDataset[i])
								}else{
									currentAvaiableDataset.push($scope.tmpCurrentAvaiableDataset[i]);
								}
							}

							deferred.resolve(angular.copy($scope.tmpCurrentAvaiableDataset));
							mdPanelRef.close();
							$scope.$destroy();

						}else{
							if($scope.tmpCurrentAvaiableDataset.parameters!=null && $scope.tmpCurrentAvaiableDataset.parameters.length>0){
								//fill the parameter

								 $mdDialog.show({
								      controller: function($scope,sbiModule_translate,parameters){
								    	  $scope.translate=sbiModule_translate;
								    	  $scope.tmpParam=angular.copy(parameters);
								    	  $scope.saveConfiguration=function(){
								    		  $mdDialog.hide($scope.tmpParam);
								    	  }
								    	  $scope.cancelConfiguration=function(){
								    		  $mdDialog.cancel();
								    	  }
								      },
								      templateUrl: baseScriptPath+ '/directives/cockpit-data-configuration/templates/CockpitDataConfigurationDatasetParameterFill.html',
								      clickOutsideToClose:false,
								      parent: mdPanelRef._panelContainer[0].querySelector(".md-panel md-card"),
								      hasBackdrop :true,
								      preserveScope :true,
								      locals:{parameters:$scope.tmpCurrentAvaiableDataset.parameters}
								    })
								    .then(function(data) {
								    	angular.copy(data,$scope.tmpCurrentAvaiableDataset.parameters)
								    	if(autoAdd){
											ds.addAvaiableDataset($scope.tmpCurrentAvaiableDataset)
										}else{
											angular.copy($scope.tmpCurrentAvaiableDataset,currentAvaiableDataset);
										}
										deferred.resolve(angular.copy($scope.tmpCurrentAvaiableDataset));
										mdPanelRef.close();
										$scope.$destroy();

								    }, function() {
								      $scope.status = 'You cancelled the dialog.';
								    });


							}else{
								if(autoAdd){
									ds.addAvaiableDataset($scope.tmpCurrentAvaiableDataset)
								}else{
									angular.copy($scope.tmpCurrentAvaiableDataset,currentAvaiableDataset);
								}
								deferred.resolve(angular.copy($scope.tmpCurrentAvaiableDataset));
								mdPanelRef.close();
								$scope.$destroy();

							}
						}

					}
				},
				disableParentScroll: true,
				templateUrl: baseScriptPath+ '/directives/cockpit-data-configuration/templates/cockpitDataConfigurationDatasetChoice.html',
//				hasBackdrop: true,
				position: $mdPanel.newPanelPosition().absolute().center(),
				trapFocus: true,
				zIndex: 150,
				fullscreen :true,
				clickOutsideToClose: true,
				escapeToClose: false,
				focusOnOpen: false,
				onRemoving :function(){
				}
		};

		$mdPanel.open(config);
		return deferred.promise;
	}

	this.addDatasetInCache = function(datasets){
		var def=$q.defer();
		var dataToSend = [];
		angular.forEach(datasets, function(item){
			var dataset = ds.getAvaiableDatasetByLabel(item);
			if(dataset!=undefined){
				var params ={};
				params.datasetLabel = dataset.label;
				params.aggregation = cockpitModule_widgetSelection.getAggregation(undefined,dataset,undefined, undefined);
				params.parameters = ds.getDatasetParameters(dataset.id.dsId);
				if(dataset.useCache==false){
					params.nearRealtime = true;
				}
				this.push(params);
			}
		},dataToSend)
		sbiModule_restServices.restToRootProject();
		sbiModule_restServices.promisePost("2.0/datasets","addDatasetInCache",dataToSend)
		.then(function(response){
			angular.forEach(datasets, function(item){
				this.push(item);
			},cockpitModule_properties.DS_IN_CACHE);

			def.resolve()
		},function(response){
			sbiModule_restServices.errorHandler(response.data,"");
			def.reject()
		})

		return def.promise;
	}

	this.autodetect=function(attachToElementWithId,tmpAvaiableDatasets,tmpAssociations){
		var deferred = $q.defer();
		var elemToAtt=document.body;
		if(attachToElementWithId!=undefined){
			elemToAtt=angular.element(document.getElementById(attachToElementWithId))
		}

		var config = {
			attachTo: elemToAtt,
			locals :{datasets:tmpAvaiableDatasets,associations:tmpAssociations,deferred:deferred},
			controller: function($scope,mdPanelRef,sbiModule_translate,cockpitModule_datasetServices,datasets,associations,deferred,$mdDialog){

				$scope.translate = sbiModule_translate;

				// table columns
				$scope.cockpitAutodetectColumns=[{label:sbiModule_translate.load("sbi.cockpit.association.editor.wizard.list.autodetect.similarity"),
					name:"___similarity",
					transformer:function(input){return $filter('number')(input * 100, 2) + '%';}
				}];
				angular.forEach(datasets,function(item){
					var column = {label:item.label, name:item.label};
					this.push(column);
				},$scope.cockpitAutodetectColumns);

				// table search columns
				$scope.cockpitAutodetectColumnsSearch=[];
				angular.forEach(datasets,function(item){
					this.push(item.label);
				},$scope.cockpitAutodetectColumnsSearch);

				// table selected row
				$scope.cockpitAutodetectSelectedRow = null;

				$scope.saveAutodetect=function(){
					deferred.resolve(angular.copy($scope.cockpitAutodetectSelectedRow));
					mdPanelRef.close();
					$scope.$destroy();
				}

				$scope.closeDialog=function(){
					mdPanelRef.close();
					$scope.$destroy();
					deferred.reject();
				}

				// Similarity filter management

				$scope.minSimilarity = 0.2;

				$scope.minSimilarityValues = [0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2];
				for(var i=$scope.minSimilarityValues.length-1; i>=0; i--){
					if($scope.minSimilarityValues[i] < $scope.minSimilarity){
						$scope.minSimilarityValues.splice(i, 1);
					}
				}

				$scope.selectedMinSimilarityValue = $scope.minSimilarity;

				$scope.$watch("selectedMinSimilarityValue",function(newValue,oldValue){
		    		  $scope.filterCockpitAutodetectRows(newValue, $scope.selectedMinLengthValue);
				});

				// Length filter management

				$scope.minLength = 2;

				$scope.minLengthValues = [];
				for(var i=$scope.minLength; i<=datasets.length; i++){
					$scope.minLengthValues.unshift(i);
				}

				$scope.selectedMinLengthValue = $scope.minLength;

				$scope.$watch("selectedMinLengthValue",function(newValue,oldValue){
		    		  $scope.filterCockpitAutodetectRows($scope.selectedMinSimilarityValue, newValue);
				});

				// Filtered table model

				$scope.cockpitAutodetectRows = [];
				$scope.cockpitAutodetectFilteredRows = [];
				$scope.showTable = false;

				$scope.filterCockpitAutodetectRows=function(minSimilarity, minLength){
					var rows = [];
					angular.copy($scope.cockpitAutodetectRows, rows);

					for(var i=rows.length-1; i>=0; i--){
						var row = rows[i];
						if(row["___similarity"] < minSimilarity || row["___length"] < minLength){
							rows.splice(i, 1);
						}
					}

					angular.copy(rows, $scope.cockpitAutodetectFilteredRows);
				}

				var datasetNames = {};
				angular.forEach(datasets,function(item){
					var params = {};
					angular.forEach(item.parameters,function(parameter){
						this[parameter.name] = (parameter.value ? parameter.value : parameter.defaultValue);
					},params);
					this[item.label] = params;
				},datasetNames);

				var payload = JSON.stringify(datasetNames);
				sbiModule_restServices.restToRootProject();
				sbiModule_restServices.promisePost("2.0/datasets","associations/autodetect?wait=true&aggregate=true&evaluateNumber=true&threshold=" + $scope.minSimilarity, payload)
				.then(function(response){
					// get table rows from REST service response
					$scope.cockpitAutodetectRows = [];
					angular.forEach(response.data,function(item, key){
						var row = {};
						row["___id"] = key;
						row["___similarity"] = item.coefficient;
						row["___length"] = item.fields.length;
						angular.forEach(datasets,function(dataset){
							row[dataset.label] = null;
						}, row);
						angular.forEach(item.fields,function(field){
							row[field.datasetLabel] = field.datasetColumn;
						}, row);
						this.push(row);
					},$scope.cockpitAutodetectRows);

					// remove rows equal to existing associations
					for(var i=$scope.cockpitAutodetectRows.length-1; i>=0; i--){
						var autodetectRow = $scope.cockpitAutodetectRows[i];
						for(var j=0; j<associations.length; j++){
							var association = associations[j];
							var isEqual = true;
							for(var k=0; k<association.fields.length; k++){
								var field = association.fields[k];
								if(!autodetectRow.hasOwnProperty(field.store) || autodetectRow[field.store] != field.column){
									isEqual = false;
								}
							}
							if(isEqual){
								$scope.cockpitAutodetectRows.splice(i, 1);
							}
						}
					}

					angular.copy($scope.cockpitAutodetectRows, $scope.cockpitAutodetectFilteredRows);
					$scope.showTable = true;
				},function(response){
					$scope.showTable = true;
					sbiModule_restServices.errorHandler(response.data,"");
				});
			},
			disableParentScroll: true,
			templateUrl: baseScriptPath+'/directives/cockpit-data-configuration/templates/dataAssociationAutodetectChoice.html',
//				hasBackdrop: true,
			position: $mdPanel.newPanelPosition().absolute().center(),
			trapFocus: true,
			zIndex: 150,
			fullscreen :true,
			clickOutsideToClose: true,
			escapeToClose: false,
			focusOnOpen: false,
			onRemoving :function(){
			}
		};

		$mdPanel.open(config);
		return deferred.promise;
	}

	this.substitutePlaceholderValues = function(text, datasetLabel, model){

		if(text != undefined){
			var deferred = $q.defer();
			var dataset = this.getDatasetByLabel(datasetLabel);
			if (dataset != undefined) {
				var columnsToshow = [];
				var columnsToshowMeta = [];
        		var columnsToshowIndex = [];
				var localModel = model;
	       	 	var datasetId = dataset.id.dsId;
	       	 	model.dataset = {}
	       	 	model.dataset.dsId = datasetId;

	       	 	//get columnsSelected metadata: adds aggregation functions if required
				for(var dsField in dataset.metadata.fieldsMeta){
					var dsObject = dataset.metadata.fieldsMeta[dsField];
					var header = dsObject.alias;
					var reg = new RegExp('\\$F\\{('+dataset.label+'.'+header+')\\}','g');
            		var matches = text.match(reg);
            		if (matches){
//            			//aggregation function management (ie: COUNT($F{xxx}) )
            			var regAgg = new RegExp('(AVG|MIN|MAX|SUM|COUNT|DISTINCT COUNT)(\\(\\$F{'+dataset.label+'.'+header+'}\\))','g');
            			var matchAgg = text.match(regAgg);
    					if (matchAgg && dsObject.fieldType == 'MEASURE'){
    						//get the optional function
                    		var regFunc = new RegExp('(AVG|MIN|MAX|SUM|COUNT|DISTINCT COUNT)','g');
                    		var matchFunc = matchAgg[0].match(regFunc);
                    		if (matchFunc){
	    						dsObject.aggregationSelected = matchFunc[0];
//	    						dsObject.funcSummary = matchFunc[0];
	    						//set configuration for ask summary values
//	    						if (!model.style) model.style={};
//	    						model.style.showSummary=true;
//                    		}else{
//                    			dsObject.aggregationSelected = 'NONE';
//	    						dsObject.funcSummary = 'NONE';
                    		}

    					}
    					//column is required
            			columnsToshow.push(dataset.label+'.'+header);
            			columnsToshowMeta.push(dsObject);
            		}
				}
//				model.content.columnSelectedOfDataset = dataset.metadata.fieldsMeta;
				model.content.columnSelectedOfDataset = columnsToshowMeta;

				this.loadDatasetRecordsById(datasetId, undefined, undefined, undefined, undefined, model).then(function(allDatasetRecords){

	 				//get columnsSelected dataIndex
	 				for (var col in columnsToshow){
	 					var headerToSearch = columnsToshow[col].substring(columnsToshow[col].indexOf('.')+1);
	                	for (var field in allDatasetRecords.metaData.fields){
	                		if (allDatasetRecords.metaData.fields[field] && allDatasetRecords.metaData.fields[field].header){
	                			var header = allDatasetRecords.metaData.fields[field].header;
	                			if (header == headerToSearch){
	                				columnsToshowIndex.push(columnsToshow[col] + '|' +allDatasetRecords.metaData.fields[field].dataIndex);
	                				break;
	                			}
	                		}
	                	}
	 				}
	 				//get columnsSelected values and replace placeholders
	 				var row = allDatasetRecords.rows[0] || []; //get the first row
	 				for (var col in columnsToshowIndex){
	 					var colAlias =  columnsToshowIndex[col].substring(0,  columnsToshowIndex[col].indexOf('|'));
	 					var colIdx = columnsToshowIndex[col].substring( columnsToshowIndex[col].indexOf('|')+1);
	 					var colValue = row[colIdx];
	 					//at first check for aggregation functions , than for simple values
	 					var reg = new RegExp('(AVG|MIN|MAX|SUM|COUNT|DISTINCT COUNT)(\\(\\$F{'+colAlias+'}\\))','g');
	 					var matches = text.match(reg);
	            		if (matches){
	            			text = text.replace(reg, colValue);
	            		}else{
	            			var reg = new RegExp('\\$F\\{('+colAlias+')\\}','g');
	            			matches = text.match(reg);
	            			if (matches){
		            			text = text.replace(reg, colValue);
	            			}
	            		}
	 				}
	 				deferred.resolve(text);
	 			},function(error){
	         		deferred.reject(error);
	 			});
				return deferred.promise;

			}

		}

	}

})
.run(function() {
	//adds methods for IE11
	if (!String.prototype.startsWith) {
	    String.prototype.startsWith = function(searchString, position){
	      position = position || 0;
	      return this.substr(position, searchString.length) === searchString;
	  };
	}

	if (!String.prototype.endsWith) {
		  String.prototype.endsWith = function(searchString, position) {
		      var subjectString = this.toString();
		      if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position || position > subjectString.length) {
		        position = subjectString.length;
		      }
		      position -= searchString.length;
		      var lastIndex = subjectString.lastIndexOf(searchString, position);
		      return lastIndex !== -1 && lastIndex === position;
		  };
		}

});