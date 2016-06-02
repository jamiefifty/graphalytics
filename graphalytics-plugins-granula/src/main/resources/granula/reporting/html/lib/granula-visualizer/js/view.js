

function setJob() {

    job.usage = jobMetrics.map(function (rawdata) { return metricData(rawdata, 1); });

    $('#job').append(jobPanel(job));
    // $('a[href="#environment-tab"]').tab('show');
    // $('a[href="#operation-tab"]').tab('show');
}



function jobPanel(job) {

    var jobPanel = $('<div class="card text-xs-center job-card" ></div>');
    var panelWrapper = $('<div class="row"><div class="col-md-12 col-centered"></div></div>');
    panelWrapper.find('div').append(jobPanel);

    jobPanel.append();


    jobPanel.append(jobHeader(job));

    var tabItems = [{id: "overview-tab", name: "Overview"},
        {id: "operation-tab", name: "Operation"},
        {id: "environment-tab", name: "Environment"}];



    jobPanel.append(tabDiv(tabItems));

    jobOverview(job, jobPanel.find('#'+tabItems[0].id));
    operationView(job, jobPanel.find('#'+tabItems[1].id));
    envView(job, jobPanel.find('#'+tabItems[2].id));
    
    jobPanel.append(jobFooter(job));
    return panelWrapper;
}



function jobHeader(job) {
    var name = job.name;
    var header =$('<div class="card-header dark job-border">' + name + '</div>');
    return header;
}


function jobFooter() {
    var name = job.name;
    var footer =$('<div class="card-header dark">' + name + '</div>');
    return footer;
}


