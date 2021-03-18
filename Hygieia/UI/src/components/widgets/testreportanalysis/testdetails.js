(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('TestDetailsController', TestDetailsController);

    TestDetailsController.$inject = ['$scope','$uibModalInstance', 'testResult', 'DashStatus'];
    function TestDetailsController($scope, $uibModalInstance, testResult, DashStatus) {
        /*jshint validthis:true */
        var ctrl = this;

        ctrl.statuses = DashStatus;
        ctrl.testResult = testResult;
        console.log('I am testResult')
        console.log(testResult)
        //ctrl.duration = secondsToTime(testResult.duration);
        $scope.duration = 0;
        $scope.duration = secondsToTime(testResult.duration);
        ctrl.close = close;

        function close() {
            $uibModalInstance.dismiss('close');
        }

        $scope.showCapabilityDetail = function (capability) {
          console.log('showCapabilityDetail: activeCapability-description:  ');
          console.log($scope.activeCapability);
          console.log('showCapabilityDetail: capability-description:  ');
          console.log(capability.description);
            if ($scope.activeCapability != capability.description) {
                $scope.activeCapability = capability.description;
            }
            else {
                $scope.activeCapability = null;
            }
        };

        $scope.showTestSuiteDetail = function (testSuite) {
            if ($scope.activeSuite != testSuite.description) {
                $scope.activeSuite = testSuite.description;
            }
            else {
                $scope.activeSuite = null;
            }
        };
        $scope.showTestCaseDetail = function (testCase) {
            if ($scope.activeCase != testCase.description) {
                $scope.activeCase = testCase.description;
            }
            else {
                $scope.activeCase = null;
            }
        };

        $scope.showStatusIcon =
        function showStatusIcon(item) {
            if (item.status.toLowerCase() == 'success') {
                return 'ok';
            } else if (item.status.toLowerCase() == 'skipped') {
                return 'warning';
            } else {
                return 'error';
            }
        };

        function msToTime(duration) {
            var milliseconds = parseInt((duration%1000)/100),
                seconds = parseInt((duration/1000)%60),
                minutes = parseInt((duration/(1000*60))%60),
                hours = parseInt((duration/(1000*60*60))%24);

            hours = (hours < 10) ? "0" + hours : hours;
            minutes = (minutes < 10) ? "0" + minutes : minutes;
            seconds = (seconds < 10) ? "0" + seconds : seconds;

            return hours + ":" + minutes + ":" + seconds;
        }

        function secondsToTime(totalSeconds){
	  console.log("secondsToTime : " + totalSeconds);
          var hours   = Math.floor(totalSeconds / 3600);
          var minutes = Math.floor((totalSeconds - (hours * 3600)) / 60);
          var seconds = totalSeconds - (hours * 3600) - (minutes * 60);

          // round seconds
          seconds = Math.round(seconds * 100) / 100

          var result = "";
	  if(hours > 0){
	    result += (hours < 10 ? "0" + hours : hours) + "hrs ";
	  }
          if(minutes > 0){
            result += (minutes < 10 ? "0" + minutes : minutes) + "min ";
          }
          result += (seconds  < 10 ? "0" + seconds : seconds);
          result += "sec";
          return result;
        }
    }

})();
