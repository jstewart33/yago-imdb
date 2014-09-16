use sigma;
/*
select y.relation, y.id1, y.id2 from yago_rel_1 y, gt05_movies g
   where g.yago_id = y.id1
   into outfile '/tmp/yago_rel_movies1_05.txt';

load data infile '/tmp/yago_rel_movies1_05.txt' into table yago_rel_05;

select y.relation, y.id1, y.id2 from yago_rel_1 y, gt05_movies g
   where g.yago_id = y.id2
   into outfile '/tmp/yago_rel_movies2_05.txt';

load data infile '/tmp/yago_rel_movies2_05.txt' into table yago_rel_05;

select y.relation, y.id1, y.id2 from yago_rel_1 y, gt05_people g
   where g.yago_id = y.id1
   into outfile '/tmp/yago_rel_people1_05.txt';

load data infile '/tmp/yago_rel_people1_05.txt' into table yago_rel_05;

select y.relation, y.id1, y.id2 from yago_rel_1 y, gt05_people g 
   where g.yago_id = y.id2 
   into outfile '/tmp/yago_rel_people2_05.txt';

load data infile '/tmp/yago_rel_people2_05.txt' into table yago_rel_05;
*/
select y.relation, y.id1, y.id2 from yago_rel_1 y
   where y.id1 like 'yp%' and
         y.id1 not in (select yago_id from gt05_people)
   having rand() > 0.5
   into outfile '/tmp/yago_rel_other_people_05.txt';

load data infile '/tmp/yago_rel_other_people_05.txt' into table yago_other_rel_05;

select y.relation, y.id1, y.id2 from yago_rel_1 y
   where y.id2 like 'ym%' and
         y.id2 not in (select yago_id from gt05_movies)
   having rand() > 0.5
   into outfile '/tmp/yago_rel_other_movies_05.txt';

load data infile '/tmp/yago_rel_other_movies_05.txt' into table yago_other_rel_05;
