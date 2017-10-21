function [ avgResUsage ] = convertToAvgUsage( res1, num_queues, num_time_steps, startIdx)
%CONVERTTOAVGUSAGE Summary of this function goes here
%   Detailed explanation goes here

    resAll = zeros(1,num_queues*num_time_steps);
    res = res1(startIdx:length(res1));
    if(length(resAll)>length(res))
       resAll(1:length(res)) = res;
    else
       resAll = res(1:num_queues*num_time_steps);
    end
    shapeRes = reshape(resAll,num_queues,num_time_steps);
    shapeRes = fipQueues( shapeRes, 0, num_queues);    
    avgResUsage = mean(shapeRes');
    
end

