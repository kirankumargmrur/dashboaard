(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module + '.core')
        .factory('versionFilter', versionFilter);

    function versionFilter() {
        var versionFilterData = -1;
        return {
            setVersionFilterData: setVersionFilterData,
            getVersionFilterData: getVersionFilterData
        };

        function setVersionFilterData(id) {
		    versionFilterData = -1;
		    if( typeof id != 'undefined' && id != null ){
				versionFilterData = id;
			}
        }
		
        function getVersionFilterData() {
            return versionFilterData;
        }
    }
})();
