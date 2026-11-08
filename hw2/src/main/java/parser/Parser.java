package parser;

import ast.Ast;
import tokens.Token;
import tokens.TokenType;

import java.util.ArrayList;
import java.util.List;

public final class Parser {
    private final List<Token> tokens;
    private final List<String> errors = new ArrayList<>();
    private int index;
    private boolean panic;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Result parse() {
        Ast.Module module;
        if (isEmpty()) {
            error(peek(), "пустой ввод");
            module = new Ast.Module(List.of());
        } else {
            List<Ast.Function> functions = new ArrayList<>();
            while (!check(TokenType.EOF) && errors.size() < 40) {
                skipLexicalErrors();
                if (check(TokenType.EOF)) {
                    break;
                }
                if (check(TokenType.KW_FUNCTION)) {
                    error(peek(), "ключевое слово function не входит в грамматику Funny");
                    advance();
                }
                if (!check(TokenType.IDENT)) {
                    error(peek(), "ожидалась функция, найдено " + describe(peek()));
                    recoverFunction();
                    continue;
                }
                int before = index;
                functions.add(parseFunction());
                if (index == before) {
                    advance();
                }
            }
            module = new Ast.Module(functions);
        }
        return new Result(module, List.copyOf(errors));
    }

    private boolean isEmpty() {
        for (Token token : tokens) {
            if (token.type != TokenType.EOF) {
                return false;
            }
        }
        return true;
    }

    private Ast.Function parseFunction() {
        Token name = expect(TokenType.IDENT, "имя функции");
        expect(TokenType.LPAREN, "'('");
        List<Ast.Param> params = parseParamList();
        expect(TokenType.RPAREN, "')'");

        Ast.Node requires = null;
        if (check(TokenType.KW_REQUIRES)) {
            advance();
            requires = parseCondition();
        }

        expect(TokenType.KW_RETURNS, "'returns'");
        List<Ast.Param> returns = parseParamList();
        if (returns.isEmpty()) {
            error(peek(), "у функции должен быть хотя бы один результат");
        }

        Ast.Node ensures = null;
        if (check(TokenType.KW_ENSURES)) {
            advance();
            ensures = parseCondition();
        }

        List<Ast.Local> uses = new ArrayList<>();
        if (check(TokenType.KW_USES)) {
            advance();
            uses = parseUses();
        }

        Ast.Node body = parseStatement();
        if (panic) {
            recoverFunction();
        }
        return new Ast.Function(name, name.lexeme, params, requires, returns, ensures, uses, body);
    }

    private List<Ast.Param> parseParamList() {
        List<Ast.Param> params = new ArrayList<>();
        if (!check(TokenType.IDENT)) {
            return params;
        }
        params.add(parseParam());
        while (check(TokenType.COMMA)) {
            advance();
            params.add(parseParam());
        }
        return params;
    }

    private Ast.Param parseParam() {
        Token name = expect(TokenType.IDENT, "имя");
        expect(TokenType.COLON, "':'");
        String type = parseType();
        return new Ast.Param(name, name.lexeme, type);
    }

    private String parseType() {
        if (check(TokenType.KW_INT)) {
            advance();
            if (check(TokenType.LBRACK)) {
                advance();
                expect(TokenType.RBRACK, "']'");
                if (check(TokenType.LBRACK)) {
                    error(peek(), "многомерные массивы недопустимы");
                    panic = true;
                }
                return "int[]";
            }
            return "int";
        }
        error(peek(), "ожидался тип int, найдено " + describe(peek()));
        panic = true;
        return "int";
    }

    private List<Ast.Local> parseUses() {
        List<Ast.Local> locals = new ArrayList<>();
        locals.add(parseLocal());
        while (check(TokenType.COMMA)) {
            advance();
            locals.add(parseLocal());
        }
        return locals;
    }

    private Ast.Local parseLocal() {
        Token name = expect(TokenType.IDENT, "локальное имя");
        String type = null;
        if (check(TokenType.COLON)) {
            advance();
            type = parseType();
        }
        return new Ast.Local(name, name.lexeme, type);
    }

    private Ast.Node parseStatement() {
        skipLexicalErrors();
        if (panic) {
            return new Ast.ErrorExpr(peek());
        }
        if (check(TokenType.LBRACE)) {
            return parseBlock();
        }
        if (check(TokenType.KW_IF)) {
            return parseIf();
        }
        if (check(TokenType.KW_WHILE)) {
            return parseWhile();
        }
        if (check(TokenType.KW_ASSERT)) {
            return parseAssert();
        }
        if (check(TokenType.KW_ASSUME)) {
            return parseAssume();
        }
        if (check(TokenType.IDENT)) {
            return parseAssignment();
        }
        error(peek(), "ожидался оператор, найдено " + describe(peek()));
        panic = true;
        return new Ast.ErrorExpr(peek());
    }

    private Ast.Block parseBlock() {
        Token brace = expect(TokenType.LBRACE, "'{'");
        List<Ast.Node> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            int before = index;
            statements.add(parseStatement());
            if (panic) {
                recoverStatement();
            }
            if (index == before) {
                advance();
            }
        }
        expect(TokenType.RBRACE, "'}'");
        return new Ast.Block(brace, statements);
    }

    private Ast.If parseIf() {
        Token token = expect(TokenType.KW_IF, "'if'");
        expect(TokenType.LPAREN, "'('");
        Ast.Node condition = parseCondition();
        expect(TokenType.RPAREN, "')'");
        Ast.Node thenBranch = parseBlock();
        Ast.Node elseBranch = null;
        if (check(TokenType.KW_ELSE)) {
            advance();
            elseBranch = parseBlock();
        }
        return new Ast.If(token, condition, thenBranch, elseBranch);
    }

    private Ast.While parseWhile() {
        Token token = expect(TokenType.KW_WHILE, "'while'");
        expect(TokenType.LPAREN, "'('");
        Ast.Node condition = parseCondition();
        expect(TokenType.RPAREN, "')'");
        Ast.Node invariant = null;
        if (check(TokenType.KW_INVARIANT)) {
            advance();
            invariant = parseCondition();
        }
        Ast.Node body = parseBlock();
        return new Ast.While(token, condition, invariant, body);
    }

    private Ast.Assert parseAssert() {
        Token token = expect(TokenType.KW_ASSERT, "'assert'");
        Ast.Node condition = parseCondition();
        expect(TokenType.SEMICOLON, "';'");
        return new Ast.Assert(token, condition);
    }

    private Ast.Assume parseAssume() {
        Token token = expect(TokenType.KW_ASSUME, "'assume'");
        Ast.Node condition = parseCondition();
        expect(TokenType.SEMICOLON, "';'");
        return new Ast.Assume(token, condition);
    }

    private Ast.Node parseAssignment() {
        Token first = expect(TokenType.IDENT, "имя");
        if (check(TokenType.LBRACK)) {
            Token bracket = advance();
            Ast.Node index = parseExpr();
            expect(TokenType.RBRACK, "']'");
            if (check(TokenType.LBRACK)) {
                error(peek(), "многомерные массивы недопустимы");
                advance();
            }
            expect(TokenType.ASSIGN, "'='");
            Ast.Node value = parseExpr();
            expect(TokenType.SEMICOLON, "';'");
            Ast.Call updated = new Ast.Call(bracket, "set", List.of(
                    new Ast.Var(first, first.lexeme),
                    index,
                    value
            ));
            return new Ast.Assign(first, first.lexeme, updated);
        }

        List<String> names = new ArrayList<>();
        names.add(first.lexeme);
        if (check(TokenType.COMMA)) {
            while (check(TokenType.COMMA)) {
                advance();
                Token name = expect(TokenType.IDENT, "имя");
                names.add(name.lexeme);
            }
            expect(TokenType.ASSIGN, "'='");
            Ast.Node value = parseExpr();
            expect(TokenType.SEMICOLON, "';'");
            if (!(value instanceof Ast.Call)) {
                error(first, "кортежное присваивание требует вызов функции");
            }
            return new Ast.TupleAssign(first, names, value);
        }

        expect(TokenType.ASSIGN, "'='");
        Ast.Node value = parseExpr();
        expect(TokenType.SEMICOLON, "';'");
        return new Ast.Assign(first, first.lexeme, value);
    }

    private Ast.Node parseCondition() {
        return parseImplication();
    }

    private Ast.Node parseImplication() {
        Ast.Node left = parseOr();
        if (check(TokenType.ARROW)) {
            Token op = advance();
            return new Ast.Binary(op, "->", left, parseImplication());
        }
        return left;
    }

    private Ast.Node parseOr() {
        Ast.Node left = parseAnd();
        while (check(TokenType.KW_OR)) {
            Token op = advance();
            left = new Ast.Binary(op, "or", left, parseAnd());
        }
        return left;
    }

    private Ast.Node parseAnd() {
        Ast.Node left = parseNot();
        while (check(TokenType.KW_AND)) {
            Token op = advance();
            left = new Ast.Binary(op, "and", left, parseNot());
        }
        return left;
    }

    private Ast.Node parseNot() {
        if (check(TokenType.KW_NOT)) {
            Token op = advance();
            return new Ast.Unary(op, "not", parseNot());
        }
        return parseAtom();
    }

    private Ast.Node parseAtom() {
        if (check(TokenType.KW_TRUE)) {
            return new Ast.BoolLit(advance(), true);
        }
        if (check(TokenType.KW_FALSE)) {
            return new Ast.BoolLit(advance(), false);
        }
        if (check(TokenType.LPAREN) && groupedCondition()) {
            advance();
            Ast.Node inner = parseCondition();
            expect(TokenType.RPAREN, "')'");
            return inner;
        }
        return parseComparison();
    }

    private boolean groupedCondition() {
        int depth = 0;
        for (int cursor = index; cursor < tokens.size(); cursor++) {
            TokenType type = tokens.get(cursor).type;
            if (type == TokenType.LPAREN) {
                depth++;
            } else if (type == TokenType.RPAREN) {
                depth--;
                if (depth == 0) {
                    TokenType after = cursor + 1 < tokens.size()
                            ? tokens.get(cursor + 1).type
                            : TokenType.EOF;
                    return !isRelop(after);
                }
            } else if (type == TokenType.EOF) {
                break;
            }
        }
        return true;
    }

    private Ast.Node parseComparison() {
        Ast.Node left = parseExpr();
        if (!isRelop(peek().type)) {
            error(peek(), "ожидался оператор сравнения, найдено " + describe(peek()));
            panic = true;
            return left;
        }
        Token op = advance();
        Ast.Node right = parseExpr();
        return new Ast.Binary(op, op.lexeme, left, right);
    }

    private Ast.Node parseExpr() {
        Ast.Node left = parseTerm();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            left = new Ast.Binary(op, op.lexeme, left, parseTerm());
        }
        return left;
    }

    private Ast.Node parseTerm() {
        Ast.Node left = parseUnary();
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            Token op = advance();
            left = new Ast.Binary(op, op.lexeme, left, parseUnary());
        }
        return left;
    }

    private Ast.Node parseUnary() {
        if (check(TokenType.MINUS)) {
            Token op = advance();
            return new Ast.Unary(op, "-", parseUnary());
        }
        return parsePrimary();
    }

    private Ast.Node parsePrimary() {
        if (check(TokenType.INT)) {
            return new Ast.IntLit(advance());
        }
        if (check(TokenType.IDENT) || check(TokenType.KW_LENGTH)) {
            Token name = advance();
            if (check(TokenType.LPAREN)) {
                advance();
                List<Ast.Node> args = parseArguments();
                expect(TokenType.RPAREN, "')'");
                return new Ast.Call(name, name.lexeme, args);
            }
            Ast.Node node = new Ast.Var(name, name.lexeme);
            if (check(TokenType.LBRACK)) {
                Token bracket = advance();
                Ast.Node index = parseExpr();
                expect(TokenType.RBRACK, "']'");
                if (check(TokenType.LBRACK)) {
                    error(peek(), "многомерные массивы недопустимы");
                }
                node = new Ast.Index(bracket, node, index);
            }
            return node;
        }
        if (check(TokenType.LPAREN)) {
            advance();
            Ast.Node inner = parseExpr();
            expect(TokenType.RPAREN, "')'");
            return inner;
        }
        if (check(TokenType.ERROR)) {
            error(peek(), "лексическая ошибка '" + peek().lexeme + "'");
            Token bad = advance();
            panic = true;
            return new Ast.ErrorExpr(bad);
        }
        error(peek(), "ожидалось выражение, найдено " + describe(peek()));
        panic = true;
        return new Ast.ErrorExpr(peek());
    }

    private List<Ast.Node> parseArguments() {
        List<Ast.Node> args = new ArrayList<>();
        if (check(TokenType.RPAREN)) {
            return args;
        }
        args.add(parseExpr());
        while (check(TokenType.COMMA)) {
            advance();
            args.add(parseExpr());
        }
        return args;
    }

    private void recoverStatement() {
        while (!check(TokenType.EOF) && !check(TokenType.SEMICOLON) && !check(TokenType.RBRACE)) {
            advance();
        }
        if (check(TokenType.SEMICOLON)) {
            advance();
        }
        panic = false;
    }

    private void recoverFunction() {
        int depth = 0;
        boolean opened = false;
        while (!check(TokenType.EOF)) {
            if (check(TokenType.LBRACE)) {
                opened = true;
                depth++;
                advance();
            } else if (check(TokenType.RBRACE)) {
                if (!opened || depth <= 1) {
                    advance();
                    break;
                }
                depth--;
                advance();
            } else {
                advance();
            }
        }
        panic = false;
    }

    private void skipLexicalErrors() {
        while (check(TokenType.ERROR)) {
            error(peek(), "лексическая ошибка '" + peek().lexeme + "'");
            advance();
        }
    }

    private Token expect(TokenType type, String what) {
        if (check(type)) {
            return advance();
        }
        error(peek(), "ожидалось " + what + ", найдено " + describe(peek()));
        panic = true;
        return peek();
    }

    private boolean check(TokenType type) {
        return peek().type == type;
    }

    private Token advance() {
        Token token = peek();
        if (token.type != TokenType.EOF && index < tokens.size() - 1) {
            index++;
        }
        return token;
    }

    private Token peek() {
        return tokens.get(index);
    }

    private void error(Token token, String message) {
        if (errors.size() >= 40) {
            return;
        }
        errors.add(token.position() + ": " + message);
    }

    private static boolean isRelop(TokenType type) {
        return type == TokenType.EQ_EQ
                || type == TokenType.NOT_EQ
                || type == TokenType.LESS
                || type == TokenType.GREATER
                || type == TokenType.LESS_EQ
                || type == TokenType.GREATER_EQ;
    }

    private static String describe(Token token) {
        if (token.type == TokenType.EOF) {
            return "конец файла";
        }
        if (token.lexeme == null || token.lexeme.isEmpty()) {
            return token.type.name();
        }
        return "'" + token.lexeme + "'";
    }

    public record Result(Ast.Module module, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }
}
