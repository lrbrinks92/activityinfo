package org.activityinfo.model.expr;

import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import org.activityinfo.model.expr.diagnostic.ExprSyntaxException;

import java.util.List;

/**
 * Splits an expression string into a sequence of tokens
 */
public class ExprLexer extends UnmodifiableIterator<Token> {

    public static final char DOUBLE_QUOTE = '"';
    public static final char SINGLE_QUOTE = '\'';

    private String string;
    private int currentCharIndex;
    private int currentTokenStart = 0;

    private static final String OPERATOR_CHARS = "+-/*&|=!";

    public ExprLexer(String string) {
        assert string != null : "expr cannot be null";
        this.string = string;
    }

    /**
     * @return the current character within the string being processed
     */
    private char peekChar() {
        return string.charAt(currentCharIndex);
    }

    private char nextChar() {
        return string.charAt(currentCharIndex++);
    }

    /**
     * Adds the current char to the current token
     */
    private void consumeChar() {
        currentCharIndex++;
    }

    private Token finishToken(TokenType type) {
        return finishToken(type, string.substring(currentTokenStart, currentCharIndex));
    }

    private Token finishToken(TokenType type, String text) {
        Token token = new Token(type, currentTokenStart, text);
        currentTokenStart = currentCharIndex;
        return token;
    }

    public List<Token> readAll() {
        List<Token> tokens = Lists.newArrayList();
        while (!isEndOfInput()) {
            tokens.add(next());
        }
        return tokens;
    }

    public boolean isEndOfInput() {
        return currentCharIndex >= string.length();
    }

    @Override
    public boolean hasNext() {
        return !isEndOfInput();
    }

    @Override
    public Token next() {
        char c = nextChar();
        if (c == '(') {
            return finishToken(TokenType.PAREN_START);

        } else if (c == ')') {
            return finishToken(TokenType.PAREN_END);

        } else if (c == '{') {
            return readQuotedToken(TokenType.SYMBOL, '}');

        } else if (c == '[') {
            return readQuotedToken(TokenType.SYMBOL, ']');

        } else if (c == ',') {
            return finishToken(TokenType.COMMA);

        } else if (c == '.') {
            return finishToken(TokenType.DOT);

        } else if (c == DOUBLE_QUOTE) {
            return readQuotedToken(TokenType.STRING_LITERAL, DOUBLE_QUOTE);

        } else if (c == SINGLE_QUOTE) {
            return readQuotedToken(TokenType.STRING_LITERAL, SINGLE_QUOTE);

        } else if (StringUtil.isWhitespace(c)) {
            return readWhitespace();

        } else if (isNumberPart(c)) {
            return readNumber();

        } else if (isOperator(c)) {
            return readOperator(c);

        } else if (isBooleanLiteral(c)) {
            return readBooleanLiteral(c);

        } else if (isSymbolStart(c)) {
            return readSymbol(TokenType.SYMBOL);

        } else {
            throw new RuntimeException("Symbol '" + c + "' is not supported");
        }
    }

    private boolean isOperator(char c) {
        return OPERATOR_CHARS.indexOf(c) != -1;
    }

    private Token readOperator(char c) {
        while (!isEndOfInput() && isOperator(peekChar())) {
            consumeChar();
        }
        return finishToken(TokenType.OPERATOR);
    }

    private boolean isSymbolStart(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private boolean isSymbolChar(char c) {
        return c == '_' || StringUtil.isAlphabetic(c) || Character.isDigit(c);
    }

    private boolean isNumberPart(char c) {
        return Character.isDigit(c) || c == '.';
    }

    private boolean isBooleanLiteral(char c) {
        final int currentIndex = currentCharIndex - 1;
        if (c == 't' || c == 'T') {
            String trueLiteral = Boolean.TRUE.toString();
            int endIndex = currentIndex + trueLiteral.length();
            if (endIndex <= string.length()) {
                String literal = string.substring(currentIndex, endIndex);
                return trueLiteral.equalsIgnoreCase(literal);
            }
        } else if (c == 'f' || c == 'F') {
            String falseLiteral = Boolean.FALSE.toString();
            int endIndex = currentIndex + falseLiteral.length();
            if (endIndex <= string.length()) {
                String literal = string.substring(currentIndex, endIndex);
                return falseLiteral.equalsIgnoreCase(literal);
            }
        }
        return false;
    }

    private Token readWhitespace() {
        while (!isEndOfInput() && StringUtil.isWhitespace(peekChar())) {
            consumeChar();
        }
        return finishToken(TokenType.WHITESPACE);
    }

    private Token readNumber() {
        while (!isEndOfInput() && isNumberPart(peekChar())) {
            consumeChar();
        }
        return finishToken(TokenType.NUMBER);
    }

    private Token readSymbol(TokenType tokenType) {
        while (!isEndOfInput() && isSymbolChar(peekChar())) {
            consumeChar();
        }
        return finishToken(tokenType);
    }

    private Token readQuotedToken(TokenType type, char closingQuote) throws ExprSyntaxException {
        while(true) {
            if (isEndOfInput()) {
                throw new ExprSyntaxException("End of input reached while looking for closing '" + closingQuote + "");
            }
            if (nextChar() == closingQuote) {
                return finishToken(type, string.substring(currentTokenStart+1, currentCharIndex-1));
            }
        }
    }

    private Token readBooleanLiteral(char c) {
        currentCharIndex--;
        if (c == 't' || c == 'T') {
            String trueLiteral = Boolean.TRUE.toString();
            String literal = string.substring(currentCharIndex, currentCharIndex + trueLiteral.length());
            if (trueLiteral.equalsIgnoreCase(literal)) {
                currentCharIndex += trueLiteral.length();
                return finishToken(TokenType.BOOLEAN_LITERAL);
            }
        } else if (c == 'f' || c == 'F') {
            String falseLiteral = Boolean.FALSE.toString();
            String literal = string.substring(currentCharIndex, currentCharIndex + falseLiteral.length());
            if (falseLiteral.equalsIgnoreCase(literal)) {
                currentCharIndex += falseLiteral.length();
                return finishToken(TokenType.BOOLEAN_LITERAL);
            }
        }
        throw new RuntimeException("Bug in isBooleanLiteral() ?");
    }

    private Token readBooleanOperator(char c) {
        if (c == '!') {
            if (string.charAt(currentCharIndex) == '=') { // check whether it's NOT (!) or NOT_EQUAL operator (!=)
                currentCharIndex++;
            }
            return finishToken(TokenType.OPERATOR);
        } else if (c == '&') {
            // if next char is also & then its && operator
            currentCharIndex++;
            return finishToken(TokenType.OPERATOR);
        } else if (c == '|') {
            currentCharIndex++;
            // if next char is also | then its || operator
            return finishToken(TokenType.OPERATOR);
        } else if (c == '=') {
            currentCharIndex++;
            // if next char is also = then its == operator
            return finishToken(TokenType.OPERATOR);
        }
        throw new RuntimeException("Invalid boolean operator.");
    }

}
