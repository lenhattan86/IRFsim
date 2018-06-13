% report = [cpu/mem, beta, gpu/mem] absolute value;
% cluster_size = [cpu gpu mem];
% sample input: report = [16/12 80 1/12; 16/12 112 1/12; 16/12 192 1/12; 16/12 288 1/12];
% cluster_size = [384 12 1152];
% [final_alloc,final_price] = AllocX(report,cluster_size)


function [final_alloc,final_price] = AllocX(report,cluster_size)
    count = 0;
    % data processing
    [n,~]=size(report);
    report(:,1) = report(:,1)* cluster_size(3)/cluster_size(1);
    report(:,2) = report(:,2)* cluster_size(2)/cluster_size(1);
    report(:,3) = report(:,3)* cluster_size(3)/cluster_size(2);
    ratio_cm = report(:,1);  % cpu/mem ratio;
    ratio_gm = report(:,3);  % gpu/mem ratio;
    beta = report(:,2);   

    %price for gpu
    pg = [0 sort(beta).'];
    flag = 1;
    
    % scanning the price gpu list
    while flag > 0 
        alloc = zeros(n,3);
        price = pg(flag);
        lb = [0 0];
        b = [1 1];
        x_up = zeros(1,n);
        x_lo = zeros(1,n);
        y_lo = zeros(1,n);
        y_up = zeros(1,n);
        value = zeros(1,n);
        payment = zeros(1,n);
        for i = 1:n
            A = [1 price;1/ratio_cm(i)*(1+price) 1/ratio_gm(i)*(1+price)];
            f = [-1 -beta(i)];
            % solving LP
            alloc(i,1:2) = linprog(f,A,b,[],[],lb,[],[0 0], optimoptions('linprog','Display','off'));
            alloc(i,3) = alloc(i,1)/ratio_cm(i) + alloc(i,2)/ratio_gm(i);
            payment(i) = max(alloc(i,1)+price*alloc(i,2),(1+price)*alloc(i,3));
            value(i) = alloc(i,1) + beta(i)*alloc(i,2);
            % working with cases with non-unique solution (degeneracy), possibly when
            % beta*r2=r1 or price = beta(i)
            if ratio_cm(i) == beta(i)*ratio_gm(i) || price == beta(i)
                f = [-1 0];
                A = [1 price;1/ratio_cm(i)*(1+price) 1/ratio_gm(i)*(1+price)];
                Aeq = [1 beta(i)];
                beq = value(i);
                count = count + 1;
                sol =  linprog(f,A,b,Aeq,beq,lb,[],[0 0],optimoptions('linprog','Display','off'));
                x_up(i) = sol(1) - alloc(i,1) ;  % bound on inceasement of cpu; (>0)
                y_lo(i) = -1*x_up(i)/beta(i); % bound on decrement of gpu; (<0)
                f = [1 0];
                count = count + 1;
                sol =  linprog(f,A,b,Aeq,beq,lb,[],[0 0],optimoptions('linprog','Display','off'));
                x_lo(i) = sol(1) - alloc(i,1);   %(<0)
                y_up(i) = -1* x_lo(i)/beta(i);  %(>0)                               
            end
        end   
            % calculating upper load and lower load of cpu
            cur_load = sum(alloc);
            if (cur_load(3)>max(cur_load(1)+cur_load(2)))  %mem bottleneck
                budget = cur_load(3);
                final_price = [1 price 1+price]/budget;                
                break
            end
            load_up = [cur_load(1)+sum(x_up) cur_load(2)+sum(y_lo)];
            load_lo = [cur_load(1)+sum(x_lo) cur_load(2)+sum(y_up)];
        if load_up(1) < load_up(2)  % for the best case cpu load is still lower
            flag = flag+1;
            continue
        else %optimal price between some beta
            for k = pg(flag-1):0.01:pg(flag)
                alloc = zeros(n,3);
                for i = 1:n
                    A = [1 k;1/ratio_cm(i)*(1+k) 1/ratio_gm(i)*(1+k)];
                    f = [-1 -beta(i)];
                    % solving LP
                    count = count + 1;
                    alloc(i,1:2) = linprog(f,A,b,[],[],lb,[],[0 0],optimoptions('linprog','Display','off'));
                    alloc(i,3) = alloc(i,1)/ratio_cm(i) + alloc(i,2)/ratio_gm(i);
                end
                cur_load = sum(alloc);
                if abs(cur_load(1)-cur_load(2))<0.001
                    budget = max(cur_load(1),cur_load(3));
                    final_price = [1 k 1+k]/budget;  
                    flag = 0;
                    break
                end
            end
            if flag
              [~, order] = sort(beta);
              i = order(flag-1);
              cur_load = cur_load - alloc(i,:);
              f = [0 -1];
              A = [1 beta(i);1/ratio_cm(i)*(1+beta(i)) 1/ratio_gm(i)*(1+beta(i))];
              b = [1 1];
              Aeq = [1 beta(i); 1 -1];
              beq = [value(i) cur_load(2)-cur_load(1)];
              size(beq)
              lb = [0 0];
              count = count + 1;
              alloc(i,1:2) =  linprog(f,A,b,Aeq,beq,lb,[],[0 0],optimoptions('linprog','Display','off'));  %alloc(i,1:2)
              alloc(i,3) = alloc(i,1)/ratio_cm(i) + alloc(i,2)/ratio_gm(i); 
              cur_load = cur_load + alloc(i,:);
              budget = max(cur_load(1),cur_load(3));
              final_price = [1 beta(i) 1+beta(i)]* budget;  
              flag = 0;
            end
        end
        
    end
    unit = ones(length(report), 1);
    final_alloc = (alloc/budget).* (unit * cluster_size); 
    count
end