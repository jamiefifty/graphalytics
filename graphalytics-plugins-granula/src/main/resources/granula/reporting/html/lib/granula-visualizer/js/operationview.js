var operation = {};
var isOperation = true;
var operations = {};
var infos = {};
var actors = {};
var missions = {};
var operationTab = {};

operResTabAttrMap = {};

function operationView(job, opTab) {

    operations = job.tree.operations;
    infos = job.tree.infos;
    actors = job.tree.actors;
    missions = job.tree.missions;
    operationTab = opTab;

    var operation = operations[job.tree.rootIds[0]];
    drawOperChart(operation);

}

function operDesrCard(operation) {
    var iMap = infoMap(operation.infoIds);

    var card = divCard();
    var table = divTable();

    var summary = iMap["Summary"].summary;

    table.append(caption(getOperationName(operation)));
    // var infoStrings = summary.match(/\[\{[\d]+\}\]/g);
    // for(var i=0; i< infoStrings.length; i++) {
    //     var infoString = infoStrings[i];
    //     summary = summary.replace(infoString, "");
    // }

    table.append($('<p class="text-left">'+summary+'</p>'));
    card.append(table);
    return card;
}


function operDurationCard(operation) {
    var iMap = infoMap(operation.infoIds);

    var card = divCard();
    var table = divTable();

    var startTime = iMap["StartTime"].value;
    var endTime = iMap["EndTime"].value;
    table.append(caption("Duration"));


    table.append(tableRow("StartTime", timeConverter(startTime) + ' (' + startTime + ')'));
    table.append(tableRow("EndTime", timeConverter(endTime) + ' (' + endTime + ')'));
    table.append(tableRow("Duration", parseInt(endTime) - parseInt(startTime) + " ms"));

    card.append(table);
    return card;
}

function drawOperChart(operation) {

    operationTab.empty();
    var cardRow1 = divCardGroup();
    var cardRow2 = divCardGroup();
    var cardRow3 = divCardGroup();
    var cardRow4 = divCardGroup();
    operationTab.append(cardRow1);
    operationTab.append(cardRow2);
    operationTab.append(cardRow3);
    operationTab.append(cardRow4);


    cardRow1.append(navigationLink(operation));
    cardRow2.append(operDesrCard(operation), operDurationCard(operation));

    var operationChart = operChart(operation);
    cardRow3.append(operationChart);

    var resTabAttrs = operResTabAttrs(operation);
    operResTabAttrMap = list2Map(resTabAttrs, "id");
    operationTab.append(operResTabs(operation, resTabAttrs));


}

function operResTabAttrs(operation) {

    var dataList = job.usage;

    var operInfoMap = infoMap(operation.infoIds);

    var startTime = operInfoMap["StartTime"].value;
    var endTime = operInfoMap["EndTime"].value;

    var cpuChartAttr = {xLabel:"Execution Time (s)", yLabel:"Cpu Time (s)"};
    var cpuChartData = chartData(filterMetricDataList(dataList, "cpu", 1000), startTime, endTime, 1000, 1, true);
    var cpuTabAttr = {id: "oper-cpu-metric", name: "CPU", chartAttr: cpuChartAttr, chartData: cpuChartData};

    var memChartAttr = {xLabel:"Execution Time (s)", yLabel:"Memory Usage (GB)"};
    var memChartData = chartData(filterMetricDataList(dataList, "mem", 1000), startTime, endTime, 1000, 1000000, false);
    var memTabAttr = {id: "oper-mem-metric", name: "Memory", chartAttr: memChartAttr, chartData: memChartData};


    var netIbSndChartAttr = {xLabel:"Execution Time (s)", yLabel:"Network Send Bandwidth(GB, Infiniband)"};
    var netIbSndChartData = chartData(filterMetricDataList(dataList, "net-ib-snd", 1000), startTime, endTime, 1000, 1000000000, true);
    var netIbSndTabAttr = {id: "oper-netIbSndMetrics-metric", name: "Network-Snd", chartAttr: netIbSndChartAttr, chartData: netIbSndChartData};

    var netIbRecChartAttr = {xLabel:"Execution Time (s)", yLabel:"Network Receive Bandwidth (GB, Infiniband)"};
    var netIbRecChartData = chartData(filterMetricDataList(dataList, "net-ib-rec", 1000), startTime, endTime, 1000, 1000000000, true);
    var netIbRecTabAttr = {id: "oper-netIbRecMetrics-metric", name: "Network-Rec", chartAttr: netIbRecChartAttr, chartData: netIbRecChartData};

    return [cpuTabAttr, memTabAttr, netIbSndTabAttr, netIbRecTabAttr];
}

function operResTabs(operation, tabAttrs) {

    var operInfoMap = infoMap(operation.infoIds);

    var startTime = operInfoMap["StartTime"].value;
    var endTime = operInfoMap["EndTime"].value;


    var tabDiv = $('<div role="tabpanel">');
    var tabList = $('<ul class="nav nav-tabs" role="tablist" />');
    var tabContent = $('<div class="tab-content" />');

    tabDiv.append(tabList);
    tabDiv.append(tabContent);


    tabList.append('<li class="nav-item">' +
        '<a class="nav-link" data-toggle="tab" href="#oper-summary-tab" role="tab">All</a>' +
        '</li>');

    var content = $('<div class="tab-pane" id="oper-summary-tab" role="tabpanel"></div>');
    tabContent.append(content);
    content.append(envSummaryCard(startTime, endTime));


    tabAttrs.forEach(function(tabAttr, i) {
        tabList.append('<li class="nav-item">' +
            '<a class="nav-link" data-toggle="tab" metric-id="'+tabAttr.id+'" href="#env-'+tabAttr.id+'-tab" role="tab">'+tabAttr.name+'</a>' +
            '</li>');

        var content = $('<div class="tab-pane " id="env-'+tabAttr.id+'-tab" role="tabpanel"></div>');
        content.append(areaChartCard('env-'+tabAttr.id+'-chart'));
        tabContent.append(content);
    });

    tabDiv.find('[data-toggle = "tab"]').on('shown.bs.tab', function (e) {
        try{
            var metricId = $(e.target).attr('metric-id');

            if(metricId != null) {
                var thismetric = operResTabAttrMap[metricId];
                areaChart('#env-'+thismetric.id+"-chart", thismetric.chartAttr, thismetric.chartData);
            }
        }catch(err){
            logging("" + err);
        }
    });

    return tabDiv;
}


function navigationLink(operation) {

    var currOperation = operation;

    var ancestorLink = $('<p class="link"></p>');

    // ancestorLink.prepend('&nbsp;&nbsp;>&nbsp;&nbsp;' + currentOperation.getTitle());
    // currentOperation = currentOperation.getSuperoperation();

    while(currOperation != null) {
        var link = $('<a uuid="' + currOperation.uuid + '">' + getOperationName(currOperation)+'</a>');

        link.on('click', function (e) {
            e.preventDefault();
            logging($(this).attr('uuid'));
            drawOperChart(operations[$(this).attr('uuid')]);
        });
        ancestorLink.prepend(link);
        ancestorLink.prepend('&nbsp;&nbsp;>&nbsp;&nbsp;');
        currOperation = operations[currOperation.parentId];

    }
    ancestorLink.prepend('Job');
    return ancestorLink;
}

