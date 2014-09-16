use sigma;

/*
select y.yid, y.val from yago_prop y, gt10_people g where g.yago_id = y.yid and y.prop = 'hasLabel' into outfile '/tmp/yago_people_hasLabel.txt';

select y.yid, y.val from yago_prop y, gt10_people g where g.yago_id = y.yid and y.prop = 'diedOnDate' into outfile '/tmp/yago_people_diedOnDate.txt';

select y.yid, y.val from yago_prop y, gt10_people g where g.yago_id = y.yid and y.prop = 'hasFamilyName' into outfile '/tmp/yago_people_hasFamilyName.txt';

select y.yid, y.val from yago_prop y, gt10_people g where g.yago_id = y.yid and y.prop = 'hasGender' into outfile '/tmp/yago_people_hasGender.txt';

select y.yid, y.val from yago_prop y, gt10_people g where g.yago_id = y.yid and y.prop = 'hasGivenName' into outfile '/tmp/yago_people_hasGivenName.txt';

select y.yid, y.val from yago_prop y, gt10_people g where g.yago_id = y.yid and y.prop = 'hasHeight' into outfile '/tmp/yago_people_hasHeight.txt';

select y.yid, y.val from yago_prop y, gt10_people g where g.yago_id = y.yid and y.prop = 'wasBornOnDate' into outfile '/tmp/yago_people_wasBornOnDate.txt';

select y.yid, y.val from yago_prop y, gt10_people g where g.yago_id = y.yid and y.prop = 'wasCreatedOnDate' into outfile '/tmp/yago_people_wasCreatedOnDate.txt';

select y.yid, y.val from yago_prop y, gt10_movies g where g.yago_id = y.yid and y.prop = 'hasLabel' into outfile '/tmp/yago_movies_hasLabel.txt';

select y.yid, y.val from yago_prop y, gt10_movies g where g.yago_id = y.yid and y.prop = 'diedOnDate' into outfile '/tmp/yago_movies_diedOnDate.txt';

select y.yid, y.val from yago_prop y, gt10_movies g where g.yago_id = y.yid and y.prop = 'hasFamilyName' into outfile '/tmp/yago_movies_hasFamilyName.txt';

select y.yid, y.val from yago_prop y, gt10_movies g where g.yago_id = y.yid and y.prop = 'hasGender' into outfile '/tmp/yago_movies_hasGender.txt';

select y.yid, y.val from yago_prop y, gt10_movies g where g.yago_id = y.yid and y.prop = 'hasGivenName' into outfile '/tmp/yago_movies_hasGivenName.txt';

select y.yid, y.val from yago_prop y, gt10_movies g where g.yago_id = y.yid and y.prop = 'hasHeight' into outfile '/tmp/yago_movies_hasHeight.txt';

select y.yid, y.val from yago_prop y, gt10_movies g where g.yago_id = y.yid and y.prop = 'wasBornOnDate' into outfile '/tmp/yago_movies_wasBornOnDate.txt';

select y.yid, y.val from yago_prop y, gt10_movies g where g.yago_id = y.yid and y.prop = 'wasCreatedOnDate' into outfile '/tmp/yago_movies_wasCreatedOnDate.txt';
*/

select y.id1, y.id2 from yago_rel y, gt10_movies g where g.yago_id = y.id2 and y.relation = 'actedIn' into outfile '/tmp/yago_actedIn.txt'

select y.id1, y.id2 from yago_rel y, gt10_movies g where g.yago_id = y.id2 and y.relation = 'created' into outfile '/tmp/yago_created.txt'

select y.id1, y.id2 from yago_rel y, gt10_movies g where g.yago_id = y.id2 and y.relation = 'directed' into outfile '/tmp/yago_directed.txt'

select y.id1, y.id2 from yago_rel y, gt10_movies g where g.yago_id = y.id2 and y.relation = 'produced' into outfile '/tmp/yago_produced.txt'

load data infile '/tmp/yago_actedIn.txt' into table actedIn;
load data infile '/tmp/yago_created.txt' into table created;
load data infile '/tmp/yago_directed.txt' into table directed;
load data infile '/tmp/yago_produced.txt' into table produced;
