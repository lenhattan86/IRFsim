addpath('matlab_func');
common_settings;

workload='BB';
% workload='TPCDS';
% workload='TPCH';

fig_path = ['../EuroSys17/fig/'];

%%

% result_folder = ['result/20170127_multi/' workload '/']; 
result_folder = ''; 


%%
xlabels = {'LQ-0', 'LQ-1','LQ-2','TQ-0'}; 
queues = {'bursty0','bursty1','bursty2','batch0'};
% xlabels = {'LQ-0', 'LQ-1','LQ-2'};
% queues = {'bursty0','bursty1','bursty2'};
colorCellsExperiment = {colorDRF; colorStrict; colorProposed; colorhard};

if true  
  compl_files = {'DRF-output_3_1_1000.csv';
                 'Strict-output_3_1_1000.csv';
                 'SpeedFair_drf-output_3_1_1000.csv';
                 'Hard_drf-output_3_1_1000.csv';};  
               
  methods = {'DRF','SP','BPF', 'Hard'};
  extra='admit';
elseif false
  compl_files = {'DRF_Reject-output_3_1_1000.csv';
                  'Strict_Reject-output_3_1_1000.csv';
                  'SpeedFair-output_3_1_1000.csv';
                  'Hard-output_3_1_1000.csv';};  
                
  methods = {'DRF','SP','BPF', 'Hard'};
  extra='reject';
elseif false
  compl_files = {'DRF-output_3_1_1000.csv';
                 'Strict-output_3_1_1000.csv';
                 'SpeedFair_drf-output_3_1_1000.csv';
                 'Hard_drf-output_3_1_1000.csv';
                 'DRF_Reject-output_3_1_1000.csv';
                  'Strict_Reject-output_3_1_1000.csv';
                  'SpeedFair-output_3_1_1000.csv';
                  'Hard-output_3_1_1000.csv';};  
                
  methods = {'DRF','SP','BPF admit', 'Hard admit','DRF rej','SP rej','BPF', 'Hard'};
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
maxY = 900;
queues_len = length(queues);
plots  = [true, false];
improvements = zeros(queues_len, 4);
if plots(1) 
   
   [ avg_compl_times ] = obtain_compl_time_multiple( output_folder, compl_files, queues);
   
   figure;
   scrsz = get(groot,'ScreenSize');   
   barChart = bar(avg_compl_times, 'group');
   
   maxVal = max(max(avg_compl_times));
   
   for i=1:length(barChart)
       barChart(i).FaceColor = colorCellsExperiment{i};
   end
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
%    xLabel='number of batch queues';
    yLabel=strAvgComplTime;
    legendStr=methods;

    xLabels=xlabels;
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
%     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
    ylim([0 max(maxY,maxVal)]);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'avg_multi_queues';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end
   
end

return;
%%

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName extra '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end