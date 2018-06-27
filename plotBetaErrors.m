addpath('matlab_func');
common_settings;
is_printed = 1;
%%
figureSize = figSizeOneCol/3*2;
plots  = [true];

%%
result_folder = '';
output_folder = [result_folder 'output/'];

figIdx = 0;

%%
if plots(1) 
    betaErr = [0 , 0.1, 0.2, 0.3, 0.4];
    queueName = 'queue';
    jobCompleted = [376, 373, 363, 344, 145, 57; ... % DRF
                    608, 609, 595, 569, 294, 266] % AlloX;
  

   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   
   scrsz = get(groot,'ScreenSize');   
   yVals = jobCompleted(2,(1:length(betaErr)))./(jobCompleted(2,1));
   plot(betaErr, yVals, 'LineWidth', LineWidth);
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel='estimation error';
   ylim([0 max(yVals)*1.1]);
    yLabel='normalized progress';
  set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
   

    fileNames{figIdx} = 'betarErr';
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