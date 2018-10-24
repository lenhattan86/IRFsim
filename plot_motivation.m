addpath('matlab_func');
common_settings;
barWidth = 0.5;
figureSize = figSizeOneCol;

plots  = [1, 1, 1, 1, 1, 1, 1 ];

machines = {'G1','G2','C1', 'C2'};
enableSeparateLegend = true;

textColor = 'white';
strJob = 'J';
fontWeight = 'bold';
fontText = 12;
leftMargin = 1;

%%
jobs=[40, 50; 40, 50; 40, 80; 40, 80];
xMax = 100;
if plots(1)    
    temp = [0 0 figureSize(3) figureSize(4)*3/5];
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   
   scrsz = get(groot, 'ScreenSize');
    hold on   
    % GPU 1
    node = 1;
    jobLength = jobs(1,1);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob1);
    text(jobStart+jobLength/2, node,[strJob '1'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    % GPU 2
%     h(2)= fill([jobs(2,1) jobs(2,1) 0 0],[1.5 2.5 2.5 1.5],colorJob2);
     node = 2;
    jobLength = jobs(2,1);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob2);
    text(jobStart+jobLength/2, node,[strJob '2'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    % CPU 1
%     h(3)= fill([jobs(3,2) jobs(3,2) 0 0],[2.5 3.5 3.5 2.5],colorJob3);
    node = 3;
    jobLength = jobs(3,2);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob3);
    text(jobStart+jobLength/2, node,[strJob '3'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    % CPU 2
%     h(4)= fill([jobs(4,2) jobs(4,2) 0 0],[3.5 4.5 4.5 3.5],colorJob4);
    node = 4;
    jobLength = jobs(4,2);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob4);
    text(jobStart+jobLength/2, node,[strJob '4'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    if ~enableSeparateLegend
        legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','northoutside' ...
            ,'Orientation', 'horizontal','FontSize', fontLegend)
    end
    
    hold off
    xlim([0 xMax]);
    ylim([0.5 4.5]);
    xlabel('machines');
    xlabel('time');
    
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis,'XGrid','on');
    fileNames{figIdx} = 'JSQ_ex';
end

if plots(1)    
    temp = [0 0 figureSize(3) figureSize(4)*3/5];
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   
   scrsz = get(groot, 'ScreenSize');     
    hold on   
%     h(1)= fill([jobs(3,1) jobs(3,1) 0 0],[0.5 1.5 1.5 0.5],colorJob3);
     node = 1;
    jobLength = jobs(1,1);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob3);
    text(jobStart+jobLength/2, node,[strJob '3'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
%     h(2)= fill([jobs(4,1) jobs(4,1) 0 0],[1.5 2.5 2.5 1.5],colorJob4);
     node = 2;
    jobLength = jobs(4,1);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob4);
    text(jobStart+jobLength/2, node,[strJob '4'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
%     h(3)= fill([jobs(1,2) jobs(1,2) 0 0],[2.5 3.5 3.5 2.5],colorJob1);
    node = 3;
    jobLength = jobs(1,2);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob1);
    text(jobStart+jobLength/2, node,[strJob '1'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
%     h(4)= fill([jobs(2,2) jobs(2,2) 0 0],[3.5 4.5 4.5 3.5],colorJob2);
     node = 4;
    jobLength = jobs(2,2);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob2);
    text(jobStart+jobLength/2, node,[strJob '2'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    if ~enableSeparateLegend
        legend([h(3) h(4) h(1) h(2)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','northoutside' ...
            ,'Orientation', 'horizontal','FontSize', fontLegend)
    end
    
    hold off
    xlim([0 xMax]);
    ylim([0.5 4.5]);
    xlabel('machines');
    xlabel('time');
    
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis,'XGrid','on');
    fileNames{figIdx} = 'JSQ_ex_opt';
end

if plots(1)
    if enableSeparateLegend && false
      figIdx=figIdx +1;
      figures{figIdx} = figure; 
        hold on   
        h(1)= fill([jobs(1,1) jobs(1,1) 0 0],[0.5 1.5 1.5 0.5],colorJob1);
        h(2)= fill([jobs(2,1) jobs(2,1) 0 0],[1.5 2.5 2.5 1.5],colorJob2);
        h(3)= fill([jobs(3,2) jobs(3,2) 0 0],[2.5 3.5 3.5 2.5],colorJob3);
        h(4)= fill([jobs(4,2) jobs(4,2) 0 0],[3.5 4.5 4.5 3.5],colorJob4);

        legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','southoutside' ...
            ,'Orientation', 'horizontal','FontSize', fontLegend)
        hold off
    
      set(gca,'FontSize',fontSize);
      axis([20000,20001,20000,20001]) %move dummy points out of view
      axis off %hide axis  
      set(gca,'YColor','none');
      set (gcf, 'Units', 'Inches', 'Position', legendSize, 'PaperUnits', 'inches', 'PaperPosition', legendSize);    
      fileNames{figIdx} = 'JSQ_ex_legend';      
   end
end

%%
xMax = 100;
jobs=[10, 20; 10, 20; 20, 90; 20, 90];
if plots(2)    
   figIdx=figIdx +1;    
   temp = [0 0 figureSize(3) figureSize(4)*3/5];
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
%         h(1)= fill([jobs(1,1) jobs(1,1) 0 0],[0.5 1.5 1.5 0.5],colorJob1);
        node =1;
        jobLength = jobs(1,1);
        jobStart = 0;
        jobComplt = jobStart + jobLength;
        h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob1);
        text(jobStart+jobLength/2, node,[strJob '1'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
%         h(2)= fill([jobs(2,1) jobs(2,1) 0 0],[1.5 2.5 2.5 1.5],colorJob2);
        node =2;
        jobLength = jobs(2,1);
        jobStart = 0;
        jobComplt = jobStart + jobLength;
        h(2) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob2);
        text(jobStart+jobLength/2, node,[strJob '2'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
%         h(3)= fill([jobs(3,2) jobs(3,2) 0 0],[2.5 3.5 3.5 2.5],colorJob3);
        node =3;
        jobLength = jobs(3,2);
        jobStart = 0;
        jobComplt = jobStart + jobLength;
        h(3) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob3);
        text(jobStart+jobLength/2, node,[strJob '3'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
%         h(4)= fill([jobs(4,2) jobs(4,2) 0 0],[3.5 4.5 4.5 3.5],colorJob4);  
        node =4;
        jobLength = jobs(4,2);
        jobStart = 0;
        jobComplt = jobStart + jobLength;
        h(4) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob4);
        text(jobStart+jobLength/2, node,[strJob '4'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
        if ~enableSeparateLegend
            legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','northoutside' ...
            ,'Orientation', 'horizontal','FontSize', fontLegend)
        end
    hold off
     xlim([0 xMax]);
     ylim([0.5 4.5]);
     xlabel('machines');
     xlabel('time');
     
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis,'XGrid','on');
    fileNames{figIdx} = 'SJFplus_ex';
end

if plots(2)    
   figIdx=figIdx +1;    
   temp = [0 0 figureSize(3) figureSize(4)*3/5];
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
%         h(1)= fill([jobs(3,1) jobs(3,1) 0 0],[0.5 1.5 1.5 0.5],colorJob3);
        node =1;
        jobLength = jobs(3,1);
        jobStart = 0;
        jobComplt = jobStart + jobLength;
        h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob3);
        text(jobStart+jobLength/2, node,[strJob '3'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
        
%         h(2)= fill([jobs(4,1) jobs(4,1) 0 0],[1.5 2.5 2.5 1.5],colorJob4);
        node =2;
        jobLength = jobs(4,1);
        jobStart = 0;
        jobComplt = jobStart + jobLength;
        h(2) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob4);
        text(jobStart+jobLength/2, node,[strJob '4'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
        
%         h(3)= fill([jobs(1,2) jobs(1,2) 0 0],[2.5 3.5 3.5 2.5],colorJob1);
        node =3;
        jobLength = jobs(1,2);
        jobStart = 0;
        jobComplt = jobStart + jobLength;
        h(3) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob1);
        text(jobStart+jobLength/2, node,[strJob '1'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
        
%         h(4)= fill([jobs(2,2) jobs(2,2) 0 0],[3.5 4.5 4.5 3.5],colorJob2);        
        node =4;
        jobLength = jobs(2,2);
        jobStart = 0;
        jobComplt = jobStart + jobLength;
        h(4) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob2);
        text(jobStart+jobLength/2, node,[strJob '2'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
        
        if ~enableSeparateLegend
            legend([h(3) h(4) h(1) h(2)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','northoutside' ...
                ,'Orientation', 'horizontal','FontSize', fontLegend)
        end
    hold off
     xlim([0 xMax]);
     ylim([0.5 4.5]);
     xlabel('machines');
     xlabel('time');
     
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis,'XGrid','on');
    fileNames{figIdx} = 'SJFplus_ex_opt';
end

if plots(2)
    if enableSeparateLegend  && false
      figIdx=figIdx +1;
      figures{figIdx} = figure; 
        hold on   
        h(1)= fill([jobs(1,1) jobs(1,1) 0 0],[0.5 1.5 1.5 0.5],colorJob1);
        h(2)= fill([jobs(2,1) jobs(2,1) 0 0],[1.5 2.5 2.5 1.5],colorJob2);
        h(3)= fill([jobs(3,2) jobs(3,2) 0 0],[2.5 3.5 3.5 2.5],colorJob3);
        h(4)= fill([jobs(4,2) jobs(4,2) 0 0],[3.5 4.5 4.5 3.5],colorJob4);

        legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','southoutside' ...
            ,'Orientation', 'horizontal','FontSize', fontLegend)
        hold off
    
      set(gca,'FontSize',fontSize);
      axis([20000,20001,20000,20001]) %move dummy points out of view
      axis off %hide axis  
      set(gca,'YColor','none');
      set (gcf, 'Units', 'Inches', 'Position', legendSize, 'PaperUnits', 'inches', 'PaperPosition', legendSize);    
      fileNames{figIdx} = 'SJFplus_ex_legend';      
   end
end

%%
if plots(3)    
   figIdx=figIdx +1;    
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([3 3 0 0],[0.5 1.5 1.5 0.5],colorJob1);
       h(2)= fill([7 7 3 3],[0.5 1.5 1.5 0.5],colorJob2);
        h(3)= fill([12 12 7 7],[0.5 1.5 1.5 0.5],colorJob3);
        
       % legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','southeast')
    hold off
     xlim([0 13]);
     ylim([0.5 1.5]);
     temp = [0 0 figureSize(3) figureSize(4)*1.3/5];
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);    
    xlabel('time','FontSize',fontAxis);
   set(gca,'XGrid','on');
   
   set(gca,'YTickLabel',[]);
   xticks([0 3 7 12]);
    fileNames{figIdx} = 'alg_ex1';
end

if plots(4)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([3 3 0 0],[0.5 3.5 3.5 0.5]-0.5,colorJob1);
       h(2)= fill([7 7 3 3],[0.5 2.5 2.5 0.5]-0.5,colorJob2);
        h(3)= fill([12 12 7 7],[0.5 1.5 1.5 0.5]-0.5,colorJob3);
        
     %   legend([h(1) h(2) h(3) h(4)],{'Job 1', 'Job 2','Job 3','Job 4'},'Location','southeast')
    hold off
     xlim([0 13]);
      xticks([0 3 7 12]);
     ylim([0 3]);
     temp = [0 0 figureSize(3) figureSize(4)*2.6/5];
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
    xlabel('time','FontSize',fontAxis);
    set(gca,'XGrid','on');
    % set(gca,'YTickLabel', machines,'FontSize',fontAxis,'XGrid','on');
    fileNames{figIdx} = 'alg_ex2';
end
%%
if plots(5)    
       temp = [0 0 figureSize(3) figureSize(4)*1.9/5];
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
%         h(1)= fill([4 4 0 0],[0.5 1.5 1.5 0.5],colorJob2);
    node = 1;
    jobLength = 4;
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob2);
    text(jobStart+jobLength/2, node,[strJob '2'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
%       h(2)= fill([9 9 4 4],[0.5 1.5 1.5 0.5],colorJob3);
      node = 1;
    jobLength = 5;
    jobStart = 4;
    jobComplt = jobStart + jobLength;
    h(2) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob3);
    text(jobStart+jobLength/2, node,[strJob '3'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
%         h(3)= fill([4 4 0 0],[1.5 2.5 2.5 1.5],colorJob1);
    node = 2;
    jobLength = 4;
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(3) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorJob1);
    text(jobStart+jobLength/2, node,[strJob '1'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
        
%     legend([h(1) h(2) h(3)],{'Job 2', 'Job 3','Job 1'},'Location','northoutside' ...
%             ,'Orientation', 'horizontal','FontSize', fontLegend)
    hold off
    xlabel('time','FontSize',fontAxis);
     xlim([0 10]);
     ylim([0.5 2.5]);
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'YTickLabel', {'G','C'},'FontSize',fontAxis,'XGrid','on');
    fileNames{figIdx} = 'FS_ex';
end
%%
if plots(6)   
    temp = [0 0 figureSize(3) figureSize(4)*1.4/5];
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');      
    hold on   
        h(1)= fill([10 10 0 0],[0.5 1.5 1.5 0.5],colorJob1);
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

%% simple example in introduction.
user1_jobs=[10, 15; 10, 50; 10, 15];
user2_jobs=[8,  10;  5, 75; 10, 15];
xMax = 80;
if plots(7)    
   temp = [0 0 figureSize(3) figureSize(4)*3/5];
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   
    scrsz = get(groot, 'ScreenSize');
    hold on   
    
    % GPU 1
    node = 1;
    jobLength = user1_jobs(1,1);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser1);
    text(jobStart+jobLength/2, node,[strJob '1'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    jobLength = user1_jobs(3,1);
    jobStart = user1_jobs(1,1);
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser1);
    text(jobStart+jobLength/2, node,[strJob '5'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    % GPU 2
    node =2;
    jobLength = user2_jobs(1,1);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser2);
    text(jobStart+jobLength/2, node,[strJob '2'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
    jobLength = user2_jobs(3,1);
    jobStart = user2_jobs(1,1);
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser2);
    text(jobStart+jobLength/2, node,[strJob '6'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
    % CPU 1
    node =3;
    jobLength = user1_jobs(2,2);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser1);
    text(jobStart+jobLength/2, node,[strJob '3'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
    % CPU 2    
    node =4;
    jobLength = user2_jobs(2,2);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser2);
    text(jobStart+jobLength/2, node,[strJob '4'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
    
    if ~enableSeparateLegend
        legend([h(1) h(2)],{'User 1', 'User 2'},'Location','northoutside' ...
            ,'Orientation', 'horizontal','FontSize', fontLegend)
    end
    
    hold off
    xlim([0 xMax]);
    ylim([0.5 4.5]);
    xlabel('machines');
    xlabel('time');
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis,'XGrid','on');
    fileNames{figIdx} = 'example_fair';
end

if plots(7)    
   temp = [0 0 figureSize(3) figureSize(4)*3/5];
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   
    scrsz = get(groot, 'ScreenSize');
    hold on   
    
    % GPU 1
    node = 1;
    jobLength = user2_jobs(2,1);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser2);
    text(jobStart+jobLength/2, node,[strJob '4'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    jobLength = user1_jobs(1,1);
    jobStart = user2_jobs(2,1);
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser1);
    text(jobStart+jobLength/2, node,[strJob '1'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');    
    
    % GPU 2
    node =2;
    jobLength = user2_jobs(1,1);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser2);
    text(jobStart+jobLength/2, node,[strJob '2'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
    jobLength = user1_jobs(2,1);
    jobStart = user2_jobs(1,1);
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser1);
    text(jobStart+jobLength/2, node,[strJob '3'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
    % CPU 1
    node =3;
    jobLength = user1_jobs(3,2);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser1);
    text(jobStart+jobLength/2, node,[strJob '5'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
    % CPU 2    
    node =4;
    jobLength = user2_jobs(3,2);
    jobStart = 0;
    jobComplt = jobStart + jobLength;
    h(1) = fill([jobComplt jobComplt jobStart jobStart],[node-0.5 node+0.5 node+0.5 node-0.5],colorUser2);
    text(jobStart+jobLength/2, node,[strJob '6'], 'FontSize', fontText, 'Color',textColor, 'FontWeight',fontWeight,'HorizontalAlignment','center');       
    
    
    if ~enableSeparateLegend
        legend([h(1) h(2)],{'User 1', 'User 2'},'Location','northoutside' ...
            ,'Orientation', 'horizontal','FontSize', fontLegend)
    end
    
    hold off
    xlim([0 xMax]);
    ylim([0.5 4.5]);
    xlabel('machines');
    xlabel('time');
    set (gcf, 'Units', 'Inches', 'Position', temp, 'PaperUnits', 'inches', 'PaperPosition', temp);
    set(gca,'YTickLabel', machines,'FontSize',fontAxis,'XGrid','on');
    fileNames{figIdx} = 'example_opt';
end

if plots(7)
    if enableSeparateLegend
      figIdx=figIdx +1;
      figures{figIdx} = figure; 
        hold on   
        h(1)= fill([user1_jobs(1,1) user1_jobs(1,1) 0 0],[0.5 1.5 1.5 0.5],colorUser1);
        h(2)= fill([user1_jobs(2,1) user1_jobs(1,1) 0 0],[1.5 2.5 2.5 1.5],colorUser2);
        
        legend([h(1) h(2)],{'User 1', 'User 2'},'Location','southoutside' ...
            ,'Orientation', 'horizontal','FontSize', fontLegend)
        hold off
    
      set(gca,'FontSize',fontSize);
      axis([20000,20001,20000,20001]) %move dummy points out of view
      axis off %hide axis  
      set(gca,'YColor','none');
      set (gcf, 'Units', 'Inches', 'Position', legendSize, 'PaperUnits', 'inches', 'PaperPosition', legendSize);    
      fileNames{figIdx} = 'example_legend';      
   end
end
%%
if ~is_printed    
    return;
else
    pause(1);
end

%% print out figures
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName '.pdf'] 
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
close all;