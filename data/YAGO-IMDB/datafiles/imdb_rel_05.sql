use sigma;

select i.relation, i.id1, i.id2 from imdb_rel_1 i, gt05_movies g
   where g.imdb_id = i.id1
   into outfile '/tmp/imdb_rel_movies1_05.txt';

load data infile '/tmp/imdb_rel_movies1_05.txt' into table imdb_rel_05;

select i.relation, i.id1, i.id2 from imdb_rel_1 i, gt05_movies g
   where g.imdb_id = i.id2
   into outfile '/tmp/imdb_rel_movies2_05.txt';

load data infile '/tmp/imdb_rel_movies2_05.txt' into table imdb_rel_05;

select i.relation, i.id1, i.id2 from imdb_rel_1 i, gt05_people g
   where g.imdb_id = i.id1
   into outfile '/tmp/imdb_rel_people1_05.txt';

load data infile '/tmp/imdb_rel_people1_05.txt' into table imdb_rel_05;

select i.relation, i.id1, i.id2 from imdb_rel_1 i, gt05_people g 
   where g.imdb_id = i.id2 
   into outfile '/tmp/imdb_rel_people2_05.txt';

load data infile '/tmp/imdb_rel_people2_05.txt' into table imdb_rel_05;

select i.relation, i.id1, i.id2 from imdb_rel_1 i
   where i.id1 like 'p%' and
         i.id1 not in (select imdb_id from gt05_people)
   having rand() > 0.5
   into outfile '/tmp/imdb_rel_other_people_05.txt';

load data infile '/tmp/imdb_rel_other_people_05.txt' into table imdb_other_rel_05;

select i.relation, i.id1, i.id2 from imdb_rel_1 i
   where i.id2 like 'tt%' and
         i.id2 not in (select imdb_id from gt05_movies)
   having rand() > 0.5
   into outfile '/tmp/imdb_rel_other_movies_05.txt';

load data infile '/tmp/imdb_rel_other_movies_05.txt' into table imdb_other_rel_05;
