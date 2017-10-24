addpath('matlab_func');
common_settings;

%%
queue_num = 4;


plots  = [true];

cluster_size = 100;
END_TIME = 10;
largeStr='_analysis_';
queue_num=4;


outputExtra = '';
colorUsers = {colorUser1; colorUser2; colorUser3; colorUser4};
result_folder = '';
output_folder = [result_folder 'output/'];

%%
betas =[0.5 1.0 5 25.0 50.0 100.0];
methods = {strES, strDRF, strFDRF, strMP, strMSR, strPricing};
QUEUES = {'queue0', 'queue1', 'queue2', 'queue3'};

  jobCompleted = ones(length(betas), length(methods));
  performanceGains = ones(length(betas),  length(methods));
  for b=1:length(betas)    
    str = num2str(betas(b));
    if(betas(b)>=1)
      str = [num2str(betas(b)) '.0'];
    end
    largeStr= ['_analysis_' str];
    files = {
          ['ES-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
          ['DRF-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
          ['FDRF-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
          ['MaxMinMem-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
          ['SpeedUp-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv'];
          ['Pricing-output_' num2str(queue_num) '_' num2str(cluster_size) largeStr '.csv']
           };
    i = 1;
    
    [ jobCompleted(b,:) ] = obtain_job_completed( output_folder, files, QUEUES{1});     
    
  end
  for b=2:length(betas)  
    for imethod=1:length(methods)
        performanceGains(b, imethod) = (jobCompleted(b, imethod)-jobCompleted(b, 1))/jobCompleted(b, 1)*100;
%         performanceGains(b, imethod) = (jobCompleted(b, imethod)-jobCompleted(1, imethod))/jobCompleted(1, imethod)*100;
    end
  end
%%    
if plots(1) 
   figure;
   scrsz = get(groot,'ScreenSize');   
%    hBar = bar(jobCompleted', 'group');
%   plot(betas,performanceGains(:,2:length(methods)));     
%    plot(betas,performanceGains(:,1), 'LineWidth',LineWidth);     
%    hold on;
%    plot(betas,performanceGains(:,2), 'LineWidth',LineWidth);     
%    hold on;
   
%    plot(betas,performanceGains(:,4), 'LineWidth',LineWidth);   
%    hold on;
   plot(betas,performanceGains(:,3), 'LineWidth',LineWidth);   
   hold on;
%    plot(betas,performanceGains(:,5), 'LineWidth',LineWidth);   
%    hold on;
   
   plot(betas,performanceGains(:,6), 'LineWidth',LineWidth);     
   
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   
   xLabel='speedup rate \beta';
    yLabel='performance gain (%)';
%     legendStr={'DRF','FDRF','MP','MSR','Pricing'};
 legendStr={'FDRF','Pricing'};
    
%     set(gca,'yscale','log')

    xLabels=methods;
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'performance_gain';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end  
end
%%
for i=1:length(fileNames)
    fileName = fileNames{i}
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName '.pdf']    
%     pdfFile = [ LOCAL_FIG fileName '.pdf']    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end