jQuery.noConflict();

if(ProcessArtifactsList == null) var ProcessArtifactsList = function() {};

ProcessArtifactsList.prototype.createArtifactsTable = function(procInstId, tblSelector) {

    this.grid = jQuery(tblSelector).jqGrid({ 
            url:'local',
            retrieveMode: 'function',
             
            populateFromFunction: function(params, callback) {
            
                params.piId = procInstId;
                                
                JbpmProcessArtifacts.processArtifactsList(params,
                    {
                        callback: function(result) {
                            console.log("got callback");
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
    var tbl = artifactsList.createArtifactsTable(344, "#list11");
});