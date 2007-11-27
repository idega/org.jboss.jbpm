jQuery.noConflict();

if(ProcessArtifactsList == null) var ProcessArtifactsList = function() {};

ProcessArtifactsList.prototype.createArtifactsTable = function(tblSelector) {

    this.grid = jQuery(tblSelector);
    var processInstanceId = this.grid.attr("processInstanceId");
        
    this.grid = this.grid.jqGrid({ 
            url:'local',
            retrieveMode: 'function',
             
            populateFromFunction: function(params, callback) {
            
                params.piId = processInstanceId;
                                
                JbpmProcessArtifacts.processArtifactsList(params,
                    {
                        callback: function(result) {
                            callback(result);
                        }
                    }
                );
            },
            
            datatype: "xml", 
            height: 250, 
            colNames:['Inv No','Date', 'Client', 'Amount','Tax','Total','Notes'], 
            colModel:[ 
                {name:'id',index:'id', width:55}, 
                {name:'invdate',index:'invdate', width:90}, 
                {name:'name',index:'name', width:100}, 
                {name:'amount',index:'amount', width:80, align:"right"}, 
                {name:'tax',index:'tax', width:80, align:"right"}, 
                {name:'total',index:'total', width:80,align:"right"}, 
                {name:'note',index:'note', width:150, sortable:false} 
            ], 
            rowNum: null, 
            rowList: null, 
            pager: null, 
            sortname: 'id',
            viewrecords: true, 
            sortorder: "desc", 
            multiselect: false, 
            subGrid : false 
        });
        
        return this.grid;
}

jQuery(document).ready(function(){

    var artifactsList = new ProcessArtifactsList();
    var tbl = artifactsList.createArtifactsTable("#artifactsList");
    
    //console.log(tbl.attr("processInstanceId"));
});