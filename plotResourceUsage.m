clear; close all;
addpath('matlab_func');
common_settings;

workload='BB';

num_batch_queues = 2;
num_interactive_queue = 0;
num_queues = num_batch_queues + num_interactive_queue;
START_TIME = 0; END_TIME = 200;
is_printed = true;
cluster_size = 40;

% figureSize = [1 1 2/3 2/3].* figSizeOneCol;
figureSize = [1 1 4/5 6/5].* figSizeOneCol;
legendSize = [1 1 4/5 1] .* legendSize;

enableSeparateLegend = true;

barColors = colorb2(1:num_queues);

scale_down_mem = 1;

fig_path='../IRF/figs/';

%%



%result_folder = ['result/20170105/' workload '/']; STEP_TIME = 1.0; output_sufix = '';
% result_folder = ['result/20170127_multi/' workload '/'];


%%
result_folder= '';

output_sufix = ''; STEP_TIME = 0.1; 

%%
% EC
plots=[true, true]; 
logfolder = [result_folder 'log/'];

start_time_step = START_TIME/STEP_TIME;
max_time_step = END_TIME/STEP_TIME;
startIdx = start_time_step*num_queues+1;
endIdx = max_time_step*num_queues;
num_time_steps = max_time_step-start_time_step;
linewidth= 2;
barwidth = 1.0;
timeInSeconds = START_TIME+STEP_TIME:STEP_TIME:END_TIME;

lengendStr = cell(1, num_queues);
for i=1:num_interactive_queue
%     lengendStr{i} = ['LQ-' int2str(num_interactive_queue-i)];
    lengendStr{i} = ['LQ-' int2str(i-1)];
end
for i=1:num_batch_queues
    lengendStr{i+num_interactive_queue} = ['Queue-' int2str(i-1)];
end
%%
% extraStr = '';
extraStr = ['_' int2str(num_batch_queues) '_' int2str(cluster_size)];
extraStrReject = ['_' int2str(num_batch_queues) '_' int2str(cluster_size)];


   %%
if plots(1)   
   logFile = [ logfolder 'ES-output' extraStr  '.csv'];
   [queueNames, res1, res2, res3, flag] = importResUsageLog(logFile);   
   
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
      shapeRes = reshape(resAll,num_queues,num_time_steps);
      shapeRes = fipQueues( shapeRes, num_interactive_queue, num_batch_queues);
      
      hBar = bar(timeInSeconds,shapeRes',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);   
      ylabel('CPU');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('DRF - CPU','fontsize',fontLegend);
      
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
      set(hBar,{'FaceColor'},barColors);   
      ylabel('GPU');xlabel('seconds');
      ylim([0 cluster_size]);
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
      set(hBar,{'FaceColor'},barColors);   
      ylabel('memory');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('DRF - Memory','fontsize',fontLegend);
      
      set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
      if is_printed         
          figIdx=figIdx +1;
        fileNames{figIdx} = ['q' int2str(num_batch_queues) '_' int2str(scale_down_mem) '_res_usage_es'];         
        epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
      end
   end
end

%%
if plots(2)   
   logFile = [ logfolder 'MaxMinMem-output' extraStr  '.csv'];
   [queueNames, res1, res2, res3, flag] = importResUsageLog(logFile);   
   
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
      shapeRes = reshape(resAll,num_queues,num_time_steps);
      shapeRes = fipQueues( shapeRes, num_interactive_queue, num_batch_queues);
      
      hBar = bar(timeInSeconds,shapeRes',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);   
      ylabel('CPU');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('DRF - CPU','fontsize',fontLegend);
      
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
      set(hBar,{'FaceColor'},barColors);   
      ylabel('GPU');xlabel('seconds');
      ylim([0 cluster_size]);
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
      set(hBar,{'FaceColor'},barColors);   
      ylabel('memory');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('DRF - Memory','fontsize',fontLegend);
      
      set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
      if is_printed         
          figIdx=figIdx +1;
        fileNames{figIdx} = ['q' int2str(num_batch_queues) '_' int2str(scale_down_mem) '_res_usage_maxminmem'];         
        epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
      end
   end
end

%%

%% create dummy graph with legends
if enableSeparateLegend
  figure
  hBar = bar(timeInSeconds,shapeRes',barwidth,'stacked','EdgeColor','none');
  set(hBar,{'FaceColor'},barColors);
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

for i=1:length(fileNames)
    fileName = fileNames{i}
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
fileNames