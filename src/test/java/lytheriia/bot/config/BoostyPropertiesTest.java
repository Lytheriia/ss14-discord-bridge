package lytheriia.bot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BoostyPropertiesTest {

    @ParameterizedTest
    @CsvSource({ "0,0", "1,0", "2,1", "3,3", "4,0", "-1,0" })
    void tokensForRankUsesPositionAndIgnoresUnknownRanks(int rank, int expected) {
        assertThat(new BoostyProperties(List.of(0, 1, 3)).tokensForRank(rank)).isEqualTo(expected);
    }

    @Test
    void missingListMeansNoTokens() {
        assertThat(new BoostyProperties(null).tokensForRank(1)).isZero();
    }

    @Test
    void rejectsEmptyAndNegativeValues() {
        assertThatThrownBy(() -> new BoostyProperties(Arrays.asList(0, null, 3)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoostyProperties(List.of(0, -1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
