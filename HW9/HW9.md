# Задание

- Разворачиваем кластер Etcd любым способом. Проверяем отказоустойчивость
- Разворачиваем кластер Consul любым способом. Проверяем отказоустойчивость

## Решение

## Разворачиваем кластер Etcd любым способом. Проверяем отказоустойчивость
Поднимаем кластеры etcd на 3 нодах с помощью следующей конфигурации (полностью конфигурация приведена в docker-compose.yml)
```
etcd1:
    image: bitnami/etcd
    container_name: etcd1
    networks:
      - app-tier
    ports:
      - 12379:2379
      - 12380:2380
    environment:
      - ALLOW_NONE_AUTHENTICATION=yes
      - ETCD_NAME=etcd1
      - ETCD_AUTO_TLS=true
      - ETCD_ADVERTISE_CLIENT_URLS=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379
      - ETCD_INITIAL_ADVERTISE_PEER_URLS=http://etcd1:2380
      - ETCD_INITIAL_CLUSTER_TOKEN=cluster1
      - ETCD_INITIAL_CLUSTER=etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380
      - ETCD_INITIAL_CLUSTER_STATE=new
      - ETCD_LISTEN_PEER_URLS=http://0.0.0.0:2380
```

Из неочевидных сложностей с которыми пришлось столкнутся - необходимость указывать имя инстанса для каждого контейнера с помощью переменной окружения

Запрашиваем список нод:
```
etcdctl --endpoints=http://localhost:12379,http://localhost:22379,http://localhost:32379 member list
4ca9acff65f93a04, started, etcd3, http://etcd3:2380, http://etcd1:2379,http://etcd2:2379,http://etcd3:2379, false
edad0180f2cde681, started, etcd2, http://etcd2:2380, http://etcd1:2379,http://etcd2:2379,http://etcd3:2379, false
f62d62c1252a2721, started, etcd1, http://etcd1:2380, http://etcd1:2379,http://etcd2:2379,http://etcd3:2379, false
```

Сохраняем значение в базу

```
etcdctl --endpoints=http://localhost:12379,http://localhost:22379,http://localhost:32379 put test 2
OK
```

Отключаем одну ноду etcd

Все продолжает работать

Отключаем еще одну ноду и получаем ошибку в связи с отсутствием кворума

```
etcdctl --endpoints=http://localhost:12379,http://localhost:22379,http://localhost:32379 get test
{"level":"warn","ts":"2023-12-13T11:53:50.735683+0300","logger":"etcd-client","caller":"v3@v3.5.11/retry_interceptor.go:62","msg":"retrying of unary invoker failed","target":"etcd-endpoints://0x140000c6380/localhost:12379","attempt":0,"error":"rpc error: code = DeadlineExceeded desc = context deadline exceeded"}
Error: context deadline exceeded
```

Включаем ноды обратно

```
etcdctl --endpoints=http://localhost:12379,http://localhost:22379,http://localhost:32379 get test
test
2
```

Все работает

## Разворачиваем кластер Consul любым способом. Проверяем отказоустойчивость

Поднимаем кластер consul на 3 нодах с помощью следующей конфигурации (полностью конфигурация приведена в docker-compose.yml)

```
consul1:
    image: hashicorp/consul
    container_name: consul1
    volumes:
      - ./consul.json:/consul/config/server.json
      - ./etcd1.json:/consul/config/etcd-check.json
    networks:
      - app-tier
    ports:
      - 8500:8500
      - 8600:8600/tcp
      - 8600:8600/udp

```

Со следующей конфигурацией:

```
{
    "bind_addr": "0.0.0.0",
    "server": true,
    "enable_script_checks": true,
    "enable_local_script_checks": true,
    "retry_join": ["consul1","consul2","consul3"]
}

```

Для интереса добавим на каждую ноду проверку соответствующей ноды etcd:

```
{
    "service": {
        "id": "etcd1",
        "name": "etcd",
        "tags": [ "etcd" ],
        "port": 2379,
        "enable_tag_override": false,
        "check": {
            "id": "etcd_up",
            "name": "etcd healthcheck",
            "tcp": "etcd1:2379",
            "interval": "10s",
            "timeout": "2s"
        }
    }
}
```

Проверяем работоспособность

```
dig -p 8600 @localhost etcd.service.consul

; <<>> DiG 9.10.6 <<>> -p 8600 @localhost etcd.service.consul
; (2 servers found)
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 12021
;; flags: qr aa rd; QUERY: 1, ANSWER: 3, AUTHORITY: 0, ADDITIONAL: 1
;; WARNING: recursion requested but not available

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 4096
;; QUESTION SECTION:
;etcd.service.consul.		IN	A

;; ANSWER SECTION:
etcd.service.consul.	0	IN	A	172.19.0.4
etcd.service.consul.	0	IN	A	172.19.0.3
etcd.service.consul.	0	IN	A	172.19.0.6
```

Попробуем выключить одну из нод кластера

Кластер продолжает работать,  но после отключения ноды consul соответствующая нода etcd из выдачи DNS также была удалена
```
dig -p 8600 @localhost etcd.service.consul

; <<>> DiG 9.10.6 <<>> -p 8600 @localhost etcd.service.consul
; (2 servers found)
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 29477
;; flags: qr aa rd; QUERY: 1, ANSWER: 2, AUTHORITY: 0, ADDITIONAL: 1
;; WARNING: recursion requested but not available

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 4096
;; QUESTION SECTION:
;etcd.service.consul.		IN	A

;; ANSWER SECTION:
etcd.service.consul.	0	IN	A	172.19.0.4
etcd.service.consul.	0	IN	A	172.19.0.6

;; Query time: 0 msec
;; SERVER: ::1#8600(::1)
;; WHEN: Wed Dec 13 15:17:47 MSK 2023
;; MSG SIZE  rcvd: 80

```

После включения ноды назад функциональность кластера была полностью восстановлена


