/**
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
 * Controller for Analysis view of the Workspace.
 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
 */
(function() {

	var scripts = document.getElementsByTagName("script");
	var currentScriptPath = scripts[scripts.length - 1].src;
	currentScriptPath = currentScriptPath.substring(0, currentScriptPath.lastIndexOf('/') + 1);

angular
	.module('analysis_view_workspace', [])
	/**
	 * The HTML content of the Analysis view (analysis documents).
	 */
	.directive('analysisViewWorkspace', function () {		
		 return {			 
		      restrict: 'E',
		      replace: 'true',
//		      templateUrl: '/knowage/js/src/angular_1.4/tools/workspace/templates/analysisViewWorkspace.html',
		      templateUrl: currentScriptPath + '../../../templates/analysisViewWorkspace.html',
		      controller: analysisController
		  };	  
	})
	.filter("asDate", function () {
		
	    return function (input) {
	        return new Date(input);
	    }
	    
	});

function analysisController($scope, sbiModule_restServices, sbiModule_translate, sbiModule_config, sbiModule_user, 
			$mdDialog, $mdSidenav, $documentViewer, $qbeViewer, toastr) {
	
	$scope.cockpitAnalysisDocsInitial = [];	
	$scope.activeTabAnalysis = null;	
	$scope.translate = sbiModule_translate;
	
	$scope.loadAllMyAnalysisDocuments = function() {
		
		sbiModule_restServices
			.promiseGet("documents", "myAnalysisDocsList")
			.then(
					function(response) {
						
						/**
						 * TODO: Provide a comment
						 */
						angular.copy(response.data.root,$scope.allAnalysisDocs);
						$scope.cockpitAnalysisDocs = [];
						var tempDocumentType = "";
						
						/**
						 * TODO: Provide a comment
						 */
						for(var i=0; i<$scope.allAnalysisDocs.length; i++) {
							
							tempDocumentType = $scope.allAnalysisDocs[i].typeCode;						
							
							switch(tempDocumentType.toUpperCase()) {	
																
								case "DOCUMENT_COMPOSITE": 
									$scope.cockpitAnalysisDocs.push($scope.allAnalysisDocs[i]); 
									break;	
								case "MAP":
								case "DOCUMENT_COMPOSITE": 
									$scope.cockpitAnalysisDocs.push($scope.allAnalysisDocs[i]); 
									break;	
							}
							
						}
						
						angular.copy($scope.cockpitAnalysisDocs,$scope.cockpitAnalysisDocsInitial);
						
						console.info("[LOAD END]: Loading of Analysis Cockpit documents is finished.");
						
					},
					
					function(response) {
						
						// Take the toaster duration set inside the main controller of the Workspace. (danristo)
						toastr.error(response.data, sbiModule_translate.load('sbi.browser.folder.load.error'), $scope.toasterConfig);
						
					}
				);
	}
	
	/**
	 * If we are coming to the Workspace interface (web page) from the interface for the creation of the new Cockpit document, reload all Analysis documents 
	 * (Cockpit documents), so we can see the changes for the option from which we started a creation of a new documents (Analysis option).
	 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
	 */
	if (whereAreWeComingFrom == "NewCockpit") {
		$scope.loadAllMyAnalysisDocuments();
		// Do not load Analysis documents again when clicking on its option after returning back to the Workspace.
		$scope.analysisDocumentsLoaded = true;
		whereAreWeComingFrom == null;
	}
	
	/**
	 * Clone a particular Analysis document.
	 * @commentBy Danilo Ristovski (danristo, danilo.ristovski@mht.net)
	 */
	$scope.cloneAnalysisDocument = function(document) {		
		
		console.info("[CLONE START]: The cloning of a selected '" + document.label + "' has started.");	
		
		var confirm = $mdDialog
						.confirm()
						.title($scope.translate.load("sbi.workspace.clone.confirm.title"))
						.content($scope.translate.load("sbi.workspace.analysis.clone.document.confirm.msg"))
						.ariaLabel('delete Document') 
						.ok($scope.translate.load("sbi.general.yes"))
						.cancel($scope.translate.load("sbi.general.No"));
			
		$mdDialog
			.show(confirm)
			.then(				
					function() {
		
					sbiModule_restServices
						.promisePost("documents","clone?docId="+document.id)
						.then(
								function(response) {
									
									if (document.typeCode == "DOCUMENT_COMPOSITE") { 
										$scope.cockpitAnalysisDocsInitial.push(response.data);
										$scope.cockpitAnalysisDocs.push(response.data);
									}
									
									console.info("[CLONE END]: The cloning of a selected '" + document.label + "' went successfully.");
																		
									// Take the toaster duration set inside the main controller of the Workspace. (danristo)
									toastr.success(sbiModule_translate.load('sbi.workspace.analysis.clone.document.success.msg'),
											sbiModule_translate.load('sbi.generic.success'), $scope.toasterConfig);
									
								},
								
								function(response) {
								
									// Take the toaster duration set inside the main controller of the Workspace. (danristo)
									toastr.error(response.data, sbiModule_translate.load('sbi.browser.document.clone.error'), $scope.toasterConfig);
									
								}
							);
					}
			);			
	}
	
	/**
	 * Delete particular Analysis document from the Workspace.
	 */
	$scope.deleteAnalysisDocument = function(document) {
				
		console.info("[DELETE START]: Delete of Analysis Cockpit document with the label '" + document.label + "' is started.");
		
		var confirm = $mdDialog
						.confirm()
						.title(sbiModule_translate.load("sbi.workspace.delete.confirm.title"))
						.content(sbiModule_translate.load("sbi.workspace.analysis.delete.document.confirm.msg"))
						.ariaLabel('delete Document') 
						.ok(sbiModule_translate.load("sbi.general.yes"))
						.cancel(sbiModule_translate.load("sbi.general.No"));
		
		$mdDialog
			.show(confirm)
			.then(
					function() {
						
						sbiModule_restServices
							.promiseDelete("1.0/documents", document.label)
							.then(
									function(response) {
										
										/**
										 * Reload all Cockpits in Analysis after delete.
										 */
										$scope.loadAllMyAnalysisDocuments();
										
										$scope.selectedDocument = undefined;	// TODO: Create and define the role of this property
										
										/**
										 * If some dataset is removed from the filtered set of datasets, clear the search input, since all datasets are refreshed.
										 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
										 */
										$scope.searchInput = "";
										
										/**
										 * When deleting document from the Analysis interface, run the re-loading of documents in the Organizer, so they will be re-collected 
										 * after deletion of one of the documents available in the Analysis interface, that could on the other side be inside the Organizer 
										 * as well. E.g. physical removing of a document inside the Analysis, to which there is a (one or more) link inside the Organizer, 
										 * should be followed by the removal of that link (links) inside the Organizer.
										 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
										 */
										$scope.loadAllFolders();
										
										/**
										 * The document that does not exist anymore (removed from Analysis documents) and previously appeared in the Recent documents (recently 
										 * executed ones), should be removed from this option as well (from Recent). So, provide a reload of recently executed documents.
										 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
										 */
										$scope.loadRecentDocumentExecutionsForUser();
										
										console.info("[DELETE END]: Delete of Analysis Cockpit document with the label '" + document.label + "' is done successfully.");
																				
										// Take the toaster duration set inside the main controller of the Workspace. (danristo)
										toastr.success(sbiModule_translate.load('sbi.workspace.analysis.delete.document.success.msg')
												,sbiModule_translate.load('sbi.generic.success'), $scope.toasterConfig);
										
									},
								
									function(response) {
										
										// Take the toaster duration set inside the main controller of the Workspace. (danristo)
										toastr.error(response.data, sbiModule_translate.load('sbi.browser.document.delete.error'), $scope.toasterConfig);
										
									}
								);
					}
				);
	
	}
	
	/**
	 * TODO:
	 * Create a new Cockpit document.
	 */
	
	$scope.addNewAnalysisDocument = function() {
		console.info("[NEW COCKPIT - START]: Open page for adding a new Cockpit document.");
	    // cockpit service url from dataset parameters because sbiModule.config engineUrls not visible for user
		
   	 $mdDialog.show({
		  scope:$scope,
		  preserveScope: true,
	      controller: CreateNewAnalysisController,
	      templateUrl: sbiModule_config.contextName+'/js/src/angular_1.4/tools/documentbrowser/template/documentDialogIframeTemplate.jsp',  
	      clickOutsideToClose:true,
	      escapeToClose :true,
	      fullscreen: true
	    })
	}
	/**
	 * add new geo document
	 */
	$scope.addNewGeoMap = function() {
		console.info("[NEW GEO - START]: Open page for adding a new geo map document.");
		//console.log(datasetParameters);
   	 $mdDialog.show({
		  scope:$scope,
		  preserveScope: true,
	      controller: CreateNewGeoMapController,
	      templateUrl: sbiModule_config.contextName+'/js/src/angular_1.4/tools/documentbrowser/template/documentDialogIframeTemplate.jsp',  
	      clickOutsideToClose:true,
	      escapeToClose :true,
	      fullscreen: true
	    });
   	 
  
	}
	/**
	 * Edit existing GEO document
	 */
	
	$scope.editGeoDocument= function(document){
		console.log(document);
		if(document.dataset){
			sbiModule_restServices.promiseGet("1.0/datasets/id", document.dataset)
			.then(function(response) {
			    //console.log(response);
				$scope.openEditDialog(document.label,response.data);
			},function(response){
				
				// Take the toaster duration set inside the main controller of the Workspace. (danristo)
				toastr.error(response.data, sbiModule_translate.load('sbi.workspace.dataset.load.error'), $scope.toasterConfig);
				
			});
		}else{
			
			$scope.openEditDialog(document.label,"");
		}
	}
	
	
	$scope.openEditDialog=function(doclabel,dsLabel){
		 $mdDialog.show({
			  scope:$scope,
			  preserveScope: true,
		      controller: EditGeoMapController,
		      templateUrl: sbiModule_config.contextName+'/js/src/angular_1.4/tools/documentbrowser/template/documentDialogIframeTemplate.jsp',  
		      clickOutsideToClose:true,
		      escapeToClose :true,
		      fullscreen: true,
		      locals:{
		    	  datasetLabel:dsLabel,
		    	  documentLabel:doclabel
		      }
		    });
	}
	/**
	 * The immediate Run (preview) button functionality for the Analysis documents (for List view of documents). 
	 */
	$scope.analysisSpeedMenu = 
	[
	 	{
	 		label: sbiModule_translate.load('sbi.generic.run'),
	 		icon:'fa fa-play-circle' ,
	 		backgroundColor:'transparent',
	 		
	 		action: function(item,event) {
	 			$scope.executeDocument(item);
	 		}
	 	} 
 	];

	function CreateNewAnalysisController($scope,$mdDialog){
		$scope.iframeUrl = datasetParameters.cockpitServiceUrl + '&SBI_ENVIRONMENT=WORKSPACE&IS_TECHNICAL_USER=' + sbiModule_user.isTechnicalUser + "&documentMode=EDIT";
		$scope.cancelDialog = function() {
			$scope.loadAllMyAnalysisDocuments();
			$mdDialog.cancel();
		}
	}
	
	function CreateNewGeoMapController($scope,$mdDialog){
		//console.log(sbiModule_user.isTechnicalUser);
		
		$scope.iframeUrl = datasetParameters.georeportServiceUrl+'&SBI_ENVIRONMENT=WORKSPACE&IS_TECHNICAL_USER='+ sbiModule_user.isTechnicalUser+"&DATASET_LABEL="+'';
		
		$scope.cancelMapDesignerDialog = function() {
			$scope.loadAllMyAnalysisDocuments();
			$mdDialog.cancel();
		}
		
	 	
	}
	
	function EditGeoMapController($scope,$mdDialog,datasetLabel,documentLabel){
		//console.log(sbiModule_user.isTechnicalUser);
		
		$scope.iframeUrl = datasetParameters.georeportServiceUrl+'&SBI_ENVIRONMENT=WORKSPACE&IS_TECHNICAL_USER='+ sbiModule_user.isTechnicalUser+'&DOCUMENT_LABEL='+documentLabel+'&DATASET_LABEL='+datasetLabel;
		
		$scope.cancelMapDesignerDialog = function() {
			$scope.loadAllMyAnalysisDocuments();
			$mdDialog.cancel();
		}
		
	 	
	}
	
}
})();