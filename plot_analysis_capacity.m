addpath('matlab_func');
common_settings;
% is_printed = 1;
%%
barWidth = 0.5;
queue_num = 20;
figureSize = figSizeThreeFourth;
plots  = [true false];
methods = {strDRFFIFO, strDRF,  strES, strAlloX, strSRPT};
files = {'DRFFIFO', 'DRF', 'ES', 'AlloX', 'SRPT'};
ESId = 3;
AlloXId = 4;
caps = [10, 11, 12, 13, 14, 15, 20, 25, 30];
load = zeros(size(caps));
methodColors = {colorES; colorDRF; colorProposed};

%% load data
resVals = zeros(length(methods), length(caps)); 
loads = zeros(1, length(caps));
for i=1:length(methods)
    for j=1:length(caps)
        extraStr = ['_' int2str(queue_num) '_' int2str(caps(j)) '_c' int2str(caps(j))];
        outputFile = [ 'output/' files{i} '-output' extraStr  '.csv'];
        [JobIds, startTimes, endTimes, durations, queueNames] = import_compl_time_real_job(outputFile);
        if(~isnan(durations))
            resVals(i,j) = mean(durations);
        end
        
        if (i==ESId)
            logFile = [ 'log/' files{i} '-output' extraStr  '.csv'];
            [queueNames, res1, res2, res3, fairScores, flag] = importResUsageLog(logFile);          
            num_time_steps = length(res1)/queue_num;
            resAll = zeros(1,queue_num*num_time_steps);
            res = res1(1:length(res1));
            if(length(resAll)>length(res))
               resAll(1:length(res)) = res;
            else
               resAll = res(1:queue_num*num_time_steps);
            end
            shapeRes = reshape(resAll, queue_num, num_time_steps);
            shapeRes = fipQueues( shapeRes, 0, queue_num);            
            cpuUsage = sum(shapeRes);
            maxTime = num_time_steps;
            for maxTime = num_time_steps:-1:1
                if cpuUsage(maxTime) > 0.99*(caps(j)*32)
                    cpuUsage = cpuUsage(1:maxTime);
                    break;
                end
            end
            cpuMean = mean(cpuUsage(cpuUsage>0))/(caps(j)*32);
            loads(j) = cpuMean*100;
        end
    end
end
%%
if plots(1)    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');   
    for i=1:length(methods)
        plot(caps, resVals(i,:), 'LineWidth', lineWidth);
        hold on;
    end    
    xLabel='gpu capacity';
    yLabel= strAvgCmplt;
    legendStr=methods;    
   xlim([min(caps) max(caps)]);
%     xlim([0.1 1]);
    legend(methods,'Location', 'best','FontSize',fontAxis);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    %     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    xlabel(xLabel,'FontSize',fontAxis);   
    fileNames{figIdx} = 'analysis_cap';    
end

%%
if plots(2)    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');   
    for i=1:length(methods)
        if (AlloXId == i)
            plot(loads, resVals(i,:), '-o', 'LineWidth', 1.5);
        else
            plot(loads, resVals(i,:), 'LineWidth', lineWidth);
        end 
        
        hold on;
    end    
    xLabel='avg. CPU usage (%)';
    yLabel= strAvgCmplt;
    legendStr=methods;    
%     ylim([0 1]);
    xlim([min(loads) max(loads)]);
    legend(methods,'Location', 'best','FontSize',fontAxis);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    %     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    xlabel(xLabel,'FontSize',fontAxis);   
    fileNames{figIdx} = 'analysis_load';    
end

return;
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName '.pdf'];
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end