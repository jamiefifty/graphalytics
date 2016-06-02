

function convertData(jobdata) {
    var metric = new Object();
    metric.nodeId = jobdata.key.split("/")[0];
    metric.processId = jobdata.key.split("/")[1];
    metric.type = jobdata.key.split("/")[2].split("_")[0];
    metric.interval = jobdata.key.split("/")[2].split("_")[1].split("ms")[0];
    metric.data = jobdata;
    return metric;
}


function reduceData(jobdata) {
    var newdata = {};
    newdata.key = jobdata.key;
    newdata.values = jobdata.values.filter(function (dp) {
        return dp[0] >= job.startTime && dp[0] <= job.endTime;
    });
    return newdata;
};

function demagnitude(jobdata, magnitude) {
    var newdata = {};
    newdata.key = jobdata.key;
    newdata.values = jobdata.values.map(function (dp) {
        return [dp[0], dp[1] / magnitude];
    });
    return newdata;
};

//-------------------new-----------------------------
function metricData(jobdata, magnitude) {
    var metricData = {};
    metricData.entity = jobdata.key.split("/")[0] + "/" + jobdata.key.split("/")[1];
    metricData.type = jobdata.key.split("/")[2].split("_")[0];
    metricData.interval = jobdata.key.split("/")[2].split("_")[1].split("ms")[0];
    metricData.data =  kvList2Map(jobdata.values);
    metricData.magnitude = magnitude;
    return metricData;
}


function filterMetricDataList(dataList, type, interval) {
    return dataList.
        filter(function (metricData) {
            return metricData.type.indexOf(type) > -1
                && metricData.interval == interval
    });
}

function diffMetricValues(metricValues, startTime, endTime) {
    var count = 0;
    var minTimestamp = Number.MAX_VALUE;
    var maxTimestamp = Number.MIN_VALUE;

    for (var timestamp in metricValues) {
        minTimestamp = (timestamp < minTimestamp && timestamp > startTime) ? timestamp : minTimestamp;
        maxTimestamp = (timestamp > maxTimestamp && timestamp < endTime) ? timestamp : maxTimestamp;
        count++;
    }

    return (count > 0) ? parseFloat(metricValues[maxTimestamp]) - parseFloat(metricValues[minTimestamp]) : null;

}

function extractValues(oValues, startTime, endTime) {

    var nValues = {};

    for (var timestamp in oValues) {
        if((timestamp < endTime && timestamp > startTime)) {
            nValues[timestamp] = oValues[timestamp];
        }
    }
    return nValues;
}

function avgMethod(values) {
    var sum = 0;
    var count = 0;

    for (var timestamp in values) {
            sum += parseFloat(values[timestamp]);
            count++;
    }


    return (count > 0) ? (sum / count) : null;
}

function maxMethod(values) {
    var count = 0;
    var value = Number.MIN_VALUE;

    for (var timestamp in values) {
        value = (parseFloat(values[timestamp]) > parseFloat(value)) ? values[timestamp] : value;
        count++;
    }
    return (count > 0) ? value : null;
}

function minMethod(values) {
    var count = 0;
    var value = Number.MAX_VALUE;

    for (var timestamp in values) {
        value = (parseFloat(values[timestamp]) < parseFloat(value)) ? values[timestamp] : value;
        count++;
    }
    return (count > 0) ? value : null;
}



function aggregatedMetricValues(metricDataList) {
    var aggValues = {};
    var aggCount = {};

    metricDataList.forEach(function (metricData) {
        for (var timestamp in metricData.data) {
            if(aggValues[timestamp]) {
                aggValues[timestamp] += parseFloat(metricData.data[timestamp]);
                aggCount[timestamp] += 1;
            } else {
                aggValues[timestamp] = parseFloat(metricData.data[timestamp]);
                aggCount[timestamp] = 1;
            }
        }
    });

    var realAggValues = {}
    for (var timestamp in aggCount) {
        if (aggCount[timestamp] == metricDataList.length) {
            realAggValues[timestamp] = aggValues[timestamp];
        }
    }

    return realAggValues;
}

function reduceDatapoints(dataList, startTime, endTime) {
    return dataList.map(function (completeData) {
        var metricData = {};
        metricData.entity = completeData.entity;
        metricData.type = completeData.type;
        metricData.interval = completeData.interval;
        metricData.data = {};
        for (var timestamp in completeData.data) {
            if(timestamp > startTime && timestamp < endTime) {
                metricData.data[timestamp] = completeData.data[timestamp];
            }
        }
        return metricData;
    });
}

function chartData(dataList, startTime, endTime, interval, magnitude, cumulative) {
    return dataList.map(function (metricData) {
        var values = {};

        for (var timestamp in metricData.data) {
            if(timestamp > startTime && timestamp < endTime) {
                values[timestamp] = metricData.data[timestamp];
            }
        }
        values = (cumulative) ? decumulative2(values) : values;
        values = fillGap2(values, startTime, endTime, interval);

        values = map2KVList(values).map(function (value) {
            return [value[0] - startTime, value[1] * metricData.magnitude / magnitude];
        });

        return {key:metricData.entity, values:values};
    });
}



function fillGap2(gValues, startTime, endTime, interval) {
    var fValues = {};

    var start = parseInt(startTime) + (interval - startTime % interval);
    var end = parseInt(endTime) - endTime % interval;


    fValues[startTime] = (gValues[startTime]) ? gValues[startTime] : 0.0; // improve later by interpolation

    for (var i = start; i <= end; i = i + interval) {
        if(!gValues[i]) {
            fValues[i] = 0;
        } else {
            fValues[i] = gValues[i];
        }
    }

    fValues[endTime] = (gValues[endTime]) ? gValues[endTime] : 0.0; // improve later by interpolation

    return fValues;
};

function decumulative2(cValues) {
    var dValues = {};

    var diff;
    var currValue;
    var prevValue;
    var prevTimestamp;
    for (var timestamp in cValues) {
        currValue = cValues[timestamp];

        diff = (prevValue) ? (currValue - prevValue) / (timestamp - prevTimestamp) * 1000.0: 0;
        dValues[timestamp] = diff;
        prevValue = currValue;
        prevTimestamp = timestamp;
    }
    return dValues;
}


//-------------------new-----------------------------



function fillGap(jobdata) {
    var newdata = {};
    newdata.key = jobdata.key;
    newdata.values = [];

    var valuesMap = {};
    for (var i = 0; i < jobdata.values.length; i++) {
        // console.log(jobMetrics.values[i][0])
        valuesMap[jobdata.values[i][0]] = jobdata.values[i];
    }

    var start = parseInt(job.startTime) + (1000 - job.startTime % 1000);
    var end = parseInt(job.endTime) - job.endTime % 1000;

    newdata.values.push([job.startTime, 0]);

    for (var i = start; i <= end; i = i + 1000) {
        if(valuesMap[i] == null) {
            newdata.values.push([i, 0]);
        } else {
            newdata.values.push([i, valuesMap[i][1]]);
        }
    }
    newdata.values.push([job.endTime, 0]);

    return newdata;
};


function decumulative(data) {
    var newdata = {};

    newdata.key = data.key;

    var newValues =  [];
    var oldValues = data.values;
    newValues[0] = [oldValues[0][0], 0];
    for (var i = 1; i < oldValues.length; i++) {
        var dff = (oldValues[i][1] - oldValues[i - 1][1])
            / (oldValues[i][0] - oldValues[i - 1][0]) * 1000;
        newValues[i] = [oldValues[i][0], dff];
    }

    newdata.values = newValues;
    return newdata;
}


function selectData(metrics, type, interval, cumulative, magnitude, gap) {
    var netMetrics = metrics
        .filter(function (metric) {
            return metric.type.indexOf(type) > -1 && metric.interval == interval
        }).map(function (metric) {
            var selectedData = reduceData(metric.data);
            selectedData.key = selectedData.key.split("_")[0];
            selectedData = cumulative ? decumulative(selectedData) : reduceData(selectedData);
            selectedData = demagnitude(selectedData, magnitude);
            selectedData = gap ? fillGap(selectedData) : selectedData;
            return selectedData;
        });

    return netMetrics;
}





function list2Map(list, id) {
    var map = {};
    list.forEach(function (item) {
        map[item[id]] = item;
    });
    return map;
}

function kvList2Map(list, id) {
    var map = {};
    list.forEach(function (item) {
        map[item[0]] = item[1];
    });
    return map;
}


function map2List(map) {
    var list = [];
    for (var item in map) {
        list.push(map[item]);
    }
    return list;
}

function map2KVList(map) {
    var list = [];
    for (var item in map) {
        list.push([item, map[item]]);
    }
    return list;
}