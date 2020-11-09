package alien4cloud.var;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class VariableTokenizer {

    @FunctionalInterface
    private interface TransitionFunc {
            void apply(char c, VariableTokenizer lexer) throws TokenizerException;
    };

    private static class Transition {
        int next;

        TransitionFunc func;

        Transition(int next,TransitionFunc func) {
            this.next = next;
            this.func = func;
        }
    }

    private int state = 0;

    private StringBuffer buffer = new StringBuffer();

    private List<AbstractToken> tokens = Lists.newArrayList();

    private VariableTokenizer() {
    }

    private static enum Atom {
        ALPHA,
        DIGIT,
        DASH,
        LBRACE,
        RBRACE,
        UNDERSCORE,
        DOT,
        OTHER,
        END;

        public static Atom toAtom(char c) {
            if ((c >= 'a' && c <= 'z') || (c >='A' && c <= 'Z')) {
                return ALPHA;
            } else if (Character.isDigit(c)) {
                return DIGIT;
            } else {
                switch(c) {
                    case '#': return DASH;
                    case '{': return LBRACE;
                    case '}': return RBRACE;
                    case '.': return DOT;
                    case '_': return UNDERSCORE;
                    default:
                        return OTHER;
                }
            }
        }
    };

    private static final Transition transitions[] = {
            /*  0 */ new Transition(0, VariableTokenizer::append),
            /*  1 */ new Transition(1, VariableTokenizer::append),
            /*  2 */ new Transition(0, VariableTokenizer::append),
            /*  3 */ new Transition(2, VariableTokenizer::vbgn),
            /*  4 */ new Transition(3, VariableTokenizer::append),
            /*  5 */ new Transition(3, VariableTokenizer::append),
            /*  6 */ new Transition(0, VariableTokenizer::vend),
            /*  7 */ new Transition( 4,VariableTokenizer::append),
            /*  8 */ new Transition(5,VariableTokenizer::append),
            /*  9 */ new Transition( 0,VariableTokenizer::vend),
            /* 10 */ new Transition( 5,VariableTokenizer::eos),
            /* 11 */ new Transition( 7,VariableTokenizer::error)
    };
    /*
                                         0:
                                      +----------+
                                      |          |
                                      |          |
                                +-----|----------v----+                         9: }
                                |                     <-------------------------------------------------------------+
    +--------------------------->        START(0)     |    10: :end:                                                |
    |                           |                     -----------+                                                  |
    |                           +-----^----------|----+          |                                                  |
    |                                 |          |             +-v----+                                             |
    |                            2:   |          | 1: #        |      |                                  +----------|----------+
    |                                 |          |             | END  |              11:                 |                     <-----+
    |                                 |          |             | (7)  |        +--------------------------         5           |     | 8: :alpha: :digit: _
    |                           +-----|----------v----+        +-^----+        |                         |                     ------+
    |                           |                     |          |             |                         +-----|----------^----+
    |                           |         1           |          |             |                               |          |
    |                           |                     -----------+             |                               |          |
    |                           +---------------------+  10: :end:             |                               |          |
    |                                      |                        +----------v----------+                    |          |
    |                                      | 3: {                   |                     |               7: . |          | 8: :alpha: :digit: _
    | 6: }                                 |                        |         E(6)        |                    |          |
    |                                      |                        |                     |                    |          |
    |                           +----------v----------+             +-----^----^-----^----+                    |          |
    |                           |                     |                   |    |     |                         |          |
    |                           |         2           |                   |    |     |                   +-----v----------|----+
    |                           |                     --------------------+    |     |                   |                     |
    |                           +----------|----------+       11:              |     +--------------------         4           |
    |                                      |                                   |         11:             |                     |
    |                                      | 4: :alpha:                        |                         +----------^----------+
    |                                      |                                   |                                    |
    |                                      |                                   |                                    |
    |                           +----------v----------+        11:             |                                    |
    |                           |                     |------------------------+                                    |
    +----------------------------         3           |                                                             |
                                |                     |-------------------------------------------------------------+
                                +-----|----------^----+                           7: .
                                      |          |
                                      |          |
                                      +----------+
                                  5: :alpha: :digit: _
     */
    private static final int matrix[][] = {
                    /*  ALPHA   DIGIT   DASH    LBRACE  RBRACE  UNDERSCORE  DOT     OTHER   END*/
            /* S */ {    0,     0,      1,      0,      0,      0,          0,      0,      10},
            /* 1 */ {    2,     2,      2,      3,      2,      2,          2,      2,      10},
            /* 2 */ {    4,     11,     11,     11,     11,     11,         11,    11,      11},
            /* 3 */ {    5,     5,      11,     11,     6,      5,          7,     11,      11},
            /* 4 */ {    8,     8,      11,     11,     11,     8,          11,    11,      11},
            /* 5 */ {    8,     8,      11,     11,     9,      8,          7,     11,      11},
            /* E */ {    11,    11,     11,     11,     11,     11,         11,    11,      11}
    };

    private static void append(char c, VariableTokenizer lexer) {
        lexer.buffer.append(c);
    }

    private static void vbgn(char c, VariableTokenizer lexer) {
        if (lexer.buffer.length() > 1) {
            lexer.tokens.add(new ScalarToken(lexer.buffer.deleteCharAt(lexer.buffer.length()-1).toString()));
        }

        lexer.buffer = new StringBuffer();
    }

    private static void vend(char c, VariableTokenizer lexer) {
        lexer.tokens.add(new VariableToken(lexer.buffer.toString()));
        lexer.buffer = new StringBuffer();
    }

    private static void eos(char c, VariableTokenizer lexer) {
        if (lexer.buffer.length() > 0) {
            lexer.tokens.add(new ScalarToken(lexer.buffer.toString()));
        }
    }

    private static void error(char c, VariableTokenizer lexer) throws TokenizerException {
        throw new TokenizerException();
    }

    public static List<AbstractToken> tokenize(String value) throws TokenizerException {
        VariableTokenizer lexer = new VariableTokenizer();

        for (int i = 0; i < value.length() ; i++) {
            Atom a = Atom.toAtom(value.charAt(i));
            Transition transition = transitions[matrix[lexer.state][a.ordinal()]];

            // Call transition func
            transition.func.apply(value.charAt(i),lexer);

            // Switch state
            lexer.state = transition.next;
        }

        // Finally The End Transition
        Transition transition = transitions[matrix[lexer.state][Atom.END.ordinal()]];
        transition.func.apply('\0',lexer);

        return lexer.tokens;
    }
}
