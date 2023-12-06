# Задание

Необходимо:

- сохранить большой жсон (~20МБ) в виде разных структур - строка, hset, zset, list;
- протестировать скорость сохранения и чтения;
- предоставить отчет.


## Решение

На основании анализа ДЗ я пришел к выводу, что очевидно подразумевается сохранение данных в формате разных типов в Redis и их последующее чтение. Мной был разработан класс DTO с двумя текстовыми полями (приложен в файле DataDto.java), со значениями, генерируемыми в момент создания и простой тест сохраняющий и читающий этот класс 100_000 раз в 4 разных форматах. Для реализации был выбран знакомый мне язык Java и фреймворк Sping Boot + Spring Data Redis (без использования репозиториев, только connection). Текст теста приведен в файле RedisProfilerApplicationTests.java, все остальные файлы стандартные и интереса не представляют.

В результате выполнения теста были получены следующие результаты:
```
===== Start HSET test write =====
==== TIME: 25 s
===== End HSET test write =====
===== Start HSET test read =====
==== TIME: 23 s
===== End HSET test read =====
===== Start List test write =====
==== TIME: 13 s
===== End List test write =====
===== Start List test read =====
==== TIME: 24 s
===== End List test read =====
===== Start ZSET test write =====
==== TIME: 13 s
===== End ZSET test write =====
===== Start ZSET test read =====
==== TIME: 25 s
===== End ZSET test read =====
===== Start string test write =====
==== TIME: 12 s
===== End string test write =====
===== Start string test read =====
==== TIME: 12 s
===== End string test read =====

```

P.S. От себя могу добавить, что задание было полезно, но я бы его сформулировал более точно