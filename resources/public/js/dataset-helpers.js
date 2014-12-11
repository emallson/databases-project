(function(DataSet, undefined) {

    DataSet.extract = function(key, data) {
        return data.map(function(v) { return v[key]; });
    };

    // stolen from highcharts. Maybe switch to that? dunno...
    DataSet.colors = ['#7cb5ec', '#434348', '#90ed7d', '#f7a35c', '#8085e9',
                      '#f15c80', '#e4d354', '#8085e8', '#8d4653', '#91e8e1'];

    // takes a dataset of the form [{x: , y: , etc}, {x: , y: , etc] and returns one of the form
    // [{label: x, data: }, {label: y, data: }, etc}]
    DataSet.invert = function(data) {
        if(data.length === 0) {
            return [];
        }

        var keys = Object.keys(data[0]);
        var colors = DataSet.colors.slice(0);

        return keys.reduce(function(reduction, k) {
            var color = reduction.colors.shift();
            reduction.data.push({
                label: k,
                data: DataSet.extract(k, data),
                strokeColor: color,
                pointColor: color,
                pointStrokeColor: "#fff",
                pointHighlightFill: "#fff",
                pointHighlightedStroke: "rgba(220,220,220,1)"
            });
            return reduction;
        }, {data: [], colors: colors}).data;
    };

})(window.DataSet = window.DataSet || {});
