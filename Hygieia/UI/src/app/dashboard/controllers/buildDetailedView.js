/**
 * Controller for the modal popup when creating
 * a new dashboard on the startup page
 */
(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('BuildDetailedViewController', BuildDetailedViewController);

    BuildDetailedViewController.$inject = ['$location', '$uibModalInstance', 'dashboardData', 'userService', 'DashboardType', 'cmdbData', 'dashboardService', 'templateMangerData','$uibModal', 'ScoreDisplayType' , 'buildData'];
    function BuildDetailedViewController($location, $uibModalInstance, dashboardData, userService, DashboardType, cmdbData, dashboardService, templateMangerData,$uibModal, ScoreDisplayType, buildData) {

    
        var ctrl = this;

        ctrl.tabs = [
            { name: "Jira Detalis"},
            { name: "Commit Details"},
            { name: "Artifacts"}
        ];

        ctrl.tabView = ctrl.tabs[0].name;
        
        // public variables
        ctrl.buildData = buildData;
        ctrl.jira =[];
        ctrl.commits = [];
        ctrl.artifacts = [];

        buildCustomJirasArray();
        buildCustomCommitsArray();
        buildCustomArtifactsArray();

        //ctrl.jira = ctrl.buildData.jiras;
        //ctrl.dashboardtitle = 

        ctrl.sortByColumn = 'CustomerName';
        ctrl.sortByReverse = false;
        ctrl.sortBy = function(column) {
        if (column === ctrl.sortByColumn) {
            ctrl.sortByReverse = !ctrl.sortByReverse;
        } else {
            ctrl.sortByReverse = false;
        }

        ctrl.sortByColumn = column;
        };
        
        ctrl.getSortColumn = function () {
        // it has to be like this, otherwize, the `orderBy sortByColumn`
        // breaks for special names like "South Korea"
        return '"' + ctrl.sortByColumn + '"';
        };
        
        ctrl.myArray = [];
        // TODO: dynamically register templates with script
        
        // public methods
        ctrl.tabToggleView = tabToggleView;

        function buildCustomJirasArray(jiras){
           // ctrl.jira.clear();
            for (var x = 0; x < ctrl.buildData.jiras.length; x++) {
                var local = {
                    "Id": ctrl.buildData.jiras[x].jiraId ,
                    "Description": ctrl.buildData.jiras[x].jiraDescription , 
                    "State": ctrl.buildData.jiras[x].jiraState ,
                    "Crucible Id": ctrl.buildData.jiras[x].crucible                          
                } 

            ctrl.jira.push(local);            
            }
        }

        function buildCustomCommitsArray(){
            //ctrl.commits.clear();
            for (var x = 0; x < ctrl.buildData.commits.length; x++) {
                var local = {
                    "Id": ctrl.buildData.commits[x].commitId ,
                    "Description": ctrl.buildData.commits[x].commitDescription , 
                    "URL" : ctrl.buildData.commits[x].commitURL ,
                    "Author": ctrl.buildData.commits[x].commitAuthor ,
                    "Date": ctrl.buildData.commits[x].commitDate                          
                } 

            ctrl.commits.push(local);            
            }
        }

        function buildCustomArtifactsArray(){
            //ctrl.artifacts.clear();
            for (var x = 0; x < ctrl.buildData.artifacts.length; x++) {
                var local = {
                    "Name": ctrl.buildData.artifacts[x].artifactName ,
                    "Description": ctrl.buildData.artifacts[x].artifactDescription , 
                    "URL": ctrl.buildData.artifacts[x].artifactURL                        
                } 

            ctrl.artifacts.push(local);            
            }
        }

        function tabToggleView(index) {
            ctrl.dupErroMessage = "";
            ctrl.tabView = typeof ctrl.tabs[index] === 'undefined' ? ctrl.tabs[0].name : ctrl.tabs[index].name;
        };

        
    }
})();
