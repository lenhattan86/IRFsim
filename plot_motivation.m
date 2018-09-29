addpath('matlab_func');
common_settings;
barWidth = 0.5;
figureSize = figSizeThreeFourth;
plots  = [0, 0, 0, 0, 0,true ];

machines = {'G1','G2','C3', 'C4'};
if plots(1)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([15 15 0 0],[0.5 1.5 1.5 0.5],colorUser1);
       h(2)= fill([15 15 0 0],[1.5 2.5 2.5 1.5],colorUser2);
        h(3)= fill([10 10 0 0],[2.5 3.5 3.5 2.5],colorUser3);
        h(4)= fill([10 10 0 0],[3.5 4.5 4.5 3.5],colorUser4);
        
        legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','northeast')
    hold off
     xlim([0 18]);
     ylim([0.5 4.5]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis);
    fileNames{figIdx} = 'JSQ_ex';
end

if plots(2)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([2 2 0 0],[0.5 1.5 1.5 0.5],colorUser1);
       h(2)= fill([2 2 0 0],[1.5 2.5 2.5 1.5],colorUser2);
        h(3)= fill([200 200 0 0],[2.5 3.5 3.5 2.5],colorUser3);
        h(4)= fill([200 200 0 0],[3.5 4.5 4.5 3.5],colorUser4);
        
        legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','southeast')
    hold off
     xlim([0 220]);
     ylim([0.5 4.5]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis);
    fileNames{figIdx} = 'SJFplus_ex';
end


if plots(3)    
   figIdx=figIdx +1;    
   temp = [0.0 0 15/4 0.4];
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([3 3 0 0],[0.5 1.5 1.5 0.5],colorUser1);
       h(2)= fill([7 7 3 3],[0.5 1.5 1.5 0.5],colorUser2);
        h(3)= fill([12 12 7 7],[0.5 1.5 1.5 0.5],colorUser3);
        
       % legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','southeast')
    hold off
     xlim([0 13]);
     ylim([0.5 1.5]);
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
%     xlabel(xLabel,'FontSize',fontAxis);
   % set(gca,'YTickLabel', 'G1','FontSize',fontAxis);
   set(gca,'YTickLabel',[]);
   xticks([0 3 7 12]);
    fileNames{figIdx} = 'alg_ex1';
end

if plots(4)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([3 3 0 0],[0.5 3.5 3.5 0.5]-0.5,colorUser1);
       h(2)= fill([7 7 3 3],[0.5 2.5 2.5 0.5]-0.5,colorUser2);
        h(3)= fill([12 12 7 7],[0.5 1.5 1.5 0.5]-0.5,colorUser3);
        
     %   legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','southeast')
    hold off
     xlim([0 13]);
      xticks([0 3 7 12]);
     ylim([0 3]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    % set(gca,'YTickLabel', machines,'FontSize',fontAxis);
    fileNames{figIdx} = 'alg_ex2';
end

if plots(5)    
       temp = [0.0 0 15/4 6/4];
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([4 4 0 0],[0.5 1.5 1.5 0.5],colorUser2);
       h(2)= fill([9 9 4 4],[0.5 1.5 1.5 0.5],colorUser3);
        h(3)= fill([4 4 0 0],[1.5 2.5 2.5 1.5],colorUser1);
     %   h(4)= fill([10 10 0 0],[3.5 4.5 4.5 3.5],colorUser4);
        
        legend([h(1) h(2) h(3)],{'Job 2', 'Job 3','Job 1'},'Location','northeast')
    hold off
     xlim([0 10]);
     ylim([0.5 2.5]);
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis);
    fileNames{figIdx} = 'FS_ex';
end

if plots(6)   
     temp = [0.0 0 15/4 6/4];
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([10 10 0 0],[0.5 1.5 1.5 0.5],colorUser1);
         h(2) = rectangle('Position',[10 0.5 5 1],'LineStyle',':');
         plot(4,0.5,'r*')
         plot(7,0.5,'r*')
         plot(10,0.5,'r*')
         plot(15,0.5,'r*')
         text(3.3,0.2,'$$a_{j}=4$$','Interpreter','latex')
          text(6.7,0.2,'$$T$$','Interpreter','latex')
          text(9.3,0.2,'$$\omega_{i} = 10$$','Interpreter','latex')
          text(14.3,0.2,'$$c_{j} = 15$$','Interpreter','latex')
         axis off
    hold off
     xlim([0 18]);
     ylim([-0.5 1.5]);
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', []);
    set(gca,'xtick',[]);
    fileNames{figIdx} = 'arrival_ex';
end





for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
return;



