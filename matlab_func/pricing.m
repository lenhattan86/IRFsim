% report = [cpugpu beta memory];
% example
% an example with randomized N jobs


function [finalalloc,price] = pricing(report)
[n,~]=size(report);

%sort based on \beta ascendingly
[sortrep,index] = sortrows(report,2);
beta = sortrep(:,2);
ratio = sortrep(:,3)./sortrep(:,1);


% initialization
% price = [cpu_price gpu_price memory_price]; cur_load = [cpu gpu memory];
price = [1 beta(n) 1+beta(n)];
% initial allocation, 1 to (n-1) goes to CPU, n to gpu
user_alloc = userallocGPU(beta,ratio,price);
cur_load = sum(user_alloc);
gpumin =n;
flag = 1;

if cur_load(2) > cur_load(1)
    finalalloc = user_alloc;
    finalalloc(n,1) = 0;
    finalalloc(n,2) = 0;
    cur_load = sum(finalalloc);   % x+beta y = z/ri; cur_load(1)+x = cur_load(2)+ y;
    finalalloc(n,2) = (cur_load(1)-cur_load(2) + finalalloc(n,3)/ratio(n))/(1+beta(n));
    finalalloc(n,1) = cur_load(2)+ finalalloc(n,2) - cur_load(1);
    
    cur_load = sum(finalalloc);
end


    while abs(cur_load(1)-cur_load(2))>10^(-5) && flag == 1  %not equal
            gpumin = gpumin -1;
            price = [1 beta(gpumin) 1+beta(gpumin)];   
            user_alloc = useralloc(beta,ratio,price);  % 1 to gpumin on cpu
            cur_load = sum(user_alloc);
            if cur_load(1) > cur_load(2)           
                user_allocg = userallocGPU(beta,ratio,price);  % 1 to gpumin-1 on cpu
                cur_loadg = sum(user_allocg);
                if cur_loadg(1) > cur_loadg(2) % cpu is higher even if we put job gpumin to gpu
                    continue
                else    % somehow balance job gpumin  
                       finalalloc = user_allocg;
                       finalalloc(gpumin,1) = 0;
                       finalalloc(gpumin,2) = 0; 
                       cur_loadg = sum(finalalloc); 
                       finalalloc(gpumin,2) = (cur_loadg(1)-cur_loadg(2) + finalalloc(gpumin,3)/ratio(gpumin))/(1+beta(gpumin));
                       finalalloc(gpumin,1) = cur_loadg(2)+ finalalloc(gpumin,2) - cur_loadg(1);
                       break                    
                end
            else   % gradually decrease price_g, bisearch would do better
                for k = beta(gpumin+1):-0.00001: beta(gpumin)
                    price = [1 k k+1];
                    user_alloc = userallocGPU(beta,ratio,price);
                    cur_load = sum(user_alloc);
                    if abs(cur_load(1)-cur_load(2))<10^(-5)
                        finalalloc = user_alloc;
                        flag = 0;
                        break
                    end  
                end             
            end

    end

 temp = finalalloc;
 finalalloc = zeros(n,3);
 for w = 1:n
     finalalloc(index(w),:) = temp(w,:);
 end
 budget = max(sum(finalalloc));
 price = price * budget;
 finalalloc = finalalloc/budget;
    
        
        
        
        
        
    function [useralloc] = userallocGPU( beta, ratio, current_price)  %allocation calculation
        useralloc = zeros(n,3);
        for j = 1:n
        useralloc(j,3) = min(1/current_price(3),max(ratio(j),beta(j)*ratio(j)/current_price(2)))
            if beta(j) < current_price(2)  
                useralloc(j,1) = useralloc(j,3)/ratio(j);
                useralloc(j,2) = 0;
            else  % if beta = price, then put in GPU
                useralloc(j,1) = 0;
                useralloc(j,2) = useralloc(j,3)/(ratio(j)*beta(j));
            end 
        end
    end
    



    function [useralloc] = useralloc( beta, ratio, current_price)  %allocation calculation
        useralloc = zeros(n,3);
        for j = 1:n
        useralloc(j,3) = min(1/current_price(3),max(ratio(j),beta(j)*ratio(j)/current_price(2)));
            if beta(j) <= current_price(2)  % if beta = price, then put in CPU
                useralloc(j,1) = useralloc(j,3)/ratio(j);
                useralloc(j,2) = 0;
            else  
                useralloc(j,1) = 0;
                useralloc(j,2) = useralloc(j,3)/(ratio(j)*beta(j));
            end 
        end
    end



end
