package ast;

import tokens.Token;

import java.util.List;

public final class Ast {
    private Ast() {
    }

    public abstract static class Node {
        public final int line;
        public final int column;

        protected Node(Token token) {
            this(token.line, token.column);
        }

        protected Node(int line, int column) {
            this.line = line;
            this.column = column;
        }

        public final String pretty() {
            StringBuilder out = new StringBuilder();
            write(out, 0);
            return out.toString();
        }

        public abstract void write(StringBuilder out, int indent);

        protected static void line(StringBuilder out, int indent, String text) {
            out.append("  ".repeat(indent)).append(text).append('\n');
        }
    }

    public static final class Module extends Node {
        public final List<Function> functions;

        public Module(List<Function> functions) {
            super(1, 1);
            this.functions = functions;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Module");
            for (Function function : functions) {
                function.write(out, indent + 1);
            }
        }
    }

    public static final class Function extends Node {
        public final String name;
        public final List<Param> params;
        public final Node requires;
        public final List<Param> returns;
        public final Node ensures;
        public final List<Local> uses;
        public final Node body;

        public Function(Token nameToken, String name, List<Param> params, Node requires,
                        List<Param> returns, Node ensures, List<Local> uses, Node body) {
            super(nameToken);
            this.name = name;
            this.params = params;
            this.requires = requires;
            this.returns = returns;
            this.ensures = ensures;
            this.uses = uses;
            this.body = body;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Function " + name);
            line(out, indent + 1, "params");
            for (Param param : params) {
                param.write(out, indent + 2);
            }
            if (requires != null) {
                line(out, indent + 1, "requires");
                requires.write(out, indent + 2);
            }
            line(out, indent + 1, "returns");
            for (Param param : returns) {
                param.write(out, indent + 2);
            }
            if (ensures != null) {
                line(out, indent + 1, "ensures");
                ensures.write(out, indent + 2);
            }
            if (!uses.isEmpty()) {
                line(out, indent + 1, "uses");
                for (Local local : uses) {
                    local.write(out, indent + 2);
                }
            }
            line(out, indent + 1, "body");
            body.write(out, indent + 2);
        }
    }

    public static final class Param extends Node {
        public final String name;
        public final String type;

        public Param(Token nameToken, String name, String type) {
            super(nameToken);
            this.name = name;
            this.type = type;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Param " + name + " : " + type);
        }
    }

    public static final class Local extends Node {
        public final String name;
        public final String type;

        public Local(Token nameToken, String name, String type) {
            super(nameToken);
            this.name = name;
            this.type = type;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, type == null ? "Local " + name : "Local " + name + " : " + type);
        }
    }

    public static final class Block extends Node {
        public final List<Node> statements;

        public Block(Token brace, List<Node> statements) {
            super(brace);
            this.statements = statements;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Block");
            for (Node statement : statements) {
                statement.write(out, indent + 1);
            }
        }
    }

    public static final class Assign extends Node {
        public final String target;
        public final Node value;

        public Assign(Token token, String target, Node value) {
            super(token);
            this.target = target;
            this.value = value;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Assign " + target);
            value.write(out, indent + 1);
        }
    }

    public static final class TupleAssign extends Node {
        public final List<String> targets;
        public final Node value;

        public TupleAssign(Token token, List<String> targets, Node value) {
            super(token);
            this.targets = targets;
            this.value = value;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "TupleAssign " + String.join(", ", targets));
            value.write(out, indent + 1);
        }
    }

    public static final class If extends Node {
        public final Node condition;
        public final Node thenBranch;
        public final Node elseBranch;

        public If(Token token, Node condition, Node thenBranch, Node elseBranch) {
            super(token);
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "If");
            condition.write(out, indent + 1);
            line(out, indent + 1, "then");
            thenBranch.write(out, indent + 2);
            if (elseBranch != null) {
                line(out, indent + 1, "else");
                elseBranch.write(out, indent + 2);
            }
        }
    }

    public static final class While extends Node {
        public final Node condition;
        public final Node invariant;
        public final Node body;

        public While(Token token, Node condition, Node invariant, Node body) {
            super(token);
            this.condition = condition;
            this.invariant = invariant;
            this.body = body;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "While");
            condition.write(out, indent + 1);
            if (invariant != null) {
                line(out, indent + 1, "invariant");
                invariant.write(out, indent + 2);
            }
            line(out, indent + 1, "body");
            body.write(out, indent + 2);
        }
    }

    public static final class Assert extends Node {
        public final Node condition;

        public Assert(Token token, Node condition) {
            super(token);
            this.condition = condition;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Assert");
            condition.write(out, indent + 1);
        }
    }

    public static final class Assume extends Node {
        public final Node condition;

        public Assume(Token token, Node condition) {
            super(token);
            this.condition = condition;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Assume");
            condition.write(out, indent + 1);
        }
    }

    public static final class Binary extends Node {
        public final String op;
        public final Node left;
        public final Node right;

        public Binary(Token token, String op, Node left, Node right) {
            super(token);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Binary " + op);
            left.write(out, indent + 1);
            right.write(out, indent + 1);
        }
    }

    public static final class Unary extends Node {
        public final String op;
        public final Node inner;

        public Unary(Token token, String op, Node inner) {
            super(token);
            this.op = op;
            this.inner = inner;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Unary " + op);
            inner.write(out, indent + 1);
        }
    }

    public static final class Call extends Node {
        public final String name;
        public final List<Node> args;

        public Call(Token token, String name, List<Node> args) {
            super(token);
            this.name = name;
            this.args = args;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Call " + name);
            for (Node arg : args) {
                arg.write(out, indent + 1);
            }
        }
    }

    public static final class Index extends Node {
        public final Node array;
        public final Node index;

        public Index(Token token, Node array, Node index) {
            super(token);
            this.array = array;
            this.index = index;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Index");
            array.write(out, indent + 1);
            index.write(out, indent + 1);
        }
    }

    public static final class Var extends Node {
        public final String name;

        public Var(Token token, String name) {
            super(token);
            this.name = name;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Var " + name);
        }
    }

    public static final class IntLit extends Node {
        public final String text;

        public IntLit(Token token) {
            super(token);
            this.text = token.lexeme;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Int " + text);
        }
    }

    public static final class BoolLit extends Node {
        public final boolean value;

        public BoolLit(Token token, boolean value) {
            super(token);
            this.value = value;
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Bool " + value);
        }
    }

    public static final class ErrorExpr extends Node {
        public ErrorExpr(Token token) {
            super(token);
        }

        @Override
        public void write(StringBuilder out, int indent) {
            line(out, indent, "Error");
        }
    }
}
