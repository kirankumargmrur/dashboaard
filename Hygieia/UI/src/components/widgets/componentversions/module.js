(function () {
    'use strict';

    var widget_state,
        config = {
            view: {
                defaults: {
                    title: 'Quality' // widget title
                },
                controller: 'VersionsAnalysisViewController',
                controllerAs: 'ComponentVersionsWidget',
                templateUrl: 'components/widgets/componentversions/view.html'
            },
            config: {
                controller: 'VersionDataAnalysisConfigController',
                controllerAs: 'ComponentVersionsWidget',
                templateUrl: 'components/widgets/componentversions/config.html'
            },
            getState: getState,
            collectors: ['codequality']
        };

    angular
        .module(HygieiaConfig.module)
        .config(register);

    register.$inject = ['widgetManagerProvider', 'WidgetState'];
    function register(widgetManagerProvider, WidgetState) {
        widget_state = WidgetState;
        widgetManagerProvider.register('componentversions', config);
    }

    function getState(widgetConfig) {
        // make sure config values are set
        return HygieiaConfig.local || (widgetConfig.id) ? widget_state.READY : widget_state.CONFIGURE;
    }
})();
