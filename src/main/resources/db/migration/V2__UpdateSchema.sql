-- Замена значения 'Комиссия' на 'Расход'
update budget set type = 'Расход' where type = 'Комиссия';

-- Таблица Author
create table author
(
    id       serial primary key,
    name     text not null,
    creation  timestamp
);

-- Привязка по Author.id
alter table budget add column author_id int;
alter table budget add constraint budget_author_id_fk foreign key (author_id) references author (id);