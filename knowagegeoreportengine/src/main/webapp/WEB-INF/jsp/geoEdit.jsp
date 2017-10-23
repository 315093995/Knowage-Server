<%--
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
--%>


<%@ page language="java" pageEncoding="utf-8" session="true"%>
<%@include file="/WEB-INF/jsp/commons/angular/angularResource.jspf"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html ng-app="geoTemplateBuild">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%@include file="/WEB-INF/jsp/commons/angular/angularImport.jsp"%>
<%@include file="/WEB-INF/jsp/commons/angular/geoImport.jsp"%>

<!-- document-viewer -->
<script type="text/javascript" src="/knowage/js/src/angular_1.4/tools/commons/document-viewer/documentViewer.js"></script>

<title>{{::translate.load("gisengine.designer.title")}}</title>
<script>
 var documentLabel='<%=docLabel%>';
 var dataset='<%=docDatasetLabel%>';
 var docTemplate= '<%=template%>';
 var isTechnicalUser = '<%=isUserTechnical%>';
 var datasetLabel='<%=datasetLabel%>';
 if(datasetLabel==null){
	 datasetLabel='';
 }
</script>
</head>
<body ng-controller="geoTemplateBuildController" class="kn-geoEdit">
<md-toolbar  class="toolbar" layout="row">
	<div class="md-toolbar-tools" flex>
		<h2 class="md-flex" >{{::translate.load("gisengine.designer.title")}}</h2>
		<span flex></span>
		<md-button  ng-click="editMap()" ng-disabled="editDisabled" >{{::translate.load("gisengine.designer.edit.map")}}</md-button>
		<md-button  ng-click="saveTemplate()">{{::translate.load("sbi.generic.save")}}</md-button>
	    <md-button  ng-if="!tecnicalUser" ng-click="cancelBuildTemplate()">{{::translate.load("sbi.generic.close")}}</md-button>
	    <md-button  ng-if="tecnicalUser" ng-click="cancelBuildTemplateAdmin()">{{::translate.load("sbi.generic.close")}}</md-button>
	</div>
</md-toolbar>
<md-whiteframe class="md-whiteframe-2dp relative" layout-fill flex  >
<!-- CHOSE DATA SET FOR FINAL USER -->
<div layout="row" flex ng-if="!tecnicalUser">
	<label flex=20>{{datasetLabel}}</label>
  	<md-button class="md-fab md-mini md-primary" ng-if="!isDatasetChosen && !disableChooseDs" ng-click="choseDataset()" aria-label="Add dataset">
          <md-icon class="fa fa-plus-circle fa-2x"></md-icon>
    </md-button>
    <md-button class="md-fab md-mini md-primary" ng-if="isDatasetChosen && !disableChooseDs" ng-click="clearDataset()" aria-label="Clear dataset">
          <md-icon class="fa fa-minus-circle fa-2x"></md-icon>
    </md-button>      
</div>
<!-- SINGLE SELECT FROM LAYER CATALOG  WHEN DATASET IS CHOSEN-->
	<expander-box id="layersList" color="white" background-color="#a9c3db" ng-if="isDatasetChosen" expanded="true" title="{{::translate.load('gisengine.designer.layer.select')}}">
		<div flex  class="innerExpander" layout-column> 
		
		<!-- div layout="row" layout-align="center">
			<div class="kn-info">
				{{translate.load(Message to show)}}
			</div>
		</div-->
		
	 		<angular-table class="datasetLayer"
			id='table' ng-model="selectedLayer"
			columns='[{"label":"ID","name":"layerId"},{"label":"Name","name":"name"},{"label":"Description","name":"descr"},{"label":"Type","name":"type"}]'
			columns-search='["name"]' show-search-bar=false
			no-pagination="true"
			scope-functions='tableFunctionSingleLayer' 
			>
				<queue-table>
					<div layout="row"> 
						<span flex></span>
						<md-button ng-click="scopeFunctions.loadListLayers()">{{::scopeFunctions.translate.load('gisengine.designer.layer.change')}}</md-button>
					</div>
				</queue-table> 
			</angular-table>	
   		</div>       
	</expander-box>
	<!-- MULTI SELECT LAYER  -->
	<expander-box id="layersListMultiSelect" color="white" background-color="#a9c3db" ng-if="!isDatasetChosen" expanded="true" title="{{::translate.load('gisengine.designer.layer.select')}}">
		<div class="innerExpander" layout-column>  

	 		<angular-table 
			id='tableLayerMultiSelect' ng-model="selectedLayer"
			columns='[{"label":"ID","name":"layerId"},{"label":"Name","name":"name"},{"label":"Description","name":"descr"},{"label":"Type","name":"type"}]'
			columns-search='["name"]' show-search-bar=true no-pagination="false" items-per-page="5"
			scope-functions='tableFunctionMultiLayer' speed-menu-option='multipleLayerSpeedMenu'
			>
				<queue-table>
					<div layout="row"> 
						<span flex></span>
						<md-button ng-click="scopeFunctions.loadListLayers()">{{::scopeFunctions.translate.load('gisengine.designer.layer.add')}}</md-button>
					</div>
				</queue-table> 
			</angular-table>	
   		</div>       
	</expander-box>
	<!-- CHOSING LAYER FILTERS visible if there is no dataset-->
	<expander-box id="filterSelectBox" color="white" background-color="#a9c3db" ng-if="!isDatasetChosen && allDriverParamteres.length" expanded="false" title="{{::translate.load('gisengine.designer.layer.filters')}}">
		<div class="innerExpander" layout-column>  
			<angular-table
			id='tableDriver' ng-model="selectedDriverParamteres"
			columns='[{"label":"Driver parameter","name":"label"},{"label":"URL","name":"url"}]'
			columns-search='["name"]' show-search-bar=true
			scope-functions='tableFunctionFilters' speed-menu-option='filtersSpeedMenu'
			>
				<queue-table>
					<div layout="row"> 
						<span flex></span>
						<md-button ng-click="scopeFunctions.loadFilters()">{{::scopeFunctions.translate.load('gisengine.designer.layer.filters.add')}}</md-button>
					</div>
				</queue-table> 
			</angular-table>
		</div>      
	</expander-box>
	<!-- DATASET JOIN COLUMNS INTERFACE -->
	<expander-box id="datasetJoinBox" color="white" background-color="#a9c3db" ng-if="isDatasetChosen" expanded="false" title="{{::translate.load('gisengine.designer.dataset.joincolumns')}}">
	<div class="innerExpander" layout-column>  
	    <angular-table 
		id='datasetJoinColumnsTable' ng-model="datasetJoinColumns"
		columns='[{"label":"Dataset join column","name":"datasetColumnView","hideTooltip":true},{"label":"Layer join column","name":"layerColumnView","hideTooltip":true}]'
		columns-search='["datasetColumn","layerColumn"]' show-search-bar=true
		scope-functions='tableFunctionsJoin' speed-menu-option='datasetJoinSpeedMenu'
		allow-edit="true"
		>
			<queue-table>
				<div layout="row"> 
					<span flex></span>
					<md-button ng-click="scopeFunctions.addJoinColumn()">{{::scopeFunctions.translate.load('gisengine.designer.dataset.joincolumns.add')}}</md-button>
				</div>
			</queue-table> 
		</angular-table>	
	</div>
	</expander-box>
	<!-- DATASET INDICATORS -->
	<expander-box id="datasetIndicators" color="white" background-color="#a9c3db" ng-if="isDatasetChosen" expanded="false" title="{{::translate.load('gisengine.designer.dataset.indicators')}}">
	<div class="innerExpander" layout-column> 
	    <angular-table 
		id='indicatorsTable' ng-model="datasetIndicators"
		columns='[{"label":"Measure","name":"indicatorNameView","hideTooltip":true},{"label":"Label","name":"indicatorLabel","hideTooltip":true,"editable":true}]'
		columns-search='["indicatorName","indicatorLabel"]' show-search-bar=true
		scope-functions='tableFunctionIndicator' speed-menu-option='indicatorsSpeedMenu'
		allow-edit="true"
		>
			<queue-table>
				<div layout="row"> 
					<span flex></span>
					<md-button ng-click="scopeFunctions.addIndicator()">{{::scopeFunctions.translate.load('gisengine.designer.dataset.indicators.add')}}</md-button>
				</div>
			</queue-table> 
		</angular-table>	
	</div>
	</expander-box>
	<!-- DATASET FILTERS  -->
	<expander-box id="datasetFilters" color="white" background-color="#a9c3db" ng-if="isDatasetChosen" expanded="false" title="{{::translate.load('gisengine.designer.dataset.filters')}}">
	<div class="innerExpander" layout-column> 
	    <angular-table 
		id='filtersTable' ng-model="datasetFilters"
		columns='[{"label":"Name","name":"dsFilterNameView","hideTooltip":true},{"label":"Label","name":"dsFilterLabel","hideTooltip":true,"editable":true}]'
		columns-search='["dsFilterName","dsFilterLabel"]' show-search-bar=true
		scope-functions='tableFunctionDatasetFilters' speed-menu-option='dsFiltersSpeedMenu'
		allow-edit="true"
		>
			<queue-table>
				<div layout="row"> 
					<span flex></span>
					<md-button ng-click="scopeFunctions.addDatasetFilter()">{{::scopeFunctions.translate.load('gisengine.designer.dataset.dsfilters.add')}}</md-button>
				</div>
			</queue-table> 
		</angular-table>	
	</div>
	</expander-box>
	<!-- GEO CONFIC VISIBILITY SETTINGS -->
	<expander-box id="visibilitySettings" background-color="#a9c3db" aria-label="menu configuration" color="white" expanded="false" title="{{::translate.load('gisengine.designer.menuConfiguration')}}">
 	<div flex layout="row" layout-wrap> 
 	      
 	      <md-checkbox ng-model="visibility.showRightConfigMenu" aria-label="show right menu" flex=40 class="md-block">
            {{::translate.load('gisengine.designer.showRigtMenu')}}
          </md-checkbox> 
            <md-checkbox ng-model="visibility.showLegendButton" aria-label="show legend button" flex=40 class="md-block">
            {{::translate.load('gisengine.designer.showLegendButton')}}
          </md-checkbox> 
            <md-checkbox ng-model="visibility.showDistanceCalculator" aria-label="show distance calculator" flex=40 class="md-block">
            {{::translate.load('gisengine.designer.showDistanceCalculator')}}
          </md-checkbox> 
          
           
          <md-checkbox ng-model="visibility.showDownloadButton" aria-label="show download button" flex=40 class="md-block">
            {{::translate.load('gisengine.designer.showDownloadButton')}}
          </md-checkbox> 
              <md-checkbox ng-model="visibility.showSelectMode" ng-disabled="!visibility.showRightConfigMenu" aria-label="show select mode configuration" flex=40 class="md-block">
            {{::translate.load('gisengine.designer.showSelectMode')}}
          </md-checkbox> 
             <md-checkbox ng-model="visibility.showLayer" ng-disabled="!visibility.showRightConfigMenu" aria-label="show layer selection" flex=40 class="md-block">
            {{::translate.load('gisengine.designer.showLayer')}}
          </md-checkbox> 
             <md-checkbox ng-model="visibility.showBaseLayer" ng-disabled="!visibility.showRightConfigMenu" aria-label="show base layer selection" flex=40>
            {{::translate.load('gisengine.designer.showBaseLayer')}}
          </md-checkbox> 
          <md-checkbox ng-model="visibility.showMapConfig" ng-disabled="!visibility.showRightConfigMenu" aria-label="show map style configuration panel" flex=40>
            {{::translate.load('gisengine.designer.showMapConfig')}}
          </md-checkbox> 
 	</div>
 	</expander-box>
 </md-whiteframe>
</body>
</html>