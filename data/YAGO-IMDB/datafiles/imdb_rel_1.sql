use sigma;

select i.relation, i.id1, i.id2 from imdb_rel i, gt1_movies g
   where g.imdb_id = i.id1
   into outfile '/tmp/imdb_rel_movies1_1.txt';

load data infile '/tmp/imdb_rel_movies1_1.txt' into table imdb_rel_1;

select i.relation, i.id1, i.id2 from imdb_rel i, gt1_movies g
   where g.imdb_id = i.id2
   into outfile '/tmp/imdb_rel_movies2_1.txt';

load data infile '/tmp/imdb_rel_movies2_1.txt' into table imdb_rel_1;

select i.relation, i.id1, i.id2 from imdb_rel i, gt1_people g
   where g.imdb_id = i.id1
   into outfile '/tmp/imdb_rel_people1_1.txt';

load data infile '/tmp/imdb_rel_people1_1.txt' into table imdb_rel_1;

select i.relation, i.id1, i.id2 from imdb_rel i, gt1_people g 
   where g.imdb_id = i.id2 
   into outfile '/tmp/imdb_rel_people2_1.txt';

load data infile '/tmp/imdb_rel_people2_1.txt' into table imdb_rel_1;

select i.relation, i.id1, i.id2 from imdb_rel i
   where i.id1 like 'p%' and
         i.id1 not in (select imdb_id from gt1_people)
   having rand() > 0.99
   into outfile '/tmp/imdb_rel_other_people_1.txt';

load data infile '/tmp/imdb_rel_other_people_1.txt' into table imdb_other_rel_1;

select i.relation, i.id1, i.id2 from imdb_rel i
   where i.id2 like 'tt%' and
         i.id2 not in (select imdb_id from gt1_movies)
   having rand() > 0.99
   into outfile '/tmp/imdb_rel_other_movies_1.txt';

load data infile '/tmp/imdb_rel_other_movies_1.txt' into table imdb_other_rel_1;
