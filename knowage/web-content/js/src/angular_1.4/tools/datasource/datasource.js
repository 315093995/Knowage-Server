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

/**
 * @author Simović Nikola (nikola.simovic@mht.net)
 */
var app = angular.module('dataSourceModule', ['ngMaterial', 'angular_list', 'angular_table' ,'sbiModule', 'angular_2_col','angular-list-detail']);

app.controller('dataSourceController', ["sbiModule_translate","sbiModule_restServices", "$scope","$mdDialog","$mdToast",
                                        "$timeout","sbiModule_messaging","sbiModule_user","sbiModule_messaging", dataSourceFunction]);

var emptyDataSource = {
	label : "",
	descr : "",
	urlConnection: "",
	user: "",
	pwd: "",
	driver: "",
	dialectId: "",
	hibDialectClass: "",
	hibDialectName: "",
	schemaSttribute: "",
	multiSchema: false,
	readOnly: false,
	writeDefault: false
};

function dataSourceFunction(sbiModule_translate, sbiModule_restServices, $scope, $mdDialog, $mdToast, $timeout,sbiModule_messaging,sbiModule_user,sbiModule_messaging){

	//DECLARING VARIABLES
	$scope.showMe=false;
	$scope.translate = sbiModule_translate;
	$scope.dataSourceList = [];
	$scope.dialects = [];
	$scope.selectedDataSource = {};
	$scope.selectedDataSourceItems = [];
	$scope.isDirty = false;
	$scope.readOnly= false;
	$scope.forms = {};
	$scope.isSuperAdmin = superadmin;
	$scope.jdbcOrJndi = {};
	$scope.currentUser = sbiModule_user.userUniqueIdentifier;

	$scope.isSuperAdminFunction=function(){
        return superadmin;
	};

	angular.element(document).ready(function () {
        $scope.getDataSources();
        
    });
	
	$scope.setDirty = function () {
		$scope.isDirty = true;
	}

	//REST
	$scope.getDataSources = function(){

		//GET DATA SOURCES
		sbiModule_restServices.promiseGet("2.0/datasources", "")
		.then(function(response) {
			$scope.dataSourceList = response.data;
		}, function(response) {
			sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

		});

		//GET DIALECT TYPES

		sbiModule_restServices.promiseGet("domains", "listValueDescriptionByType","DOMAIN_TYPE=DIALECT_HIB")
		.then(function(response) {
			$scope.dialects = response.data;
		}, function(response) {
			sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

		});

	};

	//REST
	$scope.saveOrUpdateDataSource = function(){

		
		
		if($scope.jdbcOrJndi.type=="JDBC") {
			$scope.selectedDataSource.jndi = "";
		} else if($scope.jdbcOrJndi.type=="JNDI") {
			$scope.selectedDataSource.driver = "";
			$scope.selectedDataSource.pwd = "";
			$scope.selectedDataSource.user = "";
			$scope.selectedDataSource.urlConnection = "";
		}
		
		delete $scope.jdbcOrJndi.type;

		if($scope.selectedDataSource.hasOwnProperty("dsId")){


			var errorU = "Error updating the datasource!"

			//MODIFY DATA SOURCE
				$scope.checkReadOnly();
						
				sbiModule_restServices.promisePut('2.0/datasources','',angular.toJson($scope.selectedDataSource))
				.then(function(response) {
					console.log("[PUT]: SUCCESS!");
					$scope.dataSourceList = [];
					$scope.getDataSources();
					$scope.closeForm();
					sbiModule_messaging.showSuccessMessage(sbiModule_translate.load("sbi.catalogues.toast.updated"), 'Success!');
				}, function(response) {
					sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

				});
		} else {

			var errorS = "Error saving the datasource!";
			$scope.checkReadOnly();
			//CREATE NEW DATA SOURCE
			sbiModule_restServices.promisePost('2.0/datasources','', angular.toJson($scope.selectedDataSource))
			.then(function(response) {
				console.log("[POST]: SUCCESS!");
				$scope.dataSourceList = [];
				$scope.getDataSources();
				$scope.closeForm();
				sbiModule_messaging.showSuccessMessage(sbiModule_translate.load("sbi.catalogues.toast.created"), 'Success!');
			}, function(response) {
				sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

			});

		}
	};
	
	$scope.clearType = function() {
		
		if($scope.selectedDataSource.dsId == null){
		
		if ($scope.jdbcOrJndi.type == 'JDBC') {
			$scope.selectedDataSource.jndi = "";
		}else {
			$scope.selectedDataSource.urlConnection = "";
			$scope.selectedDataSource.user = "";
			$scope.selectedDataSource.pwd = "";
			$scope.selectedDataSource.driver= "";
		}
		
		}
	
	};
	
	$scope.checkReadOnly = function() {
		if($scope.selectedDataSource.readOnly==0){
			$scope.selectedDataSource.readOnly=false;
		} else if($scope.selectedDataSource.readOnly==1){
			$scope.selectedDataSource.readOnly=true;
		}
	};

	//REST
	$scope.deleteDataSource = function() {

		//DELETE SEVERAL DATA SORUCES
		if($scope.selectedDataSourceItems.length > 1) {

			sbiModule_restServices.delete("2.0/datasources",queryParamDataSourceIdsToDelete()).success(
					function(data, status, headers, config) {
						console.log(data);
						if (data.hasOwnProperty("errors")) {
							console.log("[DELETE MULTIPLE]: PROPERTY HAS ERRORS!");
						} else {
							console.log("[DELETE MULTIPLE]: SUCCESS!")
							$scope.showActionMultiDelete();
							$scope.closeForm();
							$scope.showActionDelete();
							$scope.selectedDataSourceItems = [];
							$scope.getDataSources();
						}
					}).error(function(data, status, headers, config) {
						console.log("[DELETE MULTIPLE]: FAIL!"+status)
					})

		} else {

			//DELETE  ONE DATA SOURCE

			sbiModule_restServices.promiseDelete("2.0/datasources", $scope.selectedDataSource.dsId)
			.then(function(response) {
				console.log("[DELETE]: SUCCESS!");
				$scope.dataSourceList = [];
				$scope.getDataSources();
				$scope.closeForm();
				sbiModule_messaging.showSuccessMessage(sbiModule_translate.load("sbi.catalogues.toast.deleted"), 'Success!');
			}, function(response) {
				sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

			});
		}
	};

	//SHOW RIGHT-COLUMN
	$scope.createNewDatasource = function () {

		if($scope.isDirty==false) {
			$scope.showMe=true;
			$scope.jdbcOrJndi = {type:"JDBC"};
			$scope.selectedDataSource = {
					label : "",
					descr : "",
					dialectId: "",
					multiSchema: false,
					schemaAttribute: "",
					readOnly: false,
					writeDefault: false,
					urlConnection: "",
					user: "",
					pwd: "",
					driver: "",
					jndi: ""
			};

		} else {

			$mdDialog.show($scope.confirm).then(function() {
				$scope.showMe=true;
				$scope.selectedDataSource = {
						label : "",
						descr : "",
						dialectId: "",
						multiSchema: false,
						readOnly: false,
						writeDefault: false,
						urlConnection: "",
						user: "",
						pwd: "",
						driver: ""
				};

				$scope.isDirty = false;


			}, function() {
				$scope.showMe = true;
			});
		}

	};
	

	//LOAD SELECTED SOURCE
	$scope.loadSelectedDataSource = function(item) {
		console.log(item);
		
		if($scope.isSuperAdmin){
			$scope.readOnly= false;
		}
		
		else if( $scope.currentUser == item.userIn && item.jndi ==""){
			
			$scope.readOnly= false;

		}else{
			
			sbiModule_messaging.showInfoMessage("You are not the owner of this catalog", 'Information');
			$scope.readOnly= true;
		}
		
		console.log(item);
		$scope.jdbcOrJndi.type = null;
		$scope.showMe=true;

			if($scope.isDirty==false) {

				$scope.selectedDataSource = angular.copy(item);

			} else {



				$mdDialog.show($scope.confirm).then(function() {


					$scope.selectedDataSource = angular.copy(item);
					$scope.isDirty = false;


				}, function() {
					$scope.showMe = true;
				});
			}

			$scope.connectionType();
	};

	$scope.connectionType = function () {

		 if($scope.selectedDataSource.driver){
			 $scope.jdbcOrJndi.type = "JDBC";
		 }
		 if($scope.selectedDataSource.jndi!="") {
			 $scope.jdbcOrJndi.type = "JNDI";
		 }

	}

	//CLOSE RIGHT-COLUMN AND SET SELECTED DATA SORUCE TO AN EMPTY OBJECT
	$scope.closeForm = function(){
		$scope.dataSourceForm.$setPristine();
		$scope.dataSourceForm.$setUntouched();
		$scope.showMe=false;
		$scope.isDirty = false;
		$scope.selectedDataSource = {};
	};

	//CONFIRM DELETE
	$scope.showActionDelete = function() {		
		sbiModule_messaging.showInfoMessage(sbiModule_translate.load("sbi.datasource.deleted"),"");
	};

	//CONFIRM MULTIPLE DELETE
	$scope.showActionMultiDelete = function() {
	
		sbiModule_messaging.showInfoMessage(sbiModule_translate.load("sbi.datasource.deleted"),"");
	};

	//CONFIRM OK
	$scope.showActionOK = function() {
		sbiModule_messaging.showInfoMessage(sbiModule_translate.load("sbi.datasource.saved"),"");
		
	};

	//CREATING PATH FOR DELETING MULTIPLE DATA SOURCES
	queryParamDataSourceIdsToDelete = function(){

		   var q="?";

		   for(var i=0; i<$scope.selectedDataSourceItems.length;i++){
			   q+="id="+$scope.selectedDataSourceItems[i].dsId+"&";
		   }

		   return q;

	};

	$scope.deleteItem = function (item) {
		console.log(item)
		sbiModule_restServices.promiseDelete("2.0/datasources", item.dsId)
		.then(function(response) {
			console.log("[DELETE]: SUCCESS!");
			$scope.dataSourceList = [];
			$scope.getDataSources();
			$scope.closeForm();
			sbiModule_messaging.showSuccessMessage(sbiModule_translate.load("sbi.catalogues.toast.deleted"), 'Success!');
		}, function(response) {
			sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

		});
	}

	//REST
	$scope.testDataSource = function () {

		//TEST DATA SOURCE
		var testJSON = angular.copy($scope.selectedDataSource);

		if(testJSON.hasOwnProperty("dsId")){
			delete testJSON.dsId;
		}

		if(testJSON.hasOwnProperty("userIn")) {
			delete testJSON.userIn;
		}
		
		if(testJSON.readOnly=="1"){
			testJSON.readOnly=true;
		} else if(testJSON.readOnly=="0"){
			testJSON.readOnly=false;
		}
		
		sbiModule_restServices.promisePost('2.0/datasources/test','',testJSON)
		.then(function(response) {
			sbiModule_messaging.showInfoMessage(sbiModule_translate.load("sbi.datasource.testing.ok"), sbiModule_translate.load("sbi.datasource.info.msg"));
		}, function(response) {
			if(response.data.hasOwnProperty('RemoteException')){
				sbiModule_messaging.showErrorMessage(response.data.RemoteException.message, sbiModule_translate.load("sbi.datasource.error.msg"));
			}else{
				if (response.data.errors[0].message=="") {
					sbiModule_messaging.showErrorMessage(sbiModule_translate.load("sbi.ds.error.testing.datasource"), sbiModule_translate.load("sbi.datasource.error.msg"));
				} else {
					sbiModule_messaging.showErrorMessage(response.data.errors[0].message, sbiModule_translate.load("sbi.datasource.error.msg"));
				}			}
			
		});
	}

	
	//SPEED MENU TRASH ITEM
	$scope.dsSpeedMenu= [
	                     {
	                    	label:sbiModule_translate.load("sbi.generic.delete"),
	                    	icon:'fa fa-trash-o',
	                    	color:'#a3a5a6',
	                    	action:function(item,event){

	                    		$scope.confirmDelete(item);
	                    	},
	                    	 visible: function(row) {
	             				return row.userIn==$scope.currentUser || $scope.isSuperAdmin ? true : false;
	                		 }

	                     }
	                    
	                    ];

	//INFO ABOUT THE JNDI INPUT FORM
	$scope.showJdniInfo = function(ev){
		$mdDialog.show(
				$mdDialog.alert()
					.clickOutsideToClose(true)
					.content(sbiModule_translate.load("sbi.datasource.jndiname.info"))
					.ok(sbiModule_translate.load("sbi.federationdefinition.template.button.close"))
					.targetEvent(ev)
		);
	}

	$scope.confirm = $mdDialog
	.confirm()
	.title(sbiModule_translate.load("sbi.catalogues.generic.modify"))
	.content(
			sbiModule_translate
			.load("sbi.catalogues.generic.modify.msg")).ok(
					sbiModule_translate.load("sbi.general.yes")).cancel(
							sbiModule_translate.load("sbi.general.No"));

	
	
	 $scope.confirmDelete = function(item,ev) {
		    var confirm = $mdDialog.confirm()
		          .title(sbiModule_translate.load("sbi.catalogues.toast.confirm.title"))
		          .content(sbiModule_translate.load("sbi.catalogues.toast.confirm.content"))
		          .ariaLabel("confirm_delete")
		          .targetEvent(ev)
		          .ok(sbiModule_translate.load("sbi.general.continue"))
		          .cancel(sbiModule_translate.load("sbi.general.cancel"));
		    $mdDialog.show(confirm).then(function() {
		    	$scope.deleteItem(item);
		    }, function() {
		
		    });
		  };
};

app.config(['$mdThemingProvider', function($mdThemingProvider) {

    $mdThemingProvider.theme('knowage')

$mdThemingProvider.setDefaultTheme('knowage');
}]);
