/*
Knowage, Open Source Business Intelligence suite
Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

Knowage is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

Knowage is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @authors Giovanni Luca Ulivo (GiovanniLuca.Ulivo@eng.it)
 * v0.0.1
 * 
 */


var wf={};
var addWidgetFunctionality=function(type,config){
	wf[type]=config;
};

//to add a widget type in the cockpitModule_widgetConfigurator , 
//call the function addWidgetFunctionality from the js of the widget directive
angular.module("cockpitModule").factory("cockpitModule_widgetConfigurator",function(){
	var wc={};
	return wc;

});



angular.module("cockpitModule").service("cockpitModule_widgetServices",function($rootScope,cockpitModule_widgetConfigurator,cockpitModule_template,$mdDialog,sbiModule_translate,$timeout,$q,cockpitModule_datasetServices,sbiModule_restServices,cockpitModule_properties,cockpitModule_widgetSelection,cockpitModule_templateServices){

	var wi=this;
	
	var fullPageWidget=false;  
	var widgetInit=0;
	var widgetCount=cockpitModule_templateServices.getNumberOfWidgets();
	var widIni= $rootScope.$on("WIDGET_INITIALIZED",function(){
		//enter inside this if when all widgets (length-1 call) are initialized
		//and when all ds in association are in cache (1 call).
		widgetInit++;
		if(widgetCount+1==widgetInit){
			//remove the interceptor
			widIni();
			$rootScope.$broadcast('ALL_WIDGET_INITIALIZED');
			cockpitModule_properties.all_widget_initialized=true;
		} 
	});

	
	
	addWidgetFunctionality=function(type,config){
		cockpitModule_widgetConfigurator[type]=config;
	};
	for(var key in wf){
		addWidgetFunctionality(key,wf[key]);
	}

	this.items=[];

	this.getAllWidgets=function(){
		var ret = [];
		var numSheets = cockpitModule_template.sheets.length;
		for(var sheet = 0; sheet < numSheets; sheet++){
			ret.push.apply(ret, this.getWidgets(sheet));
		}
		return ret;
	}

	this.getWidgets=function(sheetIndex){
		return cockpitModule_template.sheets[sheetIndex].widgets;
	};

	this.addWidget=function(sheetIndex,item){
		cockpitModule_template.sheets[sheetIndex].widgets.push(item)
	};
	this.loadDatasetRecords = function(ngModel, page, itemPerPage,columnOrdering, reverseOrdering){
		if(ngModel.dataset!=undefined && ngModel.dataset.dsId!=undefined){
			return cockpitModule_datasetServices.loadDatasetRecordsById(ngModel.dataset.dsId,page,itemPerPage,columnOrdering, reverseOrdering, ngModel);
		}
		return null ;
	}
		
	this.isFullPageWidget=function()
	{return fullPageWidget;}
	
	this.setFullPageWidget=function(boolean)
	{ fullPageWidget=boolean}
	
	
	function DialogController($scope, $mdDialog) {
	   $scope.hide = function() {
	     $mdDialog.hide();
	   };

	   $scope.cancel = function() {
	     $mdDialog.cancel();
	   };
	}
	
	
	
	this.deleteWidget=function(sheetIndex,widget,nomessage){
		if(nomessage == true){
			cockpitModule_template.sheets[sheetIndex].widgets.splice(cockpitModule_template.sheets[sheetIndex].widgets.indexOf(widget),1);
			this.setFullPageWidget(false);
		}else{
			var confirm = $mdDialog.confirm()
			.title(sbiModule_translate.load("sbi.cockpit.widget.delete.title"))
			.textContent(sbiModule_translate.load("sbi.cockpit.widget.delete.content"))
			.ariaLabel('delete widget')
			.ok(sbiModule_translate.load("sbi.ds.wizard.confirm"))
			.cancel(sbiModule_translate.load("sbi.ds.wizard.cancel"));
			$mdDialog.show(confirm).then(function() {
				cockpitModule_template.sheets[sheetIndex].widgets.splice(cockpitModule_template.sheets[sheetIndex].widgets.indexOf(widget),1);
				
				wi.setFullPageWidget(false);
			});
		}
	};

	this.initWidget=function(element,config,options){
		element.addClass('fadeOut');
		element.removeClass('fadeIn');

		try{
			var width = angular.element(element)[0].parentElement.offsetWidth;
			var height = angular.element(element)[0].parentElement.offsetHeight;
				if(config.dataset!=undefined && cockpitModule_widgetSelection.haveSelection() && cockpitModule_properties.all_widget_initialized!=true){
					cockpitModule_datasetServices.loadDatasetRecordsById(config.dataset.dsId,options.page,options.itemPerPage,options.columnOrdering, options.reverseOrdering, config)
					.then(function(){
					$rootScope.$broadcast("WIDGET_INITIALIZED");
					},function(){});
				}else{
					$rootScope.$broadcast("WIDGET_EVENT"+config.id,"INIT",{element:element,width:width,height:height});
					$rootScope.$broadcast("WIDGET_INITIALIZED");
				}

		}catch(err){
			console.error("The init function of "+config.type+" widget is not configured",err)

		}
		$timeout(function() {
			element.addClass('fadeIn');
			element.removeClass('fadeOut');
		}, 400);
	};

	this.refreshWidget=function(element,config,nature,options){
		
		var width = angular.element(element)[0].parentElement.offsetWidth;
		var height = angular.element(element)[0].parentElement.offsetHeight;
		
		if(nature == "fullExpand"){
			$rootScope.$broadcast("WIDGET_EVENT"+config.id,"REFRESH",{element:element,width:width,height:height,data:null,nature:nature});
		}else{
			var dsRecords = this.loadDatasetRecords(config,options.page, options.itemPerPage,options.columnOrdering, options.reverseOrdering);
			if(dsRecords == null){
				$rootScope.$broadcast("WIDGET_EVENT"+config.id,"REFRESH",{element:element,width:width,height:height,data:undefined,nature:nature});
			}else{
				/*
					author: rselakov, Radmila Selakovic,
					radmila.selakovic@mht.net
					checking type of widget because of removing load spinner
					in case of updating charts
				*/
				if (options.type && options.type!="chart"){
					$rootScope.$broadcast("WIDGET_EVENT"+config.id,"WIDGET_SPINNER",{show:true});
				}
				
				dsRecords.then(function(data){
					$rootScope.$broadcast("WIDGET_EVENT"+config.id,"WIDGET_SPINNER",{show:false});
					$rootScope.$broadcast("WIDGET_EVENT"+config.id,"REFRESH",{element:element,width:width,height:height,data:data,nature:nature});
				}, function(){
					$rootScope.$broadcast("WIDGET_EVENT"+config.id,"WIDGET_SPINNER",{show:false});
					console.log("Error retry data");
				});
			}
		}
	};

	this.updateGlobalWidgetStyle=function(){
		console.log("updateGlobalWidgetStyle");
		var widgetList=document.getElementsByTagName("cockpit-widget");
		angular.forEach(widgetList, function(value, key) {
			angular.element(value).isolateScope().refreshWidgetStyle();
		});
	}

	

	
});

