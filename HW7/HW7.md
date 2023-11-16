# Задание

- Воспользовавшись инструкцией https://cassandra.apache.org/doc/latest/cassandra/operating/backups.html создать бэкап и восстановиться из него.
Какие вы видите подводные камни в этом процессе?

Задание с **:
- воспользоваться сторонними средствами для бэкапа всего кластера, например 3dnap:
https://portworx.com/blog/kubernetes-cassandra-run-ha-cassandra-rancher-kubernetes-engine/
примерный конфиг для 3DSnap https://github.com/aeuge/noSqlOtus/tree/main/cassandra_second

## Воспользовавшись инструкцией https://cassandra.apache.org/doc/latest/cassandra/operating/backups.html создать бэкап и восстановиться из него.

Воспользовавшись конфигурацией из HW6 поднимем кластер, создадим структуру таблиц и заполним их данными

Создадим snapshot на каждой из нод кластера

```
root@46378b5d11fe:/# nodetool snapshot --tag TEST_DB TEST_DB
Requested creating snapshot(s) for [TEST_DB] with snapshot name [TEST_DB] and options {skipFlush=false}
Snapshot directory: TEST_DB
```

Выполнив команду, убедимся, что snapshot действительно создан

```
root@46378b5d11fe:/# nodetool listsnapshots
Snapshot Details:
Snapshot name Keyspace name Column family name True size Size on disk Creation time            Expiration time
TEST_DB       TEST_DB       user               1.07 KiB  6.25 KiB     2023-11-16T06:48:49.071Z
TEST_DB       TEST_DB       video              1.04 KiB  6.07 KiB     2023-11-16T06:48:49.071Z
```

Удалим keyspace и создадим его заново

```
drop keyspace "TEST_DB";

create keyspace "TEST_DB"
    with replication = {
        'class': 'NetworkTopologyStrategy',
        'replication_factor': 2
        };

```

Создаем папки и переносим snapshot в правильное место
```
root@46378b5d11fe:/# mkdir TEST_DB
root@46378b5d11fe:/# cd TEST_DB/
root@46378b5d11fe:/TEST_DB# mkdir user && mkdir video
root@46378b5d11fe:/TEST_DB# mv /var/lib/cassandra/data/TEST_DB/user-f0b34810845211ee95eb19f59a425a64/snapshots/TEST_DB/* user/
root@46378b5d11fe:/TEST_DB# mv /var/lib/cassandra/data/TEST_DB/video-f0c1ee10845211ee95eb19f59a425a64/* video/
```

Загружаем snapshot в базу
Сперва загружаем схему

```
root@46378b5d11fe:/TEST_DB# cqlsh < video/schema.cql
root@46378b5d11fe:/TEST_DB# cqlsh < user/schema.cql
```

И загружаем данные

```
root@46378b5d11fe:/TEST_DB# sstableloader -nodes 172.22.0.2 video/
WARN  07:46:15,893 Only 56.153GiB free across all data volumes. Consider adding more capacity to your cluster or removing obsolete snapshots
Established connection to initial hosts
Opening sstables and calculating sections to stream
Streaming relevant part of /TEST_DB/video/nb-1-big-Data.db  to [/172.22.0.4:7000, /172.22.0.2:7000, /172.22.0.3:7000]
progress: [/172.22.0.4:7000]0:0/7 0  % [/172.22.0.3:7000]0:1/7 2  % total: 1% 0.049KiB/s (avg: 0.049KiB/s)
progress: [/172.22.0.4:7000]0:0/7 0  % [/172.22.0.3:7000]0:2/7 3  % total: 1% 6.003KiB/s (avg: 0.057KiB/s)
progress: [/172.22.0.4:7000]0:0/7 0  % [/172.22.0.3:7000]0:3/7 97 % total: 48% 7.917MiB/s (avg: 1.718KiB/s)
progress: [/172.22.0.4:7000]0:1/7 2  % [/172.22.0.3:7000]0:3/7 97 % total: 50% 120.287KiB/s (avg: 1.766KiB/s)
progress: [/172.22.0.4:7000]0:1/7 2  % [/172.22.0.3:7000]0:4/7 98 % total: 50% 149.811KiB/s (avg: 1.782KiB/s)
progress: [/172.22.0.4:7000]0:2/7 3  % [/172.22.0.3:7000]0:4/7 98 % total: 50% 22.084KiB/s (avg: 1.790KiB/s)
progress: [/172.22.0.4:7000]0:2/7 3  % [/172.22.0.3:7000]0:5/7 98 % total: 50% 47.456KiB/s (avg: 1.795KiB/s)
progress: [/172.22.0.4:7000]0:3/7 97 % [/172.22.0.3:7000]0:5/7 98 % total: 98% 10.720MiB/s (avg: 3.454KiB/s)
progress: [/172.22.0.4:7000]0:3/7 97 % [/172.22.0.3:7000]0:6/7 99 % total: 98% 46.405KiB/s (avg: 3.472KiB/s)
progress: [/172.22.0.4:7000]0:4/7 98 % [/172.22.0.3:7000]0:6/7 99 % total: 99% 49.461KiB/s (avg: 3.486KiB/s)
progress: [/172.22.0.4:7000]0:4/7 98 % [/172.22.0.3:7000]0:7/7 100% total: 99% 20.917KiB/s (avg: 3.489KiB/s)
progress: [/172.22.0.4:7000]0:5/7 98 % [/172.22.0.3:7000]0:7/7 100% total: 99% 16.531KiB/s (avg: 3.494KiB/s)
progress: [/172.22.0.4:7000]0:6/7 99 % [/172.22.0.3:7000]0:7/7 100% total: 99% 73.213KiB/s (avg: 3.513KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 9.426KiB/s (avg: 3.515KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:1/7 2  % [/172.22.0.3:7000]0:7/7 100% total: 67% 8.660KiB/s (avg: 3.543KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:2/7 3  % [/172.22.0.3:7000]0:7/7 100% total: 67% 35.113KiB/s (avg: 3.551KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:3/7 97 % [/172.22.0.3:7000]0:7/7 100% total: 99% 5.940MiB/s (avg: 5.195KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:4/7 98 % [/172.22.0.3:7000]0:7/7 100% total: 99% 57.909KiB/s (avg: 5.210KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:5/7 98 % [/172.22.0.3:7000]0:7/7 100% total: 99% 22.064KiB/s (avg: 5.215KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:6/7 99 % [/172.22.0.3:7000]0:7/7 100% total: 99% 62.721KiB/s (avg: 5.231KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 15.101KiB/s (avg: 5.234KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 0.000KiB/s (avg: 5.215KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 0.000KiB/s (avg: 5.211KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 0.000KiB/s (avg: 5.178KiB/s)

Summary statistics:
   Connections per host    : 1
   Total files transferred : 14
   Total bytes transferred : 14.991KiB
   Total duration          : 2961 ms
   Average transfer rate   : 5.062KiB/s
   Peak transfer rate      : 5.234KiB/s

root@46378b5d11fe:/TEST_DB# sstableloader -nodes 172.22.0.2 user/
WARN  07:46:50,657 Only 56.153GiB free across all data volumes. Consider adding more capacity to your cluster or removing obsolete snapshots
Established connection to initial hosts
Opening sstables and calculating sections to stream
Streaming relevant part of /TEST_DB/user/nb-1-big-Data.db  to [/172.22.0.4:7000, /172.22.0.2:7000, /172.22.0.3:7000]
progress: [/172.22.0.4:7000]0:1/7 3  % [/172.22.0.2:7000]0:1/7 3  % [/172.22.0.3:7000]0:1/7 3  % total: 3% 0.168KiB/s (avg: 0.168KiB/s)
progress: [/172.22.0.4:7000]0:1/7 3  % [/172.22.0.2:7000]0:1/7 3  % [/172.22.0.3:7000]0:1/7 3  % total: 3% 0.000KiB/s (avg: 0.168KiB/s)
progress: [/172.22.0.4:7000]0:2/7 3  % [/172.22.0.2:7000]0:1/7 3  % [/172.22.0.3:7000]0:1/7 3  % total: 3% 92.447KiB/s (avg: 0.177KiB/s)
progress: [/172.22.0.4:7000]0:2/7 3  % [/172.22.0.2:7000]0:2/7 3  % [/172.22.0.3:7000]0:1/7 3  % total: 3% 35.916KiB/s (avg: 0.186KiB/s)
progress: [/172.22.0.4:7000]0:3/7 97 % [/172.22.0.2:7000]0:2/7 3  % [/172.22.0.3:7000]0:1/7 3  % total: 34% 7.773MiB/s (avg: 1.901KiB/s)
progress: [/172.22.0.4:7000]0:3/7 97 % [/172.22.0.2:7000]0:2/7 3  % [/172.22.0.3:7000]0:2/7 3  % total: 34% 36.555KiB/s (avg: 1.909KiB/s)
progress: [/172.22.0.4:7000]0:4/7 98 % [/172.22.0.2:7000]0:2/7 3  % [/172.22.0.3:7000]0:2/7 3  % total: 35% 42.724KiB/s (avg: 1.925KiB/s)
progress: [/172.22.0.4:7000]0:4/7 98 % [/172.22.0.2:7000]0:2/7 3  % [/172.22.0.3:7000]0:3/7 97 % total: 66% 16.885MiB/s (avg: 3.639KiB/s)
progress: [/172.22.0.4:7000]0:4/7 98 % [/172.22.0.2:7000]0:3/7 97 % [/172.22.0.3:7000]0:3/7 97 % total: 97% 5.809MiB/s (avg: 5.352KiB/s)
progress: [/172.22.0.4:7000]0:5/7 98 % [/172.22.0.2:7000]0:3/7 97 % [/172.22.0.3:7000]0:3/7 97 % total: 97% 17.296KiB/s (avg: 5.355KiB/s)
progress: [/172.22.0.4:7000]0:5/7 98 % [/172.22.0.2:7000]0:4/7 98 % [/172.22.0.3:7000]0:3/7 97 % total: 98% 80.759KiB/s (avg: 5.371KiB/s)
progress: [/172.22.0.4:7000]0:5/7 98 % [/172.22.0.2:7000]0:5/7 98 % [/172.22.0.3:7000]0:4/7 98 % total: 98% 55.717KiB/s (avg: 5.391KiB/s)
progress: [/172.22.0.4:7000]0:5/7 98 % [/172.22.0.2:7000]0:5/7 98 % [/172.22.0.3:7000]0:4/7 98 % total: 98% 0.000KiB/s (avg: 5.390KiB/s)
progress: [/172.22.0.4:7000]0:6/7 99 % [/172.22.0.2:7000]0:5/7 98 % [/172.22.0.3:7000]0:4/7 98 % total: 98% 89.885KiB/s (avg: 5.407KiB/s)
progress: [/172.22.0.4:7000]0:6/7 99 % [/172.22.0.2:7000]0:6/7 99 % [/172.22.0.3:7000]0:4/7 98 % total: 99% 314.369KiB/s (avg: 5.427KiB/s)
progress: [/172.22.0.4:7000]0:6/7 99 % [/172.22.0.2:7000]0:6/7 99 % [/172.22.0.3:7000]0:5/7 98 % total: 99% 29.462KiB/s (avg: 5.431KiB/s)
progress: [/172.22.0.4:7000]0:6/7 99 % [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:5/7 98 % total: 99% 9.388KiB/s (avg: 5.433KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:5/7 98 % total: 99% 3.462KiB/s (avg: 5.431KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:6/7 99 % total: 99% 212.791KiB/s (avg: 5.449KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:6/7 99 % total: 99% 0.000KiB/s (avg: 5.449KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 16.226KiB/s (avg: 5.451KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 0.000KiB/s (avg: 5.401KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 0.000KiB/s (avg: 5.399KiB/s)
progress: [/172.22.0.4:7000]0:7/7 100% [/172.22.0.2:7000]0:7/7 100% [/172.22.0.3:7000]0:7/7 100% total: 100% 0.000KiB/s (avg: 5.397KiB/s)

Summary statistics:
   Connections per host    : 1
   Total files transferred : 21
   Total bytes transferred : 15.428KiB
   Total duration          : 2923 ms
   Average transfer rate   : 5.277KiB/s
   Peak transfer rate      : 5.451KiB/s
   ```

Из недостатков данного метода можно отметить большое количество ручных манипуляций в процессе бекапа и восстановления, необходимость выполнения команд на всех нодах кластера, хранение данных внутри файловой структуры кассандры, что влечет повышенные требования к файловому пространству и необходимость последующего ручного копирования 

## - воспользоваться сторонними средствами для бэкапа всего кластера, например 3dnap:

Так как найти дистрибутив для 3dnap без kubernetes с ходу не удалось, попробуем использовать Medusa (https://github.com/thelastpickle/cassandra-medusa/blob/master/docs/Performing-backups.md)

Установим его на все узлы кластера

```
curl -1sLf \
  'https://dl.cloudsmith.io/public/thelastpickle/medusa/setup.deb.sh' \
  | sudo -E bash

sudo apt-get update
sudo apt-get install cassandra-medusa
```

и произведем бекап

```
medusa backup --backup-name=TEST-DB --mode=full
```