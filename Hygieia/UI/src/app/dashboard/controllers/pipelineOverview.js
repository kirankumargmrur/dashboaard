/**
 * Controller for the modal popup when creating
 * a new dashboard on the startup page
 */
(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('PipelineOverviewController', PipelineOverviewController);

    //PipelineOverviewController.$inject = ['$location', '$uibModalInstance', 'dashboardData', 'userService', 'DashboardType', 'cmdbData', 'dashboardService', 'templateMangerData','$uibModal', 'ScoreDisplayType' , 'collectorItemId'];
    //function PipelineOverviewController($location, $uibModalInstance, dashboardData, userService, DashboardType, cmdbData, dashboardService, templateMangerData,$uibModal, ScoreDisplayType, collectorItemId) {

    PipelineOverviewController.$inject = [ '$location', 'dashboardData', 'userService', 'DashboardType', 'cmdbData', 'dashboardService', 'templateMangerData','$uibModal', 'ScoreDisplayType' , '$stateParams' , 'buildData', 'paginationWrapperService'];
    function PipelineOverviewController($location, dashboardData, userService, DashboardType, cmdbData, dashboardService, templateMangerData,$uibModal, ScoreDisplayType, $stateParams, buildData , paginationWrapperService) {
        var ctrl = this;
                
        // public variables
        ctrl.collectorItemId = $stateParams.collectorId;
        ctrl.dashboardId = $stateParams.dashboardId;
        //ctrl.buildReleaseVersionMap = new Map();

        var buildReleaseVersionMap = new Map();
        var params = {
            componentId: "",
            numberOfDays: 15
        };

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
            return '"' + ctrl.sortByColumn + '"';
        };
        
        ctrl.myArray = [];

        ctrl.pipelineData = [];

        // TODO: dynamically register templates with script
        
        // public methods

        ctrl.buildDetails = buildDetails;
        ctrl.proceedDashboardView = proceedDashboardView;
        ctrl.buildDashboardReport = buildDashboardReport;

        fetchBuildReleaseVersions();

        //dashboardData.getPipeLineOverview(ctrl.collectorItemId).then(processPipeLineDetail);

        function fetchBuildReleaseVersions(){            
            dashboardData.search().then(function(data){
                //var localDashboard = paginationWrapperService.processDashboardResponse({"data" : data});
                var localDashboard = data
                if(localDashboard){
                    for (var x = 0; x < localDashboard.length; x++) {
                        if(localDashboard[x].id === ctrl.dashboardId){
                            if(localDashboard[x].application.components){
                                params.componentId = localDashboard[x].application.components[0].id;
                                dashboardData.getPipeLineOverview(ctrl.collectorItemId).then(processPipeLineDetail);                                 
                            }                                         
                        }
                    }
                }
            });
        }

        function processPipeLineDetail(response){            
            ctrl.pipelineData = response.result;
            buildData.details(params).then(function(data) {
                var builds = data.result;        
                for (var x = 0; x < builds.length; x++) {
                     buildReleaseVersionMap.set(parseInt(builds[x].number), builds[x].description);
                }

                for (var x = 0; x < ctrl.pipelineData.length; x++) {
                    var jira="", crucible = "";
                    if(ctrl.pipelineData[x].jiras.length != 0){
                        jira = ctrl.pipelineData[x].jiras[0].jiraId;
                        crucible = ctrl.pipelineData[x].jiras[0].crucible;
                    }
                    var local = {
                        "Id": ctrl.pipelineData[x].buildId,
                        "Version" : getBuildVersion(ctrl.pipelineData[x].buildId),
                        "Author": ctrl.pipelineData[x].buildAuthor, 
                        "Status": ctrl.pipelineData[x].buildStatus,
                        "Jira Id": jira, 
                        "Crucible Id": crucible,
                        "Date" : EpochToDate(ctrl.pipelineData[x].timestamp)                             
                    } 
        
                    ctrl.myArray.push(local);            
                }
             });               
        }

        function getBuildVersion(buildNumber){
            var tempVersion;
            if(buildReleaseVersionMap.has(buildNumber)){
                tempVersion = buildReleaseVersionMap.get(buildNumber);
            }
           return tempVersion;
        }

        function EpochToDate(epoch) {
            if (epoch < 10000000000)
                    epoch *= 1000; // convert to milliseconds (Epoch is usually expressed in seconds, but Javascript uses Milliseconds)
                var epoch = epoch + (new Date().getTimezoneOffset() * -1); //for timeZone
                return new Date(epoch);
        } 

        function buildDetails(build) {

            var data = getBuildData(build["Id"])
            $uibModal.open({
                    templateUrl: 'app/dashboard/views/buildDetailedView.html',
                    controller: 'BuildDetailedViewController',
                    controllerAs: 'ctrl',
                    size:'lg',
                    resolve: {
                        buildData: function () {
                          return data;
                        }
                      }
            });
        }

        function proceedDashboardView(){
            $location.path('/dashboard/' + ctrl.dashboardId);
        }

        function buildDashboardReport(buildId){
            //$location.path('/myURL/').search({param: 'value'});
            //var path ='/dashboard/'+ctrl.dashboardId).search({version: buildId});
            $location.path('/dashboard/'+ctrl.dashboardId).search({version: buildId});
        }

        function getBuildData(buildId){
            for (var x = 0; x < ctrl.pipelineData.length; x++) {
                if(ctrl.pipelineData[x].buildId === buildId){
                    return ctrl.pipelineData[x];
                }
            }
        }
    }
})();
