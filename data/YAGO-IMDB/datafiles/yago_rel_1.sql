use sigma;

select y.relation, y.id1, y.id2 from yago_rel y, gt1_movies g
   where g.yago_id = y.id1
   into outfile '/tmp/yago_rel_movies1_1.txt';

load data infile '/tmp/yago_rel_movies1_1.txt' into table yago_rel_1;

select y.relation, y.id1, y.id2 from yago_rel y, gt1_movies g
   where g.yago_id = y.id2
   into outfile '/tmp/yago_rel_movies2_1.txt';

load data infile '/tmp/yago_rel_movies2_1.txt' into table yago_rel_1;

select y.relation, y.id1, y.id2 from yago_rel y, gt1_people g
   where g.yago_id = y.id1
   into outfile '/tmp/yago_rel_people1_1.txt';

load data infile '/tmp/yago_rel_people1_1.txt' into table yago_rel_1;

select y.relation, y.id1, y.id2 from yago_rel y, gt1_people g 
   where g.yago_id = y.id2 
   into outfile '/tmp/yago_rel_people2_1.txt';

load data infile '/tmp/yago_rel_people2_1.txt' into table yago_rel_1;

select y.relation, y.id1, y.id2 from yago_rel y
   where y.id1 like 'yp%' and
         y.id1 not in (select yago_id from gt1_people)
   having rand() > 0.99
   into outfile '/tmp/yago_rel_other_people_1.txt';

load data infile '/tmp/yago_rel_other_people_1.txt' into table yago_other_rel_1;

select y.relation, y.id1, y.id2 from yago_rel y
   where y.id2 like 'ym%' and
         y.id2 not in (select yago_id from gt1_movies)
   having rand() > 0.99
   into outfile '/tmp/yago_rel_other_movies_1.txt';

load data infile '/tmp/yago_rel_other_movies_1.txt' into table yago_other_rel_1;
