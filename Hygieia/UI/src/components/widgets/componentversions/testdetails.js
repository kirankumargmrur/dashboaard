(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('VersionDetailsController', VersionDetailsController);

    VersionDetailsController.$inject = ['$scope', '$uibModalInstance', 'testResult', 'DashStatus'];
    function VersionDetailsController($scope, $uibModalInstance, testResult, DashStatus) {
        /*jshint validthis:true */
        var ctrl = this;

        ctrl.statuses = DashStatus;
        ctrl.component = testResult.name;
        ctrl.componentDetails = testResult.components;
        console.log('Name : ' + ctrl.component);
        console.log('Length: ' + ctrl.componentDetails.length);
        ctrl.close = close;

        function close() {
            $uibModalInstance.dismiss('close');
        }
    }

})();
