addpath('matlab_func');
common_settings;

workload='BB';
% workload='TPCDS';
% workload='TPCH';

%%

% result_folder = 'result/20161008/vshort/'; STEP_TIME = 0.1; output_sufix = 'vshort-interactive/';
% result_folder = 'result/20161008/short/'; STEP_TIME = 1.0; output_sufix = 'short-interactive/';
% result_folder = 'result/20161008/long/';  STEP_TIME = 1.0; output_sufix = 'long-interactive/';
% result_folder = 'result/20161008/short_m/'; STEP_TIME = 1.0; output_sufix = 'short_m/';

result_folder = ['result/20170105/' workload '/']; STEP_TIME = 1.0; output_sufix = '';


%%
% result_folder= '';
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


if true
   queues = {1,2,4,8,16,32};
  drf_compl_files = {'DRF-output_1_1_1000.csv';
                    'DRF-output_1_2_1000.csv';
                    'DRF-output_1_4_1000.csv';
                    'DRF-output_1_8_1000.csv';
                    'DRF-output_1_16_1000.csv';
                    'DRF-output_1_32_1000.csv'};

  drfw_compl_files = {'DRF-W-output_1_1_1000.csv';
                    'DRF-W-output_1_2_1000.csv';
                    'DRF-W-output_1_4_1000.csv';
                    'DRF-W-output_1_8_1000.csv';
                    'DRF-W-output_1_16_1000.csv';
                    'DRF-W-output_1_32_1000.csv'};  

  speedfair_compl_files = {'SpeedFair-output_1_1_1000.csv';
                    'SpeedFair-output_1_2_1000.csv';
                    'SpeedFair-output_1_4_1000.csv';
                    'SpeedFair-output_1_8_1000.csv';
                    'SpeedFair-output_1_16_1000.csv';
                    'SpeedFair-output_1_32_1000.csv'};  

  strict_priority_compl_files = { 'Strict-output_1_1_1000.csv';
                    'Strict-output_1_2_1000.csv';
                    'Strict-output_1_4_1000.csv';
                    'Strict-output_1_8_1000.csv';
                    'Strict-output_1_16_1000.csv';
                    'Strict-output_1_32_1000.csv'};   
else  
  queues = {1,2,4,8,16,32};
    drf_compl_files = {'DRF-output_1_1_1000.csv';
                      'DRF-output_1_2_1000.csv';
                      'DRF-output_1_4_1000.csv';
                      'DRF-output_1_8_1000.csv';
                      'DRF-output_1_16_1000.csv';
                      'DRF-output_1_32_1000.csv'};

    drfw_compl_files = {'DRF-W-output_1_1_1000.csv';
                      'DRF-W-output_1_2_1000.csv';
                      'DRF-W-output_1_4_1000.csv';
                      'DRF-W-output_1_8_1000.csv';
                      'DRF-W-output_1_16_1000.csv';
                      'DRF-W-output_1_32_1000.csv'};  

    speedfair_compl_files = {'SpeedFair-output_1_1_1000.csv';
                      'SpeedFair-output_1_2_1000.csv';
                      'SpeedFair-output_1_4_1000.csv';
                      'SpeedFair-output_1_8_1000.csv';
                      'SpeedFair-output_1_16_1000.csv';
                      'SpeedFair-output_1_32_1000.csv'};  

    strict_priority_compl_files = { 'Strict-output_1_1_1000.csv';
                      'Strict-output_1_2_1000.csv';
                      'Strict-output_1_4_1000.csv';
                      'Strict-output_1_8_1000.csv';
                      'Strict-output_1_16_1000.csv';
                      'Strict-output_1_32_1000.csv'};  
end


num_batch_queues = 1;
num_interactive_queue = 3;
num_queues = num_batch_queues + num_interactive_queue;
START_TIME = 0; END_TIME = 800;
is_printed = true;
cluster_size = 1000;

barColors = colorb1i3;

%%
output_folder = [result_folder 'output/'];

figIdx = 0;

% fig_path = 'figs\';
%%
% global batchJobRange
% batchJobRange = [1:10]

queues_len = length(queues);
plots  = [true, false];
improvements = zeros(queues_len, 4);
if plots(1) 
%    INTERACTIVE_QUEUE = 'interactive';
    INTERACTIVE_QUEUE = 'bursty';
   
   [ drf_avg_compl_time ] = obtain_compl_time( output_folder, drf_compl_files, INTERACTIVE_QUEUE);
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, INTERACTIVE_QUEUE);
   [ drfw_avg_compl_time ] = obtain_compl_time( output_folder, drfw_compl_files, INTERACTIVE_QUEUE);
   [ strict_priority_avg_compl_time ] = obtain_compl_time( output_folder, strict_priority_compl_files, INTERACTIVE_QUEUE);

   interactive_time = [drf_avg_compl_time ;  drfw_avg_compl_time; strict_priority_avg_compl_time; speedfair_avg_compl_time];
   improvements(:,1) = (interactive_time(1,:)-interactive_time(1,:))./interactive_time(1,:);
   improvements(:,2) = (interactive_time(2,:)-interactive_time(1,:))./interactive_time(1,:);
   improvements(:,3) = (interactive_time(3,:)-interactive_time(1,:))./interactive_time(1,:);
   improvements(:,4) = (interactive_time(4,:)-interactive_time(1,:))./interactive_time(1,:);
   improvements = improvements*100;
   
   figure;
   scrsz = get(groot,'ScreenSize');   
   bar(interactive_time', 'group');
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel='number of batch queues';
    yLabel='time (seconds)';
    legendStr={'DRF', 'DRF weight', 'strict priority', 'SpeedFair'};

    xLabels=queues;
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'interactive_compl_time';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end
   
   latencySpeedUpFactors = drf_avg_compl_time./speedfair_avg_compl_time
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
   xLabel='number of batch queues';
    yLabel='time (seconds)';
    legendStr={'DRF', 'DRF weight', 'strict priority', 'SpeedFair'};

    xLabels=queues;
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');    
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   % ylim([0 6]);   
   if is_printed    
       figIdx=figIdx +1;
      fileNames{figIdx} = 'batch_compl_time';      
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end

end


return;
%%

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ LOCAL_FIG fileName '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end