(function() {

	var stringStartsWith = function (string, prefix) {
		return string.toLowerCase().slice(0, prefix.length) == prefix.toLowerCase();
	};

	var documentExecutionApp = angular.module('documentExecutionModule');
	
	documentExecutionApp.config(['$mdThemingProvider', function($mdThemingProvider) {
		$mdThemingProvider.theme('knowage')
		$mdThemingProvider.setDefaultTheme('knowage');
	}]);

	
	documentExecutionApp.controller( 'documentExecutionController', 
			['$scope', '$http', '$mdSidenav', '$mdDialog', '$mdToast', 'sbiModule_translate', 'sbiModule_restServices', 'sbiModule_user', 
			 'sbiModule_config', 'sbiModule_messaging', 'execProperties', 'documentExecuteFactories', 'sbiModule_helpOnLine',
			 'documentExecuteServices', 'docExecute_urlViewPointService', 'docExecute_paramRolePanelService', 'infoMetadataService', 'sbiModule_download', '$crossNavigationScope',
			 'docExecute_dependencyService', '$timeout', 'docExecute_exportService', '$filter', 'sbiModule_dateServices', 'cockpitEditing', '$window','$mdMenu',
			 documentExecutionControllerFn]);

	function documentExecutionControllerFn(
			$scope, $http, $mdSidenav, $mdDialog,$mdToast, sbiModule_translate, sbiModule_restServices,sbiModule_user, sbiModule_config,
			sbiModule_messaging, execProperties, documentExecuteFactories, sbiModule_helpOnLine, documentExecuteServices,
			docExecute_urlViewPointService, docExecute_paramRolePanelService, infoMetadataService, sbiModule_download, $crossNavigationScope,
			docExecute_dependencyService, $timeout, docExecute_exportService, $filter, sbiModule_dateServices, cockpitEditing,$window,$mdMenu) {

		console.log("documentExecutionControllerFn IN ");
		
		
		$scope.showCollaborationMenu = sbiModule_user.functionalities.indexOf("Collaboration")>-1;


		//NAVIGATOR WHEEL
		$scope.navigatorVisibility = false;
		$scope.toggleNavigator = function(e) {
			$scope.navigatorStyle = {
					"left" : (e.pageX-150)+'px'	
			}
			$scope.navigatorVisibility = $scope.navigatorVisibility?false:true;
		}
		$scope.goBackHome = function(){
			$crossNavigationScope.crossNavigationHelper.crossNavigationSteps.stepControl.resetBreadCrumb();
			//$window.location.href = "http://localhost:8080/knowage/servlet/AdapterHTTP?ACTION_NAME=EXECUTE_DOCUMENT_ANGULAR_ACTION&SBI_ENVIRONMENT=DOCBROWSER&OBJECT_LABEL=PADRE_CROSS&OBJECT_NAME=Home%20page&IS_SOURCE_DOCUMENT=true";
			$window.location.href = "http://161.27.39.83:8080/knowage/servlet/AdapterHTTP?ACTION_NAME=EXECUTE_DOCUMENT_ANGULAR_ACTION&SBI_ENVIRONMENT=DOCBROWSER&OBJECT_LABEL=HOME_PAGE&OBJECT_NAME=Home%20page&IS_SOURCE_DOCUMENT=true";
		}
		
		$scope.execProperties = execProperties;
		$scope.cockpitEditing = cockpitEditing;
		$scope.executionInstance = execProperties.executionInstance || {};
		$scope.roles = execProperties.roles;
		$scope.selectedRole = execProperties.selectedRole;
		$scope.execContextId = "";
		$scope.showSelectRoles = true;
		$scope.translate = sbiModule_translate;
		$scope.documentParameters = execProperties.parametersData.documentParameters;
		$scope.newViewpoint = JSON.parse(JSON.stringify(documentExecuteFactories.EmptyViewpoint));
		$scope.viewpoints = [];
		$scope.documentExecuteFactories = documentExecuteFactories;
		$scope.documentExecuteServices = documentExecuteServices;
		$scope.paramRolePanelService = docExecute_paramRolePanelService;
		$scope.urlViewPointService = docExecute_urlViewPointService;		
		$scope.currentView = execProperties.currentView;
		$scope.parameterView = execProperties.parameterView;
		$scope.isParameterRolePanelDisabled = execProperties.isParameterRolePanelDisabled;
		$scope.showParametersPanel = execProperties.showParametersPanel;
		//rank
		$scope.rankDocumentSaved = 0;
		$scope.requestToRating={};		
		$scope.isClick = false;
		$scope.setRank = false;
		//note
		$scope.noteLoaded = {};
		$scope.typeNote='Private';
		$scope.notesList = [];
		$scope.profile="";
		$scope.selectedTab={'tab':0};
		$scope.contentNotes = "";
		$scope.dependenciesService = docExecute_dependencyService;
		$scope.exportService = docExecute_exportService;
		$scope.crossNavigationScope=$crossNavigationScope;
		$scope.firstExecutionProcessRestV1=true;
		$scope.download=sbiModule_download;
		$scope.sidenavToShow = 'east';
		$scope.sidenavCenter = null;
		$scope.filterDropping = null; 
		
		/**
		 * Add these 'documentExecutionNg.jsp' Javascript variables to the scope of the document execution controller and use them
		 * for managing the view part of the application (e.g. whether the "Add to my workspace" document execution menu option (or
		 * some other one) should be shown). They will be used for binding on this JSP page.
		 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
		 */
		$scope.executedFrom = executedFrom.toUpperCase();
		$scope.isAdmin = isAdmin;
		$scope.isSuperAdmin = isSuperAdmin;
		$scope.isAbleToExecuteAction = isAbleToExecuteAction;
		$scope.addToWorkspaceEnabled = (sbiModule_user.functionalities.indexOf("SaveIntoFolderFunctionality")>-1)? true:false;
		$scope.showScheduled = (sbiModule_user.functionalities.indexOf("SchedulerManagement")>-1)? true:false;
		
		//navigation default parameters
		$scope.navigatorEnabled 	= false;
		$scope.navigatorVisibility 	= false;
		
		//menu Toggle override
		$scope.closeMdMenu = function() { $mdMenu.hide(); };
		
		$scope.isOrganizerEnabled = function () {
			if(!$scope.addToWorkspaceEnabled){
				return false
			} else {
				return !($scope.executedFrom=='WORKSPACE_ORGANIZER'|| isAdmin || isSuperAdmin)
			}
		}
		
		if ($scope.executionInstance.SidenavOri === 'north'){
			$scope.sidenavCenter = "center left";
			$scope.filterDropping = "row"; 
		}
			
		else{
			$scope.sidenavCenter = "center center";
			$scope.filterDropping = "column";
		}

		$scope.hideProgressCircular = execProperties.hideProgressCircular;
		
		$scope.getSidenavType = function(){
			var xx = execProperties;
			return $scope.sidenavToShow;
		}
		
		$scope.openInfoMetadata = function() {
			infoMetadataService.openInfoMetadata();
		};

		$scope.initSelectedRole = function() {
			console.log("initSelectedRole IN ");  
			var isRoleSelected =false;
			if(execProperties.roles && execProperties.roles.length > 0) {
				if(!angular.equals(execProperties.selectedRole.name,'')){
						for(role in execProperties.roles){
							if(angular.equals(execProperties.selectedRole.name,execProperties.roles[role])){
								isRoleSelected = true;
								break;
							}
						}
						if(!isRoleSelected){
							execProperties.selectedRole.name="";

						}
				}
				
				if(execProperties.roles.length==1 || (execProperties.roles.length>1 && isRoleSelected) ) {
					 
					execProperties.selectedRole.name = isRoleSelected ? execProperties.selectedRole.name : execProperties.roles[0];
					$crossNavigationScope.changeNavigationRole(execProperties.selectedRole);
					$scope.showSelectRoles = false;					
					//loads parameters if role is selected
					execProperties.isParameterRolePanelDisabled.status = true;
					docExecute_urlViewPointService.getParametersForExecution(execProperties.selectedRole.name, $scope.buildCorrelation,docExecute_urlViewPointService.buildParameterForFirstExecution(execProperties.executionInstance.CROSS_PARAMETER,execProperties.executionInstance.MENU_PARAMETER));
				}else{ 
					docExecute_paramRolePanelService.toggleParametersPanel(true);
				}
				docExecute_urlViewPointService.frameLoaded=false;
				// TODO controllare a cosa serve
				//docExecute_urlViewPointService.executionProcesRestV1(execProperties.selectedRole.name,docExecute_urlViewPointService.buildParameterForFirstExecution(execProperties.executionInstance.CROSS_PARAMETER,execProperties.executionInstance.MENU_PARAMETER));
				$scope.firstExecutionProcessRestV1=false;
				 
			}
			
			console.log("initSelectedRole OUT ");
		};
				
		
		
		
		/*
		 * DEPENDENCIES
		 */
		$scope.dependenciesService.observableVisualParameterArray = [];
		$scope.dependenciesService.observableDataDependenciesArray = [];
		$scope.dependenciesService.visualCorrelationMap = {};
		$scope.dependenciesService.dataDependenciesMap = {};
		$scope.dependenciesService.observableLovParameterArray = [];
		$scope.dependenciesService.lovCorrelationMap = {};
		
		/*
		 * BUILD CORRELATION
		 * Callback function from service getParameter for visual dependencies
		 */
		$scope.buildCorrelation = function(parameters){
			docExecute_dependencyService.buildVisualCorrelationMap(parameters);
			docExecute_dependencyService.buildDataDependenciesMap(parameters);
			docExecute_dependencyService.buildLovCorrelationMap(parameters);
			//INIT VISUAL CORRELATION PARAMS
			for(var i=0; i<parameters.length; i++){
				docExecute_dependencyService.visualCorrelationWatch(parameters[i]);
				//docExecute_dependencyService.lovCorrelationWatch(parameters[i]);
			}
		};
				
		
		 /*
		  * WATCH ON LOV DEPENDENCIES PARAMETER OBJECT
		  */
		  $scope.$watch( function() {
			  return $scope.dependenciesService.observableLovParameterArray;
			},
			function(newValue, oldValue) {
				if (!angular.equals(newValue, oldValue)) {
					for(var i=0; i<newValue.length; i++){
						if(oldValue[i] && (!angular.equals(newValue[i].parameterValue, oldValue[i].parameterValue)) ){
							docExecute_dependencyService.lovCorrelationWatch(newValue[i]);
							break;
						}
						
					}
				}
			},true);	
		
		
		
		
	 /*
	  * WATCH ON VISUAL DEPENDENCIES PARAMETER OBJECT
	  */
		$scope.$watch( function() {
			return $scope.dependenciesService.observableVisualParameterArray;
		},
		function(newValue, oldValue) {
			if (!angular.equals(newValue, oldValue)) {
				for(var i=0; i<newValue.length; i++){
					if(oldValue[i] && (!angular.equals(newValue[i].parameterValue, oldValue[i].parameterValue)) ){
						docExecute_dependencyService.visualCorrelationWatch(newValue[i]);
						break;
					}

				}
			}
		},true);
		 
	     /*
		  * WATCH ON DATA DEPENDENCIES PARAMETER OBJECT
		  */
		$scope.$watch( function() {
			return $scope.dependenciesService.observableDataDependenciesArray;
		},
		// new value and old Value are the whole parameters
		function(newValue, oldValue) {
			if (!angular.equals(newValue, oldValue)) {
				for(var i=0; i<newValue.length; i++){
					
					var oldValPar = oldValue[i];
					var newValPar = newValue[i];
					
					//only new value different old value
					if(oldValPar && (!angular.equals(newValPar, oldValPar)) ){
						
						var oldParValue = oldValPar.parameterValue; 
						var newParValue = newValPar.parameterValue; 
						
						
						
						if(oldParValue == undefined || oldParValue == "" ||
								(oldParValue && (!angular.equals(newParValue, oldParValue))) 
								){
							docExecute_dependencyService.dataDependenciesCorrelationWatch(newValPar);
						}
						break;
						
					}

				}
			}
		},true);
	  
		//ranking document
		$scope.rankDocument = function() {
			var obj = {
					'obj':$scope.executionInstance.OBJECT_ID
			};
			sbiModule_restServices.promisePost("documentrating", "getvote",obj).then(function(response) { 
				//angular.copy(response.data,$scope.rankDocumentSaved);
				$scope.rankDocumentSaved = response.data;
			},function(response) {
				$mdDialog.cancel();
				$scope.isClick = false;
			});

			$mdDialog.show({
				controller: rankControllerFunction,
				templateUrl:sbiModule_config.contextName+'/js/src/angular_1.4/tools/documentbrowser/template/documentRank.html',
				scope:$scope,
				preserveScope: true,
				clickOutsideToClose:true
			})
			.then(function(answer) {
				$scope.status = 'You said the information was "' + answer + '".';
				$scope.isClick = false;
			}, function() {
				$scope.status = 'You cancelled the dialog.';
				$scope.isClick = false;
			});
		};

		//mail
		$scope.sendMail = function(){
			$mdDialog.show({
				scope:$scope,
				preserveScope: true,
				clickOutsideToClose:true,
				controllerAs : 'sendMailCtrl',
				controller : function($mdDialog) {
					var sendmailctl = this;
					sendmailctl.mail = {};
					sendmailctl.mail.label = $scope.executionInstance.OBJECT_LABEL;
					sendmailctl.mail.docId = $scope.executionInstance.OBJECT_ID;
					sendmailctl.mail.userId = sbiModule_user.userId;
					sendmailctl.mail.MESSAGE = "";
					params = documentExecuteServices.buildStringParameters(execProperties.parametersData.documentParameters);
					params= typeof params === 'undefined' ? {} : params;
					sendmailctl.mail.parameters = params;
					sendmailctl.submit = function() {
						sbiModule_restServices
						.promisePost("1.0/documentexecutionmail", "sendMail", sendmailctl.mail)
						.then(
								function(response) {
									$mdDialog.hide();
									documentExecuteServices.showToast(sbiModule_translate.load("sbi.execution.sendmail.success"), 3000);
								},
								function(response){
									documentExecuteServices.showToast(response.data.errors);
								}	
						);
					};

					sendmailctl.annulla = function($event) {
						$mdDialog.hide();
					};
				},

				templateUrl : sbiModule_config.contextName 
				+ '/js/src/angular_1.4/tools/documentexecution/templates/documentSendMail.html'
			});
		};
		
		//note document
		$scope.noteDocument = function() {
			var obj = {'id' : $scope.executionInstance.OBJECT_ID};
			sbiModule_restServices
			.promisePost("documentnotes", 'getNote',obj)
			.then(
					function(response) {
						if (response.data.hasOwnProperty("errors")) {
							$scope.showAction(response.data);
						} else {
							console.log(response);
							angular.copy(response.data,$scope.noteLoaded);
							$scope.contentNotes = $scope.noteLoaded.nota;
							$scope.profile = response.data.profile;
						}
					},
					function(response) {
						$scope.errorHandler(response.data,"");
					});

			$mdDialog.show({
				controller: noteControllerFunction,
				templateUrl:sbiModule_config.contextName+'/js/src/angular_1.4/tools/documentbrowser/template/documentNote.html',
				scope:$scope,
				preserveScope: true,
				clickOutsideToClose:true
			})
			.then(function(answer) {
				$scope.status = 'You said the information was "' + answer + '".';
			}, function() {
				$scope.status = 'You cancelled the dialog.';
			});
		};


		$scope.checkHelpOnline = function(){
			return sbiModule_user.isAbleTo("Glossary");
		}
		
		$scope.openHelpOnLine = function() {	
			sbiModule_helpOnLine.showDocumentHelpOnLine($scope.executionInstance.OBJECT_LABEL);
		};
		
		$scope.execShowHelpOnLine = function(data) {	
			sbiModule_helpOnLine.show(data);
		};

		//davverna - mcortella: toggle visibility of the navigator between documents
		$scope.openNavigator = function(){
			$scope.navigatorVisibility = $scope.navigatorVisibility ? false: true;
		}
					
		/*
		 * EXECUTE PARAMS
		 * Submit param form
		 */
		$scope.executeParameter = function() {
			console.log("executeParameter IN ");
			
			var action = function() {
				docExecute_urlViewPointService.frameLoaded=false;
				docExecute_urlViewPointService.executionProcesRestV1(execProperties.selectedRole.name, 
						 documentExecuteServices.buildStringParameters(execProperties.parametersData.documentParameters));
				docExecute_paramRolePanelService.toggleParametersPanel(false);
				$scope.cockpitEditing.documentMode="VIEW";
			};
			
			if($scope.cockpitEditing.documentMode == "EDIT"){
				var confirm = $mdDialog.confirm()
						.title(sbiModule_translate.load('sbi.execution.executionpage.toolbar.editmode'))
						.content(sbiModule_translate.load('sbi.execution.executionpage.toolbar.editmode.quit'))
						.ariaLabel('Leave edit mode')
						.ok(sbiModule_translate.load("sbi.general.continue"))
						.cancel(sbiModule_translate.load("sbi.general.cancel"));
				
				$mdDialog.show(confirm).then(function(){action.call()});
			}else{
				action.call();
			}
			
			console.log("executeParameter OUT ");
		};
		
		$scope.changeRole = function(role) {
			console.log("changeRole IN ");
			if(role != execProperties.selectedRole.name) {  
				$crossNavigationScope.changeNavigationRole(execProperties.selectedRole);
				docExecute_urlViewPointService.getParametersForExecution(role,$scope.buildCorrelation,docExecute_urlViewPointService.buildParameterForFirstExecution(execProperties.executionInstance.CROSS_PARAMETER,execProperties.executionInstance.MENU_PARAMETER));
				docExecute_urlViewPointService.frameLoaded=false;
				if($scope.firstExecutionProcessRestV1){
					docExecute_urlViewPointService.executionProcesRestV1(role,docExecute_urlViewPointService.buildParameterForFirstExecution(execProperties.executionInstance.CROSS_PARAMETER,execProperties.executionInstance.MENU_PARAMETER));
					$scope.firstExecutionProcessRestV1=false;
				}else{
					docExecute_urlViewPointService.executionProcesRestV1(role, documentExecuteServices.buildStringParameters(execProperties.parametersData.documentParameters));
				}

			}
			console.log("changeRole OUT ");
		};

		$scope.isParameterPanelDisabled = function() {
			return (!execProperties.parametersData.documentParameters || execProperties.parametersData.documentParameters.length == 0);
		};

		$scope.executeDocument = function() {
			console.log('Executing document -> ', execProperties);
		};

		$scope.editDocument = function() {
			alert('Editing document');
			console.log('Editing document -> ', execProperties);
		};

		$scope.deleteDocument = function() {
			alert('Deleting document');
			console.log('Deleting document -> ', execProperties);
		};

		$scope.clearListParametersForm = function() {
			if(execProperties.parametersData.documentParameters.length > 0) {
				for(var i = 0; i < execProperties.parametersData.documentParameters.length; i++) {
					var parameter = execProperties.parametersData.documentParameters[i];
					documentExecuteServices.resetParameter(parameter);
					//INIT VISUAL CORRELATION PARAMS
					docExecute_dependencyService.visualCorrelationWatch(parameter);
				}
			}
		};

		$scope.printDocument = function() {
			var frame = window.frames["documentFrame"];
			if(frame.print) {
				frame.print();
			} else if(frame.contentWindow) {
				frame.contentWindow.print();
			}
		};
		
		$scope.closeDocument = function() {
			var action = function() {
				$crossNavigationScope.closeDocument($scope.executionInstance.OBJECT_ID);
			};
			
			if($scope.cockpitEditing.documentMode == "EDIT"){
				var confirm = $mdDialog.confirm()
						.title(sbiModule_translate.load('sbi.execution.executionpage.toolbar.editmode'))
						.content(sbiModule_translate.load('sbi.execution.executionpage.toolbar.editmode.quit'))
						.ariaLabel('Leave edit mode')
						.ok(sbiModule_translate.load("sbi.general.continue"))
						.cancel(sbiModule_translate.load("sbi.general.cancel"));
				
				$mdDialog.show(confirm).then(function(){action.call()});
			}else{
				action.call();
			}
		};

		$scope.isCloseDocumentButtonVisible=function(){
			return $crossNavigationScope.isCloseDocumentButtonVisible();  
		};

		$scope.iframeOnload = function(){
			docExecute_urlViewPointService.frameLoaded = true;
			if ($scope.$root.$$phase != '$apply' && $scope.$root.$$phase != '$digest') {
				$scope.$apply();
			}
		};
		
		$scope.navigateTo= function(outputParameters,inputParameters,targetCrossNavigation,docLabel, staticParameters){
			$crossNavigationScope.crossNavigationHelper.navigateTo(outputParameters,execProperties.parametersData.documentParameters,targetCrossNavigation,docLabel,staticParameters);
//			$crossNavigationScope.crossNavigationHelper.navigateTo(outputParameters,inputParameters,targetCrossNavigation,docLabel);
		};
		
		$scope.internalNavigateTo= function(params,targetDocLabel){
			$crossNavigationScope.crossNavigationHelper.internalNavigateTo(params,targetDocLabel);
		};
		 
		console.log("documentExecutionControllerFn OUT ");
	};

	documentExecutionApp.directive('iframeSetDimensionsOnload', ['docExecute_urlViewPointService',function(docExecute_urlViewPointService) {
		return {
			scope: {
				iframeOnload: '&?'
			},
			restrict: 'A',
			link: function(scope, element, attrs) {
				element.on('load', function() {
					// var iFrameHeight = element[0].parentElement.scrollHeight + 'px';
					// changed to height 100% because of phantomjs rendering errors
					element.css('height', '100%');				
					element.css('width', '100%');
					if(scope.iframeOnload)
						scope.iframeOnload();
				});
			}
		};
	}]);
})();

//from executed document, call this function to exec old cross navigation method
//from executed document, call this function to exec old cross navigation method
var execCrossNavigation=function(frameid, doclabel, params, subobjid, title, target){
	var jsonEncodedParams=params?JSON.parse('{"' + decodeURI(params).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g,'":"') + '"}'):{};
	var parent = angular.element(frameElement).scope().$parent;
	while(parent != undefined){
		if(parent.internalNavigateTo != undefined){
			break;
		}
		parent = parent.$parent;
	}
	parent.internalNavigateTo(jsonEncodedParams,doclabel);
};

var execExternalCrossNavigation=function(outputParameters,inputParameters,targetCrossNavigation,docLabel,staticParameters){ 
	var parent = angular.element(frameElement).scope().$parent;
	while(parent != undefined){
		if(parent.navigateTo != undefined){
			break;
		}
		parent = parent.$parent;
	}
	parent.navigateTo(outputParameters,inputParameters,targetCrossNavigation,docLabel,staticParameters);
};

var execShowHelpOnLine=function(data){
	var parent = angular.element(frameElement).scope().$parent;
	while(parent != undefined){
		if(parent.execShowHelpOnLine != undefined){
			break;
		}
		parent = parent.$parent;
	}
	parent.execShowHelpOnLine(data);
}
