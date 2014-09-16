use sigma;

select i.iid, i.prop, i.val from imdb_prop_1 i, gt05_movies g
   where g.imdb_id = i.iid
   into outfile '/tmp/imdb_movies_prop_05.txt';

load data infile '/tmp/imdb_movies_prop_05.txt' into table imdb_prop_05;

select i.iid, i.prop, i.val from imdb_prop_1 i, gt05_people g
   where g.imdb_id = i.iid 
   into outfile '/tmp/imdb_people_prop_05.txt';

load data infile '/tmp/imdb_people_prop_05.txt' into table imdb_prop_05;

select i.iid, i.prop, i.val from imdb_prop_1 i, imdb_rel_05 r
   where i.iid = r.id1 and 
         i.iid not in (select imdb_id from gt05_movies) and
         i.iid not in (select imdb_id from gt05_people)
   into outfile '/tmp/imdb_other_prop1_05.txt';

select i.iid, i.prop, i.val from imdb_prop_1 i, imdb_rel_05 r
   where i.iid = r.id2 and 
         i.iid not in (select imdb_id from gt05_movies) and
         i.iid not in (select imdb_id from gt05_people)
   into outfile '/tmp/imdb_other_prop2_05.txt';

select i.iid, i.prop, i.val from imdb_prop_1 i
   where i.iid not in (select imdb_id from gt05_people)
   having rand() > 0.5
   into outfile '/tmp/imdb_nd_people_prop_05.txt';

select i.iid, i.prop, i.val from imdb_prop_1 i
   where i.iid not in (select imdb_id from gt05_movies)
   having rand() > 0.5
   into outfile '/tmp/imdb_nd_movies_prop_05.txt';


