package com.gamer.fowever.tabletopapi.dto;

import java.util.List;

/**
 * Result of compiling a draft: either field-level violations (no sheet) or the
 * validated sheet preview.
 */
public record CompileResult(boolean valid, List<String> violations, CharacterSheetDto sheet) {

    public static CompileResult invalid(List<String> violations) {
        return new CompileResult(false, List.copyOf(violations), null);
    }

    public static CompileResult ok(CharacterSheetDto sheet) {
        return new CompileResult(true, List.of(), sheet);
    }
}