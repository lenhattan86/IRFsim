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
  cluster_size = 60;
  END_TIME = 150;
  largeStr='';
  queue_num=3;
end

outputExtra = '';
colorUsers = {colorUser1; colorUser2; colorUser3};
methods = { strDRF, strES, strAlloX};
%%
result_folder = '';
output_folder = [result_folder 'output/'];

figIdx = 0;
QUEUES = {'queue0', 'queue1', 'queue2'};
users = {strUser1, strUser2, strUser3};

cluster_size = 60*[64 1 96];

allocationsAlloX = [ 3520, 0, 880 ;  320, 28.57, 217.14 ;  0, 31.43, 62.86];
allocationsDRF = [ 0, cluster_size(2)/3, 880 ;  0, cluster_size(2)/3, 217.14 ;  0, cluster_size(2)/3, 62.86];
allocationsES = [ 3520, 0, 880 ;  320, 28.57, 217.14 ;  0, 31.43, 62.86];

resArray = {strCPU, strGPU, strMemory};
%% compute budgets 
final_price = [1.0909    1.9091    3.0000];
% AlloX
budgets = ones(3,3);
budgets(3,:) =  max(allocationsAlloX(:,1)*final_price(1)/cluster_size(1) ...
  + allocationsAlloX(:,2)*final_price(2)/cluster_size(2), allocationsAlloX(:,3)*final_price(2)/cluster_size(3));
budgets(1,:) =  max(allocationsDRF(:,1)*final_price(1)/cluster_size(1) ...
  + allocationsDRF(:,2)*final_price(2)/cluster_size(2), allocationsDRF(:,3)*final_price(2)/cluster_size(3));
budgets(2,:) =  max(allocationsES(:,1)*final_price(1)/cluster_size(1) ...
  + allocationsES(:,2)*final_price(2)/cluster_size(2), allocationsES(:,3)*final_price(2)/cluster_size(3));
% budgets = [ 1 1 1; 0.3 0.3 0.3; 1 1 1];
%%
if plots(1) 
   figIdx=figIdx +1;         figures{figIdx} =figure;
   yValues = budgets';
   hBar = bar(yValues, 'group');
   set(hBar,{'FaceColor'}, colorUsers);   

   %    set(gca,'yscale','log')
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
%    xLabel=strUser;
    yLabel=strBudget;
    legendStr={strUser1, strUser2, strUser3, strUser4};
    xlim([0.5 3.5]);
    ylim([0 max(max(yValues))*1.1]);
    xLabels=users;
    legend(methods,'Location', 'northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   
    fileNames{figIdx} = 'fair_budget';
end
%% prices

if plots(2) 
   figIdx=figIdx +1;         figures{figIdx} =figure;
   yValues = final_price;
   hBar = bar(yValues, 0.2);

    yLabel='norm. prices';
    xlim([0.5 3.5]);
    ylim([0 max(max(yValues))*1.1]);    
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',resArray,'FontSize',fontAxis);
   
    fileNames{figIdx} = 'prices';
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