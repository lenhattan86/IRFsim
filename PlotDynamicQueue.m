addpath('matlab_func');
common_settings;

fig_path = '/home/tanle/Dropbox/proposals/QueueScheduler/fig/';

queues = {1,2,4,8,16,32};
single_queue_files = {'DRF-output_u1_u1.csv';
                      'DRF-output_u1_u2.csv';
                      'DRF-output_u1_u3.csv'};
                  
multi_queue_files = {'DRF-output_u1_u1.csv';
                     'DRF-output_u2_u2.csv';
                     'DRF-output_u3_u3.csv'};

%%
% result_folder = '';
% result_folder = '/home/tanle/projects/0_dynamic_q/';
result_folder = '/home/tanle/projects/0_dynamic_q2/';
workload='BB';
user1_q_num = 1;
user2_q_num = 10;
num_queues = user1_q_num + user2_q_num;
START_TIME = 0; END_TIME = 4000;
STEP_TIME = 1.0;
is_printed = true;


%%
output_folder = [result_folder 'output/'];
output_file = ['DRF-outputu1_u10.csv'];

figIdx = 0;

% fig_path = 'figs\';
%%
% global batchJobRange
% batchJobRange = [1:10]

queues_len = length(queues);
plots  = [false true true];
improvements = zeros(queues_len, 4);
user1_queue = 'user1_';
user2_queue = 'user2_';
if plots(1) 
   [ user1_compl_time ]     = obtain_compl_time( output_folder, drf_compl_files, user1_queue);
   [ user2_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, user2_queue);   

   interactive_time = [drf_avg_compl_time ;  drfw_avg_compl_time; strict_priority_avg_compl_time; speedfair_avg_compl_time];   
   
   figure;
   scrsz = get(groot,'ScreenSize');   
   bar(interactive_time', 'group');
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel='number of batch queues';
    yLabel='time (seconds)';
    legendStr={'DRF', 'DRF weight', 'strict priority', 'SpeedFair'};

    xLabels=queues;
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    figSize = [0.0 0 5.0 3.0];
    set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels ,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'interactive_compl_time';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end
end
%%
user1_queue = 'user1_';
if plots(2) 
   [ user1_compl_time ]  = queue_compl_time( output_folder, output_file, user1_queue);   
   figure;
   
   bar(user1_compl_time,0.5)
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
    xLabel='Job id';
    yLabel='completion time (seconds)';       
    
    figSize = [0.0 0 5.0 3.0];
    set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'user_1_compl_time';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end
end
%%
logfolder = [result_folder 'log/'];

start_time_step = START_TIME/STEP_TIME;
max_time_step = END_TIME/STEP_TIME;
startIdx = start_time_step*num_queues+1;
endIdx = max_time_step*num_queues;
num_time_steps = max_time_step-start_time_step;
linewidth= 2;
barwidth = 1.0;
timeInSeconds = START_TIME+STEP_TIME:STEP_TIME:END_TIME;
MAX_RESOURCE = 100;

lengendStr = cell(1, num_queues);
for i=1:user1_q_num
    lengendStr{i} = ['user1_' int2str(i-1)];
end
for i=1:user2_q_num
    lengendStr{i+user1_q_num} = ['user2_' int2str(i-1)];
end

% if num_interactive_queue==1
%    extraStr = '';
% else
   extraStr = ['u' int2str(user1_q_num) '_' 'u' int2str(user2_q_num)];
   
if plots(3)   
   logFile = [ logfolder 'DRF-output' extraStr '.csv'];
   [queueNames, res1, res2, flag] = import_res_usage(logFile);
   
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
      shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('CPUs');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      xlim([0 END_TIME]);
      legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      title('DRF - CPUs','fontsize',fontLegend);
      
      subplot(2,1,2);
      resAll = zeros(1,num_queues*num_time_steps);
      res = res2(startIdx:length(res2));
      if(length(resAll)>length(res))
         resAll(1:length(res)) = res;
      else
         resAll = res(1:num_queues*num_time_steps);
      end
%       resCutOff = res2(startIdx:endIdx);
      shapeRes1 = reshape(resAll,num_queues,num_time_steps);
      bar(timeInSeconds,shapeRes1',barwidth,'stacked','EdgeColor','none');
      ylabel('GB');xlabel('seconds');
      ylim([0 MAX_RESOURCE]);
      xlim([0 END_TIME]);
      legend(lengendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
      title('DRF - Memory','fontsize',fontLegend);
      
      
      figSize = [0.0 0 10.0 8.0];
      set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);
      if is_printed         
          figIdx=figIdx +1;
            fileNames{figIdx} = [extraStr '_res_usage_drf'];         
            epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
            print ('-depsc', epsFile);
      end
   end
end

return;
%%

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end