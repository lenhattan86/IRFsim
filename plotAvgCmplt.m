addpath('matlab_func');
common_settings;
% is_printed = 1;
%%
queue_num = 3;
figureSize = figSizeOneCol/3*2;

plots  = [true, true];

colorUsers = {colorUser1; colorUser2; colorUser3};
methods = { strDRF, strES, strAlloX};

%%
if plots(1) 
    
    avgCmplt = [315.0, 231.0, 136.5;
      207.0, 175.1, 118.4;
      217.5, 159.64, 90.415];
   figIdx=figIdx +1;         figures{figIdx} =figure;
   
   scrsz = get(groot,'ScreenSize');   
   hBar = bar(avgCmplt', 'group');
   set(hBar,{'FaceColor'}, colorUsers);   
%    set(gca,'yscale','log')
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
    xLabel=strMethods;
    yLabel=strAvgCmplt;
    legendStr=methods;
    xlim([0.5 3.5]);
    ylim([0 max(max(avgCmplt))*1.1]);
    xLabels={strUser1, strUser2, strUser3};
    legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
   

      fileNames{figIdx} = 'avgCmplt';
end

return;
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);
    
    pdfFile = [ fig_path fileName '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end