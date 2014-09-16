--
-- script to select half of the 1% of the entities in the ground truth
-- this is to collect the training data
--

select * from gt1_people 
   having rand() > 0.5 
   into outfile "/tmp/gt05_people.txt";

load data infile '/tmp/gt05_people.txt' into table gt05_people;

select * from gt1_movies 
   having rand() > 0.5 
   into outfile "/tmp/gt05_movies.txt";

load data infile '/tmp/gt05_movies.txt' into table gt05_movies;
