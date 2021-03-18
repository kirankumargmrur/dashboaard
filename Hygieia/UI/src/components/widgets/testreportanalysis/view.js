(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('TestReportAnalysisViewController', TestReportAnalysisViewController);

    TestReportAnalysisViewController.$inject = ['$scope', 'testSuiteData', '$q', '$filter', '$uibModal', 'versionFilter'];
    function TestReportAnalysisViewController($scope,  testSuiteData, $q, $filter, $uibModal, versionFilter) {
        var ctrl = this;
        var versionFilter = versionFilter.getVersionFilterData();

        function createModal(label, historicalSanityData) {
           console.log('Inside create Modal and logging historicalSanityData')
           console.log(historicalSanityData)
           var dataToDisplay = null
           historicalSanityData.forEach(function(sanityData){
            if(sanityData['executionId'] == label.toString()){
                    dataToDisplay = sanityData
            }
          });
           showDetail(dataToDisplay)
        }

        $scope.chartistEvents = {
             created : function(){
               $('.ct-chart-bar .ct-series-a line, .ct-chart-bar .ct-series-b line, .ct-chart-bar .ct-series-c line, .ct-chart-bar .ct-series-d line').click(function() {
                  var index = $(this).index();
                  var label = $(this).closest('.ct-chart-bar').find('.ct-labels foreignObject:nth-child('+(index+1)+') span').text();
                console.log('label ' +  label);
                if (ctrl.showSanityTab)
                   createModal(label,ctrl.historicalSanityData);
                if (ctrl.showFunctionalTab)
                   createModal(label,ctrl.historicalFunctionalData);
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
            { name: "Summary"},
            { name: "Unit"},
            { name: "Regression"},
            { name: "Functional"},
            { name: "Sanity"}
        ];

        ctrl.miniWidgetView = ctrl.minitabs[0].name;
        ctrl.miniToggleView = function (index) {
            ctrl.miniWidgetView = typeof ctrl.minitabs[index] === 'undefined' ? ctrl.minitabs[0].name : ctrl.minitabs[index].name;
        };

        ctrl.showUnitTab = false;
        ctrl.showSanityTab = false;
        ctrl.showFunctionalTab = false;
        ctrl.showIntegrationTab = false;
        ctrl.showStatusIcon = showStatusIcon;
        ctrl.showDetail = showDetail;
        ctrl.historicalSanityData = [];
        ctrl.historicalUnitData = [];
        ctrl.historicalFunctionalData = [];
        ctrl.historicalIntegrationData = [];
        //ctrl.showLibraryPolicyDetails = showLibraryPolicyDetails;


        //coveragePieChart({});
        unitTestcaseCoveragePieChart({});
        unitTestsuiteCoveragePieChart({});

        sanityTestcaseCoveragePieChart({});
        sanityTestsuiteCoveragePieChart({});

        functionalTestcaseCoveragePieChart({});
        functionalTestsuiteCoveragePieChart({});

        getBarChartDetails('all');

        ctrl.load = function () {

            var unitTestRequest = {
                componentId: $scope.widgetConfig.componentId,
                types: ['Unit'],
                max: 1
            };
            var functionalTestRequest = {
                componentId: $scope.widgetConfig.componentId,
                types: ['Functional'],
                max: 1
            };
            var SanityTestRequest = {
                componentId: $scope.widgetConfig.componentId,
                types: ['Regression'],
                max: 1
            };
            var IntegrationTestRequest = {
                componentId: $scope.widgetConfig.componentId,
                types: ['Integration'],
                max: 1
            };

            var AllRegressionTestRequest = {
                componentId: $scope.widgetConfig.componentId,
                types: ['Regression'],
                max: 5
            };

            var AllUnitTestRequest = {
                componentId: $scope.widgetConfig.componentId,
                types: ['Unit'],
                max: 5
            };

            var AllFunctionalTestRequest = {
                componentId: $scope.widgetConfig.componentId,
                types: ['Functional'],
                max: 5
            };

            var AllIntegrationTestRequest = {
                componentId: $scope.widgetConfig.componentId,
                types: ['Integration'],
                max: 5
            };
            return $q.all([
                testSuiteData.details(unitTestRequest).then(processUnitTestResponse),
                testSuiteData.details(functionalTestRequest).then(processFunctionalTestResponse),
                //testSuiteData.details(SanityTestRequest).then(processFunctionalTestResponse),
                testSuiteData.details(SanityTestRequest).then(processSanityTestResponse),
                testSuiteData.details(IntegrationTestRequest).then(processIntegrationTestResponse),
                testSuiteData.details(AllFunctionalTestRequest).then(processAllFunctionalTestResponse),
                testSuiteData.details(AllRegressionTestRequest).then(processAllRegressionTestResponse),
                testSuiteData.details(AllUnitTestRequest).then(processAllUnitTestResponse),
                testSuiteData.details(AllIntegrationTestRequest).then(processAllIntegrationTestResponse)
            ]);
        };

        ctrl.barOptions = {
          seriesBarDistance: 10,
          low  : 0 ,
          high : 100,
          scaleMinSpace: 15,
          plugins : [Chartist.plugins.tooltip(),
            Chartist.plugins.ctAxisTitle({
            axisX: {
              axisTitle: 'BUILD ID',
              //axisClass: 'ct-axis-title',
              offset: {
                x: 0,
                y: 30
              },
              textAnchor: 'middle',
              flipTitle: false,

            },
            axisY: {
              axisTitle: 'PASS %',
              //axisClass: 'ct-axis-title',
              offset: {
                x: 0,
                y: 0
              },
              textAnchor: 'middle',
              flipTitle: false,
            }
          }),

          ]
        };



        function processAllRegressionTestResponse(response) {
            console.log("processAllRegressionTestResponse");
            console.debug(response);
            ctrl.historicalSanityData = [];
            var deferred = $q.defer();

            var index;
            var totalSize = _.isEmpty(response.result) ? 0 : response.result.length;
            for (index = 0; index < totalSize; ++index) {
              var testResult = _.isEmpty(response.result) ? {testCapabilities: []} : response.result[index];
              if(testResult.executionId === versionFilter){
                ctrl.sanityTests = [];

              }

              if((testResult.executionId === versionFilter) || (versionFilter === -1)) {
              var allZeros = {
                  failedTestSuiteCount: 0, successTestSuiteCount: 0, skippedTestSuiteCount: 0, totalTestSuiteCount: 0,
                  failedTestCaseCount: 0, successTestCaseCount: 0, skippedTestCaseCount: 0, totalTestCaseCount: 0
              };

              // Aggregate the counts of all Functional test suites
              var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Regression"}), function (result, capability) {
                  result.failedTestSuiteCount += capability.failedTestSuiteCount;
	          result.successTestSuiteCount += capability.successTestSuiteCount;
		  result.skippedTestSuiteCount += capability.skippedTestSuiteCount;
		  result.totalTestSuiteCount += capability.totalTestSuiteCount;
                  var testSuites = capability.testSuites;
                  for (var index in testSuites) {
                      //console.log("determine unit testcase results - totaltestcase");
                      //console.log(testSuites[index].totalTestCaseCount);
                      result.failedTestCaseCount += testSuites[index].failedTestCaseCount;
                      result.successTestCaseCount += testSuites[index].successTestCaseCount;
                      result.skippedTestCaseCount += testSuites[index].skippedTestCaseCount;
                      result.totalTestCaseCount += testSuites[index].totalTestCaseCount;
                      /*ctrl.historicalSanityData.push({
                          executionId:  _.isEmpty(response.result) ? "-" : response.result[index].executionId,
                          passPercent: successTestCase
                      }); */
                  }

                  return result;
              }, allZeros);

              if(testResult.executionId === versionFilter){
              var passed = aggregate.successTestSuiteCount;
	      var allPassed = aggregate.successTestSuiteCount === (aggregate.totalTestSuiteCount - aggregate.skippedTestSuiteCount);
	      var success = allPassed ? 100 : ((passed / (aggregate.totalTestSuiteCount-aggregate.skippedTestSuiteCount)) * 100);

	      sanityTestsuiteCoveragePieChart(success);
              }
              var passedTestCase = aggregate.successTestCaseCount;
              var allPassedTestCase = aggregate.successTestCaseCount === (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount);
              var successTestCase = allPassedTestCase ? 100 : ((passedTestCase / (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount)) * 100);
	      console.log("successTestCase percent: " + successTestCase);
	      var date = EpochToDate(response.result[index].timestamp);

              if(testResult.executionId === versionFilter){
                 sanityTestcaseCoveragePieChart(successTestCase);
		  ctrl.executionId = _.isEmpty(response.result) ? "-" : response.result[index].executionId;
		  ctrl.sanityTests.push({
			  name: $scope.widgetConfig.options.testJobNames[index],
			  totalTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.totalTestSuiteCount, 0),
			  successTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.successTestSuiteCount, 0),
			  failedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.failedTestSuiteCount, 0),
			  skippedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.skippedTestSuiteCount, 0),
			  totalTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.totalTestCaseCount, 0),
			  successTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.successTestCaseCount, 0),
			  failedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.failedTestCaseCount, 0),
			  skippedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.skippedTestCaseCount, 0),
			  testsuiteSuccessPercent: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(success, 0) + '%',
			  testcaseSuccessPercent: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(successTestCase, 0) + '%',
			  //unitTestsDone: $filter('number')(success, 0) > 95 ? true: false,
			  sanityTestsDone: $filter('number')(successTestCase, 0) > 95 ? true: false,
			  //functionalTestsDone: $filter('number')(success, 0) > 95 ? true: false,
			  details: testResult
		  });
                }

              /*   ctrl.historicalSanityData.push({
                  executionId:  _.isEmpty(response.result) ? "-" : response.result[index].executionId,
                  passPercent: successTestCase,
                  timeStamp: date
              }); */
              response.result[index]['passPercent'] = successTestCase;
              response.result[index]['timeStamp'] = date;
              ctrl.historicalSanityData.push(response.result[index]);
              }
          }
          console.log("Historical sanity data :")
          console.log(ctrl.historicalSanityData);
          getBarChartDetails('sanity');
          deferred.resolve(response.lastUpdated);
          return deferred.promise;
        }

	function EpochToDate(epoch) {
	    if (epoch < 10000000000)
       		 epoch *= 1000; // convert to milliseconds (Epoch is usually expressed in seconds, but Javascript uses Milliseconds)
    	    var epoch = epoch + (new Date().getTimezoneOffset() * -1); //for timeZone
            return new Date(epoch);
	}

        function processAllFunctionalTestResponse(response) {
            console.log("processAllFunctionalTestResponse");
            console.debug(response);
            ctrl.historicalFunctionalData = [];
            var deferred = $q.defer();

            var index;
            var totalSize = _.isEmpty(response.result) ? 0 : response.result.length;
            for (index = 0; index < totalSize; ++index) {
              var testResult = _.isEmpty(response.result) ? {testCapabilities: []} : response.result[index];
              var allZeros = {
                  failedTestCaseCount: 0, successTestCaseCount: 0, skippedTestCaseCount: 0, totalTestCaseCount: 0
              };

              // Aggregate the counts of all Functional test suites
              var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Functional"}), function (result, capability) {
                  var testSuites = capability.testSuites;
                  for (var index in testSuites) {
                      //console.log("determine unit testcase results - totaltestcase");
                      //console.log(testSuites[index].totalTestCaseCount);
                      result.failedTestCaseCount += testSuites[index].failedTestCaseCount;
                      result.successTestCaseCount += testSuites[index].successTestCaseCount;
                      result.skippedTestCaseCount += testSuites[index].skippedTestCaseCount;
                      result.totalTestCaseCount += testSuites[index].totalTestCaseCount;

                      /*ctrl.historicalFunctionalData.push({
                          executionId:  _.isEmpty(response.result) ? "-" : response.result[index].executionId,
                          passPercent: successTestCase
                      }); */
                  }

                  return result;
              }, allZeros);

              var passedTestCase = aggregate.successTestCaseCount;
              var allPassedTestCase = aggregate.successTestCaseCount === (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount);
              var successTestCase = allPassedTestCase ? 100 : ((passedTestCase / (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount)) * 100);
	      var date = EpochToDate(response.result[index].timestamp);

                 /*ctrl.historicalFunctionalData.push({
                  executionId:  _.isEmpty(response.result) ? "-" : response.result[index].executionId,
                  passPercent: successTestCase,
                  timeStamp: date})*/
               response.result[index]['passPercent'] = successTestCase;
               response.result[index]['timeStamp'] = date;

               ctrl.historicalFunctionalData.push(response.result[index]);

          }
          console.log("Historical Functional data :")
          console.log(ctrl.historicalFunctionalData);
          getBarChartDetails('functional');
          deferred.resolve(response.lastUpdated);
          return deferred.promise;
        }

        function processAllUnitTestResponse(response) {
            console.log("processAllUnitTestResponse");
            console.debug(response);
            ctrl.historicalUnitData = [];
                        var deferred = $q.defer();

                        var index;
                        var totalSize = _.isEmpty(response.result) ? 0 : response.result.length;
                        for (index = 0; index < totalSize; ++index) {
                          var testResult = _.isEmpty(response.result) ? {testCapabilities: []} : response.result[index];
                          var allZeros = {
                              failedTestCaseCount: 0, successTestCaseCount: 0, skippedTestCaseCount: 0, totalTestCaseCount: 0
                          };

                          // Aggregate the counts of all Functional test suites
                          var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Unit"}), function (result, capability) {
                              var testSuites = capability.testSuites;
                              for (var index in testSuites) {
                                  //console.log("determine unit testcase results - totaltestcase");
                                  //console.log(testSuites[index].totalTestCaseCount);
                                  result.failedTestCaseCount += testSuites[index].failedTestCaseCount;
                                  result.successTestCaseCount += testSuites[index].successTestCaseCount;
                                  result.skippedTestCaseCount += testSuites[index].skippedTestCaseCount;
                                  result.totalTestCaseCount += testSuites[index].totalTestCaseCount;

                                  /*ctrl.historicalUnitData.push({
                                      executionId:  _.isEmpty(response.result) ? "-" : response.result[index].executionId,
                                      passPercent: successTestCase
                                  });*/
                              }

                              return result;
                          }, allZeros);

                          var passedTestCase = aggregate.successTestCaseCount;
                          var allPassedTestCase = aggregate.successTestCaseCount === (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount);
                          var successTestCase = allPassedTestCase ? 100 : ((passedTestCase / (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount)) * 100);
			  var date = EpochToDate(response.result[index].timestamp);
                          /*ctrl.historicalUnitData.push({
                              executionId:  _.isEmpty(response.result) ? "-" : response.result[index].executionId,
                              passPercent: successTestCase,
                              timeStamp: date
                          });*/

                          response.result[index]['passPercent'] = successTestCase;
                          response.result[index]['timeStamp'] = date;
                          ctrl.historicalUnitData.push(response.result[index]);
                      }
                      console.log("Historical Unit data :")
                      console.log(ctrl.historicalUnitData);
                      //getBarChartDetails();
                      deferred.resolve(response.lastUpdated);
                      getBarChartDetails('unit');
                      return deferred.promise;
        }

        function processAllIntegrationTestResponse(response) {
            console.log("processAllIntegrationTestResponse");
            console.debug(response);
            ctrl.historicalIntegrationData = [];
                        var deferred = $q.defer();

                        var index;
                        var totalSize = _.isEmpty(response.result) ? 0 : response.result.length;
                        for (index = 0; index < totalSize; ++index) {
                          var testResult = _.isEmpty(response.result) ? {testCapabilities: []} : response.result[index];
                          var allZeros = {
                              failedTestCaseCount: 0, successTestCaseCount: 0, skippedTestCaseCount: 0, totalTestCaseCount: 0
                          };

                          // Aggregate the counts of all Functional test suites
                          var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Integration"}), function (result, capability) {
                              var testSuites = capability.testSuites;
                              for (var index in testSuites) {
                                  //console.log("determine unit testcase results - totaltestcase");
                                  //console.log(testSuites[index].totalTestCaseCount);
                                  result.failedTestCaseCount += testSuites[index].failedTestCaseCount;
                                  result.successTestCaseCount += testSuites[index].successTestCaseCount;
                                  result.skippedTestCaseCount += testSuites[index].skippedTestCaseCount;
                                  result.totalTestCaseCount += testSuites[index].totalTestCaseCount;

                                  /*ctrl.historicalUnitData.push({
                                      executionId:  _.isEmpty(response.result) ? "-" : response.result[index].executionId,
                                      passPercent: successTestCase
                                  });*/
                              }

                              return result;
                          }, allZeros);

                          var passedTestCase = aggregate.successTestCaseCount;
                          var allPassedTestCase = aggregate.successTestCaseCount === (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount);
                          var successTestCase = allPassedTestCase ? 100 : ((passedTestCase / (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount)) * 100);
        var date = EpochToDate(response.result[index].timestamp);
                          /*ctrl.historicalUnitData.push({
                              executionId:  _.isEmpty(response.result) ? "-" : response.result[index].executionId,
                              passPercent: successTestCase,
                              timeStamp: date
                          });*/

                          response.result[index]['passPercent'] = successTestCase;
                          response.result[index]['timeStamp'] = date;
                          ctrl.historicalIntegrationData.push(response.result[index]);
                      }
                      console.log("Historical Integration data :")
                      console.log(ctrl.historicalIntegrationData);
                      //getBarChartDetails();
                      deferred.resolve(response.lastUpdated);
                      getBarChartDetails('integration');
                      return deferred.promise;
        }


        function getSecurityMetricsData (data) {
            var issues = [];
            var totalSize = _.isEmpty(data.metrics) ? 0 : data.metrics.length;
            for (var index = 0; index < totalSize; ++index) {
                issues.push({name: data.metrics[index].name, formattedValue : data.metrics[index].formattedValue, status:data.metrics[index].status});
            }
            return issues;
        }


        function processUnitTestResponse(response) {
            var deferred = $q.defer();

            ctrl.testResult = testResult;

            ctrl.unitTests = [];
            var index;
            var totalSize = _.isEmpty(response.result) ? 0 : response.result.length;
            if (totalSize != 0) {
               ctrl.showUnitTab = true;
            }

            for (index = 0; index < totalSize; ++index) {

                var testResult = _.isEmpty(response.result) ? {testCapabilities: []} : response.result[index];
                var allZeros = {
                    failedTestSuiteCount: 0, successTestSuiteCount: 0, skippedTestSuiteCount: 0, totalTestSuiteCount: 0,
                    failedTestCaseCount: 0, successTestCaseCount: 0, skippedTestCaseCount: 0, totalTestCaseCount: 0
                };

                // Aggregate the counts of all Functional test suites
                var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Unit"}), function (result, capability) {
                    //New calculation: 3/10/16 - Topo Pal
                    result.failedTestSuiteCount += capability.failedTestSuiteCount;
                    result.successTestSuiteCount += capability.successTestSuiteCount;
                    result.skippedTestSuiteCount += capability.skippedTestSuiteCount;
                    result.totalTestSuiteCount += capability.totalTestSuiteCount;

                    var testSuites = capability.testSuites;
                    for (var index in testSuites) {
                        //console.log("determine unit testcase results - totaltestcase");
                        //console.log(testSuites[index].totalTestCaseCount);
                        result.failedTestCaseCount += testSuites[index].failedTestCaseCount;
                        result.successTestCaseCount += testSuites[index].successTestCaseCount;
                        result.skippedTestCaseCount += testSuites[index].skippedTestCaseCount;
                        result.totalTestCaseCount += testSuites[index].totalTestCaseCount;
                    }

                    return result;
                }, allZeros);


                /*var testcaseAggregate = _.reduce(_.filter(testResult.testCapabilities.testSuites, {type: "Functional"}), function (result, testsuites) {
                    //New calculation: 3/10/16 - Topo Pal
                    result.testsuitename += testsuites.description;
                    result.totalTestSuiteCount += testsuites.totalTestCaseCount;
                    result.failedCount += testsuites.failedTestCaseCount;
                    result.successTestSuiteCount += testsuites.successTestCaseCount;
                    result.skippedTestSuiteCount += testsuites.skippedTestCaseCount

                    return result;
                }, allZeros);*/
                var passed = aggregate.successTestSuiteCount;
                var allPassed = aggregate.successTestSuiteCount === (aggregate.totalTestSuiteCount - aggregate.skippedTestSuiteCount);
                var success = allPassed ? 100 : ((passed / (aggregate.totalTestSuiteCount - aggregate.skippedTestSuiteCount)) * 100);
                unitTestsuiteCoveragePieChart(success);
                var passedTestCase = aggregate.successTestCaseCount;
                var allPassedTestCase = aggregate.successTestCaseCount === (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount);
                var successTestCase = allPassedTestCase ? 100 : ((passedTestCase / (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount)) * 100);
                unitTestcaseCoveragePieChart(successTestCase);

                ctrl.executionId = _.isEmpty(response.result) ? "-" : response.result[index].executionId;
                ctrl.unitTests.push({
                    name: $scope.widgetConfig.options.testJobNames[index],
                    totalTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.totalTestSuiteCount, 0),
                    successTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.successTestSuiteCount, 0),
                    failedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.failedTestSuiteCount, 0),
                    skippedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.skippedTestSuiteCount, 0),
                    totalTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.totalTestCaseCount, 0),
                    successTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.successTestCaseCount, 0),
                    failedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.failedTestCaseCount, 0),
                    skippedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.skippedTestCaseCount, 0),
                    testsuiteSuccessPercent: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(success, 0) + '%',
                    testcaseSuccessPercent: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(successTestCase, 0) + '%',
                    unitTestsDone: $filter('number')(successTestCase, 0) > 95 ? true: false,
                    //sanityTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                    //functionalTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                    details: testResult
                });
            }
            deferred.resolve(response.lastUpdated);
            return deferred.promise;
        }

        function processFunctionalTestResponse(response) {
          var deferred = $q.defer();

          ctrl.testResult = testResult;

          ctrl.functionalTests = [];
          var index;
          var totalSize = _.isEmpty(response.result) ? 0 : response.result.length;
          if (totalSize != 0) {
             ctrl.showFunctionalTab = true;
          }
          for (index = 0; index < totalSize; ++index) {

              var testResult = _.isEmpty(response.result) ? {testCapabilities: []} : response.result[index];
              var allZeros = {
                  failedTestSuiteCount: 0, successTestSuiteCount: 0, skippedTestSuiteCount: 0, totalTestSuiteCount: 0,
                  failedTestCaseCount: 0, successTestCaseCount: 0, skippedTestCaseCount: 0, totalTestCaseCount: 0
              };

              // Aggregate the counts of all Functional test suites
              var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Functional"}), function (result, capability) {
              //var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "RegressioRegressionn"}), function (result, capability) {
              //var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Regression"}), function (result, capability) {
                  //New calculation: 3/10/16 - Topo Pal
                  result.failedTestSuiteCount += capability.failedTestSuiteCount;
                  result.successTestSuiteCount += capability.successTestSuiteCount;
                  result.skippedTestSuiteCount += capability.skippedTestSuiteCount;
                  result.totalTestSuiteCount += capability.totalTestSuiteCount;

                  var testSuites = capability.testSuites;
                  for (var index in testSuites) {
                      console.log("determine functional testcase results - totaltestcase");
                      console.log(testSuites[index].totalTestCaseCount);
                      result.failedTestCaseCount += testSuites[index].failedTestCaseCount;
                      result.successTestCaseCount += testSuites[index].successTestCaseCount;
                      result.skippedTestCaseCount += testSuites[index].skippedTestCaseCount;
                      result.totalTestCaseCount += testSuites[index].totalTestCaseCount;
                  }

                  return result;
              }, allZeros);


              /*var testcaseAggregate = _.reduce(_.filter(testResult.testCapabilities.testSuites, {type: "Functional"}), function (result, testsuites) {
                  //New calculation: 3/10/16 - Topo Pal
                  result.testsuitename += testsuites.description;
                  result.totalTestSuiteCount += testsuites.totalTestCaseCount;
                  result.failedCount += testsuites.failedTestCaseCount;
                  result.successTestSuiteCount += testsuites.successTestCaseCount;
                  result.skippedTestSuiteCount += testsuites.skippedTestCaseCount

                  return result;
              }, allZeros);*/
              var passed = aggregate.successTestSuiteCount;
              var allPassed = aggregate.successTestSuiteCount === (aggregate.totalTestSuiteCount - aggregate.skippedTestSuiteCount);
              var success = allPassed ? 100 : ((passed / (aggregate.totalTestSuiteCount - aggregate.skippedTestSuiteCount)) * 100);
              functionalTestsuiteCoveragePieChart(success);
              var passedTestCase = aggregate.successTestCaseCount;
              var allPassedTestCase = aggregate.successTestCaseCount === (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount);
              var successTestCase = allPassedTestCase ? 100 : ((passedTestCase / (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount)) * 100);
              functionalTestcaseCoveragePieChart(successTestCase);

              ctrl.executionId = _.isEmpty(response.result) ? "-" : response.result[index].executionId;
              ctrl.functionalTests.push({
                  name: $scope.widgetConfig.options.testJobNames[index],
                  totalTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.totalTestSuiteCount, 0),
                  successTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.successTestSuiteCount, 0),
                  failedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.failedTestSuiteCount, 0),
                  skippedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.skippedTestSuiteCount, 0),
                  totalTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.totalTestCaseCount, 0),
                  successTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.successTestCaseCount, 0),
                  failedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.failedTestCaseCount, 0),
                  skippedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.skippedTestCaseCount, 0),
                  testsuiteSuccessPercent: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(success, 0) + '%',
                  testcaseSuccessPercent: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(successTestCase, 0) + '%',
                  //unitTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                  //sanityTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                  functionalTestsDone: $filter('number')(successTestCase, 0) > 95 ? true: false,
                  details: testResult
              });
          }
          deferred.resolve(response.lastUpdated);
          return deferred.promise;

        }

        function processIntegrationTestResponse(response) {
          var deferred = $q.defer();

          ctrl.testResult = testResult;

          ctrl.integrationTests = [];
          var index;
          var totalSize = _.isEmpty(response.result) ? 0 : response.result.length;
          if (totalSize != 0) {
             ctrl.showIntegrationTab = true;
          }
          for (index = 0; index < totalSize; ++index) {

              var testResult = _.isEmpty(response.result) ? {testCapabilities: []} : response.result[index];
              var allZeros = {
                  failedTestSuiteCount: 0, successTestSuiteCount: 0, skippedTestSuiteCount: 0, totalTestSuiteCount: 0,
                  failedTestCaseCount: 0, successTestCaseCount: 0, skippedTestCaseCount: 0, totalTestCaseCount: 0
              };

              // Aggregate the counts of all Functional test suites
              var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Integration"}), function (result, capability) {
              //var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "RegressioRegressionn"}), function (result, capability) {
              //var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Regression"}), function (result, capability) {
                  //New calculation: 3/10/16 - Topo Pal
                  result.failedTestSuiteCount += capability.failedTestSuiteCount;
                  result.successTestSuiteCount += capability.successTestSuiteCount;
                  result.skippedTestSuiteCount += capability.skippedTestSuiteCount;
                  result.totalTestSuiteCount += capability.totalTestSuiteCount;

                  var testSuites = capability.testSuites;
                  for (var index in testSuites) {
                      console.log("determine integration testcase results - totaltestcase");
                      console.log(testSuites[index].totalTestCaseCount);
                      result.failedTestCaseCount += testSuites[index].failedTestCaseCount;
                      result.successTestCaseCount += testSuites[index].successTestCaseCount;
                      result.skippedTestCaseCount += testSuites[index].skippedTestCaseCount;
                      result.totalTestCaseCount += testSuites[index].totalTestCaseCount;
                  }

                  return result;
              }, allZeros);


              var passed = aggregate.successTestSuiteCount;
              var allPassed = aggregate.successTestSuiteCount === (aggregate.totalTestSuiteCount - aggregate.skippedTestSuiteCount);
              var success = allPassed ? 100 : ((passed / (aggregate.totalTestSuiteCount - aggregate.skippedTestSuiteCount)) * 100);
              integrationTestsuiteCoveragePieChart(success);
              var passedTestCase = aggregate.successTestCaseCount;
              var allPassedTestCase = aggregate.successTestCaseCount === (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount);
              var successTestCase = allPassedTestCase ? 100 : ((passedTestCase / (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount)) * 100);
              integrationTestcaseCoveragePieChart(successTestCase);

              ctrl.executionId = _.isEmpty(response.result) ? "-" : response.result[index].executionId;
              ctrl.integrationTests.push({
                  name: $scope.widgetConfig.options.testJobNames[index],
                  totalTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.totalTestSuiteCount, 0),
                  successTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.successTestSuiteCount, 0),
                  failedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.failedTestSuiteCount, 0),
                  skippedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.skippedTestSuiteCount, 0),
                  totalTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.totalTestCaseCount, 0),
                  successTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.successTestCaseCount, 0),
                  failedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.failedTestCaseCount, 0),
                  skippedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.skippedTestCaseCount, 0),
                  testsuiteSuccessPercent: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(success, 0) + '%',
                  testcaseSuccessPercent: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(successTestCase, 0) + '%',
                  //unitTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                  //sanityTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                  integrationTestsDone: $filter('number')(successTestCase, 0) > 95 ? true: false,
                  details: testResult
              });
          }
          deferred.resolve(response.lastUpdated);
          return deferred.promise;
        }

        function processSanityTestResponse(response) {
          var deferred = $q.defer();

          ctrl.testResult = testResult;

          ctrl.sanityTests = [];
          var index;
          var totalSize = _.isEmpty(response.result) ? 0 : response.result.length;
          if (totalSize != 0) {
             ctrl.showSanityTab = true;
          }
          for (index = 0; index < totalSize; ++index) {
              var testResult = _.isEmpty(response.result) ? {testCapabilities: []} : response.result[index];
              if((testResult.executionId === versionFilter) || (versionFilter === -1)) {

              var allZeros = {
                  failedTestSuiteCount: 0, successTestSuiteCount: 0, skippedTestSuiteCount: 0, totalTestSuiteCount: 0,
                  failedTestCaseCount: 0, successTestCaseCount: 0, skippedTestCaseCount: 0, totalTestCaseCount: 0
              };

              // Aggregate the counts of all Functional test suites
              var aggregate = _.reduce(_.filter(testResult.testCapabilities, {type: "Regression"}), function (result, capability) {
                  //New calculation: 3/10/16 - Topo Pal
                  result.failedTestSuiteCount += capability.failedTestSuiteCount;
                  result.successTestSuiteCount += capability.successTestSuiteCount;
                  result.skippedTestSuiteCount += capability.skippedTestSuiteCount;
                  result.totalTestSuiteCount += capability.totalTestSuiteCount;

                  var testSuites = capability.testSuites;
                  for (var index in testSuites) {
                      console.log("determine testcase results - totaltestcase");
                      console.log(testSuites[index].totalTestCaseCount);
                      result.failedTestCaseCount += testSuites[index].failedTestCaseCount;
                      result.successTestCaseCount += testSuites[index].successTestCaseCount;
                      result.skippedTestCaseCount += testSuites[index].skippedTestCaseCount;
                      result.totalTestCaseCount += testSuites[index].totalTestCaseCount;
                  }

                  return result;
              }, allZeros);


              /*var testcaseAggregate = _.reduce(_.filter(testResult.testCapabilities.testSuites, {type: "Functional"}), function (result, testsuites) {
                  //New calculation: 3/10/16 - Topo Pal
                  result.testsuitename += testsuites.description;
                  result.totalTestSuiteCount += testsuites.totalTestCaseCount;
                  result.failedCount += testsuites.failedTestCaseCount;
                  result.successTestSuiteCount += testsuites.successTestCaseCount;
                  result.skippedTestSuiteCount += testsuites.skippedTestCaseCount

                  return result;
              }, allZeros);*/
              var passed = aggregate.successTestSuiteCount;
              var allPassed = aggregate.successTestSuiteCount === (aggregate.totalTestSuiteCount - aggregate.skippedTestSuiteCount);
              var success = allPassed ? 100 : ((passed / (aggregate.totalTestSuiteCount-aggregate.skippedTestSuiteCount)) * 100);
              sanityTestsuiteCoveragePieChart(success);
              var passedTestCase = aggregate.successTestCaseCount;
              var allPassedTestCase = aggregate.successTestCaseCount === (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount);
              var successTestCase = allPassedTestCase ? 100 : ((passedTestCase / (aggregate.totalTestCaseCount - aggregate.skippedTestCaseCount)) * 100);
              sanityTestcaseCoveragePieChart(successTestCase);

              ctrl.executionId = _.isEmpty(response.result) ? "-" : response.result[index].executionId;
              ctrl.sanityTests.push({
                  name: $scope.widgetConfig.options.testJobNames[index],
                  totalTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.totalTestSuiteCount, 0),
                  successTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.successTestSuiteCount, 0),
                  failedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.failedTestSuiteCount, 0),
                  skippedTestSuiteCount: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(aggregate.skippedTestSuiteCount, 0),
                  totalTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.totalTestCaseCount, 0),
                  successTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.successTestCaseCount, 0),
                  failedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.failedTestCaseCount, 0),
                  skippedTestCaseCount: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(aggregate.skippedTestCaseCount, 0),
                  testsuiteSuccessPercent: aggregate.totalTestSuiteCount === 0 ? '-' : $filter('number')(success, 0) + '%',
                  testcaseSuccessPercent: aggregate.totalTestCaseCount === 0 ? '-' : $filter('number')(successTestCase, 0) + '%',
                  //unitTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                  sanityTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                  //functionalTestsDone: $filter('number')(success, 0) > 95 ? true: false,
                  details: testResult
              });
             }
          }
          deferred.resolve(response.lastUpdated);
          return deferred.promise;

        }

        /*function coveragePieChart(lineCoverage) {
            lineCoverage.value = lineCoverage.value || 0;

            ctrl.unitTestCoverageData = {
                series: [lineCoverage.value, (100 - lineCoverage.value)]
            };
        }*/
        function getBarChartDetails(type) {
          ctrl.barData = {
            labels: getExecutionIds(),
            series: getSeries()
          }
       }

       function getExecutionIds() {
         var execIds = []
         if(ctrl.showSanityTab){
            for(var i=0;i<ctrl.historicalSanityData.length;i++) {
                  execIds.push(ctrl.historicalSanityData[i]['executionId'])
            }
         }
         else if(ctrl.showFunctionalTab){
            for(var i=0;i<ctrl.historicalFunctionalData.length;i++) {
                  execIds.push(ctrl.historicalFunctionalData[i]['executionId'])
            }
         }
         else if(ctrl.showUnitTab){
            for(var i=0;i<ctrl.historicalUnitData.length;i++) {
                  execIds.push(ctrl.historicalUnitData[i]['executionId'])
            }
         }
         else if(ctrl.showIntegrationTab){
            for(var i=0;i<ctrl.historicalIntegrationData.length;i++) {
                  execIds.push(ctrl.historicalIntegrationData[i]['executionId'])
            }
         }
         console.log('execIds :')
         console.log(execIds)
         return(execIds)
       }

       function getSeries() {
         var individual_series = []
         var final_series = []
         var seriesObj = {}
         var i = 0;
         for (i=0;i<ctrl.historicalSanityData.length;i++){
                individual_series.push({'meta' : ctrl.historicalSanityData[i]['timeStamp'] + '\nPass Percentage = '+ ctrl.historicalSanityData[i]['passPercent'].toFixed(2).toString() ,'value' : ctrl.historicalSanityData[i]['passPercent']});
              }
              final_series.push(individual_series);
              individual_series = [];
         for (i=0;i<ctrl.historicalUnitData.length;i++){
                  individual_series.push({'meta' : ctrl.historicalUnitData[i]['timeStamp']+ '\nPass Percentage = '+ ctrl.historicalUnitData[i]['passPercent'].toFixed(2).toString(), 'value' : ctrl.historicalUnitData[i]['passPercent']});
                }
                final_series.push(individual_series);
                individual_series = [];
         for (i=0;i<ctrl.historicalFunctionalData.length;i++){
                    individual_series.push({'meta': ctrl.historicalFunctionalData[i]['timeStamp'] + '\nPass Percentage = '+ ctrl.historicalFunctionalData[i]['passPercent'].toFixed(2).toString(), 'value': ctrl.historicalFunctionalData[i]['passPercent']});
               }
               final_series.push(individual_series);
               individual_series = [];
        for (i=0;i<ctrl.historicalIntegrationData.length;i++){
                   individual_series.push({'meta': ctrl.historicalIntegrationData[i]['timeStamp'] + '\nPass Percentage = '+ ctrl.historicalIntegrationData[i]['passPercent'].toFixed(2).toString(), 'value': ctrl.historicalIntegrationData[i]['passPercent']});
              }
              final_series.push(individual_series);


         console.log('final_series');
         console.log(final_series);
         for(var j=0; j<4; j++){
            if(final_series[j].length !=5 ){
              for(var i=0; i<5; i++){
                 if(typeof  final_series[j][i] == 'undefined'){
                    final_series[j].push(0);
                 }
              }
            }
         }

         return(final_series)
       }

        function unitTestsuiteCoveragePieChart(passRate) {

            ctrl.unitTestsuiteCoverageData = {
                series: [passRate, (100 - passRate)]
            };
        }

        function unitTestcaseCoveragePieChart(passRate) {

            ctrl.unitTestcaseCoverageData = {
                series: [passRate, (100 - passRate)]
            };
        }

        function functionalTestsuiteCoveragePieChart(passRate) {

            ctrl.functionalTestsuiteCoverageData = {
                series: [passRate, (100 - passRate)]
            };
        }

        function functionalTestcaseCoveragePieChart(passRate) {

            ctrl.functionalTestcaseCoverageData = {
                series: [passRate, (100 - passRate)]
            };
        }

        function sanityTestsuiteCoveragePieChart(passRate) {

            ctrl.sanityTestsuiteCoverageData = {
                series: [passRate, (100 - passRate)]
            };
        }

        function sanityTestcaseCoveragePieChart(passRate) {

            ctrl.sanityTestcaseCoverageData = {
                series: [passRate, (100 - passRate)]
            };
        }

        function integrationTestsuiteCoveragePieChart(passRate) {

            ctrl.integrationTestsuiteCoverageData = {
                series: [passRate, (100 - passRate)]
            };
        }

        function integrationTestcaseCoveragePieChart(passRate) {

            ctrl.integrationTestcaseCoverageData = {
                series: [passRate, (100 - passRate)]
            };
        }


        function getMetric(metrics, metricName, title) {
            title = title || metricName;
            return angular.extend((_.find(metrics, { name: metricName }) || { name: title }), { name: title });
        }



        function showStatusIcon(item) {
            return item.status && item.status.toLowerCase() !== 'ok';
        }

        function showDetail(test) {
            $uibModal.open({
                controller: 'TestDetailsController',
                controllerAs: 'testDetails',
                templateUrl: 'components/widgets/testreportanalysis/testdetails.html',
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
