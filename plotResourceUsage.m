clear; close all;
addpath('matlab_func');
common_settings;

workload='BB';
% workload='TPCDS';
% workload='TPCH';

num_batch_queues = 1;
num_interactive_queue = 3;
num_queues = num_batch_queues + num_interactive_queue;
START_TIME = 0; END_TIME = 800;
is_printed = true;
cluster_size = 1000;

figureSize = [1 1 2/3 2/3].* figSizeOneCol;

enableSeparateLegend = true;

barColors = colorb1i3(1:num_queues);
% barColors = colorb8i1(1:num_queues);

%%

% result_folder = 'result/20161008/vshort/'; STEP_TIME = 0.1; output_sufix = 'vshort-interactive/';
% result_folder = 'result/20161008/short/'; STEP_TIME = 1.0; output_sufix = 'short-interactive/';
% result_folder = 'result/20161008/long/';  STEP_TIME = 1.0; output_sufix = 'long-interactive/';
% result_folder = 'result/20161008/short_m/'; STEP_TIME = 1.0; output_sufix = 'short_m/';

% result_folder = ['result/20170105/' workload '/']; STEP_TIME = 1.0; output_sufix = '';


%%
result_folder= '';
% result_folder = '../0_run_simple/'; workload='simple';
% result_folder = '../0_run_BB/'; workload='BB';
% result_folder = '../0_run_BB2/'; workload='BB2';
% result_folder = '../0_run_TPC-H/'; workload='TPC-H'; % weird
% result_folder = '../0_run_TPC-DS/'; workload='TPC-DS'; % okay 
% STEP_TIME = 1.0; output_sufix = '';
% fig_path = ['figs/' output_sufix]; 
% is_printed = true;


% output_sufix = 'vshort/'; STEP_TIME = 0.1; 
output_sufix = 'short/'; STEP_TIME = 1.0; 
% output_sufix = 'long/'; STEP_TIME = 1.0; 
% result_folder = ['result/20161023/' workload '/' output_sufix '/']; 
% fig_path = ['../EuroSys17/fig/' workload '-'];

%%
plots = [true, false, true, true]; %DRF, DRF-W, Strict, SpeedFair
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
    lengendStr{i+num_interactive_queue} = ['TQ-' int2str(i-1)];
end
%%
% extraStr = '';
extraStr = ['_' int2str(num_interactive_queue) '_' int2str(num_batch_queues) '_' int2str(cluster_size)];
% extraStr = ['_avg2.0'];
% extraStr = ['_err_base'];

   %%
if plots(1)   
   logFile = [ logfolder 'DRF-output' extraStr  '.csv'];
   [queueNames, res1, res2, flag] = importResUsageLog(logFile);   
   
   if (flag)
      figure;
      subplot(2,1,1);   
      resAll = zeros(1,num_queues*num_time_steps);
      res = res1(startIdx:length(res1));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res1(startIdx:endIdx);
      shapeRes1 = flipud(reshape(resAll,num_queues,num_time_steps));
%       shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);   
      ylabel('CPUs');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('DRF - CPUs','fontsize',fontLegend);
      
      subplot(2,1,2);
      resAll = zeros(1,num_queues*num_time_steps);
      res = res2(startIdx:length(res2));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res2(startIdx:endIdx);
      shapeRes1 = flipud(reshape(resAll,num_queues,num_time_steps));
%       shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);   
      ylabel('GB');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('DRF - Memory','fontsize',fontLegend);
      
      set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
      if is_printed         
          figIdx=figIdx +1;
        fileNames{figIdx} = ['b' int2str(num_batch_queues)  'i' int2str(num_interactive_queue) '_res_usage_drf'];         
        epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
      end
   end
end
if plots(2)
   logFile = [ logfolder 'DRF-W-output' extraStr '.csv'];
   [queueNames, res1, res2, flag] = importResUsageLog(logFile);
   
   if (flag)       
      figure;
      
      subplot(2,1,1);
      resAll = zeros(1,num_queues*num_time_steps);
      res = res1(startIdx:length(res1));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res1(startIdx:endIdx);
      shapeRes1 = flipud(reshape(resAll,num_queues,num_time_steps));
%       shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);   
      ylabel('CPUs');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('DRF-W - CPUs','fontsize',fontLegend);
      
      subplot(2,1,2);
      resAll = zeros(1,num_queues*num_time_steps);
      res = res2(startIdx:length(res2));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res2(startIdx:endIdx);
      shapeRes1 = flipud(reshape(resAll,num_queues,num_time_steps));
%       shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);   
      ylabel('GB');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('DRF-W - Memory','fontsize',fontLegend);
      
      set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
      if is_printed  
          figIdx=figIdx +1;
        fileNames{figIdx} = ['b' int2str(num_batch_queues)  'i' int2str(num_interactive_queue) '_res_usage_drf-w'];        
        epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
      end
   end
end

if plots(3)  
   logFile = [ logfolder 'Strict-output' extraStr '.csv']
   [queueNames, res1, res2, flag] = importResUsageLog(logFile);
   
   if (flag)       
      figure;
      
      subplot(2,1,1);
      resAll = zeros(1,num_queues*num_time_steps);
      res = res1(startIdx:length(res1));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res1(startIdx:endIdx);
      shapeRes1 = flipud(reshape(resAll,num_queues,num_time_steps));
%       shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);   
      ylabel('CPUs');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('Strict Priority - CPUs','fontsize',fontLegend);
      
      subplot(2,1,2);
      resAll = zeros(1,num_queues*num_time_steps);
      res = res2(startIdx:length(res2));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res2(startIdx:endIdx);
      shapeRes1 = flipud(reshape(resAll,num_queues,num_time_steps));
%       shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);   
      ylabel('GB');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('Strict Priority - Memory','fontsize',fontLegend);
      
      set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
      if is_printed     
          figIdx=figIdx +1;
        fileNames{figIdx} = ['b' int2str(num_batch_queues) 'i' int2str(num_interactive_queue) '_res_usage_strict'];       
        epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
      end
   end
end
%%
if plots(4)   
   logFile = [ logfolder 'SpeedFair-output' extraStr '.csv']
   [queueNames, res1, res2, flag] = importResUsageLog(logFile);
   if (flag)    
      figure;
      subplot(2,1,1); 
      resAll = zeros(1,num_queues*num_time_steps);
      res = res1(startIdx:length(res1));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res1(startIdx:endIdx);

      shapeRes1 = flipud(reshape(resAll,num_queues,num_time_steps));
%       shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);       
      
      ylabel('CPUs');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %%legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('SpeedFair - CPUs','fontsize',fontLegend);
      
      subplot(2,1,2); 
      resAll = zeros(1,num_queues*num_time_steps);
      res = res2(startIdx:length(res2));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res2(startIdx:endIdx);
      shapeRes1 = flipud(reshape(resAll,num_queues,num_time_steps));
%       shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      set(hBar,{'FaceColor'},barColors);  
      
      ylabel('GB');xlabel('seconds');
      ylim([0 cluster_size]);
      xlim([0 max(timeInSeconds)]);
      %%legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      %title('SpeedFair - Memory','fontsize',fontLegend);
      
      set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);     
      if is_printed   
          figIdx=figIdx +1;
        fileNames{figIdx} = ['b' int2str(num_batch_queues) 'i' int2str(num_interactive_queue) '_res_usage_speedfair'];        
        epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
      end
   end
end

%% create dummy graph with legends
if enableSeparateLegend
  figure
  hBar = bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
  set(hBar,{'FaceColor'},barColors);
  legend(lengendStr,'Location','southoutside','FontSize',fontLegend,'Orientation','horizontal');
  set(gca,'FontSize',fontSize);
  axis([20000,20001,20000,20001]) %move dummy points out of view
  axis off %hide axis  
  set(gca,'YColor','none');      
  set (gcf, 'Units', 'Inches', 'Position', legendSize, 'PaperUnits', 'inches', 'PaperPosition', legendSize);    

  if is_printed   
      figIdx=figIdx +1;
    fileNames{figIdx} = ['b' int2str(num_batch_queues) 'i' int2str(num_interactive_queue)  '_res_usage_legend'];        
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
fileNames
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ LOCAL_FIG fileName '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end