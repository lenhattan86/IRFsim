addpath('matlab_func');
common_settings;
%%
result_folder = '';
% result_folder = 'result_20161003_short/';
% result_folder = 'result_20161003_long/';
% result_folder = 'result_20161003_vlong/';


output_folder = [result_folder 'output/'];
is_printed = true;
% fig_path = 'C:\Users\lenha\Documents\GitHub\NSDI17\fig\';
fig_path = 'figs\';
%%
plots = [true, true];
if plots(1) 
   INTERACTIVE_QUEUE = 'interactive';
   
   [ drf_avg_compl_time ] = obtain_compl_time( output_folder, drf_compl_files, INTERACTIVE_QUEUE);
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, INTERACTIVE_QUEUE);
   [ drfw_avg_compl_time ] = obtain_compl_time( output_folder, drfw_compl_files, INTERACTIVE_QUEUE);
   [ strict_priority_avg_compl_time ] = obtain_compl_time( output_folder, strict_priority_compl_files, INTERACTIVE_QUEUE);

   interactive_time = [drf_avg_compl_time ;  drfw_avg_compl_time; strict_priority_avg_compl_time; speedfair_avg_compl_time];

   figure;
   bar(interactive_time', 'group');
   ylabel('time (seconds)');
   xlabel('number of batch queues');
   legend({'DRF', 'DRF weight', 'strict priority', 'SpeedFair'}, 'Location', 'northwest');
   % ylim([0 6]);
   title('Average completion time of interactive jobs');
   set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
   if is_printed
      print ('-depsc', [fig_path 'interactive_compl_time.eps']);
   end
end
if plots(2) 
   %%
   BATCH_QUEUE = 'batch';
   [ drf_avg_compl_time ] = obtain_compl_time( output_folder, drf_compl_files, BATCH_QUEUE);
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, BATCH_QUEUE);
   [ drfw_avg_compl_time ] = obtain_compl_time( output_folder, drfw_compl_files, BATCH_QUEUE);
   [ strict_priority_avg_compl_time ] = obtain_compl_time( output_folder, strict_priority_compl_files, BATCH_QUEUE);

   batch_time = [drf_avg_compl_time ; drfw_avg_compl_time; strict_priority_avg_compl_time; speedfair_avg_compl_time];

   figure;
   bar(batch_time', 'group');
   ylabel('time (seconds)');
   xlabel('number of batch queues');
   legend({'DRF','DRF weight', 'strict priority','SpeedFair'},'Location','northwest');
   title('Average completion time of batch jobs');
   % ylim([0 6]);
   set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
   if is_printed
      print ('-depsc', [fig_path 'batch_compl_time.eps']);
   end

end
%%
plots = [true, true, true , true]; %DRF, DRF-W, Strict, SpeedFair
logfolder = [result_folder 'log/'];
num_batch_queues = 4;
num_interactive_queue = 1;
STEP_TIME = 0.2;
num_queues = num_batch_queues+num_interactive_queue;
START_TIME = 0; END_TIME = 90;
start_time_step = START_TIME/STEP_TIME;
max_time_step = END_TIME/STEP_TIME;
startIdx = start_time_step*num_queues+1;
endIdx = max_time_step*num_queues;
num_time_steps = max_time_step-start_time_step;
linewidth= 2;
barwidth = 1.0;
timeInSeconds = START_TIME+STEP_TIME:STEP_TIME:END_TIME;
MAX_RESOURCE = 200;
   
if plots(1)   
   logFile = [ logfolder 'DRF-output_' int2str(num_batch_queues) '.csv'];
   [queueNames, res1, res2, flag] = import_res_usage(logFile);
   
   if (flag)
      figure;
      subplot(2,1,1);      
      resCutOff = res1(startIdx:endIdx);
      shapeRes1 = reshape(resCutOff,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('CPUs');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      legend('interactive','batch01','batch02','batch03','batch04');
      title('DRF - CPUs');
      
      subplot(2,1,2);
      resCutOff = res2(startIdx:endIdx);
      shapeRes1 = reshape(resCutOff,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('GB');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      legend('interactive','batch01','batch02','batch03','batch04');
      title('DRF - Memory');
      
      set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
      if is_printed
         print ('-depsc', [fig_path 'res_usage_drf.eps']);
      end
   end
end
if plots(2)
   logFile = [ logfolder 'DRF-W-output_' int2str(num_batch_queues) '.csv'];
   [queueNames, res1, res2, flag] = import_res_usage(logFile);
   if (flag)
      figure;
      
      subplot(2,1,1);
      resCutOff = res1(startIdx:endIdx);
      shapeRes1 = reshape(resCutOff,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('CPUs');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      legend('interactive','batch01','batch02','batch03','batch04');
      title('DRF-W - CPUs');
      
      subplot(2,1,2);
      resCutOff = res2(startIdx:endIdx);
      shapeRes1 = reshape(resCutOff,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('GB');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      legend('interactive','batch01','batch02','batch03','batch04');
      title('DRF-W - Memory');
      
      set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
      if is_printed
         print ('-depsc', [fig_path 'res_usage_drf-w.eps']);
      end
   end
end

if plots(3)  
   logFile = [ logfolder 'Strict-output_' int2str(num_batch_queues) '.csv'];
   [queueNames, res1, res2, flag] = import_res_usage(logFile);
   if (flag)
      figure;
      
      subplot(2,1,1);
      resCutOff = res1(startIdx:endIdx);
      shapeRes1 = reshape(resCutOff,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('CPUs');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      legend('interactive','batch01','batch02','batch03','batch04');
      title('Strict Priority - CPUs');
      
      subplot(2,1,2);
      resCutOff = res2(startIdx:endIdx);
      shapeRes1 = reshape(resCutOff,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('GB');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      legend('interactive','batch01','batch02','batch03','batch04');
      title('Strict Priority - Memory');
      
      set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
      if is_printed
         print ('-depsc', [fig_path 'res_usage_strict.eps']);
      end
   end
end
if plots(4)   
   logFile = [ logfolder 'SpeedFair-output_' int2str(num_batch_queues) '.csv'];
   [queueNames, res1, res2, flag] = import_res_usage(logFile);
   if (flag)
      figure;
      subplot(2,1,1); 
      resCutOff = res1(startIdx:endIdx);
      shapeRes1 = reshape(resCutOff,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('CPUs');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      legend('interactive','batch01','batch02','batch03','batch04');
      title('SpeedFair - CPUs');      
      
      subplot(2,1,2); 
      resCutOff = res2(startIdx:endIdx);
      shapeRes1 = reshape(resCutOff,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('GB');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      legend('interactive','batch01','batch02','batch03','batch04');
      title('SpeedFair - Memory');
      
      set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);      
      if is_printed
         print ('-depsc', [fig_path 'res_usage_speedfair.eps']);
      end
   end
end

% if is_printed
%    close all;
% end