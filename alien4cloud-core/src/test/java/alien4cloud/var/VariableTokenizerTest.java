package alien4cloud.var;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
public class VariableTokenizerTest {

    @Test(expected = TokenizerException.class)
    public void token_except0() throws TokenizerException {
       List<AbstractToken> tokens = VariableTokenizer.tokenize("#{");
    }

    @Test(expected = TokenizerException.class)
    public void token_except1() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{2error}");
    }

    @Test(expected = TokenizerException.class)
    public void token_except2() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{}");
    }

    @Test(expected = TokenizerException.class)
    public void token_except3() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{#}");
    }

    @Test(expected = TokenizerException.class)
    public void token_except4() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{héllö}");
    }

    @Test(expected = TokenizerException.class)
    public void token_except5() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{_error}");
    }

    @Test(expected = TokenizerException.class)
    public void token_except6() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{aa{");
    }

    @Test(expected = TokenizerException.class)
    public void token_except7() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{aa.}");
    }

    @Test(expected = TokenizerException.class)
    public void token_except8() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{0.a}");
    }

    @Test
    public void token0() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{hello}#{world}");

        assertThat(tokens,hasSize(2));
        assertThat(tokens.get(0),instanceOf(VariableToken.class));
        assertThat(tokens.get(1),instanceOf(VariableToken.class));
        assertThat(tokens.get(0).getValue(),equalTo("hello"));
        assertThat(tokens.get(1).getValue(),equalTo("world"));
    }

    @Test
    public void token1() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize(">#{hello}<>#{world}<");

        assertThat(tokens,hasSize(5));
        assertThat(tokens.get(0),instanceOf(ScalarToken.class));
        assertThat(tokens.get(1),instanceOf(VariableToken.class));
        assertThat(tokens.get(2),instanceOf(ScalarToken.class));
        assertThat(tokens.get(3),instanceOf(VariableToken.class));
        assertThat(tokens.get(4),instanceOf(ScalarToken.class));

        assertThat(tokens.get(0).getValue(),equalTo(">"));
        assertThat(tokens.get(1).getValue(),equalTo("hello"));
        assertThat(tokens.get(2).getValue(),equalTo("<>"));
        assertThat(tokens.get(3).getValue(),equalTo("world"));
        assertThat(tokens.get(4).getValue(),equalTo("<"));
    }

    @Test
    public void token2() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{he_ll_o}");
        assertThat(tokens,hasSize(1));
        assertThat(tokens.get(0),instanceOf(VariableToken.class));
        assertThat(tokens.get(0).getValue(),equalTo("he_ll_o"));
    }

    @Test
    public void token3() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{input.field1.field2}");
        assertThat(tokens,hasSize(1));
        assertThat(tokens.get(0),instanceOf(VariableToken.class));
        assertThat(tokens.get(0).getValue(),equalTo("input.field1.field2"));
    }

    @Test
    public void token4() throws TokenizerException {
        List<AbstractToken> tokens = VariableTokenizer.tokenize("#{input.4.field_2}");
        assertThat(tokens,hasSize(1));
        assertThat(tokens.get(0),instanceOf(VariableToken.class));
        assertThat(tokens.get(0).getValue(),equalTo("input.4.field_2"));
    }
}
