# Задание
- развернуть docker локально или в облаке
- поднять 3 узловый Cassandra кластер.
- Создать keyspase с 2-мя таблицами. Одна из таблиц должна иметь составной Partition key, как минимум одно поле - clustering key, как минимум одно поле не входящее в primiry key.
- Заполнить данными обе таблицы.
- Выполнить 2-3 варианта запроса использую WHERE
- Создать вторичный индекс на поле, не входящее в primiry key.
- (*) нагрузить кластер при помощи Cassandra Stress Tool (используя "How to use Apache Cassandra Stress Tool.pdf" из материалов).


## Поднять 3 узловый Cassandra кластер

Разворачиваем кластер кассандра с помощью следующего docker-compose файла:

```
version: '3.8'

networks:
  app-tier:
    driver: bridge


services:
  cassandra1:
    image: cassandra:latest
    container_name: cassandra1
    networks:
      - app-tier
    ports:
      - 9042:9042
    environment:
      - CASSANDRA_SEEDS=cassandra1,cassandra2,cassandra3
      - CASSANDRA_CLUSTER_NAME=test_cassandra
  cassandra2:
    image: cassandra:latest
    container_name: cassandra2
    networks:
      - app-tier
    ports:
      - 19043:9042
    environment:
      - CASSANDRA_SEEDS=cassandra1,cassandra2,cassandra3
      - CASSANDRA_CLUSTER_NAME=test_cassandra
  cassandra3:
    image: cassandra:latest
    container_name: cassandra3
    networks:
      - app-tier
    ports:
      - 19044:9042
    environment:
      - CASSANDRA_SEEDS=cassandra1,cassandra2,cassandra3
      - CASSANDRA_CLUSTER_NAME=test_cassandra
```

После запуска подключаемся с помощью Datagrip

## Создать keyspase с 2-мя таблицами. Одна из таблиц должна иметь составной Partition key, как минимум одно поле - clustering key, как минимум одно поле не входящее в primiry key.

Создаем keyspace

```
create keyspace "TEST_DB"
with replication = {
    'class': 'NetworkTopologyStrategy',
    'replication_factor': 2
    };
```

Создаем 2 таблицы - User и Video

```
USE "TEST_DB"

create table User
(
    id int,
    name text,
    email text,
    password text,
    groups list<text>,
    PRIMARY KEY ( (id), email )
);

create table Video
(
    id int,
    name text,
    link text,
    user int,
    PRIMARY KEY ( (user), id )
);
```


## Заполнить данными обе таблицы
```
INSERT INTO "TEST_DB".user (id, email, groups, name, password) VALUES (1, 'test@mail.ru', [], 'Alex Petrov', '123');
INSERT INTO "TEST_DB".user (id, email, groups, name, password) VALUES (2, 'admin@mail.ru', ['admin'], 'Admin', '223');
INSERT INTO "TEST_DB".user (id, email, groups, name, password) VALUES (3, 'test3@mail.ru', [], 'Alex Alex', '333');
INSERT INTO "TEST_DB".video (id, user, link, name) VALUES (1, 1, 'http://video1', 'Cats video');
INSERT INTO "TEST_DB".video (id, user, link, name) VALUES (2, 3, 'http://video2', 'Dogs video');
INSERT INTO "TEST_DB".video (id, user, link, name) VALUES (3, 2, 'http://rules', 'Rules video');
INSERT INTO "TEST_DB".video (id, user, link, name) VALUES (4, 1, 'http://video4', 'Sample video');
```

## Выполнить 2-3 варианта запроса использую WHERE

```
Получаем все видео
select * from video;
Обращаем внимание, что данные отсортированы по кластерному ключу
```

```
Получаем все видео пользователя
select * from video where user = 1 ;

```

```
Пробуем получить видео по id - 
select * from video where id =2;
И получаем ошибку - id второй компонент ключа и без первого компонента применятся не может

```

```
Получаем конкретное видео пользователя
select * from video where id =2 and user =3;
```


```
Получаем пользователя по id
select * from user where id = 1 ;
```

## Создать вторичный индекс на поле, не входящее в primiry key

Пробуем получить всех администраторов
```
select * from user where groups contains 'admin';
```

Получаем ошибку

```
Cannot execute this query as it might involve data filtering and thus may have unpredictable performance. If you want to execute this query despite the performance unpredictability, use ALLOW FILTERING
```

Создаем вторичный индекс 

```
create index if not exists on user(groups);
```

Далем запрос

```
select * from user where groups contains 'admin';
```

и получаем результат
```
[
  {
    "id": 2,
    "email": "admin@mail.ru",
    "groups": ["admin"],
    "name": "Admin",
    "password": "223"
  }
]
```

## нагрузить кластер при помощи Cassandra Stress Tool (используя "How to use Apache Cassandra Stress Tool.pdf" из материалов).

Установим Cassandra на локальную машину

```
brew install cassandra
```

Пробуем запустить Cassandra Stress Tool

```
cassandra-stress write n=1000000
```

и получаем результаты

```
Running WRITE with 200 threads for 1000000 iteration
...
Results:
Op rate                   :   54,948 op/s  [WRITE: 54,948 op/s]
Partition rate            :   54,948 pk/s  [WRITE: 54,948 pk/s]
Row rate                  :   54,948 row/s [WRITE: 54,948 row/s]
Latency mean              :    3.6 ms [WRITE: 3.6 ms]
Latency median            :    1.8 ms [WRITE: 1.8 ms]
Latency 95th percentile   :    9.8 ms [WRITE: 9.8 ms]
Latency 99th percentile   :   27.1 ms [WRITE: 27.1 ms]
Latency 99.9th percentile :  248.3 ms [WRITE: 248.3 ms]
Latency max               :  393.0 ms [WRITE: 393.0 ms]
Total partitions          :  1,000,000 [WRITE: 1,000,000]
Total errors              :          0 [WRITE: 0]
Total GC count            : 0
Total GC memory           : 0.000 KiB
Total GC time             :    0.0 seconds
Avg GC time               :    NaN ms
StdDev GC time            :    0.0 ms
Total operation time      : 00:00:18
```

Тест на чтение

```
cassandra-stress read n=1000000
```

```
Running with 81 threadCount
Running READ with 81 threads for 1000000 iteration

Results:
Op rate                   :   75,463 op/s  [READ: 75,463 op/s]
Partition rate            :   75,463 pk/s  [READ: 75,463 pk/s]
Row rate                  :   75,463 row/s [READ: 75,463 row/s]
Latency mean              :    1.0 ms [READ: 1.0 ms]
Latency median            :    0.7 ms [READ: 0.7 ms]
Latency 95th percentile   :    2.5 ms [READ: 2.5 ms]
Latency 99th percentile   :    6.3 ms [READ: 6.3 ms]
Latency 99.9th percentile :   17.2 ms [READ: 17.2 ms]
Latency max               :   79.7 ms [READ: 79.7 ms]
Total partitions          :  1,000,000 [READ: 1,000,000]
Total errors              :          0 [READ: 0]
Total GC count            : 0
Total GC memory           : 0.000 KiB
Total GC time             :    0.0 seconds
Avg GC time               :    NaN ms
StdDev GC time            :    0.0 ms
Total operation time      : 00:00:13
```

Таким образом максимальная производительность по чтению была достигнута на 81 потоке