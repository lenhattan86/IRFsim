function [ job_completed complTimes] = obtain_job_completed( folder, files, QUEUE_NAME)

%     global batchJobRange

   num_files = length(files);
   job_completed = zeros(1,num_files);
   complTimes=0;
   for i=1:num_files
      filePath = [folder files{i}];
      job_completed(i) = 0;
      if exist(filePath, 'file')
         [JobId,startTime,endTime,duration,queueName] = import_compl_time(filePath);
         complTimes = [complTimes duration'];
         idxs = false(1,length(duration));
         for j=1:length(queueName)
            strTemp =  queueName{j};
            if length(QUEUE_NAME) <= length(strTemp)
               strTemp = strTemp(1:length(QUEUE_NAME)); 
               if strcmp(QUEUE_NAME, strTemp)
                  idxs(j) = true;
               end
            end
         end
         job_completed(i) = length(duration(idxs));         
      end
   end
    size(complTimes);
end

