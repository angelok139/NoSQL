# Задание 

- построить шардированный кластер из 3 кластерных нод( по 3 инстанса с репликацией) и с кластером конфига(3 инстанса);
- добавить балансировку, нагрузить данными, выбрать хороший ключ шардирования, посмотреть как данные перебалансируются между шардами;
- поронять разные инстансы, посмотреть, что будет происходить, поднять обратно. Описать что произошло.
- настроить аутентификацию и многоролевой доступ;


*Формат сдачи - readme с описанием алгоритма действий, результатами и проблемами.*

## построить шардированный кластер из 3 кластерных нод( по 3 инстанса с репликацией) и с кластером конфига(3 инстанса);
Поднимаем инстансы конфигурационной БД  с помощью следующего docker-compose файла:

```
version: '3.8'

networks:
  app-tier:
    driver: bridge

services:
  mongocfg1:
    image: 'mongo'
    container_name: mongo-cfg1
    networks:
      - app-tier
    command: ["--configsvr", "--replSet", "CRS1", "--bind_ip_all"]
    ports:
      - 37017:27019
    volumes:
      - ./db/cdb/db1:/data/db
      - ./keyfile.key:/opt/keyfile.key


  mongocfg2:
    image: 'mongo'
    container_name: mongo-cfg2
    networks:
      - app-tier
    command: ["--configsvr", "--replSet", "CRS1", "--bind_ip_all"]
    ports:
      - 37018:27019
    volumes:
      - ./db/cdb/db2:/data/db
      - ./keyfile.key:/opt/keyfile.key


  mongocfg3:
    image: 'mongo'
    container_name: mongo-cfg3
    networks:
      - app-tier
    command: ["--configsvr", "--replSet", "CRS1", "--bind_ip_all"]
    ports:
      - 37019:27019
    volumes:
      - ./db/cdb/db3:/data/db
      - ./keyfile.key:/opt/keyfile.key
```

Создаем кластер командой 
```
rs.initiate({
    "_id": "CRS1",
    members: [
        {"_id": 0, priority: 3, host:"mongo-cfg1:27019"},
        {"_id": 1, host:"mongo-cfg2:27019"},
        {"_id": 2, host:"mongo-cfg3:27019"}
    ]
    }
)
```

получаем ответ OK

Выполняем команду rs.status()

Получаем ответ:
```  
...
"members": [
      {
        "_id": 0,
        "name": "mongo-cfg1:27019",
        "health": 1,
        "state": 1,
        "stateStr": "PRIMARY",
        "uptime": 37,
        "optime": {
          "ts": {"$timestamp": {"t": 1698220826, "i": 1}},
          "t": 2
        },
        "optimeDurable": {
          "ts": {"$timestamp": {"t": 1698220826, "i": 1}},
          "t": 2
        },
        "optimeDate": {"$date": "2023-10-25T08:00:26.000Z"},
        "optimeDurableDate": {"$date": "2023-10-25T08:00:26.000Z"},
        "lastAppliedWallTime": {"$date": "2023-10-25T08:00:28.877Z"},
        "lastDurableWallTime": {"$date": "2023-10-25T08:00:27.875Z"},
        "lastHeartbeat": {"$date": "2023-10-25T08:00:27.362Z"},
        "lastHeartbeatRecv": {"$date": "2023-10-25T08:00:28.890Z"},
        "pingMs": 0,
        "lastHeartbeatMessage": "",
        "syncSourceHost": "",
        "syncSourceId": -1,
        "infoMessage": "",
        "electionTime": {"$timestamp": {"t": 1698220812, "i": 2}},
        "electionDate": {"$date": "2023-10-25T08:00:12.000Z"},
        "configVersion": 1,
        "configTerm": 2
      },
      {
        "_id": 1,
        "name": "mongo-cfg2:27019",
        "health": 1,
        "state": 2,
        "stateStr": "SECONDARY",
        "uptime": 37,
        "optime": {
          "ts": {"$timestamp": {"t": 1698220827, "i": 1}},
          "t": 2
        },
        "optimeDurable": {
          "ts": {"$timestamp": {"t": 1698220827, "i": 1}},
          "t": 2
        },
        "optimeDate": {"$date": "2023-10-25T08:00:27.000Z"},
        "optimeDurableDate": {"$date": "2023-10-25T08:00:27.000Z"},
        "lastAppliedWallTime": {"$date": "2023-10-25T08:00:28.877Z"},
        "lastDurableWallTime": {"$date": "2023-10-25T08:00:28.877Z"},
        "lastHeartbeat": {"$date": "2023-10-25T08:00:28.893Z"},
        "lastHeartbeatRecv": {"$date": "2023-10-25T08:00:28.890Z"},
        "pingMs": 0,
        "lastHeartbeatMessage": "",
        "syncSourceHost": "mongo-cfg3:27019",
        "syncSourceId": 2,
        "infoMessage": "",
        "configVersion": 1,
        "configTerm": 2
      },
      {
        "_id": 2,
        "name": "mongo-cfg3:27019",
        "health": 1,
        "state": 2,
        "stateStr": "SECONDARY",
        "uptime": 167,
        "optime": {
          "ts": {"$timestamp": {"t": 1698220828, "i": 1}},
          "t": 2
        },
        "optimeDate": {"$date": "2023-10-25T08:00:28.000Z"},
        "lastAppliedWallTime": {"$date": "2023-10-25T08:00:28.877Z"},
        "lastDurableWallTime": {"$date": "2023-10-25T08:00:28.877Z"},
        "syncSourceHost": "mongo-cfg1:27019",
        "syncSourceId": 0,
        "infoMessage": "",
        "configVersion": 1,
        "configTerm": 2,
        "self": true,
        "lastHeartbeatMessage": ""
      }
    ]
  ...
```

Конфигурационный кластер собрался успешно, добавляем в docker-compose.yml mongos
```
  mongos:
    image: 'mongo'
    container_name: mongo-s
    networks:
      - app-tier
    command: mongos --configdb CRS1/mongo-cfg1:27019,mongo-cfg2:27019,mongo-cfg3:27019 --bind_ip_all
    ports:
      - 27017:27017
```
и обновляем конфигурацию docker-compose up -d
подключаемся к mongos 
```
mongosh mongodb://localhost:27017
```
и видим результат
```
[direct: mongos] test> show dbs
admin   112.00 KiB
config  304.00 KiB
```

добавляем в docker-compose.yml записи для 9 нод кластера, для краткости опущу 8 остальных нод, они однотипны и не представляют интереса (но есть в репозитории в соседнем файле)
```
  mongors1-1:
    image: 'mongo'
    container_name: mongo-rs1-1
    networks:
      - app-tier
    command: ["--shardsvr", "--replSet", "RS1", "--bind_ip_all"]
    ports:
      - 37027:27018
```
Поключемся к любой ноде каждого из 3 replSet и выполняем на ней следующую команду (с изменениями для каждого из replSet)
```
rs.initiate({
    "_id": "RS1",
    members: [
        {"_id": 0, priority: 3, host:"mongo-rs1-1:27018"},
        {"_id": 1, host:"mongo-rs1-2:27018"},
        {"_id": 2, host:"mongo-rs1-3:27018"}
    ]
    }
)
```

Получаем ОК

Подключаемся к Mongos и добавляем шарды 
```

sh.addShard( "RS1/mongo-rs1-1:27018,mongo-rs1-2:27018,mongo-rs1-3:27018")
sh.addShard( "RS2/mongo-rs2-1:27018,mongo-rs2-2:27018,mongo-rs2-3:27018")
sh.addShard( "RS3/mongo-rs3-1:27018,mongo-rs3-2:27018,mongo-rs3-3:27018")
```

Получаем ответы
```
[
  {
    "$clusterTime": {
      "clusterTime": {"$timestamp": {"t": 1698222334, "i": 14}},
      "signature": {
        "hash": {"$binary": {"base64": "AAAAAAAAAAAAAAAAAAAAAAAAAAA=", "subType": "00"}},
        "keyId": 0
      }
    },
    "ok": 1,
    "operationTime": {"$timestamp": {"t": 1698222334, "i": 4}},
    "shardAdded": "RS1"
  }
]
```

## добавить балансировку, нагрузить данными, выбрать хороший ключ шардирования, посмотреть как данные перебалансируются между шардами

Меняем максимальный размер чанка по умолчанию
```
db.settings.updateOne(
   { _id: "chunksize" },
   { $set: { _id: "chunksize", value: 1 } },
   { upsert: true }
)
```

Создаем хешированный индекс по полю _id для более равномерного распределения
```
db.listingAndReviews.createIndex(
    {_id: "hashed"}
)
```

и включаем шардинг
```
sh.shardCollection("test.listingAndReviews", {"_id":"hashed"})
```



Загружаем данные в коллекцию с помощью команды mongoimport

```
mongoimport --db test --collection listingAndReviews mongodb://localhost:27017 listingsAndReviews.json
```
Получаем ответ
```
2023-10-25T11:30:56.653+0300	connected to: mongodb://localhost:27017
2023-10-25T11:30:58.734+0300	5555 document(s) imported successfully. 0 document(s) failed to import.
```

Делаем sh.status()
```
[direct: mongos] test> sh.status()
[direct: mongos] config> sh.status()
shardingVersion
{ _id: 1, clusterId: ObjectId("6538cb02a8fe207f4759e482") }
---
shards
[
  {
    _id: 'RS1',
    host: 'RS1/mongo-rs1-1:27018,mongo-rs1-2:27018,mongo-rs1-3:27018',
    state: 1,
    topologyTime: Timestamp({ t: 1698222333, i: 2 })
  },
  {
    _id: 'RS2',
    host: 'RS2/mongo-rs2-1:27018,mongo-rs2-2:27018,mongo-rs2-3:27018',
    state: 1,
    topologyTime: Timestamp({ t: 1698222334, i: 2 })
  },
  {
    _id: 'RS3',
    host: 'RS3/mongo-rs3-1:27018,mongo-rs3-2:27018,mongo-rs3-3:27018',
    state: 1,
    topologyTime: Timestamp({ t: 1698222334, i: 15 })
  }
]
---
active mongoses
[ { '7.0.2': 1 } ]
---
autosplit
{ 'Currently enabled': 'yes' }
---
balancer
{
  'Currently enabled': 'yes',
  'Failed balancer rounds in last 5 attempts': 0,
  'Currently running': 'no',
  'Migration Results for the last 24 hours': { '61': 'Success' }
}
---
databases
[
  {
    database: { _id: 'config', primary: 'config', partitioned: true },
    collections: {
      'config.system.sessions': {
        shardKey: { _id: 1 },
        unique: false,
        balancing: true,
        chunkMetadata: [ { shard: 'RS1', nChunks: 1 } ],
        chunks: [
          { min: { _id: MinKey() }, max: { _id: MaxKey() }, 'on shard': 'RS1', 'last modified': Timestamp({ t: 1, i: 0 }) }
        ],
        tags: []
      }
    }
  },
  {
    database: {
      _id: 'test',
      primary: 'RS2',
      partitioned: false,
      version: {
        uuid: new UUID("9d95d3f8-f4f2-4781-b529-360d3b018ef5"),
        timestamp: Timestamp({ t: 1698222656, i: 1 }),
        lastMod: 1
      }
    },
    collections: {
      'test.listingAndReviews': {
        shardKey: { _id: 'hashed' },
        unique: false,
        balancing: true,
        chunkMetadata: [
          { shard: 'RS1', nChunks: 2 },
          { shard: 'RS2', nChunks: 2 },
          { shard: 'RS3', nChunks: 2 }
        ],
        chunks: [
          { min: { _id: MinKey() }, max: { _id: Long("-6148914691236517204") }, 'on shard': 'RS3', 'last modified': Timestamp({ t: 1, i: 0 }) },
          { min: { _id: Long("-6148914691236517204") }, max: { _id: Long("-3074457345618258602") }, 'on shard': 'RS3', 'last modified': Timestamp({ t: 1, i: 1 }) },
          { min: { _id: Long("-3074457345618258602") }, max: { _id: Long("0") }, 'on shard': 'RS1', 'last modified': Timestamp({ t: 1, i: 2 }) },
          { min: { _id: Long("0") }, max: { _id: Long("3074457345618258602") }, 'on shard': 'RS1', 'last modified': Timestamp({ t: 1, i: 3 }) },
          { min: { _id: Long("3074457345618258602") }, max: { _id: Long("6148914691236517204") }, 'on shard': 'RS2', 'last modified': Timestamp({ t: 1, i: 4 }) },
          { min: { _id: Long("6148914691236517204") }, max: { _id: MaxKey() }, 'on shard': 'RS2', 'last modified': Timestamp({ t: 1, i: 5 }) }
        ],
        tags: []
      }
    }
  }
]
```

Коллекция шардирована и равномерно распределена по шардам

Попробуем удалить коллекцию, создать заново, но сперва залить данные и только потом добавить шардирование

Мы можем в реальном времени наблюдать, как данные переливаются из primary шарда во вторичные шарды

```
     chunkMetadata: [
          { shard: 'RS1', nChunks: 5 },
          { shard: 'RS2', nChunks: 1 },
          { shard: 'RS3', nChunks: 5 }
        ],
```
Пока не достигнут нормального распределения
```
...
  'Migration Results for the last 24 hours': { '117': 'Success' }
...
        chunkMetadata: [
          { shard: 'RS1', nChunks: 28 },
          { shard: 'RS2', nChunks: 1 },
          { shard: 'RS3', nChunks: 28 }
        ]
```

## поронять разные инстансы, посмотреть, что будет происходить, поднять обратно. Описать что произошло.
Попробуем выключить по одной RS с каждого шарда
Мы видим, что кластер успешно пережил отключение, данные доступны и по чтению и по записи. Primary Node была перемещена на другую ноду
Отключаем вторую ноду на RS2
Оставшаяся нода на RS2 падает в статус secondary
В кластере не работает даже чтение
Разрешаем чтение с secondary
```
db.getMongo().setReadPref('primaryPreferred')
```
Чтение начинает работать, но вставка не работает
```
MongoServerError: Write results unavailable from failing to target a host in the shard RS2 :: caused by :: Could not find host matching read preference { mode: "primary" } for set RS2
```
Включаем ноды обратно - все начинает работать, кластер приходит в консистентное состояние

## настроить аутентификацию и многоролевой доступ

Создаем пользователей в СУБД
```
db.createUser({
    user: "root",
    pwd: "123456",
    roles: ["root"]
})

db.createUser({
    user: "user",
    pwd: "654321",
    roles: [{role: "readWrite", db: "test"}]
})

db.createUser({
    user: "user2",
    pwd: "654321",
    roles: []
})
```

Генерируем ключевой файл 
```
openssl rand -base64 741 > mongodb-keyfile
chmod 600 mongodb-keyfile
```

Добавляем ключевой файл в параметры запуска всех контейнеров

Таким образом текущая конфигурация запуска ноды выглядит так:
  mongors1-3:
    image: 'mongo'
    container_name: mongo-rs1-3
    networks:
      - app-tier
    command: ["--shardsvr", "--replSet", "RS1", "--bind_ip_all", "--auth", "--keyFile","/opt/keyfile.key"]
    ports:
      - 37029:27018
    volumes:
      - ./db/rs1/db3:/data/db
      - ./keyfile.key:/opt/keyfile.key

Также добавляем ключ на mongos для того, чтобы работала внутрикластерная авторизация

При подключении без пароля - в доступе отказано
```
[direct: mongos] test> show dbs;
MongoServerError: Command listDatabases requires authentication
```

При подключении под пользователем user доступна только БД test
```
 ✘ avih@FreeBook  ~  mongosh --authenticationDatabase admin mongodb://user:654321@localhost:27017
(node:69616) [DEP0040] DeprecationWarning: The `punycode` module is deprecated. Please use a userland alternative instead.
(Use `node --trace-deprecation ...` to show where the warning was created)
Current Mongosh Log ID:	6538ee6c0b503581d1e5dc43
Connecting to:		mongodb://<credentials>@localhost:27017/?directConnection=true&serverSelectionTimeoutMS=2000&authSource=admin&appName=mongosh+2.0.2
Using MongoDB:		7.0.2
Using Mongosh:		2.0.2

For mongosh info see: https://docs.mongodb.com/mongodb-shell/

Warning: Found ~/.mongorc.js, but not ~/.mongoshrc.js. ~/.mongorc.js will not be loaded.
  You may want to copy or rename ~/.mongorc.js to ~/.mongoshrc.js.
[direct: mongos] test> show dbs
test  52.31 MiB
[direct: mongos] test>
```
Административные функции не доступны
```
[direct: mongos] test> sh.status()
MongoServerError: not authorized on config to execute command { find: "version", filter: {}, projection: { minCompatibleVersion: 0, excluding: 0, upgradeId: 0, upgradeState: 0 }, limit: 1, lsid: { id: UUID("8f1b410d-6375-44d5-a850-a933f289b081") }, $clusterTime: { clusterTime: Timestamp(1698229873, 1), signature: { hash: BinData(0, 4A70964314DA3446A475F6BE776EF69FC93B7AFE), keyId: 7293838488565186578 } }, $db: "config" }
```

При подключении под пользователем user2 доступные данные отсутствуют
