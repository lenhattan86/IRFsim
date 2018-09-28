clear; close all;
addpath('matlab_func');
common_settings;
is_printed = false;

num_batch_queues = 4;
num_interactive_queue = 0;
num_queues = num_batch_queues + num_interactive_queue;
START_TIME = 0; END_TIME = 10000;  STEP_TIME = 1;
cluster_size = 4;

CPUCap = cluster_size * 32;
GPUCap = cluster_size;
MemCap = cluster_size* 128;

% figureSize = [1 1 2/3 2/3].* figSizeOneCol;
figureSize = [1 1 4/5 6/5].* figSizeOneCol;
legendSize = [1 1 4/5 1] .* legendSize;

enableSeparateLegend = false;
% barColors = colorb3(1:num_queues);
scale_down_mem = 1;

% fig_path='../IRF/figs/';

%%
result_folder= '';
output_sufix = ''; 
%%
% EC
plots=[1, 1]; 
logfolder = [result_folder 'log/'];

start_time_step = START_TIME/STEP_TIME;
max_time_step = END_TIME/STEP_TIME;
startIdx = start_time_step*num_queues+1;
endIdx = max_time_step*num_queues;
num_time_steps = max_time_step-start_time_step;
linewidth= 2;
barwidth = 1.0;
timeScale = 1;
timeInSeconds = START_TIME+STEP_TIME:STEP_TIME:END_TIME;
timeInSeconds = timeInSeconds * timeScale;

lengendStr = cell(1, num_queues);
for i=1:num_interactive_queue
%     lengendStr{i} = ['LQ-' int2str(num_interactive_queue-i)];
    lengendStr{i} = ['LQ-' int2str(i-1)];
end
for i=1:num_batch_queues
    lengendStr{i+num_interactive_queue} = ['User ' int2str(i-1)];
end
%%
% extraStr = '';
extraStr = ['_' int2str(num_batch_queues) '_' int2str(cluster_size)];
% extraStr = ['_' int2str(num_batch_queues) '_' int2str(cluster_size) '_c'];

%%
% prefixes = {'DRF', 'ES', 'DRFExt', 'AlloX', 'SJF'};
prefixes = {'FS','SRPT'};
% prefixes = {'DRFFIFO'};
% prefixes = {'AlloX'};
% prefixes = {'DRF','AlloX'};
% prefixes = {'DRFExt','SJF'};
for iFile=1:length(prefixes)
  if plots(1)   
     logFile = [ logfolder prefixes{iFile} '-output' extraStr  '.csv'];
     [queueNames, res1, res2, res3, fairScores, flag] = importResUsageLog(logFile);
     if (flag)
        figure;
        subplot(3,1,1);   
        resAll = zeros(1,num_queues*num_time_steps);
        res = res1(startIdx:length(res1));
        if(length(resAll)>length(res))
           resAll(1:length(res)) = res;
        else
           resAll = res(1:num_queues*num_time_steps);
        end
        shapeRes = reshape(resAll, num_queues, num_time_steps);
        shapeRes = fipQueues( shapeRes, num_interactive_queue, num_batch_queues);

        hBar = bar(timeInSeconds,shapeRes',barwidth,'stacked','EdgeColor','none');
        cpuUsage = sum(shapeRes);
        cpuMean = mean(cpuUsage(cpuUsage>0))/CPUCap;
%         set(hBar,{'FaceColor'},barColors);   
        ylabel('CPU');xlabel('mins');
        ylim([0 CPUCap]);
        xlim([0 max(timeInSeconds)]);
        %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
        title(prefixes{iFile},'fontsize',fontLegend);

        subplot(3,1,2);
        resAll = zeros(1,num_queues*num_time_steps);
        res = res2(startIdx:length(res2));
        if(length(resAll)>length(res))
           resAll(1:length(res)) = res;
        else
           resAll = res(1:num_queues*num_time_steps);
        end
        shapeRes = reshape(resAll,num_queues,num_time_steps);
        shapeRes = fipQueues( shapeRes, num_interactive_queue, num_batch_queues);

        hBar = bar(timeInSeconds,shapeRes',barwidth,'stacked','EdgeColor','none');
        gpuUsage = sum(shapeRes);
        gpuMean = mean(gpuUsage(gpuUsage>0))/GPUCap;
%         set(hBar,{'FaceColor'},barColors);   
        ylabel('GPU','FontSize',fontAxis);xlabel('mins','FontSize',fontAxis);
        ylim([0 GPUCap]);
        xlim([0 max(timeInSeconds)]);
        %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
        %title('DRF - Memory','fontsize',fontLegend);

        subplot(3,1,3);
        resAll = zeros(1,num_queues*num_time_steps);
        res = res3(startIdx:length(res3));
        if(length(resAll)>length(res))
           resAll(1:length(res)) = res;
        else
           resAll = res(1:num_queues*num_time_steps);
        end 
        shapeRes = reshape(resAll,num_queues,num_time_steps);
        shapeRes = fipQueues( shapeRes, num_interactive_queue, num_batch_queues);

        hBar = bar(timeInSeconds,shapeRes',barwidth,'stacked','EdgeColor','none');
%         set(hBar,{'FaceColor'},barColors);   
        ylabel('memory');xlabel('mins');
        ylim([0 MemCap]);
        xlim([0 max(timeInSeconds)]);
        %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
        %title('DRF - Memory','fontsize',fontLegend);

        set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
        if is_printed         
          figIdx=figIdx +1;
          fileNames{figIdx} = ['q' int2str(num_batch_queues) '_res_usage_' prefixes{iFile}];         
          epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
          print ('-depsc', epsFile);
        end
     end
  end
end
%%

%% create dummy graph with legends
if enableSeparateLegend
  figure
  hBar = bar(timeInSeconds,shapeRes',barwidth,'stacked','EdgeColor','none');
%   set(hBar,{'FaceColor'},barColors);
  legend(lengendStr,'Location','southoutside','FontSize',fontLegend,'Orientation','horizontal');
  set(gca,'FontSize',fontSize);
  axis([20000,20001,20000,20001]) %move dummy points out of view
  axis off %hide axis  
  set(gca,'YColor','none');
  set (gcf, 'Units', 'Inches', 'Position', legendSize, 'PaperUnits', 'inches', 'PaperPosition', legendSize);    

  if is_printed   
      figIdx=figIdx +1;
    fileNames{figIdx} = ['q' int2str(num_batch_queues)  '_res_usage_legend'];        
    epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
    print ('-depsc', epsFile);
  end
end

% if is_printed
%    pause(30);
%    close all;
% end

return;
%%
extra='_bad';
for i=1:length(fileNames)
    fileName = fileNames{i}
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName extra '.pdf']    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
%fileNames