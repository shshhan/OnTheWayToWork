package com.shawn.notification.domain;

import com.shawn.notification.SeoulMetroFinder;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@ActiveProfiles("local")
@SpringBootTest()
public class SeoulMetroFinderTest {

    @Autowired
    private SeoulMetroFinder seoulMetroFinder;

    private LocalDate today = LocalDate.of(2022,9,28);
    private LocalDateTime now = LocalDateTime.of(2022,9,28,8,0);

    @Test
    public void crawlTitle() throws IOException {
        seoulMetroFinder.crawlTitlesByDate(today, now).forEach(title -> {
            System.out.println(title);
            assertThat(title.html()).contains("운행 지연");
            assertThat(title.html()).contains(String.valueOf(today.getMonthValue()));
            assertThat(title.html().contains(String.valueOf(today.getDayOfMonth())));
        });
    }

    @Test
    public void isSoon() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String elHtml = "열차운행 지연 예정 안내(9/26~30)";

        Method isSoon = seoulMetroFinder
                .getClass()
                .getDeclaredMethod("isSoon", String.class, LocalDate.class, LocalDateTime.class);
        isSoon.setAccessible(true);

        assertThat(isSoon.invoke(seoulMetroFinder, elHtml, today, now)).isEqualTo(true);
    }

    @Test
    public void periodFormatter() {
        List<LocalDate> list1 = seoulMetroFinder.periodFormatter("9/ 01", today);
        assertThat(list1.size()).isEqualTo(1);
        assertThat(list1.get(0)).isEqualTo(LocalDate.of(2022,9,1));

        List<LocalDate> list2 = seoulMetroFinder.periodFormatter("09/20 ~ 22", today);
        assertThat(list2.size()).isEqualTo(3);
        for(int i=0; i<list2.size(); i++){
            assertThat(list2.get(i)).isEqualTo(LocalDate.of(2022,9,20+i));
        }

        List<LocalDate> list3 = seoulMetroFinder.periodFormatter("9/15~09/19", today);
        assertThat(list3.size()).isEqualTo(5);
        for(int i=0; i<list3.size(); i++){
            assertThat(list3.get(i)).isEqualTo(LocalDate.of(2022,9,15+i));
        }

        List<LocalDate> list4 = seoulMetroFinder.periodFormatter("09/1, 9/3", today);
        assertThat(list4.size()).isEqualTo(2);
        assertThat(list4.get(0)).isEqualTo(LocalDate.of(2022,9,1));
        assertThat(list4.get(1)).isEqualTo(LocalDate.of(2022,9,3));

    }

    @Test
    public void crawlBody() throws IOException {
        Element title = new Element("a");
        title.attr("title", "열차운행 지연 예정 안내(9/26~30)");
        title.attr("href", "board.do?menuIdx=546&bbsIdx=2214548");
        title.text("열차운행 지연 예정 안내(9/26~30)") ;

        List<Element> pList = seoulMetroFinder.crawlBody(title);
        pList.forEach(p -> System.out.println(p.text()));

        assertThat(pList.get(0).text()).contains("서울교통공사에서 알려드립니다.");
        assertThat(pList.get(1).text()).startsWith("9월");

    }

    @Test
    public void crawlInformation() {
        List<Element> pList = new ArrayList<>();
        pList.add(new Element("p").text("열차운행 지연"));
        pList.add(new Element("p").text(" 5호선"));
        pList.add(new Element("p").text(", 7호선"));

        assertThat(seoulMetroFinder.crawlInformation(pList)).contains("열차");
        assertThat(seoulMetroFinder.crawlInformation(pList)).contains("5호선");
        assertThat(seoulMetroFinder.crawlInformation(pList)).contains("7");

    }

}