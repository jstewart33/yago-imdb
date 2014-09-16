use sigma;

/*
select y.id1, y.id2 from yago_rel y, gt10_movies g where g.yago_id = y.id2 and y.relation = 'actedIn' into outfile '/tmp/yago_actedIn.txt';

select y.id1, y.id2 from yago_rel y, gt10_movies g where g.yago_id = y.id2 and y.relation = 'created' into outfile '/tmp/yago_created.txt';

select y.id1, y.id2 from yago_rel y, gt10_movies g where g.yago_id = y.id2 and y.relation = 'directed' into outfile '/tmp/yago_directed.txt';

select y.id1, y.id2 from yago_rel y, gt10_movies g where g.yago_id = y.id2 and y.relation = 'produced' into outfile '/tmp/yago_produced.txt';

load data infile '/tmp/yago_actedIn.txt' into table actedIn;
load data infile '/tmp/yago_created.txt' into table created;
load data infile '/tmp/yago_directed.txt' into table directed;
load data infile '/tmp/yago_produced.txt' into table produced;
*/

select y.yid, y.val from yago_prop y, actedIn a where y.yid = a.pid and y.prop = 'hasLabel' and y.yid not in (select yago_id from gt10_people) into outfile '/tmp/yago_other_people_hasLabel_a.txt';

select y.yid, y.val from yago_prop y, created c where y.yid = c.pid and y.prop = 'hasLabel' and y.yid not in (select yago_id from gt10_people) into outfile '/tmp/yago_other_people_hasLabel_c.txt';

select y.yid, y.val from yago_prop y, directed d where y.yid = d.pid and y.prop = 'hasLabel' and y.yid not in (select yago_id from gt10_people) into outfile '/tmp/yago_other_people_hasLabel_d.txt';

select y.yid, y.val from yago_prop y, produced p where y.yid = p.pid and y.prop = 'hasLabel' and y.yid not in (select yago_id from gt10_people) into outfile '/tmp/yago_other_people_hasLabel_p.txt';
/*
select y.yid, y.val from yago_prop y, gt10_people g, actedIn a, created c, directed d, produced p  where (y.yid = a.pid or y.yid = c.pid or y.yid = d.pid or y.yid = p.pid) and y.prop = 'diedOnDate' and y.yid not in (select yago_id from gt10_people where y.prop = 'diedOnDate') into outfile '/tmp/yago_other_people_diedOnDate.txt';

select y.yid, y.val from yago_prop y, gt10_people g, actedIn a, created c, directed d, produced p  where (y.yid = a.pid or y.yid = c.pid or y.yid = d.pid or y.yid = p.pid) and y.prop = 'hasFamilyName' and y.yid not in (select yago_id from gt10_people where y.prop = 'hasFamilyName') into outfile '/tmp/yago_other_people_hasFamilyName.txt';

select y.yid, y.val from yago_prop y, gt10_people g, actedIn a, created c, directed d, produced p  where (y.yid = a.pid or y.yid = c.pid or y.yid = d.pid or y.yid = p.pid) and y.prop = 'hasGender' and y.yid not in (select yago_id from gt10_people where y.prop  = 'hasGender') into outfile '/tmp/yago_other_people_hasGender.txt';

select y.yid, y.val from yago_prop y, gt10_people g, actedIn a, created c, directed d, produced p  where (y.yid = a.pid or y.yid = c.pid or y.yid = d.pid or y.yid = p.pid) and y.prop = 'hasGivenName' and y.yid not in (select yago_id from gt10_people where y.prop = 'hasGivenName') into outfile '/tmp/yago_other_people_hasGivenName.txt';

select y.yid, y.val from yago_prop y, gt10_people g, actedIn a, created c, directed d, produced p  where (y.yid = a.pid or y.yid = c.pid or y.yid = d.pid or y.yid = p.pid) and y.prop = 'hasHeight' and y.yid not in (select yago_id from gt10_people where y.prop = 'hasHeight') into outfile '/tmp/yago_other_people_hasHeight.txt';

select y.yid, y.val from yago_prop y, gt10_people g, actedIn a, created c, directed d, produced p  where (y.yid = a.pid or y.yid = c.pid or y.yid = d.pid or y.yid = p.pid) and y.prop = 'wasBornOnDate' and y.yid not in (select yago_id from gt10_people where y.prop = 'wasBornOnDate') into outfile '/tmp/yago_other_people_wasBornOnDate.txt';

select y.yid, y.val from yago_prop y, gt10_people g, actedIn a, created c, directed d, produced p  where (y.yid = a.pid or y.yid = c.pid or y.yid = d.pid or y.yid = p.pid) and y.prop = 'wasCreatedOnDate' and y.yid not in (select yago_id from gt10_people where y.prop = 'wasCreatedOnDate') into outfile '/tmp/yago_other_people_wasCreatedOnDate.txt';
*/
