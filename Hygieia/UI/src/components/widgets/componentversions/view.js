(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('VersionsAnalysisViewController', VersionsAnalysisViewController);

    VersionsAnalysisViewController.$inject = ['$scope', 'componentVersionsData', 'testSuiteData', '$q', '$filter', '$uibModal', 'versionFilter'];
    function VersionsAnalysisViewController($scope, componentVersionsData, testSuiteData, $q, $filter, $uibModal, versionFilter) {
        var ctrl = this;
        var versionFilter = versionFilter.getVersionFilterData();

        function createModal(label, historicalSanityData) {
            console.log('Inside create Modal and logging historicalSanityData')
            console.log(historicalSanityData)
            var dataToDisplay = null
            historicalSanityData.forEach(function (sanityData) {
                if (sanityData['executionId'] == label.toString()) {
                    dataToDisplay = sanityData
                }
            });
            showDetail(dataToDisplay)
        }

        $scope.chartistEvents = {
            created: function () {
                $('.ct-chart-bar .ct-series-a line, .ct-chart-bar .ct-series-b line, .ct-chart-bar .ct-series-c line, .ct-chart-bar .ct-series-d line').click(function () {
                    var index = $(this).index();
                    var label = $(this).closest('.ct-chart-bar').find('.ct-labels foreignObject:nth-child(' + (index + 1) + ') span').text();
                    console.log('label ' + label);
                    if (ctrl.showSanityTab)
                        createModal(label, ctrl.historicalSanityData);
                    if (ctrl.showFunctionalTab)
                        createModal(label, ctrl.historicalFunctionalData);
                    if (ctrl.showUnitTab)
                        createModal(label, ctrl.historicalUnitData);
                    if (ctrl.showIntegrationTab)
                        createModal(label, ctrl.historicalIntegrationData);
                });
            }
        }


        ctrl.pieOptions = {
            donut: true,
            donutWidth: 20,
            startAngle: 270,
            total: 200,
            showLabel: false
        };

        ctrl.minitabs = [
            { name: 'Components' },
        ];

        ctrl.showComponents = showDetail;
        ctrl.getData = getData;
        var responseData = null;
        ctrl.miniWidgetView = ctrl.minitabs[0].name;
        ctrl.miniToggleView = function (index) {
            ctrl.miniWidgetView = typeof ctrl.minitabs[index] === 'undefined' ? ctrl.minitabs[0].name : ctrl.minitabs[index].name;
        };



        ctrl.load = function () {

            var caRequest = {
                componentId: $scope.widgetConfig.componentId,
                max: 5
            };

            return $q.all([
                componentVersionsData.details(caRequest).then(processCaResponse)
            ]);
        };

        function processCaResponse(response) {
            var deferred = $q.defer()
            ctrl.components = []
            ctrl.buildNumbers = []
            var caData = _.isEmpty(response.result) ? {} : response.result;
            responseData = caData;
            for (var k = 0; k < caData.length; k++) {
                ctrl.buildNumbers.push(caData[k].buildId);
            }
            ctrl.buildNumbers.sort(function (a, b) { return b - a });
            ctrl.defaultBuildId = ctrl.buildNumbers[0];
            ctrl.buildIds = [];
            for (var k = 1; k < ctrl.buildNumbers.length; k++) {
                ctrl.buildIds.push(ctrl.buildNumbers[k]);
            }
            ctrl.components = [];
            for (var j = 0; j < responseData.length; j++) {
                if (responseData[j].buildId == ctrl.defaultBuildId) {
                    for (var i = 0; i < responseData[j].componentVersions.length; i++) {
                        ctrl.components.push({ 'name': responseData[j].componentVersions[i].name, 'version': responseData[j].componentVersions[i].version, 'components': responseData[j].componentVersions[i].components });
                    }
                    break;
                }
            }
        }

        function getData(buildId) {
            if (buildId == null) {
                buildId = ctrl.defaultBuildId;
            }
            ctrl.components = [];
            for (var j = 0; j < responseData.length; j++) {
                if (responseData[j].buildId == buildId) {
                    for (var i = 0; i < responseData[j].componentVersions.length; i++) {
                        ctrl.components.push({ 'name': responseData[j].componentVersions[i].name, 'version': responseData[j].componentVersions[i].version, 'components': responseData[j].componentVersions[i].components });
                    }
                    break;
                }
            }
        }

        function showDetail(test) {
            $uibModal.open({
                controller: 'VersionDetailsController',
                controllerAs: 'versionDetails',
                templateUrl: 'components/widgets/componentversions/testdetails.html',
                size: 'lg',
                resolve: {
                    testResult: function () {
                        return test;
                    }
                }
            });
        }
    }
})
    ();