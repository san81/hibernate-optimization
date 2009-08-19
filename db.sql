
CREATE TABLE `parent` (
  `sno` int(6) NOT NULL auto_increment,
`name` varchar(200) collate latin1_german2_ci default NULL,
`version` int(6) not null,
PRIMARY KEY  (`sno`)
)ENGINE=InnoDB;

CREATE TABLE child (
  sno int(6) NOT NULL auto_increment,
child_name varchar(200),
parent_id int(6) not null,
PRIMARY KEY  (sno),
FOREIGN KEY(parent_id) REFERENCES parent(sno) ON UPDATE CASCADE
)ENGINE=InnoDB;


-- for oracle db
create table parent ( sno number(2) primary key,name varchar(200))
create table child (sno number(2) primary key,child_name varchar(200),parent_id number(2) references parent(sno) not null)

select * from parent
select * from child

insert into child (parent_id,name,comment,version) values (2,'first','firscomment',1);
insert into child (parent_id,name,comment,version) values (2,'second','2comment',1);
insert into child (parent_id,name,comment,version) values (2,'third','3comment',1);

insert into child (parent_id,name,comment,version) values (2,'third','3comment',1);
insert into child (parent_id,name,comment,version) values (2,'third','3comment',1);
insert into child (parent_id,name,comment,version) values (2,'third','3comment',1);