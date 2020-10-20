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
                    default:
                        return OTHER;
                }
            }
        }
    };

    private static final Transition transitions[] = {
            /* 0 */ new Transition(0, VariableTokenizer::append),
            /* 1 */ new Transition(1, VariableTokenizer::append),
            /* 2 */ new Transition(0, VariableTokenizer::append),
            /* 3 */ new Transition(2, VariableTokenizer::vbgn),
            /* 4 */ new Transition(3, VariableTokenizer::append),
            /* 5 */ new Transition(3, VariableTokenizer::append),
            /* 6 */ new Transition(4, VariableTokenizer::error),
            /* 7 */ new Transition(0, VariableTokenizer::vend),
            /* 8 */ new Transition( 5,VariableTokenizer::eos)
    };

    /*
            +----------------------------------------------------------------+
            |                            RBRACE 7                            |
            v                                                                |
        +---+---+               +-------+           +-------+           +----+---+
        |       |   DASH  1     |       | LBRACE 3  |       |  ALPHA  4 |        +------+
        | Start +--------------->   1   +---------->+  2    +---------->+   3    |      | ALPHA or DIGIT 5
        |       <---------------+       |           |       |           |        +<-----+
        +-+---+-+    ALPHA  2   +-------+           +--+----+           +----+---+
          ^   |      DIGIT                             |                     |
          | 0 |      DASH                              |  6                  | 6
          +---+      RBRACE                            |    +---------+      |
                     OTHER                             |    |         |      |
          ALPHA                                        +--->+ ERROR   +<-----+
          DIGIT                                             |         |
          LBRACE                                            +---------+
          RBRACE
          OTHER

     */
    private static final int matrix[][] = {
                    /*  ALPHA   DIGIT   DASH    LBRACE  RBRACE  OTHER   END*/
            /* S */ {    0,     0,      1,      0,      0,      0,      8},
            /* 1 */ {    2,     2,      2,      3,      2,      2,      8},
            /* 2 */ {    4,     6,      6,      6,      6,      6,      6},
            /* 3 */ {    5,     5,      6,      6,      7,      6,      6},
            /* E */ {    6,     6,      6,      6,      6,      6,      6}
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
