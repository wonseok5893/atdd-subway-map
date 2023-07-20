package subway.section;

import io.restassured.RestAssured;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import subway.line.LineRequest;
import subway.line.LineResponse;
import subway.linesection.LineSectionRepository;
import subway.linesection.LineSectionRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static subway.line.LineFixture.지하철_노선_생성_ID;
import static subway.line.LineFixture.지하철_노선_조회;
import static subway.station.StationFixture.지하철역_생성_ID;

@DisplayName("지하철 구간 관련 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LineSectionAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    LineSectionRepository lineSectionRepository;

    private Long firstStationId;
    private Long secondStationId;
    private Long thirdStationId;
    private Long fourthStationId;

    private Long fistLineId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        firstStationId = 지하철역_생성_ID("노원역");
        secondStationId = 지하철역_생성_ID("창동역");
        thirdStationId = 지하철역_생성_ID("강남역");
        fourthStationId = 지하철역_생성_ID("사당역");

        fistLineId = 지하철_노선_생성_ID(LineRequest.builder()
                .name("4호선")
                .color("light-blue")
                .upStationId(firstStationId)
                .downStationId(secondStationId)
                .distance(10)
                .build());
    }

    /**
     * Given 지하철역이 4개가 등록되어있다.
     * Given 지하철 노선이 1개가 등록되어있다. (노선 (Station: [first,second])
     * Given 새로운 구간 요청(지하철 노선의 하행 종점 역 second , 노선에 존재하지 않는 역 third)
     * When 지하철 노선에 구간을 등록 요청한다
     * Then 지하철 노선 목록에 구간의 하행역 B가 추가 되었는지 확인한다.
     * And 지하철 노선
     */
    @DisplayName("지하찰_구간_등록")
    @Test
    void createStation_case_0() {
        //when
        지하철_구간_생성(fistLineId, 구간_생성_요청서(secondStationId, thirdStationId));
        //then
        LineResponse response = 지하철_노선_조회(fistLineId);
        assertThat(response.getStations().size()).isEqualTo(3);
        assertThat(response.getStations().get(2).getId()).isEqualTo(thirdStationId);
    }

    /**
     * Given 지하철역이 4개가 등록되어있다.
     * And 지하철 노선이 1개가 등록되어있다.
     * And 지하철 노선에 1개의 구간을 추가해둔다.(노선 (Station: [first,second,third])
     * Given 새로운 구간 요청(지하철 노선의 하행 종점 역 third , 노선에 존재하지 않는 역 fourth)
     * When 지하철 노선에 구간을 등록 요청한다
     * Then 지하철 노선 목록에 구간의 하행역 fourth가 추가 되었는지 확인한다.
     * And 지하철 노선
     */
    @DisplayName("지하찰_구간_등록 - 복수 등록")
    @Test
    void createStation_case_1() {
        //given
        지하철_구간_생성(fistLineId, 구간_생성_요청서(secondStationId, thirdStationId));
        //when
        지하철_구간_생성(fistLineId, 구간_생성_요청서(thirdStationId, fourthStationId));
        //then
        LineResponse response = 지하철_노선_조회(fistLineId);
        assertThat(response.getStations().size()).isEqualTo(4);
        assertThat(response.getStations().get(3).getId()).isEqualTo(fourthStationId);
    }

    /**
     * Given 지하철역이 4개가 등록되어있다.
     * Given 지하철 노선이 1개가 등록되어있다.  (노선 (Station: [first,second])
     * Given 새로운 구간 요청(지하철 노선의 하행 종점 역이 아닌 third , 노선에 존재하지 않는 역 fourth)
     * When 지하철 노선에 구간을 등록 요청한다
     * Then 지하철 구간이 등록이 되지 않고 예외를 발생시킨다.
     */
    @DisplayName("지하찰_구간_등록 - 새로운 구간의 상행역은 해당 노선에 등록되어있는 하행 종점역이어야 한다.")
    @Test
    void createStation_case_2() {
        //when
        //then
        지하철_구간_생성_응답_상태값_체크(fistLineId, 구간_생성_요청서(thirdStationId, fourthStationId), HttpStatus.BAD_REQUEST);
    }

    /**
     * Given 지하철역이 4개가 등록되어있다.
     * Given 지하철 노선이 1개가 등록되어있다. (노선 (Station: [first,second])
     * Given 새로운 구간 요청(지하철 노선의 하행 종점 역 second , 노선에 존재하는 역 first)
     * When 지하철 노선에 구간을 등록 요청한다
     * Then 지하철 구간이 등록이 되지 않고 예외를 발생시킨다.
     */
    @DisplayName("지하찰_구간_등록 - 새로운 구간의 하행역은 해당 노선에 등록되어있는 역일 수 없다.")
    @Test
    void createStation_case_3() {
        //when
        //then
        지하철_구간_생성_응답_상태값_체크(fistLineId, 구간_생성_요청서(secondStationId, firstStationId), HttpStatus.BAD_REQUEST);

    }

    /**
     * Given 지하철역이 4개가 등록되어있다.
     * And 지하철 노선이 1개가 등록되어있다.
     * And 지하철 노선에 구간 2개를 추가해둔다. (노선 (Station: [first,second,third,fourth])
     * When 지하철 구간을 제거한다.
     * then 지하철 노선에서 해당 구간이 제거되어있는지 확인한다.
     */
    @DisplayName("지하찰_구간_삭제")
    @Test
    void deleteStation_case_0() {
        //given
        지하철_구간_생성(fistLineId, 구간_생성_요청서(secondStationId, thirdStationId));
        지하철_구간_생성(fistLineId, 구간_생성_요청서(thirdStationId, fourthStationId));
        //when
        지하철_구간_삭제(fistLineId, fourthStationId);
        //then
        LineResponse response = 지하철_노선_조회(fistLineId);
        assertThat(response.getStations().size()).isEqualTo(3);
        assertThat(response.getStations().get(2).getId()).isEqualTo(thirdStationId);
    }

    /**
     * Given 지하철역이 4개가 등록되어있다.
     * And 지하철 노선이 1개가 등록되어있다.
     * And 지하철 노선에 구간 2개를 추가해둔다. (노선 (Station: [first,second,third,fourth])
     * When 지하철 구간을 제거한다.
     * Then 지하철 구간이 제거 되지 않고 예외를 발생시킨다.
     */
    @DisplayName("지하찰_구간_삭제 - 지하철 노선에 등록된 역(하행 종점역)만 제거할 수 있다. 즉, 마지막 구간만 제거할 수 있다.")
    @Test
    void deleteStation_case_1() {
        //given
        지하철_구간_생성(fistLineId, 구간_생성_요청서(secondStationId, thirdStationId));
        지하철_구간_생성(fistLineId, 구간_생성_요청서(thirdStationId, fourthStationId));
        //when
        지하철_구간_삭제_응답_상태값_체크(fistLineId, thirdStationId, HttpStatus.BAD_REQUEST);
        //then
    }

    /**
     * Given 지하철역이 4개가 등록되어있다.
     * And 지하철 노선이 1개가 등록되어있다. (노선 (Station: [first,second])
     * When 지하철 구간을 제거한다.
     * Then 지하철 구간이 제거 되지 않고 예외를 발생시킨다.
     */
    @DisplayName("지하찰_구간_삭제 - 지하철 노선에 상행 종점역과 하행 종점역만 있는 경우(구간이 1개인 경우) 역을 삭제할 수 없다.")
    @Test
    void deleteStation_case_2() {
        //when
        //then
        지하철_구간_삭제_응답_상태값_체크(fistLineId, secondStationId, HttpStatus.BAD_REQUEST);
    }

    private void 지하철_구간_삭제(Long lineId, Long stationId) {
        RestAssured.given().log().all()
                .when()
                .param("stationId", stationId)
                .delete(String.format("/lines/%d/sections", lineId))
                .then().log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    private void 지하철_구간_삭제_응답_상태값_체크(Long lineId, Long stationId, HttpStatus expected) {
        RestAssured.given().log().all()
                .when()
                .param("stationId", stationId)
                .delete(String.format("/lines/%d/sections", lineId))
                .then().log().all()
                .statusCode(expected.value());
    }

    private void 지하철_구간_생성_응답_상태값_체크(Long lineId, LineSectionRequest request, HttpStatus expected) {
        RestAssured.given().log().all()
                .when()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .post(String.format("/lines/%d/sections", lineId))
                .then().log().all()
                .statusCode(expected.value());
    }

    private void 지하철_구간_생성(Long lineId, LineSectionRequest request) {
        RestAssured.given().log().all()
                .when()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .post(String.format("/lines/%d/sections", lineId))
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());
    }

    private LineSectionRequest 구간_생성_요청서(Long startStationId, Long endStationId) {
        LineSectionRequest request = new LineSectionRequest(startStationId, endStationId, RandomUtils.nextInt(1, 10));
        return request;
    }

}