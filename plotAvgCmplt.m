addpath('matlab_func');
common_settings;
% is_printed = 1;
% fig_path = ['figs/'];
%%
simulation = true;
if simulation 
%     queue_num = 20;
%     cluster_size= 10;
    queue_num = 10;
    cluster_size= 20;
    plots  = [true, false, false, false, false];
else 
    fprintf('Verify the experiment results');
    queue_num = 4;
    cluster_size= 4;
    % ClusterAvg =  [5.3396    5.0837    4.8939    4.8312    4.4267]*1000; % eurosys 4.2
    SimAvg = [3.810    3.6888    3.3361     3.3257    2.6007] * 1000; % from simulation    
    SimWaitingAvg = [0.7315    0 0 0 0] * 1000; % from simulation
    SimRunningAvg = [3.0726    0 0 0 0] * 1000; % from simulation
    ClusterAvg = [3.8899    3.3052    3.3247    3.4347    2.5079] * 1000;% eurosys 4.3
%     ClusterWatingTime = [3.1584    2.0612+0.12    1.6876+0.12    1.6690+0.12    1.3565+0.12] * 1000;
%     ClusterRunningTime = [0.7315    1.1240    1.5171    1.6457    1.0314] * 1000;
    plots  = [false, false, false, false, false];
end

figureSize = figSizeThreeFourth;

colorUsers = {colorUser1; colorUser2; colorUser3};
% methods = {strDRF,  strES,  'DRFE', strAlloX, 'SJF', 'FS','SRPT'};
% files = {'DRF', 'ES', 'DRFExt', 'AlloX','SJF', 'FS','SRPT'};
% DRFId = 1; ESId = 2; DRFExtId = 3; AlloXId = 4; SJFId = 5; FSId = 6;

methods = {strDRFFIFO, strDRFSJF, strES, strDRFExt, strAlloX, strSRPT};
files = {'DRFFIFO', 'DRF', 'ES', 'DRFExt', 'AlloX', 'SRPT'};
% methods = {'DRFF', 'DRFS', strES, 'DRFE', 'AlloX'};
% files = {'DRFFIFO', 'DRF', 'ES', 'DRFExt', 'FS'};
DRFFIFOId = 1; DRFId = 2; ESId = 3; DRFExt = 4; SJFId = 5; FSId = 6;

alphas = [0.1 0.3];

methodColors = {colorES; colorDRF; colorProposed};
% extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size)];
extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_debug'];
% extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_0.05'];
EXTRA='';
% EXTRA='_w_prof';
JobIds={};
startTimes={};
endTimes = {};
durations = {};
queueNames = {};
startRunningTimes = {};
runningTimes = {};
scaleTime = 1; % minutes
%% load data

for i=1:length(methods)
    outputFile = [ 'output/' files{i} '-output' extraStr  '.csv'];
    [JobIds{i}, startTimes{i}, endTimes{i}, durations{i}, queueNames{i}, startRunningTimes{i}, runningTimes{i}] ...
        = import_compl_time(outputFile);   
       
%     waitingTimes{i} = startRunningTimes{i} - startTimes{i};
    waitingTimes{i} = durations{i} - runningTimes{i};
%     runningTimes{i} = durations{i} - waitingTimes{i};
    
    fullJobsIndices{i} = find(JobIds{i}>=0);
    durations{i} = durations{i}(fullJobsIndices{i}); 
    waitingTimes{i} = waitingTimes{i}(fullJobsIndices{i});
    runningTimes{i} = runningTimes{i}(fullJobsIndices{i});
    JobIds{i} = JobIds{i}(fullJobsIndices{i});
end

%%

%%
if plots(1)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');   
%    hBar = bar(avgCmplt', barWidth);
%    set(hBar,{'FaceColor'}, colorUsers);   
   
    hold on
    for i = 1:length(methods)
%         avgCmplt(i) = mean(durations{i}) * scaleTime;
        avgCmplt(i) = mean(runningTimes{i}) * scaleTime;
%         h=bar(i, avgCmplt(i), barWidth);
%         avgCmplt(i,:) = mean([waitingTimes{i} runningTimes{i}]) * scaleTime;        
%         set(h,'FaceColor', methodColors{i});
    end
    h=bar(1:length(methods), avgCmplt(:,:), barWidth , 'stacked');
    hold off

    xLabel=strMethods;
    yLabel=strAvgCmplt;
    legendStr={'waiting time','running time', 'Location','best'};
    legend(legendStr,'FontSize', fontLegend);
     xlim([0.6 length(methods)+0.4]);
%     ylim([0 ]);
    xLabels={strUser1, strUser2, strUser3}; 
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    fileNames{figIdx} = 'avgCmplt';
end

if ~simulation
    % compare
    for i = 1:length(methods)
        SimAvgCmplt(i) = mean(durations{i}) * scaleTime;
    end
    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');   
%     bar([ClusterAvg' SimAvgCmplt(1:5)'], 'group');
    bar([ClusterAvg' SimAvg'], 'group');
    xLabel=strMethods;
    yLabel=strAvgCmplt;
    legendStr=methods;
    xlim([0.6 length(ClusterAvg)+0.4]);
%     ylim([0 6000]);
    legend('cluster', 'simulator');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    fileNames{figIdx} = 'avg_comparison';
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

if plots(2)    
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
if plots(3) 
   STOP_TIME = 300;
   figIdx=figIdx +1;         [~, DRFSortIds] = sort(JobIds{DRFId});
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
if plots(4)    
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
if plots(5)
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

%%
if plots(5)
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
%%
if~is_printed
    return;
else
   pause(1)  
end
%%
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName EXTRA '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
return;
