/**
 * Gets Component Version related data
 */
(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module + '.core')
        .factory('componentVersionsData', componentVersionsData);

    function componentVersionsData($http) {
        var testDetailRoute = 'test-data/component_version_detail.json';
        var caDetailRoute = '/api/quality/versions/';

        return {
            details: details
        };

        // search for test suite data
        function details(params) {
            return $http.get(HygieiaConfig.local ? testDetailRoute : caDetailRoute, { params: params })
                .then(function (response) {
                    return response.data;
                });
        }
    }
})();
