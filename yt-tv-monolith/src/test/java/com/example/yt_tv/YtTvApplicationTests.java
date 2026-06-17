package com.example.yt_tv;

import com.example.yt_tv.tools.SearchQueryParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class YtTvApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void parseDetectsCategories() {
        var plan = SearchQueryParser.parse("play some science videos");
        assertThat(plan.categories()).contains("Science");
    }

    @Test
    void parseDetectsChannels() {
        var plan = SearchQueryParser.parse("videos from Veritasium and Astrum");
        assertThat(plan.channels()).contains("Veritasium", "Astrum");
    }

    @Test
    void parseDefaultsQueryWhenBlank() {
        var plan = SearchQueryParser.parse("   ");
        assertThat(plan.query()).isEqualTo("videos");
    }
}
