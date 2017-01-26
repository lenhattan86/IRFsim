function [ res ] = fipQueues( shapeRes1, num_interactive_queue, num_batch_queues)
%FIPQUEUES Summary of this function goes here
%   Detailed explanation goes here
res = zeros(size(shapeRes1));
num_queues = num_interactive_queue+num_batch_queues;
res(1:num_interactive_queue,:) = shapeRes1(num_batch_queues+1:num_queues,:);
res(num_interactive_queue+1:num_queues,:) = shapeRes1(1:num_batch_queues,:);
end

