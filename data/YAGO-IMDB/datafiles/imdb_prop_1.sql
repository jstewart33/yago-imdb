use sigma;

select i.iid, i.prop, i.val from imdb_prop i, gt1_movies g
   where g.imdb_id = i.iid
   into outfile '/tmp/imdb_movies_prop_1.txt';

load data infile '/tmp/imdb_movies_prop_1.txt' into table imdb_prop_1;

select i.iid, i.prop, i.val from imdb_prop i, gt1_people g
   where g.imdb_id = i.iid 
   into outfile '/tmp/imdb_people_prop_1.txt';

load data infile '/tmp/imdb_people_prop_1.txt' into table imdb_prop_1;

select i.iid, i.prop, i.val from imdb_prop i, imdb_rel_1 r
   where i.iid = r.id1 and 
         i.iid not in (select imdb_id from gt1_movies) and
         i.iid not in (select imdb_id from gt1_people)
   into outfile '/tmp/imdb_other_prop1_1.txt';

select i.iid, i.prop, i.val from imdb_prop i, imdb_rel_1 r
   where i.iid = r.id2 and 
         i.iid not in (select imdb_id from gt1_movies) and
         i.iid not in (select imdb_id from gt1_people)
   into outfile '/tmp/imdb_other_prop2_1.txt';

select i.iid, i.prop, i.val from imdb_prop i
   where i.iid not in (select imdb_id from gt1_people)
   having rand() > 0.995
   into outfile '/tmp/imdb_nd_people_prop_1.txt';

select i.iid, i.prop, i.val from imdb_prop i
   where i.iid not in (select imdb_id from gt1_movies)
   having rand() > 0.995
   into outfile '/tmp/imdb_nd_movies_prop_1.txt';


