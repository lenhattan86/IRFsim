addpath('matlab_func');
common_settings;
%%
barWidth = 0.5;
queue_num = 20;
cluster_size=20;
GPU_CAP = 10;
CPU_CAP = GPU_CAP*32;
MEM_CAP = (GPU_CAP*2) * 64;
figureSize = figSizeThreeFourth;
plots  = [false, false, true];
methods = {strDRFFIFO, strDRF, strES, strDRFExt,  strAlloX, 'SRPT'};
% files = {'DRF', 'ES', 'AlloX'};
% speedups = [0.1, 0.5, 1.0]
% alphas = [0.05 0.1 0.2 0.3 0.4];
alphas = [0.05, 0.1, 0.2, 0.3, 0.4];
files = {'DRFFIFO','DRF', 'ES', 'DRFExt', 'AlloX', 'SRPT'};
DRFFId = 1; DRFId = 2; ESId = 3; DRFExtId = 4;  AlloXId = 5; SRPTId = 6;
methodColors = {colorES; colorDRF; colorProposed};

START_TIME = 0; END_TIME = 3000;  STEP_TIME = 1;
start_time_step = START_TIME/STEP_TIME;
max_time_step = END_TIME/STEP_TIME;
startIdx = start_time_step*queue_num+1;
num_time_steps = max_time_step-start_time_step;

%% load data & compute what we need

for i=1:length(methods)
    for j=1:length(alphas)
%         extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_a' sprintf('%1.2f',alphas(j))];
        extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_a' num2str(alphas(j)) ];
        outputFile = [ output_folder files{i} '-output' extraStr  '.csv'];
        [JobIds{i,j}, startTimes{i,j}, endTimes{i,j}, durations{i,j}, queueNames{i,j}] = import_compl_time_real_job(outputFile);
        if(~isnan(durations{i,j}))
%             resVals(i,j) = mean(endTimes{i,j} - startTimes{i,j});
            resVals(i,j) = mean(durations{i,j});
        end
    end
end
avgCmpltES = resVals(ESId,1);
avgCmpltSRPT = resVals(SRPTId,1);

% compute JFI
for iFile=1:length(methods)  
    for j=1:length(alphas)        
        extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_a' num2str(alphas(j)) ];
        logFile = [ log_folder files{iFile} '-output' extraStr  '.csv'];
        [temp, res1, res2, res3, fairScores, flag] = importResUsageLog(logFile);        
        
        if (iFile == AlloXId)
            resAll = zeros(1,queue_num*num_time_steps);
            temp = fairScores(startIdx:length(fairScores));
            if(length(resAll)>length(temp))
               resAll(1:length(temp)) = temp;
            else
               resAll = temp(1:queue_num*num_time_steps);
            end
            fairScoreUsers = reshape(resAll, queue_num, num_time_steps);
            JFIs(iFile,j) = mean(  sum(fairScoreUsers).^2 / (queue_num*sum(fairScoreUsers.^2))  );       
        elseif (iFile == ESId)
            resAll = zeros(1,queue_num*num_time_steps);
            temp = res2(startIdx:length(res2));
            if(length(resAll)>length(temp))
               resAll(1:length(temp)) = temp;
            else
               resAll = temp(1:queue_num*num_time_steps);
            end
            gpuUsage = reshape(resAll, queue_num, num_time_steps);
            JFIs(iFile,j) = mean(sum(gpuUsage).^2 / (queue_num*sum(gpuUsage.^2))  );       
        else
            % DRF
            resAll = zeros(1,queue_num*num_time_steps);
            res = max(max(res1./CPU_CAP, res2./GPU_CAP),res3./MEM_CAP);
            temp = res(startIdx:length(res));
            if(length(resAll)>length(temp))
               resAll(1:length(temp)) = temp;
            else
               resAll = temp(1:queue_num*num_time_steps);
            end
            dominantUsage = reshape(resAll, queue_num, num_time_steps);
            JFIs(iFile,j) = mean(  sum(dominantUsage).^2 / (queue_num*sum(dominantUsage.^2))  );       
        end        
    end
end

%%
if plots(1)    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');   
    plot(alphas, resVals(AlloXId,length(alphas))./resVals(AlloXId,:), 'LineWidth', lineWidth);   
    hold on;
    plot(alphas, resVals(AlloXId,length(alphas))/avgCmpltES*ones(size(alphas)), 'LineWidth', lineWidth);   

    xLabel='\alpha';
    yLabel=strFactorImprove;
    legendStr=methods;    
    ylim([0 1]);
    xlim([0.1 1]);
    legend({'AlloX', 'ES+RP'},'Location', 'best','FontSize',fontAxis);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    %     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    xlabel(xLabel,'FontSize',fontAxis);   
    fileNames{figIdx} = 'analysis_alpha';
    
end

%
minImproveVsES = zeros(size(alphas));
maxImproveVsES = zeros(size(alphas));
for i=1:length(alphas)
    [~, DRFSortIds] = sort(JobIds{DRFId,i});
    [~, ESSortIds] = sort(JobIds{ESId,i});
    [~, AlloXSortIds] = sort(JobIds{AlloXId,i});
    [~, SRPTSortIds] = sort(JobIds{SRPTId,i});

    DRFQueues = queueNames{DRFId,i}(DRFSortIds);
    ESQueues = queueNames{ESId,i}(ESSortIds);
    AlloXQueues = queueNames{AlloXId,i}(AlloXSortIds);
    SRPTQueues = queueNames{SRPTId,i}(SRPTSortIds);

    ESTotalCompltTime = [];
    DRFTotalCompltTime = [];
    AlloXTotalCompltTime = [];
    SRPTTotalCompltTime = [];

    ESDurations = durations{ESId,i}(ESSortIds);
    DRFDurations = durations{DRFId,i}(DRFSortIds);
    AlloXDurations = durations{AlloXId,i}(AlloXSortIds);
    SRPTDurations = durations{SRPTId,i}(SRPTSortIds);

    if ~isnan(AlloXDurations) 
        queueSet = {};
        for q=1:length(ESQueues)
            queueName = ESQueues{q};
            idx = 0;        
            if ~any(strcmp(queueSet,queueName))
                queueSet{length(queueSet)+1} = queueName;
                idx = length(queueSet);
                ESTotalCompltTime = [ESTotalCompltTime  ESDurations(q)];
    %             DRFTotalCompltTime = [DRFTotalCompltTime  DRFDurations(q)];
                AlloXTotalCompltTime = [AlloXTotalCompltTime  AlloXDurations(q)];
                SRPTTotalCompltTime = [SRPTTotalCompltTime SRPTDurations(q)];
            else            
                idx = strcmp(queueSet,queueName);
                ESTotalCompltTime(idx) = ESTotalCompltTime(idx) + ESDurations(q);
    %             DRFTotalCompltTime(idx) = DRFTotalCompltTime(idx) + DRFDurations(q);
                AlloXTotalCompltTime(idx) = AlloXTotalCompltTime(idx) + AlloXDurations(q);
                SRPTTotalCompltTime(idx) = SRPTTotalCompltTime(idx) + SRPTDurations(q);
            end        
        end
        improvement = (ESTotalCompltTime./AlloXTotalCompltTime - 1)*100;
        minImproveVsES(i) = min(improvement);
        maxImproveVsES(i) = max(improvement);
        stdVsES(i) = std(improvement);
    else
        i
        disp('[ERROR] we dont have this data file');        
    end    
end

%%
    
if plots(2)    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');       
    hold on;
%     improvePercent = (resVals(ESId,:)./resVals(AlloXId,:) -1)*100;
%     improvePercent = (resVals(ESId,:)./resVals(AlloXId,:))*100;
%     plot(alphas, improvePercent, 'LineWidth', lineWidth);  
    improvePercent = (resVals(AlloXId,:)./resVals(SRPTId,:)-1)*100;
    plot(alphas, improvePercent, 'LineWidth', lineWidth);  
    hold off;
    xLabel='fairness parameter \alpha';
    yLabel=strPerfGap;
    legendStr=methods;        
    xlim([0.05 1]);
    ylim([0 30]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    legend({'vs. SRPT'});
    ylabel(yLabel,'FontSize',fontAxis);    
    xlabel(xLabel,'FontSize',fontAxis);   
    fileNames{figIdx} = 'fairness_alpha';
end


%% plot the trade-offs between fairness score and performance.
if plots(3)    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');       
    hold on;
    %% SRPT
    lengendIdx = 1;
    strLegends = {};
    
    avgComplt_SRPT = resVals(SRPTId, 1);
    
    SRPT_fair  = JFIs(SRPTId, 1);
    SRPT_performance= avgComplt_SRPT/avgComplt_SRPT;
    scatter(SRPT_fair, SRPT_performance/SRPT_performance, 'LineWidth', lineWidth);
    strLegends{lengendIdx} = strSRPT; lengendIdx = lengendIdx+ 1;
    
    if (JFIs(DRFFId, 1) > 0)
        DRFFIFO_fair = JFIs(DRFFId, 1);
        DRFFIFO_performance = avgComplt_SRPT/resVals(DRFFId, 1);
        scatter(DRFFIFO_fair, DRFFIFO_performance, 'LineWidth', lineWidth);
        strLegends{lengendIdx} = strDRFFIFO; lengendIdx = lengendIdx+ 1;
    end
    
    if (JFIs(DRFId, 1) > 0)
        DRF_fair = JFIs(DRFId, 1);
        DRF_performance = avgComplt_SRPT/resVals(DRFId, 1);
        scatter(DRF_fair, DRF_performance, 'LineWidth', lineWidth);
        strLegends{lengendIdx} = strDRF; lengendIdx = lengendIdx+ 1;
    end
    
    if (JFIs(ESId, 1) > 0)
        ES_fair = JFIs(ESId, 1);
        ES_performance = avgComplt_SRPT/resVals(ESId, 1);
        scatter(ES_fair, ES_performance, 'LineWidth', lineWidth);
        strLegends{lengendIdx} = strES; lengendIdx = lengendIdx+ 1;
    end
    
    if (JFIs(DRFExtId, 1) > 0)
        DRFExt_fair = JFIs(DRFExtId, 1);
        DRFExt_performance = avgComplt_SRPT/resVals(DRFExtId, 1);
        scatter(DRFExt_fair, DRFExt_performance, 'LineWidth', lineWidth);
        strLegends{lengendIdx} = strDRFExt; lengendIdx = lengendIdx+ 1;
    end    
    
    for i=1:length(alphas)
        strAlloXAlpha = [strAlloX ' \alpha=' num2str(alphas(i))]; 
        Allox_fair = JFIs(AlloXId, i);
        Allox_perf = avgComplt_SRPT/resVals(AlloXId, i);
        if (Allox_fair > 0)
            scatter(Allox_fair, Allox_perf, 'x', 'LineWidth', lineWidth');
            strLegends{lengendIdx} = strAlloXAlpha; lengendIdx = lengendIdx+ 1;        
        end
    end    
    
    hold off;
    xLabel='fairness';
    yLabel='performance';
    legendStr=methods;        
    xlim([0 1]);
    ylim([0 1]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize .* [1 1 1.25 1], 'PaperUnits', 'inches', 'PaperPosition', figureSize.* [1 1 1.25 1]);
    legend(strLegends,'Location', 'eastoutside', 'FontSize', fontLegend);
    ylabel(yLabel,'FontSize',fontAxis);    
    xlabel(xLabel,'FontSize',fontAxis);   
    fileNames{figIdx} = 'perf_fair_tradeoffs';
end

%%
if~is_printed
    return;
end
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end