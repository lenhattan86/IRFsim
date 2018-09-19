addpath('matlab_func');
common_settings;
% is_printed = 1;
fig_path = ['figs/'];
%%
barWidth = 0.5;
queue_num = 25;
cluster_size=100;
figureSize = figSizeOneCol .* [1 1 2/3 2/3];
plots  = [false, true, true, false, true, false];

colorUsers = {colorUser1; colorUser2; colorUser3};
methods = {strDRF,  strES,  'DRFExt', strAlloX, 'SJF'};
files = {'DRF', 'ES', 'DRFExt', 'AlloX','SJF'};
DRFId = 1; ESId = 2; DRFExtId = 3; AlloXId = 4; SJFId = 5;

% plots  = [true, false, false, false, false, false];
% methods = {strES,  strAlloX};

methodColors = {colorES; colorDRF; colorProposed};
extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size)];
% EXTRA='_s';
EXTRA='_w_prof';

scaleTime = 1; % minutes
%% load data

for i=1:length(methods)
    outputFile = [ 'output/' files{i} '-output' extraStr  '.csv'];
    [JobIds{i}, startTimes{i}, endTimes{i}, durations{i}, queueNames{i}] = import_compl_time(outputFile);
    fullJobsIndices{i} = find(JobIds{i}>=0);
    durations{i} = durations{i}(fullJobsIndices{i});
    JobIds{i} = JobIds{i}(fullJobsIndices{i});    
end

%%
%%
if plots(1)  
   complTimes = [];
  figIdx=figIdx +1;     
  durations = {[403 103 254 155 306 353];[103 152 253 204 303 305]};
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');   
%    hBar = bar(avgCmplt', barWidth);
%    set(hBar,{'FaceColor'}, colorUsers);   
   
    hold on
    for i = 1:length(methods)
        avgCmplt = mean(durations{i});
        h=bar(i, avgCmplt, 0.2);
%         set(h,'FaceColor', methodColors{i});
    end
    hold off

    xLabel='methods';
    yLabel='seconds';
    legendStr=methods;
     xlim([0.4 length(methods)+0.6]);
%     ylim([0 max(max(avgCmplt))*1.1]);
    xLabels={strUser1, strUser2, strUser3};
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    fileNames{figIdx} = 'avgCmplt_exp';      
end

%%
if plots(2)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');   
%    hBar = bar(avgCmplt', barWidth);
%    set(hBar,{'FaceColor'}, colorUsers);   
   
    hold on
    for i = 1:length(methods)
        avgCmplt = mean(durations{i}) * scaleTime;
        h=bar(i, avgCmplt, barWidth);
%         set(h,'FaceColor', methodColors{i});
    end
    hold off

    xLabel=strMethods;
    yLabel=strAvgCmplt;
    legendStr=methods;
     xlim([0.6 length(methods)+0.4]);
%     ylim([0 max(max(avgCmplt))*1.1]);
    xLabels={strUser1, strUser2, strUser3};
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    fileNames{figIdx} = 'avgCmplt';
end

%%
if false    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');  
   
    hold on
    normMakespan = -1;
    for i = 1:length(methods)
        makespan =  max(endTimes{i});
        if (normMakespan < 0)
            normMakespan = makespan;
        end
        h=bar(i, makespan/normMakespan, barWidth);
        set(h,'FaceColor', methodColors{i});
    end
    hold off

    xLabel=strMethods;
    yLabel= ['norm. ' strMakeSpan];
    legendStr=methods;
    xlim([0.6 length(methods)+0.4]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods ,'FontSize',fontAxis);
    fileNames{figIdx} = 'makespan';
end

if plots(3)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');  
   
   maxEndTime = max(endTimes{SJFId}); % AlloX
    hold on
    normMakespan = -1;
    for i = 1:length(methods)
        numJobs = sum(endTimes{i} <= maxEndTime);    
        h=bar(i, numJobs , barWidth);
%         set(h,'FaceColor', methodColors{i});
    end
    hold off

    xLabel=strMethods;
    yLabel= ['job completed'];
    legendStr=methods;
    xlim([0.6 length(methods)+0.4]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods ,'FontSize',fontAxis);
    fileNames{figIdx} = 'job_completed';
end

%%
if plots(4) 
   STOP_TIME = 300;
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');  
   
    hold on
    for i = 1:length(methods)
        jobCompleted =  sum(endTimes{i}(:)<STOP_TIME);
        h=bar(i, jobCompleted, barWidth);
        set(h,'FaceColor', methodColors{i});
    end
    hold off

    xLabel=strMethods;
    yLabel='job completed';
    legendStr=methods;
    xlim([0.6 length(methods)+0.4]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods ,'FontSize',fontAxis);
    fileNames{figIdx} = 'job_completed';
end

%%
if plots(5)    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');     
    hold on

    [~, DRFSortIds] = sort(JobIds{DRFId});
    [~, ESSortIds] = sort(JobIds{ESId});
    [~, AlloXSortIds] = sort(JobIds{AlloXId});
    [~, DRFExtSortIds] = sort(JobIds{DRFExtId});
    
    

    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{ESId}(ESSortIds))./durations{ESId}(ESSortIds)*100;
%     slowDownVals = max (slowDownVals, 0);
    [f,x]=ecdf(slowDownVals);
    plot(x,f, 'LineWidth',LineWidth);
    hold on;
    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{DRFId}(DRFSortIds))./durations{DRFId}(DRFSortIds)*100;
%     slowDownVals = max (slowDownVals, 0);
    [f,x]=ecdf(slowDownVals);
    plot(x,f, 'LineWidth',LineWidth);
    hold on;
    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{DRFExtId}(DRFExtSortIds))./durations{DRFExtId}(DRFExtSortIds)*100;
%     slowDownVals = max (slowDownVals, 0);
    [f,x]=ecdf(slowDownVals);
    plot(x,f, 'LineWidth',LineWidth);

%     xLabel='slowdown (s)';
    xLabel='slowdown (%)';
    yLabel=strCdf;    
    xlim([-100 200]);
    
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    legendStr = {'vs. ES+RP', 'vs. DRF', 'vs. DRFExt'};
    legend(legendStr,'Location','best','FontSize',fontLegend,'Orientation','vertical');
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    fileNames{figIdx} = 'slowdown';
end
%%
if plots(6)
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');     
    hold on;

    [~, DRFSortIds] = sort(JobIds{DRFId});
    [~, ESSortIds] = sort(JobIds{ESId});
    [~, AlloXSortIds] = sort(JobIds{AlloXId});
    
    DRFQueues = queueNames{DRFId}(DRFSortIds);
    ESQueues = queueNames{ESId}(ESSortIds);
    AlloXQueues = queueNames{AlloXId}(AlloXSortIds);
    
    ESTotalCompltTime = [];
    DRFTotalCompltTime = [];
    AlloXTotalCompltTime = [];
    
    ESDurations = durations{ESId}(ESSortIds);
    DRFDurations = durations{DRFId}(DRFSortIds);
    AlloXDurations = durations{AlloXId}(AlloXSortIds);
    
    
    queueSet = {};
    for i=1:length(ESQueues)
        queueName = ESQueues{i};
        idx = 0;        
        if ~any(strcmp(queueSet,queueName))
            queueSet{length(queueSet)+1} = queueName;
            idx = length(queueSet);
            ESTotalCompltTime = [ESTotalCompltTime  ESDurations(i)];
            DRFTotalCompltTime = [DRFTotalCompltTime  DRFDurations(i)];
            AlloXTotalCompltTime = [AlloXTotalCompltTime  AlloXDurations(i)];
        else            
            idx = strcmp(queueSet,queueName);
            ESTotalCompltTime(idx) = ESTotalCompltTime(idx) + ESDurations(i);
            DRFTotalCompltTime(idx) = DRFTotalCompltTime(idx) + DRFDurations(i);
            AlloXTotalCompltTime(idx) = AlloXTotalCompltTime(idx) + AlloXDurations(i);
        end        
    end
    
    slowdown = (AlloXTotalCompltTime - ESTotalCompltTime)./ESTotalCompltTime * 100;    
    slowdown2 = (AlloXTotalCompltTime - DRFTotalCompltTime)./DRFTotalCompltTime * 100;    
    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{ESId}(ESSortIds))./durations{ESId}(ESSortIds)*100;
    
    [f,x]=ecdf(slowdown);
    plot(x,f, 'LineWidth',LineWidth);
    hold on;    
    [f,x]=ecdf(slowdown2);
    plot(x,f, 'LineWidth',LineWidth);

    xLabel='degradation (%)';
    yLabel=strCdf;    
    xlim([-100 100]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    legendStr = {'vs. ES+RP', 'vs. DRF'};
    legend(legendStr,'Location','best','FontSize',fontLegend,'Orientation','vertical');
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    fileNames{figIdx} = 'degradation';
end

return;
%%

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName EXTRA '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end