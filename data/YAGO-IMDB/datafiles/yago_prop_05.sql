use sigma;

select y.yid, y.prop, y.val from yago_prop_1 y, gt05_movies g
   where g.yago_id = y.yid
   into outfile '/tmp/yago_movies_prop_05.txt';

load data infile '/tmp/yago_movies_prop_05.txt' into table yago_prop_05;

select y.yid, y.prop, y.val from yago_prop_1 y, gt05_people g
   where g.yago_id = y.yid
   into outfile '/tmp/yago_people_prop_05.txt';

load data infile '/tmp/yago_people_prop_05.txt' into table yago_prop_05;

select y.yid, y.prop, y.val from yago_prop_1 y, yago_rel_05 r
   where y.yid = r.id1 and 
         y.yid not in (select yago_id from gt05_movies) and
         y.yid not in (select yago_id from gt05_people)
   into outfile '/tmp/yago_other_prop1_05.txt';

select y.yid, y.prop, y.val from yago_prop_1 y, yago_rel_05 r
   where y.yid = r.id2 and 
         y.yid not in (select yago_id from gt05_movies) and
         y.yid not in (select yago_id from gt05_people)
   into outfile '/tmp/yago_other_prop2_05.txt';

select y.yid, y.prop, y.val from yago_prop_1 y
   where y.yid not in (select yago_id from gt05_people)
   having rand() > 0.5
   into outfile '/tmp/yago_nd_people_prop_05.txt';

select y.yid, y.prop, y.val from yago_prop_1 y
   where y.yid not in (select yago_id from gt05_movies)
   having rand() > 0.5
   into outfile '/tmp/yago_nd_movies_prop_05.txt';


