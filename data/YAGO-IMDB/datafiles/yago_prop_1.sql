use sigma;

select y.yid, y.prop, y.val from yago_prop y, gt1_movies g
   where g.yago_id = y.yid
   into outfile '/tmp/yago_movies_prop_1.txt';

load data infile '/tmp/yago_movies_prop_1.txt' into table yago_prop_1;

select y.yid, y.prop, y.val from yago_prop y, gt1_people g
   where g.yago_id = y.yid 
   into outfile '/tmp/yago_people_prop_1.txt';

load data infile '/tmp/yago_people_prop_1.txt' into table yago_prop_1;

select y.yid, y.prop, y.val from yago_prop y, yago_rel_1 r
   where y.yid = r.id1 and 
         y.yid not in (select yago_id from gt1_movies) and
         y.yid not in (select yago_id from gt1_people)
   into outfile '/tmp/yago_other_prop1_1.txt';

select y.yid, y.prop, y.val from yago_prop y, yago_rel_1 r
   where y.yid = r.id2 and 
         y.yid not in (select yago_id from gt1_movies) and
         y.yid not in (select yago_id from gt1_people)
   into outfile '/tmp/yago_other_prop2_1.txt';

select y.yid, y.prop, y.val from yago_prop y
   where y.yid not in (select yago_id from gt1_people)
   having rand() > 0.995
   into outfile '/tmp/yago_nd_people_prop_1.txt';

select y.yid, y.prop, y.val from yago_prop y
   where y.yid not in (select yago_id from gt1_movies)
   having rand() > 0.995
   into outfile '/tmp/yago_nd_movies_prop_1.txt';


