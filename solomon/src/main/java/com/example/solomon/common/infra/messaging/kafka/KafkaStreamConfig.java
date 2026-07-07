package com.example.solomon.common.infra.messaging.kafka;

@Configuration
public class KafkaStreamsConfig {

    @Bean
    public KStream<String, MyPayload> kStream(StreamsBuilder streamsBuilder) {
        // MyPayload 클래스를 위한 JsonSerde 생성
        JsonSerde<MyPayload> myPayloadSerde = new JsonSerde<>(MyPayload.class);

        // 데이터를 읽어올 때 (Deserialize) JSON Serde 지정
        KStream<String, MyPayload> stream = streamsBuilder.stream("input-topic",
                Consumed.with(Serdes.String(), myPayloadSerde));

        // 간단한 필터링 로직 예시
        KStream<String, MyPayload> filteredStream = stream.filter(
                (key, value) -> value != null && value.getAge() >= 20);

        // 데이터를 내보낼 때 (Serialize) JSON Serde 지정
        filteredStream.to("output-topic",
                Produced.with(Serdes.String(), myPayloadSerde));

        return filteredStream;
    }
}
