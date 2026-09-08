package com.gamer.fowever.tabletopservice.support;

import com.gamer.fowever.tabletopapi.support.ApiException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiceExpressionParser {

    private static final Pattern EXPRESSION = Pattern.compile("^(\\d*)[dD](\\d{1,3})([+-]\\d{1,3})?$");
    private static final int MAX_DICE = 20;
    private static final int MIN_SIDES = 2;
    private static final int MAX_SIDES = 999;
    private static final int MAX_MODIFIER = 100;

    private DiceExpressionParser() {
    }

    public static ParsedDice parse(String expression) {
        if (expression == null) {
            throw ApiException.badRequest("Dice expression is required");
        }
        Matcher matcher = EXPRESSION.matcher(expression.trim());
        if (!matcher.matches()) {
            throw ApiException.badRequest(
                    "Unsupported dice expression \"" + expression.trim()
                            + "\" - expected a single die type like 2d6, d20 or 3d8+2 with up to "
                            + MAX_DICE + " dice, " + MAX_SIDES + " sides and a modifier between -"
                            + MAX_MODIFIER + " and +" + MAX_MODIFIER);
        }
        int count = matcher.group(1).isEmpty() ? 1 : Integer.parseInt(matcher.group(1));
        int sides = Integer.parseInt(matcher.group(2));
        int modifier = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));

        if (count < 1 || count > MAX_DICE) {
            throw ApiException.badRequest("You can roll between 1 and " + MAX_DICE + " dice");
        }
        if (sides < MIN_SIDES || sides > MAX_SIDES) {
            throw ApiException.badRequest(
                    "A die must have between " + MIN_SIDES + " and " + MAX_SIDES + " sides");
        }
        if (modifier < -MAX_MODIFIER || modifier > MAX_MODIFIER) {
            throw ApiException.badRequest(
                    "The modifier must be between -" + MAX_MODIFIER + " and +" + MAX_MODIFIER);
        }
        return new ParsedDice(count, sides, modifier);
    }

    public record ParsedDice(int count, int sides, int modifier) {

        public String normalized() {
            StringBuilder builder = new StringBuilder();
            if (count != 1) {
                builder.append(count);
            }
            builder.append('d').append(sides);
            if (modifier > 0) {
                builder.append('+').append(modifier);
            } else if (modifier < 0) {
                builder.append(modifier);
            }
            return builder.toString();
        }
    }
}