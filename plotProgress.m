addpath('matlab_func');
common_settings;
% is_printed = 1;
%%
queue_num = 3;
figureSize = figSizeOneCol/3*2;

plots  = [true, true];

isLarge = false;
largeStr = '';
if isLarge
  cluster_size = 600;
  queue_num = 30;
  END_TIME = 50;
%   largeStr='_lbeta_cpu';
%   largeStr='_lbeta_mix';
%   largeStr='_sbeta_cpu';
%   largeStr='_sbeta_mix';
%   largeStr='_mbeta_cpu';
%   largeStr='_mbeta_mix';
  largeStr='_mbeta_lcpu';
%   largeStr='';
  plots(1:2:3)=false;
else
  cluster_size = 600;
  END_TIME = 150;
  largeStr='';
  queue_num=3;
end

outputExtra = '';
colorUsers = {colorUser1; colorUser2; colorUser3};
%%
% methods = {strES, strDRF, strFDRF, strMP, strMSR, strPricing};
methods = { strDRF, strES, strAlloX};
files = { ['DRF-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
          ['ES-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];                
          ['AlloX-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv']};

num_batch_queues = 1;
num_interactive_queue = 3;
num_queues = num_batch_queues + num_interactive_queue;
%%
result_folder = '';
output_folder = [result_folder 'output/'];

figIdx = 0;
% fig_path = 'figs\';
%%x
% global batchJobRange
% batchJobRange = [1:10]




QUEUES = {'queue0', 'queue1', 'queue2'};

%%
if plots(1) 
    
    jobCompleted = zeros(length(QUEUES), length(methods));
    for i=1:length(QUEUES)
      [ jobCompleted(i,:) ] = obtain_job_completed( output_folder, files, QUEUES{i});
    end
    (jobCompleted(1,:)-jobCompleted(1,1))/jobCompleted(1,1)*100
    (jobCompleted(1,:)-jobCompleted(1,2))/jobCompleted(1,2)*100
   figIdx=figIdx +1;         figures{figIdx} =figure;
   scrsz = get(groot,'ScreenSize');   
   hBar = bar(jobCompleted', 'group');
   set(hBar,{'FaceColor'}, colorUsers);   
%    set(gca,'yscale','log')
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel=strMethods;
    yLabel=strJobCompleted;
    legendStr={strUser1, strUser2, strUser3, strUser4};
    xlim([0.5 3.5]);
    ylim([0 max(max(jobCompleted))*1.1]);
    xLabels=methods;
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   

      fileNames{figIdx} = 'job_completed_each';
end

%%
if plots(2) 
    queueName = 'queue';
   
   [ jobCompleted ] = obtain_job_completed( output_folder, files, queueName);

   figIdx=figIdx +1;         figures{figIdx} =figure;
   scrsz = get(groot,'ScreenSize');   
   yVals = jobCompleted./(jobCompleted(1));
   bar(yVals,barSize);
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel=strMethods;
    yLabel=strJobCompleted;
%     legendStr={'ES', 'DRF', 'MaxMin', 'SpeedUp', 'Pricing'};
  xlim([0.5 3.5]);
  ylim ([0 max(yVals*1.1)]);
    xLabels=methods;
%     legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   

    fileNames{figIdx} = 'job_completed_all';
end

return;
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);
    
    pdfFile = [ fig_path fileName largeStr '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end