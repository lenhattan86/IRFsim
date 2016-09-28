addpath('matlab_func');
common_settings;
%%
% result_folder = 'result_20160927_long/';
% result_folder = 'result_20160927_short/';
result_folder = '';
output_folder = [result_folder 'output/'];
is_printed = false;
% fig_path = 'C:\Users\lenha\Documents\GitHub\NSDI17\fig\';
fig_path = 'figs\';
%%
plot = false;
if plot 
   INTERACTIVE_QUEUE = 'interactive';
   [ drf_avg_compl_time ] = obtain_compl_time( output_folder, drf_compl_files, INTERACTIVE_QUEUE);
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, INTERACTIVE_QUEUE);
   [ drfw_avg_compl_time ] = obtain_compl_time( output_folder, drfw_compl_files, INTERACTIVE_QUEUE);
   [ strict_priority_avg_compl_time ] = obtain_compl_time( output_folder, strict_priority_compl_files, INTERACTIVE_QUEUE);

   interactive_time = [drf_avg_compl_time ; speedfair_avg_compl_time; drfw_avg_compl_time; strict_priority_avg_compl_time];

   figure;
   bar(interactive_time', 'group');
   ylabel('time (seconds)');
   xlabel('number of batch queues');
   legend({'DRF','SpeedFair','DRF weight', 'strict priority'},'Location','northwest');
   % ylim([0 6]);
   set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
   if is_printed
      print ('-depsc', [fig_path 'interactive_compl_time.eps']);
   end

   %%
   BATCH_QUEUE = 'batch';
   [ drf_avg_compl_time ] = obtain_compl_time( output_folder, drf_compl_files, BATCH_QUEUE);
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, BATCH_QUEUE);
   [ drfw_avg_compl_time ] = obtain_compl_time( output_folder, drfw_compl_files, BATCH_QUEUE);
   [ strict_priority_avg_compl_time ] = obtain_compl_time( output_folder, strict_priority_compl_files, BATCH_QUEUE);

   batch_time = [drf_avg_compl_time ; speedfair_avg_compl_time; drfw_avg_compl_time; strict_priority_avg_compl_time];

   figure;
   bar(batch_time', 'group');
   ylabel('time (seconds)');
   xlabel('number of batch queues');
   legend({'DRF','SpeedFair','DRF weight', 'strict priority'},'Location','northwest');
   % ylim([0 6]);
   set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
   if is_printed
      print ('-depsc', [fig_path 'batch_compl_time.eps']);
   end

end
%%
plot = true;
   logfolder = [result_folder 'log/'];
   num_queues = 5;
   max_time_step = 100;
   linewidth=2;
   barwidth = 1.0;
   
if plot 
   logFile = [ logfolder 'SpeedFair-output49_1_50.csv'];
   figure;
   [queueNames, res1, res2] = import_res_usage(logFile);
   resCutOff = res1(1:max_time_step*num_queues);
   shapeRes1 = reshape(resCutOff,num_queues,max_time_step);
   bar(shapeRes1',barwidth,'stacked','EdgeColor','none');
   ylim([0 200]);
   legend('interactive','batch01','batch02','batch03','batch04');
   set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
   if is_printed
      print ('-depsc', [fig_path 'res_usage_speedfair.eps']);
   end
   return;
   logFile = [ logfolder 'DRF-output49_1_50.csv'];
   figure;
   [queueNames, res1, res2] = import_res_usage(logFile);
   resCutOff = res1(1:max_time_step*num_queues);
   shapeRes1 = reshape(resCutOff,num_queues,max_time_step);
   bar(shapeRes1',barwidth,'stacked','EdgeColor','none');
   ylim([0 200]);
   legend('interactive','batch01','batch02','batch03','batch04');
   set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
   if is_printed
      print ('-depsc', [fig_path 'res_usage_drf.eps']);
   end

   logFile = [ logfolder 'DRF-W-output49_1_50.csv'];
   figure;
   [queueNames, res1, res2] = import_res_usage(logFile);
   resCutOff = res1(1:max_time_step*num_queues);
   shapeRes1 = reshape(resCutOff,num_queues,max_time_step);
   bar(shapeRes1',barwidth,'stacked','EdgeColor','none');
   ylim([0 200]);
   legend('interactive','batch01','batch02','batch03','batch04');
   set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
   if is_printed
      print ('-depsc', [fig_path 'res_usage_drf-w.eps']);
   end
   
   logFile = [ logfolder 'Strict-output49_1_50.csv'];
   figure;
   [queueNames, res1, res2] = import_res_usage(logFile);
   resCutOff = res1(1:max_time_step*num_queues);
   shapeRes1 = reshape(resCutOff,num_queues,max_time_step);
   bar(shapeRes1',barwidth,'stacked','EdgeColor','none');
   ylim([0 200]);
   legend('interactive','batch01','batch02','batch03','batch04');
   set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
   if is_printed
      print ('-depsc', [fig_path 'res_usage_strict.eps']);
   end
end

if is_printed
   close all;
end