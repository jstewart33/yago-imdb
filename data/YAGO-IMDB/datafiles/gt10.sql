select * from ground_truth where yago_id like 'ym%' having rand() > 0.9 into outfile "/tmp/gt10_movies.txt";

select * from ground_truth where yago_id like 'yp%' having rand() > 0.9 into outfile "/tmp/gt10_people.txt";
