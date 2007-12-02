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
            colNames:['Nr','Task name', 'Created date'], 
            colModel:[ 
                {name:'id',index:'id'},
                {name:'name',index:'name'}, 
                {name:'createdDate',index:'createdDate'} 
            ], 
            rowNum: null, 
            rowList: null, 
            pager: null, 
            sortname: 'id',
            viewrecords: true, 
            sortorder: "desc", 
            multiselect: false, 
            subGrid : false,
            onSelectRow: function(rowId) {
            
                JbpmProcessArtifacts.getViewDisplay(rowId, {
                        callback: function(result) {
                        
                            var container = document.getElementById("viewDisplay");
                            jQuery(container).empty();
                            
                            insertNodesToContainer(result, container);
                        }
                    }
                );
            }
        });
        
        return this.grid;
}

jQuery(document).ready(function(){

    var artifactsList = new ProcessArtifactsList();
    var tbl = artifactsList.createArtifactsTable("#artifactsList");
});