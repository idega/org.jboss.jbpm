jQuery.noConflict();

jQuery(document).ready(function(){

    var mygrid = jQuery("#list11").jqGrid({ 
            url:'local',
            retrieveMode: 'function', 
            populateFromFunction: function(params, callback) {
            
                JbpmProcessArtifacts.processArtifactsList(params,
                    {
                        callback: function(result) {
                            console.log("got callback");
                            callback(result);
                        }
                    }
                );
            },
            
            populateSubgridFromFunction: function(params, callback) {
            
                JbpmProcessArtifacts.processArtifactsList(
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
});