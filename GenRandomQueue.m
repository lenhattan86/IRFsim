clear; close all;

lambda = 2;
numChanges = 500;
QUEUE_MAX = 10;
QUEUE_MIN = 1;
interval = 100*2;   

toWrite = zeros(1,numChanges);

traceNum = 2;

if traceNum==0
    temp = poissrnd(lambda,1,numChanges);
    QueueNum = round(temp/max(temp)*(QUEUE_MAX-QUEUE_MIN)) + QUEUE_MIN;
else
    if traceNum==1 % facebook
        cycle = [800 700 1100 1800 1100 1500 600 1050 900 700 800  1000 ...
                 400 700 1200 1600 1900 1200 2000 1100 800 600 900 400 ...
                 400 1000 900 800  1500 1100 1300 1000 800  1100 700 600 ...
                 500 1050 700 2000 1600 1100 1105 600  1000 800 900 600 ...
                 500 700 800  1000 1500 1250 1200 1900 1000 800 600 500 ...
                 400 600 2000 1500 1800 1000 1200 1000 800 1100 600 400 ...
                 300 800 800  600  1100 1100 800 900 600 1100 600 1100 ...
                 ];
    elseif traceNum==2 % CC
        cycle = [60 30 70 40 20 60 80 50 40 20 70 30 ...
                 80 30 40 75 40 55 55 90 90 30 30 70 ...
                 80 35 70 45 25 80 60 100 50 150 30 100 ...
                 80 40 70 100 60 50 80 70 80 60 30 50 ...
                 90 30 70 40 30 50 190 50 30 20 70 50 ...
                 80 30 50 90 50 40 70 80 90 50 20 60  ...
                 80 20 70 20 70 20 60 70 30 60 20 120  ...
                 ];
    end
    
    cycle = round(cycle/max(cycle)*(QUEUE_MAX));     
    t = 0:interval:interval*(length(cycle)-1);
    figure;
    plot(t,cycle,'x-');
    xlabel('time (secs)');
    ylabel('queue numbers');
    ylim([0 max(cycle)]);
    
    for j=1:length(cycle): numChanges
        startIdx = j;
        endIdx = min(j + length(cycle)-1,numChanges);
        temp = cycle(1:endIdx-startIdx+1);
        QueueNum(startIdx:endIdx) = temp;
    end
    
    QueueNum = round(QueueNum/max(QueueNum)*(QUEUE_MAX));
end

csvwrite('queue_nums.csv',QueueNum);

% fprintf('int[] QUEUE_NUM = {');
% for i=1:numChanges
%     fprintf('%d,',QueueNum);
% end
% fprintf('}');
interval = 100*2;
range = 100;
t = 0:interval:interval*(range-1);

figure;
plot(t,QueueNum(1:range));
ylim([0 max(QueueNum)]);
xlabel('time (secs)');
ylabel('queue numbers');