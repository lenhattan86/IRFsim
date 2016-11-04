clear; close all;

lambda = 2;
numChanges = 1000;
QUEUE_MAX = 10;
QUEUE_MIN = 1;
interval = 100*2;   

toWrite = zeros(1,numChanges);

if false
    temp = poissrnd(lambda,1,numChanges);
    QueueNum = round(temp/max(temp)*(QUEUE_MAX-QUEUE_MIN)) + QUEUE_MIN;
else
    cycle = [800 700 1100 1800 1100 1500 600 1050 700  1000 ...
             500 800 1200 1900 1300 2000 1100 800 900  600 ...
             500 800 1200 1500 1300 1200 1000 800 1000 600 ...
             500 1050 900 2000 1100 1105 600  1000 800 600 ...
             500 700 1000 1700 1250 1200 1900 1000 800 500 ...
             400 600 2000 1500 1800 1000 1200 1000 700 450 ...
             400 800 800 1100 1100 800 900 1100 900 1100 ...
             ];
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