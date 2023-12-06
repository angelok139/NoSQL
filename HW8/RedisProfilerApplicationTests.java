package ru.otus.redisprofiler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.zset.Tuple;
import ru.otus.redisprofiler.dto.DataDto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@SpringBootTest
class RedisProfilerApplicationTests {
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private ObjectMapper mapper = new ObjectMapper();

    private final int SAMPLES = 100_000;

    @BeforeEach
    void flushDB() {
        redisConnectionFactory.getConnection().serverCommands().flushDb();
    }


    @Test
    void testRedisWriteByString() throws JsonProcessingException {
        var connection = redisConnectionFactory.getConnection();
        System.out.println("===== Start string test write =====");

        var startTest = LocalDateTime.now();

        for(Integer i = 0; i< SAMPLES; i++) {
            var dataDtoString = mapper.writeValueAsString(new DataDto());
            connection.stringCommands().set(i.toString().getBytes(), dataDtoString.getBytes());
        }

        var endTest = LocalDateTime.now();

        System.out.println("==== TIME: " + Duration.between(startTest, endTest).getSeconds() + " s");
        System.out.println("===== End string test write =====");

        System.out.println("===== Start string test read =====");
        startTest = LocalDateTime.now();

        for(Integer i = 0; i< SAMPLES; i++) {
            var tmp = connection.stringCommands().get(i.toString().getBytes());
        }

        endTest = LocalDateTime.now();
        System.out.println("==== TIME: " + Duration.between(startTest, endTest).getSeconds() + " s");
        System.out.println("===== End string test read =====");

    }


    @Test
    void testRedisWriteByHSet() {
        var connection = redisConnectionFactory.getConnection();
        System.out.println("===== Start HSET test write =====");

        var startTest = LocalDateTime.now();

        for(Integer i = 0; i< SAMPLES; i++) {
            var dataDto = new DataDto();
            connection.hashCommands().hSet(i.toString().getBytes(), "data1".getBytes(), dataDto.getData1().getBytes());
            connection.hashCommands().hSet(i.toString().getBytes(), "data2".getBytes(), dataDto.getData2().getBytes());
        }

        var endTest = LocalDateTime.now();

        System.out.println("==== TIME: " + Duration.between(startTest, endTest).getSeconds() + " s");
        System.out.println("===== End HSET test write =====");

        System.out.println("===== Start HSET test read =====");
        startTest = LocalDateTime.now();

        for(Integer i = 0; i< SAMPLES; i++) {
            var dataDto = new DataDto();
            dataDto.setData1(new String(connection.hashCommands().hGet(i.toString().getBytes(),"data1".getBytes())));
            dataDto.setData2(new String(connection.hashCommands().hGet(i.toString().getBytes(),"data2".getBytes())));
        }

        endTest = LocalDateTime.now();
        System.out.println("==== TIME: " + Duration.between(startTest, endTest).getSeconds() + " s");
        System.out.println("===== End HSET test read =====");

    }

    @Test
    void testRedisWriteByZSet() {
        var connection = redisConnectionFactory.getConnection();
        System.out.println("===== Start ZSET test write =====");

        var startTest = LocalDateTime.now();

        for(Integer i = 0; i< SAMPLES; i++) {
            var dataDto = new DataDto();
            connection.zSetCommands().zAdd(i.toString().getBytes(), Set.of(
                    Tuple.of(dataDto.getData1().getBytes(),1.),
                    Tuple.of(dataDto.getData2().getBytes(), 2.)));

        }

        var endTest = LocalDateTime.now();

        System.out.println("==== TIME: " + Duration.between(startTest, endTest).getSeconds() + " s");
        System.out.println("===== End ZSET test write =====");

        System.out.println("===== Start ZSET test read =====");
        startTest = LocalDateTime.now();

        for(Integer i = 0; i< SAMPLES; i++) {
            var dataDto = new DataDto();
            dataDto.setData1(new String(connection.zSetCommands().zPopMin(i.toString().getBytes()).getValue()));
            dataDto.setData2(new String(connection.zSetCommands().zPopMin(i.toString().getBytes()).getValue()));
        }

        endTest = LocalDateTime.now();
        System.out.println("==== TIME: " + Duration.between(startTest, endTest).getSeconds() + " s");
        System.out.println("===== End ZSET test read =====");

    }

    @Test
    void testRedisWriteByList() {
        var connection = redisConnectionFactory.getConnection();
        System.out.println("===== Start List test write =====");

        var startTest = LocalDateTime.now();

        for(Integer i = 0; i< SAMPLES; i++) {
            var dataDto = new DataDto();
            connection.listCommands().rPush(i.toString().getBytes(),
                    dataDto.getData1().getBytes(), dataDto.getData2().getBytes());

        }

        var endTest = LocalDateTime.now();

        System.out.println("==== TIME: " + Duration.between(startTest, endTest).getSeconds() + " s");
        System.out.println("===== End List test write =====");

        System.out.println("===== Start List test read =====");
        startTest = LocalDateTime.now();

        for(Integer i = 0; i< SAMPLES; i++) {
            var dataDto = new DataDto();
            dataDto.setData1(new String(connection.listCommands().lPop(i.toString().getBytes())));
            dataDto.setData2(new String(connection.listCommands().lPop(i.toString().getBytes())));
        }

        endTest = LocalDateTime.now();
        System.out.println("==== TIME: " + Duration.between(startTest, endTest).getSeconds() + " s");
        System.out.println("===== End List test read =====");

    }

}
