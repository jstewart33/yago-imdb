--
-- create the tables for the 1% dataset
--

use sigma;

create table gt1_people (yago_id char(10), imdb_id char(10));
create table gt1_movies (yago_id char(10), imdb_id char(10));

load data infile '/tmp/gt1_people.txt' into table gt1_people;
load data infile '/tmp/gt1_movies.txt' into table gt1_movies;

create table yago_rel_1 (relation char(10), id1 char(10), id2 char(10));
create table imdb_rel_1 (relation char(10), id1 char(10), id2 char(10));

create table yago_other_rel_1 (relation char(10), id1 char(10), id2 char(10));
create table imdb_other_rel_1 (relation char(10), id1 char(10), id2 char(10));

create table yago_prop_1 (yid char(10), prop varchar(20), val varchar(100));
create table imdb_prop_1 (iid char(10), prop varchar(20), val varchar(100));

create table yago_rel_05 (relation char(10), id1 char(10), id2 char(10));
create table imdb_rel_05 (relation char(10), id1 char(10), id2 char(10));

create table yago_other_rel_05 (relation char(10), id1 char(10), id2 char(10));
create table imdb_other_rel_05 (relation char(10), id1 char(10), id2 char(10));

create table yago_prop_05 (yid char(10), prop varchar(20), val varchar(100));
create table imdb_prop_05 (iid char(10), prop varchar(20), val varchar(100));
