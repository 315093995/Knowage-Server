<md-dialog aria-label="Save measure" class="saveDialogMeasureKpi" style="height:80%;width:80%" ng-cloak>
 
    <md-toolbar>
      <div class="md-toolbar-tools">
        <h2>{{translate.load("sbi.general.save.in.progress")}}</h2>
        <span flex></span>
	      <md-button ng-click="cancel()">
	      {{translate.load("sbi.general.cancel")}}
	      </md-button>
	      <md-button ng-click="save()" ng-disabled="currentRule.name.length==0">
	     {{translate.load("sbi.generic.update")}}
	      </md-button>
       </div>
    </md-toolbar>
    <md-dialog-content flex layout="column"  >
    
        <md-input-container class="md-block">
            <label> {{translate.load("sbi.generic.name")}}</label>
            <input ng-model="currentRule.name">
          </md-input-container>
       
      	
      	<div layout="column" flex  >
      		<md-toolbar md-scroll-shrink ng-if="newAlias.length>0 || reusedAlias.length>0"  >
			    <div class="md-toolbar-tools">
			      <h3>
			        <span> {{translate.load("sbi.kpi.alias")}}</span>
			      </h3>
			    </div>
			</md-toolbar>
			<div layour="row">
			
				<div flex  ng-if="newAlias.length>0">
					 <md-subheader class="md-primary">{{translate.load("sbi.generic.new")}}</md-subheader>
					<md-chips class="newChips" ng-model="newAlias" readonly="true">
					    <md-chip-template>
					        {{$chip}}
					    </md-chip-template>
					</md-chips>  
				</div>
				<div flex ng-if="reusedAlias.length>0">
					 <md-subheader class="md-primary">{{translate.load("sbi.generic.reused")}}</md-subheader>
					<md-chips class="reusedChips" ng-model="reusedAlias" readonly="true">
					    <md-chip-template>
					        {{$chip}}
					    </md-chip-template>
					</md-chips>
				</div>
			</div>
			<md-toolbar md-scroll-shrink ng-if="newPlaceholder.length>0 || reusedPlaceholder.length>0"  >
			    <div class="md-toolbar-tools">
			      <h3>
			        <span>{{translate.load("sbi.kpi.placeholder")}}</span>
			      </h3>
			    </div>
			</md-toolbar>
			
			<div layout="row">
				<div flex ng-if="newPlaceholder.length>0">
					 <md-subheader class="md-primary">{{translate.load("sbi.generic.new")}}</md-subheader>
					<md-chips class="newChips" ng-model="newPlaceholder" readonly="true">
					    <md-chip-template>
					        {{$chip}}
					    </md-chip-template>
					</md-chips>  
				</div>
				<div flex  ng-if="reusedPlaceholder.length>0">
					 <md-subheader class="md-primary">{{translate.load("sbi.generic.reused")}}</md-subheader>
					<md-chips  class="reusedChips"  ng-model="reusedPlaceholder" readonly="true">
					    <md-chip-template>
					        {{$chip}}
					    </md-chip-template>
					</md-chips>
				</div>
			</div>
			  
      	</div>
     
    </md-dialog-content> 
 
</md-dialog>