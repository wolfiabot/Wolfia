/*
 * Copyright (C) 2017 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

function requestDataPoint(path, series, updateInterval, range = 0) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4) {
            if (this.status === 200) {
                var shift = range > 0 && series.data.length > range; // shift if the series is longer than the range
                var point = JSON.parse(this.responseText);
                //only add it if it it not the same as the last point
                if (series.data.length < 1 || series.data[series.data.length - 1].x !== point[0]) {
                    series.addPoint(point, true, shift);
                }
            }
            // call it again after the update interval
            setTimeout(function () {
                requestDataPoint(path, series, updateInterval, range);
            }, updateInterval);
        }
    };
    xhttp.open("GET", path, true);
    xhttp.send();
}

//period = time in milliseconds for which this should show data, by default the last hour
function requestPeriodicData(path, series, updateInterval, period = 3600000) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4) {
            if (this.status === 200) {
                // set the data
                console.log(this.responseText);
                series.setData(JSON.parse(this.responseText), true);
            }
            // request updates
            setTimeout(function () {
                requestPeriodicData(path, series, updateInterval, period);
            }, updateInterval);
        }
    };
    var since = new Date().valueOf() - period;
    var params = "?since=" + since;
    xhttp.open("GET", path + params, true);
    xhttp.send();
}

// function getSeries(names, datas) {
//     var result = [];
//     //is this an array? then create several series
//     if (Array.isArray(names)) {
//         for (var i = 0; i < names.length; i++) {
//             var series = {};
//             series.name = names[i];
//             series.data = datas[i];
//             result.push(series);
//         }
//     } else {
//         result.push({name: names, data: datas});
//     }
//     return result;
// }

// function createLiveDateTimeChart(element, dataGetPath, title, initialData, updateInterval, max = 100, range = 0) {
//     return Highcharts.chart(element, {
//         chart: {
//             type: 'spline',
//             events: {
//                 load: function () {
//                     // requestData(dataGetPath, this.series[0], updateInterval, range);
//                     requestDataPoint(dataGetPath + "/latest", this.series[0], updateInterval, range);
//                 }
//             }
//         },
//         title: {
//             text: title
//         },
//         xAxis: {
//             type: 'datetime',
//             minRange: range * updateInterval
//         },
//         yAxis: {
//             min: 0,
//             max: max
//         },
//         plotOptions: {
//             spline: {
//                 marker: {
//                     enabled: false
//                 }
//             }
//         },
//         // series: getSeries(seriesNames, initialDatas)
//         series: [{
//             name: title,
//             data: initialData
//         }]
//     });
// }

function getLiveChartOptions(dataGetPath, title, initialData = [], updateInterval = 800, range = 20, max = -1) {
    var chartOptions = getDefaultChartOptions(dataGetPath, title, initialData, updateInterval, range);
    chartOptions.xAxis.minRange = range * updateInterval;
    if (max > -1) {
        chartOptions.yAxis.max = max;
    }
    return chartOptions;
}


function getDefaultChartOptions(dataGetPath, title, initialData = [], updateInterval = 60000, range = 0) {
    return {
        chart: {
            type: 'areaspline',
            events: {
                load: function () {
                    requestDataPoint(dataGetPath + "/latest", this.series[0], updateInterval, range);
                }
            }
        },
        title: {
            text: title
        },
        xAxis: {
            type: 'datetime'
        },
        yAxis: {
            min: 0
        },
        plotOptions: {
            areaspline: {
                marker: {
                    enabled: false
                }
            },
            spline: {
                marker: {
                    enabled: false
                }
            }
        },
        // series: getSeries(seriesNames, initialDatas)
        series: [{
            name: title,
            data: initialData
        }]
    }
}

function createDateTimeChart(element, chartOptions) {
    return Highcharts.chart(element, chartOptions);
}