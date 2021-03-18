(function () {
    'use strict';

    var widget_state,
        config = {
            view: {
                defaults: {
                    title: 'Quality' // widget title
                },
                controller: 'TestReportAnalysisViewController',
                controllerAs: 'TestReportWidget',
                templateUrl: 'components/widgets/testreportanalysis/view.html'
            },
            config: {
                controller: 'TestReportAnalysisConfigController',
                controllerAs: 'TestReportWidget',
                templateUrl: 'components/widgets/testreportanalysis/config.html'
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
        widgetManagerProvider.register('testreport', config);
    }

    function getState(widgetConfig) {
        // make sure config values are set
        return HygieiaConfig.local || (widgetConfig.id) ? widget_state.READY : widget_state.CONFIGURE;
    }
})();
