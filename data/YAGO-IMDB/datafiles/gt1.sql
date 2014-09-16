--
-- script to select 1% of the entities in the ground truth
--

select * from ground_truth 
   where yago_id like 'ym%' 
   having rand() > 0.99
   into outfile "/tmp/gt1_movies.txt";

load data infile '/tmp/gt1_movies.txt' into table gt1_movies;

select * from ground_truth 
   where yago_id like 'yp%' 
   having rand() > 0.99 
   into outfile "/tmp/gt1_people.txt";

load data infile '/tmp/gt1_people.txt' into table gt1_people;
