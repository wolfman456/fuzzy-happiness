package com.gamer.fowever.tabletopservice.support;

import com.gamer.fowever.tabletopapi.support.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiceExpressionParserTest {

    @Test
    void parsesCountSidesAndModifier() {
        DiceExpressionParser.ParsedDice dice = DiceExpressionParser.parse("2d6+3");
        assertThat(dice.count()).isEqualTo(2);
        assertThat(dice.sides()).isEqualTo(6);
        assertThat(dice.modifier()).isEqualTo(3);
        assertThat(dice.normalized()).isEqualTo("2d6+3");
    }

    @Test
    void defaultsSingleDieAndNegativeModifier() {
        DiceExpressionParser.ParsedDice dice = DiceExpressionParser.parse("d20");
        assertThat(dice.count()).isEqualTo(1);
        assertThat(dice.sides()).isEqualTo(20);
        assertThat(dice.modifier()).isZero();

        DiceExpressionParser.ParsedDice minus = DiceExpressionParser.parse("1d4-2");
        assertThat(minus.normalized()).isEqualTo("d4-2");
    }

    @Test
    void acceptsCaseInsensitiveNontrivialGrammar() {
        DiceExpressionParser.ParsedDice dice = DiceExpressionParser.parse("20D100");
        assertThat(dice.count()).isEqualTo(20);
        assertThat(dice.sides()).isEqualTo(100);
        assertThat(dice.modifier()).isZero();
    }

    @Test
    void rejectsNullOrGarbledExpressions() {
        assertThatThrownBy(() -> DiceExpressionParser.parse(null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() -> DiceExpressionParser.parse("hello"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported dice expression");
        assertThatThrownBy(() -> DiceExpressionParser.parse("2d6+1d4"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported dice expression");
        assertThatThrownBy(() -> DiceExpressionParser.parse("d"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsTooManyDice() {
        assertThatThrownBy(() -> DiceExpressionParser.parse("21d6"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("between 1 and 20");
    }

    @Test
    void rejectsOutOfRangeSides() {
        assertThatThrownBy(() -> DiceExpressionParser.parse("d1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("between 2 and 999");
        assertThatThrownBy(() -> DiceExpressionParser.parse("d1000"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported dice expression");
    }

    @Test
    void rejectsOutOfRangeModifier() {
        assertThatThrownBy(() -> DiceExpressionParser.parse("d20+101"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("between -100 and +100");
        assertThatThrownBy(() -> DiceExpressionParser.parse("d20-101"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("between -100 and +100");
    }
}