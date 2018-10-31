addpath('matlab_func');
common_settings;
% is_printed = 1;
% fig_path = ['figs/'];
version = '_c1';
isSmallJobs = true;
%%
cluster_size= 20;
nLargeJobs = 100;
nSmallUsers = 9;
nLargeUsers = 1;
queue_num = nSmallUsers+nLargeUsers;

plots  = [true, true];
figureSize = figSizeThreeFourth;
% figureSize = figSizeOneCol;

% maxEndTime = max(endTimes_l{AlloXId}); % AlloX
maxEndTime = 20000; 
maxStartTime = 10000;

%%
% methods = {strDRFFIFO, strDRFSJF, strES, strDRFExt, strAlloX, 'AlloXopt', strSRPT};
% files = {'DRFFIFO', 'DRF', 'ES', 'DRFExt', 'AlloX', 'AlloXopt', 'SRPT'};
% DRFFIFOId = 1; DRFId = 2; ESId = 3; DRFExtId = 4;  AlloXId = 5; AlloXIdOpt = 6; SRPTId = 7;
methods = {strDRFFIFO, strDRFSJF, strES, strDRFExt, strAlloX, strSRPT};
colors = {colorDRFFIFO, colorDRFSJF,  colorES, colorDRFExt, colorAlloX, colorSRPT};
files = {'DRFFIFO', 'DRF', 'ES', 'DRFExt', 'AlloX', 'SRPT'};
DRFFIFOId = 1; DRFId = 2; ESId = 3; DRFExtId = 4;  AlloXId = 5; SRPTId = 6;

alphas = [0.1 0.3];
methodColors = {colorES; colorDRF; colorProposed};

extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) version];
EXTRA='';
JobIds={};
startTimes={};
endTimes = {};
durations = {};
queueNames = {};
startRunningTimes = {};
runningTimes = {};
scaleTime = 1; % minutes



logfolder = ['log/'];
%% load data
commonShortIds = [];
for i=1:length(methods)

    outputFile = [ 'output/' files{i} '-output' extraStr  '.csv'];
    [JobIds{i}, startTimes{i}, endTimes{i}, durations{i}, queueNames{i}, startRunningTimes{i}, runningTimes{i}] ...
        = import_compl_time(outputFile);   

    waitingTimes{i} = durations{i} - runningTimes{i};
%     runningTimes{i} = durations{i} - waitingTimes{i};

    fullJobsIndices{i} = find(JobIds{i}>=0);
    % for long jobs
    largeJobsIndices{i} = find( JobIds{i}>=0 & (JobIds{i}<nLargeJobs*(nSmallUsers+nLargeUsers)) ...
        & (mod( JobIds{i}, (nSmallUsers+nLargeUsers) )>=nSmallUsers));    
    
    % for short jobs
    smallJobsIndices{i} = find( JobIds{i}>=0 & ...
        ( (JobIds{i}<nLargeJobs*(nSmallUsers+nLargeUsers) & (mod(JobIds{i},(nSmallUsers+nLargeUsers) )< nSmallUsers)) ...
                | (JobIds{i}>=nLargeJobs*(nSmallUsers+nLargeUsers))) );
            
    if (length(smallJobsIndices{i}) + length(largeJobsIndices{i}) ~= sum(JobIds{i}>=0))
        error('wrong computation here');
    end

    durations_s{i} = durations{i}(smallJobsIndices{i}); 
    waitingTimes_s{i} = waitingTimes{i}(smallJobsIndices{i});
    runningTimes_s{i} = runningTimes{i}(smallJobsIndices{i});
    JobIds_s{i} = JobIds{i}(smallJobsIndices{i});
    startTimes_s{i} = startTimes{i}(smallJobsIndices{i});
    endTimes_s{i} = endTimes{i}(smallJobsIndices{i});
    queueNames_s{i} = queueNames{i}(smallJobsIndices{i});
    startRunningTimes_s{i} = startRunningTimes{i}(smallJobsIndices{i});
    
    if(length(JobIds_s{i})>0)
        if(i>1)
            commonShortIds = intersect(commonShortIds,JobIds_s{i});
        else
            commonShortIds = JobIds_s{i};
        end
    end
    
    durations_l{i} = durations{i}(largeJobsIndices{i}); 
    waitingTimes_l{i} = waitingTimes{i}(largeJobsIndices{i});
    runningTimes_l{i} = runningTimes{i}(largeJobsIndices{i});
    JobIds_l{i} = JobIds{i}(largeJobsIndices{i});
    startTimes_l{i} = startTimes{i}(largeJobsIndices{i});
    endTimes_l{i} = endTimes{i}(largeJobsIndices{i});
    queueNames_l{i} = queueNames{i}(largeJobsIndices{i});
    startRunningTimes_l{i} = startRunningTimes{i}(largeJobsIndices{i});
end


%% Do not use this one as it farvors the SJF based schedulers
if true
    for i=1:length(methods)    
        % care the start time for small jobs.
%         smallIds = find(startTimes_s{i}<=maxStartTime);
%         [~, smallIds] = intersect( JobIds_s{i}, commonShortIds) ;
%         smallIds = find(endTimes_s{i}<=maxEndTime);
        smallIds = 1:length(JobIds_s{i});
        length(smallIds)
        
        durations_s{i} = durations_s{i}(smallIds);
        waitingTimes_s{i} =waitingTimes_s{i}(smallIds);
        runningTimes_s{i} =runningTimes_s{i}(smallIds);
        JobIds_s{i} =  JobIds_s{i}(smallIds);
        startTimes_s{i} = startTimes_s{i}(smallIds);
        endTimes_s{i} =  endTimes_s{i}(smallIds);
        queueNames_s{i} = queueNames_s{i}(smallIds);
        startRunningTimes_s{i} =  startRunningTimes_s{i}(smallIds);

        % care the end time for large jobs.
        largeIds = find(endTimes_l{i}<=maxEndTime);    
        
        durations_l{i} = durations_l{i}(largeIds) ; 
        waitingTimes_l{i} = waitingTimes_l{i}(largeIds);
        runningTimes_l{i} = runningTimes_l{i} (largeIds);
        JobIds_l{i} = JobIds_l{i}((largeIds));
        startTimes_l{i} = startTimes_l{i}((largeIds));
        endTimes_l{i} = endTimes_l{i}((largeIds));
        queueNames_l{i} = queueNames_l{i}((largeIds));
        startRunningTimes_l{i} = startRunningTimes_l{i}((largeIds));
    end
end

%%
if plots(1)    
   figIdx=figIdx +1;  
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');   

    stackData = zeros(length(methods), 3 , 2);
    
    for i = 1:length(methods)
        avgCmplt(i,:) = mean([waitingTimes_s{i} runningTimes_s{i}]) * scaleTime;        
    end

    h=bar(1:length(methods), avgCmplt(:,:), barWidth , 'stacked'); 
      
    temp = sum(avgCmplt'); 
    improvement = (temp-temp(AlloXId))./temp * 100
    maxImprovement = (temp-temp(SRPTId))./temp * 100
    xLabel=strMethods;
    yLabel=strAvgCmplt;
    legendStr={'wait time','runtime'};
    legend(legendStr, 'Location','northeast','FontSize', fontLegend);
    xlim([0.6 length(methods)+0.4]);
    ylabel(yLabel,'FontSize', fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    box off;

%     axes('position',[.35 .25 0.32 0.7])
%     box on % put box around new pair onSmallUsersf axes
%     plotBarStackGroups(stackData(3:end,:,:), methods(3:end), false);
%     axis tight
%     xlim([2.6 length(methods)+0.4]);


%     set(gca, 'YScale', 'log'); 
%     grid on;
%     ylim([0 ]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);

    fileNames{figIdx} = 'avgCmplt_small';
end

%%
%%
if false    
%    figIdx=figIdx +1;  
%    figures{figIdx} = figure;
    figure
   scrsz = get(groot, 'ScreenSize');   

    stackData = zeros(length(methods), 3 , 2);
    
    for i = 1:length(methods)
        avgCmplt(i,:) = mean([waitingTimes_l{i} runningTimes_l{i}]) * scaleTime;        
    end

    h=bar(1:length(methods), avgCmplt(:,:), barWidth , 'stacked'); 
      
    temp = sum(avgCmplt'); 
%     improvement = (temp-temp(AlloXId))./temp * 100
%     maxImprovement = (temp-temp(SRPTId))./temp * 100
    xLabel=strMethods;
    yLabel=strAvgCmplt;
    legendStr={'wait time','runtime','wait-top 5%','run-top 5%','wait-top 1%','run-top 1%',};
    legend(legendStr, 'Location','northeastoutside','FontSize', fontLegend);
    xlim([0.6 length(methods)+0.4]);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);

%     axes('position',[.35 .25 0.32 0.7])
%     box on % put box around new pair of axes
%     plotBarStackGroups(stackData(3:end,:,:), methods(3:end), false);
%     axis tight
%     xlim([2.6 length(methods)+0.4]);


%     set(gca, 'YScale', 'log'); 
%     grid on;
%     ylim([0 ]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize.*[1 1 1.4 1], 'PaperUnits', 'inches', 'PaperPosition', figureSize.*[1 1 1.4 1]);
%     fileNames{figIdx} = 'avgCmplt_large';
end

%%
%%
if plots(2)   
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');     
    hold on
    normMakespan = -1;
    for i = 1:length(methods)
        numJobs = sum(endTimes_l{i} <= maxEndTime);    
        h=bar(i, numJobs , barWidth, 'FaceColor', colors{i});
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

if false    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');     
    hold on
    normMakespan = -1;
    for i = 1:length(methods)
        numJobs = sum(endTimes_s{i} <= maxEndTime);    
        h=bar(i, numJobs , barWidth);
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
    fileNames{figIdx} = 'job_completed_s';
end
version
%%
if~is_printed
    return;
else
   pause(1);
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